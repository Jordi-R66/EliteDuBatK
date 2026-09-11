package fr.umontpellier.iut.discordbot.studysuite;

import fr.umontpellier.iut.discordbot.studysuite.model.GroupRef;
import fr.umontpellier.iut.discordbot.studysuite.model.RoleMapping;
import fr.umontpellier.iut.discordbot.studysuite.model.StudyEvent;
import fr.umontpellier.iut.discordbot.studysuite.model.StudyGroup;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reprend la forme réelle : promo → semestre (caché) → TD → TP. */
class GroupHierarchyTest {
    static final StudyGroup PROMO = group("promo", "BUT 1A PROMO", "BUT1", false);
    static final StudyGroup SEMESTER = group("sem", "A1-Semestre-1", null, true, PROMO);
    static final StudyGroup S1 = group("s1", "S1", null, false, SEMESTER);
    static final StudyGroup S1A = group("s1a", "S1a", null, false, S1);
    static final StudyGroup SETE = group("sete", "Sète", "S-Sète", false, SEMESTER);
    static final StudyGroup OTHER_PROMO = group("promo2", "BUT 2A PROMO", "BUT2", false);

    static final GroupHierarchy HIERARCHY = new GroupHierarchy(List.of(PROMO, SEMESTER, S1, S1A, SETE, OTHER_PROMO));

    static StudyGroup group(String id, String internal, String display, boolean hidden, StudyGroup... parents) {
        return new StudyGroup(id, internal, display, hidden, java.util.Arrays.stream(parents).map(StudyGroup::asRef).toList(), List.of());
    }

    static StudyEvent event(String title, StudyGroup... groups) {
        return new StudyEvent(title, title, "2026-09-14T08:00:00.000Z", "2026-09-14T10:00:00.000Z", List.of(), List.of(),
                java.util.Arrays.stream(groups).map(StudyGroup::asRef).toList());
    }

    @Test
    void ancestorsGoUpThroughHiddenGroups() {
        assertEquals(Set.of("s1a", "s1", "sem", "promo"), HIERARCHY.withAncestors(List.of("s1a")));
    }

    @Test
    void survivesACycle() {
        StudyGroup a = new StudyGroup("a", "A", null, false, List.of(new GroupRef("b", "B", null)), List.of());
        StudyGroup b = new StudyGroup("b", "B", null, false, List.of(new GroupRef("a", "A", null)), List.of());
        assertEquals(Set.of("a", "b"), new GroupHierarchy(List.of(a, b)).withAncestors(List.of("a")));
    }

    @Test
    void mostSpecificDropsAncestors() {
        assertEquals(Set.of("s1a"), HIERARCHY.mostSpecific(List.of("promo", "s1a", "s1")));
        // Deux branches sans lien restent toutes les deux
        assertEquals(Set.of("s1a", "promo2"), HIERARCHY.mostSpecific(List.of("s1a", "promo2")));
    }

    @Test
    void groupPlanningIncludesInheritedCourses() {
        List<StudyEvent> events = List.of(
                event("CM de promo", PROMO),
                event("TD S1", S1),
                event("TP S1a", S1A),
                event("TD Sète", SETE),
                event("BUT2", OTHER_PROMO)
        );

        assertEquals(List.of("CM de promo", "TD S1", "TP S1a"),
                HIERARCHY.eventsFor(List.of("s1a"), events).stream().map(StudyEvent::title).toList());
        // Le TD ne voit pas les cours de ses TP
        assertEquals(List.of("CM de promo", "TD S1"),
                HIERARCHY.eventsFor(List.of("s1"), events).stream().map(StudyEvent::title).toList());
    }

    @Test
    void hiddenGroupsAreShownAsTheirVisibleAncestor() {
        List<StudyEvent> events = HIERARCHY.withVisibleGroups(List.of(event("CM", SEMESTER), event("TP", S1A, SEMESTER, PROMO)));

        assertEquals(List.of("BUT1"), events.get(0).groups().stream().map(GroupRef::label).toList());
        // Le semestre et la promo deviennent le même groupe : il n'apparaît qu'une fois
        assertEquals(List.of("S1a", "BUT1"), events.get(1).groups().stream().map(GroupRef::label).toList());
    }

    @Test
    void planningUrlUsesTheNamesTheSiteUses() {
        String site = "https://study-info.umontp.fr";

        assertEquals(site + "/planning?group=S1a", HIERARCHY.planningUrl(site, List.of(S1A.asRef())));
        // Le nom d'affichage, encodé
        assertEquals(site + "/planning?group=BUT1", HIERARCHY.planningUrl(site, List.of(PROMO.asRef())));
        assertEquals(site + "/planning?group=S-S%C3%A8te", HIERARCHY.planningUrl(site, List.of(SETE.asRef())));
        assertEquals(site + "/planning?group=S1a,BUT2", HIERARCHY.planningUrl(site, List.of(S1A.asRef(), OTHER_PROMO.asRef())));
        assertEquals(site + "/planning", HIERARCHY.planningUrl(site, List.of()));

        // Un nom d'affichage porté aussi par un autre groupe est ambigu : le nom du planning, lui, est unique
        StudyGroup clash = group("x", "BUT 1 bis", "BUT1", false);
        GroupHierarchy withClash = new GroupHierarchy(List.of(PROMO, clash));
        assertEquals(site + "/planning?group=BUT%201A%20PROMO", withClash.planningUrl(site, List.of(PROMO.asRef())));
    }

    @Test
    void findsByAnyNameIgnoringCaseAndAccents() {
        assertEquals("s1a", HIERARCHY.find("s1a").orElseThrow().id());
        assertEquals("promo", HIERARCHY.find("but1").orElseThrow().id());
        assertEquals("promo", HIERARCHY.find("BUT 1A PROMO").orElseThrow().id());
        assertEquals("sete", HIERARCHY.find("Sète").orElseThrow().id());
        assertEquals("sete", HIERARCHY.find("sete").orElseThrow().id());
        assertEquals("sete", HIERARCHY.find("s-sète").orElseThrow().id());
        assertEquals("s1", HIERARCHY.find("s1").orElseThrow().id());
        assertTrue(HIERARCHY.find("S7").isEmpty());
    }

    @Test
    void searchOnlyOffersVisibleGroups() {
        assertEquals(List.of("s1", "s1a"), HIERARCHY.search("s1").stream().map(StudyGroup::id).toList());
        assertTrue(HIERARCHY.search("semestre").isEmpty());
    }

    @Test
    void memberClassComesFromTheirMostPreciseRole() {
        List<RoleMapping> mappings = List.of(
                new RoleMapping("role-but1", "student", PROMO.asRef()),
                new RoleMapping("role-s1a", "student", S1A.asRef()),
                new RoleMapping("role-teacher", "teacher", null)
        );

        assertEquals(Set.of("s1a"), MemberGroups.resolve(List.of("role-but1", "role-s1a", "other"), mappings, HIERARCHY));
        assertEquals(Set.of("promo"), MemberGroups.resolve(List.of("role-but1"), mappings, HIERARCHY));
        assertTrue(MemberGroups.resolve(List.of("role-teacher", "other"), mappings, HIERARCHY).isEmpty());
    }
}
