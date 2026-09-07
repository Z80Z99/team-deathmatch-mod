package cn.blockforge.generated.generatedmod.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** 统一竞技风格按钮，保留原版键盘焦点和按键行为。 */
public class UiButton extends Button {
    private final Kind kind;
    private boolean selected;
    private int historyIcon;
    public UiButton historyIcon(boolean redo) { historyIcon = redo ? 1 : -1; return this; }

    public UiButton(int x, int y, int width, int height, Component message, OnPress onPress, Kind kind) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.kind = kind == null ? Kind.SECONDARY : kind;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int background = !active ? UiTheme.PANEL : selected ? kind.hover
                : isHoveredOrFocused() ? kind.hover : kind.background;
        int border = !active ? UiTheme.BORDER_SUBTLE : selected || isHoveredOrFocused() ? kind.accent : kind.border;
        int text = active ? UiTheme.TEXT : UiTheme.SUBTLE;

        graphics.fill(x, y, x + width, y + height, background);
        graphics.renderOutline(x, y, width, height, border);
        if (selected) graphics.fill(x + 1, y + height - 3, x + width - 1, y + height - 1, kind.accent);
        if (isFocused()) graphics.renderOutline(x + 2, y + 2, Math.max(1, width - 4), Math.max(1, height - 4), UiTheme.TEXT);

        Font font = Minecraft.getInstance().font;
        if (historyIcon != 0) {
            int cx = x + width / 2, cy = y + height / 2;
            for (int row = -3; row <= 3; row++) {
                int arrowX = cx + historyIcon * (4 - Math.abs(row));
                graphics.fill(arrowX, cy - 2 + row, arrowX + 1, cy - 1 + row, text);
            }
            graphics.fill(cx - 4, cy - 2, cx + 5, cy - 1, text);
            graphics.fill(cx - historyIcon * 4, cy - 1, cx - historyIcon * 4 + 1, cy + 4, text);
            graphics.fill(cx - 3, cy + 3, cx + 4, cy + 4, text);
            return;
        }
        String label = UiTheme.fit(font, getMessage().getString(), Math.max(1, width - labelPadding()));
        graphics.drawCenteredString(font, label, x + width / 2, y + Math.max(1, (height - 8) / 2), text);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    protected int labelPadding() { return 12; }

    public boolean isSelected() {
        return selected;
    }

    public enum Kind {
        SECONDARY(0xFF25272B, 0xFF383B40, UiTheme.MUTED, UiTheme.BORDER_SUBTLE),
        PRIMARY(0xFF315A3D, 0xFF41774F, UiTheme.ACCENT, 0xFF679F75),
        DANGER(0xFF38292D, 0xFF63353E, UiTheme.ERROR, 0xFF88505B),
        WARNING(0xFF37342A, 0xFF56503B, UiTheme.WARNING, 0xFF807653);

        private final int background;
        private final int hover;
        private final int accent;
        private final int border;

        Kind(int background, int hover, int accent, int border) {
            this.background = background;
            this.hover = hover;
            this.accent = accent;
            this.border = border;
        }
    }
}
