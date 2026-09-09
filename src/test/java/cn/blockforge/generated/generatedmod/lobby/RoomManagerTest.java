package cn.blockforge.generated.generatedmod.lobby;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomManagerTest {
    @Test void hostCanStartOrdinaryRoomAloneWithoutEveryTeamBeingFilled() throws Exception {
        var defaults = RoomRules.fallback();
        try (var rules = org.mockito.Mockito.mockStatic(RoomRules.class)) {
            rules.when(RoomRules::serverDefaults).thenReturn(defaults);
            var room = new Room("room", "room", java.util.UUID.randomUUID(), 8, "arena", false);
            var manager = org.mockito.Mockito.mock(RoomManager.class, org.mockito.Mockito.CALLS_REAL_METHODS);
            var method = RoomManager.class.getDeclaredMethod("canStart", Room.class);
            method.setAccessible(true);
            assertTrue((boolean) method.invoke(manager, room));
        }
    }

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
