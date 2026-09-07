package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientLobbyData;
import cn.blockforge.generated.generatedmod.client.SceneHudOverlay;
import cn.blockforge.generated.generatedmod.client.ui.*;
import cn.blockforge.generated.generatedmod.lobby.RoomAction;
import cn.blockforge.generated.generatedmod.lobby.RoomState;
import cn.blockforge.generated.generatedmod.lobby.RoomView;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.RoomActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class RoomMapSelectScreen extends UiListScreen<ClientLobbyData.MapOption> {
    private final Screen parent;
    private UiButton select;
    private String pendingId = "";
    private int ticks;
    private int waitTicks;
    private int submittedRevision;
    public RoomMapSelectScreen(Screen parent) { super(Component.literal("选择比赛地图")); this.parent = parent; }
    public static void open(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.getConnection() != null) { mc.setScreen(new RoomMapSelectScreen(parent)); request(); }
    }
    @Override protected void init() {
        beginLayout(600, 0, BUTTON_HEIGHT);
        addSearch("搜索地图或作者"); addList();
        RoomView room = SceneHudOverlay.ownRoom();
        if (room != null) selectEntry(room.mapId());
        footerButton("返回", 0, 3, 0, this::onClose, null, UiButton.Kind.SECONDARY);
        footerButton("刷新", 1, 3, 0, RoomMapSelectScreen::request, null, UiButton.Kind.SECONDARY);
        select = footerButton("选用地图", 2, 3, 0, this::confirm, null, UiButton.Kind.PRIMARY);
        updateButton();
    }
    private static void request() { LobbyScreen.request(RoomAction.MAP_LIST, "", "", 0, ""); }
    private boolean editable() {
        RoomView room = SceneHudOverlay.ownRoom();
        return room != null && ClientLobbyData.ownOwner() && !room.matchmaking() && room.state() == RoomState.OPEN
                && !ClientLobbyData.matchActive() && ClientLobbyData.config().allowOwnerMapSelection();
    }
    private void confirm() {
        ClientLobbyData.MapOption map = selectedEntry();
        if (editable() && map != null && minecraft.getConnection() != null) {
            pendingId = map.id(); waitTicks = 0; submittedRevision = ClientLobbyData.roomRevision();
            FpsTdmNetwork.sendToServer(new RoomActionPacket(RoomAction.SET_MAP, ClientLobbyData.ownRoomId(), "", pendingId, 0, ""));
            updateButton();
        }
    }
    private void updateButton() { select.active = editable() && selectedEntry() != null && pendingId.isBlank(); select.setMessage(Component.literal(pendingId.isBlank() ? "选用地图" : "正在应用")); }
    @Override protected List<ClientLobbyData.MapOption> entries() { return ClientLobbyData.mapOptions(); }
    @Override protected String entryId(ClientLobbyData.MapOption map) { return map.id(); }
    @Override protected String entryTitle(ClientLobbyData.MapOption map) { return map.displayName(); }
    @Override protected String entryDetail(ClientLobbyData.MapOption map) { return (map.ownerName().isBlank() ? "服务器" : map.ownerName()) + " · " + map.id(); }
    @Override protected String entryBadge(ClientLobbyData.MapOption map) {
        RoomView room = SceneHudOverlay.ownRoom(); return room != null && map.id().equals(room.mapId()) ? "当前使用" : map.ownerName().isBlank() ? "服务器地图" : "玩家作品";
    }
    @Override protected int entryColor(ClientLobbyData.MapOption map) { return map.ownerName().isBlank() ? UiTheme.INFO : UiTheme.SUCCESS; }
    @Override public void tick() {
        super.tick(); refreshEntries();
        RoomView room = SceneHudOverlay.ownRoom();
        if (!pendingId.isBlank()) {
            if (room != null && pendingId.equals(room.mapId())) { onClose(); return; }
            if (++waitTicks >= 200 || (ClientLobbyData.roomRevision() != submittedRevision && ClientLobbyData.roomError())) pendingId = "";
        }
        updateButton(); if (++ticks % 40 == 0) request();
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderShell(graphics, resultCount() + " 张可选地图"); renderList(graphics, mouseX, mouseY, "服务器尚未注册地图");
        renderStatus(graphics, ClientLobbyData.roomMessage(), ClientLobbyData.roomError() ? UiTheme.ERROR : UiTheme.INFO);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
}
