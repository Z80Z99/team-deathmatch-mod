package cn.blockforge.generated.generatedmod.client.ui;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** 使用统一按钮外观的键盘可操作循环选择控件。 */
public final class UiCycleButton<T> extends UiButton {
    private final List<T> values;
    private final Function<T, String> display;
    private final Consumer<T> onValueChange;
    private int index;

    public UiCycleButton(int x, int y, int width, int height, List<T> values, T initial,
                         Function<T, String> display, Consumer<T> onValueChange,
                         String tooltip, Kind kind) {
        super(x, y, width, height, Component.literal(initial == null ? "" : display.apply(initial)),
                ignored -> { }, kind);
        this.values = List.copyOf(values);
        this.display = display;
        this.onValueChange = onValueChange;
        this.index = findInitial(initial);
        if (tooltip != null && !tooltip.isBlank()) {
            setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(tooltip)));
        }
    }

    @Override
    public void onPress() {
        if (!active || values.isEmpty()) {
            return;
        }
        var screen = net.minecraft.client.Minecraft.getInstance().screen;
        if (!(getValue() instanceof Boolean) && screen instanceof UiChoiceHost host) {
            host.choicePopup().open(this, screen);
            return;
        }
        choose((index + 1) % values.size());
    }

    public int optionCount() { return values.size(); }
    public int selectedIndex() { return index; }
    public String optionLabel(int option) { return display.apply(values.get(option)); }
    public void choose(int option) {
        if (!active || option < 0 || option >= values.size()) return;
        index = option;
        T value = values.get(index);
        setMessage(Component.literal(display.apply(value)));
        if (onValueChange != null) {
            onValueChange.accept(value);
        }
    }

    public T getValue() {
        return values.isEmpty() ? null : values.get(index);
    }

    @Override
    protected void renderWidget(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (getValue() instanceof Boolean flag) {
            int x = getX(), y = getY();
            graphics.fill(x, y, x + getWidth(), y + getHeight(), UiTheme.PANEL_RAISED);
            int trackWidth = Math.min(24, getWidth() - 8);
            int trackX = x + getWidth() - trackWidth - 4;
            int centerY = y + getHeight() / 2;
            int color = !active ? UiTheme.SUBTLE : flag ? UiTheme.ACCENT : UiTheme.MUTED;
            graphics.fill(trackX, centerY - 5, trackX + trackWidth, centerY + 5, flag ? UiTheme.withAlpha(color, 30) : UiTheme.BORDER_SUBTLE);
            int knobX = flag ? trackX + trackWidth - 8 : trackX + 1;
            graphics.fill(knobX, centerY - 4, knobX + 7, centerY + 4, color);
            var font = net.minecraft.client.Minecraft.getInstance().font;
            graphics.drawString(font, UiTheme.fit(font, getMessage().getString(), getWidth() - trackWidth - 14), x + 5, centerY - 4, color, false);
            if (isHoveredOrFocused()) graphics.renderOutline(x, y, getWidth(), getHeight(), color);
            return;
        }
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        int x = getX() + getWidth() - 10, y = getY() + getHeight() / 2;
        for (int row = 0; row < 3; row++) graphics.fill(x + row, y - 1 + row, x + 5 - row, y + row,
                active ? UiTheme.MUTED : UiTheme.SUBTLE);
    }

    @Override protected int labelPadding() { return 30; }

    /** 更新显示值但不触发回调，适合重置草稿或接收服务器同步。 */
    public void setValue(T value) {
        int next = findInitial(value);
        if (values.isEmpty() || next < 0 || next >= values.size()) {
            return;
        }
        index = next;
        setMessage(Component.literal(display.apply(values.get(index))));
    }

    private int findInitial(T initial) {
        if (initial == null) {
            return 0;
        }
        for (int candidate = 0; candidate < values.size(); candidate++) {
            if (initial.equals(values.get(candidate))) {
                return candidate;
            }
        }
        return 0;
    }
}
