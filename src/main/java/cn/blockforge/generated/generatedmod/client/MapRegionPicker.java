package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.map.MapDefinition;
import cn.blockforge.generated.generatedmod.map.MapEditorView;
import cn.blockforge.generated.generatedmod.map.MapRegion;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 从准星射线中找出可编辑区域，并计算画笔的可选方块。 */
public final class MapRegionPicker {
    private MapRegionPicker() {
    }

    public static List<MapRegion> pick(Minecraft minecraft, MapEditorView view, double maxDistance) {
        if (minecraft.player == null || !view.hasTarget()) return List.of();
        Vec3 start = minecraft.player.getEyePosition(1.0F);
        Vec3 direction = minecraft.player.getLookAngle();
        List<Hit> hits = new ArrayList<>();
        for (MapRegion region : view.regions()) {
            double distance = intersectionDistance(start, direction, region.region(), maxDistance);
            if (distance >= 0.0D) {
                hits.add(new Hit(region, distance, region.region().volume()));
            }
        }
        if (hits.isEmpty()) {
            for (MapRegion region : view.regions()) {
                if (region.region().contains(start.x, start.y, start.z)) {
                    hits.add(new Hit(region, 0.0D, region.region().volume()));
                }
            }
        }
        hits.sort(Comparator
                .comparingInt((Hit hit) -> hit.distance() <= 0.0D ? 1 : 0)
                .thenComparingDouble(Hit::distance)
                .thenComparingLong(Hit::volume));
        return hits.stream().map(Hit::region).toList();
    }

    public static BlockPos brushTarget(Minecraft minecraft, int range) {
        return brushTarget(minecraft, range, true);
    }

    public static BlockPos editTarget(Minecraft minecraft, MapEditorView view, boolean remove) {
        if (view.brushMode() != cn.blockforge.generated.generatedmod.map.MapBrushMode.BLOCK
                || view.selectedTool().kind() != cn.blockforge.generated.generatedmod.map.MapTool.Kind.REGION)
            return brushTarget(minecraft, view.brushRange());
        var region = view.regions().stream().filter(r -> r.id().equals(view.selectedRegionId())).findFirst();
        if (region.isEmpty() || minecraft.player == null) return brushTarget(minecraft, view.brushRange());
        Vec3 start = minecraft.player.getEyePosition(1);
        Vec3 direction = minecraft.player.getLookAngle();
        BlockPos previous = BlockPos.containing(start);
        boolean startedInside = region.get().region().contains(previous);
        for (double distance = .1; distance <= view.brushRange(); distance += .1) {
            BlockPos pos = BlockPos.containing(start.add(direction.scale(distance)));
            boolean inside = region.get().region().contains(pos);
            if (inside != startedInside) return remove ? (inside ? pos : previous) : (inside ? previous : pos);
            previous = pos;
        }
        return previous;
    }

    public static BlockPos brushTarget(Minecraft minecraft, int range, boolean allowAir) {
        if (minecraft.player == null) return allowAir ? BlockPos.ZERO : null;
        Vec3 start = minecraft.player.getEyePosition(1.0F);
        Vec3 direction = minecraft.player.getLookAngle();
        double safeRange = Math.max(1, Math.min(64, range));
        for (double distance = 0.25D; distance <= safeRange; distance += 0.25D) {
            Vec3 point = start.add(direction.scale(distance));
            BlockPos pos = BlockPos.containing(point);
            if (minecraft.level != null && !minecraft.level.getBlockState(pos).isAir()) {
                return pos;
            }
        }
        if (!allowAir) {
            return null;
        }
        return BlockPos.containing(start.add(direction.scale(safeRange)));
    }

    private static double intersectionDistance(Vec3 start, Vec3 direction,
                                               MapDefinition.Region region, double maxDistance) {
        if (!region.parts().isEmpty()) return region.parts().stream()
                .mapToDouble(part -> intersectionDistance(start, direction, part, maxDistance))
                .filter(value -> value >= 0).min().orElse(-1);
        double minX = region.min().getX();
        double minY = region.min().getY();
        double minZ = region.min().getZ();
        double maxX = region.max().getX() + 1.0D;
        double maxY = region.max().getY() + 1.0D;
        double maxZ = region.max().getZ() + 1.0D;
        double tMin = 0.0D;
        double tMax = maxDistance;
        double[] origin = {start.x, start.y, start.z};
        double[] delta = {direction.x, direction.y, direction.z};
        double[] lows = {minX, minY, minZ};
        double[] highs = {maxX, maxY, maxZ};
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(delta[axis]) < 1.0E-6D) {
                if (origin[axis] < lows[axis] || origin[axis] > highs[axis]) return -1.0D;
                continue;
            }
            double near = (lows[axis] - origin[axis]) / delta[axis];
            double far = (highs[axis] - origin[axis]) / delta[axis];
            if (near > far) {
                double temp = near;
                near = far;
                far = temp;
            }
            tMin = Math.max(tMin, near);
            tMax = Math.min(tMax, far);
            if (tMin > tMax) return -1.0D;
        }
        return tMin;
    }

    private record Hit(MapRegion region, double distance, long volume) {
    }
}
