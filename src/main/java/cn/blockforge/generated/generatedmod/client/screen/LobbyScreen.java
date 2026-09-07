package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientLobbyData;
import cn.blockforge.generated.generatedmod.client.ui.*;
import cn.blockforge.generated.generatedmod.lobby.*;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.RoomActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class LobbyScreen extends UiListScreen<RoomView> {
    private final Screen parent;
    private UiEditBox password;
    private UiButton join;
    private UiButton create;
    private UiButton own;
    private String passwordDraft = "";
    private String lastRoom = "";
    private int ticks;
    public LobbyScreen(Screen parent) { super(Component.literal("房间大厅")); this.parent = parent; }
    public static void open(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.getConnection() != null) {
            mc.setScreen(new LobbyScreen(parent)); request(RoomAction.REFRESH, "", "", 0, "");
        }
    }
    @Override protected void init() {
        beginLayout(680, 0, BUTTON_HEIGHT);
        addSearch("搜索房间、模式或房主");
        int y = flowRow(BUTTON_HEIGHT);
        int gap = 6;
        password = flowWidget(new UiEditBox(font, columnX(0, 3, gap), y, columnWidth(3, gap), BUTTON_HEIGHT, Component.literal("房间密码")), y);
        password.setMaxLength(16);
        password.setValue(passwordDraft);
        password.setResponder(value -> passwordDraft = value);
        password.setFormatter((value, start) -> net.minecraft.util.FormattedCharSequence.forward("*".repeat(value.length()), net.minecraft.network.chat.Style.EMPTY));
        create = flowWidget(uiButton("创建房间", columnX(1, 3, gap), y, columnWidth(3, gap),
                () -> minecraft.setScreen(new RoomCreateScreen(this)), "创建自定义房间", UiButton.Kind.SECONDARY), y);
        own = flowWidget(uiButton("我的房间", columnX(2, 3, gap), y, columnWidth(3, gap),
                () -> RoomScreen.open(this), "打开当前房间", UiButton.Kind.SECONDARY), y);
        addList();
        footerButton("返回", 0, 3, 0, this::onClose, null, UiButton.Kind.SECONDARY);
        footerButton("刷新", 1, 3, 0, () -> request(RoomAction.REFRESH, "", "", 0, ""), null, UiButton.Kind.SECONDARY);
        join = footerButton("加入房间", 2, 3, 0, this::joinSelected, null, UiButton.Kind.PRIMARY);
        lastRoom = ClientLobbyData.ownRoomId(); updateButtons();
    }
    static void request(RoomAction action, String id, String name, int capacity, String password) {
        if (Minecraft.getInstance().getConnection() != null) FpsTdmNetwork.sendToServer(new RoomActionPacket(action, id, name, "", capacity, password));
    }
    private boolean joinable(RoomView room) {
        return room != null && room.state() == RoomState.OPEN && (room.maxPlayers() <= 0 || room.memberCount() < room.maxPlayers());
    }
    private void joinSelected() { RoomView room = selectedEntry(); if (joinable(room)) request(RoomAction.JOIN, room.id(), "", 0, passwordDraft.trim()); }
    private void updateButtons() {
        boolean available = ClientLobbyData.ownRoomId().isBlank() && !ClientLobbyData.matchmaking().queued();
        create.active = available; own.active = !ClientLobbyData.ownRoomId().isBlank(); join.active = available && joinable(selectedEntry());
    }
    @Override protected List<RoomView> entries() { return ClientLobbyData.rooms(); }
    @Override protected String entryId(RoomView room) { return room.id(); }
    @Override protected String entryTitle(RoomView room) { return room.name(); }
    @Override protected String entryDetail(RoomView room) { return room.rules().mode().displayName() + " · " + room.owner() + " · " + ClientLobbyData.mapDisplayName(room.mapId()); }
    @Override protected String entryBadge(RoomView room) {
        return (room.locked() ? "私密 " : "") + room.memberCount() + "/" + (room.maxPlayers() <= 0 ? "不限" : room.maxPlayers())
                + " " + switch (room.state()) { case OPEN -> "开放"; case COUNTDOWN -> "准备"; case WAITING_MAP -> "载入"; case RUNNING -> "比赛中"; };
    }
    @Override protected int entryColor(RoomView room) { return joinable(room) ? UiTheme.SUCCESS : UiTheme.WARNING; }
    @Override public void tick() {
        super.tick(); password.tick(); refreshEntries(); updateButtons();
        String id = ClientLobbyData.ownRoomId();
        if (!id.isBlank() && !id.equals(lastRoom)) { lastRoom = id; RoomScreen.open(this); return; }
        lastRoom = id;
        if (++ticks % 40 == 0) request(RoomAction.REFRESH, "", "", 0, "");
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderShell(graphics, resultCount() + " 个房间 · " + (ClientLobbyData.matchmaking().queued() ? "匹配排队中" : "在线"));
        renderList(graphics, mouseX, mouseY, "暂无房间");
        renderStatus(graphics, ClientLobbyData.roomMessage(), ClientLobbyData.roomError() ? UiTheme.ERROR : UiTheme.INFO);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
}
