package cn.blockforge.generated.generatedmod.map;

import cn.blockforge.generated.generatedmod.MinecraftTestBootstrap;
import cn.blockforge.generated.generatedmod.spawn.SafeSpawnFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SafeSpawnFinderTest {
    @BeforeAll static void bootstrap() { MinecraftTestBootstrap.initialize(); }

    @Test void rejectsBlockedHeadUnsupportedGroundAndHazardousPaths() {
        var level = mock(ServerLevel.class);
        var border = new WorldBorder();
        when(level.getWorldBorder()).thenReturn(border);
        when(level.hasChunkAt(any(BlockPos.class))).thenReturn(true);
        when(level.getBlockState(any(BlockPos.class))).thenReturn(Blocks.STONE.defaultBlockState());
        when(level.getFluidState(any(BlockPos.class))).thenReturn(Fluids.EMPTY.defaultFluidState());
        when(level.noCollision(any(AABB.class))).thenReturn(true);
        var bounds = new MapDefinition.Region(new BlockPos(-4, 0, -4), new BlockPos(4, 8, 4));
        var feet = new BlockPos(0, 2, 0);
        try (var paths = mockStatic(WalkNodeEvaluator.class)) {
            paths.when(() -> WalkNodeEvaluator.getBlockPathTypeStatic(eq(level), any(BlockPos.MutableBlockPos.class)))
                    .thenReturn(BlockPathTypes.WALKABLE);
            assertTrue(SafeSpawnFinder.safe(level, bounds, feet));
            when(level.noCollision(any(AABB.class))).thenReturn(false);
            assertFalse(SafeSpawnFinder.safe(level, bounds, feet));
            when(level.noCollision(any(AABB.class))).thenReturn(true);
            when(level.getBlockState(feet.below())).thenReturn(Blocks.AIR.defaultBlockState());
            assertFalse(SafeSpawnFinder.safe(level, bounds, feet));
            when(level.getBlockState(feet.below())).thenReturn(Blocks.STONE.defaultBlockState());
            paths.when(() -> WalkNodeEvaluator.getBlockPathTypeStatic(eq(level), any(BlockPos.MutableBlockPos.class)))
                    .thenReturn(BlockPathTypes.DAMAGE_FIRE);
            assertFalse(SafeSpawnFinder.safe(level, bounds, feet));
        }
    }
}
