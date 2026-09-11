package fr.umontpellier.iut.discordbot.studysuite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudyDatesTest {
    /** Un vendredi. */
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 11);

    @ParameterizedTest
    @CsvSource({
            "aujourd'hui, 2026-09-11",
            "Aujourd’hui, 2026-09-11",
            "demain, 2026-09-12",
            "après-demain, 2026-09-13",
            "hier, 2026-09-10",
            // Le prochain lundi ; le jour même compte
            "lundi, 2026-09-14",
            "Vendredi, 2026-09-11",
            "vendredi prochain, 2026-09-18",
            "15/09, 2026-09-15",
            "15/09/2027, 2027-09-15",
            "1/2/27, 2027-02-01",
            "2026-09-15, 2026-09-15",
            // Passé de peu : on regarde en arrière dans l'année en cours
            "01/09, 2026-09-01",
            // Passé depuis longtemps : c'est l'an prochain
            "01/03, 2027-03-01",
    })
    void parsesWhatStudentsType(String input, LocalDate expected) {
        assertEquals(expected, StudyDates.parse(input, TODAY).orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "bientôt", "31/02", "13/13", "2026-02-30"})
    void rejectsTheRest(String input) {
        assertTrue(StudyDates.parse(input, TODAY).isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "18h, 18:00",
            "18h30, 18:30",
            "8h05, 08:05",
            "8:05, 08:05",
            "23:59, 23:59",
            "18 h 30, 18:30",
            "midi, 12:00",
            "minuit, 23:59",
    })
    void parsesTimes(String input, LocalTime expected) {
        assertEquals(expected, StudyDates.parseTime(input).orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "18", "24h", "18h60", "18h5", "demain", "6pm"})
    void rejectsOtherTimes(String input) {
        assertTrue(StudyDates.parseTime(input).isEmpty(), input);
    }

    @Test
    void mondayOfTheWeek() {
        assertEquals(LocalDate.of(2026, 9, 7), StudyDates.monday(TODAY));
        assertEquals(LocalDate.of(2026, 9, 7), StudyDates.monday(LocalDate.of(2026, 9, 13)));
        assertEquals(LocalDate.of(2026, 9, 14), StudyDates.monday(LocalDate.of(2026, 9, 14)));
    }

    @Test
    void weekendJumpsToMonday() {
        assertEquals(TODAY, StudyDates.skipWeekend(TODAY));
        assertEquals(LocalDate.of(2026, 9, 14), StudyDates.skipWeekend(LocalDate.of(2026, 9, 12)));
        assertEquals(LocalDate.of(2026, 9, 14), StudyDates.skipWeekend(LocalDate.of(2026, 9, 13)));
    }
}
