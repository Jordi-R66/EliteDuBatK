package fr.umontpellier.iut.discordbot.commands.study;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.studysuite.GroupHierarchy;
import fr.umontpellier.iut.discordbot.studysuite.MemberGroups;
import fr.umontpellier.iut.discordbot.studysuite.PlanningFormatter;
import fr.umontpellier.iut.discordbot.studysuite.StudyDates;
import fr.umontpellier.iut.discordbot.studysuite.StudySuiteClient;
import fr.umontpellier.iut.discordbot.studysuite.model.GroupRef;
import fr.umontpellier.iut.discordbot.studysuite.model.StudyEvent;
import fr.umontpellier.iut.discordbot.studysuite.model.StudyGroup;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * {@code /study planning [periode] [date] [groupe] [prive]} : l'emploi du temps d'un jour ou d'une semaine.
 * <p>
 * Sans {@code groupe}, c'est la classe du membre, lue depuis ses rôles (associations rôle → groupe gérées sur
 * StudySuite, qui demandent la clé d'API).
 */
public class PlanningSubcommand extends StudySubcommand {
    private static final String OPTION_PERIOD = "periode";
    private static final String OPTION_DATE = "date";
    private static final String OPTION_GROUP = "groupe";
    private static final String OPTION_PRIVATE = "prive";

    private static final String PERIOD_DAY = "jour";
    private static final String PERIOD_WEEK = "semaine";

    public PlanningSubcommand(@NotNull Bot bot) {
        super(bot);
    }

    @NotNull
    @Override
    public SubcommandData getData() {
        return new SubcommandData("planning", "Afficher l'emploi du temps")
                .addOptions(
                        new OptionData(OptionType.STRING, OPTION_PERIOD, "Un jour ou toute la semaine (par défaut : jour)")
                                .addChoice("Jour", PERIOD_DAY)
                                .addChoice("Semaine", PERIOD_WEEK),
                        new OptionData(OptionType.STRING, OPTION_DATE, "« demain », « lundi », « 15/09 »… (par défaut : aujourd'hui)"),
                        new OptionData(OptionType.STRING, OPTION_GROUP, "Le groupe (par défaut : le tien, d'après tes rôles)")
                                .setAutoComplete(true),
                        new OptionData(OptionType.BOOLEAN, OPTION_PRIVATE, "Ne l'afficher que pour toi")
                );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        boolean week = PERIOD_WEEK.equals(event.getOption(OPTION_PERIOD, PERIOD_DAY, OptionMapping::getAsString));
        String dateInput = event.getOption(OPTION_DATE, OptionMapping::getAsString);
        String groupInput = event.getOption(OPTION_GROUP, OptionMapping::getAsString);
        boolean ephemeral = event.getOption(OPTION_PRIVATE, false, OptionMapping::getAsBoolean);
        Member member = event.getMember();

        // Validée avant de différer, pour pouvoir répondre en privé sans message public à retirer.
        LocalDate date;
        if (dateInput == null || dateInput.isBlank()) {
            date = StudyDates.skipWeekend(StudyDates.today());
        } else {
            var parsed = StudyDates.parse(dateInput, StudyDates.today());
            if (parsed.isEmpty()) {
                event.reply("❌ Je ne comprends pas la date « " + dateInput + " ». Essaie « demain », « lundi » ou « 15/09 ».")
                        .setEphemeral(true).queue();
                return;
            }
            date = parsed.get();
        }

        replyLater(event, ephemeral, () -> render(week, date, groupInput, member));
    }

    private MessageEmbed render(boolean week, LocalDate date, String groupInput, Member member) {
        StudySuiteClient client = getBot().getStudySuite();
        GroupHierarchy hierarchy = client.getHierarchy();
        List<GroupRef> groups = resolveGroups(groupInput, member, hierarchy, client);
        Set<String> groupIds = Set.copyOf(groups.stream().map(GroupRef::id).toList());
        String planningUrl = hierarchy.planningUrl(client.getBaseUrl(), groups);

        if (week) {
            LocalDate monday = StudyDates.monday(date);
            List<StudyEvent> events = hierarchy.withVisibleGroups(hierarchy.eventsFor(groupIds, client.getWeekEvents(monday)));
            return PlanningFormatter.week(monday, groups, events, client.getBaseUrl(), planningUrl);
        }
        List<StudyEvent> events = hierarchy.withVisibleGroups(hierarchy.eventsFor(groupIds, client.getDayEvents(date)));
        return PlanningFormatter.day(date, groups, events, client.getBaseUrl(), planningUrl);
    }

    @Override
    public void autocomplete(CommandAutoCompleteInteractionEvent event) {
        if (OPTION_GROUP.equals(event.getFocusedOption().getName())) {
            autocompleteGroup(event);
        } else {
            event.replyChoices(List.of()).queue();
        }
    }
}
