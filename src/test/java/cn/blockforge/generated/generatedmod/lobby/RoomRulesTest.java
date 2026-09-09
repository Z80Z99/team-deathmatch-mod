package cn.blockforge.generated.generatedmod.lobby;

import cn.blockforge.generated.generatedmod.match.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoomRulesTest {
    @Test void legacySwitchesCannotOverrideModePolicies() {
        for (GameMode mode : GameMode.values()) {
            RoomRules rules = new RoomRules(mode, 25, 600, 8, 90, 5,
                    false, false, 2, 1, 5, 15, false, false, true, true,
                    TeamChangePolicy.ONLY_BEFORE_MATCH, AutoBalanceMode.OFF,
                    SpawnSelectionStrategy.FARTHEST_FROM_ENEMIES, 1).normalized();
            assertEquals(mode.respawnRules(), rules.autoRespawn());
            assertEquals(mode != GameMode.SEARCH_DESTROY, rules.keepInventoryOnDeath());
            assertTrue(rules.suppressDeathMessages());
            assertFalse(rules.requireBothTeams());
            assertEquals(10, rules.warmupDurationSeconds());
            assertEquals(2, rules.minPlayersToStart());
            assertEquals(SpawnSelectionStrategy.RANDOM, rules.spawnSelectionStrategy());
            assertEquals(mode == GameMode.SEARCH_DESTROY ? 8 : 1, rules.roundWinTarget());
            assertEquals(rules, rules.normalized());
        }
    }

    @Test void defaultsAndTemplatesAlreadyApplyPolicies() {
        assertFalse(RoomRules.fallback().requireBothTeams());
        for (GameMode mode : GameMode.values()) {
            RoomRules rules = RoomRules.templateFor(mode, RoomRules.fallback());
            assertEquals(rules.normalized(), rules);
        }
        assertEquals("团队竞技", GameMode.TEAM_DEATHMATCH.displayName());
    }
}
