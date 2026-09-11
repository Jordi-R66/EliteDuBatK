package fr.umontpellier.iut.discordbot.commands.study;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.studysuite.HomeworkFormatter;
import fr.umontpellier.iut.discordbot.studysuite.StudyDates;
import fr.umontpellier.iut.discordbot.studysuite.StudySuiteClient;
import fr.umontpellier.iut.discordbot.studysuite.model.Assignment;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * {@code /study devoirs [passes]} : les devoirs du membre, d'après son compte StudySuite, avec un menu pour cocher
 * ceux qu'il a faits. Toujours en privé : c'est sa liste à lui.
 */
public class HomeworkSubcommand extends StudySubcommand {
    static final String NAME = "devoirs";
    private static final String OPTION_PAST = "passes";
    /** Avec {@code passes}, jusqu'où on regarde en arrière. */
    private static final Duration PAST_WINDOW = Duration.ofDays(14);

    private static final DateTimeFormatter OPTION_DATE =
            DateTimeFormatter.ofPattern("'pour le' EEE d/MM 'à' HH:mm", Locale.FRENCH).withZone(StudyDates.PARIS);

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

    /**
     * Le menu porte dans son identifiant le début de la période affichée, pour réafficher la même liste une fois les
     * cases mises à jour. L'état « fait » de départ, lui, est celui des options cochées par défaut.
     */
    @Override
    public void onStringSelect(StringSelectInteractionEvent event) {
        String[] parts = event.getComponentId().split(":");
        Instant from = Instant.ofEpochSecond(Long.parseLong(parts[2]));
        String userId = event.getUser().getId();

        Set<String> before = new HashSet<>();
        event.getSelectMenu().getOptions().stream().filter(SelectOption::isDefault).forEach(o -> before.add(o.getValue()));
        Set<String> after = new HashSet<>(event.getValues());

        editLater(event, () -> {
            StudySuiteClient client = getBot().getStudySuite();
            for (SelectOption option : event.getSelectMenu().getOptions()) {
                String id = option.getValue();
                boolean done = after.contains(id);
                if (done != before.contains(id)) {
                    client.setCompleted(userId, id, done);
                }
            }
            return render(userId, from);
        });
    }

    private MessageEditData render(String userId, Instant from) {
        StudySuiteClient client = getBot().getStudySuite();
        requireApiKey(client);
        List<Assignment> assignments = HomeworkFormatter.sort(client.getAssignments(userId, from));

        MessageEditBuilder message = new MessageEditBuilder()
                .setEmbeds(HomeworkFormatter.list(assignments, client.getBaseUrl()));

        List<Assignment> listed = assignments.stream().limit(HomeworkFormatter.MAX_LISTED).toList();
        if (listed.isEmpty()) {
            return message.setComponents().build();
        }

        StringSelectMenu.Builder menu = StringSelectMenu.create("study:" + NAME + ":" + from.getEpochSecond())
                .setPlaceholder("Coche ce que tu as fait")
                .setRequiredRange(0, listed.size());
        for (Assignment assignment : listed) {
            menu.addOption(
                    truncate(HomeworkFormatter.title(assignment), SelectOption.LABEL_MAX_LENGTH),
                    assignment.id(),
                    truncate(OPTION_DATE.format(assignment.due()), SelectOption.DESCRIPTION_MAX_LENGTH)
            );
        }
        menu.setDefaultValues(listed.stream().filter(Assignment::completedByMe).map(Assignment::id).toList());

        return message.setComponents(ActionRow.of(menu.build())).build();
    }

    static void requireApiKey(StudySuiteClient client) {
        if (!client.hasApiKey()) {
            throw new UserFacingException("Les devoirs ne sont pas disponibles : le bot n'a pas de clé StudySuite.");
        }
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
