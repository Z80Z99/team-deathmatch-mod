package cn.blockforge.generated.generatedmod.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HUD 外部图片：加载玩家放进 config/fpsmod/hud_images 的 PNG，
 * 以 DynamicTexture 直接上传 GPU。同一张图既可做全屏背景，
 * 也可作为“图片模块”画在任意矩形区域内；多张图片按最近使用缓存（上限 16 张）。
 */
public final class HudBackground {
    private static final int CACHE_LIMIT = 16;

    private record Cached(DynamicTexture texture, long modified) {
    }

    private static final Map<String, Cached> CACHE = new LinkedHashMap<>(8, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Cached> eldest) {
            if (size() <= CACHE_LIMIT) {
                return false;
            }
            eldest.getValue().texture().close();
            return true;
        }
    };

    private HudBackground() {
    }

    /** 目录里的 PNG 列表（按文件名排序）；目录不存在时自动创建。 */
    public static List<String> availableImages() {
        List<String> names = new ArrayList<>();
        Path directory = ClientHudLayout.backgroundDirectory();
        try {
            Files.createDirectories(directory);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.{png,PNG}")) {
                for (Path path : stream) {
                    names.add(path.getFileName().toString());
                }
            }
            names.sort(String.CASE_INSENSITIVE_ORDER);
        } catch (Exception ignored) {
            // 目录不可读时按“无图片”处理。
        }
        return names;
    }

    private static Path resolve(String fileName) {
        return ClientHudLayout.backgroundDirectory().resolve(fileName);
    }

    private static DynamicTexture textureFor(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        try {
            Path path = resolve(fileName);
            long modified = Files.getLastModifiedTime(path).toMillis();
            Cached cached = CACHE.get(fileName);
            if (cached != null && cached.modified() == modified) {
                return cached.texture();
            }
            DynamicTexture texture;
            try (InputStream stream = Files.newInputStream(path)) {
                NativeImage image = NativeImage.read(stream);
                texture = new DynamicTexture(image);
            }
            if (cached != null) {
                cached.texture().close();
            }
            CACHE.put(fileName, new Cached(texture, modified));
            return texture;
        } catch (Exception error) {
            return null;
        }
    }

    /** 全屏幕拉伸绘制背景图；文件缺失或透明度为 0 时什么都不画。 */
    public static void drawStretch(GuiGraphics graphics, String fileName, int opacityPercent,
                                   int screenWidth, int screenHeight) {
        drawImage(graphics, fileName, 0, 0, screenWidth, screenHeight, opacityPercent);
    }

    /** 把图片拉伸画进指定屏幕矩形；返回是否真的画了（缺图返回 false 供调用方画占位）。 */
    public static boolean drawImage(GuiGraphics graphics, String fileName, int x, int y,
                                    int width, int height, int opacityPercent) {
        if (fileName == null || fileName.isBlank() || opacityPercent <= 0
                || width <= 0 || height <= 0) {
            return false;
        }
        DynamicTexture texture = textureFor(fileName);
        if (texture == null) {
            return false;
        }
        float alpha = Mth.clamp(opacityPercent / 100.0F, 0.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, texture.getId());
        Matrix4f pose = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        float z = 0.001F;
        buffer.vertex(pose, x, y + height, z).uv(0.0F, 1.0F).endVertex();
        buffer.vertex(pose, x + width, y + height, z).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(pose, x + width, y, z).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(pose, x, y, z).uv(0.0F, 0.0F).endVertex();
        Tesselator.getInstance().end();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        return true;
    }

    /** 选中的图片文件是否仍然可读。 */
    public static boolean imageExists(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return true;
        }
        return Files.isRegularFile(resolve(fileName));
    }
}
