package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientLobbyData;
import cn.blockforge.generated.generatedmod.client.SceneHudOverlay;
import cn.blockforge.generated.generatedmod.client.ui.*;
import cn.blockforge.generated.generatedmod.lobby.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class RoomScreen extends UiListScreen<String> {
    private final Screen parent;
    private UiButton start, map, leave;
    private int infoY;
    private int ticks;
    private boolean hadRoom;
    public RoomScreen(Screen parent) { super(Component.literal("我的房间")); this.parent = parent; }
    public static void open(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.getConnection() != null) { mc.setScreen(new RoomScreen(parent)); refresh(); }
    }
    private static void refresh() { LobbyScreen.request(RoomAction.REFRESH, "", "", 0, ""); }
    private RoomView room() { return SceneHudOverlay.ownRoom(); }
    @Override protected void init() {
        beginLayout(680, 0, BUTTON_HEIGHT);
        infoY = flowRow(28);
        int y = flowRow(BUTTON_HEIGHT);
        map = flowWidget(uiButton("比赛地图", columnX(0, 2, 6), y, columnWidth(2, 6),
                () -> RoomMapSelectScreen.open(this), null), y);
        flowWidget(uiButton("比赛规则", columnX(1, 2, 6), y, columnWidth(2, 6),
                () -> RoomRulesScreen.open(this), null), y);
        addSearch("搜索房间成员"); addList();
        footerButton("返回", 0, 3, 0, this::onClose, null, UiButton.Kind.SECONDARY);
        leave = footerButton("离开房间", 1, 3, 0, () -> confirmAction("离开房间", "确定离开当前房间？比赛中离开会退出本局。房主离开后将转移房主权限。",
                () -> LobbyScreen.request(RoomAction.LEAVE, "", "", 0, "")), null, UiButton.Kind.DANGER);
        start = footerButton("开始比赛", 2, 3, 0, () -> LobbyScreen.request(RoomAction.START, "", "", 0, ""), null, UiButton.Kind.PRIMARY);
        hadRoom = room() != null; updateButtons();
    }
    private String startReason() {
        RoomView room = room();
        if (room == null) return "当前不在房间中";
        if (room.state() != RoomState.OPEN) return switch (room.state()) {
            case COUNTDOWN -> "开赛倒计时"; case WAITING_MAP -> "等待地图载入"; default -> "比赛进行中";
        };
        if (!ClientLobbyData.ownOwner()) return "等待房主开始比赛";
        if (ClientLobbyData.matchActive()) return "等待服务器当前比赛结束";
        if (room.mapId() == null || room.mapId().isBlank() || "未选择".equals(room.mapId())) return "尚未选择比赛地图";
        int missing = room.rules().minPlayersToStart() - room.memberCount();
        return missing > 0 ? "还需 " + missing + " 名玩家" : "可以开始比赛";
    }
    private void updateButtons() {
        RoomView room = room();
        start.active = room != null && ClientLobbyData.ownOwner() && room.state() == RoomState.OPEN
                && !ClientLobbyData.matchActive() && room.mapId() != null && !room.mapId().isBlank()
                && !"未选择".equals(room.mapId()) && room.memberCount() >= room.rules().minPlayersToStart();
        start.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(startReason())));
        leave.active = room != null;
        map.active = room != null && ClientLobbyData.ownOwner() && room.state() == RoomState.OPEN
                && !room.matchmaking() && !ClientLobbyData.matchActive() && ClientLobbyData.config().allowOwnerMapSelection();
        map.setMessage(Component.literal("地图 · " + (room == null ? "未选择" : ClientLobbyData.mapDisplayName(room.mapId()))));
    }
    @Override protected List<String> entries() { RoomView room = room(); return room == null ? List.of() : room.members(); }
    @Override protected String entryId(String name) { return name; }
    @Override protected String entryTitle(String name) { return name; }
    @Override protected String entryDetail(String name) { return room() != null && name.equals(room().owner()) ? "房主" : "成员"; }
    @Override protected String entryBadge(String name) { return minecraft != null && minecraft.player != null && name.equals(minecraft.player.getGameProfile().getName()) ? "你" : "已加入"; }
    @Override protected int entryColor(String name) { return room() != null && name.equals(room().owner()) ? UiTheme.WARNING : UiTheme.SUCCESS; }
    @Override public void tick() {
        super.tick();
        if (hadRoom && room() == null) { onClose(); return; }
        hadRoom = room() != null; refreshEntries(); updateButtons();
        if (++ticks % 40 == 0) refresh();
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RoomView room = room();
        renderShell(graphics, room == null ? "无房间" : room.name() + " · " + room.memberCount() + "/" + (room.maxPlayers() <= 0 ? "不限" : room.maxPlayers()));
        paintBand(graphics, infoY, 28, () -> {
            graphics.drawString(font, fit(room == null ? "" : room.rules().describe(), innerWidth), innerLeft, infoY + 2, UiTheme.TEXT, false);
            graphics.drawString(font, fit(startReason(), innerWidth), innerLeft, infoY + 16, start.active ? UiTheme.SUCCESS : UiTheme.WARNING, false);
        });
        renderList(graphics, mouseX, mouseY, "暂无成员");
        renderStatus(graphics, ClientLobbyData.roomMessage(), ClientLobbyData.roomError() ? UiTheme.ERROR : UiTheme.INFO);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
}
