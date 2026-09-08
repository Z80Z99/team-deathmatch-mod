package cn.blockforge.generated.generatedmod.map;

import cn.blockforge.generated.generatedmod.match.Team;
import cn.blockforge.generated.generatedmod.spawn.SpawnPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapRegionTest {
    @org.junit.jupiter.api.BeforeAll
    static void bootstrap() {
        cn.blockforge.generated.generatedmod.MinecraftTestBootstrap.initialize();
    }

    @Test
    void regionRoundTripsThroughJson() {
        MapDefinition.Region bounds = new MapDefinition.Region(
                new BlockPos(-4, 32, -5), new BlockPos(19, 48, 22));
        MapRegion region = new MapRegion("bomb_site_a", "A 点", MapRegion.Type.BOMB, bounds,
                false, 128, MapRegion.Appearance.NEARBY, MapRegion.Activation.MODE, "bomb",
                0xFF4070FF, false, true, 7, "测试备注");

        MapRegion decoded = MapRegion.fromJson(region.toJson());

        assertEquals(region, decoded);
    }

    @Test
    void customRegionsSurviveSpawnAndRegionEditsAndJson() {
        MapDefinition.Region bounds = new MapDefinition.Region(
                new BlockPos(0, 0, 0), new BlockPos(32, 32, 32));
        MapRegion region = MapRegion.custom("capture_center", "中心点",
                MapRegion.Type.CAPTURE, bounds);
        MapDefinition base = new MapDefinition("map", "map", Level.OVERWORLD, bounds, bounds,
                List.of(), List.of(), List.of());
        SpawnPoint point = new SpawnPoint(Level.OVERWORLD, 10, 10, 10, 0, 0);
        MapDefinition.Region nextBounds = new MapDefinition.Region(
                new BlockPos(1, 1, 1), new BlockPos(31, 31, 31));

        MapDefinition edited = base.withRegion(region)
                .withRegions(nextBounds, nextBounds)
                .withTeamSpawns(Team.TEAM_A, List.of(point));

        assertEquals(List.of(region), edited.customRegions());
        assertEquals(List.of(region), edited.regions().stream()
                .filter(candidate -> candidate.type() == MapRegion.Type.CAPTURE).toList());
        assertTrue(edited.sameConfiguration(MapDefinition.fromJson(edited.toJson(), "map")));
    }

    @Test
    void blankMapRequiresAuthorCreatedFoundationRegionsAndCanDeleteThem() {
        MapDefinition.Region point = new MapDefinition.Region(BlockPos.ZERO, BlockPos.ZERO);
        MapDefinition blank = MapDefinition.incomplete("blank", "Blank", Level.OVERWORLD, point);
        assertTrue(blank.regions().isEmpty());
        assertTrue(!blank.isComplete());

        MapDefinition withBounds = blank.withRegion(MapRegion.builtIn("bounds", "地图边界",
                MapRegion.Type.BOUNDS, point, MapRegion.Type.BOUNDS.defaultColor()));
        assertTrue(withBounds.hasBounds());
        assertTrue(!withBounds.hasResetRegion());
        MapDefinition complete = withBounds.withRegion(MapRegion.builtIn("reset", "重置区域",
                MapRegion.Type.RESET, point, MapRegion.Type.RESET.defaultColor()));
        assertTrue(complete.isComplete());

        MapDefinition decoded = MapDefinition.fromJson(complete.withoutRegion("bounds").toJson(), "blank");
        assertEquals("blank", decoded.id());
        assertEquals("blank", complete.withoutRegion("reset").id());
        MapRegion custom = MapRegion.custom("zone", "zone", MapRegion.Type.CUSTOM, point);
        MapDefinition removedCustom = complete.withRegion(custom).withoutRegion("zone");
        assertEquals("blank", removedCustom.id());
        assertTrue(removedCustom.customRegions().isEmpty());
        assertTrue(removedCustom.isComplete());
        assertTrue(!decoded.hasBounds());
        assertTrue(decoded.hasResetRegion());
        assertTrue(!decoded.isComplete());
    }
}
