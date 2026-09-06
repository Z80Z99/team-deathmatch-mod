package cn.blockforge.generated.generatedmod.match;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchManagerTest {
    @Test void activeMatchesBroadcastMoreFrequently() {
        assertEquals(10, MatchManager.stateBroadcastInterval(true));
    }

    @Test void idleMatchesUseTheLowFrequencySync() {
        assertEquals(100, MatchManager.stateBroadcastInterval(false));
    }
}
