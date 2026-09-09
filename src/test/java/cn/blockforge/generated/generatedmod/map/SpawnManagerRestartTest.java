package cn.blockforge.generated.generatedmod.map;

import cn.blockforge.generated.generatedmod.MinecraftTestBootstrap;
import cn.blockforge.generated.generatedmod.spawn.DynamicSpawnPool;
import cn.blockforge.generated.generatedmod.spawn.SafeSpawnFinder;
import cn.blockforge.generated.generatedmod.spawn.SpawnManager;
import cn.blockforge.generated.generatedmod.spawn.SpawnPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SpawnManagerRestartTest {
    @BeforeAll static void bootstrap() { MinecraftTestBootstrap.initialize(); }

    @Test void restoredMapReusesVerifiedSpawnWithoutAnotherRandomProbe() throws Exception {
        var server = mock(MinecraftServer.class);
        var level = mock(ServerLevel.class);
        var maps = mock(MapManager.class);
        var bounds = new MapDefinition.Region(BlockPos.ZERO, new BlockPos(32, 8, 32));
        var map = new MapDefinition("arena", "arena", Level.OVERWORLD, bounds, bounds,
                List.of(), List.of(), List.of());
        var point = new SpawnPoint(Level.OVERWORLD, 8.5, 2, 8.5, 0, 0);
        when(maps.currentMap()).thenReturn(Optional.of(map));
        when(server.getLevel(Level.OVERWORLD)).thenReturn(level);
        when(level.getMinBuildHeight()).thenReturn(0);
        when(level.getMaxBuildHeight()).thenReturn(320);
        when(level.getWorldBorder()).thenReturn(new WorldBorder());
        when(level.hasChunkAt(any(BlockPos.class))).thenReturn(true);
        when(level.getBlockState(any(BlockPos.class))).thenReturn(Blocks.STONE.defaultBlockState());
        when(level.getFluidState(any(BlockPos.class))).thenReturn(Fluids.EMPTY.defaultFluidState());
        when(level.noCollision(any(AABB.class))).thenReturn(true);
        SpawnManager manager = new SpawnManager(server, maps);
        Field poolField = SpawnManager.class.getDeclaredField("randomSpawns");
        poolField.setAccessible(true);
        DynamicSpawnPool pool = (DynamicSpawnPool) poolField.get(manager);
        var restored = new java.util.concurrent.atomic.AtomicBoolean(false);
        try (var finder = mockStatic(SafeSpawnFinder.class)) {
            finder.when(() -> SafeSpawnFinder.find(level, map)).thenReturn(Optional.empty());
            finder.when(() -> SafeSpawnFinder.findForMatchStart(level, map)).thenReturn(Optional.of(point));
            finder.when(() -> SafeSpawnFinder.safe(any(ServerLevel.class),
                    any(MapDefinition.Region.class), any(BlockPos.class))).thenAnswer(call -> restored.get());
            assertEquals(point, manager.prepareRandomSpawnForMatch().orElseThrow());
            pool.clear(); // End-of-match cleanup removes the rolling pool, not the verified map point.
            restored.set(true);
            assertEquals(point, manager.prepareRandomSpawnForMatch().orElseThrow());
            finder.verify(() -> SafeSpawnFinder.findForMatchStart(level, map), times(1));
            verify(level, atLeastOnce()).getChunk(0, 0);
        }
    }

    @Test void equivalentDefinitionReloadKeepsVerifiedSpawnButRealEditInvalidatesIt() throws Exception {
        var server = mock(MinecraftServer.class);
        var level = mock(ServerLevel.class);
        var maps = mock(MapManager.class);
        var bounds = new MapDefinition.Region(BlockPos.ZERO, new BlockPos(32, 8, 32));
        var original = new MapDefinition("arena", "arena", Level.OVERWORLD, bounds, bounds,
                List.of(), List.of(), List.of());
        var equivalent = new MapDefinition("arena", "arena", Level.OVERWORLD, bounds, bounds,
                List.of(), List.of(), List.of());
        var changedBounds = new MapDefinition.Region(BlockPos.ZERO, new BlockPos(48, 8, 48));
        var changed = new MapDefinition("arena", "arena", Level.OVERWORLD, changedBounds, changedBounds,
                List.of(), List.of(), List.of());
        var point = new SpawnPoint(Level.OVERWORLD, 8.5, 2, 8.5, 0, 0);
        var selected = new java.util.concurrent.atomic.AtomicReference<>(original);
        when(maps.currentMap()).thenAnswer(call -> Optional.of(selected.get()));
        when(server.getLevel(Level.OVERWORLD)).thenReturn(level);
        when(level.getMinBuildHeight()).thenReturn(0);
        when(level.getMaxBuildHeight()).thenReturn(320);
        when(level.hasChunkAt(any(BlockPos.class))).thenReturn(true);
        var restored = new java.util.concurrent.atomic.AtomicBoolean(false);
        SpawnManager manager = new SpawnManager(server, maps);
        try (var finder = mockStatic(SafeSpawnFinder.class)) {
            finder.when(() -> SafeSpawnFinder.safe(any(ServerLevel.class),
                    any(MapDefinition.Region.class), any(BlockPos.class))).thenAnswer(call -> restored.get());
            finder.when(() -> SafeSpawnFinder.find(level, original)).thenReturn(Optional.empty());
            finder.when(() -> SafeSpawnFinder.findForMatchStart(level, original)).thenReturn(Optional.of(point));
            finder.when(() -> SafeSpawnFinder.find(level, changed)).thenReturn(Optional.empty());
            finder.when(() -> SafeSpawnFinder.findForMatchStart(level, changed)).thenReturn(Optional.empty());
            assertEquals(point, manager.prepareRandomSpawnForMatch().orElseThrow());
            restored.set(true);
            selected.set(equivalent);
            assertEquals(point, manager.prepareRandomSpawnForMatch().orElseThrow());
            restored.set(false);
            selected.set(changed);
            org.junit.jupiter.api.Assertions.assertTrue(manager.prepareRandomSpawnForMatch().isEmpty());
        }
    }
}
