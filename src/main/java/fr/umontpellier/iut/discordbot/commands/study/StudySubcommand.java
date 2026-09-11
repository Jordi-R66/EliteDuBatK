package fr.umontpellier.iut.discordbot.commands.study;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.lib.SharedBot;
import fr.umontpellier.iut.discordbot.studysuite.StudySuiteException;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Une sous-commande de {@code /study}. Elles ne sont pas découvertes par réflexion comme les commandes : c'est
 * {@link StudyCommand} qui les liste.
 */
public abstract class StudySubcommand extends SharedBot {
    /** Les appels à StudySuite sont bloquants : ils ne doivent pas occuper le thread d'événements de JDA. */
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected StudySubcommand(@NotNull Bot bot) {
        super(bot);
    }

    public abstract @NotNull SubcommandData getData();

    public abstract void execute(SlashCommandInteractionEvent event);

    public void autocomplete(CommandAutoCompleteInteractionEvent event) {
        event.replyChoices(List.of()).queue();
    }

    public String getName() {
        return getData().getName();
    }

    /** Une erreur à montrer telle quelle, à la seule personne qui a lancé la commande. */
    protected static class UserFacingException extends RuntimeException {
        public UserFacingException(String message) {
            super(message);
        }
    }

    /**
     * Répond plus tard : l'interaction est différée (Discord n'attend que 3 s), puis {@code task} tourne hors du
     * thread de JDA.
     * <p>
     * Une {@link UserFacingException} ou une panne de StudySuite remplace la réponse par un message que seul
     * l'auteur voit, même si la réponse prévue était publique.
     */
    protected void replyLater(SlashCommandInteractionEvent event, boolean ephemeral, EmbedTask task) {
        event.deferReply(ephemeral).queue(hook -> EXECUTOR.execute(() -> {
            try {
                hook.editOriginalEmbeds(task.run()).queue();
            } catch (UserFacingException e) {
                replyError(hook, ephemeral, e.getMessage());
            } catch (StudySuiteException e) {
                logger.warn("StudySuite call failed: {}", e.getMessage());
                replyError(hook, ephemeral, "StudySuite ne répond pas pour l'instant, réessaie dans un moment.");
            } catch (RuntimeException e) {
                logger.error("Unexpected error in /study {}", getName(), e);
                replyError(hook, ephemeral, "Une erreur inattendue est survenue.");
            }
        }));
    }

    /** Lance {@code task} hors du thread de JDA (autocomplétion qui a besoin de StudySuite). */
    protected void async(Runnable task) {
        EXECUTOR.execute(() -> {
            try {
                task.run();
            } catch (RuntimeException e) {
                logger.warn("Background task of /study {} failed: {}", getName(), e.getMessage());
            }
        });
    }

    private void replyError(InteractionHook hook, boolean ephemeral, String message) {
        if (ephemeral) {
            hook.editOriginal("❌ " + message).queue();
            return;
        }
        // Le message différé est public : on le retire pour répondre en privé.
        hook.deleteOriginal().queue(ok -> hook.sendMessage("❌ " + message).setEphemeral(true).queue());
    }

    @FunctionalInterface
    protected interface EmbedTask {
        MessageEmbed run();
    }
}
