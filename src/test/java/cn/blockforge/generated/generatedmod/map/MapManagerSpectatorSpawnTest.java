package cn.blockforge.generated.generatedmod.map;

import cn.blockforge.generated.generatedmod.MinecraftTestBootstrap;
import cn.blockforge.generated.generatedmod.spawn.SpawnPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapManagerSpectatorSpawnTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.initialize();
    }

    @Test
    void spectatorSpawnMayBeOutsideMapBoundsInTheSameDimension() {
        MapDefinition.Region bounds = new MapDefinition.Region(
                BlockPos.ZERO, new BlockPos(32, 64, 32));
        MapDefinition map = MapDefinition.incomplete("map", "Map", Level.OVERWORLD, bounds);
        SpawnPoint outside = new SpawnPoint(Level.OVERWORLD,
                new BlockPos(128, 128, 128), 0.0F);

        assertTrue(MapManager.isValidSpectatorSpawnFor(map, outside));
    }

    @Test
    void spectatorSpawnMustUseTheMapDimension() {
        MapDefinition.Region bounds = new MapDefinition.Region(
                BlockPos.ZERO, new BlockPos(32, 64, 32));
        MapDefinition map = MapDefinition.incomplete("map", "Map", Level.OVERWORLD, bounds);
        SpawnPoint nether = new SpawnPoint(Level.NETHER,
                BlockPos.ZERO, 0.0F);

        assertFalse(MapManager.isValidSpectatorSpawnFor(map, nether));
    }

    @Test
    void spectatorSpawnRequiresAMapTarget() {
        SpawnPoint point = new SpawnPoint(Level.OVERWORLD, BlockPos.ZERO, 0.0F);

        assertFalse(MapManager.isValidSpectatorSpawnFor(null, point));
    }
}
