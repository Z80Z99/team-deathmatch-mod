package cn.blockforge.generated.generatedmod.spawn;

import cn.blockforge.generated.generatedmod.map.MapDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import java.util.Optional;

/** Bounded search; requires a supported, unobstructed 3x3 walking area. */
public final class SafeSpawnFinder {
    private SafeSpawnFinder() { }

    public static Optional<SpawnPoint> find(ServerLevel level, MapDefinition map) {
        return find(level, map, false);
    }

    /** Start-up probe may load a bounded number of map chunks after the previous match unloaded them. */
    public static Optional<SpawnPoint> findForMatchStart(ServerLevel level, MapDefinition map) {
        return find(level, map, true);
    }

    private static Optional<SpawnPoint> find(ServerLevel level, MapDefinition map, boolean loadCandidateChunks) {
        if (level == null || !map.hasBounds()) return Optional.empty();
        var bounds = map.bounds();
        int minY = Math.max(level.getMinBuildHeight() + 1, bounds.min().getY());
        int maxY = Math.min(level.getMaxBuildHeight() - 2, bounds.max().getY() - 1);
        long width = (long) bounds.max().getX() - bounds.min().getX() - 1;
        long depth = (long) bounds.max().getZ() - bounds.min().getZ() - 1;
        if (width <= 0 || depth <= 0 || width > Integer.MAX_VALUE || depth > Integer.MAX_VALUE) return Optional.empty();
        int attempts = loadCandidateChunks ? 192 : 96;
        java.util.Set<Long> loadedChunks = loadCandidateChunks ? new java.util.HashSet<>() : java.util.Set.of();
        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = bounds.min().getX() + 1 + level.random.nextInt((int) width);
            int z = bounds.min().getZ() + 1 + level.random.nextInt((int) depth);
            if (loadCandidateChunks) {
                long chunkKey = net.minecraft.world.level.ChunkPos.asLong(x >> 4, z >> 4);
                if (loadedChunks.add(chunkKey)) {
                    if (loadedChunks.size() > 24) continue;
                    level.getChunk(x >> 4, z >> 4);
                } else if (!level.hasChunkAt(new BlockPos(x, minY, z))) {
                    continue;
                }
            }
            for (int y = maxY; y >= minY && maxY - y < 384; y--) {
                BlockPos feet = new BlockPos(x, y, z);
                if (safe(level, bounds, feet)) return Optional.of(new SpawnPoint(map.world(),
                        x + 0.5D, y, z + 0.5D, level.random.nextFloat() * 360.0F, 0));
            }
        }
        return Optional.empty();
    }

    public static boolean safe(ServerLevel level, MapDefinition.Region bounds, BlockPos feet) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            BlockPos pos = feet.offset(dx, 0, dz);
            if (!bounds.contains(pos) || !bounds.contains(pos.above()) || !level.hasChunkAt(pos)
                    || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                    || !level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()
                    || WalkNodeEvaluator.getBlockPathTypeStatic(level, pos.mutable()) != BlockPathTypes.WALKABLE
                    || !level.noCollision(new AABB(pos.getX() + 0.1D, pos.getY(), pos.getZ() + 0.1D,
                            pos.getX() + 0.9D, pos.getY() + 1.9D, pos.getZ() + 0.9D))) return false;
        }
        return true;
    }
}
