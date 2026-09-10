package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Animated hazard stripes reveal only the spherical patch of boundary nearest the player's feet. */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BoundaryEdgeRenderer {
    private static final double RANGE = 4.0D;
    private static final double STRIPE_GAP = 0.72D;
    private static final double STRIPE_WIDTH = 0.18D;
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(
            new com.mojang.blaze3d.vertex.BufferBuilder(4096));

    private BoundaryEdgeRenderer() { }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || !ClientMatchData.inMatch() || !ClientMatchData.boundaryBoxPresent) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        double x = minecraft.player.getX(), y = minecraft.player.getY(), z = minecraft.player.getZ();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        VertexConsumer stripes = BUFFERS.getBuffer(RenderType.debugQuads());
        RenderSystem.disableDepthTest();
        double minX = ClientMatchData.boundaryMinX, minY = ClientMatchData.boundaryMinY;
        double minZ = ClientMatchData.boundaryMinZ;
        double maxX = ClientMatchData.boundaryMaxX + 1.0D;
        double maxY = ClientMatchData.boundaryMaxY + 2.0D;
        double maxZ = ClientMatchData.boundaryMaxZ + 1.0D;
        double animation = (System.nanoTime() / 1_000_000_000.0D * 0.55D) % STRIPE_GAP;
        stripePatchX(pose, stripes, minX, z, y, radius(x, minX), minZ, maxZ, minY, maxY, animation);
        stripePatchX(pose, stripes, maxX, z, y, radius(x, maxX), minZ, maxZ, minY, maxY, animation);
        stripePatchZ(pose, stripes, minZ, x, y, radius(z, minZ), minX, maxX, minY, maxY, animation);
        stripePatchZ(pose, stripes, maxZ, x, y, radius(z, maxZ), minX, maxX, minY, maxY, animation);
        stripePatchY(pose, stripes, minY, x, z, radius(y, minY), minX, maxX, minZ, maxZ, animation);
        stripePatchY(pose, stripes, maxY, x, z, radius(y, maxY), minX, maxX, minZ, maxZ, animation);
        BUFFERS.endBatch();
        RenderSystem.enableDepthTest();
        pose.popPose();
    }

    private static double radius(double coordinate, double plane) {
        double distance = Math.abs(coordinate - plane);
        return distance >= RANGE ? 0.0D : Math.sqrt(RANGE * RANGE - distance * distance);
    }

    private static float alpha(double radius) {
        double perpendicular = Math.sqrt(Math.max(0.0D, RANGE * RANGE - radius * radius));
        float near = (float) (1.0D - perpendicular / RANGE);
        return 0.10F + 0.55F * near * near;
    }

    private static void stripePatchX(PoseStack pose, VertexConsumer out, double fixed,
                                     double centerU, double centerV, double radius,
                                     double minU, double maxU, double minV, double maxV, double animation) {
        stripes(centerU, centerV, radius, minU, maxU, minV, maxV, animation,
                (u, v, a) -> vertex(pose, out, fixed, v, u, a));
    }

    private static void stripePatchZ(PoseStack pose, VertexConsumer out, double fixed,
                                     double centerU, double centerV, double radius,
                                     double minU, double maxU, double minV, double maxV, double animation) {
        stripes(centerU, centerV, radius, minU, maxU, minV, maxV, animation,
                (u, v, a) -> vertex(pose, out, u, v, fixed, a));
    }

    private static void stripePatchY(PoseStack pose, VertexConsumer out, double fixed,
                                     double centerU, double centerV, double radius,
                                     double minU, double maxU, double minV, double maxV, double animation) {
        stripes(centerU, centerV, radius, minU, maxU, minV, maxV, animation,
                (u, v, a) -> vertex(pose, out, u, fixed, v, a));
    }

    private static void stripes(double centerU, double centerV, double radius,
                                double minU, double maxU, double minV, double maxV, double animation,
                                PlaneVertex vertex) {
        if (radius <= 0.0D) return;
        double invSqrt2 = 1.0D / Math.sqrt(2.0D);
        float alpha = alpha(radius);
        for (double q = -radius - STRIPE_GAP + animation; q <= radius + STRIPE_GAP; q += STRIPE_GAP) {
            double q0 = Math.max(-radius, q - STRIPE_WIDTH * 0.5D);
            double q1 = Math.min(radius, q + STRIPE_WIDTH * 0.5D);
            if (q1 <= q0) continue;
            double h0 = Math.sqrt(Math.max(0.0D, radius * radius - q0 * q0));
            double h1 = Math.sqrt(Math.max(0.0D, radius * radius - q1 * q1));
            point(vertex, centerU, centerV, q0, -h0, invSqrt2, minU, maxU, minV, maxV, alpha);
            point(vertex, centerU, centerV, q0, h0, invSqrt2, minU, maxU, minV, maxV, alpha);
            point(vertex, centerU, centerV, q1, h1, invSqrt2, minU, maxU, minV, maxV, alpha);
            point(vertex, centerU, centerV, q1, -h1, invSqrt2, minU, maxU, minV, maxV, alpha);
            point(vertex, centerU, centerV, q1, -h1, invSqrt2, minU, maxU, minV, maxV, alpha);
            point(vertex, centerU, centerV, q1, h1, invSqrt2, minU, maxU, minV, maxV, alpha);
            point(vertex, centerU, centerV, q0, h0, invSqrt2, minU, maxU, minV, maxV, alpha);
            point(vertex, centerU, centerV, q0, -h0, invSqrt2, minU, maxU, minV, maxV, alpha);
        }
    }

    private static void point(PlaneVertex vertex, double centerU, double centerV, double normal,
                              double along, double invSqrt2, double minU, double maxU,
                              double minV, double maxV, float alpha) {
        double u = centerU + (along - normal) * invSqrt2;
        double v = centerV + (along + normal) * invSqrt2;
        vertex.put(Math.max(minU, Math.min(maxU, u)), Math.max(minV, Math.min(maxV, v)), alpha);
    }

    private static void vertex(PoseStack pose, VertexConsumer out, double x, double y, double z, float alpha) {
        out.vertex(pose.last().pose(), (float) x, (float) y, (float) z)
                .color(1.0F, 0.025F, 0.04F, alpha).endVertex();
    }

    @FunctionalInterface
    private interface PlaneVertex {
        void put(double u, double v, float alpha);
    }
}
