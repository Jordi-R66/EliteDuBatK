package fr.umontpellier.iut.discordbot.events;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.config.ConfigStructure;
import fr.umontpellier.iut.discordbot.lib.AbstractEventListener;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;

import java.util.List;

import javax.annotation.Nonnull;

public class GuildWhitelistListener extends AbstractEventListener {

	public GuildWhitelistListener(Bot bot) {
		super(bot);
	}

	@Override
	public void onGuildJoin(@Nonnull GuildJoinEvent event) {
		boolean shouldLeave = true;
		Guild guild = event.getGuild();
		ConfigStructure config = getBot().getConfig().get();
		String allowedGuildId = null;

		boolean hasConfig = config != null;

		if (hasConfig) {
			allowedGuildId = config.getAllowedGuildId();
		}

		boolean hasAllowedGuild = allowedGuildId != null;

		if (hasAllowedGuild) {
			boolean isAllowed = allowedGuildId.equals(guild.getId());
			if (isAllowed) {
				shouldLeave = false;
			}
		}

		if (shouldLeave) {
			guild.leave().queue();
		}
	}

	@Override
	public void onReady(@Nonnull ReadyEvent event) {
		ConfigStructure config = getBot().getConfig().get();
		String allowedGuildId = null;
		List<Guild> guilds = event.getJDA().getGuilds();

		boolean hasConfig = config != null;

		if (hasConfig) {
			allowedGuildId = config.getAllowedGuildId();
		}

		boolean hasAllowedGuild = allowedGuildId != null;

		if (hasAllowedGuild) {
			for (Guild guild : guilds) {
				boolean shouldLeave = true;
				boolean isAllowed = allowedGuildId.equals(guild.getId());

				if (isAllowed) {
					shouldLeave = false;
				}

				if (shouldLeave) {
					guild.leave().queue();
				}
			}
		}
	}
}