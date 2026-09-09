package cn.blockforge.generated.generatedmod.map;

import cn.blockforge.generated.generatedmod.MinecraftTestBootstrap;
import cn.blockforge.generated.generatedmod.match.Team;
import cn.blockforge.generated.generatedmod.spawn.SafeSpawnFinder;
import cn.blockforge.generated.generatedmod.spawn.SpawnManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FixedSpawnRegionTest {
    @BeforeAll static void bootstrap() { MinecraftTestBootstrap.initialize(); }

    @Test void eachTeamUsesItsOwnRegionAndRechecksTerrainBeforeRespawn() {
        MinecraftServer server = mock(MinecraftServer.class);
        ServerLevel world = mock(ServerLevel.class);
        when(server.getLevel(Level.OVERWORLD)).thenReturn(world);
        when(world.getMinBuildHeight()).thenReturn(0);
        when(world.getMaxBuildHeight()).thenReturn(320);
        when(world.getRandom()).thenReturn(RandomSource.create(42));
        var types = List.of(MapRegion.Type.SPAWN_A, MapRegion.Type.SPAWN_B,
                MapRegion.Type.SPAWN_C, MapRegion.Type.SPAWN_D);
        var regions = new ArrayList<MapRegion>();
        for (int i = 0; i < 4; i++) {
            var area = new MapDefinition.Region(new BlockPos(i * 10, 0, 0), new BlockPos(i * 10 + 4, 4, 4));
            regions.add(MapRegion.custom("spawn_" + i, "spawn", types.get(i), area));
        }
        var bounds = new MapDefinition.Region(BlockPos.ZERO, new BlockPos(40, 6, 6));
        var definition = new MapDefinition("arena", "arena", Level.OVERWORLD, bounds, bounds,
                List.of(), List.of(), List.of(), List.of(), List.of(), regions);
        MapManager maps = mock(MapManager.class);
        when(maps.currentMap()).thenReturn(Optional.of(definition));
        SpawnManager spawns = new SpawnManager(server, maps);
        AtomicBoolean destroyed = new AtomicBoolean();
        try (var finder = mockStatic(SafeSpawnFinder.class)) {
            finder.when(() -> SafeSpawnFinder.safe(eq(world), any(), any())).thenAnswer(call -> {
                MapDefinition.Region area = call.getArgument(1);
                BlockPos feet = call.getArgument(2);
                return !destroyed.get() && feet.getY() == 1 && area.contains(feet);
            });
            for (int i = 0; i < 4; i++) {
                var point = spawns.findFixedSpawn(Team.playing(4).get(i)).orElseThrow();
                assertTrue(point.x() > i * 10 && point.x() < i * 10 + 4);
            }
            destroyed.set(true);
            for (Team team : Team.playing(4)) assertTrue(spawns.findFixedSpawn(team).isEmpty());
            destroyed.set(false);
            assertTrue(spawns.findFixedSpawn(Team.TEAM_D).isPresent());
        }
    }
}
