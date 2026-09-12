package cn.blockforge.generated.generatedmod.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * 拖动式整数滑条：适合 0～100 位置、缩放等需要即时反馈的配置项，
 * 点按轨道可直接跳转，方向键可微调。
 */
public final class UiSlider extends AbstractWidget {
    private final Font labelFont;
    private final int minimum;
    private final int maximum;
    private final IntSupplier valueSource;
    private final Consumer<Integer> onValueChange;
    private final Consumer<Integer> onValueCommit;
    private final String name;
    private final String unit;
    private boolean dragging;

    public UiSlider(Font font, int x, int y, int width, int height, int minimum, int maximum,
                    IntSupplier valueSource, Consumer<Integer> onValueChange, String name, String unit) {
        this(font, x, y, width, height, minimum, maximum, valueSource,
                onValueChange, null, name, unit);
    }

    /** 拖动时只回调本地值，松开或键盘微调时才回调提交值。 */
    public UiSlider(Font font, int x, int y, int width, int height, int minimum, int maximum,
                    IntSupplier valueSource, Consumer<Integer> onValueChange,
                    Consumer<Integer> onValueCommit, String name, String unit) {
        super(x, y, width, height, Component.literal(name));
        this.labelFont = font;
        this.minimum = minimum;
        this.maximum = Math.max(minimum, maximum);
        this.valueSource = valueSource;
        this.onValueChange = onValueChange;
        this.onValueCommit = onValueCommit;
        this.name = name;
        this.unit = unit == null ? "" : unit;
    }

    /** 配置窗缩放/滚动时重新摆位。 */
    public void setWindow(int x, int y, int width) {
        setX(x);
        setY(y);
        setWidth(Math.max(24, width));
    }

    private int clampValue(int value) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private int positionToValue(int mouseX) {
        int usable = Math.max(1, getWidth() - 6);
        int offset = Math.max(0, Math.min(usable, mouseX - getX() - 3));
        return minimum + (int) Math.round(offset * (double) (maximum - minimum) / usable);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (active && visible && isMouseOver(mouseX, mouseY) && button == 0) {
            dragging = true;
            setLocal(clampValue(positionToValue((int) mouseX)));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            setLocal(clampValue(positionToValue((int) mouseX)));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            dragging = false;
            commitLocal();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int step = Math.max(1, (maximum - minimum) / 40);
        if (keyCode == 263 || keyCode == 262) {
            setAndCommit(clampValue(valueSource.getAsInt() + (keyCode == 262 ? step : -step)));
            return true;
        }
        if (keyCode == 264 || keyCode == 265) {
            setAndCommit(clampValue(valueSource.getAsInt() + (keyCode == 265 ? step : -step)));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void setLocal(int value) {
        if (value == clampValue(valueSource.getAsInt())) return;
        onValueChange.accept(value);
    }

    private void setAndCommit(int value) {
        setLocal(value);
        commitLocal();
    }

    private void commitLocal() {
        if (onValueCommit != null) onValueCommit.accept(clampValue(valueSource.getAsInt()));
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int value = clampValue(valueSource.getAsInt());
        int percent = maximum == minimum ? 0 : (value - minimum) * 100 / (maximum - minimum);
        int x = getX();
        int y = getY();
        int width = getWidth();
        int trackY = y + (getHeight() - 2) / 2;
        graphics.fill(x, trackY, x + width, trackY + 2, UiTheme.BORDER_SUBTLE);
        graphics.fill(x, trackY, x + Math.round(width * percent / 100.0F), trackY + 2,
                active ? UiTheme.ACCENT : UiTheme.BORDER);
        int knobX = x + Math.round((width - 6) * percent / 100.0F);
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y - 9 && mouseY <= y + getHeight();
        graphics.fill(knobX, y, knobX + 5, y + getHeight(),
                active && (hovered || dragging) ? UiTheme.TEXT : UiTheme.MUTED);
        graphics.drawString(labelFont, UiTheme.fit(labelFont, name + "  " + value + unit, width), x, y - 9,
                active ? UiTheme.MUTED : UiTheme.SUBTLE, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        narration.add(NarratedElementType.TITLE, getMessage());
    }

    public void setTooltipText(String tooltip) {
        if (tooltip != null && !tooltip.isBlank()) {
            setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
    }
}
