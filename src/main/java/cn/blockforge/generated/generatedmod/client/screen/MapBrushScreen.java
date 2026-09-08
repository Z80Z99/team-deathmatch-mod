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

    public MapBrushScreen() {
        super(Component.literal("地图画笔"));
    }

    @Override
    protected void init() {
        beginLayout(MENU_WIDTH, 260, BUTTON_HEIGHT * 2 + 8, true, true);
        int titleY = flowRow(18);
        flowWidget(uiButton("？", innerLeft + innerWidth - 24, titleY, 22, this::toggleHelp,
                "查看画笔的全部使用说明。", UiButton.Kind.SECONDARY), titleY);
        if (!helpLines.isEmpty()) {
            helpY = flowRow(helpLines.size() * 12 + 10);
        }

        int controlWidth = innerWidth - 30;
        int helpX = innerLeft + innerWidth - 24;
        int modeY = flowRow(BUTTON_HEIGHT);
        flowWidget(new UiCycleButton<>(innerLeft, modeY, controlWidth, BUTTON_HEIGHT,
                List.of(MapBrushMode.REGION, MapBrushMode.BLOCK), ClientMapEditorData.view().brushMode(),
                MapBrushMode::displayName, this::setMode,
                "切换区域模式和方块模式。", UiButton.Kind.SECONDARY), modeY);
        flowWidget(helpButton(helpX, modeY,
                "区域模式：左键记录端点 A，右键记录端点 B。",
                "方块模式：左键排除方块，右键加入方块。",
                "切换模式会清空尚未配对的端点。"), modeY);

        int rangeY = flowRow(BUTTON_HEIGHT);
        flowWidget(new UiCycleButton<>(innerLeft, rangeY, controlWidth, BUTTON_HEIGHT,
                List.of(1, 2, 3, 4, 6, 8, 12, 16, 24, 32, 48, 64),
                ClientMapEditorData.view().brushRange(), value -> value + " 格", this::setRange,
                "右键选择空气时距离玩家前方的格数。", UiButton.Kind.SECONDARY), rangeY);
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
        FpsTdmNetwork.sendToServer(new MapEditorActionPacket(MapEditorAction.SET_BRUSH_RANGE,
                Integer.toString(range)));
    }

    private void toggleHelp() {
        showHelp(","+                "手持画笔时，屏幕左下和右下会显示操作与目标信息。",
                "蹲下右键打开本菜单；普通右键记录端点 B 或加入方块。",
                "左键记录端点 A、排除方块或移除出生点，不会破坏世界方块。",
                "区域和方块目标会有半透明玻璃样指示框。");
    }

    private void showHelp(String... lines) {
        List<String> next = List.of(lines);
        helpLines = helpLines.equals(next) ? List.of() : next;
        rebuildWidgets();
    }

    private void openWorkbench() {
        if (minecraft != null) {
            MapLibraryScreen.open(this);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderShell(graphics, "蹲下右键打开");
        if (!helpLines.isEmpty()) {
            int y = helpY + 4;
            for (String line : helpLines) {
                graphics.drawString(font, fit(line, innerWidth - 16),
                        innerLeft + 8, y, UiTheme.TEXT, false);
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
