package cn.blockforge.generated.generatedmod.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.Locale;

/** Drawing and input use the same filtered list. */
public abstract class UiListScreen<T> extends UiScreen {
    private static final int ROW_HEIGHT = 30;
    private UiEditBox search;
    private String query = "";
    private String selectedId = "";
    private List<T> shown = List.of();
    private int listY;
    private int listHeight;
    private int offset;

    protected UiListScreen(Component title) { super(title); }
    protected abstract List<T> entries();
    protected abstract String entryId(T entry);
    protected abstract String entryTitle(T entry);
    protected abstract String entryDetail(T entry);
    protected abstract String entryBadge(T entry);
    protected int entryColor(T entry) { return UiTheme.MUTED; }

    protected final void addSearch(String hint) {
        int y = flowRow(BUTTON_HEIGHT);
        search = flowWidget(new UiEditBox(font, innerLeft, y, innerWidth, BUTTON_HEIGHT, Component.literal(hint)), y);
        search.setMaxLength(80);
        search.setValue(query);
        search.setResponder(value -> { query = value; offset = 0; refreshEntries(); });
    }

    protected final void addList() {
        listHeight = Math.max(ROW_HEIGHT, (contentBottom - flowCursor()) / ROW_HEIGHT * ROW_HEIGHT);
        listY = flowRow(listHeight);
        refreshEntries();
    }

    protected final void refreshEntries() {
        String term = query.strip().toLowerCase(Locale.ROOT);
        shown = entries().stream().filter(entry -> term.isEmpty()
                || (entryTitle(entry) + " " + entryDetail(entry) + " " + entryId(entry))
                        .toLowerCase(Locale.ROOT).contains(term)).toList();
        if (shown.stream().noneMatch(entry -> entryId(entry).equals(selectedId))) selectedId = shown.isEmpty() ? "" : entryId(shown.get(0));
        offset = clamp(offset, 0, Math.max(0, shown.size() - visibleRows()));
    }
    protected final T selectedEntry() { return shown.stream().filter(entry -> entryId(entry).equals(selectedId)).findFirst().orElse(null); }
    protected final void selectEntry(String id) { selectedId = id == null ? "" : id; refreshEntries(); }
    protected final int resultCount() { return shown.size(); }
    private int visibleRows() { return Math.max(1, listHeight / ROW_HEIGHT); }

    protected final void renderList(GuiGraphics graphics, int mouseX, int mouseY, String empty) {
        if (shown.isEmpty()) {
            paintBand(graphics, listY, 12, () -> graphics.drawString(font,
                    fit(query.isBlank() ? empty : "没有符合搜索条件的结果", innerWidth - 12), innerLeft + 6, listY + 2, UiTheme.MUTED, false));
            return;
        }
        for (int row = 0; row < visibleRows() && offset + row < shown.size(); row++) {
            T entry = shown.get(offset + row);
            int y = listY + row * ROW_HEIGHT;
            paintBand(graphics, y, ROW_HEIGHT, () -> {
                boolean selected = entryId(entry).equals(selectedId);
                boolean hovered = mouseX >= innerLeft && mouseX < innerLeft + innerWidth
                        && mouseY >= bandScreenY(y) && mouseY < bandScreenY(y) + ROW_HEIGHT;
                if (selected || hovered) graphics.fill(innerLeft, y, innerLeft + innerWidth - 5,
                        y + ROW_HEIGHT - 1, selected ? UiTheme.PANEL_SELECTED : UiTheme.PANEL_RAISED);
                graphics.fill(innerLeft, y + 5, innerLeft + 2, y + ROW_HEIGHT - 6, selected ? UiTheme.ACCENT : entryColor(entry));
                int badgeWidth = Math.min(96, innerWidth / 3);
                int textWidth = innerWidth - badgeWidth - 22;
                graphics.drawString(font, fit(entryTitle(entry), textWidth), innerLeft + 8, y + 4, UiTheme.TEXT, false);
                graphics.drawString(font, fit(entryDetail(entry), textWidth), innerLeft + 8, y + 17, UiTheme.MUTED, false);
                graphics.drawString(font, fit(entryBadge(entry), badgeWidth - 6), innerLeft + innerWidth - badgeWidth - 6, y + 11, entryColor(entry), false);
                divider(graphics, innerLeft + 8, y + ROW_HEIGHT - 1, innerWidth - 16);
            });
        }
        if (shown.size() > visibleRows()) paintBand(graphics, listY, listHeight, () -> {
            int thumb = Math.max(10, listHeight * visibleRows() / shown.size());
            int top = listY + (listHeight - thumb) * offset / (shown.size() - visibleRows());
            graphics.fill(innerLeft + innerWidth - 3, listY, innerLeft + innerWidth - 1, listY + listHeight, UiTheme.BORDER_SUBTLE);
            graphics.fill(innerLeft + innerWidth - 3, top, innerLeft + innerWidth - 1, top + thumb, UiTheme.ACCENT);
        });
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int top = bandScreenY(listY);
        if (button == 0 && mouseX >= innerLeft && mouseX < innerLeft + innerWidth
                && mouseY >= Math.max(top, contentTop) && mouseY < Math.min(top + listHeight, contentBottom)) {
            int row = (int) (mouseY - top) / ROW_HEIGHT;
            int index = offset + row;
            if (index < shown.size() && bandFits(listY + row * ROW_HEIGHT, ROW_HEIGHT)) {
                selectedId = entryId(shown.get(index)); setFocused(null); return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int top = bandScreenY(listY);
        if (shown.size() > visibleRows() && bandFits(listY, listHeight) && mouseX >= innerLeft
                && mouseX < innerLeft + innerWidth && mouseY >= top && mouseY < top + listHeight) {
            offset = clamp(offset - (int) Math.round(amount), 0, shown.size() - visibleRows()); return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!(getFocused() instanceof EditBox) && !shown.isEmpty() && (keyCode == 264 || keyCode == 265)) {
            int index = clamp(shown.indexOf(selectedEntry()) + (keyCode == 264 ? 1 : -1), 0, shown.size() - 1);
            selectedId = entryId(shown.get(index));
            offset = clamp(offset, Math.max(0, index - visibleRows() + 1), index); return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override public void tick() { super.tick(); if (search != null) search.tick(); }
    @Override public boolean isPauseScreen() { return false; }
}
