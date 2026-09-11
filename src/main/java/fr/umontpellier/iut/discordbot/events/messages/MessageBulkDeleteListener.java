package fr.umontpellier.iut.discordbot.events.messages;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.lib.AbstractEventListener;
import fr.umontpellier.iut.discordbot.lib.CachedMessage;
import fr.umontpellier.iut.discordbot.lib.DeleteLogFormatter;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.utils.TimeFormat;

public class MessageBulkDeleteListener extends AbstractEventListener {
	public MessageBulkDeleteListener(Bot bot) {
		super(bot);
	}

	@Override
	public void onMessageBulkDelete(MessageBulkDeleteEvent event) {
		Bot bot = getBot();
		String logChannelId = bot.getConfig().get().getMessageDeleteChannelId();
		if (logChannelId == null || logChannelId.isBlank()) {
			logger.warn("No message delete log channel configured; skipping Discord log message");
			return;
		}

		StringBuilder messages = new StringBuilder();
		int uncached = 0;
		for (String messageId : event.getMessageIds()) {
			CachedMessage cached = bot.getCachedMessages().remove(messageId);
			if (cached == null) {
				uncached++;
				continue;
			}

			messages.append(String.format("\n\n<@%s> (%s)\n%s",
					cached.getSenderId(),
					TimeFormat.DATE_TIME_SHORT.format(cached.getReceptionTime()),
					DeleteLogFormatter.quote(cached.getContent())));
		}

		if (event.getChannel().getId().equals(logChannelId)) {
			return;
		}

		logger.info("{} messages bulk deleted in \"{}\"", event.getMessageIds().size(), event.getChannel().getName());

		String details = String.format("Salon: %s\nSupprimé: %s\nMessages: %d (dont %d absents du cache)%s",
				event.getChannel().getAsMention(),
				TimeFormat.DATE_TIME_SHORT.now().toString(),
				event.getMessageIds().size(),
				uncached,
				messages);

		bot.getLogSender().sendComponentToChannelId(logChannelId,
				Container.of(
						TextDisplay.of("# 🗑️ Suppression en masse"),
						Separator.createDivider(Separator.Spacing.SMALL),
						TextDisplay.of(DeleteLogFormatter.truncate(details, DeleteLogFormatter.MAX_TEXT_LENGTH))
				).withAccentColor(0xFF8800)
		);
	}
}
