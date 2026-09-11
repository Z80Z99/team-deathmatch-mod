package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientMapEditorData;
import cn.blockforge.generated.generatedmod.client.ui.UiButton;
import cn.blockforge.generated.generatedmod.client.ui.UiCycleButton;
import cn.blockforge.generated.generatedmod.client.ui.UiScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import cn.blockforge.generated.generatedmod.map.MapBrushMode;
import cn.blockforge.generated.generatedmod.map.MapEditorAction;
import cn.blockforge.generated.generatedmod.map.MapEditorView;
import cn.blockforge.generated.generatedmod.map.MapTool;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.MapEditorActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/** 地图画笔菜单：集中显示目标、模式、距离和下一步操作。 */
public final class MapBrushScreen extends UiScreen {
    private static final int MENU_WIDTH = 340;
    private List<String> helpLines = List.of();
    private int helpY;
    private int lastRevision = -1;
    private int ticks;
    private int contextY;
    private int nextY;
    private UiCycleButton<MapBrushMode> modeControl;
    private int rangeValue;
    private int pendingRangeRequest;
    private UiButton targetControl;

    public MapBrushScreen() {
        super(Component.literal("地图画笔"));
    }

    @Override
    protected void init() {
        beginLayout(MENU_WIDTH, 0, BUTTON_HEIGHT, true, true);
        contextY = flowRow(64);
        nextY = flowRow(22);

        int entryY = flowRow(BUTTON_HEIGHT);
        flowWidget(uiButton("交互说明", columnX(0, 2, 4), entryY, columnWidth(2, 4),
                this::toggleHelp, "查看画笔手持与菜单交互说明。", UiButton.Kind.SECONDARY), entryY);
        flowWidget(uiButton("完整教程", columnX(1, 2, 4), entryY, columnWidth(2, 4),
                () -> MapToolsTutorialScreen.open(this), "打开地图工具完整教程。", UiButton.Kind.SECONDARY), entryY);
        if (!helpLines.isEmpty()) {
            helpY = flowRow(helpLines.size() * 12 + 10);
        }

        int controlWidth = innerWidth - 30;
        int helpX = innerLeft + innerWidth - 24;
        int targetY = flowRow(BUTTON_HEIGHT);
        targetControl = flowWidget(uiButton(targetButtonLabel(), innerLeft, targetY, innerWidth,
                MapPlannerScreen::open,
                "在规划器中创建或选择区域、出生点。", UiButton.Kind.PRIMARY), targetY);

        int modeY = flowRow(BUTTON_HEIGHT);
        modeControl = flowWidget(new UiCycleButton<>(innerLeft, modeY, controlWidth, BUTTON_HEIGHT,
                List.of(MapBrushMode.REGION, MapBrushMode.BLOCK), ClientMapEditorData.view().brushMode(),
                mode -> mode == MapBrushMode.REGION ? "操作：两角框选" : "操作：逐格增删",
                this::setMode, "切换区域模式和方块模式。", UiButton.Kind.SECONDARY), modeY);
        flowWidget(helpButton(helpX, modeY,
                "两角框选：左键选择一个角，右键选择对角。",
                "逐格增删：左键移除一格区域，右键在相邻位置添加一格。",
                "支持内部挖空；只修改区域，不挖掘真实地形。",
                "切换模式会清空尚未配对的端点。",
                "出生点工具不受此开关影响。"), modeY);

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
                "左右键都能在空气位置取点，默认距离前方 2 格。",
                "如果中间有方块，会优先停在方块位置。",
                "逐格模式优先选择区域表面；红色为移除过渡，青色为添加过渡。",
                "远距离选择可把距离调大；随时可再调回 2 格。"), rangeY);

        footerButton("地图工作台", 0, 2, 0, this::openWorkbench,
                "打开地图工作台。", UiButton.Kind.SECONDARY);
        footerButton("关闭并使用画笔", 1, 2, 0, this::onClose,
                "返回游戏；设置会自动保存。", UiButton.Kind.PRIMARY);
        updateControls();
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
        showHelp("1. 在规划器选择区域或出生点工具。",
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
        MapLibraryScreen.open(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (++ticks % 20 == 1) {
            FpsTdmNetwork.sendToServer(new MapEditorActionPacket(MapEditorAction.POLL, ""));
        }
        if (lastRevision != ClientMapEditorData.revision()) {
            lastRevision = ClientMapEditorData.revision();
            updateControls();
        }
    }

    private void updateControls() {
        MapEditorView view = ClientMapEditorData.view();
        modeControl.setValue(view.brushMode());
        if (pendingRangeRequest == 0 || view.responseRequestId() >= pendingRangeRequest) {
            rangeValue = view.brushRange();
            pendingRangeRequest = 0;
        }
        targetControl.setMessage(Component.literal(targetButtonLabel()));
        modeControl.active = view.selectedTool().kind() != MapTool.Kind.POINT
                && !view.selectedRegionId().isBlank();
    }

    private String targetButtonLabel() {
        MapEditorView view = ClientMapEditorData.view();
        if (!view.selectedRegionId().isBlank()) {
            String regionName = view.regions().stream()
                    .filter(region -> region.id().equals(view.selectedRegionId()))
                    .findFirst().map(region -> region.displayName() + " · " + region.type().displayName())
                    .orElse(view.selectedRegionId());
            return "更换目标：" + regionName;
        }
        if (view.selectedTool().kind() == MapTool.Kind.POINT) {
            return "更换目标：" + view.selectedTool().displayName();
        }
        return "在规划器中选择编辑目标";
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderShell(graphics, "设置自动保存 · 蹲下右键打开");
        MapEditorView view = ClientMapEditorData.view();
        renderStatus(graphics, pendingRangeRequest != 0 ? "正在保存距离…" : view.message(),
                view.error() ? UiTheme.ERROR : UiTheme.TEXT);
        paintBand(graphics, contextY, 64, () -> renderContext(graphics, view));
        paintBand(graphics, nextY, 22, () -> badge(graphics, nextHint(view), innerLeft, nextY, innerWidth, nextColor(view)));
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

    private void renderContext(GuiGraphics graphics, MapEditorView view) {
        divider(graphics, innerLeft, contextY + 63, innerWidth);
        graphics.drawString(font, fit("地图  " + (view.displayName().isBlank() ? "—" : view.displayName()),
                innerWidth - 16), innerLeft + 8, contextY, UiTheme.TEXT, false);
        graphics.drawString(font, fit("目标  " + selectedTarget(view), innerWidth - 16),
                innerLeft + 8, contextY + 14, UiTheme.ACCENT, false);
        graphics.drawString(font, fit("模式  " + (view.brushMode() == MapBrushMode.REGION
                ? "两角框选" : "逐格增删"), innerWidth - 16), innerLeft + 8, contextY + 28, UiTheme.MUTED, false);
        graphics.drawString(font, fit("空气距离  " + view.brushRange() + " 格 · 被选位置会显示玻璃指示",
                innerWidth - 16), innerLeft + 8, contextY + 42, UiTheme.MUTED, false);
    }

    private String selectedTarget(MapEditorView view) {
        if (!view.selectedRegionId().isBlank()) {
            return view.regions().stream()
                    .filter(region -> region.id().equals(view.selectedRegionId()))
                    .findFirst().map(region -> region.displayName() + " · " + region.type().displayName())
                    .orElse(view.selectedRegionId());
        }
        if (view.selectedTool().kind() == MapTool.Kind.POINT) {
            return view.selectedTool().displayName();
        }
        return "未选择";
    }

    private String nextHint(MapEditorView view) {
        if (!view.hasTarget()) return "下一步：返回地图工作台选择或新建地图。";
        if (view.selectedTool().kind() == MapTool.Kind.POINT) return "下一步：关闭菜单，右键添加出生点、左键移除。";
        if (view.selectedRegionId().isBlank()) return "下一步：先在规划器选择一个区域。";
        if (view.brushMode() == MapBrushMode.REGION) return "下一步：左键选一个角，右键选对角。";
        return "下一步：左键移除格子，右键添加相邻格子。";
    }

    private int nextColor(MapEditorView view) {
        return !view.hasTarget() || (view.selectedTool().kind() != MapTool.Kind.POINT
                && view.selectedRegionId().isBlank()) ? UiTheme.WARNING : UiTheme.INFO;
    }

    @Override
    public void onClose() {
        net.minecraft.client.Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
