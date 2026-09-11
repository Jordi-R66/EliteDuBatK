package fr.umontpellier.iut.discordbot.commands.study;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.studysuite.HomeworkFormatter;
import fr.umontpellier.iut.discordbot.studysuite.StudyDates;
import fr.umontpellier.iut.discordbot.studysuite.StudySuiteClient;
import fr.umontpellier.iut.discordbot.studysuite.model.Assignment;
import fr.umontpellier.iut.discordbot.studysuite.model.GroupRef;
import fr.umontpellier.iut.discordbot.studysuite.model.NewAssignment;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * {@code /study devoir-ajouter titre date [heure] [matiere] [description] [groupe]} : ajoute un devoir sur StudySuite
 * au nom du membre, et l'annonce dans le salon avec un bouton pour que chacun le coche.
 * <p>
 * StudySuite vérifie que le compte du membre est validé et qu'il a accès au groupe ; le bot ne fait que relayer.
 */
public class AddHomeworkSubcommand extends StudySubcommand {
    private static final String OPTION_TITLE = "titre";
    private static final String OPTION_DATE = "date";
    private static final String OPTION_TIME = "heure";
    private static final String OPTION_SUBJECT = "matiere";
    private static final String OPTION_DESCRIPTION = "description";
    private static final String OPTION_GROUP = "groupe";

    public AddHomeworkSubcommand(@NotNull Bot bot) {
        super(bot);
    }

    @NotNull
    @Override
    public SubcommandData getData() {
        return new SubcommandData("devoir-ajouter", "Ajouter un devoir pour ta classe")
                .addOptions(
                        new OptionData(OptionType.STRING, OPTION_TITLE, "Ce qu'il faut rendre ou faire", true)
                                .setMaxLength(255),
                        new OptionData(OptionType.STRING, OPTION_DATE, "Pour quand : « lundi », « 15/09 »…", true),
                        new OptionData(OptionType.STRING, OPTION_TIME, "À quelle heure : « 18h », « 8h30 »… (par défaut : 23h59)"),
                        new OptionData(OptionType.STRING, OPTION_SUBJECT, "La matière, ex. R5.05")
                                .setMaxLength(100),
                        new OptionData(OptionType.STRING, OPTION_DESCRIPTION, "Les détails : consignes, lien Moodle…"),
                        new OptionData(OptionType.STRING, OPTION_GROUP, "Pour quel groupe (par défaut : le tien, d'après tes rôles)")
                                .setAutoComplete(true)
                );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String title = event.getOption(OPTION_TITLE, "", OptionMapping::getAsString).strip();
        String dateInput = event.getOption(OPTION_DATE, "", OptionMapping::getAsString);
        String timeInput = event.getOption(OPTION_TIME, OptionMapping::getAsString);
        String subject = blankToNull(event.getOption(OPTION_SUBJECT, OptionMapping::getAsString));
        String description = blankToNull(event.getOption(OPTION_DESCRIPTION, OptionMapping::getAsString));
        String groupInput = event.getOption(OPTION_GROUP, OptionMapping::getAsString);
        Member member = event.getMember();

        // Tout ce qui se vérifie sans StudySuite l'est avant de différer, pour répondre en privé.
        Optional<String> invalid = validate(title, dateInput, timeInput);
        if (invalid.isPresent()) {
            event.reply("❌ " + invalid.get()).setEphemeral(true).queue();
            return;
        }
        Instant due = dueInstant(StudyDates.parse(dateInput, StudyDates.today()).orElseThrow(),
                timeInput == null || timeInput.isBlank() ? StudyDates.END_OF_DAY : StudyDates.parseTime(timeInput).orElseThrow());

        String userId = event.getUser().getId();
        String mention = event.getUser().getAsMention();
        replyLaterWith(event, false, () -> create(userId, mention, member, groupInput, new Draft(title, subject, description, due)));
    }

    private record Draft(String title, String subject, String description, Instant due) {
    }

    private MessageEditData create(String userId, String mention, Member member, String groupInput, Draft draft) {
        StudySuiteClient client = getBot().getStudySuite();
        HomeworkSubcommand.requireApiKey(client);

        List<GroupRef> groups = resolveGroups(groupInput, member, client.getHierarchy(), client);
        if (groups.size() > 1) {
            throw new UserFacingException("Tes rôles correspondent à plusieurs classes ("
                    + String.join(", ", groups.stream().map(GroupRef::label).toList())
                    + ") : précise laquelle avec l'option `groupe`.");
        }

        Assignment created = client.createAssignment(userId, new NewAssignment(
                draft.title(), draft.subject(), draft.description(), draft.due().toString(), groups.getFirst().id()));
        logger.info("User {} added assignment {} for group {}", userId, created.id(), groups.getFirst().internalName());
        return new MessageEditBuilder()
                .useComponentsV2()
                .setComponents(HomeworkFormatter.created(created, mention, client.getBaseUrl(),
                        HomeworkSubcommand.announcementButton(created)))
                .build();
    }

    /** Pourquoi ces options ne donnent pas un devoir valable, s'il y a une raison. */
    static Optional<String> validate(String title, String dateInput, String timeInput) {
        if (title.isEmpty()) {
            return Optional.of("Le titre ne peut pas être vide.");
        }
        Optional<LocalDate> date = StudyDates.parse(dateInput, StudyDates.today());
        if (date.isEmpty()) {
            return Optional.of("Je ne comprends pas la date « " + dateInput + " ». Essaie « lundi » ou « 15/09 ».");
        }
        LocalTime time = StudyDates.END_OF_DAY;
        if (timeInput != null && !timeInput.isBlank()) {
            Optional<LocalTime> parsed = StudyDates.parseTime(timeInput);
            if (parsed.isEmpty()) {
                return Optional.of("Je ne comprends pas l'heure « " + timeInput + " ». Essaie « 18h » ou « 8h30 ».");
            }
            time = parsed.get();
        }
        if (dueInstant(date.get(), time).isBefore(Instant.now())) {
            return Optional.of("Cette date est déjà passée.");
        }
        return Optional.empty();
    }

    /** Un rendu « lundi à 18h » est un 18h à Paris. */
    static Instant dueInstant(LocalDate date, LocalTime time) {
        return date.atTime(time).atZone(StudyDates.PARIS).toInstant();
    }

    @Override
    public void autocomplete(CommandAutoCompleteInteractionEvent event) {
        if (OPTION_GROUP.equals(event.getFocusedOption().getName())) {
            autocompleteGroup(event);
        } else {
            event.replyChoices(List.of()).queue();
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
