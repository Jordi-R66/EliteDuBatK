package fr.umontpellier.iut.discordbot.events.messages;

import java.time.ZoneOffset;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.lib.AbstractEventListener;
import fr.umontpellier.iut.discordbot.lib.CachedMessage;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class MessageReceivedListener extends AbstractEventListener {
	public MessageReceivedListener(Bot bot) {
		super(bot);
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
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
