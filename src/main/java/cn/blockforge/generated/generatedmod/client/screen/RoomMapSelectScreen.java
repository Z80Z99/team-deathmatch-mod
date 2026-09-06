package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientLobbyData;
import cn.blockforge.generated.generatedmod.client.ui.UiButton;
import cn.blockforge.generated.generatedmod.client.ui.UiScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import cn.blockforge.generated.generatedmod.lobby.RoomAction;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.RoomActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 房间地图选择：列表展示服务器上所有已注册地图（含所有玩家制作的地图），
 * 点选即可把该地图设为本房间用图，不再需要手输地图 ID。
 */
public final class RoomMapSelectScreen extends UiScreen {
    private static final int MAP_ROW = 24;

    private final Screen parent;
    private UiButton selectButton;
    private UiButton refreshButton;
    private String selectedMapId = "";
    private int listTop;
    private int listBottom;
    private boolean listVisible;
    private int scroll;
    private int ticks;
    private int lastOptionsRevision = -1;

    public RoomMapSelectScreen(Screen parent) {
        super(Component.literal("选择地图"));
        this.parent = parent;
    }

    public static void open(Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.getConnection() != null) {
            RoomMapSelectScreen screen = new RoomMapSelectScreen(parent);
            minecraft.setScreen(screen);
            screen.requestList();
        }
    }

    @Override
    protected void init() {
        beginLayout(560, 0, BUTTON_HEIGHT);
        listTop = flowRow(13) + 15;
        int space = statusTop - 2 - listTop;
        listVisible = space >= MAP_ROW + 4;
        listBottom = listTop + Math.max(1, space / MAP_ROW) * MAP_ROW;

        selectButton = footerButton("选用此图", 0, 3, 0, this::confirm,
                "把选中的地图设置为房间用图。", UiButton.Kind.PRIMARY);
        refreshButton = footerButton("刷新列表", 1, 3, 0, this::requestList,
                "重新读取服务器地图列表（新制作的地图会出现在这里）。", UiButton.Kind.SECONDARY);
        footerButton("返回", 2, 3, 0, this::onClose, "返回我的房间。", UiButton.Kind.DANGER);

        selectedMapId = currentRoomMapId();
        updateButtons();
    }

    private void requestList() {
        if (minecraft != null && minecraft.getConnection() != null) {
            FpsTdmNetwork.sendToServer(new RoomActionPacket(RoomAction.MAP_LIST, "", "", "", 0, ""));
        }
    }

    private void confirm() {
        RoomViewSelected holder = new RoomViewSelected();
        if (!selectedMapId.isBlank() && holder.ok()) {
            if (minecraft != null && minecraft.getConnection() != null) {
                FpsTdmNetwork.sendToServer(new RoomActionPacket(RoomAction.SET_MAP,
                        ClientLobbyData.ownRoomId(), "", selectedMapId, 0, ""));
            }
            onClose();
        }
    }

    /** 轻量校验：仍在房间内才允许下发设置。 */
    private static final class RoomViewSelected {
        boolean ok() {
            return !ClientLobbyData.ownRoomId().isBlank();
        }
    }

    private String currentRoomMapId() {
        String ownId = ClientLobbyData.ownRoomId();
        for (var room : ClientLobbyData.rooms()) {
            if (room.id().equals(ownId)) {
                return room.mapId() == null ? "" : room.mapId();
            }
        }
        return "";
    }

    private List<ClientLobbyData.MapOption> options() {
        return ClientLobbyData.mapOptions();
    }

    private int visibleRows() {
        return listVisible ? Math.max(1, (listBottom - listTop) / MAP_ROW) : 0;
    }

    private int listMaxScroll() {
        return Math.max(0, options().size() - visibleRows());
    }

    private void updateButtons() {
        boolean inRoom = !ClientLobbyData.ownRoomId().isBlank();
        if (selectButton != null) {
            selectButton.active = inRoom && !selectedMapId.isBlank();
        }
        if (refreshButton != null) refreshButton.active = true;
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        if (ClientLobbyData.mapOptionsRevision() != lastOptionsRevision) {
            lastOptionsRevision = ClientLobbyData.mapOptionsRevision();
        }
        if (ticks % 40 == 0) requestList();
        updateButtons();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (listVisible && button == 0 && mouseX >= innerLeft && mouseX <= innerLeft + innerWidth
                && mouseY >= listTop && mouseY < listTop + visibleRows() * MAP_ROW) {
            int index = scroll + (int) ((mouseY - listTop) / MAP_ROW);
            List<ClientLobbyData.MapOption> options = options();
            if (index >= 0 && index < options.size()) {
                selectedMapId = options.get(index).id();
                updateButtons();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (listVisible && mouseY >= listTop && mouseY < listBottom && listMaxScroll() > 0) {
            scroll = clamp(scroll - (int) Math.round(amount), 0, listMaxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderShell(graphics, "所有玩家制作的地图都在这个列表里");
        final int titleBand = listTop - 15;
        paintBand(graphics, titleBand, 13, () -> {
            section(graphics, "地图列表", innerLeft, titleBand, innerWidth / 2);
            String hint = listMaxScroll() > 0 ? "点击选择 · 滚轮翻页" : "点击选择";
            graphics.drawString(font, hint, innerLeft + innerWidth - font.width(hint), titleBand + 4,
                    UiTheme.SUBTLE, false);
        });
        List<ClientLobbyData.MapOption> options = options();
        UiTheme.card(graphics, innerLeft, listTop, innerWidth,
                Math.max(MAP_ROW, listBottom - listTop), 0x800D141B);
        if (options.isEmpty()) {
            UiTheme.card(graphics, innerLeft + 2, listTop + 2, innerWidth - 4, MAP_ROW * 2,
                    UiTheme.PANEL_RAISED);
            graphics.drawString(font, fit("服务器还没有任何地图。", innerWidth - 20),
                    innerLeft + 8, listTop + 8, UiTheme.MUTED, false);
            graphics.drawString(font, fit("可在“地图工作台”里新建地图并画出出生点，之后回到这里选择。",
                    innerWidth - 20), innerLeft + 8, listTop + 20, UiTheme.SUBTLE, false);
        } else {
            String roomMapId = currentRoomMapId();
            for (int row = 0; row < visibleRows(); row++) {
                int index = scroll + row;
                if (index >= options.size()) {
                    break;
                }
                renderRow(graphics, options.get(index), listTop + row * MAP_ROW, roomMapId);
            }
        }
        renderStatus(graphics, ClientLobbyData.roomMessage(),
                ClientLobbyData.roomError() ? UiTheme.ERROR : UiTheme.INFO);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderRow(GuiGraphics graphics, ClientLobbyData.MapOption option, int rowTop,
                           String roomMapId) {
        int y = rowTop + 1;
        boolean selected = option.id().equals(selectedMapId);
        UiTheme.card(graphics, innerLeft + 2, y, innerWidth - 8, MAP_ROW - 4,
                selected ? UiTheme.PANEL_SELECTED : UiTheme.PANEL_RAISED);
        graphics.fill(innerLeft + 2, y, innerLeft + 5, y + MAP_ROW - 4,
                selected ? UiTheme.ACCENT : UiTheme.BORDER_SUBTLE);
        boolean current = !roomMapId.isBlank() && roomMapId.equals(option.id());
        int badgeWidth = innerWidth < 420 ? 64 : 84;
        int textWidth = Math.max(1, innerWidth - badgeWidth - 26);
        graphics.drawString(font, fit(option.displayName() + (current ? "  ·  本房在用" : ""), textWidth),
                innerLeft + 10, y + 1, current ? UiTheme.ACCENT : UiTheme.TEXT, false);
        graphics.drawString(font, fit(option.id() + (option.ownerName().isBlank()
                ? "  ·  服务器地图" : "  ·  玩家 " + option.ownerName() + " 制作"), textWidth),
                innerLeft + 10, y + 11, UiTheme.MUTED, false);
        badge(graphics, option.ownerName().isBlank() ? "官方" : "玩家作品",
                innerLeft + innerWidth - badgeWidth - 4, y + 1, badgeWidth,
                option.ownerName().isBlank() ? UiTheme.INFO : UiTheme.SUCCESS);
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
