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

    public UiButton(int x, int y, int width, int height, Component message, OnPress onPress, Kind kind) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.kind = kind == null ? Kind.SECONDARY : kind;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int background = !active ? 0xB51B232B : selected ? kind.hover
                : isHoveredOrFocused() ? kind.hover : kind.background;
        int border = !active ? 0xFF34404A : selected || isFocused() ? kind.accent : kind.border;
        int text = active ? UiTheme.TEXT : UiTheme.SUBTLE;

        graphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, UiTheme.SHADOW);
        graphics.fill(x, y, x + width, y + height, background);
        graphics.renderOutline(x, y, width, height, border);
        graphics.fill(x, y, x + width, y + 2, active ? kind.accent : UiTheme.SUBTLE);
        graphics.fill(x, y + height - 2, x + 2, y + height, active ? kind.accent : UiTheme.SUBTLE);

        Font font = Minecraft.getInstance().font;
        String label = UiTheme.fit(font, getMessage().getString(), Math.max(1, width - 12));
        graphics.drawCenteredString(font, label, x + width / 2, y + Math.max(1, (height - 8) / 2), text);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    public enum Kind {
        SECONDARY(0xD8222D38, 0xF0354654, 0xFF4B667A, UiTheme.BORDER),
        PRIMARY(0xD8274E55, 0xF039716F, 0xFF45D3C2, 0xFF70E3D4),
        DANGER(0xD83A252B, 0xF05B3039, 0xFFE05D69, 0xFFFF8990),
        WARNING(0xD8463925, 0xF06E592D, 0xFFFFB84D, 0xFFFFD27A);

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
