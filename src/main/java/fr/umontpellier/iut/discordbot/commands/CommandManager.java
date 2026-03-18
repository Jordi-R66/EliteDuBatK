package fr.umontpellier.iut.discordbot.commands;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.lib.AbstractCommand;
import fr.umontpellier.iut.discordbot.lib.ISharedBot;
import fr.umontpellier.iut.discordbot.lib.ObjectManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CommandManager extends ObjectManager<AbstractCommand> implements ISharedBot {
	private final Bot bot;

	public CommandManager(Bot bot) {
		super("fr.umontpellier.iut.discordbot.commands", AbstractCommand.class, new Object[]{bot}, Bot.class);
		this.bot = bot;
	}

	public List<AbstractCommand> getCommands() {
		return super.get();
	}

	public void registerCommands() {
		bot.getJda().updateCommands().addCommands(
				this.get().stream().map(AbstractCommand::getCommandInformation).toList()
		).queue();
	}

	@NotNull
	@Override
	public Bot getBot() {
		return this.bot;
	}
}
