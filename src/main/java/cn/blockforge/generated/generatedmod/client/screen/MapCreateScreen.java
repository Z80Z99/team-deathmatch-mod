package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientMapEditorData;
import cn.blockforge.generated.generatedmod.client.ui.UiButton;
import cn.blockforge.generated.generatedmod.client.ui.UiEditBox;
import cn.blockforge.generated.generatedmod.client.ui.UiScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import cn.blockforge.generated.generatedmod.map.MapEditorAction;
import cn.blockforge.generated.generatedmod.map.MapEditorView;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.MapEditorActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 创建属于自己的地图：只需显示名称，内部 id 由服务器按“玩家名-名称”自动生成。 */
public final class MapCreateScreen extends UiScreen {
    private static final int FLOW_HEIGHT = 64;

    private final Screen parent;
    private UiEditBox nameBox;
    private UiButton createButton;
    private UiButton backButton;
    private int nameLabelY;
    private int nameBoxY;
    private int flowY;
    private int observedRevision = ClientMapEditorData.revision();
    private boolean waiting;
    private boolean created;
    private int pendingRequestId;
    private int waitTicks;
    private String status = "";
    private String nameDraft = "";
    private int statusColor = UiTheme.INFO;

    public MapCreateScreen(Screen parent) {
        super(Component.literal("创建我的地图"));
        this.parent = parent;
        updateDefaultStatus();
    }

    public static void open(Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.getConnection() != null) {
            minecraft.setScreen(new MapCreateScreen(parent));
        }
    }

    @Override
    protected void init() {
        beginLayout(460, 260, BUTTON_HEIGHT);
        nameLabelY = flowRow(10);
        nameBoxY = flowRow(BUTTON_HEIGHT);
        flowRow(10);
        flowY = flowRow(FLOW_HEIGHT);

        nameBox = new UiEditBox(font, innerLeft, nameBoxY, innerWidth, BUTTON_HEIGHT,
                Component.literal("地图显示名称"));
        nameBox.setMaxLength(48);
        nameBox.setValue(nameDraft);
        nameBox.setResponder(value -> { nameDraft = value; updateControls(); });
        nameBox.setTooltip(Tooltip.create(Component.literal(
                "玩家看到的名字，可用中文；内部 ID 由服务器自动生成（玩家名-名称），保证不与别人的地图冲突。")));
        flowWidget(nameBox, nameBoxY);

        createButton = footerButton("创建地图", 0, 2, 0, this::createMap,
                "以当前维度和当前位置生成小型初始区域，地图归你所有。", UiButton.Kind.PRIMARY);
        backButton = footerButton("返回地图工作台", 1, 2, 0, this::onClose,
                "返回“我的地图”列表。", UiButton.Kind.DANGER);
        updateControls();
    }

    private void createMap() {
        if (waiting || created || minecraft == null || minecraft.getConnection() == null) {
            return;
        }
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) {
            setStatus("请填写地图显示名称。", UiTheme.ERROR);
            return;
        }
        if (name.indexOf('|') >= 0 || name.chars().anyMatch(Character::isISOControl)) {
            setStatus("地图名称不能包含“|”或控制字符。", UiTheme.ERROR);
            return;
        }
        waiting = true;
        waitTicks = 0;
        observedRevision = ClientMapEditorData.revision();
        setStatus("正在由服务器创建地图，请稍候……", UiTheme.INFO);
        updateControls();
        MapEditorActionPacket packet = new MapEditorActionPacket(MapEditorAction.CREATE_MAP, "", name);
        pendingRequestId = packet.requestId();
        FpsTdmNetwork.sendToServer(packet);
    }

    @Override
    public void tick() {
        super.tick();
        if (nameBox != null) {
            nameBox.tick();
        }
        int revision = ClientMapEditorData.revision();
        if (waiting) waitTicks++;
        if (revision != observedRevision) {
            observedRevision = revision;
            if (waiting) {
                MapEditorView view = ClientMapEditorData.view();
                if (view.responseRequestId() == pendingRequestId) {
                    waiting = false;
                    waitTicks = 0;
                    created = !view.error() && view.hasTarget();
                    setStatus(view.message().isBlank() ? "服务器未返回创建结果。" : view.message(),
                            view.error() ? UiTheme.ERROR : created ? UiTheme.SUCCESS : UiTheme.INFO);
                }
            }
        }
        if (waiting && waitTicks >= 200) {
            waiting = false;
            waitTicks = 0;
            setStatus("地图创建请求超时，请检查网络或稍后重试。", UiTheme.ERROR);
        }
        updateControls();
    }

    private void updateControls() {
        MapEditorView view = ClientMapEditorData.view();
        boolean editable = !view.locked() && !waiting && !created;
        boolean validName = nameBox != null && !nameBox.getValue().trim().isEmpty();
        if (nameBox != null) {
            nameBox.setEditable(editable);
        }
        if (createButton != null) {
            createButton.active = editable && validName;
            createButton.setMessage(Component.literal(created ? "地图已创建"
                    : waiting ? "正在创建……" : "创建地图"));
            createButton.setSelected(waiting || created);
        }
        if (backButton != null) {
            backButton.setMessage(Component.literal(created ? "去编辑这张地图" : "返回地图工作台"));
            backButton.setSelected(created);
        }
    }

    private void updateDefaultStatus() {
        MapEditorView view = ClientMapEditorData.view();
        if (view.locked()) {
            setStatus("比赛或地图快照任务进行中，暂时不能创建地图。", UiTheme.WARNING);
        } else {
            setStatus("填写名称后创建；地图归你所有，只有你能编辑。", UiTheme.INFO);
        }
    }

    private void setStatus(String message, int color) {
        status = message == null ? "" : message;
        statusColor = color;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderShell(graphics, created ? "创建完成" : "新地图");
        renderStatus(graphics, status, statusColor);

        final int label = nameLabelY;
        paintBand(graphics, label, 10,
                () -> graphics.drawString(font, fit("地图显示名称", innerWidth), innerLeft, label,
                        nameBox != null && !nameBox.getValue().trim().isEmpty()
                                ? UiTheme.SUCCESS : UiTheme.MUTED, false));
        final int flow = flowY;
        paintBand(graphics, flow, FLOW_HEIGHT, () -> renderCreationFlow(graphics, flow));
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderCreationFlow(GuiGraphics graphics, int y) {
        divider(graphics, innerLeft, y, innerWidth);
        String name = nameBox == null ? "" : nameBox.getValue().trim();
        int markerColor = created ? UiTheme.SUCCESS : waiting ? UiTheme.WARNING : UiTheme.ACCENT;
        graphics.fill(innerLeft + 8, y + 6, innerLeft + 11, y + FLOW_HEIGHT - 6, markerColor);
        String first = name.isBlank() ? "名称  未填写" : "名称  " + name;
        graphics.drawString(font, fit(first, innerWidth - 28), innerLeft + 18, y + 7,
                name.isBlank() ? UiTheme.SUBTLE : UiTheme.TEXT, false);
        graphics.drawString(font, fit("维度  " + (minecraft.player == null ? "未知" : minecraft.player.level().dimension().location()), innerWidth - 28),
                innerLeft + 18, y + 21, UiTheme.MUTED, false);
        graphics.drawString(font, fit("位置  " + (minecraft.player == null ? "未知" : minecraft.player.blockPosition().toShortString()), innerWidth - 28),
                innerLeft + 18, y + 35, UiTheme.MUTED, false);
        String fourth = created ? "状态  已创建"
                : waiting ? "状态  正在保存" : "拥有者  " + (minecraft.player == null ? "未知" : minecraft.player.getGameProfile().getName());
        graphics.drawString(font, fit(fourth, innerWidth - 28), innerLeft + 18, y + 49,
                created ? UiTheme.SUCCESS : waiting ? UiTheme.WARNING : UiTheme.SUBTLE, false);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            if (created) MapPlannerScreen.open(); else minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
