package fr.umontpellier.iut.discordbot.commands.study;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.studysuite.HomeworkFormatter;
import fr.umontpellier.iut.discordbot.studysuite.StudySuiteClient;
import fr.umontpellier.iut.discordbot.studysuite.model.Assignment;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;

/**
 * {@code /study devoirs [passes]} : les devoirs du membre, d'après son compte StudySuite, chacun avec un bouton pour
 * le cocher. Toujours en privé : c'est sa liste à lui.
 * <p>
 * Les boutons ({@code study:devoirs:<done|undo>:<devoir>:<contexte>}) servent aussi aux annonces de
 * {@link AddHomeworkSubcommand}. Le contexte dit quoi faire après : dans une liste, c'est le début de la période
 * affichée, pour la réafficher à jour ; sur une annonce ({@value #ANNOUNCEMENT}), le message est public et le même
 * pour tous, on confirme donc en privé à celui qui a cliqué.
 */
public class HomeworkSubcommand extends StudySubcommand {
    static final String NAME = "devoirs";
    static final String ANNOUNCEMENT = "a";
    private static final String DONE = "done";
    private static final String UNDO = "undo";
    private static final String OPTION_PAST = "passes";
    /** Avec {@code passes}, jusqu'où on regarde en arrière. */
    private static final Duration PAST_WINDOW = Duration.ofDays(14);

    public HomeworkSubcommand(@NotNull Bot bot) {
        super(bot);
    }

    @NotNull
    @Override
    public SubcommandData getData() {
        return new SubcommandData(NAME, "Voir tes devoirs et cocher ceux qui sont faits")
                .addOptions(new OptionData(OptionType.BOOLEAN, OPTION_PAST, "Inclure ceux des deux dernières semaines"));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        boolean past = event.getOption(OPTION_PAST, false, OptionMapping::getAsBoolean);
        Instant from = past ? Instant.now().minus(PAST_WINDOW) : Instant.now();
        String userId = event.getUser().getId();

        replyLaterWith(event, true, () -> render(userId, from));
    }

    @Override
    public void onButton(ButtonInteractionEvent event) {
        String[] parts = event.getComponentId().split(":");
        if (parts.length != 5 || !(parts[2].equals(DONE) || parts[2].equals(UNDO))) {
            event.reply("Ce bouton n'est plus actif.").setEphemeral(true).queue();
            return;
        }
        boolean done = parts[2].equals(DONE);
        String assignmentId = parts[3];
        String context = parts[4];
        String userId = event.getUser().getId();
        StudySuiteClient client = getBot().getStudySuite();

        if (context.equals(ANNOUNCEMENT)) {
            replyPrivatelyLater(event, () -> {
                requireApiKey(client);
                client.setCompleted(userId, assignmentId, done);
                return new MessageEditBuilder()
                        .setContent(done ? "✅ Coché dans ta liste de devoirs." : "↩️ Décoché.")
                        .setComponents(ActionRow.of(button(assignmentId, !done, ANNOUNCEMENT)))
                        .build();
            });
            return;
        }

        Instant from = Instant.ofEpochSecond(Long.parseLong(context));
        editLater(event, () -> {
            requireApiKey(client);
            client.setCompleted(userId, assignmentId, done);
            return render(userId, from);
        });
    }

    /** Le bouton qui met ce devoir dans l'état inverse de {@code completed}. */
    static Button button(String assignmentId, boolean completed, String context) {
        String id = "study:" + NAME + ":" + (completed ? UNDO : DONE) + ":" + assignmentId + ":" + context;
        return completed ? Button.secondary(id, "Annuler") : Button.success(id, "Fait ✓");
    }

    private MessageEditData render(String userId, Instant from) {
        StudySuiteClient client = getBot().getStudySuite();
        requireApiKey(client);
        String context = String.valueOf(from.getEpochSecond());

        return new MessageEditBuilder()
                .useComponentsV2()
                .setComponents(HomeworkFormatter.list(
                        client.getAssignments(userId, from),
                        client.getBaseUrl(),
                        a -> button(a.id(), a.completedByMe(), context)
                ))
                .build();
    }

    static void requireApiKey(StudySuiteClient client) {
        if (!client.hasApiKey()) {
            throw new UserFacingException("Les devoirs ne sont pas disponibles : le bot n'a pas de clé StudySuite.");
        }
    }

    /** Le bouton d'une annonce toute fraîche : personne ne l'a encore coché. */
    static Button announcementButton(Assignment assignment) {
        return button(assignment.id(), false, ANNOUNCEMENT);
    }
}
