package fr.umontpellier.iut.discordbot.lib;

import fr.umontpellier.iut.discordbot.Bot;
import org.jetbrains.annotations.NotNull;

public class SharedBot implements ISharedBot {
	private final Bot bot;

	public SharedBot(@NotNull Bot bot) {
		this.bot = bot;
	}

	public @NotNull Bot getBot() {
		return this.bot;
	}
}
