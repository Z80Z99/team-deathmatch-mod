package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.client.screen.MapPlannerScreen;
import cn.blockforge.generated.generatedmod.map.MapEditorView;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

/** Bounded, local-only preview; closing the editor stops new particles. */
public final class MapPreview {
    private static final int MAX_EDGE_STEPS = 8;
    private static final double MAX_DISTANCE_SQUARED = 64.0D * 64.0D;

    private MapPreview() { }

    public static void tick(Minecraft minecraft) {
        if (!(minecraft.screen instanceof MapPlannerScreen) || minecraft.level == null
                || minecraft.player == null || minecraft.player.tickCount % 10 != 0) {
            return;
        }
        MapEditorView view = ClientMapEditorData.view();
        if (!view.hasTarget() || !view.canEdit() || view.locked()
                || !minecraft.level.dimension().location().toString().equals(view.world())) {
            return;
        }
        render(minecraft, view.draftBounds(), ParticleTypes.END_ROD);
        render(minecraft, view.draftResetRegion(), ParticleTypes.FLAME);
    }

    private static void render(Minecraft minecraft, MapEditorView.RegionData region, ParticleOptions particle) {
        if (!region.present()) {
            return;
        }
        double[] min = {Math.min(region.minX(), region.maxX()) + 0.5D,
                Math.min(region.minY(), region.maxY()) + 0.5D,
                Math.min(region.minZ(), region.maxZ()) + 0.5D};
        double[] max = {Math.max(region.minX(), region.maxX()) + 0.5D,
                Math.max(region.minY(), region.maxY()) + 0.5D,
                Math.max(region.minZ(), region.maxZ()) + 0.5D};
        for (int axis = 0; axis < 3; axis++) {
            int steps = edgeSteps(max[axis] - min[axis]);
            for (int edge = 0; edge < 4; edge++) {
                double[] point = min.clone();
                point[(axis + 1) % 3] = (edge & 1) == 0 ? min[(axis + 1) % 3] : max[(axis + 1) % 3];
                point[(axis + 2) % 3] = (edge & 2) == 0 ? min[(axis + 2) % 3] : max[(axis + 2) % 3];
                for (int step = 0; step <= steps; step++) {
                    point[axis] = min[axis] + (max[axis] - min[axis]) * step / steps;
                    if (minecraft.player.distanceToSqr(point[0], point[1], point[2]) <= MAX_DISTANCE_SQUARED) {
                        minecraft.level.addParticle(particle, point[0], point[1], point[2], 0.0D, 0.0D, 0.0D);
                    }
                }
            }
        }
    }

    static int edgeSteps(double span) {
        return Math.max(1, (int) Math.min(MAX_EDGE_STEPS, Math.ceil(span / 2.0D)));
    }
}
