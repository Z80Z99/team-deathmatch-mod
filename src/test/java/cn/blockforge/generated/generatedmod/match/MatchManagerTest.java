package cn.blockforge.generated.generatedmod.match;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchManagerTest {
    @Test void warmupWaitsAndRestartsAFullCountdownAfterPlayersLeave() {
        assertEquals(0, MatchManager.warmupDeadline(0, 1, 1, 100));
        assertEquals(300, MatchManager.warmupDeadline(0, 2, 2, 100));
        assertEquals(300, MatchManager.warmupDeadline(300, 2, 2, 200));
        assertEquals(0, MatchManager.warmupDeadline(300, 1, 2, 250));
        assertEquals(600, MatchManager.warmupDeadline(0, 2, 2, 400));
        assertEquals(0, MatchManager.warmupDeadline(0, 3, 4, 400));
    }

    @Test void activeMatchesBroadcastMoreFrequently() {
        assertEquals(10, MatchManager.stateBroadcastInterval(true));
    }

    @Test void idleMatchesUseTheLowFrequencySync() {
        assertEquals(100, MatchManager.stateBroadcastInterval(false));
    }
}
