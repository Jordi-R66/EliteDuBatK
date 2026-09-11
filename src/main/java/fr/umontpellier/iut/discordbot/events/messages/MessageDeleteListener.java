package fr.umontpellier.iut.discordbot.events.messages;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.lib.AbstractEventListener;
import fr.umontpellier.iut.discordbot.lib.CachedMessage;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.utils.TimeFormat;

public class MessageDeleteListener extends AbstractEventListener {
	// Discord limite le texte total d'un message à composants à 4000 caractères
	private static final int MAX_CONTENT_LENGTH = 3000;

	public MessageDeleteListener(Bot bot) {
		super(bot);
	}

	@Override
	public void onMessageDelete(MessageDeleteEvent event) {
		if (!event.isFromGuild()) {
			return;
		}

		Bot bot = getBot();
		CachedMessage cached = bot.getCachedMessages().remove(event.getMessageId());
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
					formatContent(cached.getContent()));
		} else {
			logger.info("Uncached message {} deleted in \"{}\"", event.getMessageId(), event.getChannel().getName());
			details = String.format("Salon: %s\nSupprimé: %s\n\n*Message non présent dans le cache (envoyé avant le démarrage du bot ou trop ancien).*",
					channelMention,
					deletedAt);
		}

		sendDeleteLog(
				Container.of(
						TextDisplay.of("# 🗑️ Message supprimé"),
						Separator.createDivider(Separator.Spacing.SMALL),
						TextDisplay.of(details)
				).withAccentColor(0xFF8800)
		);
	}

	private String formatContent(String content) {
		if (content.isBlank()) {
			return "*(aucun contenu texte)*";
		}

		if (content.length() > MAX_CONTENT_LENGTH) {
			content = content.substring(0, MAX_CONTENT_LENGTH) + "…";
		}

		return "> " + content.replace("\n", "\n> ");
	}

	private void sendDeleteLog(Container container) {
		String channelId = getBot().getConfig().get().getMessageDeleteChannelId();
		if (channelId == null || channelId.isBlank()) {
			logger.warn("No message delete log channel configured; skipping Discord log message");
			return;
		}

		getBot().getLogSender().sendComponentToChannelId(channelId, container);
	}
}
