package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientLobbyData;
import cn.blockforge.generated.generatedmod.client.ui.*;
import cn.blockforge.generated.generatedmod.lobby.RoomAction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RoomCreateScreen extends UiScreen {
    private final Screen parent;
    private final String[] values = {"我的房间", "", "16"};
    private final String[] labels = {"房间名称", "密码（留空为公开）", "人数上限（2～64，0 为默认）"};
    private final int[] labelYs = new int[3];
    private UiButton create;
    private boolean waiting;
    private int waitTicks;
    private int revision;
    public RoomCreateScreen(Screen parent) { super(Component.literal("创建房间")); this.parent = parent; }
    @Override protected void init() {
        beginLayout(460, 310, BUTTON_HEIGHT);
        for (int i = 0; i < 3; i++) {
            int index = i;
            labelYs[i] = flowRow(10);
            int y = flowRow(BUTTON_HEIGHT);
            UiEditBox box = flowWidget(new UiEditBox(font, innerLeft, y, innerWidth, BUTTON_HEIGHT, Component.literal(labels[i])), y);
            box.setMaxLength(i == 0 ? 32 : i == 1 ? 16 : 3);
            box.setValue(values[i]); box.setResponder(value -> values[index] = value);
            if (i == 2) box.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
            if (i == 1) {
                box.setFilter(value -> value.chars().allMatch(c -> c >= 33 && c <= 126));
                box.setFormatter((value, start) -> net.minecraft.util.FormattedCharSequence.forward("*".repeat(value.length()), net.minecraft.network.chat.Style.EMPTY));
            }
        }
        footerButton("取消", 0, 2, 0, this::onClose, null, UiButton.Kind.SECONDARY);
        create = footerButton("创建房间", 1, 2, 0, () -> {
            if (!canCreate()) return;
            waiting = true; waitTicks = 0; revision = ClientLobbyData.roomRevision();
            LobbyScreen.request(RoomAction.CREATE, "", values[0].trim(), Integer.parseInt(values[2]), values[1]);
        }, null, UiButton.Kind.PRIMARY);
        updateButton();
    }
    private void updateButton() {
        create.active = canCreate();
        create.setMessage(Component.literal(waiting ? "正在创建" : "创建房间"));
    }
    private boolean canCreate() {
        if (waiting || values[0].isBlank() || !ClientLobbyData.ownRoomId().isBlank() || ClientLobbyData.matchmaking().queued()) return false;
        try {
            int capacity = Integer.parseInt(values[2]);
            return capacity == 0 || capacity >= 2 && capacity <= 64;
        } catch (NumberFormatException ignored) { return false; }
    }
    @Override public void tick() {
        super.tick();
        if (!ClientLobbyData.ownRoomId().isBlank()) { RoomScreen.open(parent); return; }
        if (waiting && (++waitTicks >= 200 || (revision != ClientLobbyData.roomRevision() && ClientLobbyData.roomError()))) waiting = false;
        updateButton();
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderShell(graphics, values[1].isBlank() ? "公开房间" : "密码房间");
        for (int i = 0; i < labels.length; i++) {
            int y = labelYs[i]; String label = labels[i];
            paintBand(graphics, y, 10, () -> graphics.drawString(font, label, innerLeft, y, UiTheme.MUTED, false));
        }
        renderStatus(graphics, ClientLobbyData.roomMessage(), ClientLobbyData.roomError() ? UiTheme.ERROR : UiTheme.INFO);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
