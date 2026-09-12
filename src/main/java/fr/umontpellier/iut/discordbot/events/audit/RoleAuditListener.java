package fr.umontpellier.iut.discordbot.events.audit;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.config.ConfigStructure.SystemChannel;
import fr.umontpellier.iut.discordbot.lib.AuditLogFormatter;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogChange;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Création, modification (dont permissions) et suppression de rôles.
 */
public class RoleAuditListener extends AbstractAuditLogListener {
	private static final Map<String, String> LABELS = Map.of(
			"name", "Nom",
			"color", "Couleur",
			"hoist", "Affiché séparément",
			"mentionable", "Mentionnable",
			"icon_hash", "Icône",
			"unicode_emoji", "Émoji"
	);

	private static final Set<String> IGNORED = Set.of("permissions", "id", "colors", "flags");

	public RoleAuditListener(Bot bot) {
		super(bot, Set.of(ActionType.ROLE_CREATE, ActionType.ROLE_UPDATE, ActionType.ROLE_DELETE));
	}

	@Override
	protected void handle(@NotNull AuditLogEntry entry) {
		String role = "<@&" + entry.getTargetId() + ">";

		switch (entry.getType()) {
			case ROLE_CREATE -> sendLog(SystemChannel.ROLE_LOG_CHANNEL, "# ➕ Rôle créé", 0x00FF00,
					"Rôle : " + role
							+ "\nNom : " + AuditLogFormatter.value(newValue(entry, "name"))
							+ "\nPermissions : " + AuditLogFormatter.permissionList(AuditLogFormatter.toLong(newValue(entry, "permissions")))
							+ footer(entry));
			case ROLE_DELETE -> sendLog(SystemChannel.ROLE_LOG_CHANNEL, "# ➖ Rôle supprimé", 0xFF0000,
					"Nom : " + AuditLogFormatter.value(oldValue(entry, "name"))
							+ "\nPermissions : " + AuditLogFormatter.permissionList(AuditLogFormatter.toLong(oldValue(entry, "permissions")))
							+ footer(entry));
			case ROLE_UPDATE -> {
				List<String> changes = describeChanges(entry);
				if (changes.isEmpty()) {
					return;
				}

				sendLog(SystemChannel.ROLE_LOG_CHANNEL, "# ✏️ Rôle modifié", 0xFFFF00,
						"Rôle : " + role + "\n\n" + String.join("\n", changes) + footer(entry));
			}
			default -> {
			}
		}
	}

	private List<String> describeChanges(AuditLogEntry entry) {
		List<String> lines = new ArrayList<>();

		for (AuditLogChange change : entry.getChanges().values()) {
			String key = change.getKey();
			if (IGNORED.contains(key)) {
				continue;
			}

			Object before = change.getOldValue();
			Object after = change.getNewValue();
			if ("color".equals(key)) {
				before = color(before);
				after = color(after);
			}

			lines.add(LABELS.getOrDefault(key, key) + " : " + AuditLogFormatter.change(before, after));
		}

		AuditLogChange permissions = entry.getChangeByKey("permissions");
		if (permissions != null) {
			lines.add("Permissions :\n" + AuditLogFormatter.permissionDiff(
					AuditLogFormatter.toLong(permissions.getOldValue()),
					AuditLogFormatter.toLong(permissions.getNewValue())
			));
		}

		return lines;
	}

	private static String color(Object raw) {
		long value = AuditLogFormatter.toLong(raw);
		return value == 0 ? "aucune" : String.format("#%06X", value);
	}
}
