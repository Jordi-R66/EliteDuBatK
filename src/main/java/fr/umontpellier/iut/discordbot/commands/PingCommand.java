package fr.umontpellier.iut.discordbot.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.lib.AbstractCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
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
				.addOption(OptionType.BOOLEAN, "ephemeral", "Rendre la réponse éphémère (visible uniquement par vous)",
						false)
				.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR));
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		Member member = Objects.requireNonNull(event.getMember());

		if (member.hasPermission(Permission.ADMINISTRATOR)) {
			if (event.getOption("ephemeral") != null
					&& Objects.requireNonNull(event.getOption("ephemeral")).getAsBoolean()) {
				event.reply("Pong!").setEphemeral(true).queue();
			} else {
				event.reply("Pong!").queue();
			}
		} else {
			event.reply("T'as pas le droit de faire cette commande.").setEphemeral(true).queue();
		}
	}
}
