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

        default void render(GuiGraphics graphics, Font font, ClientHudLayout.CustomElement element,
                            HudGeometry.Rect rect, boolean editor, HudContext context,
                            double amount) {
            render(graphics, font, element, rect, editor);
        }
    }

    private HudCustomRenderer() {
    }

    /** 画一个场景的全部模块；showHidden=true 时给隐藏模块画虚线占位（配置窗用）。 */
    public static void render(GuiGraphics graphics, Font font, List<CustomElement> elements,
                              int screenWidth, int screenHeight, boolean showHidden, boolean editor) {
        render(graphics, font, elements, screenWidth, screenHeight, showHidden, editor, "",
                HudContext.GLOBAL);
    }

    /** 绘制列表时跳过当前活动模块，留到最后绘制以保证活动窗口在最上层。 */
    public static void render(GuiGraphics graphics, Font font, List<CustomElement> elements,
                              int screenWidth, int screenHeight, boolean showHidden, boolean editor,
                              String skipId) {
        render(graphics, font, elements, screenWidth, screenHeight, showHidden, editor, skipId,
                HudContext.GLOBAL);
    }

    public static void render(GuiGraphics graphics, Font font, List<CustomElement> elements,
                              int screenWidth, int screenHeight, boolean showHidden, boolean editor,
                              String skipId, HudContext context) {
        render(graphics, font, elements, screenWidth, screenHeight, showHidden, editor, skipId,
                context, "");
    }

    /**
     * Editor rendering variant with an explicit selected module.
     * Condition-hidden modules stay readable in the editor: unselected ones use reduced
     * opacity, while the selected one is rendered at full opacity.
     */
    public static void render(GuiGraphics graphics, Font font, List<CustomElement> elements,
                              int screenWidth, int screenHeight, boolean showHidden, boolean editor,
                              String skipId, HudContext context, String selectedId) {
        for (CustomElement element : elements) {
            if (skipId != null && !skipId.isBlank() && skipId.equals(element.id())) {
                continue;
            }
            HudGeometry.Rect rect = HudGeometry.custom(element, screenWidth, screenHeight);
            boolean visible = HudConditions.visible(element, elements, editor, context);
            boolean conditionHidden = editor && element.visible() && !visible;
            double amount;
            if (editor) {
                if (!element.visible()) {
                    amount = 0;
                } else if (conditionHidden) {
                    amount = element.id().equals(selectedId) ? 1.0D : 0.38D;
                } else {
                    amount = 1.0D;
                }
            } else {
                amount = HudAnimation.frame(HudAnimation.key(element.id()), visible,
                        element.placement());
            }
            if (amount > 0) {
                graphics.pose().pushPose();
                if (element.placement().animation().equals("slide")) graphics.pose().translate(0, (1 - amount) * 16, 0);
                if (element.placement().animation().equals("zoom")) {
                    float scale = (float) (.85 + .15 * amount);
                    graphics.pose().translate(rect.centerX(), rect.centerY(), 0);
                    graphics.pose().scale(scale, scale, 1);
                    graphics.pose().translate(-rect.centerX(), -rect.centerY(), 0);
                }
                drawContent(graphics, font, element, rect, editor, amount, context);
                graphics.pose().popPose();
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
        draw(graphics, font, element, rect, editor, HudContext.GLOBAL);
    }

    public static void draw(GuiGraphics graphics, Font font, CustomElement element,
                            HudGeometry.Rect rect, boolean editor, HudContext context) {
        if (!element.visible()
                || !HudConditions.visible(element, List.of(element), editor, context)) return;
        drawContent(graphics, font, element, rect, editor, 1, context);
    }

    private static void drawContent(GuiGraphics graphics, Font font, CustomElement element,
                                    HudGeometry.Rect rect, boolean editor, double amount,
                                    HudContext context) {
        HudStats.Source bound = element.boundSource();
        if (bound != null && !bound.usable(context, editor)) {
            return;
        }
        ElementRenderer external = EXTERNAL_RENDERERS.get(element.type());
        if (external != null) {
            external.render(graphics, font, element, rect, editor, context, amount);
            return;
        }
        int alpha = (int) Math.round(Math.max(0, Math.min(100, element.opacityPercent())) * amount);
        if (!editor && element.placement().condition().equals("feed")) {
            alpha = alpha * Math.min(20, Math.max(0, ClientMatchData.killFeedTicksLeft())) / 20;
        }
        int color = UiTheme.withAlpha(element.color(), alpha);
        switch (element.type()) {
            case "respawn" -> {
                if (editor) {
                    String previewTitle = resolveText(element, context, true);
                    drawStatusPreview(graphics, font, element, rect, alpha, color,
                            previewTitle.isBlank() ? "已阵亡" : previewTitle,
                            "重新部署  4", resolveRatio(element, context, true, 0.42));
                }
            }
            case "boundary" -> {
                if (editor) {
                    String previewTitle = resolveText(element, context, true);
                    drawStatusPreview(graphics, font, element, rect, alpha, color,
                            previewTitle.isBlank() ? "返回作战区域" : previewTitle,
                            "8", resolveRatio(element, context, true, 0.80));
                }
            }
            case "block" -> {
                if (element.shadow()) {
                    graphics.fill(rect.left() + 2, rect.top() + 2, rect.right() + 2, rect.bottom() + 2,
                            UiTheme.withAlpha(shadowColor(element), alpha));
                }
                if (element.background()) {
                    graphics.fill(rect.left(), rect.top(), rect.right(), rect.bottom(),
                            UiTheme.withAlpha(backgroundColor(element, element.color()), alpha));
                }
                if (element.border()) {
                    graphics.renderOutline(rect.left(), rect.top(), rect.width(), rect.height(),
                            UiTheme.withAlpha(borderColor(element, element.color()), alpha));
                }
                glow(graphics, element, rect, alpha);
            }
            case "progress" -> drawProgress(graphics, font, element, rect, alpha, color, editor,
                    context);
            case "image" -> {
                if (element.shadow()) {
                    graphics.fill(rect.left() + 2, rect.top() + 2, rect.right() + 2, rect.bottom() + 2,
                            UiTheme.withAlpha(shadowColor(element), alpha));
                }
                if (!HudBackground.drawImage(graphics, element.text(), rect.left(), rect.top(),
                        rect.width(), rect.height(), alpha)) {
                    drawGhostWithLabel(graphics, font, rect, "缺图：" + (element.text().isBlank()
                            ? "未选择" : element.text()));
                }
                if (element.border()) {
                    graphics.renderOutline(rect.left(), rect.top(), rect.width(), rect.height(),
                            UiTheme.withAlpha(borderColor(element, element.color()), alpha));
                }
                glow(graphics, element, rect, alpha);
            }
            default -> drawText(graphics, font, element, rect, alpha, color, editor, context);
        }
    }

    private static void drawStatusPreview(GuiGraphics graphics, Font font, CustomElement element,
                                          HudGeometry.Rect rect, int alpha, int color,
                                          String title, String detail, double progress) {
        if (element.shadow()) graphics.fill(rect.left() + 2, rect.top() + 2, rect.right() + 2,
                rect.bottom() + 2, UiTheme.withAlpha(shadowColor(element), alpha));
        if (element.background()) graphics.fill(rect.left(), rect.top(), rect.right(), rect.bottom(),
                UiTheme.withAlpha(backgroundColor(element, UiTheme.PANEL_RAISED), alpha));
        if (element.border()) graphics.renderOutline(rect.left(), rect.top(), rect.width(), rect.height(),
                UiTheme.withAlpha(borderColor(element, element.color()), alpha));
        glow(graphics, element, rect, alpha);
        graphics.drawCenteredString(font, UiTheme.fit(font, title, rect.width() - 12), rect.centerX(),
                rect.top() + 9, UiTheme.withAlpha(UiTheme.TEXT, alpha));
        graphics.drawCenteredString(font, detail, rect.centerX(), rect.top() + 25,
                UiTheme.withAlpha(UiTheme.TEXT, alpha));
        int y = rect.bottom() - 7;
        graphics.fill(rect.left() + 6, y, rect.right() - 6, y + 3, UiTheme.withAlpha(UiTheme.BORDER, alpha));
        graphics.fill(rect.left() + 6, y, rect.left() + 6 + (int) ((rect.width() - 12) * progress), y + 3, color);
    }

    /** 进度条：绑定数据源后按比例填充，标签走模板（%s=百分比/数值）。 */
    private static void drawProgress(GuiGraphics graphics, Font font, CustomElement element,
                                     HudGeometry.Rect rect, int alpha, int color, boolean editor,
                                     HudContext context) {
        double ratio = FIXED_DEMO_RATIO;
        String label = element.text();
        HudStats.Source source = element.boundSource();
        if (source != null) {
            ratio = source.ratio(editor, context);
            if (element.placement().progressMaximum() > 0) ratio = Math.max(0, Math.min(1,
                    source.number(editor, context) / (element.placement().progressMaximum() * (source.kind() == HudStats.Kind.TIME ? 20D : 1D))));
            int maximum = element.placement().progressMaximum() > 0 ? element.placement().progressMaximum()
                    * (source.kind() == HudStats.Kind.TIME ? 20 : 1) : source.maximum(editor, context);
            label = HudStats.applyTemplate(element.text(), Math.round(ratio * 100) + "%",
                    Long.toString(Math.round(source.number(editor, context))), Integer.toString(maximum));
        } else if (!element.source().isBlank()) {
            label = element.text() + "（源缺失）";
        }
        if (!editor) ratio = HudAnimation.progressFrame(HudAnimation.key(element.id()) + ":progress",
                ratio, element.placement());
        if (element.shadow()) {
            graphics.fill(rect.left() + 2, rect.top() + 2, rect.right() + 2, rect.bottom() + 2,
                    UiTheme.withAlpha(shadowColor(element), alpha));
        }
        if (element.background()) {
            graphics.fill(rect.left(), rect.top(), rect.right(), rect.bottom(),
                    UiTheme.withAlpha(backgroundColor(element, UiTheme.PANEL), alpha));
        }
        label = HudParameters.render(label, context, editor);
        int inset = rect.height() < 6 ? 0 : 2;
        int inner = Math.max(1, rect.width() - inset * 2);
        int filled = ratio <= 0.0 ? 0 : Math.max(1, (int) Math.round(inner * Math.min(1.0, ratio)));
        if (element.placement().progressDirection().equals("drain")) {
            graphics.fill(rect.right() - inset - filled, rect.top() + inset, rect.right() - inset,
                    rect.bottom() - inset, color);
        } else {
            graphics.fill(rect.left() + inset, rect.top() + inset, rect.left() + inset + filled,
                    rect.bottom() - inset, color);
        }
        if (element.border()) {
            graphics.renderOutline(rect.left(), rect.top(), rect.width(), rect.height(),
                    UiTheme.withAlpha(borderColor(element, UiTheme.BORDER), alpha));
        }
        if (rect.height() >= 12 && !label.isBlank()) {
            String fitted = UiTheme.fit(font, label, inner);
            double contentAmount = editor ? 1.0D : HudAnimation.contentFrame(
                    HudAnimation.key(element.id()) + ":label",
                    fitted, element.placement());
            graphics.pose().pushPose();
            applyContentTransition(graphics, element, rect, contentAmount);
            graphics.drawString(font, fitted, alignedX(element, font, fitted, rect.left() + 4, rect.right() - 4),
                    rect.centerY() - 4, UiTheme.withAlpha(UiTheme.TEXT,
                            (int) Math.round(alpha * contentAmount)), false);
            graphics.pose().popPose();
        }
        glow(graphics, element, rect, alpha);
    }

    /** Resolve a text module for integrations that render their own container. */
    public static String resolveText(CustomElement element, HudContext context, boolean editor) {
        String content = element.text();
        HudStats.Source source = element.boundSource();
        if (source != null) {
            if (!source.usable(context, editor)) return "";
            content = HudStats.applyTemplate(content, source.display(editor, context),
                    Long.toString(Math.round(source.number(editor, context))),
                    Integer.toString(source.maximum(editor, context)));
        } else if (!element.source().isBlank()) {
            content = content + "（源缺失）";
        }
        return HudParameters.render(content, context, editor);
    }

    /** Resolve the progress ratio for integrations that render their own progress bar. */
    public static double resolveRatio(CustomElement element, HudContext context,
                                      boolean editor, double fallback) {
        HudStats.Source source = element.boundSource();
        if (source != null) {
            if (!source.usable(context, editor)) return fallback;
            if (element.placement().progressMaximum() > 0) {
                double divisor = element.placement().progressMaximum()
                        * (source.kind() == HudStats.Kind.TIME ? 20D : 1D);
                return Math.max(0, Math.min(1, source.number(editor, context) / divisor));
            }
            return source.ratio(editor, context);
        }
        return fallback;
    }

    /** 文字模块：绑定数据源时内容 = 模板(统计值)；支持 50%~300% 字号缩放。 */
    private static void drawText(GuiGraphics graphics, Font font, CustomElement element,
                                 HudGeometry.Rect rect, int alpha, int color, boolean editor,
                                 HudContext context) {
        String content = element.text();
        HudStats.Source source = element.boundSource();
        if (source != null) {
            content = HudStats.applyTemplate(element.text(), source.display(editor, context),
                    Long.toString(Math.round(source.number(editor, context))),
                    Integer.toString(source.maximum(editor, context)));
        } else if (!element.source().isBlank()) {
            content = element.text() + "（源缺失）";
        }
        content = HudParameters.render(content, context, editor);
        double contentAmount = editor ? 1.0D : HudAnimation.contentFrame(
                HudAnimation.key(element.id()), content,
                element.placement());
        float scale = Math.max(0.5F, Math.min(3.0F, element.scalePercent() / 100.0F)) * rect.scale();
        graphics.pose().pushPose();
        graphics.pose().translate(rect.centerX(), rect.centerY(), 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        applyContentTransition(graphics, element, rect, contentAmount);
        int halfWidth = Math.round(rect.width() / scale) / 2;
        int halfHeight = Math.round(rect.height() / scale) / 2;
        if (element.shadow()) {
            graphics.fill(-halfWidth + 2, -halfHeight + 2, halfWidth + 2, halfHeight + 2,
                    UiTheme.withAlpha(shadowColor(element), alpha));
        }
        if (element.background()) {
            graphics.fill(-halfWidth, -halfHeight, halfWidth, halfHeight,
                    UiTheme.withAlpha(backgroundColor(element, UiTheme.PANEL_RAISED), alpha));
        }
        if (element.border()) {
            graphics.renderOutline(-halfWidth, -halfHeight, halfWidth * 2, halfHeight * 2,
                    UiTheme.withAlpha(borderColor(element, element.color()), alpha));
        }
        // 坐标已按 scale 放大，限宽要换算回本地坐标，文字才不会溢出面板。
        int padding = element.placement().example().isBlank() ? 8 : 0;
        int localLimit = Math.max(1, Math.round((rect.width() - padding) / scale));
        String fitted = UiTheme.fit(font, content, localLimit);
        int textX = switch (element.placement().alignment()) {
            case "left" -> -halfWidth + 4;
            case "right" -> halfWidth - 4 - font.width(fitted);
            default -> -font.width(fitted) / 2;
        };
        graphics.drawString(font, fitted, textX, -4, UiTheme.withAlpha(color,
                (int) Math.round(alpha * contentAmount)), false);
        graphics.pose().popPose();
        glow(graphics, element, rect, alpha);
    }

    private static void applyContentTransition(GuiGraphics graphics, CustomElement element,
                                               HudGeometry.Rect rect, double amount) {
        switch (element.placement().contentAnimation()) {
            case "slide" -> graphics.pose().translate(0, (1 - amount) * 8, 0);
            case "zoom" -> {
                float scale = (float) (.9 + .1 * amount);
                graphics.pose().translate(rect.centerX(), rect.centerY(), 0);
                graphics.pose().scale(scale, scale, 1);
                graphics.pose().translate(-rect.centerX(), -rect.centerY(), 0);
            }
            default -> { }
        }
    }

    private static int backgroundColor(CustomElement element, int fallback) {
        return element.placement().backgroundColor() == 0 ? fallback : element.placement().backgroundColor();
    }
    private static int borderColor(CustomElement element, int fallback) {
        return element.placement().borderColor() == 0 ? fallback : element.placement().borderColor();
    }
    private static int shadowColor(CustomElement element) {
        return element.placement().shadowColor() == 0 ? UiTheme.SHADOW : element.placement().shadowColor();
    }
    private static int alignedX(CustomElement element, Font font, String text, int left, int right) {
        return switch (element.placement().alignment()) {
            case "right" -> right - font.width(text);
            case "center" -> (left + right - font.width(text)) / 2;
            default -> left;
        };
    }
    public static void glow(GuiGraphics graphics, CustomElement element, HudGeometry.Rect rect,
                            int alpha) {
        if (!element.placement().glow()) return;
        int color = element.placement().glowColor() == 0 ? element.color() : element.placement().glowColor();
        graphics.renderOutline(rect.left() - 1, rect.top() - 1, rect.width() + 2, rect.height() + 2,
                UiTheme.withAlpha(color, Math.max(12, alpha / 3)));
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
