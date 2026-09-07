package cn.blockforge.generated.generatedmod.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** A bounded option menu owned by its screen, including keyboard selection. */
public final class UiChoicePopup {
    private static final int ROW = 20;
    private UiCycleButton<?> source;
    private int left, top, width, rows, selected, offset;
    public boolean isOpen() { return source != null; }
    public void close() { source = null; }
    public void open(UiCycleButton<?> button, Screen screen) {
        source = button;
        width = Math.min(Math.max(button.getWidth(), 180), screen.width - 12);
        rows = Math.min(button.optionCount(), Math.min(8, Math.max(1, (screen.height - 16) / ROW)));
        left = Math.max(6, Math.min(button.getX(), screen.width - width - 6));
        top = button.getY() + button.getHeight() + 2;
        if (top + rows * ROW > screen.height - 6) top = Math.max(6, button.getY() - rows * ROW - 2);
        selected = button.selectedIndex(); offset = Math.max(0, selected - rows + 1);
    }
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (!isOpen()) return;
        graphics.pose().pushPose(); graphics.pose().translate(0, 0, 400);
        graphics.fill(left - 1, top - 1, left + width + 1, top + rows * ROW + 1, UiTheme.BORDER);
        graphics.fill(left, top, left + width, top + rows * ROW, UiTheme.PANEL);
        for (int row = 0; row < rows; row++) {
            int index = offset + row;
            int y = top + row * ROW;
            if (index == selected || (mouseX >= left && mouseX < left + width && mouseY >= y && mouseY < y + ROW))
                graphics.fill(left, y, left + width, y + ROW, UiTheme.PANEL_SELECTED);
            graphics.drawString(font, UiTheme.fit(font, source.optionLabel(index), width - 18), left + 7, y + 6,
                    index == source.selectedIndex() ? UiTheme.ACCENT : UiTheme.TEXT, false);
        }
        if (source.optionCount() > rows) {
            int h = rows * ROW; int thumb = Math.max(8, h * rows / source.optionCount());
            int y = top + (h - thumb) * offset / (source.optionCount() - rows);
            graphics.fill(left + width - 3, y, left + width - 1, y + thumb, UiTheme.ACCENT);
        }
        graphics.pose().popPose();
    }
    public boolean click(double x, double y, int button) {
        if (!isOpen()) return false;
        if (button == 0 && x >= left && x < left + width && y >= top && y < top + rows * ROW) {
            int index = offset + (int) (y - top) / ROW;
            UiCycleButton<?> target = source; close(); target.choose(index);
        } else close();
        return true;
    }
    public boolean scroll(double amount) {
        if (!isOpen()) return false;
        offset = Math.max(0, Math.min(source.optionCount() - rows, offset - (int) Math.round(amount)));
        return true;
    }
    public boolean key(int key) {
        if (!isOpen()) return false;
        if (key == 256 || key == 258) close();
        else if (key == 257 || key == 335 || key == 32) { UiCycleButton<?> target = source; close(); target.choose(selected); }
        else if (key == 264 || key == 265) {
            selected = Math.max(0, Math.min(source.optionCount() - 1, selected + (key == 264 ? 1 : -1)));
            offset = Math.min(selected, Math.max(offset, selected - rows + 1));
        }
        return true;
    }
}
