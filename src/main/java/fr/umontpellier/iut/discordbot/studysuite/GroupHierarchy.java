package fr.umontpellier.iut.discordbot.studysuite;

import fr.umontpellier.iut.discordbot.studysuite.model.GroupRef;
import fr.umontpellier.iut.discordbot.studysuite.model.StudyEvent;
import fr.umontpellier.iut.discordbot.studysuite.model.StudyGroup;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * La hiérarchie des groupes, telle que le site la voit.
 * <p>
 * Un groupe hérite des cours de ses ancêtres : un CM de promo est rattaché à la promo, pas à chaque groupe de TP. Le
 * planning d'un groupe, c'est donc les cours de ce groupe <em>et</em> de tous ses parents.
 */
public class GroupHierarchy {
    private final Map<String, StudyGroup> byId = new LinkedHashMap<>();

    public GroupHierarchy(Collection<StudyGroup> groups) {
        groups.forEach(g -> byId.put(g.id(), g));
    }

    public Optional<StudyGroup> get(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** Les groupes proposés aux étudiants : les cachés ne servent qu'à relier les autres. */
    public List<StudyGroup> visible() {
        return byId.values().stream().filter(g -> !g.hidden()).toList();
    }

    /** {@code ids} et tous leurs ancêtres. */
    public Set<String> withAncestors(Collection<String> ids) {
        Set<String> result = new LinkedHashSet<>();
        Deque<String> toVisit = new ArrayDeque<>(ids);
        while (!toVisit.isEmpty()) {
            String id = toVisit.pop();
            // Le test évite de boucler si l'admin a créé un cycle.
            if (!result.add(id)) continue;
            StudyGroup group = byId.get(id);
            if (group != null) {
                group.parents().forEach(p -> toVisit.push(p.id()));
            }
        }
        return result;
    }

    /**
     * Retire de {@code ids} les groupes qui sont l'ancêtre d'un autre groupe de {@code ids}.
     * <p>
     * Un membre peut avoir le rôle de sa promo et celui de son TP : c'est le TP qui décrit sa classe, la promo est
     * déjà incluse dans ses ancêtres.
     */
    public Set<String> mostSpecific(Collection<String> ids) {
        Set<String> result = new LinkedHashSet<>(ids);
        for (String id : ids) {
            Set<String> ancestors = withAncestors(List.of(id));
            ancestors.remove(id);
            result.removeAll(ancestors);
        }
        return result;
    }

    /** Le groupe dont l'identifiant, le nom d'affichage ou le nom du planning vaut {@code input}, sans tenir compte des accents ni de la casse. */
    public Optional<StudyGroup> find(String input) {
        if (input == null || input.isBlank()) return Optional.empty();
        StudyGroup exact = byId.get(input.trim());
        if (exact != null) return Optional.of(exact);

        String wanted = normalize(input);
        return byId.values().stream()
                .filter(g -> normalize(g.internalName()).equals(wanted)
                        || (g.displayName() != null && normalize(g.displayName()).equals(wanted)))
                // Un nom peut désigner un groupe caché et un visible : on préfère celui que les étudiants voient.
                .min((a, b) -> Boolean.compare(a.hidden(), b.hidden()));
    }

    /** Les groupes visibles dont un des noms contient {@code input}, pour l'autocomplétion. */
    public List<StudyGroup> search(String input) {
        String wanted = normalize(input == null ? "" : input);
        return visible().stream()
                .filter(g -> normalize(g.internalName()).contains(wanted)
                        || (g.displayName() != null && normalize(g.displayName()).contains(wanted)))
                .toList();
    }

    /** Les cours qui concernent un des groupes {@code groupIds}, ancêtres compris. */
    public List<StudyEvent> eventsFor(Collection<String> groupIds, List<StudyEvent> events) {
        Set<String> scope = withAncestors(groupIds);
        return events.stream()
                .filter(e -> e.groups().stream().map(GroupRef::id).anyMatch(scope::contains))
                .toList();
    }

    /**
     * Les cours, avec leurs groupes cachés remplacés par le plus proche ancêtre visible : un cours rattaché à
     * « A1-Semestre-1 » s'affiche « BUT1 », le nom que les étudiants connaissent.
     */
    public List<StudyEvent> withVisibleGroups(List<StudyEvent> events) {
        return events.stream()
                .map(e -> new StudyEvent(e.id(), e.title(), e.startDate(), e.endDate(), e.rooms(), e.teachers(),
                        e.groups().stream().map(this::visibleRef).distinct().toList()))
                .toList();
    }

    private GroupRef visibleRef(GroupRef ref) {
        Deque<String> toVisit = new ArrayDeque<>(List.of(ref.id()));
        Set<String> seen = new LinkedHashSet<>();
        while (!toVisit.isEmpty()) {
            String id = toVisit.removeFirst();
            if (!seen.add(id)) continue;
            StudyGroup group = byId.get(id);
            if (group == null) continue;
            if (!group.hidden()) return group.asRef();
            group.parents().forEach(p -> toVisit.addLast(p.id()));
        }
        // Aucun ancêtre visible : mieux vaut le nom du planning que rien.
        return ref;
    }

    /**
     * Le lien vers le planning de ces groupes sur le site ({@code /planning?group=S1a,S1b}), avec les noms que le site
     * met lui-même dans ses liens : le nom d'affichage, sauf s'il est aussi celui d'un autre groupe.
     */
    public String planningUrl(String siteUrl, Collection<GroupRef> groups) {
        String names = groups.stream()
                .map(this::urlName)
                // %20 plutôt que + : sans ambiguïté, quel que soit le décodeur
                .map(n -> URLEncoder.encode(n, StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(Collectors.joining(","));
        return siteUrl + "/planning" + (names.isEmpty() ? "" : "?group=" + names);
    }

    private String urlName(GroupRef group) {
        String display = group.displayName() == null ? "" : group.displayName().strip();
        if (display.isEmpty()) return group.internalName();
        boolean clashes = byId.values().stream()
                .anyMatch(g -> !g.id().equals(group.id()) && normalize(g.label()).equals(normalize(display)));
        return clashes ? group.internalName() : display;
    }

    static String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
