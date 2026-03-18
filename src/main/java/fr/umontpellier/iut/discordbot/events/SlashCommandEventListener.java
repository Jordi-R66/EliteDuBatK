package fr.umontpellier.iut.discordbot.events;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.lib.AbstractEventListener;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

public class SlashCommandEventListener extends AbstractEventListener {
    public SlashCommandEventListener(Bot bot) {
        super(bot);
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        this.getBot()
                .getCommandManager()
                .getCommands()
                .stream()
                .filter(command -> command
                        .getCommandInformation()
                        .getName()
                        .equals(event.getName())
                ).findFirst()
                .ifPresentOrElse(
                        command -> command.execute(event),
                        () -> event.reply("Commande inconnue...").queue()
                );
    }
}
