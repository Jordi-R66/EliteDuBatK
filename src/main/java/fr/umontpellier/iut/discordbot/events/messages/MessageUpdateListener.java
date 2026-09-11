package fr.umontpellier.iut.discordbot.events.messages;

import java.time.ZoneOffset;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.lib.AbstractEventListener;
import fr.umontpellier.iut.discordbot.lib.CachedMessage;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;

/**
 * Met à jour le cache quand un message est modifié, pour que le log de suppression affiche la dernière version.
 */
public class MessageUpdateListener extends AbstractEventListener {
	public MessageUpdateListener(Bot bot) {
		super(bot);
	}

	@Override
	public void onMessageUpdate(MessageUpdateEvent event) {
		if (!event.isFromGuild() || event.getAuthor().isBot()) {
			return;
		}

		Message msg = event.getMessage();

		getBot().getCachedMessages().put(event.getMessageId(), new CachedMessage(
				event.getAuthor().getId(),
				msg.getContentRaw(),
				msg.getTimeCreated().atZoneSameInstant(ZoneOffset.UTC)
		));
	}
}
