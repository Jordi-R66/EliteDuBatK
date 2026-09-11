package fr.umontpellier.iut.discordbot.lib;

import net.dv8tion.jda.api.Permission;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditLogFormatterTest {
    private static final long SEND = Permission.MESSAGE_SEND.getRawValue();
    private static final long VIEW = Permission.VIEW_CHANNEL.getRawValue();

    @Test
    void toLongAcceptsStringsNumbersAndNull() {
        assertEquals(2048, AuditLogFormatter.toLong("2048"));
        assertEquals(2048, AuditLogFormatter.toLong(2048));
        assertEquals(0, AuditLogFormatter.toLong(null));
    }

    @Test
    void valueIsReadable() {
        assertEquals(AuditLogFormatter.NONE, AuditLogFormatter.value(null));
        assertEquals(AuditLogFormatter.NONE, AuditLogFormatter.value(""));
        assertEquals("oui", AuditLogFormatter.value(true));
        assertEquals("a → b", AuditLogFormatter.change("a", "b"));
    }

    @Test
    void permissionDiffListsAddedAndRemoved() {
        assertEquals("Ajoutées : " + Permission.MESSAGE_SEND.getName() + "\nRetirées : " + Permission.VIEW_CHANNEL.getName(),
                AuditLogFormatter.permissionDiff(VIEW, SEND));
        assertEquals("*(aucun changement)*", AuditLogFormatter.permissionDiff(SEND, SEND));
    }

    @Test
    void permissionListHandlesEmpty() {
        assertEquals("*(aucune)*", AuditLogFormatter.permissionList(0));
    }

    @Test
    void overrideDiffShowsStateTransitions() {
        // SEND : héritée → refusée ; VIEW : autorisée → autorisée (non affichée)
        assertEquals(Permission.MESSAGE_SEND.getName() + " : ➖ → ❌",
                AuditLogFormatter.overrideDiff(VIEW, 0, VIEW, SEND));
    }

    @Test
    void overrideDiffForDeletedOverride() {
        assertEquals(Permission.MESSAGE_SEND.getName() + " : ✅ → ➖",
                AuditLogFormatter.overrideDiff(SEND, 0, 0, 0));
    }

    @Test
    void roleMentionsFromAuditPayload() {
        assertEquals("<@&1>, <@&2>", AuditLogFormatter.roleMentions(List.of(Map.of("id", "1", "name", "A"), Map.of("id", "2", "name", "B"))));
        assertEquals(AuditLogFormatter.NONE, AuditLogFormatter.roleMentions(null));
    }

    @Test
    void channelTypeInFrench() {
        assertEquals("Textuel", AuditLogFormatter.channelType(0));
        assertEquals("Vocal", AuditLogFormatter.channelType("2"));
    }
}
