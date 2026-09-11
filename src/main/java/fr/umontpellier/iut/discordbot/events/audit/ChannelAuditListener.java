package fr.umontpellier.iut.discordbot.events.audit;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.config.ConfigStructure.LogChannel;
import fr.umontpellier.iut.discordbot.lib.AuditLogFormatter;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogChange;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.channel.attribute.ICategorizableChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Création, modification et suppression de salons.
 */
public class ChannelAuditListener extends AbstractAuditLogListener {
	private static final Map<String, String> LABELS = Map.of(
			"name", "Nom",
			"topic", "Sujet",
			"nsfw", "NSFW",
			"rate_limit_per_user", "Mode lent (s)",
			"parent_id", "Catégorie",
			"bitrate", "Débit audio",
			"user_limit", "Limite d'utilisateurs",
			"rtc_region", "Région",
			"type", "Type"
	);

	// Changements trop techniques ou déjà journalisés ailleurs (permissions : ChannelPermissionAuditListener)
	private static final Set<String> IGNORED = Set.of("permission_overwrites", "position", "id", "flags");

	public ChannelAuditListener(Bot bot) {
		super(bot, Set.of(ActionType.CHANNEL_CREATE, ActionType.CHANNEL_UPDATE, ActionType.CHANNEL_DELETE));
	}

	@Override
	protected void handle(@NotNull AuditLogEntry entry) {
		String channel = "<#" + entry.getTargetId() + ">";

		switch (entry.getType()) {
			case CHANNEL_CREATE -> sendLog(LogChannel.CHANNEL_LOG_CHANNEL, "# ➕ Salon créé", 0x00FF00,
					"Salon : " + channel
							+ "\nNom : " + AuditLogFormatter.value(newValue(entry, "name"))
							+ "\nType : " + AuditLogFormatter.channelType(newValue(entry, "type"))
							+ parent(createdParentId(entry))
							+ footer(entry));
			case CHANNEL_DELETE -> sendLog(LogChannel.CHANNEL_LOG_CHANNEL, "# ➖ Salon supprimé", 0xFF0000,
					"Nom : " + AuditLogFormatter.value(oldValue(entry, "name"))
							+ "\nType : " + AuditLogFormatter.channelType(oldValue(entry, "type"))
							+ parent(oldValue(entry, "parent_id"))
							+ footer(entry));
			case CHANNEL_UPDATE -> {
				List<String> changes = describeChanges(entry);
				if (changes.isEmpty()) {
					return;
				}

				sendLog(LogChannel.CHANNEL_LOG_CHANNEL, "# ✏️ Salon modifié", 0xFFFF00,
						"Salon : " + channel + "\n\n" + String.join("\n", changes) + footer(entry));
			}
			default -> {
			}
		}
	}

	private List<String> describeChanges(AuditLogEntry entry) {
		List<String> lines = new ArrayList<>();

		for (AuditLogChange change : entry.getChanges().values()) {
			String key = change.getKey();
			if (IGNORED.contains(key)) {
				continue;
			}

			Object before = change.getOldValue();
			Object after = change.getNewValue();
			if ("parent_id".equals(key)) {
				before = before == null ? null : "<#" + before + ">";
				after = after == null ? null : "<#" + after + ">";
			} else if ("type".equals(key)) {
				before = AuditLogFormatter.channelType(before);
				after = AuditLogFormatter.channelType(after);
			}

			lines.add(LABELS.getOrDefault(key, key) + " : " + AuditLogFormatter.change(before, after));
		}

		return lines;
	}

	/**
	 * Discord ne met pas la catégorie dans l'entrée d'audit d'une création : on la lit dans le cache JDA.
	 */
	private static Object createdParentId(AuditLogEntry entry) {
		GuildChannel channel = entry.getGuild().getGuildChannelById(entry.getTargetIdLong());
		if (channel instanceof ICategorizableChannel categorizable && categorizable.getParentCategoryIdLong() != 0) {
			return categorizable.getParentCategoryId();
		}
		return newValue(entry, "parent_id");
	}

	private static String parent(Object parentId) {
		return parentId == null ? "" : "\nCatégorie : <#" + parentId + ">";
	}
}
