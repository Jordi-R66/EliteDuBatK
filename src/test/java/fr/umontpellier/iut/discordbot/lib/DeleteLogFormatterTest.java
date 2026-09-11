package fr.umontpellier.iut.discordbot.lib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeleteLogFormatterTest {

    @Test
    void quotesEveryLine() {
        assertEquals("> a\n> b", DeleteLogFormatter.quote("a\nb"));
    }

    @Test
    void blankContentIsLabelled() {
        assertEquals("*(aucun contenu texte)*", DeleteLogFormatter.quote("  "));
    }

    @Test
    void shortTextIsUnchanged() {
        assertEquals("abc", DeleteLogFormatter.truncate("abc", 3));
    }

    @Test
    void longTextIsCutToMaxLength() {
        String result = DeleteLogFormatter.truncate("abcdef", 4);

        assertEquals("abc…", result);
    }

    @Test
    void quotedMultilineTextStaysUnderLimit() {
        String content = "x\n".repeat(3000);
        String result = DeleteLogFormatter.truncate(DeleteLogFormatter.quote(content), DeleteLogFormatter.MAX_TEXT_LENGTH);

        assertEquals(DeleteLogFormatter.MAX_TEXT_LENGTH, result.length());
    }

    @Test
    void doesNotSplitSurrogatePair() {
        // "a" + 😀 (2 chars) + "b" : couper à 3 tomberait au milieu de l'emoji
        String result = DeleteLogFormatter.truncate("a😀b", 3);

        assertEquals("a…", result);
    }
}
