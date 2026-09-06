package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientLobbyData;
import cn.blockforge.generated.generatedmod.client.ui.UiButton;
import cn.blockforge.generated.generatedmod.client.ui.UiEditBox;
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

/** 房间大厅：只负责创建、浏览和加入房间；快速匹配使用独立界面，本界面底部没有文本框。 */
public final class LobbyScreen extends UiScreen {
    private static final int ROOM_ROW = 22;
    private static final int FOOTER_COLUMNS = 4;
    /** 顶部临时提示（浮层）停留的 tick 数。 */
    private static final int TOAST_TICKS = 90;

    private final Screen parent;
    private UiButton createButton;
    private UiButton joinButton;
    private UiButton myRoomButton;
    private UiEditBox nameBox;
    private UiEditBox passwordBox;
    private UiEditBox maxPlayersBox;
    private String selectedRoomId = "";
    private String lastEnteredRoomId = "";
    private int createTitleY;
    private int labelRowY;
    private int inputRowY;
    private int listTitleY;
    private int listTop;
    private int listBottom;
    private int nameX;
    private int nameWidth;
    private int passwordX;
    private int passwordWidth;
    private int playersX;
    private int playersWidth;
    private int roomScroll;
    private int ticks;
    private int lastRoomRevision = -1;
    private String toastMessage = "";
    private int toastColor = UiTheme.INFO;
    private int toastTicks;

    public LobbyScreen(Screen parent) {
        super(Component.literal("房间大厅"));
        this.parent = parent;
    }

    public static void open(Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.getConnection() != null) {
            LobbyScreen screen = new LobbyScreen(parent);
            minecraft.setScreen(screen);
            screen.refresh();
        }
    }

    @Override
    protected void init() {
        beginLayout(640, 0, BUTTON_HEIGHT, false);
        createTitleY = flowRow(13);
        labelRowY = flowRow(10);
        inputRowY = flowRow(BUTTON_HEIGHT);
        listTitleY = flowRow(13);
        listTop = flowCursor() + 2;
        listBottom = footerTop - 2;
        contentBottom = Math.min(contentBottom, listTop);

        int gap = 6;
        int grid = Math.max(1, innerWidth - gap * 2);
        playersWidth = clamp(grid * 12 / 100, 34, 64);
        passwordWidth = clamp(grid * 22 / 100, 72, 150);
        nameWidth = Math.max(60, grid - playersWidth - passwordWidth);
        if (nameWidth + passwordWidth + playersWidth + gap * 2 > innerWidth) {
            nameWidth = Math.max(1, innerWidth - passwordWidth - playersWidth - gap * 2);
        }
        nameX = innerLeft;
        passwordX = nameX + nameWidth + gap;
        playersX = passwordX + passwordWidth + gap;

        nameBox = flowWidget(box("房间名称", "我的房间", nameX, inputRowY, nameWidth, 32), inputRowY);
        passwordBox = flowWidget(box("密码（创建/加入）", "", passwordX, inputRowY, passwordWidth, 16), inputRowY);
        maxPlayersBox = flowWidget(box("人数", "16", playersX, inputRowY, playersWidth, 3), inputRowY);

        createButton = footerButton("创建房间", 0, FOOTER_COLUMNS, 0, this::createRoom,
                "按上方名称、密码和人数创建房间。", UiButton.Kind.PRIMARY);
        joinButton = footerButton("加入房间", 1, FOOTER_COLUMNS, 0, this::joinSelected,
                "选择房间后加入；私有房间的密码填写在上方密码框。", UiButton.Kind.PRIMARY);
        myRoomButton = footerButton("我的房间", 2, FOOTER_COLUMNS, 0, this::openOwnRoom,
                "打开自己所在房间的管理界面。", UiButton.Kind.SECONDARY);
        footerButton("返回", 3, FOOTER_COLUMNS, 0, this::onClose,
                "返回上一级界面。", UiButton.Kind.DANGER);
        reconcileSelection();
        lastEnteredRoomId = ClientLobbyData.ownRoomId();
        updateButtons();
    }

    private UiEditBox box(String hint, String initial, int x, int y, int width, int maxLength) {
        UiEditBox box = new UiEditBox(font, x, y, width, BUTTON_HEIGHT, Component.literal(hint));
        box.setMaxLength(maxLength);
        box.setValue(initial == null ? "" : initial);
        if ("人数".equals(hint)) {
            box.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
        } else if (hint.startsWith("密码")) {
            box.setFilter(value -> value.chars().allMatch(c -> !Character.isWhitespace(c)
                    && c >= 33 && c <= 126));
        }
        return box;
    }

    private void createRoom() {
        send(RoomAction.CREATE, "", nameBox.getValue().trim(), parseInt(maxPlayersBox.getValue(), 16),
                passwordBox.getValue().trim());
    }

    private void joinSelected() {
        RoomView room = selectedRoom();
        if (room != null) {
            send(RoomAction.JOIN, room.id(), "", 0, passwordBox.getValue().trim());
        }
    }

    private void openOwnRoom() {
        if (!ClientLobbyData.ownRoomId().isBlank()) RoomScreen.open(this);
    }

    private void send(RoomAction action, String roomId, String roomName, int maxPlayers, String password) {
        if (minecraft != null && minecraft.getConnection() != null) {
            FpsTdmNetwork.sendToServer(new RoomActionPacket(action, roomId, roomName, "", maxPlayers, password));
        }
    }

    private void refresh() {
        if (minecraft != null && minecraft.getConnection() != null) {
            send(RoomAction.REFRESH, "", "", 0, "");
        }
    }

    private void reconcileSelection() {
        List<RoomView> rooms = ClientLobbyData.rooms();
        if (roomById(rooms, selectedRoomId) == null) {
            selectedRoomId = rooms.isEmpty() ? "" : rooms.get(0).id();
        }
        roomScroll = clamp(roomScroll, 0, maxScrollRows());
    }

    private RoomView selectedRoom() {
        return roomById(ClientLobbyData.rooms(), selectedRoomId);
    }

    private RoomView roomById(List<RoomView> rooms, String id) {
        if (id == null || id.isBlank()) return null;
        for (RoomView room : rooms) if (id.equals(room.id())) return room;
        return null;
    }

    private int visibleRows() {
        return Math.max(1, (listBottom - listTop) / ROOM_ROW);
    }

    private int maxScrollRows() {
        return Math.max(0, ClientLobbyData.rooms().size() - visibleRows());
    }

    @Override
    public void tick() {
        super.tick();
        if (nameBox != null) nameBox.tick();
        if (passwordBox != null) passwordBox.tick();
        if (maxPlayersBox != null) maxPlayersBox.tick();
        ticks++;
        if (ClientLobbyData.roomRevision() != lastRoomRevision) {
            lastRoomRevision = ClientLobbyData.roomRevision();
            reconcileSelection();
            String message = ClientLobbyData.roomMessage();
            if (!message.isBlank()) {
                toastMessage = message;
                toastColor = ClientLobbyData.roomError() ? UiTheme.ERROR : UiTheme.SUCCESS;
                toastTicks = TOAST_TICKS;
            }
        }
        if (toastTicks > 0) toastTicks--;
        updateButtons();
        String ownId = ClientLobbyData.ownRoomId();
        if (!ownId.isBlank() && !ownId.equals(lastEnteredRoomId)) {
            lastEnteredRoomId = ownId;
            RoomScreen.open(this);
            return;
        }
        if (ownId.isBlank()) lastEnteredRoomId = "";
        if (ticks % 20 == 0) refresh();
    }

    private void updateButtons() {
        boolean inRoom = !ClientLobbyData.ownRoomId().isBlank();
        boolean queued = ClientLobbyData.matchmaking().queued();
        if (createButton != null) createButton.active = !inRoom && !queued;
        if (joinButton != null) joinButton.active = selectedRoom() != null && !inRoom && !queued
                && selectedRoom().state() == RoomState.OPEN;
        if (myRoomButton != null) myRoomButton.active = inRoom;
        if (nameBox != null) nameBox.setEditable(!inRoom && !queued);
        if (passwordBox != null) passwordBox.setEditable(!inRoom && !queued);
        if (maxPlayersBox != null) maxPlayersBox.setEditable(!inRoom && !queued);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= innerLeft && mouseX <= innerLeft + innerWidth
                && mouseY >= listTop && mouseY < listBottom) {
            int index = roomScroll + (int) ((mouseY - listTop) / ROOM_ROW);
            List<RoomView> rooms = ClientLobbyData.rooms();
            if (index >= 0 && index < rooms.size()) {
                selectedRoomId = rooms.get(index).id();
                updateButtons();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX >= innerLeft && mouseX <= innerLeft + innerWidth
                && mouseY >= listTop && mouseY < listBottom && maxScrollRows() > 0) {
            roomScroll = clamp(roomScroll - (int) Math.round(amount), 0, maxScrollRows());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderShell(graphics, "房间大厅 · 创建、浏览和加入房间");
        reconcileSelection();
        final int title = createTitleY;
        paintBand(graphics, title, 13, () -> section(graphics, "创建房间", innerLeft, title, innerWidth));
        final int labels = labelRowY;
        paintBand(graphics, labels, 10, () -> {
            graphics.drawString(font, fit("名称", nameWidth), nameX, labels + 1, UiTheme.MUTED, false);
            graphics.drawString(font, fit("密码（创建/加入）", passwordWidth), passwordX, labels + 1,
                    UiTheme.MUTED, false);
            graphics.drawString(font, fit("人数", playersWidth), playersX, labels + 1, UiTheme.MUTED, false);
        });
        final int listTitle = listTitleY;
        paintBand(graphics, listTitle, 13, () -> section(graphics, "房间列表 · 房间名 / 模式 / 人数",
                innerLeft, listTitle, innerWidth));
        renderRoomList(graphics);
        renderToast(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** 顶部临时提示浮层：服务器消息只在存活期内画在列表区顶部，界面底部没有常驻文本框。 */
    private void renderToast(GuiGraphics graphics) {
        if (toastTicks <= 0 || toastMessage.isBlank()) return;
        int toastWidth = Math.min(innerWidth - 8, font.width(toastMessage) + 20);
        int x = innerLeft + (innerWidth - toastWidth) / 2;
        int y = listTop + 4;
        UiTheme.card(graphics, x, y, toastWidth, 18, UiTheme.PANEL_SELECTED);
        graphics.renderOutline(x, y, toastWidth, 18, toastColor);
        graphics.drawString(font, fit(toastMessage, toastWidth - 12), x + 6, y + 5, toastColor, false);
    }

    private void renderRoomList(GuiGraphics graphics) {
        List<RoomView> rooms = ClientLobbyData.rooms();
        UiTheme.card(graphics, innerLeft, listTop, innerWidth, Math.max(ROOM_ROW, listBottom - listTop), 0x800D141B);
        if (rooms.isEmpty()) {
            graphics.drawString(font, fit("暂无房间，创建一间房开始游戏。", innerWidth - 20),
                    innerLeft + 8, listTop + 8, UiTheme.MUTED, false);
            return;
        }
        int modeWidth = Math.min(150, Math.max(90, innerWidth / 4));
        int countWidth = Math.min(90, Math.max(60, innerWidth / 7));
        int nameWidth = Math.max(1, innerWidth - modeWidth - countWidth - 22);
        for (int row = 0; row < visibleRows(); row++) {
            int index = roomScroll + row;
            if (index >= rooms.size()) break;
            RoomView room = rooms.get(index);
            int y = listTop + row * ROOM_ROW + 1;
            boolean selected = room.id().equals(selectedRoomId);
            UiTheme.card(graphics, innerLeft + 2, y, innerWidth - 6, ROOM_ROW - 3,
                    selected ? UiTheme.PANEL_SELECTED : UiTheme.PANEL_RAISED);
            graphics.fill(innerLeft + 2, y, innerLeft + 5, y + ROOM_ROW - 3,
                    selected ? UiTheme.ACCENT : UiTheme.BORDER_SUBTLE);
            String roomName = room.name() == null || room.name().isBlank() ? room.id() : room.name();
            graphics.drawString(font, fit(roomName, nameWidth), innerLeft + 10, y + 5, UiTheme.TEXT, false);
            graphics.drawString(font, fit(room.rules().mode().displayName(), modeWidth - 8),
                    innerLeft + 10 + nameWidth, y + 5, UiTheme.MUTED, false);
            String count = room.memberCount() + (room.maxPlayers() <= 0 ? "/不限" : "/" + room.maxPlayers());
            graphics.drawString(font, fit(count, countWidth - 8), innerLeft + innerWidth - countWidth - 2,
                    y + 5, UiTheme.TEXT, false);
        }
        if (maxScrollRows() > 0) {
            int x = innerLeft + innerWidth - 3;
            int track = visibleRows() * ROOM_ROW;
            graphics.fill(x, listTop, x + 2, listTop + track, UiTheme.BORDER_SUBTLE);
            int thumb = Math.max(10, track * visibleRows() / Math.max(1, rooms.size()));
            int range = Math.max(1, track - thumb);
            graphics.fill(x, listTop + range * roomScroll / maxScrollRows(), x + 2,
                    listTop + range * roomScroll / maxScrollRows() + thumb, UiTheme.ACCENT);
        }
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
