package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientMapEditorData;
import cn.blockforge.generated.generatedmod.client.ui.*;
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

public final class MapLibraryScreen extends UiListScreen<MapLibraryScreen.MapRow> {
    record MapRow(String id, String name, String code, String owner, boolean owned) { }
    private final Screen parent;
    private UiEditBox code;
    private String codeDraft = "";
    private UiButton create, edit, delete, share, revoke, copy, importButton;
    private int ticks;
    private int revision = -1;
    private List<MapRow> cached = List.of();
    private String localStatus = "";

    public MapLibraryScreen(Screen parent) { super(Component.literal("地图工作台")); this.parent = parent; }
    public static void open(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.getConnection() != null) {
            mc.setScreen(new MapLibraryScreen(parent)); send(MapEditorAction.REQUEST, "");
        }
    }
    @Override protected void init() {
        beginLayout(700, 0, BUTTON_HEIGHT);
        addSearch("搜索地图名称、ID 或拥有者");
        int y = flowRow(BUTTON_HEIGHT);
        int gap = 6;
        code = flowWidget(new UiEditBox(font, columnX(0, 3, gap), y, columnWidth(3, gap), BUTTON_HEIGHT, Component.literal("6 位邀请码")), y);
        code.setMaxLength(6);
        code.setFilter(value -> value.chars().allMatch(c -> c < 128 && Character.isLetterOrDigit(c)));
        code.setValue(codeDraft); code.setResponder(value -> codeDraft = value);
        importButton = flowWidget(uiButton("导入副本", columnX(1, 3, gap), y, columnWidth(3, gap),
                () -> send(MapEditorAction.IMPORT_MAP, codeDraft), "导入独立地图副本", UiButton.Kind.SECONDARY), y);
        create = flowWidget(uiButton("新建地图", columnX(2, 3, gap), y, columnWidth(3, gap),
                () -> MapCreateScreen.open(this), null, UiButton.Kind.SECONDARY), y);
        int shareY = flowRow(BUTTON_HEIGHT);
        share = flowWidget(uiButton("生成邀请码", columnX(0, 3, gap), shareY, columnWidth(3, gap),
                () -> selectedAction(MapEditorAction.GENERATE_SHARE), null), shareY);
        copy = flowWidget(uiButton("复制邀请码", columnX(1, 3, gap), shareY, columnWidth(3, gap), () -> {
            MapRow row = selectedEntry();
            if (row != null && !row.code().isBlank()) { minecraft.keyboardHandler.setClipboard(row.code()); localStatus = "邀请码已复制"; }
        }, null), shareY);
        revoke = flowWidget(uiButton("撤销邀请码", columnX(2, 3, gap), shareY, columnWidth(3, gap), () -> {
            MapRow row = selectedEntry();
            if (row != null) confirmAction("撤销邀请码", "撤销「" + row.name() + "」的邀请码？已导入的副本仍会保留。",
                    () -> send(MapEditorAction.REVOKE_SHARE, row.id()));
        }, null), shareY);
        addList();
        footerButton("返回", 0, 3, 0, this::onClose, null, UiButton.Kind.SECONDARY);
        delete = footerButton("删除地图", 1, 3, 0, () -> {
            MapRow row = selectedEntry();
            if (row != null) confirmAction("删除地图", "删除「" + row.name() + "」的地图配置和关联数据？此操作不可撤销。",
                    () -> send(MapEditorAction.DELETE_MAP, row.id()));
        }, null, UiButton.Kind.DANGER);
        edit = footerButton("编辑地图", 2, 3, 0, () -> {
            MapRow row = selectedEntry();
            if (row != null) { send(MapEditorAction.EDIT_MAP, row.id()); MapEditorScreen.open(this); }
        }, null, UiButton.Kind.PRIMARY);
        updateButtons();
    }
    private static void send(MapEditorAction action, String value) {
        if (Minecraft.getInstance().getConnection() != null) FpsTdmNetwork.sendToServer(new MapEditorActionPacket(action, value));
    }
    private void selectedAction(MapEditorAction action) { MapRow row = selectedEntry(); if (row != null) send(action, row.id()); }
    @Override protected List<MapRow> entries() {
        if (revision == ClientMapEditorData.revision()) return cached;
        revision = ClientMapEditorData.revision();
        MapEditorView view = ClientMapEditorData.view();
        List<MapRow> rows = new ArrayList<>();
        for (String entry : view.ownedMaps()) {
            String[] parts = entry.split("\\|", -1);
            rows.add(new MapRow(parts[0], parts.length > 1 ? parts[1] : parts[0], parts.length > 2 ? parts[2] : "", "我的地图", true));
        }
        for (String entry : view.serverMaps()) {
            String[] parts = entry.split("\\|", -1);
            rows.add(new MapRow(parts[0], parts.length > 1 ? parts[1] : parts[0], "", parts.length > 2 ? parts[2] : "服务器", false));
        }
        cached = List.copyOf(rows); return cached;
    }
    @Override protected String entryId(MapRow row) { return row.id(); }
    @Override protected String entryTitle(MapRow row) { return row.name(); }
    @Override protected String entryDetail(MapRow row) { return row.owner() + " · " + row.id(); }
    @Override protected String entryBadge(MapRow row) { return !row.code().isBlank() ? row.code() : row.id().equals(ClientMapEditorData.view().mapId()) ? "当前编辑" : "未分享"; }
    @Override protected int entryColor(MapRow row) { return row.owned() ? UiTheme.SUCCESS : UiTheme.INFO; }
    private void updateButtons() {
        MapEditorView view = ClientMapEditorData.view();
        MapRow row = selectedEntry();
        boolean manage = row != null && (row.owned() || view.isAdmin()) && !view.locked();
        edit.active = manage; delete.active = manage; share.active = manage;
        revoke.active = manage && (!row.code().isBlank() || view.isAdmin());
        copy.active = row != null && !row.code().isBlank();
        create.active = !view.locked(); importButton.active = !view.locked() && codeDraft.length() == 6;
    }
    @Override public void tick() {
        super.tick(); code.tick();
        if (revision != ClientMapEditorData.revision()) localStatus = "";
        refreshEntries(); updateButtons();
        if (++ticks % 40 == 0) send(MapEditorAction.POLL, "");
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderShell(graphics, resultCount() + " 张地图 · " + (ClientMapEditorData.view().locked() ? "编辑锁定" : "工作区"));
        renderList(graphics, mouseX, mouseY, "暂无地图");
        renderStatus(graphics, localStatus.isBlank() ? ClientMapEditorData.view().message() : localStatus,
                ClientMapEditorData.view().error() ? UiTheme.ERROR : UiTheme.INFO);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
}
