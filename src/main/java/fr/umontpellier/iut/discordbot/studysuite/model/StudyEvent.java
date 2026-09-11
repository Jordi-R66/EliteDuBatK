package fr.umontpellier.iut.discordbot.studysuite.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Un cours du planning.
 * <p>
 * {@code startDate} et {@code endDate} sont l'heure de Paris affichée sur le planning, étiquetée UTC : un cours à
 * 10h00 arrive en {@code 10:00:00.000Z}. On les relit donc en UTC pour retrouver l'heure affichée, sans jamais les
 * convertir vers Europe/Paris (ce qui décalerait d'une ou deux heures). Pour un vrai instant (timestamps Discord),
 * c'est l'inverse : l'heure affichée est une heure de Paris, voir {@link #startInstant()}.
 */
public record StudyEvent(
        String id,
        String title,
        String startDate,
        String endDate,
        List<Room> rooms,
        List<Teacher> teachers,
        List<GroupRef> groups
) {
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    public record Room(String id, String name) {
    }

    public record Teacher(String id, String firstName, String lastName) {
    }

    public LocalDateTime start() {
        return wallClock(startDate);
    }

    public LocalDateTime end() {
        return wallClock(endDate);
    }

    /** L'instant réel du début : l'heure affichée, lue comme une heure de Paris. */
    public Instant startInstant() {
        return start().atZone(PARIS).toInstant();
    }

    public Instant endInstant() {
        return end().atZone(PARIS).toInstant();
    }

    public List<Room> rooms() {
        return rooms == null ? List.of() : rooms;
    }

    public List<Teacher> teachers() {
        return teachers == null ? List.of() : teachers;
    }

    public List<GroupRef> groups() {
        return groups == null ? List.of() : groups;
    }

    private static LocalDateTime wallClock(String label) {
        return LocalDateTime.ofInstant(Instant.parse(label), ZoneOffset.UTC);
    }
}
