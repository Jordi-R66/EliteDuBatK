package fr.umontpellier.iut.discordbot.events.audit;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.config.ConfigStructure.SystemChannel;
import fr.umontpellier.iut.discordbot.lib.AuditLogFormatter;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.audit.AuditLogOption;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Permissions d'un salon accordées ou retirées à un rôle ou à un membre.
 */
public class ChannelPermissionAuditListener extends AbstractAuditLogListener {
	public ChannelPermissionAuditListener(Bot bot) {
		super(bot, Set.of(ActionType.CHANNEL_OVERRIDE_CREATE, ActionType.CHANNEL_OVERRIDE_UPDATE, ActionType.CHANNEL_OVERRIDE_DELETE));
	}

	@Override
	protected void handle(@NotNull AuditLogEntry entry) {
		String title = switch (entry.getType()) {
			case CHANNEL_OVERRIDE_CREATE -> "# 🔐 Permissions ajoutées sur un salon";
			case CHANNEL_OVERRIDE_DELETE -> "# 🔐 Permissions retirées d'un salon";
			default -> "# 🔐 Permissions d'un salon modifiées";
		};

		long oldAllow = AuditLogFormatter.toLong(oldValue(entry, "allow"));
		long oldDeny = AuditLogFormatter.toLong(oldValue(entry, "deny"));
		long newAllow = AuditLogFormatter.toLong(newValue(entry, "allow"));
		long newDeny = AuditLogFormatter.toLong(newValue(entry, "deny"));

		// L'interface de Discord crée d'abord une surcharge vide, puis la modifie : seule la modification est utile
		if (oldAllow == newAllow && oldDeny == newDeny) {
			return;
		}

		String diff = AuditLogFormatter.overrideDiff(oldAllow, oldDeny, newAllow, newDeny);

		sendLog(SystemChannel.CHANNEL_LOG_CHANNEL, title, 0xFFFF00,
				"Salon : <#" + entry.getTargetId() + ">"
						+ "\nPour : " + holder(entry)
						+ "\n\n" + diff
						+ "\n-# ✅ autorisée, ❌ refusée, ➖ héritée"
						+ footer(entry));
	}

	/**
	 * Rôle ou membre concerné par la surcharge.
	 */
	private static String holder(AuditLogEntry entry) {
		Object id = entry.getOption(AuditLogOption.ID);
		Object type = entry.getOption(AuditLogOption.TYPE);
		if (id == null) {
			return AuditLogFormatter.NONE;
		}

		// Discord envoie "0" pour un rôle et "1" pour un membre
		boolean isMember = "1".equals(String.valueOf(type)) || "member".equals(String.valueOf(type));
		if (isMember) {
			return "<@" + id + "> (membre)";
		}

		// @everyone a l'ID du serveur ; sa mention <@&id> ne s'affiche pas correctement
		if (id.toString().equals(entry.getGuild().getId())) {
			return "@everyone";
		}
		return "<@&" + id + "> (rôle)";
	}
}
