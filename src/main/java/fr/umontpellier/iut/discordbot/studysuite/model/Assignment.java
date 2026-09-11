package fr.umontpellier.iut.discordbot.studysuite.model;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;

/**
 * Un devoir. Contrairement aux cours, {@code dueDate} est un vrai instant (UTC), pas une heure de Paris étiquetée
 * UTC.
 */
public record Assignment(
        String id,
        String title,
        @Nullable String subject,
        @Nullable String description,
        String dueDate,
        GroupRef studentGroup,
        @Nullable EventRef event,
        @Nullable Author createdBy,
        boolean completedByMe,
        int completionCount
) {
    public record EventRef(String id, String title) {
    }

    public record Author(String id, String displayName) {
    }

    public Instant due() {
        return Instant.parse(dueDate);
    }
}
