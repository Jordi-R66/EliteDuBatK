package fr.umontpellier.iut.discordbot.events;

import fr.umontpellier.iut.discordbot.Bot;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import fr.umontpellier.iut.discordbot.events.messages.MessageReceivedListener;

import fr.umontpellier.iut.discordbot.config.ConfigStructure;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import java.util.concurrent.TimeUnit;

public class HoneypotListener extends MessageReceivedListener {

	public HoneypotListener(Bot bot) {
		super(bot);
	}

	@SuppressWarnings("null")
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		super.onMessageReceived(event);

		boolean isHoneypot = false;
		boolean isUser = false;
		boolean hasPermission = false;

		Member member = event.getMember();
		Guild guild = event.getGuild();
		ConfigStructure config = getBot().getConfig().get();
		String honeypotId = null;

		boolean hasEssentials = member != null && guild != null && config != null;

		if (hasEssentials) {
			honeypotId = config.getChannelId(ConfigStructure.SystemChannel.HONEYPOT_CHANNEL);
		}

		boolean hasHoneypotId = honeypotId != null;

		if (hasEssentials && hasHoneypotId) {
			isHoneypot = honeypotId.equals(event.getChannel().getId());
		}

		if (isHoneypot) {
			isUser = !event.getAuthor().isBot();
		}

		if (isHoneypot && isUser) {
			hasPermission = guild.getSelfMember().canInteract(member);
		}

		if (isHoneypot && isUser) {
			String message_title = "# 🍯 CRITIQUE 🚨 : Honeypot déclenché";
			@SuppressWarnings("null")
			String message_body = "Honeypot déclenché par " + member.getAsMention()
					+ " mais il est impossible de bannir son compte !\nVeuillez le faire manuellement.";

			if (hasPermission) {
				message_title = "# 🍯 Honeypot déclenché";
				message_body = "Honeypot déclenché par " + member.getAsMention()
						+ ". Compte banni et derniers messages effacés.";
			}

			getBot().getLogSender().sendLog(
					ConfigStructure.SystemChannel.MODERATION_CHANNEL,
					message_title,
					0xFF0000,
					message_body);

			if (hasPermission) {
				enforceHoneypotBan(guild, member);
			}
		}
	}

	@SuppressWarnings("null")
	private void enforceHoneypotBan(Guild guild, Member member) {
		boolean canExecute = false;
		String banReason = "Pris(e) dans le honeypot";
		int deletionTime = 1;
		TimeUnit timeUnit = TimeUnit.SECONDS;

		boolean hasValidParameters = guild != null && member != null;

		if (hasValidParameters) {
			canExecute = guild.getSelfMember().canInteract(member);
		}

		if (canExecute) {
			guild.ban(member, deletionTime, timeUnit).reason(banReason).queue();
		}
	}
}