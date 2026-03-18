package fr.umontpellier.iut.discordbot.events;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.lib.AbstractEventListener;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import org.jetbrains.annotations.NotNull;

public class SlashCommandAutocompleteEventListener extends AbstractEventListener {
    public SlashCommandAutocompleteEventListener(Bot bot) {
        super(bot);
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        this.getBot()
                .getCommandManager()
                .getCommands()
                .stream()
                .filter(command -> command
                        .getCommandInformation()
                        .getName()
                        .equals(event.getName())
                )
                .flatMap(abstractCommand -> abstractCommand.asAutocompleteCommand().stream())
                .findFirst()
                .ifPresent(command -> command.autocomplete(event));
    }
}
