package fr.umontpellier.iut.discordbot.studysuite.model;

import java.util.List;

/**
 * Un groupe d'étudiants et ses liens : une promo contient des semestres, qui contiennent des groupes de TD, qui
 * contiennent des groupes de TP. Les groupes cachés ({@code hidden}) existent quand même comme parents.
 */
public record StudyGroup(
        String id,
        String internalName,
        String displayName,
        boolean hidden,
        List<GroupRef> parents,
        List<GroupRef> children
) {
    public String label() {
        return asRef().label();
    }

    public GroupRef asRef() {
        return new GroupRef(id, internalName, displayName);
    }

    public List<GroupRef> parents() {
        return parents == null ? List.of() : parents;
    }

    public List<GroupRef> children() {
        return children == null ? List.of() : children;
    }
}
