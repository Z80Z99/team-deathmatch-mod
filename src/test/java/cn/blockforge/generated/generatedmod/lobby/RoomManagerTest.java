package cn.blockforge.generated.generatedmod.lobby;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomManagerTest {
    @Test void matchmakingCountdownContinuesWhenTheMinimumStillExists() {
        assertFalse(RoomManager.needsCountdownCancellation(true, true));
    }

    @Test void matchmakingCountdownCancelsBelowTheMinimum() {
        assertTrue(RoomManager.needsCountdownCancellation(true, false));
    }

    @Test void ordinaryRoomReturnsToOpenWhenStartIsInterrupted() {
        assertTrue(RoomManager.needsCountdownCancellation(false, true));
    }
}
