package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientMapEditorData;
import cn.blockforge.generated.generatedmod.client.MapToolClientState;
import cn.blockforge.generated.generatedmod.client.ui.UiButton;
import cn.blockforge.generated.generatedmod.client.ui.UiScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import cn.blockforge.generated.generatedmod.map.MapDefinition;
import cn.blockforge.generated.generatedmod.map.MapEditorView;
import cn.blockforge.generated.generatedmod.map.MapRegion;
import cn.blockforge.generated.generatedmod.map.MapRegionAction;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.MapRegionPacket;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** 规划器的区域选择与编辑菜单。 */
public final class MapPlannerScreen extends UiScreen {
    private static final int MENU_WIDTH = 350;
    private List<MapRegion> regions;
    private int lastRevision = -1;
    private final List<RegionButton> buttons = new ArrayList<>();
    private final CameraType previousCameraType;
    private List<String> helpLines = List.of();
    private int helpY;

    public MapPlannerScreen(List<MapRegion> regions) {
        super(Component.literal("地图规划器"));
        this.regions = List.copyOf(regions == null ? List.of() : regions);
        Minecraft minecraft = Minecraft.getInstance();
        this.previousCameraType = minecraft.options.getCameraType();
    }

    @Override
    protected void init() {
        lastRevision = ClientMapEditorData.revision();
        buttons.clear();
        beginLayout(MENU_WIDTH, 0, BUTTON_HEIGHT * 2 + 8, true, true);
        int titleY = flowRow(18);
        flowWidget(uiButton("？", innerLeft + innerWidth - 24, titleY, 22, this::toggleHelp,
                "查看规划器的全部使用说明。", UiButton.Kind.SECONDARY), titleY);
        if (!helpLines.isEmpty()) {
            helpY = flowRow(helpLines.size() * 12 + 10);
        }
        int cameraY = flowRow(BUTTON_HEIGHT);
        CameraType[] cameras = {CameraType.FIRST_PERSON, CameraType.THIRD_PERSON_BACK, CameraType.THIRD_PERSON_FRONT};
        String[] labels = {"第一人称", "背后观察", "正面观察"};
        for (int i = 0; i < cameras.length; i++) {
            CameraType camera = cameras[i];
            UiButton button = uiButton(labels[i], columnX(i, 3, 4), cameraY, columnWidth(3, 4),
                    () -> { minecraft.options.setCameraType(camera); rebuildWidgets(); },
                    "切换到" + labels[i] + "，关闭菜单后保留此选择。", UiButton.Kind.SECONDARY);
            button.setSelected(minecraft.options.getCameraType() == camera);
            flowWidget(button, cameraY);
        }

        int controlWidth = innerWidth - 30;
        int helpX = innerLeft + innerWidth - 24;
        for (MapRegion region : regions) {
            int y = flowRow(BUTTON_HEIGHT);
            UiButton button = uiButton(region.displayName() + " · " + region.type().displayName(),
                    innerLeft, y, controlWidth, () -> edit(region),
                    "选中并在编辑器中打开这个区域。", UiButton.Kind.SECONDARY);
            flowWidget(button, y);
            buttons.add(new RegionButton(button, region));
            flowWidget(regionHelpButton(helpX, y, region), y);
        }

        MapEditorView view = ClientMapEditorData.view();
        if (!view.bounds().present()) addFoundationButton("创建地图边界", MapRegion.Type.BOUNDS);
        if (!view.resetRegion().present()) addFoundationButton("创建重置区域", MapRegion.Type.RESET);

        int createY = flowRow(BUTTON_HEIGHT);
        flowWidget(uiButton("新增自定义区域", innerLeft, createY, controlWidth, this::createRegion,
                "创建一个新的可编辑区域。", UiButton.Kind.PRIMARY), createY);
        flowWidget(helpButton(helpX, createY,
                "新增区域从脚下一个方块开始，并成为画笔目标。",
                "创建后可用画笔在区域内重新圈定两个端点。",
                "区域名称、类型、显示与生效逻辑可在编辑页调整。"), createY);
        int spawnY = flowRow(BUTTON_HEIGHT);
        flowWidget(new cn.blockforge.generated.generatedmod.client.ui.UiCycleButton<>(innerLeft, spawnY,
                innerWidth, BUTTON_HEIGHT,
                List.of(cn.blockforge.generated.generatedmod.map.MapTool.SPAWN_A,
                        cn.blockforge.generated.generatedmod.map.MapTool.SPAWN_B,
                        cn.blockforge.generated.generatedmod.map.MapTool.SPAWN_C,
                        cn.blockforge.generated.generatedmod.map.MapTool.SPAWN_D,
                        cn.blockforge.generated.generatedmod.map.MapTool.SPECTATOR),
                cn.blockforge.generated.generatedmod.map.MapTool.SPAWN_A,
                tool -> "编辑出生点：" + tool.displayName(), tool -> {
                    FpsTdmNetwork.sendToServer(new cn.blockforge.generated.generatedmod.network.packet.MapEditorActionPacket(
                            cn.blockforge.generated.generatedmod.map.MapEditorAction.SELECT_TOOL, tool.id()));
                    onClose();
                }, "选择队伍后使用画笔添加或移除出生点。", UiButton.Kind.SECONDARY), spawnY);

        footerButton("地图工作台", 0, 2, 0, this::openWorkbench,
                "打开地图工作台。", UiButton.Kind.SECONDARY);
        footerButton("继续编辑", 1, 2, 0, this::onClose, "返回游戏，保留当前视角。", UiButton.Kind.PRIMARY);
        footerButton("恢复进入前视角", 0, 1, 1, this::exitEditMode,
                "恢复打开本菜单之前的视角，并返回游戏。", UiButton.Kind.SECONDARY);
    }

    private void addFoundationButton(String label, MapRegion.Type type) {
        int y = flowRow(BUTTON_HEIGHT);
        flowWidget(uiButton(label, innerLeft, y, innerWidth, () -> createFoundation(type),
                "在脚下创建初始区域；随后用画笔选择两个角。", UiButton.Kind.PRIMARY), y);
    }

    private void createFoundation(MapRegion.Type type) {
        if (minecraft == null || minecraft.player == null) return;
        BlockPos point = minecraft.player.blockPosition();
        String id = type == MapRegion.Type.BOUNDS ? "bounds" : "reset";
        String name = type == MapRegion.Type.BOUNDS ? "地图边界" : "重置区域";
        MapRegion region = MapRegion.builtIn(id, name, type,
                new MapDefinition.Region(point, point), type.defaultColor());
        FpsTdmNetwork.sendToServer(new MapRegionPacket(MapRegionAction.CREATE, region, 0));
        onClose();
    }

    private UiButton helpButton(int x, int y, String... lines) {
        return uiButton("？", x, y, 24, () -> showHelp(lines),
                "查看这一项的详细使用说明。", UiButton.Kind.SECONDARY);
    }

    private UiButton regionHelpButton(int x, int y, MapRegion region) {
        return helpButton(x, y,
                "区域：" + region.displayName(),
                "类型：" + region.type().displayName(),
                "范围：" + region.region(),
                "点击名称进入编辑；悬停名称会让边框发光。");
    }

    private void edit(MapRegion region) {
        if (minecraft != null) {
            FpsTdmNetwork.sendToServer(new MapRegionPacket(MapRegionAction.SELECT, region, 0));
            minecraft.setScreen(new MapRegionEditScreen(region));
        }
    }

    private void createRegion() {
        MapEditorView view = ClientMapEditorData.view();
        if (minecraft == null || minecraft.player == null) return;
        MapDefinition.Region bounds = new MapDefinition.Region(minecraft.player.blockPosition(), minecraft.player.blockPosition());
        MapRegion region = MapRegion.custom("", "自定义区域", MapRegion.Type.CUSTOM, bounds);
        FpsTdmNetwork.sendToServer(new MapRegionPacket(MapRegionAction.CREATE, region, 0));
        onClose();
    }

    private void toggleHelp() {
        showHelp(
                "右键打开菜单，左键直接选定准星命中的区域。",
                "站在区域 A 内指向区域 B 时，会优先选择 B。",
                "多个区域重叠时，菜单会列出全部可编辑目标。",
                "悬停菜单中的区域名称，对应边框会呼吸发光。",
                "打开菜单不改变视角。可直接选择第一人称、背后或正面。",
                "继续编辑保留选择；恢复进入前视角会还原并关闭。");
    }

    private void showHelp(String... lines) {
        List<String> next = java.util.Arrays.stream(lines)
                .flatMap(line -> font.getSplitter().splitLines(line, innerWidth - 16,
                        net.minecraft.network.chat.Style.EMPTY).stream())
                .map(net.minecraft.network.chat.FormattedText::getString).toList();
        helpLines = helpLines.equals(next) ? List.of() : next;
        rebuildWidgets();
    }

    private void exitEditMode() {
        Minecraft.getInstance().options.setCameraType(previousCameraType);
        onClose();
    }

    private void openWorkbench() {
        if (minecraft != null) {
            MapLibraryScreen.open(this);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (lastRevision != ClientMapEditorData.revision()) {
            regions = ClientMapEditorData.view().regions();
            rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderShell(graphics, "右键打开 · 左键选定");
        for (RegionButton entry : buttons) {
            if (entry.button().isHoveredOrFocused()) {
                MapToolClientState.setHoveredRegion(entry.region().id());
            }
        }
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

    private record RegionButton(UiButton button, MapRegion region) {
    }
}
