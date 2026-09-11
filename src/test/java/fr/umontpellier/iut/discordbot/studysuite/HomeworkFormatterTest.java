package fr.umontpellier.iut.discordbot.studysuite;

import fr.umontpellier.iut.discordbot.studysuite.model.Assignment;
import fr.umontpellier.iut.discordbot.studysuite.model.GroupRef;
import net.dv8tion.jda.api.components.Component;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeworkFormatterTest {
    private static final String SITE = "https://study-info.umontp.fr";
    private static final GroupRef S1A = new GroupRef("s1a", "S1a", null);
    private static final GroupRef PROMO = new GroupRef("promo", "BUT 1A PROMO", "BUT1");
    private static final Function<Assignment, Button> BUTTON = a -> Button.success("b:" + a.id(), "Fait ✓");

    private static Assignment assignment(String title, String subject, String due, GroupRef group, boolean done) {
        return new Assignment(title, title, subject, null, due, group, null, null, done, done ? 1 : 0);
    }

    /** Tous les textes du conteneur, dans l'ordre, sections comprises. */
    private static List<String> texts(Container container) {
        return container.getComponents().stream()
                .flatMap(c -> switch (c.getType()) {
                    case SECTION -> c.asSection().getContentComponents().stream().map(t -> t.asTextDisplay().getContent());
                    case TEXT_DISPLAY -> java.util.stream.Stream.of(c.asTextDisplay().getContent());
                    default -> java.util.stream.Stream.empty();
                })
                .toList();
    }

    private static List<Section> sections(Container container) {
        return container.getComponents().stream().filter(c -> c.getType() == Component.Type.SECTION).map(c -> (Section) c.asSection()).toList();
    }

    /** Le conteneur, ses enfants, et le texte et le bouton de chaque section. */
    private static int componentCount(Container container) {
        return 1 + container.getComponents().stream()
                .mapToInt(c -> c.getType() == Component.Type.SECTION ? 1 + c.asSection().getContentComponents().size() + 1 : 1)
                .sum();
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
    void oneSectionPerAssignmentWithItsButtonAtTheEnd() {
        Container container = HomeworkFormatter.list(List.of(
                assignment("Plus tard", null, "2026-09-20T21:59:00Z", S1A, true),
                assignment("Bientôt", null, "2026-09-15T21:59:00Z", S1A, false)
        ), SITE, BUTTON);

        List<Section> sections = sections(container);
        assertEquals(2, sections.size());
        // Triés par échéance, chacun avec son propre bouton
        assertTrue(sections.get(0).getContentComponents().getFirst().asTextDisplay().getContent().contains("Bientôt"));
        assertEquals("b:Bientôt", sections.get(0).getAccessory().asButton().getCustomId());
        assertEquals("b:Plus tard", sections.get(1).getAccessory().asButton().getCustomId());

        List<String> texts = texts(container);
        assertTrue(texts.getFirst().contains(SITE + "/homework"), texts.getFirst());
        assertTrue(texts.getLast().contains("1/2"), texts.getLast());
        // Un seul groupe : pas besoin de le répéter sur chaque ligne
        assertFalse(String.join("\n", texts).contains("S1a"));
    }

    @Test
    void groupIsShownWhenSeveral() {
        String all = String.join("\n", texts(HomeworkFormatter.list(List.of(
                assignment("TD", null, "2026-09-15T21:59:00Z", S1A, false),
                assignment("Promo", null, "2026-09-16T21:59:00Z", PROMO, false)
        ), SITE, BUTTON)));

        assertTrue(all.contains("· S1a"), all);
        assertTrue(all.contains("· BUT1"), all);
    }

    @Test
    void emptyList() {
        Container container = HomeworkFormatter.list(List.of(), SITE, BUTTON);
        assertTrue(sections(container).isEmpty());
        assertEquals("Rien à rendre pour l'instant 🎉", texts(container).getLast());
    }

    @Test
    void longListStaysWithinDiscordLimits() {
        List<Assignment> many = IntStream.range(0, 40)
                .mapToObj(i -> new Assignment("id" + i, "Devoir " + "z".repeat(150) + i, "R1.0" + (i % 9),
                        "d".repeat(500), "2026-09-15T21:59:00Z", S1A, null, null, false, 0))
                .toList();

        Container container = HomeworkFormatter.list(many, SITE, BUTTON);
        assertTrue(componentCount(container) <= Message.MAX_COMPONENT_COUNT_IN_COMPONENT_TREE, "components " + componentCount(container));
        int length = texts(container).stream().mapToInt(String::length).sum();
        assertTrue(length <= Message.MAX_CONTENT_LENGTH_COMPONENT_V2, "text " + length);
        assertTrue(texts(container).getLast().contains("autre(s)"), texts(container).getLast());
        assertDoesNotThrow(() -> new MessageEditBuilder().useComponentsV2().setComponents(container).build());
    }

    @Test
    void fullListOfTwelveFitsExactly() {
        List<Assignment> twelve = IntStream.range(0, HomeworkFormatter.MAX_LISTED)
                .mapToObj(i -> assignment("TP" + i, null, "2026-09-15T21:59:00Z", S1A, false))
                .toList();

        Container container = HomeworkFormatter.list(twelve, SITE, BUTTON);
        assertEquals(12, sections(container).size());
        assertTrue(componentCount(container) <= Message.MAX_COMPONENT_COUNT_IN_COMPONENT_TREE);
        assertFalse(texts(container).getLast().contains("autre(s)"));
    }

    @Test
    void announcementHasTheButtonAndTheAuthor() {
        Assignment a = new Assignment("id", "TP3", "R5.05", "Archive sur Moodle", "2026-09-15T21:59:00Z", S1A, null, null, false, 0);
        Container container = HomeworkFormatter.created(a, "<@42>", SITE, Button.success("b:id", "Fait ✓"));

        Section section = sections(container).getFirst();
        assertEquals("b:id", section.getAccessory().asButton().getCustomId());
        String all = String.join("\n", texts(container));
        assertTrue(all.contains("Nouveau devoir pour S1a"), all);
        assertTrue(all.contains("R5.05 — TP3"), all);
        assertTrue(all.contains("Archive sur Moodle"), all);
        assertTrue(all.contains("Ajouté par <@42>"), all);
        assertDoesNotThrow(() -> new MessageEditBuilder().useComponentsV2().setComponents(container).build());
    }
}
