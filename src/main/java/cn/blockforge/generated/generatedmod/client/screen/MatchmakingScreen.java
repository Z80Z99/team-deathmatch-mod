package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientLobbyData;
import cn.blockforge.generated.generatedmod.client.ui.UiButton;
import cn.blockforge.generated.generatedmod.client.ui.UiScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import cn.blockforge.generated.generatedmod.lobby.MatchmakingAction;
import cn.blockforge.generated.generatedmod.lobby.MatchmakingManager;
import cn.blockforge.generated.generatedmod.lobby.MatchmakingStatus;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.MatchmakingActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 独立的快速匹配界面。匹配队列不再和房间大厅共用一套操作区，避免把两种游戏入口混在一起。
 */
public final class MatchmakingScreen extends UiScreen {
    private static final int CARD_HEIGHT = 76;
    private final Screen parent;
    private UiButton actionButton;
    private int cardY;
    private int ruleY;
    private int hintY;
    private int ticks;

    public MatchmakingScreen(Screen parent) {
        super(Component.literal("快速匹配"));
        this.parent = parent;
    }

    public static void open(Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.getConnection() != null) {
            MatchmakingScreen screen = new MatchmakingScreen(parent);
            minecraft.setScreen(screen);
            screen.refresh();
        }
    }

    @Override
    protected void init() {
        beginLayout(500, 290, BUTTON_HEIGHT * 2 + 4);
        cardY = flowRow(CARD_HEIGHT);
        ruleY = flowRow(28);
        hintY = flowRow(18);
        actionButton = footerButton("快速匹配", 0, 2, 0, this::toggle,
                "进入或退出独立的快速匹配队列。", UiButton.Kind.PRIMARY);
        footerButton("房间大厅", 1, 2, 0, () -> LobbyScreen.open(parent),
                "去房间大厅创建 / 加入房间；匹配与房间彼此独立。", UiButton.Kind.SECONDARY);
        footerButton("返回", 0, 2, 1, this::onClose,
                "返回上一级界面。", UiButton.Kind.DANGER);
        footerButton("刷新状态", 1, 2, 1, this::refresh,
                "重新读取匹配队列状态。", UiButton.Kind.SECONDARY);
        updateButton();
    }

    private void toggle() {
        MatchmakingStatus status = ClientLobbyData.matchmaking();
        send(status.queued() ? MatchmakingAction.LEAVE : MatchmakingAction.JOIN);
    }

    private void send(MatchmakingAction action) {
        if (minecraft != null && minecraft.getConnection() != null) {
            FpsTdmNetwork.sendToServer(new MatchmakingActionPacket(action));
        }
    }

    private void refresh() {
        send(MatchmakingAction.REFRESH);
    }

    private void updateButton() {
        if (actionButton == null) return;
        MatchmakingStatus status = ClientLobbyData.matchmaking();
        boolean forming = ClientLobbyData.dynamicReadySeconds() > 0;
        actionButton.setMessage(Component.literal(forming ? "已匹配" : status.queued() ? "取消匹配" : "开始匹配"));
        actionButton.setSelected(status.queued() || forming);
        actionButton.active = !forming && ClientLobbyData.ownRoomId().isBlank()
                && !ClientLobbyData.matchActive();
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        updateButton();
        if (ticks % 20 == 0) refresh();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderShell(graphics, "匹配队列独立运行；房间列表请返回大厅");
        MatchmakingStatus status = ClientLobbyData.matchmaking();
        int waited = ClientLobbyData.dynamicWaitedTicks();
        int ready = ClientLobbyData.dynamicReadySeconds();
        boolean forming = ready > 0;
        int color = status.error() ? UiTheme.ERROR : forming ? UiTheme.SUCCESS
                : status.queued() ? UiTheme.WARNING : UiTheme.INFO;
        final int card = cardY;
        paintBand(graphics, card, CARD_HEIGHT, () -> {
            UiTheme.card(graphics, innerLeft, card, innerWidth, CARD_HEIGHT, UiTheme.PANEL_RAISED);
            String state = status.error() && !status.message().isBlank() ? status.message()
                    : forming ? "已匹配，" + ready + " 秒后开赛"
                    : status.queued() ? "正在寻找对手"
                    : ClientLobbyData.matchActive() ? "服务器已有比赛，暂时不能匹配"
                    : "当前未进入匹配队列";
            graphics.drawString(font, fit(state, innerWidth - 20), innerLeft + 10, card + 9, color, false);
            String detail = status.queued() || forming
                    ? "队列 " + status.queueSize() + " 人  ·  你的序位 " + Math.max(1, status.position())
                    : "加入后会显示队列人数、序位和等待时间";
            graphics.drawString(font, fit(detail, innerWidth - 20), innerLeft + 10, card + 29,
                    UiTheme.TEXT, false);
            graphics.drawString(font, fit("已等待 " + UiTheme.formatTicks(waited)
                    + (forming ? "  ·  准备倒计时动态更新" : ""), innerWidth - 20),
                    innerLeft + 10, card + 49, UiTheme.MUTED, false);
        });
        final int rules = ruleY;
        paintBand(graphics, rules, 28, () -> {
            section(graphics, "固定匹配规则", innerLeft, rules, innerWidth);
            graphics.drawString(font, fit("至少 " + MatchmakingManager.MIN_PLAYERS_TO_FORM
                    + " 人成局 · 无人数上限 · 无限等待 · 成局后 "
                    + MatchmakingManager.READY_SECONDS + " 秒准备", innerWidth), innerLeft, rules + 15,
                    UiTheme.MUTED, false);
        });
        final int hint = hintY;
        paintBand(graphics, hint, 18, () -> graphics.drawString(font,
                fit("大厅只负责创建 / 浏览 / 加入房间；匹配状态会同步显示在游戏主界面 HUD。", innerWidth),
                innerLeft, hint, UiTheme.SUBTLE, false));
        String message = status.message().isBlank() ? ClientLobbyData.matchmakingMessage() : status.message();
        renderStatus(graphics, message, status.error() ? UiTheme.ERROR : color);
        super.render(graphics, mouseX, mouseY, partialTick);
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
