package fr.umontpellier.iut.discordbot.commands.study;

import fr.umontpellier.iut.discordbot.studysuite.StudyDates;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddHomeworkSubcommandTest {
    @Test
    void dueTimeIsParisTime() {
        // 23h59 à Paris : 21:59 UTC en été, 22:59 UTC en hiver
        assertEquals(Instant.parse("2026-09-15T21:59:00Z"), AddHomeworkSubcommand.dueInstant(LocalDate.of(2026, 9, 15), StudyDates.END_OF_DAY));
        assertEquals(Instant.parse("2026-12-15T22:59:00Z"), AddHomeworkSubcommand.dueInstant(LocalDate.of(2026, 12, 15), StudyDates.END_OF_DAY));
        assertEquals(Instant.parse("2026-09-15T16:00:00Z"), AddHomeworkSubcommand.dueInstant(LocalDate.of(2026, 9, 15), LocalTime.of(18, 0)));
    }

    @Test
    void validation() {
        assertTrue(AddHomeworkSubcommand.validate("TP3", "demain", null).isEmpty());
        assertTrue(AddHomeworkSubcommand.validate("TP3", "demain", "18h").isEmpty());

        assertTrue(AddHomeworkSubcommand.validate("", "demain", null).isPresent());
        assertTrue(AddHomeworkSubcommand.validate("TP3", "bientôt", null).orElseThrow().contains("date"));
        assertTrue(AddHomeworkSubcommand.validate("TP3", "demain", "6pm").orElseThrow().contains("heure"));
        assertTrue(AddHomeworkSubcommand.validate("TP3", "hier", null).orElseThrow().contains("passée"));
    }
}
