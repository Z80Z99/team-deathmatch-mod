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

/** A translucent red plane appears only where the player is close to the playable boundary. */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BoundaryEdgeRenderer {
    private static final double RANGE = 3.0D;
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(
            new com.mojang.blaze3d.vertex.BufferBuilder(2048));

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
        VertexConsumer planes = BUFFERS.getBuffer(RenderType.debugQuads());
        RenderSystem.disableDepthTest();
        double minX = ClientMatchData.boundaryMinX, minY = ClientMatchData.boundaryMinY, minZ = ClientMatchData.boundaryMinZ;
        double maxX = ClientMatchData.boundaryMaxX + 1.0D, maxY = ClientMatchData.boundaryMaxY + 2.0D, maxZ = ClientMatchData.boundaryMaxZ + 1.0D;
        double y0 = Math.max(minY, y - RANGE), y1 = Math.min(maxY, y + RANGE);
        double x0 = Math.max(minX, x - RANGE), x1 = Math.min(maxX, x + RANGE);
        double z0 = Math.max(minZ, z - RANGE), z1 = Math.min(maxZ, z + RANGE);
        planeX(pose, planes, minX, y0, y1, z0, z1, proximity(x, minX));
        planeX(pose, planes, maxX, y0, y1, z0, z1, proximity(x, maxX));
        planeZ(pose, planes, minZ, x0, x1, y0, y1, proximity(z, minZ));
        planeZ(pose, planes, maxZ, x0, x1, y0, y1, proximity(z, maxZ));
        planeY(pose, planes, minY, x0, x1, z0, z1, proximity(y, minY));
        planeY(pose, planes, maxY, x0, x1, z0, z1, proximity(y, maxY));
        BUFFERS.endBatch();
        RenderSystem.enableDepthTest();
        pose.popPose();
    }

    private static float proximity(double coordinate, double plane) {
        double distance = Math.abs(coordinate - plane);
        if (distance > RANGE) return 0.0F;
        float near = (float) (1.0D - distance / RANGE);
        return 0.08F + 0.52F * near * near;
    }

    private static void planeX(PoseStack pose, VertexConsumer out, double x, double y0, double y1,
                               double z0, double z1, float alpha) {
        if (alpha <= 0 || y1 <= y0 || z1 <= z0) return;
        vertex(pose, out, x, y0, z0, alpha); vertex(pose, out, x, y1, z0, alpha);
        vertex(pose, out, x, y1, z1, alpha); vertex(pose, out, x, y0, z1, alpha);
        vertex(pose, out, x, y0, z1, alpha); vertex(pose, out, x, y1, z1, alpha);
        vertex(pose, out, x, y1, z0, alpha); vertex(pose, out, x, y0, z0, alpha);
    }

    private static void planeY(PoseStack pose, VertexConsumer out, double y, double x0, double x1,
                               double z0, double z1, float alpha) {
        if (alpha <= 0 || x1 <= x0 || z1 <= z0) return;
        vertex(pose, out, x0, y, z0, alpha); vertex(pose, out, x1, y, z0, alpha);
        vertex(pose, out, x1, y, z1, alpha); vertex(pose, out, x0, y, z1, alpha);
        vertex(pose, out, x0, y, z1, alpha); vertex(pose, out, x1, y, z1, alpha);
        vertex(pose, out, x1, y, z0, alpha); vertex(pose, out, x0, y, z0, alpha);
    }

    private static void planeZ(PoseStack pose, VertexConsumer out, double z, double x0, double x1,
                               double y0, double y1, float alpha) {
        if (alpha <= 0 || x1 <= x0 || y1 <= y0) return;
        vertex(pose, out, x0, y0, z, alpha); vertex(pose, out, x1, y0, z, alpha);
        vertex(pose, out, x1, y1, z, alpha); vertex(pose, out, x0, y1, z, alpha);
        vertex(pose, out, x0, y1, z, alpha); vertex(pose, out, x1, y1, z, alpha);
        vertex(pose, out, x1, y0, z, alpha); vertex(pose, out, x0, y0, z, alpha);
    }

    private static void vertex(PoseStack pose, VertexConsumer out, double x, double y, double z, float alpha) {
        out.vertex(pose.last().pose(), (float) x, (float) y, (float) z)
                .color(1.0F, 0.02F, 0.04F, alpha).endVertex();
    }
}
