package cn.blockforge.generated.generatedmod.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.List;

public final class UiConfirmScreen extends UiScreen {
    private final Screen parent;
    private final String message;
    private final Runnable action;
    private List<FormattedCharSequence> lines;
    private int messageY;

    public UiConfirmScreen(Screen parent, String title, String message, Runnable action) {
        super(Component.literal(title));
        this.parent = parent;
        this.message = message;
        this.action = action;
    }

    @Override
    protected void init() {
        beginLayout(420, 180, BUTTON_HEIGHT, false);
        lines = font.split(Component.literal(message), innerWidth);
        messageY = flowRow(lines.size() * 14);
        footerButton("取消", 0, 2, 0, this::onClose, null, UiButton.Kind.SECONDARY);
        footerButton("确认", 1, 2, 0, () -> {
            onClose();
            action.run();
        }, null, UiButton.Kind.DANGER);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderShell(graphics, null);
        for (int i = 0; i < lines.size(); i++) {
            int y = messageY + i * 14;
            FormattedCharSequence line = lines.get(i);
            paintBand(graphics, y, 10, () -> graphics.drawString(font, line, innerLeft, y, UiTheme.MUTED, false));
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
