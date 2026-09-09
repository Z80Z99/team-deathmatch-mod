package cn.blockforge.generated.generatedmod.map;

import cn.blockforge.generated.generatedmod.MinecraftTestBootstrap;
import cn.blockforge.generated.generatedmod.spawn.DynamicSpawnPool;
import cn.blockforge.generated.generatedmod.spawn.SafeSpawnFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DynamicSpawnPoolTest {
    @BeforeAll static void bootstrap() { MinecraftTestBootstrap.initialize(); }

    private static ServerLevel world() {
        ServerLevel world = mock(ServerLevel.class);
        when(world.getMinBuildHeight()).thenReturn(0);
        when(world.getMaxBuildHeight()).thenReturn(320);
        when(world.getRandom()).thenReturn(RandomSource.create(42));
        return world;
    }

    private static MapDefinition map(int size) {
        var bounds = new MapDefinition.Region(BlockPos.ZERO, new BlockPos(size, 6, size));
        return new MapDefinition("arena", "arena", Level.OVERWORLD, bounds, bounds,
                List.of(), List.of(), List.of());
    }

    @Test void destructionAndRepairAreReflectedWithoutExplosionEvents() {
        var world = world();
        var map = map(4);
        var pool = new DynamicSpawnPool();
        var first = new BlockPos(1, 1, 1);
        var repaired = new BlockPos(3, 2, 3);
        Set<BlockPos> safe = new HashSet<>(Set.of(first));
        try (var finder = mockStatic(SafeSpawnFinder.class)) {
            finder.when(() -> SafeSpawnFinder.safe(eq(world), eq(map.bounds()), any(BlockPos.class)))
                    .thenAnswer(call -> safe.contains(call.getArgument(2)));
            pool.tick(world, map, 1);
            assertEquals(1.5D, pool.find(world, map).orElseThrow().x());
            pool.requestRefresh();
            safe.clear(); // The explosion applies its delayed destruction after the death callback.
            pool.tick(world, map, 2);
            assertTrue(pool.find(world, map).isEmpty());
            safe.add(repaired);
            pool.tick(world, map, 3);
            var result = pool.find(world, map).orElseThrow();
            assertEquals(3.5D, result.x());
            assertEquals(2D, result.y());
            safe.clear(); // A later crater batch occurs after the latest pool update.
            assertTrue(pool.find(world, map).isEmpty());
        }
    }

    @Test void scansHaveFixedBudgetsAndDeathsCoalesce() {
        var world = world();
        var map = map(100);
        var pool = new DynamicSpawnPool();
        try (var finder = mockStatic(SafeSpawnFinder.class)) {
            pool.tick(world, map, 1);
            finder.verify(() -> SafeSpawnFinder.safe(eq(world), eq(map.bounds()), any(BlockPos.class)), times(256));
            finder.clearInvocations();
            pool.requestRefresh();
            pool.requestRefresh();
            pool.tick(world, map, 2);
            finder.verify(() -> SafeSpawnFinder.safe(eq(world), eq(map.bounds()), any(BlockPos.class)), times(1024));
        }
    }

    @Test void scoredSelectionPrefersDistanceButNeverUsesDestroyedCandidates() {
        var world = world();
        var map = map(4);
        var pool = new DynamicSpawnPool();
        var near = new BlockPos(1, 1, 1);
        var far = new BlockPos(3, 1, 3);
        Set<BlockPos> safe = new HashSet<>(Set.of(near, far));
        try (var finder = mockStatic(SafeSpawnFinder.class)) {
            finder.when(() -> SafeSpawnFinder.safe(eq(world), eq(map.bounds()), any(BlockPos.class)))
                    .thenAnswer(call -> safe.contains(call.getArgument(2)));
            assertEquals(3.5, pool.find(world, map, pos -> pos.distSqr(BlockPos.ZERO)).orElseThrow().x());
            safe.remove(far);
            assertEquals(1.5, pool.find(world, map, pos -> pos.distSqr(BlockPos.ZERO)).orElseThrow().x());
        }
    }

    @Test void changingWorldOrRemovingMapCannotReuseOldCandidates() {
        var world = world();
        var other = world();
        var map = map(4);
        var pool = new DynamicSpawnPool();
        try (var finder = mockStatic(SafeSpawnFinder.class)) {
            finder.when(() -> SafeSpawnFinder.safe(eq(world), eq(map.bounds()), any(BlockPos.class))).thenReturn(true);
            pool.tick(world, map, 1);
            assertTrue(pool.find(world, map).isPresent());
            assertTrue(pool.find(other, map).isEmpty());
            assertTrue(pool.find(world, null).isEmpty());
        }
    }

    @Test void matchStartRebuildDoesNotDependOnPreviouslyLoadedChunks() {
        var world = world();
        var map = map(40);
        var pool = new DynamicSpawnPool();
        var point = new cn.blockforge.generated.generatedmod.spawn.SpawnPoint(Level.OVERWORLD,
                12.5, 2, 12.5, 0, 0);
        try (var finder = mockStatic(SafeSpawnFinder.class)) {
            finder.when(() -> SafeSpawnFinder.findForMatchStart(world, map)).thenReturn(java.util.Optional.of(point));
            finder.when(() -> SafeSpawnFinder.safe(eq(world), eq(map.bounds()), any(BlockPos.class))).thenReturn(true);
            assertEquals(point, pool.rebuildForMatchStart(world, map).orElseThrow());
            assertTrue(pool.find(world, map).isPresent());
            finder.verify(() -> SafeSpawnFinder.findForMatchStart(world, map), times(1));
        }
    }
}
