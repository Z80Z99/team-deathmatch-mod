package cn.blockforge.generated.generatedmod.spawn;

import cn.blockforge.generated.generatedmod.map.MapDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Optional;

/** Server-thread-only rolling terrain scan. Cached positions are never trusted at teleport time. */
public final class DynamicSpawnPool {
    private static final int CAPACITY = 128;
    private final LinkedHashSet<BlockPos> candidates = new LinkedHashSet<>();
    private MapDefinition map;
    private ServerLevel level;
    private int x, y, z, minY, maxY;
    private boolean refreshRequested;

    public void clear() {
        candidates.clear();
        map = null;
        level = null;
        refreshRequested = false;
    }

    private boolean bind(ServerLevel world, MapDefinition definition) {
        if (world == null || definition == null || !definition.hasBounds()) {
            clear();
            return false;
        }
        if (map != definition || level != world) {
            clear();
            map = definition;
            level = world;
            minY = Math.max(world.getMinBuildHeight() + 1, map.bounds().min().getY());
            maxY = Math.min(world.getMaxBuildHeight() - 2, map.bounds().max().getY() - 1);
            x = map.bounds().min().getX() + 1;
            z = map.bounds().min().getZ() + 1;
            y = minY;
        }
        return minY <= maxY && (long) map.bounds().max().getX() - map.bounds().min().getX() >= 2
                && (long) map.bounds().max().getZ() - map.bounds().min().getZ() >= 2;
    }

    public void requestRefresh() {
        // Damage callbacks can precede explosion block destruction. Read terrain on the following tick.
        refreshRequested = true;
    }

    public void tick(ServerLevel world, MapDefinition definition, int tick) {
        tick(world, definition, tick, 256);
    }

    public void tick(ServerLevel world, MapDefinition definition, int tick, int budget) {
        boolean urgent = refreshRequested;
        if (!bind(world, definition)) return;
        if (urgent || tick % 20 == 0) revalidate();
        refreshRequested = false;
        scan(urgent ? budget * 4 : budget);
    }

    private void revalidate() {
        candidates.removeIf(pos -> !SafeSpawnFinder.safe(level, map.bounds(), pos));
    }

    private void scan(int budget) {
        BlockPos start = new BlockPos(x, y, z);
        for (int i = 0; i < budget; i++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (SafeSpawnFinder.safe(level, map.bounds(), pos)) remember(pos);
            if (++y > maxY) {
                y = minY;
                if (++x >= map.bounds().max().getX()) {
                    x = map.bounds().min().getX() + 1;
                    if (++z >= map.bounds().max().getZ()) z = map.bounds().min().getZ() + 1;
                }
            }
            if (start.equals(new BlockPos(x, y, z))) break;
        }
    }

    private void remember(BlockPos pos) {
        if (candidates.contains(pos)) return;
        if (candidates.size() == CAPACITY) candidates.remove(candidates.iterator().next());
        candidates.add(pos);
    }

    public Optional<SpawnPoint> find(ServerLevel world, MapDefinition definition) {
        return find(world, definition, pos -> 0.0D);
    }

    public Optional<SpawnPoint> find(ServerLevel world, MapDefinition definition,
                                      java.util.function.ToDoubleFunction<BlockPos> score) {
        if (!bind(world, definition)) return Optional.empty();
        revalidate();
        scan(256);
        // Empty pools get a bounded map-wide random search, not an exhaustive synchronous scan.
        if (candidates.isEmpty()) {
            Optional<SpawnPoint> fresh = SafeSpawnFinder.find(world, definition);
            fresh.ifPresent(point -> remember(BlockPos.containing(point.x(), point.y(), point.z())));
        }
        if (candidates.isEmpty()) return Optional.empty();
        var points = new ArrayList<>(candidates);
        BlockPos pos = points.get(world.getRandom().nextInt(points.size()));
        double best = score.applyAsDouble(pos);
        for (BlockPos candidate : points) {
            double value = score.applyAsDouble(candidate);
            if (value > best) { pos = candidate; best = value; }
        }
        return Optional.of(new SpawnPoint(map.world(), pos.getX() + 0.5D, pos.getY(),
                pos.getZ() + 0.5D, world.getRandom().nextFloat() * 360.0F, 0));
    }
}
