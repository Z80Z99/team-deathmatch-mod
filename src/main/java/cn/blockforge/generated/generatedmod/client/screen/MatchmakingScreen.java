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
        beginLayout(500, 270, BUTTON_HEIGHT);
        cardY = flowRow(CARD_HEIGHT);
        ruleY = flowRow(28);
        hintY = flowRow(6);
        actionButton = footerButton("快速匹配", 1, 2, 0, this::toggle,
                "进入或退出独立的快速匹配队列。", UiButton.Kind.PRIMARY);
        footerButton("返回", 0, 2, 0, this::onClose,
                "返回上一级界面。", UiButton.Kind.DANGER);
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
        renderShell(graphics, "自动组局 · " + MatchmakingManager.MIN_PLAYERS_TO_FORM + " 人起赛");
        MatchmakingStatus status = ClientLobbyData.matchmaking();
        int waited = ClientLobbyData.dynamicWaitedTicks();
        int ready = ClientLobbyData.dynamicReadySeconds();
        boolean forming = ready > 0;
        int color = status.error() ? UiTheme.ERROR : forming ? UiTheme.SUCCESS
                : status.queued() ? UiTheme.WARNING : UiTheme.INFO;
        final int card = cardY;
        paintBand(graphics, card, CARD_HEIGHT, () -> {
            String state = status.error() && !status.message().isBlank() ? status.message()
                    : forming ? "已匹配，" + ready + " 秒后开赛"
                    : status.queued() ? "正在寻找对手"
                    : ClientLobbyData.matchActive() ? "服务器已有比赛，暂时不能匹配"
                    : !ClientLobbyData.ownRoomId().isBlank() ? "当前已在房间中" : "准备就绪";
            graphics.drawString(font, fit(state, innerWidth - 20), innerLeft + 10, card + 9, color, false);
            String detail = status.queued() || forming
                    ? "队列 " + status.queueSize() + " 人  ·  你的序位 " + Math.max(1, status.position())
                    : "队列 " + status.queueSize() + " 人";
            graphics.drawString(font, fit(detail, innerWidth - 20), innerLeft + 10, card + 29,
                    UiTheme.TEXT, false);
            graphics.drawString(font, fit("已等待 " + UiTheme.formatTicks(waited)
                    + (forming ? "  ·  即将入场" : ""), innerWidth - 20),
                    innerLeft + 10, card + 49, UiTheme.MUTED, false);
        });
        final int rules = ruleY;
        paintBand(graphics, rules, 28, () -> {
            section(graphics, "比赛规则", innerLeft, rules, innerWidth);
            graphics.drawString(font, fit("至少 " + MatchmakingManager.MIN_PLAYERS_TO_FORM
                    + " 人成局 · 不限人数 · 成局后 "
                    + MatchmakingManager.READY_SECONDS + " 秒准备", innerWidth), innerLeft, rules + 15,
                    UiTheme.MUTED, false);
        });
        final int hint = hintY;
        paintBand(graphics, hint, 6, () -> progress(graphics, innerLeft, hint, innerWidth, 4,
                forming ? 1.0F - ready / (float) MatchmakingManager.READY_SECONDS
                        : status.queueSize() / (float) MatchmakingManager.MIN_PLAYERS_TO_FORM, color));
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
