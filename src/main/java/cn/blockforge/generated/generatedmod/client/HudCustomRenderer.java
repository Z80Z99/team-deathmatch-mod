package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.client.ClientHudLayout.CustomElement;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

/**
 * 独立 HUD 模块的统一渲染：文字 / 色块 / 进度条 / 图片四类模块，
 * 实战覆盖层（比赛、正在匹配、房间中）与 HUD 配置窗预览都走这里，
 * 保证编辑器所见即所得。
 *
 * <p>文字与进度条模块可绑定 {@link HudStats} 的统计接口作为内容来源；
 * {@code editor=true} 时拿不到实时数据会回退到示例值（配置窗预览用）。
 */
public final class HudCustomRenderer {
    /** 未绑定数据源时进度条的固定演示比例。 */
    private static final double FIXED_DEMO_RATIO = 0.65;
    private static final Map<String, ElementRenderer> EXTERNAL_RENDERERS = new LinkedHashMap<>();

    @FunctionalInterface
    public interface ElementRenderer {
        void render(GuiGraphics graphics, Font font, ClientHudLayout.CustomElement element,
                    HudGeometry.Rect rect, boolean editor);
    }

    private HudCustomRenderer() {
    }

    /** 画一个场景的全部模块；showHidden=true 时给隐藏模块画虚线占位（配置窗用）。 */
    public static void render(GuiGraphics graphics, Font font, List<CustomElement> elements,
                              int screenWidth, int screenHeight, boolean showHidden, boolean editor) {
        render(graphics, font, elements, screenWidth, screenHeight, showHidden, editor, "");
    }

    /** 绘制列表时跳过当前活动模块，留到最后绘制以保证活动窗口在最上层。 */
    public static void render(GuiGraphics graphics, Font font, List<CustomElement> elements,
                              int screenWidth, int screenHeight, boolean showHidden, boolean editor,
                              String skipId) {
        for (CustomElement element : elements) {
            if (skipId != null && !skipId.isBlank() && skipId.equals(element.id())) {
                continue;
            }
            HudGeometry.Rect rect = HudGeometry.custom(element, screenWidth, screenHeight);
            if (element.visible()) {
                draw(graphics, font, element, rect, editor);
            } else if (showHidden) {
                drawGhost(graphics, font, element, rect);
            }
        }
    }

    public static synchronized void registerRenderer(String type, ElementRenderer renderer) {
        if (type == null || type.isBlank() || renderer == null
                || type.equals("text") || type.equals("block") || type.equals("progress") || type.equals("image")) {
            throw new IllegalArgumentException("HUD 渲染器参数无效或类型已被内置占用");
        }
        EXTERNAL_RENDERERS.put(type, renderer);
    }

    public static synchronized boolean unregisterRenderer(String type) {
        return type != null && EXTERNAL_RENDERERS.remove(type) != null;
    }

    public static synchronized Map<String, ElementRenderer> externalRenderers() {
        return Map.copyOf(EXTERNAL_RENDERERS);
    }

    public static void draw(GuiGraphics graphics, Font font, CustomElement element,
                            HudGeometry.Rect rect, boolean editor) {
        if (!HudParameters.visible(element.placement().condition(), editor)) return;
        ElementRenderer external = EXTERNAL_RENDERERS.get(element.type());
        if (external != null) {
            external.render(graphics, font, element, rect, editor);
            return;
        }
        int alpha = Math.max(0, Math.min(100, element.opacityPercent()));
        if (!editor && element.placement().condition().equals("feed")) {
            alpha = alpha * Math.min(20, Math.max(0, ClientMatchData.killFeedTicksLeft())) / 20;
        }
        int color = UiTheme.withAlpha(element.color(), alpha);
        switch (element.type()) {
            case "block" -> {
                if (element.shadow()) {
                    graphics.fill(rect.left() + 2, rect.top() + 2, rect.right() + 2, rect.bottom() + 2,
                            UiTheme.withAlpha(UiTheme.SHADOW, alpha));
                }
                if (element.background()) {
                    graphics.fill(rect.left(), rect.top(), rect.right(), rect.bottom(), color);
                }
                if (element.border()) {
                    graphics.renderOutline(rect.left(), rect.top(), rect.width(), rect.height(), color);
                }
            }
            case "progress" -> drawProgress(graphics, font, element, rect, alpha, color, editor);
            case "image" -> {
                if (element.shadow()) {
                    graphics.fill(rect.left() + 2, rect.top() + 2, rect.right() + 2, rect.bottom() + 2,
                            UiTheme.withAlpha(UiTheme.SHADOW, alpha));
                }
                if (!HudBackground.drawImage(graphics, element.text(), rect.left(), rect.top(),
                        rect.width(), rect.height(), alpha)) {
                    drawGhostWithLabel(graphics, font, rect, "缺图：" + (element.text().isBlank()
                            ? "未选择" : element.text()));
                }
                if (element.border()) {
                    graphics.renderOutline(rect.left(), rect.top(), rect.width(), rect.height(), color);
                }
            }
            default -> drawText(graphics, font, element, rect, alpha, color, editor);
        }
    }

    /** 进度条：绑定数据源后按比例填充，标签走模板（%s=百分比/数值）。 */
    private static void drawProgress(GuiGraphics graphics, Font font, CustomElement element,
                                     HudGeometry.Rect rect, int alpha, int color, boolean editor) {
        double ratio = FIXED_DEMO_RATIO;
        String label = element.text();
        HudStats.Source source = element.boundSource();
        if (source != null) {
            ratio = source.ratio(editor);
            if (source.kind() == HudStats.Kind.PROGRESS || source.maximum(editor) > 0) {
                label = HudStats.percentLabel(element.text(), ratio);
            } else {
                label = HudStats.applyTemplate(element.text(), source.display(editor),
                        Long.toString(Math.round(source.number(editor))), Integer.toString(source.maximum(editor)));
            }
        } else if (!element.source().isBlank()) {
            label = element.text() + "（源缺失）";
        }
        if (element.shadow()) {
            graphics.fill(rect.left() + 2, rect.top() + 2, rect.right() + 2, rect.bottom() + 2,
                    UiTheme.withAlpha(UiTheme.SHADOW, alpha));
        }
        if (element.background()) {
            graphics.fill(rect.left(), rect.top(), rect.right(), rect.bottom(),
                    UiTheme.withAlpha(UiTheme.PANEL, alpha));
        }
        label = HudParameters.render(label, editor);
        int inset = rect.height() < 6 ? 0 : 2;
        int inner = Math.max(1, rect.width() - inset * 2);
        int filled = ratio <= 0.0 ? 0 : Math.max(1, (int) Math.round(inner * Math.min(1.0, ratio)));
        graphics.fill(rect.left() + inset, rect.top() + inset, rect.left() + inset + filled,
                rect.bottom() - inset, color);
        if (element.border()) {
            graphics.renderOutline(rect.left(), rect.top(), rect.width(), rect.height(),
                    UiTheme.withAlpha(UiTheme.BORDER, alpha));
        }
        if (rect.height() >= 12 && !label.isBlank()) {
            graphics.drawString(font, UiTheme.fit(font, label, inner),
                    rect.left() + 4, rect.centerY() - 4,
                    UiTheme.withAlpha(UiTheme.TEXT, alpha), false);
        }
    }

    /** 文字模块：绑定数据源时内容 = 模板(统计值)；支持 50%~300% 字号缩放。 */
    private static void drawText(GuiGraphics graphics, Font font, CustomElement element,
                                 HudGeometry.Rect rect, int alpha, int color, boolean editor) {
        String content = element.text();
        HudStats.Source source = element.boundSource();
        if (source != null) {
            content = HudStats.applyTemplate(element.text(), source.display(editor),
                    Long.toString(Math.round(source.number(editor))), Integer.toString(source.maximum(editor)));
        } else if (!element.source().isBlank()) {
            content = element.text() + "（源缺失）";
        }
        content = HudParameters.render(content, editor);
        float scale = Math.max(0.5F, Math.min(3.0F, element.scalePercent() / 100.0F)) * rect.scale();
        graphics.pose().pushPose();
        graphics.pose().translate(rect.centerX(), rect.centerY(), 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        int halfWidth = Math.round(rect.width() / scale) / 2;
        int halfHeight = Math.round(rect.height() / scale) / 2;
        if (element.shadow()) {
            graphics.fill(-halfWidth + 2, -halfHeight + 2, halfWidth + 2, halfHeight + 2,
                    UiTheme.withAlpha(UiTheme.SHADOW, alpha));
        }
        if (element.background()) {
            graphics.fill(-halfWidth, -halfHeight, halfWidth, halfHeight,
                    UiTheme.withAlpha(UiTheme.PANEL_RAISED, alpha));
        }
        if (element.border()) {
            graphics.renderOutline(-halfWidth, -halfHeight, halfWidth * 2, halfHeight * 2, color);
        }
        // 坐标已按 scale 放大，限宽要换算回本地坐标，文字才不会溢出面板。
        int padding = element.placement().example().isBlank() ? 8 : 0;
        int localLimit = Math.max(1, Math.round((rect.width() - padding) / scale));
        graphics.drawCenteredString(font, UiTheme.fit(font, content, localLimit), 0, -4, color);
        graphics.pose().popPose();
    }

    /** 隐藏模块的虚线占位框（仅配置窗显示）。 */
    private static void drawGhost(GuiGraphics graphics, Font font, CustomElement element,
                                  HudGeometry.Rect rect) {
        drawGhostWithLabel(graphics, font, rect, "已隐藏 · " + element.displayName());
    }

    private static void drawGhostWithLabel(GuiGraphics graphics, Font font, HudGeometry.Rect rect,
                                           String label) {
        for (int x = rect.left(); x < rect.right(); x += 5) {
            graphics.fill(x, rect.top(), Math.min(x + 2, rect.right()), rect.top() + 1,
                    UiTheme.BORDER_SUBTLE);
            graphics.fill(x, rect.bottom() - 1, Math.min(x + 2, rect.right()), rect.bottom(),
                    UiTheme.BORDER_SUBTLE);
        }
        for (int y = rect.top(); y < rect.bottom(); y += 5) {
            graphics.fill(rect.left(), y, rect.left() + 1, Math.min(y + 2, rect.bottom()),
                    UiTheme.BORDER_SUBTLE);
            graphics.fill(rect.right() - 1, y, rect.right(), Math.min(y + 2, rect.bottom()),
                    UiTheme.BORDER_SUBTLE);
        }
        graphics.drawString(font, UiTheme.fit(font, label, Math.max(24, rect.width())),
                rect.left(), Math.max(2, rect.top() - 10), UiTheme.SUBTLE, false);
    }
}
