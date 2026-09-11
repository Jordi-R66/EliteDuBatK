package fr.umontpellier.iut.discordbot.studysuite.model;

import org.jetbrains.annotations.Nullable;

/** Le corps de {@code POST /api/assignments}. {@code dueDate} est un instant ISO 8601. */
public record NewAssignment(
        String title,
        @Nullable String subject,
        @Nullable String description,
        String dueDate,
        String studentGroupId
) {
}
