package cn.blockforge.generated.generatedmod.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RespawnTimelineTest {
    @Test void immediateRespawnDoesNotJumpToFullOpacity() {
        var timeline = new RespawnTimeline();
        timeline.update(true, true, false, 0);
        float before = timeline.fade(0.05);
        timeline.update(true, false, true, 0.05);
        assertEquals(before, timeline.fade(0.05), 0.0001);
        assertTrue(timeline.fade(0.2) < before);
    }
    @Test void customHudStillShowsRespawningWhileSafetySearchHasNoTimeRemaining() {
        try {
            ClientMatchData.myTeam = cn.blockforge.generated.generatedmod.match.Team.TEAM_A;
            ClientMatchData.awaitingRespawn = true;
            ClientMatchData.respawnRemainingTicks = 0;
            assertTrue(HudParameters.visible("respawning", false));
            assertFalse(HudParameters.visible("alive", false));
        } finally { ClientMatchData.clear(); }
    }
    @Test void zeroTimerDoesNotReleaseAnAuthoritativeWait() {
        var timeline = new RespawnTimeline();
        timeline.update(true, true, true, 0);
        timeline.update(true, true, true, 100);
        assertEquals(RespawnTimeline.Phase.WAITING, timeline.phase());
        assertEquals(1F, RespawnTimeline.progress(0, 100));
    }

    @Test void duplicateSnapshotsDoNotRestartFade() {
        var timeline = new RespawnTimeline();
        timeline.update(true, true, false, 10);
        timeline.update(true, true, true, 11);
        assertEquals(1, timeline.age(11));
        assertEquals(1F, timeline.fade(11));
    }

    @Test void serverReleaseAndLivePlayerAreBothRequired() {
        var timeline = new RespawnTimeline();
        timeline.update(true, true, false, 10);
        timeline.update(true, false, false, 12);
        assertEquals(RespawnTimeline.Phase.WAITING, timeline.phase());
        timeline.update(true, false, true, 13);
        assertEquals(RespawnTimeline.Phase.RETURNING, timeline.phase());
        assertEquals(1F, timeline.fade(13));
        assertEquals(0.5F, timeline.fade(13.35), 0.001);
        timeline.update(true, false, true, 14);
        assertEquals(RespawnTimeline.Phase.HIDDEN, timeline.phase());
    }

    @Test void provisionalDeathSurvivesPacketDelayButClearsWhenAlive() {
        var timeline = new RespawnTimeline();
        timeline.provisionalDeath(0);
        timeline.update(true, false, true, 0.5);
        assertEquals(RespawnTimeline.Phase.WAITING, timeline.phase());
        timeline.update(true, false, true, 2);
        assertEquals(RespawnTimeline.Phase.RETURNING, timeline.phase());
    }

    @Test void newDeathInterruptsReturnAndLeavingCombatClearsEverything() {
        var timeline = new RespawnTimeline();
        timeline.update(true, true, false, 0);
        timeline.update(true, false, true, 1);
        timeline.update(true, true, false, 1.1);
        assertEquals(RespawnTimeline.Phase.WAITING, timeline.phase());
        assertEquals(0, timeline.age(1.1));
        timeline.update(false, true, false, 2);
        assertEquals(RespawnTimeline.Phase.HIDDEN, timeline.phase());
        assertEquals(0, timeline.fade(2));
    }
}
