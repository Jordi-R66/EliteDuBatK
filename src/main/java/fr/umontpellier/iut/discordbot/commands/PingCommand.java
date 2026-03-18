package fr.umontpellier.iut.discordbot.commands;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.lib.AbstractCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PingCommand extends AbstractCommand {

	public PingCommand(Bot bot) {
		super(bot);
	}

	@NotNull
	@Override
	public SlashCommandData getCommandInformation() {
		return Commands.slash("ping", "Répond avec pong")
				.addOption(OptionType.BOOLEAN, "ephemeral", "Rendre la réponse éphémère (visible uniquement par vous)", false);
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		if (event.getOption("ephemeral") != null && Objects.requireNonNull(event.getOption("ephemeral")).getAsBoolean()) {
			event.reply("Pong!").setEphemeral(true).queue();
		} else {
			event.reply("Pong!").queue();
		}
	}
}
