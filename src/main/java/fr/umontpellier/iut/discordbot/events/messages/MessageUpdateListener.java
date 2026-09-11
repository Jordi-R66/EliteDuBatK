package fr.umontpellier.iut.discordbot.events.messages;

import java.time.ZoneOffset;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.config.ConfigStructure.LogChannel;
import fr.umontpellier.iut.discordbot.lib.AbstractEventListener;
import fr.umontpellier.iut.discordbot.lib.CachedMessage;
import fr.umontpellier.iut.discordbot.lib.DeleteLogFormatter;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.utils.TimeFormat;

/**
 * Journalise les modifications de message et met à jour le cache, pour que le log de suppression affiche la dernière
 * version.
 */
public class MessageUpdateListener extends AbstractEventListener {
	// Ancien et nouveau contenu partagent la limite de texte du log
	private static final int MAX_CONTENT_LENGTH = 1700;

	public MessageUpdateListener(Bot bot) {
		super(bot);
	}

	@Override
	public void onMessageUpdate(MessageUpdateEvent event) {
		if (!event.isFromGuild() || event.getAuthor().isBot()) {
			return;
		}

		Message msg = event.getMessage();
		String newContent = msg.getContentRaw();

		CachedMessage previous = getBot().getCachedMessages().put(event.getMessageId(), new CachedMessage(
				event.getAuthor().getId(),
				newContent,
				msg.getTimeCreated().atZoneSameInstant(ZoneOffset.UTC)
		));

		// Discord envoie aussi cet événement quand seuls les embeds ou l'épinglage changent
		if (previous != null && previous.getContent().equals(newContent)) {
			return;
		}

		String before = previous != null
				? quote(previous.getContent())
				: "*Contenu inconnu : message absent du cache.*";

		String details = String.format("Auteur: %s\nSalon: %s\nEnvoyé: %s\nModifié: %s\n[Aller au message](%s)\n\n**Avant**\n%s\n\n**Après**\n%s",
				event.getAuthor().getAsMention(),
				event.getChannel().getAsMention(),
				TimeFormat.DATE_TIME_SHORT.format(msg.getTimeCreated()),
				TimeFormat.DATE_TIME_SHORT.now(),
				msg.getJumpUrl(),
				before,
				quote(newContent));

		logger.info("Message {} from {} edited in \"{}\"", event.getMessageId(), event.getAuthor().getId(), event.getChannel().getName());
		getBot().getLogSender().sendLog(LogChannel.MESSAGE_EDIT_CHANNEL, "# ✏️ Message modifié", 0xFFFF00, details);
	}

	private static String quote(String content) {
		return DeleteLogFormatter.quote(DeleteLogFormatter.truncate(content, MAX_CONTENT_LENGTH));
	}
}
