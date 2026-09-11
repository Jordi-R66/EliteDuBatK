package fr.umontpellier.iut.discordbot.lib;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mise en forme des valeurs du journal d'audit Discord pour les logs.
 * <p>
 * Les valeurs d'une entrée d'audit sont celles du JSON brut : un même champ peut arriver en String ou en nombre.
 */
public final class AuditLogFormatter {
	public static final String NONE = "*(aucun)*";

	private static final String ALLOWED = "✅";
	private static final String DENIED = "❌";
	private static final String INHERITED = "➖";

	private AuditLogFormatter() {
	}

	/**
	 * Convertit une valeur brute (nombre, texte ou null) en long ; null vaut 0.
	 */
	public static long toLong(@Nullable Object value) {
		if (value == null) {
			return 0;
		}
		if (value instanceof Number number) {
			return number.longValue();
		}
		return Long.parseLong(value.toString());
	}

	/**
	 * Affiche une valeur brute de manière lisible.
	 */
	@NotNull
	public static String value(@Nullable Object value) {
		if (value == null) {
			return NONE;
		}
		if (value instanceof Boolean bool) {
			return bool ? "oui" : "non";
		}

		String text = value.toString();
		return text.isBlank() ? NONE : text;
	}

	/**
	 * @return "ancien → nouveau"
	 */
	@NotNull
	public static String change(@Nullable Object oldValue, @Nullable Object newValue) {
		return value(oldValue) + " → " + value(newValue);
	}

	/**
	 * Liste les permissions d'une valeur brute, séparées par des virgules.
	 */
	@NotNull
	public static String permissionList(long raw) {
		String names = permissions(raw).stream().map(Permission::getName).collect(Collectors.joining(", "));
		return names.isEmpty() ? "*(aucune)*" : names;
	}

	/**
	 * Décrit les permissions ajoutées et retirées entre deux valeurs brutes.
	 */
	@NotNull
	public static String permissionDiff(long oldRaw, long newRaw) {
		List<String> lines = new ArrayList<>();

		long added = newRaw & ~oldRaw;
		long removed = oldRaw & ~newRaw;
		if (added != 0) {
			lines.add("Ajoutées : " + permissionList(added));
		}
		if (removed != 0) {
			lines.add("Retirées : " + permissionList(removed));
		}

		return lines.isEmpty() ? "*(aucun changement)*" : String.join("\n", lines);
	}

	/**
	 * Décrit, permission par permission, le changement d'une surcharge de salon
	 * (✅ autorisée, ❌ refusée, ➖ héritée).
	 */
	@NotNull
	public static String overrideDiff(long oldAllow, long oldDeny, long newAllow, long newDeny) {
		List<String> lines = new ArrayList<>();

		for (Permission permission : permissions(oldAllow | oldDeny | newAllow | newDeny)) {
			String before = overrideState(permission, oldAllow, oldDeny);
			String after = overrideState(permission, newAllow, newDeny);
			if (!before.equals(after)) {
				lines.add(permission.getName() + " : " + before + " → " + after);
			}
		}

		return lines.isEmpty() ? "*(aucun changement)*" : String.join("\n", lines);
	}

	/**
	 * Liste les rôles d'une modification de rôles de membre ({@code $add} / {@code $remove}) sous forme de mentions.
	 */
	@NotNull
	public static String roleMentions(@Nullable Object roles) {
		if (!(roles instanceof List<?> list) || list.isEmpty()) {
			return NONE;
		}

		List<String> mentions = new ArrayList<>();
		for (Object role : list) {
			if (role instanceof Map<?, ?> map && map.get("id") != null) {
				mentions.add("<@&" + map.get("id") + ">");
			}
		}
		return mentions.isEmpty() ? NONE : String.join(", ", mentions);
	}

	@NotNull
	public static String channelType(@Nullable Object rawType) {
		if (rawType == null) {
			return NONE;
		}

		ChannelType type = ChannelType.fromId((int) toLong(rawType));
		return switch (type) {
			case TEXT -> "Textuel";
			case VOICE -> "Vocal";
			case CATEGORY -> "Catégorie";
			case NEWS -> "Annonces";
			case STAGE -> "Conférence";
			case FORUM -> "Forum";
			case MEDIA -> "Média";
			default -> type.name();
		};
	}

	private static String overrideState(Permission permission, long allow, long deny) {
		long bit = permission.getRawValue();
		if ((allow & bit) != 0) {
			return ALLOWED;
		}
		if ((deny & bit) != 0) {
			return DENIED;
		}
		return INHERITED;
	}

	private static EnumSet<Permission> permissions(long raw) {
		EnumSet<Permission> permissions = Permission.getPermissions(raw);
		permissions.remove(Permission.UNKNOWN);
		return permissions;
	}
}
