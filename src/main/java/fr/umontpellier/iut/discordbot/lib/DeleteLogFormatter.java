package fr.umontpellier.iut.discordbot.lib;

import org.jetbrains.annotations.NotNull;

/**
 * Mise en forme du contenu des messages supprimés pour les logs.
 */
public final class DeleteLogFormatter {
	// Discord limite le texte total d'un message à composants à 4000 caractères
	public static final int MAX_TEXT_LENGTH = 3800;

	private DeleteLogFormatter() {
	}

	/**
	 * Formate un contenu en citation Markdown, ou indique qu'il n'y a pas de texte.
	 */
	@NotNull
	public static String quote(@NotNull String content) {
		if (content.isBlank()) {
			return "*(aucun contenu texte)*";
		}

		return "> " + content.replace("\n", "\n> ");
	}

	/**
	 * Tronque un texte à {@code maxLength} caractères (points de suspension compris),
	 * sans couper une paire de substitution UTF-16.
	 */
	@NotNull
	public static String truncate(@NotNull String text, int maxLength) {
		if (text.length() <= maxLength) {
			return text;
		}

		int end = maxLength - 1;
		if (Character.isHighSurrogate(text.charAt(end - 1))) {
			end--;
		}

		return text.substring(0, end) + "…";
	}
}
