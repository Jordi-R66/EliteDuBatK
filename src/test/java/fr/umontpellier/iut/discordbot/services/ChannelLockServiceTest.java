package fr.umontpellier.iut.discordbot.services;

import net.dv8tion.jda.api.Permission;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChannelLockServiceTest {
    private static final long SEND = Permission.MESSAGE_SEND.getRawValue();
    private static final long VIEW = Permission.VIEW_CHANNEL.getRawValue();
    private static final long ATTACH = Permission.MESSAGE_ATTACH_FILES.getRawValue();

    @Test
    void restoresLockedBitsFromOriginal() {
        // Refusé pendant le verrouillage, autorisé à l'origine
        assertEquals(SEND, ChannelLockService.restoreLockedBits(0, SEND));
        assertEquals(0, ChannelLockService.restoreLockedBits(SEND, 0));
    }

    @Test
    void keepsOtherBitsChangedDuringLock() {
        // VIEW ajouté pendant le verrouillage : il doit rester
        assertEquals(VIEW | SEND, ChannelLockService.restoreLockedBits(VIEW, SEND));
    }

    @Test
    void ignoresNonLockedBitsOfOriginal() {
        // ATTACH retiré pendant le verrouillage : il ne doit pas revenir
        assertEquals(SEND, ChannelLockService.restoreLockedBits(0, SEND | ATTACH));
    }

    @Test
    void isIdempotent() {
        long once = ChannelLockService.restoreLockedBits(VIEW, SEND);

        assertEquals(once, ChannelLockService.restoreLockedBits(once, SEND));
    }
}
