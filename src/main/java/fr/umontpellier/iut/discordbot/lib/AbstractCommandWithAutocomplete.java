package fr.umontpellier.iut.discordbot.lib;

import fr.umontpellier.iut.discordbot.Bot;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;

import java.util.Optional;

public abstract class AbstractCommandWithAutocomplete extends AbstractCommand {
	public AbstractCommandWithAutocomplete(Bot bot) {
		super(bot);
	}

	@Override
	public boolean hasAutocomplete() {
		return true;
	}

	@Override
	public Optional<AbstractCommandWithAutocomplete> asAutocompleteCommand() {
		return Optional.of(this);
	}

	public abstract void autocomplete(CommandAutoCompleteInteractionEvent event);
}
