package fr.umontpellier.iut.discordbot.events;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.config.ConfigStructure.SystemChannel;
import fr.umontpellier.iut.discordbot.lib.AbstractEventListener;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

/**
 * Départ d'un membre. Discord envoie aussi cet événement pour les expulsions et bannissements, qui sont en plus
 * journalisés avec leur auteur dans le salon de modération.
 */
public class MemberLeaveListener extends AbstractEventListener {
	public MemberLeaveListener(Bot bot) {
		super(bot);
	}

	@Override
	public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
		User user = event.getUser();
		// Le membre n'est connu que s'il était en cache ; sinon, pas de date d'arrivée
		Member member = event.getMember();

		StringBuilder details = new StringBuilder()
				.append("Membre : ").append(user.getAsMention()).append(" (").append(user.getName()).append(')')
				.append("\nCompte créé : ").append(TimeFormat.DATE_SHORT.format(user.getTimeCreated()));
		if (member != null) {
			details.append("\nArrivé : ").append(TimeFormat.DATE_SHORT.format(member.getTimeJoined()));
		}
		details.append("\nDate : ").append(TimeFormat.DATE_TIME_SHORT.now())
				.append("\n-# Départ volontaire, expulsion ou bannissement : voir le salon de modération.");

		logger.info("\"{}\" left the server", user.getName());
		getBot().getLogSender().sendLog(SystemChannel.MEMBER_LOG_CHANNEL, "# 👋 Départ d'un membre", 0xFF0000, details.toString());
	}
}
