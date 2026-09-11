package fr.umontpellier.iut.discordbot.services;

import com.google.gson.Gson;
import fr.umontpellier.iut.discordbot.database.dataobjects.ChannelLock;
import fr.umontpellier.iut.discordbot.database.repositories.ChannelLockRepository;
import fr.umontpellier.iut.discordbot.services.exceptions.ChannelLockStateServiceException;
import fr.umontpellier.iut.discordbot.services.exceptions.ServiceException;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Verrouillage d'un salon textuel.
 * <p>
 * Le verrouillage ne touche qu'aux permissions de {@link #LOCKED_PERMISSIONS} : il les refuse à @everyone, retire
 * les autorisations explicites des autres rôles et membres, et les accorde au rôle admin. L'état d'origine de chaque
 * permission modifiée est sauvegardé ; au déverrouillage, seules ces permissions sont restaurées, les autres
 * modifications faites pendant le verrouillage sont conservées.
 */
public class ChannelLockService {
    public static final long LOCKED_PERMISSIONS = Permission.getRaw(
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_ADD_REACTION,
            Permission.MESSAGE_SEND_IN_THREADS,
            Permission.CREATE_PUBLIC_THREADS,
            Permission.CREATE_PRIVATE_THREADS
    );

    private static final int STATE_VERSION = 2;
    private static final String TYPE_ROLE = "ROLE";
    private static final String TYPE_MEMBER = "MEMBER";

    private final Logger logger;
    private final Gson gson;
    private final ChannelLockRepository repository;

    public ChannelLockService(ChannelLockRepository repository) {
        this.logger = LoggerFactory.getLogger(this.getClass());
        this.gson = new Gson();
        this.repository = repository;
    }

    public Optional<ChannelLock> getChannelLock(int id) {
        return repository.findByPrimaryKey(id);
    }

    public Optional<ChannelLock> getChannelLockByChannelId(String channelId) {
        try {
            return repository.findByChannelId(channelId);
        } catch (Exception e) {
            throw new ServiceException("Impossible de recuperer l'etat du verrouillage.", e);
        }
    }

    public boolean isChannelLocked(String channelId) {
        return getChannelLockByChannelId(channelId).isPresent();
    }

    /**
     * @param adminRoleId rôle qui garde le droit d'écrire dans le salon verrouillé (peut être null)
     */
    public void lockChannel(TextChannel channel, Guild guild, Member member, @Nullable String adminRoleId) {
        String channelId = channel.getId();

        if (isChannelLocked(channelId)) {
            throw new ChannelLockStateServiceException("Ce salon est deja verrouille.");
        }

        List<PlannedChange> plan = planLock(channel, guild, adminRoleId);
        LockState state = new LockState();
        state.version = STATE_VERSION;
        state.overrides = plan.stream().map(PlannedChange::original).toList();

        // Le verrouillage est enregistré avant de toucher aux permissions : si l'enregistrement échoue, le salon
        // n'est pas modifié ; si les permissions échouent, on annule ce qui a été fait.
        try {
            repository.insert(new ChannelLock(
                    0,
                    channelId,
                    guild.getId(),
                    member.getId(),
                    System.currentTimeMillis(),
                    gson.toJson(state)
            ));
        } catch (Exception e) {
            throw new ServiceException("Erreur lors de l'enregistrement du verrouillage.", e);
        }

        List<PlannedChange> applied = new ArrayList<>();
        try {
            for (PlannedChange change : plan) {
                channel.upsertPermissionOverride(change.holder())
                        .setPermissions(change.allowed(), change.denied())
                        .complete();
                applied.add(change);
            }
        } catch (Exception e) {
            logger.warn("Failed to lock channel {}, rolling back: {}", channelId, e.getMessage());
            int failures = rollback(channel, applied);
            if (failures == 0) {
                repository.deleteByChannelId(channelId);
            }
            throw new ServiceException("Erreur lors du verrouillage du salon"
                    + (failures == 0 ? "." : ", certaines permissions n'ont pas pu être rétablies : utilisez /lock off."), e);
        }

        logger.info("Channel {} locked by user {}", channelId, member.getId());
    }

    /**
     * @return le verrouillage qui vient d'être retiré
     */
    public ChannelLock unlockChannel(TextChannel channel) {
        String channelId = channel.getId();
        ChannelLock lock = getChannelLockByChannelId(channelId)
                .orElseThrow(() -> new ChannelLockStateServiceException("Ce salon n'est pas verrouille."));

        List<OverrideSnapshot> snapshots;
        try {
            snapshots = readSnapshots(lock.getChannelStateJson(), channel.getGuild());
        } catch (Exception e) {
            throw new ServiceException("Impossible de lire l'etat sauvegarde du salon.", e);
        }

        int failures = restore(channel, snapshots);
        if (failures > 0) {
            // On garde le verrouillage en base pour pouvoir réessayer sans perdre l'état d'origine
            throw new ServiceException(failures + " permission(s) n'ont pas pu être restaurées. "
                    + "Le salon reste marqué comme verrouillé, réessayez /lock off.");
        }

        try {
            repository.deleteByChannelId(channelId);
        } catch (Exception e) {
            throw new ServiceException("Permissions restaurées, mais impossible de supprimer le verrouillage.", e);
        }

        logger.info("Channel {} unlocked", channelId);
        return lock;
    }

    private List<PlannedChange> planLock(TextChannel channel, Guild guild, @Nullable String adminRoleId) {
        List<PlannedChange> plan = new ArrayList<>();
        Role publicRole = guild.getPublicRole();
        Role adminRole = adminRoleId == null || adminRoleId.isBlank() ? null : guild.getRoleById(adminRoleId);
        if (adminRole != null && adminRole.equals(publicRole)) {
            adminRole = null;
        }

        // @everyone : permissions refusées
        OverrideSnapshot everyone = snapshotOf(channel, TYPE_ROLE, publicRole.getId());
        plan.add(new PlannedChange(publicRole, everyone,
                everyone.allowed & ~LOCKED_PERMISSIONS,
                everyone.denied | LOCKED_PERMISSIONS));

        // Rôle admin : permissions accordées
        if (adminRole != null) {
            OverrideSnapshot admin = snapshotOf(channel, TYPE_ROLE, adminRole.getId());
            plan.add(new PlannedChange(adminRole, admin,
                    admin.allowed | LOCKED_PERMISSIONS,
                    admin.denied & ~LOCKED_PERMISSIONS));
        }

        // Autres rôles et membres : retrait des autorisations explicites qui passeraient outre le refus de @everyone
        for (PermissionOverride override : channel.getPermissionOverrides()) {
            if ((override.getAllowedRaw() & LOCKED_PERMISSIONS) == 0) {
                continue;
            }

            String id = override.getId();
            if (id.equals(publicRole.getId()) || (adminRole != null && id.equals(adminRole.getId()))) {
                continue;
            }

            IPermissionHolder holder = override.isRoleOverride()
                    ? override.getRole()
                    : resolveMember(guild, override.getIdLong());
            if (holder == null) {
                logger.warn("Holder {} of a permission override could not be resolved, skipping it", id);
                continue;
            }

            OverrideSnapshot snapshot = OverrideSnapshot.of(override);
            plan.add(new PlannedChange(holder, snapshot,
                    snapshot.allowed & ~LOCKED_PERMISSIONS,
                    snapshot.denied));
        }

        return plan;
    }

    /**
     * Annule les changements qui viennent d'être appliqués. On repart des valeurs sauvegardées plutôt que du cache
     * JDA, qui n'est pas forcément encore à jour juste après les modifications.
     *
     * @return le nombre de changements qui n'ont pas pu être annulés
     */
    private int rollback(TextChannel channel, List<PlannedChange> applied) {
        int failures = 0;

        for (PlannedChange change : applied) {
            try {
                channel.upsertPermissionOverride(change.holder())
                        .setPermissions(change.original().allowed, change.original().denied)
                        .complete();
            } catch (Exception e) {
                failures++;
                logger.warn("Failed to roll back permissions of {} {} in channel {}: {}",
                        change.original().type, change.original().id, channel.getId(), e.getMessage());
            }
        }

        return failures;
    }

    /**
     * Restaure les permissions verrouillées de chaque snapshot.
     *
     * @return le nombre de permissions qui n'ont pas pu être restaurées
     */
    private int restore(TextChannel channel, List<OverrideSnapshot> snapshots) {
        Guild guild = channel.getGuild();
        int failures = 0;

        for (OverrideSnapshot snapshot : snapshots) {
            try {
                PermissionOverride current = findOverride(channel, snapshot.id);
                long currentAllowed = current == null ? 0 : current.getAllowedRaw();
                long currentDenied = current == null ? 0 : current.getDeniedRaw();

                long allowed = restoreLockedBits(currentAllowed, snapshot.allowed);
                long denied = restoreLockedBits(currentDenied, snapshot.denied);

                if (allowed == currentAllowed && denied == currentDenied) {
                    continue;
                }

                if (allowed == 0 && denied == 0) {
                    // current ne peut pas être null ici, sinon rien n'aurait changé
                    current.delete().complete();
                    continue;
                }

                IPermissionHolder holder = TYPE_ROLE.equals(snapshot.type)
                        ? guild.getRoleById(snapshot.id)
                        : resolveMember(guild, Long.parseLong(snapshot.id));
                if (holder == null) {
                    // Rôle supprimé ou membre parti : il n'y a plus rien à restaurer
                    logger.info("{} {} no longer exists, skipping permission restore", snapshot.type, snapshot.id);
                    continue;
                }

                channel.upsertPermissionOverride(holder)
                        .setPermissions(allowed, denied)
                        .complete();
            } catch (Exception e) {
                failures++;
                logger.warn("Failed to restore permissions of {} {} in channel {}: {}",
                        snapshot.type, snapshot.id, channel.getId(), e.getMessage());
            }
        }

        return failures;
    }

    /**
     * Remet les bits de {@link #LOCKED_PERMISSIONS} à leur valeur d'origine et garde les autres bits actuels.
     */
    public static long restoreLockedBits(long current, long original) {
        return (current & ~LOCKED_PERMISSIONS) | (original & LOCKED_PERMISSIONS);
    }

    private List<OverrideSnapshot> readSnapshots(String json, Guild guild) {
        LockState state = gson.fromJson(json, LockState.class);

        if (state.version >= STATE_VERSION) {
            return state.overrides == null ? List.of() : state.overrides;
        }

        // Ancien format : copie complète des permissions du salon, mais seul @everyone était modifié
        String everyoneId = guild.getPublicRole().getId();
        OverrideSnapshot everyone = new OverrideSnapshot(TYPE_ROLE, everyoneId, 0, 0);
        if (state.permissionOverrides != null) {
            for (Map<String, Object> data : state.permissionOverrides.values()) {
                if (everyoneId.equals(String.valueOf(data.get("id")))) {
                    everyone = new OverrideSnapshot(TYPE_ROLE, everyoneId,
                            parseLong(data.get("allowed")), parseLong(data.get("denied")));
                }
            }
        }

        return List.of(everyone);
    }

    private OverrideSnapshot snapshotOf(TextChannel channel, String type, String id) {
        PermissionOverride override = findOverride(channel, id);
        return override == null ? new OverrideSnapshot(type, id, 0, 0) : OverrideSnapshot.of(override);
    }

    @Nullable
    private PermissionOverride findOverride(TextChannel channel, String id) {
        for (PermissionOverride override : channel.getPermissionOverrides()) {
            if (override.getId().equals(id)) {
                return override;
            }
        }
        return null;
    }

    private long parseLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue) {
            return Long.parseLong(stringValue);
        }
        throw new IllegalArgumentException("Unsupported numeric value: " + value);
    }

    @Nullable
    private Member resolveMember(Guild guild, long memberId) {
        Member member = guild.getMemberById(memberId);
        if (member != null) {
            return member;
        }

        try {
            return guild.retrieveMemberById(memberId).complete();
        } catch (Exception e) {
            return null;
        }
    }

    private record PlannedChange(IPermissionHolder holder, OverrideSnapshot original, long allowed, long denied) {
    }

    /**
     * État sauvegardé en base (JSON). {@code permissionOverrides} n'existe que dans l'ancien format (version 0).
     */
    private static class LockState {
        int version;
        List<OverrideSnapshot> overrides;
        Map<String, Map<String, Object>> permissionOverrides;
    }

    private static class OverrideSnapshot {
        String type;
        String id;
        long allowed;
        long denied;

        // Pour Gson
        OverrideSnapshot() {
        }

        OverrideSnapshot(String type, String id, long allowed, long denied) {
            this.type = type;
            this.id = id;
            this.allowed = allowed;
            this.denied = denied;
        }

        static OverrideSnapshot of(PermissionOverride override) {
            return new OverrideSnapshot(
                    override.isRoleOverride() ? TYPE_ROLE : TYPE_MEMBER,
                    override.getId(),
                    override.getAllowedRaw(),
                    override.getDeniedRaw()
            );
        }
    }
}
