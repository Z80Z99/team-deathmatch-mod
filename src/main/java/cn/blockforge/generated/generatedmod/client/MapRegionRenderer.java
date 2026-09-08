package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.item.ModItems;
import cn.blockforge.generated.generatedmod.map.MapDefinition;
import cn.blockforge.generated.generatedmod.map.MapEditorView;
import cn.blockforge.generated.generatedmod.map.MapRegion;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** 手持地图道具时绘制区域呼吸外框和画笔玻璃指示。 */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MapRegionRenderer {
    private static final MultiBufferSource.BufferSource LINE_BUFFERS = MultiBufferSource.immediate(
            new com.mojang.blaze3d.vertex.BufferBuilder(4096));
    private static final MultiBufferSource.BufferSource FILL_BUFFERS = MultiBufferSource.immediate(
            new com.mojang.blaze3d.vertex.BufferBuilder(4096));
    private MapRegionRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || !holdingMapTool(minecraft)) return;
        MapEditorView view = ClientMapEditorData.view();
        if (!view.hasTarget()) {
            MapToolClientState.setHoveredRegion("");
            MapToolClientState.setBrushTarget(BlockPos.ZERO, false);
            return;
        }

        boolean planner = holdingPlanner(minecraft);
        boolean brush = holdingBrush(minecraft);
        List<MapRegion> hits = planner ? MapRegionPicker.pick(minecraft, view, 128.0D) : List.of();
        MapToolClientState.setHoveredRegion(planner && !hits.isEmpty() ? hits.get(0).id() : "");
        if (brush) {
            BlockPos target = MapRegionPicker.brushTarget(minecraft, view.brushRange());
            MapToolClientState.setBrushTarget(target,
                    minecraft.level.getBlockState(target).isAir());
        }

        Camera camera = event.getCamera();
        Vec3 position = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-position.x, -position.y, -position.z);
        // Keep both builders alive: switching types on one immediate builder invalidates the first consumer.
        VertexConsumer lines = LINE_BUFFERS.getBuffer(RenderType.lines());
        VertexConsumer fills = FILL_BUFFERS.getBuffer(RenderType.debugFilledBox());
        RenderSystem.disableDepthTest();
        float pulse = 0.5F + 0.5F * (float) Math.sin((minecraft.player.tickCount + event.getPartialTick()) * 0.16D);

        for (MapRegion region : view.regions()) {
            boolean hovered = region.id().equals(MapToolClientState.hoveredRegionId());
            boolean selected = region.id().equals(view.selectedRegionId());
            float fillAlpha = hovered || selected
                    ? 0.16F + 0.16F * pulse
                    : region.type() == MapRegion.Type.BOUNDS || region.type() == MapRegion.Type.RESET
                    ? 0.08F + 0.04F * pulse
                    : 0.05F;
            float lineAlpha = hovered || selected
                    ? 0.70F + 0.30F * pulse
                    : 0.35F + 0.20F * pulse;
            drawRegion(poseStack, lines, fills, region, fillAlpha, lineAlpha);
        }

        if (brush) {
            BlockPos target = MapToolClientState.brushTarget();
            BlockPos first = ClientMapEditorData.firstPoint();
            BlockPos second = ClientMapEditorData.secondPoint();
            BlockPos anchor = first != null ? first : second;
            if (anchor != null) {
                BlockPos end = first != null && second != null ? second : target;
                LevelRenderer.renderLineBox(poseStack, lines,
                        Math.min(anchor.getX(), end.getX()) - 0.01D,
                        Math.min(anchor.getY(), end.getY()) - 0.01D,
                        Math.min(anchor.getZ(), end.getZ()) - 0.01D,
                        Math.max(anchor.getX(), end.getX()) + 1.01D,
                        Math.max(anchor.getY(), end.getY()) + 1.01D,
                        Math.max(anchor.getZ(), end.getZ()) + 1.01D, 1.0F, 0.85F, 0.2F, 1.0F);
                drawBlock(poseStack, lines, fills, anchor, 1.0F, 0.85F, 0.2F, 0.2F, 1.0F);
            }
            boolean air = MapToolClientState.brushTargetAir();
            float red = air ? 0.45F : 1.0F;
            float green = air ? 0.85F : 0.75F;
            float blue = air ? 1.0F : 0.25F;
            drawBlock(poseStack, lines, fills, target, red, green, blue,
                    0.30F + 0.12F * pulse, 0.85F + 0.15F * pulse);
        }

        FILL_BUFFERS.endBatch();
        LINE_BUFFERS.endBatch();
        RenderSystem.enableDepthTest();
        poseStack.popPose();
    }

    private static boolean holdingMapTool(Minecraft minecraft) {
        return holdingPlanner(minecraft) || holdingBrush(minecraft);
    }

    private static boolean holdingPlanner(Minecraft minecraft) {
        return minecraft.player.getMainHandItem().is(ModItems.MAP_PLANNER.get())
                || minecraft.player.getOffhandItem().is(ModItems.MAP_PLANNER.get());
    }

    private static boolean holdingBrush(Minecraft minecraft) {
        return minecraft.player.getMainHandItem().is(ModItems.MAP_BRUSH.get())
                || minecraft.player.getOffhandItem().is(ModItems.MAP_BRUSH.get());
    }

    private static void drawRegion(PoseStack poseStack, VertexConsumer lines, VertexConsumer fills,
                                   MapRegion region, float fillAlpha, float lineAlpha) {
        MapDefinition.Region bounds = region.region();
        float red = ((region.color() >>> 16) & 0xFF) / 255.0F;
        float green = ((region.color() >>> 8) & 0xFF) / 255.0F;
        float blue = (region.color() & 0xFF) / 255.0F;
        if (region.fill()) {
            drawFilledBox(poseStack, fills,
                    bounds.min().getX(), bounds.min().getY(), bounds.min().getZ(),
                    bounds.max().getX() + 1.0D, bounds.max().getY() + 1.0D, bounds.max().getZ() + 1.0D,
                    red, green, blue, fillAlpha);
        }
        if (region.outline()) {
            LevelRenderer.renderLineBox(poseStack, lines,
                    bounds.min().getX() - 0.002D, bounds.min().getY() - 0.002D, bounds.min().getZ() - 0.002D,
                    bounds.max().getX() + 1.002D, bounds.max().getY() + 1.002D, bounds.max().getZ() + 1.002D,
                    red, green, blue, lineAlpha);
        }
    }

    private static void drawBlock(PoseStack poseStack, VertexConsumer lines, VertexConsumer fills,
                                  BlockPos pos, float red, float green, float blue,
                                  float fillAlpha, float lineAlpha) {
        drawFilledBox(poseStack, fills, pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D,
                red, green, blue, fillAlpha);
        LevelRenderer.renderLineBox(poseStack, lines,
                pos.getX() - 0.002D, pos.getY() - 0.002D, pos.getZ() - 0.002D,
                pos.getX() + 1.002D, pos.getY() + 1.002D, pos.getZ() + 1.002D,
                red, green, blue, lineAlpha);
    }

    private static void drawFilledBox(PoseStack poseStack, VertexConsumer consumer,
                                      double minX, double minY, double minZ,
                                      double maxX, double maxY, double maxZ,
                                      float red, float green, float blue, float alpha) {
        var matrix = poseStack.last().pose();
        face(consumer, matrix, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ,
                red, green, blue, alpha);
        face(consumer, matrix, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                red, green, blue, alpha);
        face(consumer, matrix, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ,
                red, green, blue, alpha);
        face(consumer, matrix, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ,
                red, green, blue, alpha);
        face(consumer, matrix, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ,
                red, green, blue, alpha);
        face(consumer, matrix, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                red, green, blue, alpha);
    }

    private static void face(VertexConsumer consumer, org.joml.Matrix4f matrix,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2,
                             double x3, double y3, double z3,
                             double x4, double y4, double z4,
                             float red, float green, float blue, float alpha) {
        vertex(consumer, matrix, x1, y1, z1, red, green, blue, alpha);
        vertex(consumer, matrix, x2, y2, z2, red, green, blue, alpha);
        vertex(consumer, matrix, x3, y3, z3, red, green, blue, alpha);
        vertex(consumer, matrix, x4, y4, z4, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer consumer, org.joml.Matrix4f matrix,
                               double x, double y, double z,
                               float red, float green, float blue, float alpha) {
        consumer.vertex(matrix, (float) x, (float) y, (float) z)
                .color(red, green, blue, alpha)
                .endVertex();
    }
}
