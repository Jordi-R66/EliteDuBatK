package fr.umontpellier.iut.discordbot.studysuite.model;

import org.jetbrains.annotations.Nullable;

/** Un rôle Discord associé à un groupe sur StudySuite (géré depuis l'admin du site). */
public record RoleMapping(String discordRoleId, String userRole, @Nullable GroupRef studentGroup) {
}
