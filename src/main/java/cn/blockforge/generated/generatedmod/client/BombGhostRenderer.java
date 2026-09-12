package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Axis;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Draws the server-validated C4 placement ghost and the jammer fishing line. */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BombGhostRenderer {
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(
            new com.mojang.blaze3d.vertex.BufferBuilder(2048));

    private BombGhostRenderer() { }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        if (ClientBombPreviewData.present && ClientMatchData.state
                == cn.blockforge.generated.generatedmod.match.MatchState.PLAYING) {
            float red = ClientBombPreviewData.valid ? 0.10F : 0.90F;
            float green = ClientBombPreviewData.valid ? 0.85F : 0.08F;
            float blue = ClientBombPreviewData.valid ? 0.78F : 0.08F;
            Direction face = ClientBombPreviewData.face;
            Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
            Vec3 center = new Vec3(ClientBombPreviewData.x, ClientBombPreviewData.y, ClientBombPreviewData.z)
                    .add(normal.scale(0.16D));
            renderModel(pose, minecraft.getItemRenderer(), center, face, BUFFERS);
            drawGhost(pose, BUFFERS.getBuffer(RenderType.debugQuads()),
                    center, face, red, green, blue, 0.58F);
        }

        if (ClientBombData.active && ClientBombData.phase
                == cn.blockforge.generated.generatedmod.match.ClassicBombState.Phase.DEFUSING
                && minecraft.player.getGameProfile().getName().equals(ClientBombData.operatorName)
                && (minecraft.player.getMainHandItem().is(cn.blockforge.generated.generatedmod.item.ModItems.JAMMER_TABLET.get())
                || minecraft.player.getOffhandItem().is(cn.blockforge.generated.generatedmod.item.ModItems.JAMMER_TABLET.get()))) {
            Vec3 hand = minecraft.player.getEyePosition(event.getPartialTick())
                    .add(minecraft.player.getViewVector(event.getPartialTick()).scale(0.35D));
            Vec3 bomb = new Vec3(ClientBombData.x, ClientBombData.y, ClientBombData.z);
            drawLine(pose, BUFFERS.getBuffer(RenderType.lines()), hand, bomb,
                    0.16F, 0.92F, 0.84F, 0.9F);
        }

        BUFFERS.endBatch();
        pose.popPose();
    }

    private static void renderModel(PoseStack pose, ItemRenderer renderer, Vec3 center,
                                    Direction face, MultiBufferSource buffers) {
        pose.pushPose();
        pose.translate(center.x, center.y, center.z);
        pose.mulPose(Axis.YP.rotationDegrees(faceYaw(face)));
        if (face == Direction.DOWN) {
            pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
        } else if (face.getAxis().isHorizontal()) {
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        }
        pose.scale(0.42F, 0.42F, 0.42F);
        renderer.renderStatic(new ItemStack(cn.blockforge.generated.generatedmod.item.ModItems.C4.get()),
                ItemDisplayContext.FIXED, 0, OverlayTexture.NO_OVERLAY, pose, buffers,
                Minecraft.getInstance().level, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }

    private static float faceYaw(Direction face) {
        return switch (face) {
            case NORTH -> 180.0F;
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case EAST -> -90.0F;
            case UP, DOWN -> 0.0F;
        };
    }

    private static void drawLine(PoseStack pose, VertexConsumer out, Vec3 from, Vec3 to,
                                 float red, float green, float blue, float alpha) {
        Vec3 normal = to.subtract(from).normalize();
        out.vertex(pose.last().pose(), (float) from.x, (float) from.y, (float) from.z)
                .color(red, green, blue, alpha)
                .normal((float) normal.x, (float) normal.y, (float) normal.z).endVertex();
        out.vertex(pose.last().pose(), (float) to.x, (float) to.y, (float) to.z)
                .color(red, green, blue, alpha)
                .normal((float) normal.x, (float) normal.y, (float) normal.z).endVertex();
    }

    private static void drawGhost(PoseStack pose, VertexConsumer out, Vec3 center, Direction face,
                                  float red, float green, float blue, float alpha) {
        float depth = 0.045F;
        float wide = 0.22F;
        double hx = face.getAxis() == Direction.Axis.X ? depth : wide;
        double hy = face.getAxis() == Direction.Axis.Y ? depth : wide;
        double hz = face.getAxis() == Direction.Axis.Z ? depth : wide;
        double x0 = center.x - hx, x1 = center.x + hx;
        double y0 = center.y - hy, y1 = center.y + hy;
        double z0 = center.z - hz, z1 = center.z + hz;
        quad(pose, out, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, red, green, blue, alpha);
        quad(pose, out, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, red, green, blue, alpha);
        quad(pose, out, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, red, green, blue, alpha);
        quad(pose, out, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, red, green, blue, alpha);
        quad(pose, out, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, red, green, blue, alpha);
        quad(pose, out, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, red, green, blue, alpha);
    }

    private static void quad(PoseStack pose, VertexConsumer out,
                             double x0, double y0, double z0, double x1, double y1, double z1,
                             double x2, double y2, double z2, double x3, double y3, double z3,
                             float red, float green, float blue, float alpha) {
        vertex(pose, out, x0, y0, z0, red, green, blue, alpha);
        vertex(pose, out, x1, y1, z1, red, green, blue, alpha);
        vertex(pose, out, x2, y2, z2, red, green, blue, alpha);
        vertex(pose, out, x3, y3, z3, red, green, blue, alpha);
    }

    private static void vertex(PoseStack pose, VertexConsumer out,
                               double x, double y, double z,
                               float red, float green, float blue, float alpha) {
        out.vertex(pose.last().pose(), (float) x, (float) y, (float) z)
                .color(red, green, blue, alpha).endVertex();
    }
}
