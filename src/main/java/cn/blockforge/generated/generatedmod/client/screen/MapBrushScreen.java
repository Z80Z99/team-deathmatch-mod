package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientMapEditorData;
import cn.blockforge.generated.generatedmod.client.ui.UiButton;
import cn.blockforge.generated.generatedmod.client.ui.UiCycleButton;
import cn.blockforge.generated.generatedmod.client.ui.UiScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import cn.blockforge.generated.generatedmod.map.MapBrushMode;
import cn.blockforge.generated.generatedmod.map.MapEditorAction;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.MapEditorActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/** 地图画笔的设置菜单。 */
public final class MapBrushScreen extends UiScreen {
    private static final int MENU_WIDTH = 320;
    private List<String> helpLines = List.of();
    private int helpY;
    private int lastRevision = -1;
    private int ticks;
    private UiCycleButton<MapBrushMode> modeControl;
    private int rangeValue;
    private int pendingRangeRequest;
    private UiCycleButton<cn.blockforge.generated.generatedmod.map.MapTool> targetControl;

    public MapBrushScreen() {
        super(Component.literal("地图画笔"));
    }

    @Override
    protected void init() {
        beginLayout(MENU_WIDTH, 260, BUTTON_HEIGHT, true, true);
        int titleY = flowRow(18);
        flowWidget(uiButton("？", innerLeft + innerWidth - 24, titleY, 22, this::toggleHelp,
                "查看画笔的全部使用说明。", UiButton.Kind.SECONDARY), titleY);
        if (!helpLines.isEmpty()) {
            helpY = flowRow(helpLines.size() * 12 + 10);
        }

        int controlWidth = innerWidth - 30;
        int helpX = innerLeft + innerWidth - 24;
        int targetY = flowRow(BUTTON_HEIGHT);
        targetControl = flowWidget(new UiCycleButton<>(innerLeft, targetY, innerWidth, BUTTON_HEIGHT,
                java.util.Arrays.asList(cn.blockforge.generated.generatedmod.map.MapTool.values()),
                ClientMapEditorData.view().selectedTool(), tool -> "编辑目标：" + tool.displayName(),
                tool -> FpsTdmNetwork.sendToServer(new MapEditorActionPacket(MapEditorAction.SELECT_TOOL, tool.id())),
                "自定义区域请先在规划器中选中。出生点直接使用右键添加、左键移除。",
                UiButton.Kind.SECONDARY), targetY);
        int modeY = flowRow(BUTTON_HEIGHT);
        modeControl = flowWidget(new UiCycleButton<>(innerLeft, modeY, controlWidth, BUTTON_HEIGHT,
                List.of(MapBrushMode.REGION, MapBrushMode.BLOCK), ClientMapEditorData.view().brushMode(),
                mode -> mode == MapBrushMode.REGION ? "操作：两角框选" : "操作：调整边缘", this::setMode,
                "切换区域模式和方块模式。", UiButton.Kind.SECONDARY), modeY);
        flowWidget(helpButton(helpX, modeY,
                "两角框选：左键选择一个角，右键选择对角。",
                "调整边缘：右键扩展区域，左键收缩边缘。",
                "区域始终为长方体，不能单独挖掉内部方块。",
                "切换模式会清空尚未配对的端点。"), modeY);

        int rangeY = flowRow(BUTTON_HEIGHT + 12) + 12;
        rangeValue = ClientMapEditorData.view().brushRange();
        flowWidget(uiButton("-", innerLeft, rangeY, 22,
                () -> setRange(rangeValue - 1), "距离减少 1 格", UiButton.Kind.SECONDARY), rangeY);
        flowWidget(new cn.blockforge.generated.generatedmod.client.ui.UiSlider(font,
                innerLeft + 26, rangeY, controlWidth - 52, BUTTON_HEIGHT, 1, 64,
                () -> rangeValue, this::setRange, "空气距离", " 格"), rangeY);
        flowWidget(uiButton("+", innerLeft + controlWidth - 22, rangeY, 22,
                () -> setRange(rangeValue + 1), "距离增加 1 格", UiButton.Kind.SECONDARY), rangeY);
        flowWidget(helpButton(helpX, rangeY,
                "右键可在空气位置取点，默认距离前方 2 格。",
                "如果中间有方块，会优先停在方块位置。",
                "左键只作用于实际方块；空气不会触发左键操作。"), rangeY);

        footerButton("地图工作台", 0, 2, 0, this::openWorkbench,
                "打开地图工作台。", UiButton.Kind.SECONDARY);
        footerButton("关闭", 1, 2, 0, this::onClose, "返回游戏。", UiButton.Kind.SECONDARY);
    }

    private UiButton helpButton(int x, int y, String... lines) {
        return uiButton("？", x, y, 24, () -> showHelp(lines),
                "查看这一项的详细使用说明。", UiButton.Kind.SECONDARY);
    }

    private void setMode(MapBrushMode mode) {
        FpsTdmNetwork.sendToServer(new MapEditorActionPacket(MapEditorAction.SET_BRUSH_MODE, mode.id()));
    }

    private void setRange(int range) {
        rangeValue = Math.max(1, Math.min(64, range));
        MapEditorActionPacket packet = new MapEditorActionPacket(MapEditorAction.SET_BRUSH_RANGE,
                Integer.toString(rangeValue));
        pendingRangeRequest = packet.requestId();
        FpsTdmNetwork.sendToServer(packet);
    }

    private void toggleHelp() {
        showHelp("1. 在地图工作台选择地图，再选择编辑目标。",
                "2. 关闭菜单，拿着画笔左键点选一个角。",
                "3. 右键点选对角，圈出的长方体自动保存。",
                "出生点：右键添加，左键移除，无需选择模式。",
                "设置选择后自动记住；蹲下右键可再次调整。");
    }

    private void showHelp(String... lines) {
        List<String> next = java.util.Arrays.stream(lines)
                .flatMap(line -> font.getSplitter().splitLines(line, innerWidth - 16,
                        net.minecraft.network.chat.Style.EMPTY).stream())
                .map(net.minecraft.network.chat.FormattedText::getString).toList();
        helpLines = helpLines.equals(next) ? List.of() : next;
        rebuildWidgets();
    }

    private void openWorkbench() {
        if (minecraft != null) {
            MapLibraryScreen.open(this);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (++ticks % 20 == 1) {
            FpsTdmNetwork.sendToServer(new MapEditorActionPacket(MapEditorAction.POLL, ""));
        }
        if (lastRevision != ClientMapEditorData.revision()) {
            lastRevision = ClientMapEditorData.revision();
            modeControl.setValue(ClientMapEditorData.view().brushMode());
            if (pendingRangeRequest == 0 || ClientMapEditorData.view().responseRequestId() >= pendingRangeRequest) {
                rangeValue = ClientMapEditorData.view().brushRange();
                pendingRangeRequest = 0;
            }
            targetControl.setValue(ClientMapEditorData.view().selectedTool());
            modeControl.active = ClientMapEditorData.view().selectedTool().kind()
                    != cn.blockforge.generated.generatedmod.map.MapTool.Kind.POINT;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderShell(graphics, "设置自动保存 · 蹲下右键打开");
        renderStatus(graphics, pendingRangeRequest != 0 ? "正在保存距离…" : ClientMapEditorData.view().message(),
                ClientMapEditorData.view().error() ? UiTheme.ERROR : UiTheme.TEXT);
        if (!helpLines.isEmpty()) {
            int y = helpY + 4;
            for (String line : helpLines) {
                if (bandFits(y, 12)) graphics.drawString(font, line,
                        innerLeft + 8, bandScreenY(y), UiTheme.TEXT, false);
                y += 12;
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

}
