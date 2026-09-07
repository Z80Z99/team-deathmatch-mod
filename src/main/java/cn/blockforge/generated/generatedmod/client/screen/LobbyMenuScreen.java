package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientLobbyData;
import cn.blockforge.generated.generatedmod.client.ui.UiButton;
import cn.blockforge.generated.generatedmod.client.ui.UiScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import cn.blockforge.generated.generatedmod.lobby.MatchmakingAction;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.MatchmakingActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 大厅入口：房间、匹配和创作工具，以及当前连接状态。 */
public final class LobbyMenuScreen extends UiScreen {
    private static final int ENTRY_HEIGHT = 34;

    private final Screen parent;
    private UiButton roomButton;
    private UiButton matchButton;
    private int roomRowY;
    private int matchRowY;
    private int ticks;

    public LobbyMenuScreen(Screen parent) {
        super(Component.literal("团队死斗"));
        this.parent = parent;
    }

    public static void open(Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.getConnection() != null) {
            LobbyMenuScreen screen = new LobbyMenuScreen(parent);
            minecraft.setScreen(screen);
            screen.refresh();
        }
    }

    @Override
    protected void init() {
        beginLayout(460, 230, BUTTON_HEIGHT, true, true);
        roomRowY = flowRow(ENTRY_HEIGHT);
        matchRowY = flowRow(ENTRY_HEIGHT);

        roomButton = flowWidget(entryButton("房间大厅", roomRowY, this::openRooms,
                "创建、浏览和加入房间；已在房间时直接打开“我的房间”。"), roomRowY);
        matchButton = flowWidget(entryButton("快速匹配", matchRowY,
                () -> MatchmakingScreen.open(this),
                "进入独立的匹配界面：排队、看序位与取消都在那里完成，和房间大厅互不混用。"), matchRowY);

        int toolsY = flowRow(BUTTON_HEIGHT);
        flowWidget(uiButton("地图工作台", columnX(0, 2, 6), toolsY, columnWidth(2, 6),
                () -> MapLibraryScreen.open(this), null), toolsY);
        flowWidget(uiButton("HUD 编辑器", columnX(1, 2, 6), toolsY, columnWidth(2, 6),
                () -> minecraft.setScreen(new HudLayoutScreen(this)), null), toolsY);
        footerButton("返回游戏", 0, 1, 0, this::onClose, null, UiButton.Kind.SECONDARY);
        updateButtons();
    }

    private void openRooms() {
        if (!ClientLobbyData.ownRoomId().isBlank()) {
            RoomScreen.open(this);
        } else {
            LobbyScreen.open(this);
        }
    }

    private UiButton entryButton(String label, int y, Runnable action, String tooltip) {
        UiButton button = new UiButton(innerLeft, y, innerWidth, ENTRY_HEIGHT, Component.literal(label),
                ignored -> action.run(), label.equals("快速匹配") ? UiButton.Kind.PRIMARY : UiButton.Kind.SECONDARY);
        button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(tooltip)));
        return button;
    }

    private void refresh() {
        if (minecraft == null || minecraft.getConnection() == null) {
            return;
        }
        // 只为让返回后的状态摘要保持新鲜；本菜单不做任何匹配 / 房间操作。
        FpsTdmNetwork.sendToServer(new MatchmakingActionPacket(MatchmakingAction.REFRESH));
        FpsTdmNetwork.sendToServer(new cn.blockforge.generated.generatedmod.network.packet.RoomActionPacket(
                cn.blockforge.generated.generatedmod.lobby.RoomAction.REFRESH, "", "", "", 0, ""));
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        updateButtons();
        if (ticks % 40 == 0) {
            refresh();
        }
    }

    private void updateButtons() {
        boolean connected = minecraft != null && minecraft.getConnection() != null;
        boolean forming = ClientLobbyData.dynamicReadySeconds() > 0;
        if (roomButton != null) {
            boolean inRoom = !ClientLobbyData.ownRoomId().isBlank();
            roomButton.setMessage(Component.literal(inRoom ? "我的房间" : "房间大厅"));
            roomButton.active = connected;
        }
        if (matchButton != null) {
            String label = forming ? "快速匹配（已匹配）"
                    : ClientLobbyData.matchmaking().queued() ? "快速匹配（排队中）" : "快速匹配";
            matchButton.setMessage(Component.literal(label));
            matchButton.setSelected(ClientLobbyData.matchmaking().queued() || forming);
            matchButton.active = connected;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderShell(graphics, ClientLobbyData.matchActive() ? "比赛进行中" : "多人竞技 · 在线大厅");
        String message = ClientLobbyData.matchmakingMessage().isBlank()
                ? ClientLobbyData.roomMessage() : ClientLobbyData.matchmakingMessage();
        renderStatus(graphics, message,
                ClientLobbyData.matchmakingError() || ClientLobbyData.roomError()
                        ? UiTheme.ERROR : UiTheme.INFO);
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
