package fr.umontpellier.iut.discordbot.events;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.lib.AbstractEventListener;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;

/** Transmet un bouton à la commande qui l'a envoyé (identifiant {@code <commande>:…}). */
public class ButtonEventListener extends AbstractEventListener {
	public ButtonEventListener(Bot bot) {
		super(bot);
	}

	@Override
	public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
		String commandName = event.getComponentId().split(":", 2)[0];

		this.getBot()
				.getCommandManager()
				.getCommands()
				.stream()
				.filter(command -> command.getCommandInformation().getName().equals(commandName))
				.findFirst()
				.ifPresentOrElse(
						command -> command.onButton(event),
						() -> event.reply("Ce bouton n'est plus actif.").setEphemeral(true).queue()
				);
	}
}
