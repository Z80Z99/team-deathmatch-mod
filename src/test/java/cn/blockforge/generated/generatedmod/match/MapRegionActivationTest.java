package cn.blockforge.generated.generatedmod.match;

import cn.blockforge.generated.generatedmod.map.MapDefinition;
import cn.blockforge.generated.generatedmod.map.MapRegion;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapRegionActivationTest {
    @BeforeAll
    static void bootstrap() {
        cn.blockforge.generated.generatedmod.MinecraftTestBootstrap.initialize();
    }

    @Test
    void evaluatesBuiltInActivationModes() {
        var active = new MapRegionActivation.Context(true, GameMode.SEARCH_DESTROY, 2, 2, 8);
        var inactive = new MapRegionActivation.Context(false, GameMode.TEAM_DEATHMATCH, 0, 2, 8);

        assertTrue(MapRegionActivation.isActive(region("always", MapRegion.Activation.ALWAYS, ""), active));
        assertTrue(MapRegionActivation.isActive(region("always", MapRegion.Activation.ALWAYS, ""), inactive));
        assertTrue(MapRegionActivation.isActive(region("match", MapRegion.Activation.MATCH_ONLY, ""), active));
        assertFalse(MapRegionActivation.isActive(region("match", MapRegion.Activation.MATCH_ONLY, ""), inactive));
        assertTrue(MapRegionActivation.isActive(region("mode", MapRegion.Activation.MODE, "SEARCH_DESTROY"), active));
        assertTrue(MapRegionActivation.isActive(region("mode", MapRegion.Activation.MODE, "爆破模式"), active));
        assertTrue(MapRegionActivation.isActive(region("mode", MapRegion.Activation.MODE, "bomb"), active));
        assertFalse(MapRegionActivation.isActive(region("mode", MapRegion.Activation.MODE, "TEAM_DEATHMATCH"), active));
    }

    @Test
    void evaluatesConditionExpressions() {
        var context = new MapRegionActivation.Context(true, GameMode.SEARCH_DESTROY, 2, 2, 8);

        assertTrue(MapRegionActivation.isActive(region("condition", MapRegion.Activation.CONDITION,
                "match && mode == \"SEARCH_DESTROY\""), context));
        assertTrue(MapRegionActivation.isActive(region("condition", MapRegion.Activation.CONDITION,
                "round >= 2 && teams >= 2 && players > 3"), context));
        assertTrue(MapRegionActivation.isActive(region("condition", MapRegion.Activation.CONDITION,
                "!false || (mode != \"TEAM_DEATHMATCH\")"), context));
        assertFalse(MapRegionActivation.isActive(region("condition", MapRegion.Activation.CONDITION,
                "round < 2 || players <= 3"), context));
        assertFalse(MapRegionActivation.isActive(region("condition", MapRegion.Activation.CONDITION,
                "unknown_variable"), context));
        assertFalse(MapRegionActivation.isActive(region("condition", MapRegion.Activation.CONDITION, ""), context));
    }

    @Test
    void filtersOnlyActiveRegions() {
        var context = new MapRegionActivation.Context(true, GameMode.TEAM_DEATHMATCH, 1, 2, 6);
        MapRegion always = region("always", MapRegion.Activation.ALWAYS, "");
        MapRegion matchOnly = region("match_only", MapRegion.Activation.MATCH_ONLY, "");
        MapRegion otherMode = region("other_mode", MapRegion.Activation.MODE, "SEARCH_DESTROY");

        assertEquals(List.of(always, matchOnly),
                MapRegionActivation.activeRegions(List.of(always, matchOnly, otherMode), context));
    }

    private static MapRegion region(String id, MapRegion.Activation activation, String activationValue) {
        return new MapRegion(id, id, MapRegion.Type.SPAWN_A,
                new MapDefinition.Region(BlockPos.ZERO, BlockPos.ZERO),
                true, 64, MapRegion.Appearance.ALWAYS, activation, activationValue,
                0, true, true, 0, "");
    }
}
