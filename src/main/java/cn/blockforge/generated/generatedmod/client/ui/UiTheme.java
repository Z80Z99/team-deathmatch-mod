package cn.blockforge.generated.generatedmod.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** 统一客户端 UI 视觉令牌和低开销绘制辅助。 */
public final class UiTheme {
    public static final int SCREEN_TINT = 0x90101113;
    public static final int PANEL = 0xF518191C;
    public static final int PANEL_RAISED = 0xF525272B;
    public static final int PANEL_SELECTED = 0xFF303D35;
    public static final int SHADOW = 0x70000000;
    public static final int BORDER = 0xFF505359;
    public static final int BORDER_SUBTLE = 0xFF34363B;
    public static final int TEXT = 0xFFF5F7F8;
    public static final int MUTED = 0xFFB8BCC2;
    public static final int SUBTLE = 0xFF8C929A;
    public static final int SUCCESS = 0xFF63D39A;
    public static final int WARNING = 0xFFFFC857;
    public static final int ERROR = 0xFFFF7070;
    public static final int INFO = 0xFF70C7E8;
    public static final int TEAM_A = 0xFFE05555;
    public static final int TEAM_B = 0xFF5599FF;
    public static final int SPECTATOR = 0xFFB0B8C0;
    public static final int ACCENT = 0xFF9CE5AF;

    private UiTheme() {
    }

    public static void tintScreen(GuiGraphics graphics, int width, int height) {
        graphics.fill(0, 0, width, height, SCREEN_TINT);
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        panel(graphics, x, y, width, height, ACCENT);
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height, int accent) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, PANEL);
        graphics.fill(x, y, x + 3, y + Math.min(24, height), accent);
    }

    public static void card(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, color);
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER_SUBTLE);
    }

    public static void header(GuiGraphics graphics, Font font, String title, String subtitle,
                              int centerX, int top, int availableWidth) {
        graphics.drawCenteredString(font, fit(font, title, Math.max(1, availableWidth)), centerX, top, TEXT);
        if (subtitle != null && !subtitle.isBlank()) {
            graphics.drawCenteredString(font, fit(font, subtitle, Math.max(1, availableWidth - 12)),
                    centerX, top + 14, MUTED);
        }
    }

    public static void section(GuiGraphics graphics, Font font, String title, int x, int y, int width) {
        graphics.drawString(font, fit(font, title, width), x, y, MUTED, false);
        graphics.fill(x, y + 11, x + Math.max(1, width), y + 12, BORDER_SUBTLE);
    }

    public static void divider(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + Math.max(1, width), y + 1, BORDER_SUBTLE);
    }

    public static void badge(GuiGraphics graphics, Font font, String text, int x, int y, int width, int color) {
        int safeWidth = Math.max(1, width);
        graphics.fill(x, y, x + safeWidth, y + 18, withAlpha(color, 12));
        graphics.fill(x, y, x + 2, y + 18, color);
        graphics.drawString(font, fit(font, text, safeWidth - 10), x + 7, y + 5, TEXT, false);
    }

    public static void status(GuiGraphics graphics, Font font, String message, int x, int y,
                              int width, int color) {
        if (message == null || message.isBlank() || width <= 0) return;
        graphics.fill(x, y, x + width, y + 20, withAlpha(color, 8));
        graphics.fill(x, y, x + 2, y + 20, color);
        graphics.drawString(font, fit(font, message, Math.max(1, width - 16)), x + 8, y + 6, color, false);
    }

    public static void progress(GuiGraphics graphics, int x, int y, int width, int height,
                                float progress, int color) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        graphics.fill(x, y, x + safeWidth, y + safeHeight, BORDER_SUBTLE);
        int filled = Math.round(safeWidth * Math.max(0.0F, Math.min(1.0F, progress)));
        if (filled > 0) graphics.fill(x, y, x + filled, y + safeHeight, color);
    }

    public static int responsiveWidth(int screenWidth, int preferred, int margin) {
        return Math.max(1, Math.min(Math.max(1, preferred), Math.max(1, screenWidth - margin)));
    }

    public static int columnWidth(int panelWidth, int gap) {
        return Math.max(1, (panelWidth - Math.max(0, gap)) / 2);
    }

    /** 行内「标签列 + 控件列」的精确切分：两格宽度 + 间距严格等于列宽，互不侵占。 */
    public record Field(int labelX, int labelWidth, int controlX, int controlWidth) {
    }

    public static Field field(int columnX, int columnWidth, int preferredControlWidth) {
        int gap = 6;
        int minimumLabel = Math.min(64, Math.max(1, columnWidth / 3));
        int control = Math.max(1, Math.min(preferredControlWidth,
                Math.max(1, columnWidth - gap - minimumLabel)));
        int label = Math.max(1, columnWidth - control - gap);
        return new Field(columnX, label, columnX + label + gap, control);
    }

    public static String fit(Font font, String value, int maximumWidth) {
        if (value == null || maximumWidth <= 0) return "";
        if (value.isEmpty() || font.width(value) <= maximumWidth) return value;
        String suffix = "…";
        if (font.width(suffix) > maximumWidth) return "";
        return font.plainSubstrByWidth(value, Math.max(0, maximumWidth - font.width(suffix))) + suffix;
    }

    public static String formatTicks(int ticks) {
        int seconds = Math.max(0, ticks) / 20;
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }

    public static int withAlpha(int color, int alphaPercent) {
        int sourceAlpha = (color >>> 24) & 0xFF;
        int alpha = sourceAlpha == 0 ? 255 : sourceAlpha;
        alpha = alpha * Math.max(0, Math.min(100, alphaPercent)) / 100;
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static Component text(String value) {
        return Component.literal(value == null ? "" : value);
    }
}
