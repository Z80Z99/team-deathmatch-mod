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
        index = (index + 1) % values.size();
        T value = values.get(index);
        setMessage(Component.literal(display.apply(value)));
        if (onValueChange != null) {
            onValueChange.accept(value);
        }
    }

    public T getValue() {
        return values.isEmpty() ? null : values.get(index);
    }

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
