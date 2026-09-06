package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientMapEditorData;
import cn.blockforge.generated.generatedmod.client.ui.UiButton;
import cn.blockforge.generated.generatedmod.client.ui.UiEditBox;
import cn.blockforge.generated.generatedmod.client.ui.UiScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import cn.blockforge.generated.generatedmod.map.MapEditorAction;
import cn.blockforge.generated.generatedmod.map.MapEditorView;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.MapEditorActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 地图工作台：管理“我的地图”——新建、删除、生成/撤销邀请码、用邀请码导入副本、选择编辑目标。
 * 每张地图只有拥有者可编辑；导入的副本独立存在，编辑互不影响。管理员额外可见服务器与其他玩家的地图。
 */
public final class MapLibraryScreen extends UiScreen {
    private static final int MAP_ROW = 24;

    private final Screen parent;
    private UiEditBox codeBox;
    private UiButton createButton;
    private UiButton editButton;
    private UiButton deleteButton;
    private UiButton generateCodeButton;
    private UiButton revokeCodeButton;
    private UiButton importButton;
    private UiButton refreshButton;
    private String selectedMapId = "";
    private int titleY;
    private int listTop;
    private int listBottom;
    private boolean listVisible;
    private int listScroll;
    private int ticks;
    private int lastRevision = -1;
    private String importHint = "";
    private int importHintColor = UiTheme.MUTED;

    private record MapRow(String id, String displayName, String badgeText, boolean owned) {
    }

    public MapLibraryScreen(Screen parent) {
        super(Component.literal("地图工作台"));
        this.parent = parent;
    }

    public static void open(Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.getConnection() != null) {
            MapLibraryScreen screen = new MapLibraryScreen(parent);
            minecraft.setScreen(screen);
            screen.send(MapEditorAction.REQUEST, "");
        }
    }

    @Override
    protected void init() {
        beginLayout(640, 0, BUTTON_HEIGHT);
        int importRowY = flowRow(BUTTON_HEIGHT);
        int manageRowY = flowRow(BUTTON_HEIGHT);
        int shareRowY = flowRow(BUTTON_HEIGHT);
        titleY = flowRow(13); // “我的地图”标题带

        int gap = 6;
        int codeWidth = Math.max(70, Math.min(120, innerWidth / 5));
        int perButton = Math.max(1, (innerWidth - codeWidth - gap * 3) / 3);
        codeBox = flowWidget(new UiEditBox(font, innerLeft, importRowY, codeWidth, BUTTON_HEIGHT,
                Component.literal("邀请码")), importRowY);
        codeBox.setMaxLength(6);
        codeBox.setFilter(text -> text.chars().allMatch(c -> Character.isLetterOrDigit(c) && c < 128));
        codeBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.literal("输入其他玩家分享的 6 位邀请码，导入一份属于你自己的独立副本。")));
        int importX = innerLeft + codeWidth + gap;
        importButton = flowWidget(uiButton("导入副本", importX, importRowY, perButton,
                this::importByCode, "用邀请码导入；导入后副本归你所有，可独立编辑。",
                UiButton.Kind.PRIMARY), importRowY);
        createButton = flowWidget(uiButton("新建地图", innerLeft, manageRowY, perButton,
                () -> MapCreateScreen.open(this),
                "在你当前的维度和位置创建一张归你所有的地图。", UiButton.Kind.PRIMARY), manageRowY);
        deleteButton = flowWidget(uiButton("删除所选", innerLeft + perButton + gap, manageRowY, perButton,
                this::deleteSelected, "删除选中的地图（仅限你拥有的地图）。", UiButton.Kind.DANGER), manageRowY);
        editButton = flowWidget(uiButton("编辑所选", innerLeft + (perButton + gap) * 2, manageRowY, perButton,
                this::editSelected, "把选中的地图设为编辑目标，并打开地图编辑器。",
                UiButton.Kind.PRIMARY), manageRowY);
        int shareWidth = Math.max(1, (innerWidth - gap) / 2);
        generateCodeButton = flowWidget(uiButton("生成邀请码", innerLeft, shareRowY, shareWidth,
                () -> shareAction(MapEditorAction.GENERATE_SHARE),
                "为选中的地图生成（或复用）6 位邀请码，发给想分享的玩家。",
                UiButton.Kind.SECONDARY), shareRowY);
        revokeCodeButton = flowWidget(uiButton("撤销邀请码", innerLeft + shareWidth + gap, shareRowY,
                innerWidth - shareWidth - gap,
                () -> shareAction(MapEditorAction.REVOKE_SHARE),
                "撤销选中地图的邀请码，之后别人无法再导入。", UiButton.Kind.SECONDARY), shareRowY);
        refreshButton = footerButton("刷新", 0, 2, 0, () -> send(MapEditorAction.REQUEST, ""),
                "重新读取我的地图与邀请码。", UiButton.Kind.SECONDARY);
        footerButton("返回", 1, 2, 0, this::onClose, "返回功能中心。", UiButton.Kind.DANGER);

        listTop = flowCursor() + 2;
        contentBottom = Math.min(contentBottom, listTop);
        listVisible = statusTop - 2 - listTop >= MAP_ROW + 4;
        listBottom = listTop;
        reconcileSelection();
        updateButtons();
    }

    // ------------------------------------------------------------- data

    private List<MapRow> rows() {
        MapEditorView view = ClientMapEditorData.view();
        List<MapRow> rows = new ArrayList<>();
        for (String entry : view.ownedMaps()) {
            String[] parts = entry.split("\\|", -1);
            String id = parts.length > 0 ? parts[0] : "";
            String name = parts.length > 1 ? parts[1] : id;
            String code = parts.length > 2 ? parts[2] : "";
            rows.add(new MapRow(id, name, code.isEmpty() ? "无邀请码" : "码 " + code, true));
        }
        for (String entry : view.serverMaps()) {
            String[] parts = entry.split("\\|", -1);
            String id = parts.length > 0 ? parts[0] : "";
            String name = parts.length > 1 ? parts[1] : id;
            String owner = parts.length > 2 ? parts[2] : "服务器";
            rows.add(new MapRow(id, name, "属主 " + owner, false));
        }
        return rows;
    }

    private MapRow selectedRow() {
        for (MapRow row : rows()) {
            if (row.id().equals(selectedMapId)) {
                return row;
            }
        }
        return null;
    }

    private void reconcileSelection() {
        List<MapRow> rows = rows();
        if (rows.isEmpty() || selectedMapId.isBlank()) {
            if (!rows.isEmpty() && selectedMapId.isBlank()) {
                selectedMapId = rows.get(0).id();
            }
            if (rows.isEmpty()) {
                selectedMapId = "";
            }
            return;
        }
        boolean still = rows.stream().anyMatch(row -> row.id().equals(selectedMapId));
        if (!still) {
            selectedMapId = rows.get(0).id();
        }
    }

    private int visibleRows() {
        return listVisible ? Math.max(1, (statusTop - 2 - listTop) / MAP_ROW) : 0;
    }

    private int maxListScroll() {
        return Math.max(0, rows().size() - visibleRows());
    }

    // ------------------------------------------------------------ actions

    private void send(MapEditorAction action, String mapId) {
        if (minecraft != null && minecraft.getConnection() != null) {
            FpsTdmNetwork.sendToServer(new MapEditorActionPacket(action, mapId));
        }
    }

    private void importByCode() {
        String code = codeBox == null ? "" : codeBox.getValue().trim();
        if (code.length() != 6) {
            setImportHint("邀请码是 6 位字母数字。", UiTheme.ERROR);
            return;
        }
        setImportHint("正在导入……", UiTheme.INFO);
        send(MapEditorAction.IMPORT_MAP, code);
    }

    private void shareAction(MapEditorAction action) {
        if (selectedMapId.isBlank()) {
            setImportHint("先在列表里选中一张地图。", UiTheme.ERROR);
            return;
        }
        send(action, selectedMapId);
    }

    private void deleteSelected() {
        if (selectedMapId.isBlank()) {
            setImportHint("先在列表里选中一张地图。", UiTheme.ERROR);
            return;
        }
        send(MapEditorAction.DELETE_MAP, selectedMapId);
    }

    private void editSelected() {
        if (selectedMapId.isBlank()) {
            setImportHint("先在列表里选中一张地图。", UiTheme.ERROR);
            return;
        }
        send(MapEditorAction.EDIT_MAP, selectedMapId);
        MapEditorScreen.open(this);
    }

    private void setImportHint(String message, int color) {
        importHint = message;
        importHintColor = color;
    }

    // -------------------------------------------------------------- hooks

    @Override
    public void tick() {
        super.tick();
        if (codeBox != null) {
            codeBox.tick();
        }
        ticks++;
        if (ClientMapEditorData.revision() != lastRevision) {
            lastRevision = ClientMapEditorData.revision();
            reconcileSelection();
            String message = ClientMapEditorData.view().message();
            if (!message.isBlank()) {
                setImportHint(message, ClientMapEditorData.view().error() ? UiTheme.ERROR : UiTheme.SUCCESS);
            }
        }
        updateButtons();
        if (ticks % 40 == 0) {
            send(MapEditorAction.POLL, "");
        }
    }

    private void updateButtons() {
        MapEditorView view = ClientMapEditorData.view();
        MapRow row = selectedRow();
        boolean hasSelection = row != null;
        boolean canManageSelection = hasSelection && (row.owned() || view.isAdmin());
        boolean locked = view.locked();
        if (createButton != null) createButton.active = !locked;
        if (importButton != null) importButton.active = !locked;
        if (codeBox != null) codeBox.setEditable(!locked);
        if (editButton != null) editButton.active = canManageSelection;
        if (deleteButton != null) deleteButton.active = canManageSelection;
        if (generateCodeButton != null) generateCodeButton.active = canManageSelection;
        if (revokeCodeButton != null) revokeCodeButton.active = canManageSelection;
        if (refreshButton != null) refreshButton.active = true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (listVisible && button == 0) {
            List<MapRow> rows = rows();
            int rowsHigh = Math.min(visibleRows(), rows.size()) * MAP_ROW;
            if (mouseX >= innerLeft && mouseX <= innerLeft + innerWidth
                    && mouseY >= listTop && mouseY < listTop + rowsHigh) {
                int index = listScroll + (int) ((mouseY - listTop) / MAP_ROW);
                if (index >= 0 && index < rows.size()) {
                    selectedMapId = rows.get(index).id();
                    updateButtons();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (listVisible && maxListScroll() > 0 && mouseY >= listTop && mouseY < statusTop - 2) {
            int next = clamp(listScroll - (int) Math.round(amount), 0, maxListScroll());
            if (next != listScroll) {
                listScroll = next;
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    // ----------------------------------------------------------- render

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderShell(graphics, "我的地图独立编辑；分享需要邀请码");
        reconcileSelection();
        updateButtons();

        String message = ClientMapEditorData.view().message();
        boolean messageIsError = ClientMapEditorData.view().error();
        String status = !importHint.isBlank() ? importHint : message;
        int statusColor = (messageIsError || importHintColor == UiTheme.ERROR)
                ? UiTheme.ERROR : importHintColor;
        if (status.isBlank()) {
            status = "选中的地图只有你本人可以编辑；邀请码分享出去的是独立副本。";
            statusColor = UiTheme.MUTED;
        }
        renderStatus(graphics, status, statusColor);
        final int title = titleY;
        paintBand(graphics, title, 13, () -> {
            section(graphics, ClientMapEditorData.view().isAdmin() ? "我的地图 / 服务器地图" : "我的地图",
                    innerLeft, title, innerWidth / 2);
            String hint = "点击选择 · 滚轮翻页";
            int hintWidth = font.width(hint);
            graphics.drawString(font, hint, innerLeft + innerWidth - hintWidth, title + 4,
                    UiTheme.SUBTLE, false);
        });

        if (listVisible) {
            listBottom = listTop + visibleRows() * MAP_ROW;
            renderMapList(graphics);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderMapList(GuiGraphics graphics) {
        List<MapRow> rows = rows();
        UiTheme.card(graphics, innerLeft, listTop, innerWidth,
                Math.max(MAP_ROW, (statusTop - 2) - listTop), 0x800D141B);
        if (rows.isEmpty()) {
            UiTheme.card(graphics, innerLeft + 2, listTop + 2, innerWidth - 4, MAP_ROW - 2,
                    UiTheme.PANEL_RAISED);
            graphics.drawString(font, fit("还没有地图：点击“新建地图”创建第一张，或输入邀请码导入副本。",
                    innerWidth - 20), innerLeft + 8, listTop + 9, UiTheme.MUTED, false);
            return;
        }
        int rowsHigh = visibleRows();
        for (int row = 0; row < rowsHigh; row++) {
            int index = listScroll + row;
            if (index >= rows.size()) {
                break;
            }
            renderMapRow(graphics, rows.get(index), listTop + row * MAP_ROW);
        }
        if (maxListScroll() > 0) {
            int x = innerLeft + innerWidth - 3;
            int trackHeight = rowsHigh * MAP_ROW;
            graphics.fill(x, listTop, x + 2, listTop + trackHeight, UiTheme.BORDER_SUBTLE);
            int thumbHeight = Math.max(MAP_ROW / 2, trackHeight * rowsHigh / Math.max(1, rows.size()));
            int range = Math.max(1, trackHeight - thumbHeight);
            int thumbTop = listTop + range * listScroll / Math.max(1, maxListScroll());
            graphics.fill(x, thumbTop, x + 2, thumbTop + thumbHeight, UiTheme.ACCENT);
        }
    }

    private void renderMapRow(GuiGraphics graphics, MapRow row, int rowTop) {
        int y = rowTop + 1;
        boolean selected = row.id().equals(selectedMapId);
        boolean target = row.id().equals(ClientMapEditorData.view().mapId());
        UiTheme.card(graphics, innerLeft + 2, y, innerWidth - 8, MAP_ROW - 4,
                selected ? UiTheme.PANEL_SELECTED : UiTheme.PANEL_RAISED);
        graphics.fill(innerLeft + 2, y, innerLeft + 5, y + MAP_ROW - 4,
                selected ? UiTheme.ACCENT : UiTheme.BORDER_SUBTLE);
        int badgeWidth = Math.max(56, Math.min(96, innerWidth / 6));
        int textWidth = Math.max(1, innerWidth - badgeWidth - 26);
        graphics.drawString(font, fit(row.displayName() + (target ? "  ·  当前编辑" : ""), textWidth),
                innerLeft + 10, y + 1, target ? UiTheme.ACCENT : UiTheme.TEXT, false);
        graphics.drawString(font, fit(row.id(), textWidth), innerLeft + 10, y + 11, UiTheme.MUTED, false);
        badge(graphics, row.badgeText(), innerLeft + innerWidth - badgeWidth - 4, y + 2, badgeWidth,
                row.owned() ? UiTheme.SUCCESS : UiTheme.SUBTLE);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
