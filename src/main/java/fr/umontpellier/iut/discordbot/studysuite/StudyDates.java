package fr.umontpellier.iut.discordbot.studysuite;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Les dates que les étudiants tapent : « demain », « lundi », « 15/09 », « 2026-09-15 »… */
public final class StudyDates {
    public static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private static final Pattern DAY_MONTH = Pattern.compile("(\\d{1,2})[/.-](\\d{1,2})(?:[/.-](\\d{2}|\\d{4}))?");
    private static final Pattern TIME = Pattern.compile("(\\d{1,2})(?:[h:](\\d{2})?)");
    /** L'heure par défaut d'un rendu : la fin de la journée. */
    public static final LocalTime END_OF_DAY = LocalTime.of(23, 59);
    private static final Pattern ISO =Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})");

    private static final Map<String, DayOfWeek> WEEKDAYS = Map.of(
            "lundi", DayOfWeek.MONDAY,
            "mardi", DayOfWeek.TUESDAY,
            "mercredi", DayOfWeek.WEDNESDAY,
            "jeudi", DayOfWeek.THURSDAY,
            "vendredi", DayOfWeek.FRIDAY,
            "samedi", DayOfWeek.SATURDAY,
            "dimanche", DayOfWeek.SUNDAY
    );

    private StudyDates() {
    }

    /** Aujourd'hui à Paris, quel que soit le fuseau de la machine (UTC dans le conteneur). */
    public static LocalDate today() {
        return LocalDate.now(PARIS);
    }

    /**
     * Lit une date relative à {@code today}. Un jour de la semaine désigne le prochain (aujourd'hui compris) ; une
     * date sans année désigne la prochaine occurrence, sauf si elle est passée de moins de deux mois (on regarde
     * sans doute en arrière dans l'année en cours).
     */
    public static Optional<LocalDate> parse(String input, LocalDate today) {
        if (input == null) return Optional.empty();
        String text = GroupHierarchy.normalize(input).replace("’", "'");
        if (text.isEmpty()) return Optional.empty();

        switch (text) {
            case "aujourd'hui", "aujourdhui", "auj", "today" -> {
                return Optional.of(today);
            }
            case "demain", "tomorrow" -> {
                return Optional.of(today.plusDays(1));
            }
            case "apres-demain", "apres demain" -> {
                return Optional.of(today.plusDays(2));
            }
            case "hier", "yesterday" -> {
                return Optional.of(today.minusDays(1));
            }
            default -> {
            }
        }

        DayOfWeek weekday = WEEKDAYS.get(text.replaceAll(" prochain$", ""));
        if (weekday != null) {
            LocalDate next = today.with(TemporalAdjusters.nextOrSame(weekday));
            return Optional.of(text.endsWith(" prochain") && next.equals(today) ? next.plusWeeks(1) : next);
        }

        try {
            Matcher iso = ISO.matcher(text);
            if (iso.matches()) {
                return Optional.of(LocalDate.of(Integer.parseInt(iso.group(1)), Integer.parseInt(iso.group(2)), Integer.parseInt(iso.group(3))));
            }

            Matcher dm = DAY_MONTH.matcher(text);
            if (dm.matches()) {
                int day = Integer.parseInt(dm.group(1));
                int month = Integer.parseInt(dm.group(2));
                if (dm.group(3) != null) {
                    int year = Integer.parseInt(dm.group(3));
                    return Optional.of(LocalDate.of(year < 100 ? 2000 + year : year, month, day));
                }
                LocalDate candidate = LocalDate.of(today.getYear(), month, day);
                if (candidate.isBefore(today.minusMonths(2))) {
                    candidate = candidate.plusYears(1);
                }
                return Optional.of(candidate);
            }
        } catch (DateTimeException e) {
            // 31/02 et consorts
            return Optional.empty();
        }

        return Optional.empty();
    }

    /** Une heure tapée à la main : « 18h », « 18h30 », « 8:05 », « 23:59 », « midi », « minuit » (fin de journée). */
    public static Optional<LocalTime> parseTime(String input) {
        if (input == null) return Optional.empty();
        String text = GroupHierarchy.normalize(input).replace(" ", "");
        switch (text) {
            case "midi" -> {
                return Optional.of(LocalTime.NOON);
            }
            // Pour un rendu, « minuit » veut dire la fin de la journée, pas son début
            case "minuit" -> {
                return Optional.of(END_OF_DAY);
            }
            default -> {
            }
        }
        Matcher m = TIME.matcher(text);
        if (!m.matches()) return Optional.empty();
        int hour = Integer.parseInt(m.group(1));
        int minute = m.group(2) == null || m.group(2).isEmpty() ? 0 : Integer.parseInt(m.group(2));
        if (hour > 23 || minute > 59) return Optional.empty();
        return Optional.of(LocalTime.of(hour, minute));
    }

    /** Le lundi de la semaine de {@code date}. */
    public static LocalDate monday(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Le week-end, « aujourd'hui » et « cette semaine » n'ont plus de cours : on regarde la semaine suivante. */
    public static LocalDate skipWeekend(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case SATURDAY -> date.plusDays(2);
            case SUNDAY -> date.plusDays(1);
            default -> date;
        };
    }
}
