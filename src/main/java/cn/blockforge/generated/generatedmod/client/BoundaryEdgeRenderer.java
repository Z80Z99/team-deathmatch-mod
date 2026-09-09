package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** A short, pulsing red outline appears only where the player is close to the playable boundary. */
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
        VertexConsumer lines = BUFFERS.getBuffer(RenderType.lines());
        RenderSystem.disableDepthTest();
        float pulse = 0.72F + 0.28F * (float) Math.sin((minecraft.player.tickCount + event.getPartialTick()) * 0.35D);
        double minX = ClientMatchData.boundaryMinX, minY = ClientMatchData.boundaryMinY, minZ = ClientMatchData.boundaryMinZ;
        double maxX = ClientMatchData.boundaryMaxX + 1.0D, maxY = ClientMatchData.boundaryMaxY + 2.0D, maxZ = ClientMatchData.boundaryMaxZ + 1.0D;
        double y0 = Math.max(minY, y - RANGE), y1 = Math.min(maxY, y + RANGE);
        double x0 = Math.max(minX, x - RANGE), x1 = Math.min(maxX, x + RANGE);
        double z0 = Math.max(minZ, z - RANGE), z1 = Math.min(maxZ, z + RANGE);
        if (Math.abs(x - minX) <= RANGE) box(pose, lines, minX, y0, z0, minX + .04D, y1, z1, pulse);
        if (Math.abs(x - maxX) <= RANGE) box(pose, lines, maxX - .04D, y0, z0, maxX, y1, z1, pulse);
        if (Math.abs(z - minZ) <= RANGE) box(pose, lines, x0, y0, minZ, x1, y1, minZ + .04D, pulse);
        if (Math.abs(z - maxZ) <= RANGE) box(pose, lines, x0, y0, maxZ - .04D, x1, y1, maxZ, pulse);
        if (Math.abs(y - minY) <= RANGE) box(pose, lines, x0, minY, z0, x1, minY + .04D, z1, pulse);
        if (Math.abs(y - maxY) <= RANGE) box(pose, lines, x0, maxY - .04D, z0, x1, maxY, z1, pulse);
        BUFFERS.endBatch();
        RenderSystem.enableDepthTest();
        pose.popPose();
    }

    private static void box(PoseStack pose, VertexConsumer lines, double minX, double minY, double minZ,
                            double maxX, double maxY, double maxZ, float alpha) {
        LevelRenderer.renderLineBox(pose, lines, minX, minY, minZ, maxX, maxY, maxZ, 1.0F, 0.08F, 0.10F, alpha);
    }
}
