package fr.umontpellier.iut.discordbot.commands;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.database.dataobjects.ChannelLock;
import fr.umontpellier.iut.discordbot.database.repositories.ChannelLockRepository;
import fr.umontpellier.iut.discordbot.lib.AbstractCommand;
import fr.umontpellier.iut.discordbot.services.ChannelLockService;
import fr.umontpellier.iut.discordbot.services.exceptions.ServiceException;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class LockCommand extends AbstractCommand {
    private final ChannelLockService channelLockService;

    public LockCommand(Bot bot) {
        super(bot);
        this.channelLockService = new ChannelLockService(
                getBot().getRepositories().getRepository(ChannelLockRepository.class)
        );
    }

    @NotNull
    @Override
    public SlashCommandData getCommandInformation() {
        return Commands.slash("lock", "Bloquer les interactions avec ce salon").addSubcommands(
                new SubcommandData("on", "Activer le verrouillage du salon"),
                new SubcommandData("off", "Désactiver le verrouillage du salon"),
                new SubcommandData("status", "Obtenir l'état du verrouillage du salon")
        );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || !(event.getChannel() instanceof TextChannel)) {
            event.reply("❌ Cette commande n'est disponible que dans un salon textuel d'un serveur.").setEphemeral(true).queue();
            return;
        }

        String subcommand = Objects.requireNonNull(event.getSubcommandName());

        switch (subcommand) {
            case "on" -> handleLockOn(event);
            case "off" -> handleLockOff(event);
            case "status" -> handleStatus(event);
            default -> event.reply("Commande inconnue").setEphemeral(true).queue();
        }
    }

    private void handleLockOn(SlashCommandInteractionEvent event) {
        Optional<Guild> guildOpt = getGuild(event);
        Optional<Member> memberOpt = getMember(event);

        if (guildOpt.isEmpty() || memberOpt.isEmpty()) {
            event.reply("❌ Impossible de récupérer la guilde ou le membre.").setEphemeral(true).queue();
            return;
        }

        Member member = memberOpt.get();
        if (!isAdmin(member)) {
            event.reply("❌ Vous n'avez pas la permission de verrouiller ce salon.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();
        if (replyIfLockStateIs(event, channel, true, "❌ Ce salon est déjà verrouillé.")) {
            return;
        }

        event.deferReply().queue();
        Guild guild = guildOpt.get();

        CompletableFuture.runAsync(() -> {
            try {
                channelLockService.lockChannel(channel, guild, member, getBot().getConfig().get().getAdminRole());
                event.getHook().editOriginal("✅ Le salon a été verrouillé par " + member.getAsMention()).queue();
                sendLockLog("# \uD83D\uDD12 Salon verrouillé", 0xFF0000, String.format(
                        "Salon: %s\nPar: %s\nDate: %s",
                        channel.getAsMention(),
                        member.getAsMention(),
                        TimeFormat.DATE_TIME_SHORT.now()
                ));
            } catch (ServiceException e) {
                logger.warn("Failed to lock channel: {}", e.getMessage());
                event.getHook().editOriginal("❌ " + e.getMessage()).queue();
            } catch (Exception e) {
                logger.error("Unexpected error while locking channel", e);
                event.getHook().editOriginal("❌ Erreur inattendue lors du verrouillage.").queue();
            }
        });
    }

    private void handleLockOff(SlashCommandInteractionEvent event) {
        Optional<Member> memberOpt = getMember(event);

        if (memberOpt.isEmpty()) {
            event.reply("❌ Impossible de récupérer le membre.").setEphemeral(true).queue();
            return;
        }

        Member member = memberOpt.get();
        if (!isAdmin(member)) {
            event.reply("❌ Vous n'avez pas la permission de déverrouiller ce salon.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getChannel().asTextChannel();
        if (replyIfLockStateIs(event, channel, false, "❌ Ce salon n'est pas verrouillé.")) {
            return;
        }

        event.deferReply().queue();

        CompletableFuture.runAsync(() -> {
            try {
                ChannelLock lock = channelLockService.unlockChannel(channel);
                event.getHook().editOriginal("✅ Le salon a été déverrouillé. L'état précédent a été restauré.").queue();
                sendLockLog("# \uD83D\uDD13 Salon déverrouillé", 0x00FF00, String.format(
                        "Salon: %s\nPar: %s\nDate: %s\n\nVerrouillé par <@%s> le %s",
                        channel.getAsMention(),
                        member.getAsMention(),
                        TimeFormat.DATE_TIME_SHORT.now(),
                        lock.getLockedById(),
                        TimeFormat.DATE_TIME_SHORT.atTimestamp(lock.getLockedAt())
                ));
            } catch (ServiceException e) {
                logger.warn("Failed to unlock channel: {}", e.getMessage());
                event.getHook().editOriginal("❌ " + e.getMessage()).queue();
            } catch (Exception e) {
                logger.error("Unexpected error while unlocking channel", e);
                event.getHook().editOriginal("❌ Erreur inattendue lors du déverrouillage.").queue();
            }
        });
    }

    private void handleStatus(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        TextChannel channel = event.getChannel().asTextChannel();

        CompletableFuture.runAsync(() -> {
            try {
                Optional<ChannelLock> lockOpt = channelLockService.getChannelLockByChannelId(channel.getId());
                if (lockOpt.isEmpty()) {
                    event.getHook().editOriginal("ℹ️ Ce salon n'est pas verrouillé.").queue();
                    return;
                }

                // Une mention s'affiche même si le compte a été supprimé, contrairement à retrieveUserById
                ChannelLock lock = lockOpt.get();
                String statusMessage = """
                        🔒 Ce salon est actuellement verrouillé
                        Verrouillé par: <@%s>
                        Date du verrouillage: <t:%d:F>""".formatted(
                        lock.getLockedById(),
                        lock.getLockedAt() / 1000
                );

                event.getHook().editOriginal(statusMessage).queue();
            } catch (ServiceException e) {
                logger.warn("Failed to get lock status: {}", e.getMessage());
                event.getHook().editOriginal("❌ " + e.getMessage()).queue();
            } catch (Exception e) {
                logger.error("Unexpected error while getting lock status", e);
                event.getHook().editOriginal("❌ Erreur inattendue lors de la récupération de l'état.").queue();
            }
        });
    }

    /**
     * Répond en privé si le salon est déjà dans l'état demandé, avant la réponse publique de la commande.
     *
     * @return true si une réponse a été envoyée
     */
    private boolean replyIfLockStateIs(SlashCommandInteractionEvent event, TextChannel channel, boolean locked, String message) {
        try {
            if (channelLockService.isChannelLocked(channel.getId()) == locked) {
                event.reply(message).setEphemeral(true).queue();
                return true;
            }
            return false;
        } catch (ServiceException e) {
            logger.warn("Failed to get lock state: {}", e.getMessage());
            event.reply("❌ " + e.getMessage()).setEphemeral(true).queue();
            return true;
        }
    }

    private void sendLockLog(String title, int color, String details) {
        String channelId = getBot().getConfig().get().getLockChannelId();
        if (channelId == null || channelId.isBlank()) {
            logger.warn("No lock log channel configured; skipping Discord log message");
            return;
        }

        getBot().getLogSender().sendComponentToChannelId(channelId,
                Container.of(
                        TextDisplay.of(title),
                        Separator.createDivider(Separator.Spacing.SMALL),
                        TextDisplay.of(details)
                ).withAccentColor(color)
        );
    }
}
