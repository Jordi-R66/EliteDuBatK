package fr.umontpellier.iut.discordbot.events.audit;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.config.ConfigStructure;
import fr.umontpellier.iut.discordbot.lib.AbstractEventListener;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogChange;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.events.guild.GuildAuditLogEntryCreateEvent;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Base des listeners qui journalisent des entrées du journal d'audit Discord.
 * <p>
 * Dans le package {@code events} (et non {@code lib}) pour que l'{@code EventManager} trouve ses sous-classes.
 * Nécessite l'intent GUILD_MODERATION et la permission « Voir les logs du serveur ».
 */
public abstract class AbstractAuditLogListener extends AbstractEventListener {
	private final Set<ActionType> actionTypes;

	protected AbstractAuditLogListener(Bot bot, Set<ActionType> actionTypes) {
		super(bot);
		this.actionTypes = actionTypes;
	}

	@Override
	public final void onGuildAuditLogEntryCreate(@NotNull GuildAuditLogEntryCreateEvent event) {
		AuditLogEntry entry = event.getEntry();

		if (!actionTypes.contains(entry.getType())) {
			return;
		}

		// Les actions du bot (ex. /lock) ont déjà leurs propres logs
		if (entry.getUserIdLong() == event.getJDA().getSelfUser().getIdLong()) {
			return;
		}

		try {
			handle(entry);
		} catch (Exception e) {
			logger.error("Failed to log audit entry {} of type {}", entry.getId(), entry.getType(), e);
		}
	}

	protected abstract void handle(@NotNull AuditLogEntry entry);

	protected void sendLog(ConfigStructure.SystemChannel channel, String title, int accentColor, String details) {
		getBot().getLogSender().sendLog(channel, title, accentColor, details);
	}

	/**
	 * Lignes communes à tous les logs d'audit : auteur, raison (si donnée) et date.
	 */
	@NotNull
	protected String footer(@NotNull AuditLogEntry entry) {
		StringBuilder footer = new StringBuilder("\n\nPar : <@").append(entry.getUserId()).append('>');
		if (entry.getReason() != null && !entry.getReason().isBlank()) {
			footer.append("\nRaison : ").append(entry.getReason());
		}
		footer.append("\nDate : ").append(TimeFormat.DATE_TIME_SHORT.atTimestamp(entry.getTimeCreated().toInstant().toEpochMilli()));
		return footer.toString();
	}

	@Nullable
	protected static Object newValue(@NotNull AuditLogEntry entry, @NotNull String key) {
		AuditLogChange change = entry.getChangeByKey(key);
		return change == null ? null : change.getNewValue();
	}

	@Nullable
	protected static Object oldValue(@NotNull AuditLogEntry entry, @NotNull String key) {
		AuditLogChange change = entry.getChangeByKey(key);
		return change == null ? null : change.getOldValue();
	}
}
