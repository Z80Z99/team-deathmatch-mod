package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientLobbyData;
import cn.blockforge.generated.generatedmod.client.SceneHudOverlay;
import cn.blockforge.generated.generatedmod.client.ui.*;
import cn.blockforge.generated.generatedmod.lobby.*;
import cn.blockforge.generated.generatedmod.match.Team;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Tooltip;
import java.util.List;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Locale;

public final class RoomScreen extends UiScreen {
    private final Screen parent;
    private UiButton start, map, leave, previous, next;
    private UiCycleButton<Integer> teamCount;
    private UiEditBox search;
    private final List<UiButton> joinButtons = new ArrayList<>();
    private final EnumMap<Team, Integer> offsets = new EnumMap<>(Team.class);
    private int infoY, listY, listHeight, columns, page, ticks;
    private boolean hadRoom;
    private String query = "";
    private static final int MEMBER_HEIGHT = 25;
    public RoomScreen(Screen parent) { super(Component.literal("我的房间")); this.parent = parent; }
    public static void open(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.getConnection() != null) { mc.setScreen(new RoomScreen(parent)); refresh(); }
    }
    private static void refresh() { LobbyScreen.request(RoomAction.REFRESH, "", "", 0, ""); }
    private RoomView room() { return SceneHudOverlay.ownRoom(); }
    private List<Team> teams() { return Team.playing(room() == null ? 2 : room().teamCount()); }
    private List<Team> displayedTeams() {
        var result = new ArrayList<>(teams());
        if (room() != null && room().memberTeams().containsValue(Team.SPECTATOR)) result.add(Team.SPECTATOR);
        return result;
    }
    private List<Team> visibleTeams() {
        var displayed = displayedTeams();
        page = clamp(page, 0, (displayed.size() - 1) / columns);
        return displayed.subList(page * columns, Math.min(displayed.size(), (page + 1) * columns));
    }
    private List<String> members(Team team) {
        RoomView room = room();
        return room == null ? List.of() : room.members().stream()
                .filter(name -> room.memberTeams().get(name) == team)
                .filter(name -> name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))).toList();
    }
    @Override protected void init() {
        joinButtons.clear();
        beginLayout(860, 0, BUTTON_HEIGHT);
        columns = Math.min(4, Math.max(2, innerWidth / 145));
        infoY = flowRow(26);
        int y = flowRow(BUTTON_HEIGHT);
        map = flowWidget(uiButton("比赛地图", columnX(0, 2, 6), y, columnWidth(2, 6),
                () -> RoomMapSelectScreen.open(this), null), y);
        flowWidget(uiButton("比赛规则", columnX(1, 2, 6), y, columnWidth(2, 6),
                () -> RoomRulesScreen.open(this), null), y);
        y = flowRow(BUTTON_HEIGHT);
        int selectorWidth = Math.min(110, innerWidth / 3);
        teamCount = flowWidget(new UiCycleButton<>(innerLeft, y, selectorWidth, BUTTON_HEIGHT,
                List.of(2, 3, 4), room() == null ? 2 : room().teamCount(), value -> value + " 支队伍",
                value -> { if (room() != null) LobbyScreen.request(RoomAction.SET_TEAM_COUNT, room().id(), "", value, ""); },
                "仅房主可修改；减少队伍会将被移除队伍的成员分配到人数较少的队伍。", UiButton.Kind.SECONDARY), y);
        search = flowWidget(new UiEditBox(font, innerLeft + selectorWidth + 6, y,
                innerWidth - selectorWidth - 58, BUTTON_HEIGHT, Component.literal("搜索成员")), y);
        search.setValue(query);
        search.setResponder(value -> { query = value; offsets.clear(); });
        previous = flowWidget(uiButton("<", innerLeft + innerWidth - 46, y, 22,
                () -> { page--; updateButtons(); }, "上一组队伍"), y);
        next = flowWidget(uiButton(">", innerLeft + innerWidth - 22, y, 22,
                () -> { page++; updateButtons(); }, "下一组队伍"), y);
        y = flowRow(BUTTON_HEIGHT);
        for (int column = 0; column < columns; column++) {
            final int index = column;
            joinButtons.add(flowWidget(uiButton("", columnX(column, columns, 8), y, columnWidth(columns, 8), () -> {
                var shown = visibleTeams();
                if (room() != null && index < shown.size())
                    LobbyScreen.request(RoomAction.CHANGE_TEAM, room().id(), shown.get(index).key(), 0, "");
            }, "加入该队；开赛倒计时和比赛中不能从房间换队。"), y, () -> index < visibleTeams().size()));
        }
        listHeight = Math.max(MEMBER_HEIGHT, (contentBottom - flowCursor()) / MEMBER_HEIGHT * MEMBER_HEIGHT);
        listY = flowRow(listHeight);
        footerButton("返回", 0, 3, 0, this::onClose, null, UiButton.Kind.SECONDARY);
        leave = footerButton("离开房间", 1, 3, 0, () -> confirmAction("离开房间", "确定离开当前房间？房主离开后会转移房主权限。",
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
        return "可以开始比赛";
    }
    private void updateButtons() {
        RoomView room = room();
        var shown = visibleTeams();
        boolean open = room != null && room.state() == RoomState.OPEN && !room.matchmaking();
        start.active = "可以开始比赛".equals(startReason());
        start.setTooltip(Tooltip.create(Component.literal(startReason())));
        leave.active = room != null;
        teamCount.active = open && ClientLobbyData.ownOwner();
        if (room != null) teamCount.setValue(room.teamCount());
        map.active = open && ClientLobbyData.ownOwner() && !ClientLobbyData.matchActive()
                && ClientLobbyData.config().allowOwnerMapSelection();
        map.setMessage(Component.literal("地图 · " + (room == null ? "未选择" : ClientLobbyData.mapDisplayName(room.mapId()))));
        String me = minecraft != null && minecraft.player != null ? minecraft.player.getGameProfile().getName() : "";
        for (int i = 0; i < joinButtons.size(); i++) {
            UiButton button = joinButtons.get(i);
            if (i >= shown.size()) { button.active = false; continue; }
            Team team = shown.get(i);
            boolean own = room != null && room.memberTeams().get(me) == team;
            long count = room == null ? 0 : room.memberTeams().values().stream().filter(value -> value == team).count();
            button.setMessage(Component.literal(team.displayName() + " · " + count + (own ? " · 当前" : team.isPlayable() ? " · 加入" : "")));
            button.setSelected(own); button.active = open && !own && team.isPlayable();
        }
        previous.active = page > 0;
        next.active = (page + 1) * columns < displayedTeams().size();
        refreshFlowVisibility();
    }
    @Override public void tick() {
        super.tick(); search.tick();
        if (hadRoom && room() == null) { onClose(); return; }
        hadRoom = room() != null; updateButtons();
        if (++ticks % 40 == 0) refresh();
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RoomView room = room();
        renderShell(graphics, room == null ? "无房间" : room.name() + " · " + room.memberCount() + "/"
                + (room.maxPlayers() <= 0 ? "不限" : room.maxPlayers()) + " · " + room.teamCount() + "队");
        paintBand(graphics, infoY, 26, () -> {
            graphics.drawString(font, fit(room == null ? "" : room.rules().describe(room.teamCount()), innerWidth), innerLeft, infoY + 2, UiTheme.TEXT, false);
            graphics.drawString(font, fit(startReason(), innerWidth), innerLeft, infoY + 16, start.active ? UiTheme.SUCCESS : UiTheme.WARNING, false);
        });
        var shown = visibleTeams();
        for (int col = 0; col < shown.size(); col++) {
            Team team = shown.get(col);
            int x = columnX(col, columns, 8), w = columnWidth(columns, 8);
            var names = members(team);
            int rows = listHeight / MEMBER_HEIGHT;
            int offset = clamp(offsets.getOrDefault(team, 0), 0, Math.max(0, names.size() - rows));
            offsets.put(team, offset);
            for (int row = 0; row < rows; row++) {
                int y = listY + row * MEMBER_HEIGHT, index = offset + row;
                String name = index < names.size() ? names.get(index) : "";
                paintBand(graphics, y, MEMBER_HEIGHT, () -> {
                    graphics.fill(x, y, x + 2, y + MEMBER_HEIGHT - 2, team.hudColor());
                    if (!name.isEmpty()) {
                        graphics.drawString(font, fit(name, w - 14), x + 7, y + 2, UiTheme.TEXT, false);
                        String me = minecraft != null && minecraft.player != null ? minecraft.player.getGameProfile().getName() : "";
                        String role = room != null && name.equals(room.owner()) ? "房主" : "成员";
                        graphics.drawString(font, fit(role + (name.equals(me) ? " · 你" : ""), w - 14), x + 7, y + 13, UiTheme.MUTED, false);
                    }
                    divider(graphics, x + 6, y + MEMBER_HEIGHT - 1, w - 6);
                });
            }
            if (names.size() > rows) paintBand(graphics, listY, listHeight, () -> {
                int thumb = Math.max(6, listHeight * rows / names.size());
                int top = listY + (listHeight - thumb) * offset / (names.size() - rows);
                graphics.fill(x + w - 2, top, x + w, top + thumb, team.hudColor());
            });
        }
        renderStatus(graphics, ClientLobbyData.roomMessage(), ClientLobbyData.roomError() ? UiTheme.ERROR : UiTheme.INFO);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public boolean mouseScrolled(double x, double y, double amount) {
        if (choicePopup().isOpen()) return super.mouseScrolled(x, y, amount);
        if (bandFits(listY, listHeight) && y >= bandScreenY(listY) && y < bandScreenY(listY) + listHeight) {
            var shown = visibleTeams();
            for (int col = 0; col < shown.size(); col++) {
                if (x >= columnX(col, columns, 8) && x < columnX(col, columns, 8) + columnWidth(columns, 8)) {
                    Team team = shown.get(col);
                    offsets.put(team, clamp(offsets.getOrDefault(team, 0) - (int) Math.round(amount), 0,
                            Math.max(0, members(team).size() - listHeight / MEMBER_HEIGHT)));
                    return true;
                }
            }
        }
        return super.mouseScrolled(x, y, amount);
    }
    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
