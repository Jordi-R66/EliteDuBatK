package fr.umontpellier.iut.discordbot.studysuite.model;

public record GroupRef(String id, String internalName, String displayName) {
    /** Le nom montré aux étudiants : le nom d'affichage s'il y en a un, sinon le nom du planning. */
    public String label() {
        return displayName == null || displayName.isBlank() ? internalName : displayName;
    }
}
