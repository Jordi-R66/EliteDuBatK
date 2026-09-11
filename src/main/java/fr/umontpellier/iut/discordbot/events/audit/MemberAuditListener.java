package fr.umontpellier.iut.discordbot.events.audit;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.config.ConfigStructure.LogChannel;
import fr.umontpellier.iut.discordbot.lib.AuditLogFormatter;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogChange;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Rôles et pseudo des membres. Les exclusions temporaires sont dans {@link ModerationAuditListener}.
 */
public class MemberAuditListener extends AbstractAuditLogListener {
	public MemberAuditListener(Bot bot) {
		super(bot, Set.of(ActionType.MEMBER_ROLE_UPDATE, ActionType.MEMBER_UPDATE));
	}

	@Override
	protected void handle(@NotNull AuditLogEntry entry) {
		String member = "<@" + entry.getTargetId() + ">";

		if (entry.getType() == ActionType.MEMBER_ROLE_UPDATE) {
			StringBuilder details = new StringBuilder("Membre : ").append(member).append('\n');
			if (newValue(entry, "$add") != null) {
				details.append("\nAjoutés : ").append(AuditLogFormatter.roleMentions(newValue(entry, "$add")));
			}
			if (newValue(entry, "$remove") != null) {
				details.append("\nRetirés : ").append(AuditLogFormatter.roleMentions(newValue(entry, "$remove")));
			}

			sendLog(LogChannel.MEMBER_LOG_CHANNEL, "# 🏷️ Rôles modifiés", 0xFFFF00,
					details + footer(entry));
			return;
		}

		AuditLogChange nick = entry.getChangeByKey("nick");
		if (nick == null) {
			return;
		}

		String by = entry.getUserId().equals(entry.getTargetId()) ? "\n*(modifié par le membre lui-même)*" : "";
		sendLog(LogChannel.MEMBER_LOG_CHANNEL, "# 📝 Pseudo modifié", 0xFFFF00,
				"Membre : " + member
						+ "\nPseudo : " + AuditLogFormatter.change(nick.getOldValue(), nick.getNewValue())
						+ by
						+ footer(entry));
	}
}
