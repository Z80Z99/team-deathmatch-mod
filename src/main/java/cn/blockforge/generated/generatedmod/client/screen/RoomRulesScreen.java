package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientConfigData;
import cn.blockforge.generated.generatedmod.client.ClientLobbyData;
import cn.blockforge.generated.generatedmod.client.ui.UiButton;
import cn.blockforge.generated.generatedmod.client.ui.UiCycleButton;
import cn.blockforge.generated.generatedmod.client.ui.UiEditBox;
import cn.blockforge.generated.generatedmod.client.ui.UiScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import cn.blockforge.generated.generatedmod.lobby.RoomRules;
import cn.blockforge.generated.generatedmod.lobby.RoomState;
import cn.blockforge.generated.generatedmod.lobby.RoomView;
import cn.blockforge.generated.generatedmod.match.AutoBalanceMode;
import cn.blockforge.generated.generatedmod.match.GameMode;
import cn.blockforge.generated.generatedmod.match.SpawnSelectionStrategy;
import cn.blockforge.generated.generatedmod.match.TeamChangePolicy;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.ConfigRequestPacket;
import cn.blockforge.generated.generatedmod.network.packet.RoomRulesPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 房间规则设置：原“团队死斗配置”与“房间参数”的比赛项已合并到这里，
 * 每间房间独立一份、随房间解散销毁。顶部先选游戏模式，
 * 表单只展示当前模式适用的设置项（不适用的行整行隐藏）；
 * 只有房主可以修改，其他成员打开时全表单只读。
 */
public final class RoomRulesScreen extends UiScreen {
    private static final int FIELD_HEIGHT = 24;
    private static final int ROW_COUNT = 10;
    private static final int COLUMN_GAP = 12;

    private final Screen parent;
    private RoomRules draft;
    private UiCycleButton<GameMode> modeCycle;
    private UiEditBox minPlayersBox;
    private UiEditBox targetBox;
    private UiEditBox durationBox;
    private UiEditBox winBox;
    private UiEditBox warmupBox;
    private UiEditBox respawnBox;
    private UiCycleButton<Boolean> autoRespawnToggle;
    private UiEditBox switchBox;
    private UiCycleButton<Boolean> friendlyFireToggle;
    private UiEditBox roundEndDelayBox;
    private UiEditBox matchEndDelayBox;
    private UiCycleButton<Boolean> keepInventoryToggle;
    private UiCycleButton<Boolean> suppressDeathToggle;
    private UiCycleButton<Boolean> autoResetToggle;
    private UiCycleButton<Boolean> requireBothToggle;
    private UiCycleButton<TeamChangePolicy> teamChangeCycle;
    private UiCycleButton<AutoBalanceMode> balanceCycle;
    private UiCycleButton<SpawnSelectionStrategy> spawnCycle;
    private UiEditBox imbalanceBox;
    private UiButton saveButton;
    private UiButton resetButton;
    private int sectionY;
    private int summaryY;
    private int noticeY;
    private int rulesViewportTop;
    private int rulesViewportBottom;
    private int rulesScroll;
    private int rulesMaxScroll;
    private final int[] rowYs = new int[ROW_COUNT];
    private final int[] ruleBaseYs = new int[ROW_COUNT];
    private int marqueeTicks;
    private int lastRoomRevision = -1;
    private String status = "";
    private int statusColor = UiTheme.MUTED;

    private RoomRulesScreen(Screen parent) {
        super(Component.literal("房间规则设置"));
        this.parent = parent;
        this.draft = editingRules();
    }

    public static void open(Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.getConnection() != null) {
            RoomRulesScreen screen = new RoomRulesScreen(parent);
            minecraft.setScreen(screen);
            // 拉取服务器配置副本用于“恢复默认”。
            FpsTdmNetwork.sendToServer(new ConfigRequestPacket());
        }
    }

    private boolean editable() {
        RoomView own = ownRoom();
        return own != null && ClientLobbyData.ownOwner() && !own.matchmaking()
                && own.state() != RoomState.RUNNING;
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

    private RoomRules editingRules() {
        RoomView own = ownRoom();
        if (own != null && own.rules() != null) {
            return own.rules();
        }
        if (ClientConfigData.received()) {
            return RoomRules.fromConfigValues(ClientConfigData.values());
        }
        return RoomRules.fallback();
    }

    @Override
    protected void init() {
        beginLayout(640, 0, BUTTON_HEIGHT);
        sectionY = flowRow(13);
        summaryY = flowRow(18);
        if (draft == null) {
            draft = editingRules();
        }
        RoomRules current = draft.normalized();
        rulesViewportTop = summaryY + 18 + ROW_GAP;
        rulesViewportBottom = contentBottom - 18;
        layoutRuleRows(current.mode());
        int gap = COLUMN_GAP;
        boolean editable = editable();

        modeCycle = cycle(0, 0, "游戏模式", List.of(GameMode.values()), current.mode(),
                GameMode::displayName, value -> {
                    draft = RoomRules.templateFor(value, collect()).normalized();
                    rebuildWidgets();
                    setStatus("已切换为「" + value.displayName() + "」，设置项已按模式重置。", UiTheme.INFO);
                }, "切换后本模式的设置项会重置为推荐模板，共享项保持不变。", editable);
        minPlayersBox = integer(0, 1, "最少开赛人数", current.minPlayersToStart(), 2, editable,
                "房间达到该人数才允许开赛（1～32，每间房独立）。");

        targetBox = integer(1, 0, "击杀目标（0=不限）", current.targetKills(), 4, editable,
                "团队死斗：单回合率先累计到的击杀数。");
        durationBox = integer(1, 1, durationLabel(current.mode()), current.matchDurationSeconds(), 5,
                editable, durationTooltip(current.mode()));
        winBox = integer(2, 0, winLabel(current.mode()), current.roundWinTarget(), 2, editable,
                "需要拿下的回合数才算赢下整场比赛。");
        warmupBox = integer(2, 1, "热身时长/秒", current.warmupDurationSeconds(), 4, editable,
                "开赛前双方自由活动的热身时间（0～300）。");

        respawnBox = integer(3, 0, "阵亡恢复/秒", current.respawnDelaySeconds(), 2, editable,
                "团队死斗：阵亡后恢复作战的等待时间。");
        autoRespawnToggle = bool(3, 1, "允许自动复活", current.autoRespawn(), editable);
        switchBox = integer(4, 0, "换边间隔/回合", current.switchSideEvery(), 2, editable,
                "爆破模式：每 N 个回合双方换一次出生区（1～10）。");
        friendlyFireToggle = bool(4, 1, "友军伤害", current.friendlyFire(), editable);

        roundEndDelayBox = integer(5, 0, "回合结算间隔/秒", current.roundEndDelaySeconds(), 3, editable,
                "回合结束到下一回合开始的间隔。");
        matchEndDelayBox = integer(5, 1, "结算画面/秒", current.matchEndDelaySeconds(), 3, editable,
                "整场比赛结束后结算画面的停留时间。");
        keepInventoryToggle = bool(6, 0, "死亡保留背包", current.keepInventoryOnDeath(), editable);
        suppressDeathToggle = bool(6, 1, "隐藏死亡消息", current.suppressDeathMessages(), editable);
        autoResetToggle = bool(7, 0, "赛后自动复位地图", current.autoReset(), editable);
        requireBothToggle = bool(7, 1, "要求两队人数达标", current.requireBothTeams(), editable);

        teamChangeCycle = cycle(8, 0, "换队政策", List.of(TeamChangePolicy.values()),
                current.teamChangePolicy(), RoomRulesScreen::teamChangeName, null, null, editable);
        balanceCycle = cycle(8, 1, "自动平衡", List.of(AutoBalanceMode.values()),
                current.autoBalanceMode(), RoomRulesScreen::balanceName, null, null, editable);
        spawnCycle = cycle(9, 0, "出生点策略", List.of(SpawnSelectionStrategy.values()),
                current.spawnSelectionStrategy(), RoomRulesScreen::spawnName, null, null, editable);
        imbalanceBox = integer(9, 1, "两队最大人数差", current.maxTeamImbalance(), 1, editable,
                "自动平衡容忍的人数差（0=严格均分）。");

        applyModeVisibility();
        applyRuleScroll();

        saveButton = footerButton("保存并广播", 0, 4, 0, this::save,
                "把规则整体下发给房间，全员实时可见；开赛时生效。", UiButton.Kind.PRIMARY);
        resetButton = footerButton("恢复默认", 1, 4, 0, this::resetToServerDefaults,
                "用服务器默认配置重置本房间规则（保持当前模式）。", UiButton.Kind.SECONDARY);
        footerButton("刷新", 2, 4, 0, this::refresh, "重新读取房间状态。", UiButton.Kind.SECONDARY);
        footerButton("返回", 3, 4, 0, this::onClose, "返回我的房间。", UiButton.Kind.DANGER);

        if (!editable) {
            setStatus(ownRoom() == null ? "你当前不在房间中。"
                    : ownRoom().matchmaking() ? "匹配房使用服务器规则，只读。"
                    : ownRoom().state() == RoomState.RUNNING ? "比赛进行中，规则只读。"
                    : "只有房主可以修改规则，当前为只读查看。", UiTheme.MUTED);
        }
        lastRoomRevision = ClientLobbyData.roomRevision();
    }

    private String durationLabel(GameMode mode) {
        return switch (mode) {
            case TEAM_DEATHMATCH -> "回合时长/秒（0=不限）";
            case SEARCH_DESTROY -> "回合时长/秒";
            case LAST_STANDING -> "总时长/秒";
        };
    }

    private String durationTooltip(GameMode mode) {
        return switch (mode) {
            case TEAM_DEATHMATCH -> "单回合最长时间，击杀目标先到即回合结束。";
            case SEARCH_DESTROY -> "每回合限时；时间到按存活人数判定该回合胜者。";
            case LAST_STANDING -> "整局限时；时间到按存活人数判定胜者。";
        };
    }

    private String winLabel(GameMode mode) {
        return mode == GameMode.SEARCH_DESTROY ? "先胜回合数" : "整场需胜回合数";
    }

    /** 只给当前模式实际使用的行分配位置，隐藏行不会留下空白。 */
    private void layoutRuleRows(GameMode mode) {
        int y = rulesViewportTop;
        for (int row = 0; row < ROW_COUNT; row++) {
            if (rowVisible(mode, row)) {
                ruleBaseYs[row] = y;
                rowYs[row] = y - rulesScroll;
                y += FIELD_HEIGHT + ROW_GAP;
            } else {
                ruleBaseYs[row] = -FIELD_HEIGHT;
                rowYs[row] = -FIELD_HEIGHT;
            }
        }
        int contentHeight = Math.max(0, y - rulesViewportTop - ROW_GAP);
        rulesMaxScroll = Math.max(0, contentHeight - (rulesViewportBottom - rulesViewportTop));
        rulesScroll = Math.max(0, Math.min(rulesScroll, rulesMaxScroll));
        noticeY = contentBottom - 12;
    }

    private boolean rowVisible(GameMode mode, int row) {
        return row != 3 || mode.respawnRules();
    }

    private void applyRuleScroll() {
        for (int row = 0; row < ROW_COUNT; row++) {
            rowYs[row] = ruleBaseYs[row] < 0 ? -FIELD_HEIGHT : ruleBaseYs[row] - rulesScroll;
        }
        setRowY(modeCycle, 0);
        setRowY(minPlayersBox, 0);
        setRowY(targetBox, 1, draft != null && draft.mode().respawnRules());
        setRowY(durationBox, 1);
        setRowY(winBox, 2, draft != null && (draft.mode() == GameMode.SEARCH_DESTROY
                || draft.mode() == GameMode.TEAM_DEATHMATCH));
        setRowY(warmupBox, 2);
        setRowY(respawnBox, 3, draft != null && draft.mode().respawnRules());
        setRowY(autoRespawnToggle, 3, draft != null && draft.mode().respawnRules());
        setRowY(switchBox, 4, draft != null && draft.mode().roundSwapping());
        setRowY(friendlyFireToggle, 4);
        setRowY(roundEndDelayBox, 5);
        setRowY(matchEndDelayBox, 5);
        setRowY(keepInventoryToggle, 6);
        setRowY(suppressDeathToggle, 6);
        setRowY(autoResetToggle, 7);
        setRowY(requireBothToggle, 7);
        setRowY(teamChangeCycle, 8);
        setRowY(balanceCycle, 8);
        setRowY(spawnCycle, 9);
        setRowY(imbalanceBox, 9);
    }

    private void setRowY(net.minecraft.client.gui.components.AbstractWidget widget, int row) {
        setRowY(widget, row, true);
    }

    private void setRowY(net.minecraft.client.gui.components.AbstractWidget widget, int row, boolean allowed) {
        if (widget != null) {
            int y = rowYs[row];
            widget.setY(y < 0 ? -FIELD_HEIGHT : y);
            widget.visible = allowed && y >= rulesViewportTop && y + FIELD_HEIGHT <= rulesViewportBottom;
        }
    }

    /** 表单只展示当前模式适用的行。 */
    private void applyModeVisibility() {
        GameMode mode = draft == null ? GameMode.TEAM_DEATHMATCH : draft.mode();
        targetBox.visible = mode.respawnRules();
        respawnBox.visible = mode.respawnRules();
        autoRespawnToggle.visible = mode.respawnRules();
        switchBox.visible = mode.roundSwapping();
        winBox.visible = mode == GameMode.SEARCH_DESTROY || mode == GameMode.TEAM_DEATHMATCH;
        durationBox.setMessage(Component.literal(durationLabel(mode)));
        if (modeCycle != null) modeCycle.setValue(mode);
        applyRuleScroll();
    }

    private UiEditBox integer(int row, int column, String label, int value, int maxLength,
                              boolean editable, String tooltip) {
        UiTheme.Field field = UiTheme.field(columnX(column, 2, COLUMN_GAP), columnWidth(2, COLUMN_GAP), 96);
        UiEditBox box = new UiEditBox(font, field.controlX(), rowYs[row], field.controlWidth(),
                FIELD_HEIGHT, Component.literal(label));
        box.setMaxLength(maxLength);
        box.setValue(Integer.toString(value));
        box.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        box.setEditable(editable);
        box.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(tooltip)));
        addRenderableWidget(box);
        return box;
    }

    private UiCycleButton<Boolean> bool(int row, int column, String label, boolean value,
                                        boolean editable) {
        UiTheme.Field field = UiTheme.field(columnX(column, 2, COLUMN_GAP), columnWidth(2, COLUMN_GAP), 96);
        UiCycleButton<Boolean> toggle = new UiCycleButton<>(field.controlX(), rowYs[row],
                field.controlWidth(), FIELD_HEIGHT, List.of(Boolean.TRUE, Boolean.FALSE), value,
                flag -> label + "：" + (flag ? "开" : "关"), null, null, UiButton.Kind.SECONDARY);
        toggle.active = editable;
        addRenderableWidget(toggle);
        return toggle;
    }

    private <T> UiCycleButton<T> cycle(int row, int column, String label, List<T> values, T initial,
                                       java.util.function.Function<T, String> display,
                                       java.util.function.Consumer<T> onChange, String tooltip,
                                       boolean editable) {
        UiTheme.Field field = UiTheme.field(columnX(column, 2, COLUMN_GAP), columnWidth(2, COLUMN_GAP), 96);
        UiCycleButton<T> cycle = new UiCycleButton<>(field.controlX(), rowYs[row],
                field.controlWidth(), FIELD_HEIGHT, values, initial,
                value -> label + "·" + display.apply(value), onChange, tooltip,
                UiButton.Kind.SECONDARY);
        cycle.active = editable;
        addRenderableWidget(cycle);
        return cycle;
    }

    private RoomRules collect() {
        return new RoomRules(modeCycle.getValue(),
                parse(targetBox, draft.targetKills()),
                parse(durationBox, draft.matchDurationSeconds()),
                parse(winBox, draft.roundWinTarget()),
                parse(warmupBox, draft.warmupDurationSeconds()),
                parse(respawnBox, draft.respawnDelaySeconds()),
                autoRespawnToggle.getValue(), friendlyFireToggle.getValue(),
                parse(switchBox, draft.switchSideEvery()),
                parse(minPlayersBox, draft.minPlayersToStart()),
                parse(roundEndDelayBox, draft.roundEndDelaySeconds()),
                parse(matchEndDelayBox, draft.matchEndDelaySeconds()),
                keepInventoryToggle.getValue(), suppressDeathToggle.getValue(),
                autoResetToggle.getValue(), requireBothToggle.getValue(),
                teamChangeCycle.getValue(), balanceCycle.getValue(),
                spawnCycle.getValue(), parse(imbalanceBox, draft.maxTeamImbalance()));
    }

    private static int parse(UiEditBox box, int fallback) {
        if (box == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(box.getValue().trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void save() {
        RoomRules candidate = collect().normalized();
        String error = candidate.validationError();
        if (error != null) {
            setStatus(error, UiTheme.ERROR);
            return;
        }
        String own = ClientLobbyData.ownRoomId();
        if (own.isBlank()) {
            setStatus("你当前不在房间中。", UiTheme.ERROR);
            return;
        }
        FpsTdmNetwork.sendToServer(new RoomRulesPacket(candidate));
        draft = candidate;
        setStatus("已保存并向房间成员广播。", UiTheme.SUCCESS);
        refresh();
    }

    private void resetToServerDefaults() {
        RoomRules defaults = ClientConfigData.received()
                ? RoomRules.fromConfigValues(ClientConfigData.values()) : RoomRules.fallback();
        draft = RoomRules.templateFor(draft == null ? GameMode.TEAM_DEATHMATCH : draft.mode(), defaults);
        rebuildWidgets();
        setStatus("已恢复为服务器默认规则，确认后请点“保存并广播”。", UiTheme.INFO);
    }

    private void refresh() {
        if (minecraft != null && minecraft.getConnection() != null) {
            FpsTdmNetwork.sendToServer(new cn.blockforge.generated.generatedmod.network.packet
                    .RoomActionPacket(cn.blockforge.generated.generatedmod.lobby.RoomAction.REFRESH,
                    "", "", "", 0, ""));
        }
    }

    private void setStatus(String message, int color) {
        status = message == null ? "" : message;
        statusColor = color;
    }

    @Override
    public void tick() {
        super.tick();
        marqueeTicks++;
        if (ClientLobbyData.roomRevision() == lastRoomRevision) {
            return;
        }
        lastRoomRevision = ClientLobbyData.roomRevision();
        RoomView own = ownRoom();
        if (own == null) {
            onClose();
            return;
        }
        if (editable()) {
            setStatus(ClientLobbyData.roomMessage(),
                    ClientLobbyData.roomError() ? UiTheme.ERROR : UiTheme.INFO);
        } else {
            // 只读查看（或开赛锁定）：跟随房间同步刷新展示值。
            draft = own.rules();
            rebuildWidgets();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseY >= rulesViewportTop && mouseY <= rulesViewportBottom && rulesMaxScroll > 0) {
            int next = Math.max(0, Math.min(rulesMaxScroll,
                    rulesScroll - (int) Math.round(amount * 24)));
            if (next != rulesScroll) {
                rulesScroll = next;
                applyRuleScroll();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        RoomView own = ownRoom();
        renderShell(graphics, own == null ? "当前不在房间中"
                : own.name() + "  ·  " + (editable() ? "房主可改" : "只读查看"));
        final int section = sectionY;
        paintBand(graphics, section, 13,
                () -> graphics.drawString(font, fit("游戏模式与比赛规则（原团队死斗配置已并入这里）",
                        innerWidth), innerLeft, section, UiTheme.ACCENT, false));
        final int summary = summaryY;
        paintBand(graphics, summary, 18, () -> {
            UiTheme.card(graphics, innerLeft, summary, innerWidth, 18, UiTheme.PANEL);
            graphics.drawString(font, marquee("摘要  " + draft.describe(), innerWidth - 16),
                    innerLeft + 8, summary + 6, UiTheme.TEXT, false);
        });
        int columnWidth = columnWidth(2, COLUMN_GAP);
        for (int row = 0; row < ROW_COUNT; row++) {
            for (int column = 0; column < 2; column++) {
                String label = rowLabel(row, column);
                if (label == null || rowYs[row] < rulesViewportTop
                        || rowYs[row] + FIELD_HEIGHT > rulesViewportBottom) {
                    continue;
                }
                UiTheme.Field field = UiTheme.field(columnX(column, 2, COLUMN_GAP),
                            columnWidth(2, COLUMN_GAP), 96);
                final int bandY = rowYs[row];
                paintBand(graphics, bandY, FIELD_HEIGHT, () -> {
                    UiTheme.card(graphics, field.labelX(), bandY, field.labelWidth(), FIELD_HEIGHT,
                            UiTheme.PANEL_RAISED);
                    graphics.drawString(font, fit(label, field.labelWidth() - 8), field.labelX() + 4,
                            bandY + 8, UiTheme.TEXT, false);
                });
            }
        }
        final int notice = noticeY;
        paintBand(graphics, notice, 10, () -> graphics.drawString(font,
                marquee("不适用于当前模式的设置行会自动隐藏；规则开赛时整体生效，随房间解散销毁。", innerWidth),
                innerLeft, notice, UiTheme.SUBTLE, false));
        if (rulesMaxScroll > 0) {
            int trackTop = rulesViewportTop;
            int trackBottom = rulesViewportBottom;
            graphics.fill(innerLeft + innerWidth - 4, trackTop, innerLeft + innerWidth - 2,
                    trackBottom, UiTheme.BORDER_SUBTLE);
            int trackHeight = trackBottom - trackTop;
            int thumbHeight = Math.max(14, trackHeight * trackHeight
                    / Math.max(trackHeight, trackHeight + rulesMaxScroll));
            int thumbTop = trackTop + (trackHeight - thumbHeight) * rulesScroll
                    / Math.max(1, rulesMaxScroll);
            graphics.fill(innerLeft + innerWidth - 4, thumbTop, innerLeft + innerWidth - 2,
                    thumbTop + thumbHeight, UiTheme.ACCENT);
        }
        renderStatus(graphics, status, statusColor);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String marquee(String value, int maximumWidth) {
        if (value == null || value.isBlank() || maximumWidth <= 0 || font.width(value) <= maximumWidth) {
            return value == null ? "" : value;
        }
        String padded = value + "    " + value;
        int length = padded.length();
        int start = (marqueeTicks / 3) % Math.max(1, length - 1);
        String visible = padded.substring(start) + padded.substring(0, start);
        return font.plainSubstrByWidth(visible, maximumWidth);
    }

    /** 每行的控件标签；当前模式不适用的行返回 null（整行不画）。 */
    private String rowLabel(int row, int column) {
        GameMode mode = draft == null ? GameMode.TEAM_DEATHMATCH : draft.mode();
        if (row == 0) {
            return column == 0 ? "游戏模式" : "最少开赛人数";
        }
        if (row == 1) {
            if (column == 0) {
                return mode.respawnRules() ? "击杀目标（0=不限）" : null;
            }
            return durationLabel(mode);
        }
        if (row == 2) {
            if (column == 0) {
                return mode == GameMode.SEARCH_DESTROY || mode == GameMode.TEAM_DEATHMATCH
                        ? winLabel(mode) : null;
            }
            return "热身时长/秒";
        }
        if (row == 3) {
            if (column == 0) {
                return mode.respawnRules() ? "阵亡恢复/秒" : null;
            }
            return mode.respawnRules() ? "自动复活" : null;
        }
        if (row == 4) {
            if (column == 0) {
                return mode.roundSwapping() ? "换边间隔/回合" : null;
            }
            return "友军伤害";
        }
        if (row == 5) {
            return column == 0 ? "回合结算间隔/秒" : "结算画面/秒";
        }
        if (row == 6) {
            return column == 0 ? "死亡保留背包" : "隐藏死亡消息";
        }
        if (row == 7) {
            return column == 0 ? "赛后自动复位" : "要求两队达标";
        }
        if (row == 8) {
            return column == 0 ? "换队政策" : "自动平衡";
        }
        return column == 0 ? "出生点策略" : "两队最大人数差";
    }

    private static String teamChangeName(TeamChangePolicy value) {
        return switch (value) {
            case NEVER -> "禁止";
            case ONLY_BEFORE_MATCH -> "开赛前";
            case ANYTIME -> "随时";
        };
    }

    private static String balanceName(AutoBalanceMode value) {
        return switch (value) {
            case OFF -> "关闭";
            case ON_JOIN -> "加入时";
            case ON_MATCH_START -> "开赛时";
            case ON_JOIN_AND_MATCH_START -> "加入与开赛";
        };
    }

    private static String spawnName(SpawnSelectionStrategy value) {
        return switch (value) {
            case SEQUENTIAL -> "顺序";
            case RANDOM -> "随机";
            case FARTHEST_FROM_ENEMIES -> "远离敌人";
        };
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
