package fr.umontpellier.iut.discordbot.studysuite;

import fr.umontpellier.iut.discordbot.studysuite.model.RoleMapping;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/** La classe d'un membre, lue depuis ses rôles Discord et les associations rôle → groupe de StudySuite. */
public final class MemberGroups {
    private MemberGroups() {
    }

    /**
     * Les groupes les plus précis parmi ceux que donnent les rôles du membre : avec les rôles « BUT1 » et « S1a »,
     * c'est S1a (BUT1 en est un ancêtre, ses cours sont inclus quand même).
     */
    public static Set<String> resolve(Collection<String> memberRoleIds, List<RoleMapping> mappings, GroupHierarchy hierarchy) {
        List<String> groupIds = mappings.stream()
                .filter(m -> m.studentGroup() != null && memberRoleIds.contains(m.discordRoleId()))
                .map(m -> m.studentGroup().id())
                .distinct()
                .toList();
        return hierarchy.mostSpecific(groupIds);
    }
}
