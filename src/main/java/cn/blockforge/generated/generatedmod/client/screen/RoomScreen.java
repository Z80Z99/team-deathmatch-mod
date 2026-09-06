package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientLobbyData;
import cn.blockforge.generated.generatedmod.client.ui.UiButton;
import cn.blockforge.generated.generatedmod.client.ui.UiScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
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

/**
 * 我的房间：管理当前所在的房间。房间信息、规则摘要与成员一览使用流式横带，
 * 地图通过内容区的地图行从列表选择（所有玩家制作的地图都在列表中）；
 * 没有单独的解散按钮——房主离开时房主随机转给其他成员，最后一位成员离开房间自动解散。
 * 离开房间或被解散后自动退回上一级界面。
 */
public final class RoomScreen extends UiScreen {
    private static final int INFO_HEIGHT = 40;
    private static final int MEMBERS_HEIGHT = 38;
    private static final int FOOTER_BUTTON_COUNT = 5;

    private final Screen parent;
    private UiButton startButton;
    private UiButton rulesButton;
    private UiButton leaveButton;
    private UiButton mapRowButton;
    private int infoY;
    private int membersLabelY;
    private int membersY;
    private int mapLabelY;
    private int mapRowY;
    private int footerColumns;
    private boolean hadRoom;
    private int ticks;
    private int lastRoomRevision = -1;
    private String statusText = "";
    private int statusColor = UiTheme.MUTED;

    public RoomScreen(Screen parent) {
        super(Component.literal("我的房间"));
        this.parent = parent;
    }

    public static void open(Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.getConnection() != null) {
            RoomScreen screen = new RoomScreen(parent);
            minecraft.setScreen(screen);
            screen.refresh();
        }
    }

    @Override
    protected void init() {
        int preferredInner = Math.max(1, Math.min(640, width - 16) - CONTENT_PADDING * 2);
        // 底部按钮列数最多 4：保证统一 150×22 的标准尺寸在所有界面一致。
        footerColumns = preferredInner >= 620 ? 4 : 3;
        int footerRows = (int) Math.ceil(FOOTER_BUTTON_COUNT / (double) footerColumns);
        int footerHeight = footerRows * BUTTON_HEIGHT + (footerRows - 1) * 4;
        beginLayout(640, 0, footerHeight);

        infoY = flowRow(INFO_HEIGHT);
        membersLabelY = flowRow(13);
        membersY = flowRow(MEMBERS_HEIGHT);
        mapLabelY = flowRow(10);
        mapRowY = flowRow(BUTTON_HEIGHT);

        RoomView own = ownRoom();
        mapRowButton = flowWidget(uiButton(mapRowLabel(own), innerLeft, mapRowY, innerWidth,
                this::openMapPicker,
                "从地图列表中选择本房间用图：所有玩家制作的地图都会出现在列表里。",
                UiButton.Kind.SECONDARY), mapRowY);

        record FooterDef(String label, Runnable action, String tooltip, UiButton.Kind kind) {
        }
        List<FooterDef> defs = List.of(
                new FooterDef("开始比赛", () -> send(RoomAction.START),
                        "满足人数条件后房主立即开赛。", UiButton.Kind.PRIMARY),
                new FooterDef("规则设置", () -> RoomRulesScreen.open(this),
                        "打开本房间的比赛规则；模式与各设置项只有房主可以修改。", UiButton.Kind.SECONDARY),
                new FooterDef("离开房间", () -> send(RoomAction.LEAVE),
                        "离开当前房间；比赛中离开会退出本局。房主离开时房主随机转给一位成员，最后一位成员离开则房间自动解散。",
                        UiButton.Kind.SECONDARY),
                new FooterDef("刷新", this::refresh,
                        "重新读取服务器房间状态与地图列表。", UiButton.Kind.SECONDARY),
                new FooterDef("返回", this::onClose,
                        "返回上一级界面。", UiButton.Kind.DANGER));
        if (defs.size() != FOOTER_BUTTON_COUNT) {
            throw new IllegalStateException("底部按钮数量与预留网格不一致");
        }
        UiButton[] created = new UiButton[defs.size()];
        for (int index = 0; index < defs.size(); index++) {
            FooterDef def = defs.get(index);
            created[index] = footerButton(def.label(), index % footerColumns, footerColumns,
                    index / footerColumns, def.action(), def.tooltip(), def.kind());
        }
        startButton = created[0];
        rulesButton = created[1];
        leaveButton = created[2];

        hadRoom = ownRoom() != null;
        lastRoomRevision = ClientLobbyData.roomRevision();
        updateButtons();
    }

    private String mapRowLabel(RoomView own) {
        String mapId = own == null ? "" : own.mapId();
        return "地图：" + (mapId == null || mapId.isBlank() || "未选择".equals(mapId)
                ? "未选择（点击选择）" : ClientLobbyData.mapDisplayName(mapId));
    }

    /** 打开地图列表：先向服务器请求全量列表（含玩家制作的地图）。 */
    private void openMapPicker() {
        if (minecraft != null && minecraft.getConnection() != null) {
            FpsTdmNetwork.sendToServer(new RoomActionPacket(RoomAction.MAP_LIST, "", "", "", 0, ""));
        }
        RoomMapSelectScreen.open(this);
    }

    private void send(RoomAction action) {
        send(action, "");
    }

    private void send(RoomAction action, String mapId) {
        if (minecraft != null && minecraft.getConnection() != null) {
            FpsTdmNetwork.sendToServer(new RoomActionPacket(action, "", "", mapId, 0, ""));
        }
    }

    private void refresh() {
        send(RoomAction.REFRESH);
        if (minecraft != null && minecraft.getConnection() != null) {
            FpsTdmNetwork.sendToServer(new RoomActionPacket(RoomAction.MAP_LIST, "", "", "", 0, ""));
        }
    }

    private RoomView ownRoom() {
        String ownId = ClientLobbyData.ownRoomId();
        if (ownId.isBlank()) {
            return null;
        }
        for (RoomView room : ClientLobbyData.rooms()) {
            if (room.id().equals(ownId)) {
                return room;
            }
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        if (ClientLobbyData.roomRevision() != lastRoomRevision
                || ClientLobbyData.mapOptionsRevision() % 8 == 0 && ticks % 80 == 0) {
            lastRoomRevision = ClientLobbyData.roomRevision();
            RoomView own = ownRoom();
            if (hadRoom && own == null) {
                // 已离开 / 被解散 / 匹配房赛终自动解散：退回上一级界面。
                if (!ClientLobbyData.roomMessage().isBlank()) {
                    setStatus(ClientLobbyData.roomMessage(),
                            ClientLobbyData.roomError() ? UiTheme.ERROR : UiTheme.SUCCESS);
                }
                onCloseWithStatus();
                return;
            }
            hadRoom = own != null;
            if (mapRowButton != null && own != null) {
                mapRowButton.setMessage(Component.literal(mapRowLabel(own)));
            }
        }
        updateButtons();
        if (ticks % 40 == 0) refresh();
    }

    private void onCloseWithStatus() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void setStatus(String message, int color) {
        statusText = message == null ? "" : message;
        statusColor = color;
    }

    private void updateButtons() {
        RoomView own = ownRoom();
        boolean ownOpen = own != null && own.state() == RoomState.OPEN;
        boolean mapSelected = own != null && own.mapId() != null && !own.mapId().isBlank()
                && !"未选择".equals(own.mapId());
        boolean enoughPlayers = own != null
                && own.memberCount() >= own.rules().minPlayersToStart();
        boolean activeMatch = ClientLobbyData.matchActive();
        // 匹配房正常由倒计时自动开赛；成局失败退回开放态时允许房主手动重试。
        if (startButton != null) startButton.active = own != null && ClientLobbyData.ownOwner()
                && ownOpen && !activeMatch && mapSelected && enoughPlayers;
        if (rulesButton != null) {
            rulesButton.active = own != null;
            rulesButton.setMessage(Component.literal(
                    own != null && ClientLobbyData.ownOwner() && !own.matchmaking()
                            ? "规则设置" : "查看规则"));
        }
        boolean canSetMap = own != null && ClientLobbyData.ownOwner() && ownOpen && !activeMatch
                && !own.matchmaking() && ClientLobbyData.config().allowOwnerMapSelection();
        if (mapRowButton != null) mapRowButton.active = canSetMap;
        if (leaveButton != null) leaveButton.active = own != null;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RoomView own = ownRoom();
        renderShell(graphics, own == null ? "当前不在房间中" : roomSubtitle(own));
        final int infoBand = infoY;
        paintBand(graphics, infoBand, INFO_HEIGHT, () -> renderRoomInfo(graphics, own, infoBand));
        final int membersLabelBand = membersLabelY;
        paintBand(graphics, membersLabelBand, 13,
                () -> section(graphics, "成员", innerLeft, membersLabelBand, innerWidth));
        final int membersBand = membersY;
        paintBand(graphics, membersBand, MEMBERS_HEIGHT,
                () -> renderMembers(graphics, own, membersBand));
        final int mapLabelBand = mapLabelY;
        paintBand(graphics, mapLabelBand, 10,
                () -> graphics.drawString(font, fit("比赛地图（列表选择，房主可改）", innerWidth), innerLeft,
                        mapLabelBand + 2, UiTheme.MUTED, false));

        String message = statusText.isBlank() ? ClientLobbyData.roomMessage() : statusText;
        int messageColor = statusText.isBlank()
                ? (ClientLobbyData.roomError() ? UiTheme.ERROR : UiTheme.INFO) : statusColor;
        if (own == null && message.isBlank()) {
            message = "你当前不在房间中，请先在“加入房间”里加入或创建。";
            messageColor = UiTheme.WARNING;
        }
        renderStatus(graphics, message, messageColor);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String roomSubtitle(RoomView room) {
        String state = switch (room.state()) {
            case OPEN -> "开放中";
            case COUNTDOWN -> "开赛倒计时";
            case WAITING_MAP -> "等待地图快照";
            case RUNNING -> "比赛进行中";
        };
        return (room.matchmaking() ? "匹配比赛房" : room.name()) + "  ·  " + state
                + "  ·  人数 " + room.memberCount()
                + (room.maxPlayers() <= 0 ? "/不限" : "/" + room.maxPlayers())
                + (room.locked() ? "  ·  私有" : "");
    }

    private void renderRoomInfo(GuiGraphics graphics, RoomView own, int top) {
        UiTheme.card(graphics, innerLeft, top, innerWidth, INFO_HEIGHT, UiTheme.PANEL);
        if (own == null) {
            graphics.drawString(font, fit("加入或创建一个房间后再回到这里管理。", innerWidth - 16),
                    innerLeft + 8, top + 17, UiTheme.MUTED, false);
            return;
        }
        graphics.drawString(font, fit("房间  " + own.name() + "  ·  " + own.id()
                + (own.locked() ? "  ·  需密码" : "")
                + "  ·  房主 " + own.owner()
                + (ClientLobbyData.ownOwner() ? "（你）" : "")
                + (own.matchmaking() ? "  ·  匹配房（服务器规则，自动解散）" : ""), innerWidth - 16),
                innerLeft + 8, top + 4, UiTheme.TEXT, false);
        graphics.drawString(font, fit("地图  " + ClientLobbyData.mapDisplayName(own.mapId())
                + "  ·  人数 " + own.memberCount()
                + (own.maxPlayers() <= 0 ? "/不限" : "/" + own.maxPlayers())
                + "  ·  最少开赛 " + own.rules().minPlayersToStart() + " 人",
                innerWidth - 16), innerLeft + 8, top + 16, UiTheme.MUTED, false);
        graphics.drawString(font, fit("规则  " + own.rules().describe()
                + (own.matchmaking() ? "" : "（仅房主可改）"), innerWidth - 16),
                innerLeft + 8, top + 28, UiTheme.ACCENT, false);
    }

    private void renderMembers(GuiGraphics graphics, RoomView own, int top) {
        UiTheme.card(graphics, innerLeft, top, innerWidth, MEMBERS_HEIGHT, UiTheme.PANEL_RAISED);
        if (own == null || own.members().isEmpty()) {
            graphics.drawString(font, fit("暂无成员。", innerWidth - 16), innerLeft + 8, top + 16,
                    UiTheme.MUTED, false);
            return;
        }
        renderWrappedNames(graphics, own.members(), top);
    }

    /** 成员名单按单元格宽度折行，最多两行，超出以“…… 共 N 人”收尾。 */
    private void renderWrappedNames(GuiGraphics graphics, List<String> members, int top) {
        int maxWidth = innerWidth - 16;
        int x = innerLeft + 8;
        int y = top + 6;
        int line = 0;
        int shown = 0;
        for (int index = 0; index < members.size(); index++) {
            String name = index + 1 < members.size() ? members.get(index) + "、" : members.get(index);
            int width = font.width(name);
            if (x + width > innerLeft + 8 + maxWidth) {
                if (line == 1) {
                    graphics.drawString(font, fit("…… 共 " + members.size() + " 人", maxWidth),
                            x, y, UiTheme.MUTED, false);
                    return;
                }
                line++;
                x = innerLeft + 8;
                y += 14;
            }
            graphics.drawString(font, name, x, y, UiTheme.TEXT, false);
            x += width;
            shown++;
        }
        if (shown < members.size()) {
            graphics.drawString(font, "……", x, y, UiTheme.MUTED, false);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
