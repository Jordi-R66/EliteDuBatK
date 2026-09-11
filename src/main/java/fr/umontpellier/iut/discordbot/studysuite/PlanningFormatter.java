package fr.umontpellier.iut.discordbot.studysuite;

import fr.umontpellier.iut.discordbot.studysuite.model.GroupRef;
import fr.umontpellier.iut.discordbot.studysuite.model.StudyEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.awt.Color;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Met un planning en forme pour Discord. */
public final class PlanningFormatter {
    public static final Color COLOR = new Color(0x3F51B5);

    private static final DateTimeFormatter LONG_DAY = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter SHORT_DAY = DateTimeFormatter.ofPattern("EEEE d/MM", Locale.FRENCH);
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d MMMM", Locale.FRENCH);

    private PlanningFormatter() {
    }

    /**
     * Le planning d'une journée.
     *
     * @param groups les groupes demandés : un cours hérité d'un groupe parent (un CM de promo…) affiche le sien
     * @param planningUrl le lien du titre, vers ce planning sur le site
     */
    public static MessageEmbed day(LocalDate date, Collection<GroupRef> groups, List<StudyEvent> events, String siteUrl, String planningUrl) {
        List<StudyEvent> sorted = sort(events);
        Set<String> requested = ids(groups);

        StringBuilder description = new StringBuilder();
        if (sorted.isEmpty()) {
            description.append("Aucun cours ce jour-là 🎉");
        }
        for (StudyEvent event : sorted) {
            String block = "**" + hours(event) + "** · " + event.title() + "\n"
                    + "└ " + details(event, requested) + "\n";
            if (description.length() + block.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH - 40) {
                description.append("*… et d'autres cours, voir le site*");
                break;
            }
            description.append(block);
        }

        return base(siteUrl)
                .setTitle(truncate("Planning " + labels(groups) + " · " + LONG_DAY.format(date), MessageEmbed.TITLE_MAX_LENGTH), planningUrl)
                .setDescription(description.toString().strip())
                .build();
    }

    /** Le planning de la semaine de {@code monday}, un champ par jour. */
    public static MessageEmbed week(LocalDate monday, Collection<GroupRef> groups, List<StudyEvent> events, String siteUrl, String planningUrl) {
        Set<String> requested = ids(groups);
        Map<LocalDate, List<StudyEvent>> byDay = new TreeMap<>(sort(events).stream()
                .collect(Collectors.groupingBy(e -> e.start().toLocalDate())));

        EmbedBuilder embed = base(siteUrl).setTitle(
                truncate("Planning " + labels(groups) + " · semaine du " + DAY_MONTH.format(monday), MessageEmbed.TITLE_MAX_LENGTH),
                planningUrl
        );

        if (byDay.isEmpty()) {
            embed.setDescription("Aucun cours cette semaine 🎉");
            return embed.build();
        }

        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            List<StudyEvent> dayEvents = byDay.getOrDefault(day, List.of());
            // Le samedi et le dimanche n'apparaissent que s'il y a cours.
            boolean weekend = day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY;
            if (dayEvents.isEmpty() && weekend) continue;

            List<String> lines = new ArrayList<>();
            for (StudyEvent event : dayEvents) {
                lines.add("**" + hours(event) + "** " + event.title() + " · " + details(event, requested));
            }
            embed.addField(capitalize(SHORT_DAY.format(day)), lines.isEmpty() ? "*Pas de cours*" : fitField(lines), false);
        }

        return embed.build();
    }

    /**
     * Les heures en timestamps Discord ({@code <t:…:t>}) : chacun les voit dans son fuseau, au format de sa langue.
     * Ils ne s'affichent ni dans les titres ni dans les noms de champs, ni dans un bloc de code.
     */
    static String hours(StudyEvent event) {
        return TimeFormat.TIME_SHORT.format(event.startInstant()) + " – " + TimeFormat.TIME_SHORT.format(event.endInstant());
    }

    /** Salles, profs, et le groupe du cours s'il est hérité d'un parent (« BUT1 » pour un CM de promo). */
    static String details(StudyEvent event, Set<String> requestedGroupIds) {
        List<String> parts = new ArrayList<>();

        String rooms = event.rooms().stream().map(StudyEvent.Room::name).collect(Collectors.joining(", "));
        parts.add(rooms.isEmpty() ? "salle inconnue" : rooms);

        String teachers = event.teachers().stream().map(PlanningFormatter::teacher).collect(Collectors.joining(", "));
        if (!teachers.isEmpty()) parts.add(teachers);

        boolean ownGroup = event.groups().stream().map(GroupRef::id).anyMatch(requestedGroupIds::contains);
        if (!ownGroup && !event.groups().isEmpty()) {
            parts.add(event.groups().stream().map(GroupRef::label).collect(Collectors.joining(", ")));
        }

        return String.join(" · ", parts);
    }

    /** « A. DAUMET » */
    static String teacher(StudyEvent.Teacher teacher) {
        String first = teacher.firstName() == null ? "" : teacher.firstName().strip();
        String last = teacher.lastName() == null ? "" : teacher.lastName().strip();
        if (first.isEmpty()) return last;
        return first.charAt(0) + ". " + last;
    }

    /** Remplit un champ sans dépasser la limite de Discord, en disant combien de cours ne tiennent pas. */
    static String fitField(List<String> lines) {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String more = "\n*… et " + (lines.size() - i) + " autre(s)*";
            int needed = (value.isEmpty() ? 0 : 1) + line.length();
            boolean last = i == lines.size() - 1;
            if (value.length() + needed + (last ? 0 : more.length()) > MessageEmbed.VALUE_MAX_LENGTH) {
                if (value.isEmpty()) {
                    return truncate(line, MessageEmbed.VALUE_MAX_LENGTH);
                }
                return value.append(more).toString();
            }
            if (!value.isEmpty()) value.append('\n');
            value.append(line);
        }
        return value.toString();
    }

    private static EmbedBuilder base(String siteUrl) {
        return new EmbedBuilder()
                .setColor(COLOR)
                .setFooter("StudySuite · " + siteUrl.replaceFirst("^https?://", ""));
    }

    private static List<StudyEvent> sort(List<StudyEvent> events) {
        return events.stream()
                .sorted(Comparator.comparing(StudyEvent::start).thenComparing(StudyEvent::title))
                .toList();
    }

    private static Set<String> ids(Collection<GroupRef> groups) {
        return groups.stream().map(GroupRef::id).collect(Collectors.toSet());
    }

    private static String labels(Collection<GroupRef> groups) {
        return groups.stream().map(GroupRef::label).collect(Collectors.joining(", "));
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
