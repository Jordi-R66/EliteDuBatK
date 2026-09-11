package fr.umontpellier.iut.discordbot.events.audit;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.config.ConfigStructure.LogChannel;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogChange;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Sanctions : bannissement, expulsion et exclusion temporaire (ainsi que leur levée).
 */
public class ModerationAuditListener extends AbstractAuditLogListener {
	public ModerationAuditListener(Bot bot) {
		super(bot, Set.of(ActionType.BAN, ActionType.UNBAN, ActionType.KICK, ActionType.MEMBER_UPDATE));
	}

	@Override
	protected void handle(@NotNull AuditLogEntry entry) {
		String member = "Membre : <@" + entry.getTargetId() + "> (" + entry.getTargetId() + ")";

		switch (entry.getType()) {
			case BAN -> sendLog(LogChannel.MODERATION_CHANNEL, "# 🔨 Bannissement", 0x8B0000, member + footer(entry));
			case UNBAN -> sendLog(LogChannel.MODERATION_CHANNEL, "# 🔓 Débannissement", 0x00FF00, member + footer(entry));
			case KICK -> sendLog(LogChannel.MODERATION_CHANNEL, "# 👢 Expulsion", 0xFF8800, member + footer(entry));
			case MEMBER_UPDATE -> handleTimeout(entry, member);
			default -> {
			}
		}
	}

	private void handleTimeout(AuditLogEntry entry, String member) {
		AuditLogChange timeout = entry.getChangeByKey("communication_disabled_until");
		if (timeout == null) {
			return;
		}

		if (timeout.getNewValue() == null) {
			sendLog(LogChannel.MODERATION_CHANNEL, "# ⏱️ Fin d'exclusion temporaire", 0x00FF00, member + footer(entry));
			return;
		}

		OffsetDateTime until = OffsetDateTime.parse(timeout.getNewValue().toString());
		sendLog(LogChannel.MODERATION_CHANNEL, "# ⏱️ Exclusion temporaire", 0x9B59B6,
				member
						+ "\nJusqu'au : " + TimeFormat.DATE_TIME_SHORT.format(until)
						+ " (" + TimeFormat.RELATIVE.format(until) + ")"
						+ footer(entry));
	}
}
