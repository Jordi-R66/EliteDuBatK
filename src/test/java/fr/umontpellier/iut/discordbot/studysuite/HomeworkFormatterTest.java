package fr.umontpellier.iut.discordbot.studysuite;

import fr.umontpellier.iut.discordbot.studysuite.model.Assignment;
import fr.umontpellier.iut.discordbot.studysuite.model.GroupRef;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeworkFormatterTest {
    private static final String SITE = "https://study-info.umontp.fr";
    private static final GroupRef S1A = new GroupRef("s1a", "S1a", null);
    private static final GroupRef PROMO = new GroupRef("promo", "BUT 1A PROMO", "BUT1");

    private static Assignment assignment(String title, String subject, String due, GroupRef group, boolean done) {
        return new Assignment(title, title, subject, null, due, group, null, null, done, done ? 1 : 0);
    }

    @Test
    void deadlineIsARealInstant() {
        // Un devoir porte un vrai instant, sans l'étiquetage « heure de Paris » des cours
        Assignment a = assignment("TP3", "R5.05", "2026-09-15T21:59:00.000Z", S1A, false);
        assertEquals("<t:1789509540:F> (<t:1789509540:R>)", HomeworkFormatter.deadline(a));
    }

    @Test
    void titleCarriesTheSubject() {
        assertEquals("R5.05 — TP3", HomeworkFormatter.title(assignment("TP3", "R5.05", "2026-09-15T21:59:00Z", S1A, false)));
        assertEquals("TP3", HomeworkFormatter.title(assignment("TP3", " ", "2026-09-15T21:59:00Z", S1A, false)));
    }

    @Test
    void doneOnesAreStruckThrough() {
        String done = HomeworkFormatter.line(assignment("TP3", null, "2026-09-15T21:59:00Z", S1A, true), false);
        String todo = HomeworkFormatter.line(assignment("TP4", null, "2026-09-15T21:59:00Z", S1A, false), false);

        assertTrue(done.startsWith("✅ ~~TP3~~"), done);
        assertTrue(todo.startsWith("⬜ **TP4**"), todo);
    }

    @Test
    void listIsSortedByDeadlineAndCountsDoneOnes() {
        MessageEmbed embed = HomeworkFormatter.list(List.of(
                assignment("Plus tard", null, "2026-09-20T21:59:00Z", S1A, true),
                assignment("Bientôt", null, "2026-09-15T21:59:00Z", S1A, false)
        ), SITE);

        assertEquals(SITE + "/homework", embed.getUrl());
        assertTrue(embed.getDescription().indexOf("Bientôt") < embed.getDescription().indexOf("Plus tard"));
        assertTrue(embed.getFooter().getText().contains("1/2"), embed.getFooter().getText());
        // Un seul groupe : pas besoin de le répéter sur chaque ligne
        assertTrue(!embed.getDescription().contains("S1a"), embed.getDescription());
    }

    @Test
    void groupIsShownWhenSeveral() {
        MessageEmbed embed = HomeworkFormatter.list(List.of(
                assignment("TD", null, "2026-09-15T21:59:00Z", S1A, false),
                assignment("Promo", null, "2026-09-16T21:59:00Z", PROMO, false)
        ), SITE);

        assertTrue(embed.getDescription().contains("· S1a"), embed.getDescription());
        assertTrue(embed.getDescription().contains("· BUT1"), embed.getDescription());
    }

    @Test
    void emptyList() {
        assertEquals("Rien à rendre pour l'instant 🎉", HomeworkFormatter.list(List.of(), SITE).getDescription());
    }

    @Test
    void longListStaysSendable() {
        List<Assignment> many = IntStream.range(0, 40)
                .mapToObj(i -> new Assignment("id" + i, "Devoir " + "z".repeat(150) + i, "R1.0" + (i % 9),
                        "d".repeat(500), "2026-09-15T21:59:00Z", S1A, null, null, false, 0))
                .toList();

        MessageEmbed embed = HomeworkFormatter.list(many, SITE);
        assertTrue(embed.isSendable());
        assertTrue(embed.getDescription().contains("autre(s)"), embed.getDescription());
    }
}
