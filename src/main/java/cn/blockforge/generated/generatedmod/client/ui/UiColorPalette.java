package cn.blockforge.generated.generatedmod.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * 展开式色板：预设色网格 + 色相 / 饱和度 / 明度三条滑杆 + 当前色预览与十六进制值。
 * HUD 配置窗里点「选择颜色」展开，选到哪一格就立刻回调。
 */
public final class UiColorPalette extends AbstractWidget {
    /** 预览条 + 3 行色块 + 3 条滑杆及底部留白。 */
    public static final int HEIGHT = 156;
    private static final int PREVIEW_ROW = 16;
    private static final int CELL = 15;
    private static final int GRID_ROWS = 3;
    private static final int GRID_COLUMNS = 8;
    private static final int SLIDER_ROW = 24;
    private static final int GAP = 4;

    /** 预设色：本 Mod 主题色、A/B 队色、常用高亮色与黑白灰。 */
    private static final int[] PRESETS = {
            0xFF50E0C8, 0xFF70C7E8, 0xFF63D39A, 0xFFFFC857, 0xFFFF7070, 0xFFE05555, 0xFF5599FF, 0xFFB0B8C0,
            0xFFF5F7F8, 0xFFB2BEC6, 0xFF71818D, 0xFF2A4147, 0xFFFFD27A, 0xFFB07CFF, 0xFF7CFF9E, 0xFFFF7CD2,
            0xFF000000, 0xFF1D2731, 0xFF3C4C58, 0xFF5A3A2A, 0xFF2A5A3A, 0xFF2A3A5A, 0xFF8C1D18, 0xFF148CC8,
    };

    private final Font font;
    private final Consumer<Integer> onChange;
    private int color;
    private float hue;
    private float saturation;
    private float brightness;
    private int draggingSlider = -1;

    public UiColorPalette(Font font, int x, int y, int width, int initialColor, Consumer<Integer> onChange) {
        super(x, y, width, HEIGHT, Component.literal("色板"));
        this.font = font;
        this.onChange = onChange;
        setColor(initialColor, false);
    }

    /** 外部改色（不触发回调），并同步 HSV 状态。 */
    public void setColor(int rgb, boolean notify) {
        color = 0xFF000000 | (rgb & 0x00FFFFFF);
        float[] hsv = rgbToHsv((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF);
        hue = hsv[0];
        saturation = hsv[1];
        brightness = hsv[2];
        if (notify) {
            onChange.accept(color);
        }
    }

    public int getColor() {
        return color;
    }

    // ------------------------------------------------------------ 命中区域

    private int gridTop() {
        return getY() + PREVIEW_ROW + 2;
    }

    private int cellSize() {
        return Math.max(8, (getWidth() - GAP * (GRID_COLUMNS - 1)) / GRID_COLUMNS);
    }

    private int sliderTop(int index) {
        return gridTop() + GRID_ROWS * (cellSize() + GAP) + 2 + index * SLIDER_ROW;
    }

    private boolean sliderTrack(int index, int mouseY) {
        int top = sliderTop(index);
        return mouseY >= top + 6 && mouseY <= top + 18;
    }

    private float sliderRatio(int mouseX) {
        return Math.max(0.0F, Math.min(1.0F, (mouseX - getX()) / (float) Math.max(1, getWidth())));
    }

    private void applyHsvAndNotify() {
        int[] rgb = hsvToRgb(hue, saturation, brightness);
        setColor((0xFF << 24) | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2], true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !active || button != 0 || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        // 预设网格
        int size = cellSize();
        int top = gridTop();
        if (mouseY >= top && mouseY <= top + GRID_ROWS * (size + GAP)) {
            int row = (int) ((mouseY - top) / (size + GAP));
            int col = (int) ((mouseX - getX()) / (size + GAP));
            if (row >= 0 && row < GRID_ROWS && col >= 0 && col < GRID_COLUMNS) {
                int index = row * GRID_COLUMNS + col;
                setColor(PRESETS[index % PRESETS.length], true);
                return true;
            }
        }
        // 三条滑杆
        for (int index = 0; index < 3; index++) {
            if (sliderTrack(index, (int) mouseY)) {
                draggingSlider = index;
                sliderDragged(index, (int) mouseX);
                return true;
            }
        }
        return true;
    }

    private void sliderDragged(int index, int mouseX) {
        float ratio = sliderRatio(mouseX);
        switch (index) {
            case 0 -> hue = ratio * 359.0F;
            case 1 -> saturation = ratio;
            default -> brightness = 0.08F + ratio * 0.92F;
        }
        applyHsvAndNotify();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingSlider >= 0 && button == 0) {
            sliderDragged(draggingSlider, (int) mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingSlider = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // ------------------------------------------------------------ 渲染

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        graphics.fill(x, y, x + width, y + PREVIEW_ROW, UiTheme.PANEL_RAISED);
        graphics.fill(x + 2, y + 2, x + 16, y + PREVIEW_ROW - 2, color);
        graphics.renderOutline(x + 2, y + 2, 14, PREVIEW_ROW - 4, UiTheme.BORDER);
        graphics.drawString(font, String.format(java.util.Locale.ROOT, "#%06X", color & 0xFFFFFF),
                x + 22, y + 4, UiTheme.TEXT, false);
        int size = cellSize();
        int top = gridTop();
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                int index = row * GRID_COLUMNS + col;
                int cellX = x + col * (size + GAP);
                int cellY = top + row * (size + GAP);
                int preset = 0xFF000000 | (PRESETS[index % PRESETS.length] & 0x00FFFFFF);
                graphics.fill(cellX, cellY, cellX + size, cellY + size, preset);
                boolean sameColor = (preset & 0xFFFFFF) == (color & 0xFFFFFF);
                boolean hovered = mouseX >= cellX && mouseX <= cellX + size
                        && mouseY >= cellY && mouseY <= cellY + size;
                graphics.renderOutline(cellX, cellY, size, size,
                        sameColor ? UiTheme.TEXT : hovered ? UiTheme.ACCENT : UiTheme.BORDER_SUBTLE);
            }
        }
        for (int index = 0; index < 3; index++) {
            renderSlider(graphics, index, mouseX, mouseY);
        }
    }

    private void renderSlider(GuiGraphics graphics, int index, int mouseX, int mouseY) {
        int top = sliderTop(index);
        float ratio = switch (index) {
            case 0 -> hue / 359.0F;
            case 1 -> saturation;
            default -> (brightness - 0.08F) / 0.92F;
        };
        String label = switch (index) {
            case 0 -> "色相";
            case 1 -> "饱和";
            default -> "明度";
        };
        String value = switch (index) {
            case 0 -> Math.round(hue) + "°";
            case 1 -> Math.round(saturation * 100) + "%";
            default -> Math.round(brightness * 100) + "%";
        };
        graphics.drawString(font, label, getX(), top - 2, UiTheme.MUTED, false);
        graphics.drawString(font, value, getX() + 26, top - 2, UiTheme.SUBTLE, false);
        int trackLeft = getX() + 52;
        int trackWidth = Math.max(12, getWidth() - 56);
        int trackY = top + 7;
        if (index == 0) {
            for (int offset = 0; offset < trackWidth; offset++) {
                int[] rgb = hsvToRgb(offset * 359.0F / trackWidth, 0.9F, 0.95F);
                graphics.fill(trackLeft + offset, trackY, trackLeft + offset + 1, trackY + 5,
                        0xFF000000 | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2]);
            }
        } else {
            graphics.fill(trackLeft, trackY, trackLeft + trackWidth, trackY + 5, UiTheme.BORDER_SUBTLE);
            graphics.fill(trackLeft, trackY, trackLeft + Math.round(trackWidth * ratio), trackY + 5,
                    UiTheme.ACCENT);
        }
        int knobX = trackLeft + Math.round((trackWidth - 3) * Math.max(0.0F, Math.min(1.0F, ratio)));
        graphics.fill(knobX, trackY - 2, knobX + 3, trackY + 7,
                draggingSlider == index || isMouseOver(mouseX, mouseY) ? UiTheme.TEXT : UiTheme.MUTED);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        narration.add(NarratedElementType.TITLE, getMessage());
    }

    // ------------------------------------------------------------ HSV 换算

    private static float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255.0F;
        float gf = g / 255.0F;
        float bf = b / 255.0F;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;
        float h;
        if (delta <= 0.00001F) {
            h = 0.0F;
        } else if (max == rf) {
            h = 60.0F * (((gf - bf) / delta) % 6.0F);
        } else if (max == gf) {
            h = 60.0F * ((bf - rf) / delta + 2.0F);
        } else {
            h = 60.0F * ((rf - gf) / delta + 4.0F);
        }
        if (h < 0) {
            h += 360.0F;
        }
        float s = max <= 0.00001F ? 0.0F : delta / max;
        return new float[] {h, s, max};
    }

    private static int[] hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1.0F - Math.abs(((h / 60.0F) % 2.0F) - 1.0F));
        float m = v - c;
        float rf;
        float gf;
        float bf;
        if (h < 60) {
            rf = c; gf = x; bf = 0;
        } else if (h < 120) {
            rf = x; gf = c; bf = 0;
        } else if (h < 180) {
            rf = 0; gf = c; bf = x;
        } else if (h < 240) {
            rf = 0; gf = x; bf = c;
        } else if (h < 300) {
            rf = x; gf = 0; bf = c;
        } else {
            rf = c; gf = 0; bf = x;
        }
        return new int[] {
                Math.round((rf + m) * 255.0F),
                Math.round((gf + m) * 255.0F),
                Math.round((bf + m) * 255.0F),
        };
    }
}
