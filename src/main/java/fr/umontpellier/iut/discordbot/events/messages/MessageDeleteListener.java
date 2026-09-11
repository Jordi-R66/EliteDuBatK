package fr.umontpellier.iut.discordbot.events.messages;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.lib.AbstractEventListener;
import fr.umontpellier.iut.discordbot.lib.CachedMessage;
import fr.umontpellier.iut.discordbot.lib.DeleteLogFormatter;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.utils.TimeFormat;

public class MessageDeleteListener extends AbstractEventListener {
	public MessageDeleteListener(Bot bot) {
		super(bot);
	}

	@Override
	public void onMessageDelete(MessageDeleteEvent event) {
		if (!event.isFromGuild()) {
			return;
		}

		Bot bot = getBot();
		String logChannelId = bot.getConfig().get().getMessageDeleteChannelId();
		if (logChannelId == null || logChannelId.isBlank()) {
			logger.warn("No message delete log channel configured; skipping Discord log message");
			return;
		}

		CachedMessage cached = bot.getCachedMessages().remove(event.getMessageId());

		// Ne pas journaliser la suppression des logs eux-mêmes
		if (event.getChannel().getId().equals(logChannelId)) {
			return;
		}

		String channelMention = event.getChannel().getAsMention();
		String deletedAt = TimeFormat.DATE_TIME_SHORT.now().toString();

		String details;
		if (cached != null) {
			logger.info("Message {} from {} deleted in \"{}\"", event.getMessageId(), cached.getSenderId(), event.getChannel().getName());
			details = String.format("Auteur: <@%s>\nSalon: %s\nEnvoyé: %s\nSupprimé: %s\n\n%s",
					cached.getSenderId(),
					channelMention,
					TimeFormat.DATE_TIME_SHORT.format(cached.getReceptionTime()),
					deletedAt,
					DeleteLogFormatter.quote(cached.getContent()));
		} else {
			logger.info("Uncached message {} deleted in \"{}\"", event.getMessageId(), event.getChannel().getName());
			details = String.format("Salon: %s\nSupprimé: %s\n\n*Contenu inconnu : message absent du cache.*",
					channelMention,
					deletedAt);
		}

		bot.getLogSender().sendComponentToChannelId(logChannelId,
				Container.of(
						TextDisplay.of("# 🗑️ Message supprimé"),
						Separator.createDivider(Separator.Spacing.SMALL),
						TextDisplay.of(DeleteLogFormatter.truncate(details, DeleteLogFormatter.MAX_TEXT_LENGTH))
				).withAccentColor(0xFF8800)
		);
	}
}
