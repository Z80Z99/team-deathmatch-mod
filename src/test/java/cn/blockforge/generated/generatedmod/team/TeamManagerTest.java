package cn.blockforge.generated.generatedmod.team;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamManagerTest {
    @Test void strictBalanceAllowsTheUnavoidableOddPlayer() {
        assertEquals(1, TeamManager.effectiveImbalance(7, 0));
    }

    @Test void configuredImbalanceIsStillHonored() {
        assertEquals(3, TeamManager.effectiveImbalance(8, 3));
        assertEquals(0, TeamManager.effectiveImbalance(8, -1));
    }
}
