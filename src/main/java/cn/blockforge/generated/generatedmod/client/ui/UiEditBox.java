package cn.blockforge.generated.generatedmod.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/** 统一输入框背景、焦点边框和禁用态；文字与原版带边框输入框一样水平垂直居中。 */
public final class UiEditBox extends EditBox {
    /** 文字距左右边缘的内缩，和原版 bordered 输入框一致（各 4 像素）。 */
    private static final int TEXT_PADDING_X = 4;

    public UiEditBox(Font font, int x, int y, int width, int height, Component hint) {
        super(font, x, y, width, height, hint);
        setBordered(false);
        setTextColor(UiTheme.TEXT);
        setTextColorUneditable(UiTheme.MUTED);
        setHint(hint);
    }

    @Override
    public int getInnerWidth() {
        return Math.max(1, getWidth() - TEXT_PADDING_X * 2);
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        moveCursorToStart();
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        // 文字整体右移了 TEXT_PADDING_X，点击换算光标点位置时做同样补偿，选字才不会偏。
        // 拖拽选择走的是同一入口（AbstractWidget#onDrag 会回调 onClick），一并覆盖。
        super.onClick(mouseX - TEXT_PADDING_X, mouseY);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        int background = isActive() ? 0xFF111214 : UiTheme.PANEL;
        int border = isActive() && (isFocused() || isHovered()) ? UiTheme.ACCENT : UiTheme.BORDER_SUBTLE;
        graphics.fill(x, y, x + width, y + height, background);
        graphics.renderOutline(x, y, width, height, border);
        // bordered=false 时原版文字从控件左上角开始。只在绘制阶段平移画布，
        // 不改变控件自身坐标，避免布局/鼠标命中在渲染期间看到一套临时几何。
        graphics.enableScissor(x + TEXT_PADDING_X, y + 1,
                x + Math.max(TEXT_PADDING_X + 1, width - TEXT_PADDING_X), y + height - 1);
        graphics.pose().pushPose();
        graphics.pose().translate(TEXT_PADDING_X, Math.max(0, (height - 8) / 2), 0.0F);
        try {
            super.renderWidget(graphics, mouseX - TEXT_PADDING_X, mouseY, partialTick);
        } finally {
            graphics.pose().popPose();
            graphics.disableScissor();
        }
    }
}
