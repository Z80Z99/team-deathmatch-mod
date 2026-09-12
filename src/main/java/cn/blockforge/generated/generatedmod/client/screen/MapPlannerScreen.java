package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientMapEditorData;
import cn.blockforge.generated.generatedmod.client.MapToolClientState;
import cn.blockforge.generated.generatedmod.client.MapRegionPicker;
import cn.blockforge.generated.generatedmod.client.ui.UiButton;
import cn.blockforge.generated.generatedmod.client.ui.UiCycleButton;
import cn.blockforge.generated.generatedmod.client.ui.UiScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import cn.blockforge.generated.generatedmod.map.MapEditorAction;
import cn.blockforge.generated.generatedmod.map.MapDefinition;
import cn.blockforge.generated.generatedmod.map.MapEditorView;
import cn.blockforge.generated.generatedmod.map.MapRegion;
import cn.blockforge.generated.generatedmod.map.MapRegionAction;
import cn.blockforge.generated.generatedmod.map.MapTool;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.MapEditorActionPacket;
import cn.blockforge.generated.generatedmod.network.packet.MapRegionPacket;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 规划器菜单：按用途分组展示区域，并持续显示当前目标与下一步。 */
public final class MapPlannerScreen extends UiScreen {
    private static final int MENU_WIDTH = 360;
    private List<MapRegion> regions;
    private final List<String> priorityRegionIds;
    private int lastContentRevision = -1;
    private final List<RegionButton> buttons = new ArrayList<>();
    private final List<SectionLabel> sections = new ArrayList<>();
    private final List<EmptyLabel> emptyLabels = new ArrayList<>();
    private final List<UiButton> spawnActionButtons = new ArrayList<>();
    private final CameraType previousCameraType;
    private List<String> helpLines = List.of();
    private UiButton spawnAddButton;
    private UiButton spawnClearButton;
    private UiCycleButton<MapTool> spawnToolButton;
    private MapTool spawnTool;
    private int helpY;
    private int contextY;
    private int nextY;
    private int ticks;

    public MapPlannerScreen(List<MapRegion> regions) {
        this(regions, List.of());
    }

    public MapPlannerScreen(List<MapRegion> regions, List<MapRegion> priorityRegions) {
        super(Component.literal("地图规划器"));
        this.priorityRegionIds = List.copyOf((priorityRegions == null ? List.<MapRegion>of() : priorityRegions)
                .stream().map(MapRegion::id).toList());
        this.regions = orderRegions(regions, priorityRegionIds);
        this.previousCameraType = Minecraft.getInstance().options.getCameraType();
        MapTool selectedTool = ClientMapEditorData.view().selectedTool();
        this.spawnTool = selectedTool.kind() == MapTool.Kind.POINT ? selectedTool : MapTool.SPAWN_A;
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            return;
        }
        MapEditorView view = ClientMapEditorData.view();
        List<MapRegion> hits = MapRegionPicker.pick(minecraft, view, 128.0D);
        MapPlannerScreen screen = new MapPlannerScreen(view.regions(), hits);
        minecraft.setScreen(screen);
        screen.request(MapEditorAction.REQUEST, "");
    }

    @Override
    protected void init() {
        lastContentRevision = ClientMapEditorData.contentRevision();
        buttons.clear();
        sections.clear();
        emptyLabels.clear();
        spawnActionButtons.clear();
        beginLayout(MENU_WIDTH, 0, BUTTON_HEIGHT * 2 + 8, true, true);
        contextY = flowRow(58);
        nextY = flowRow(22);

        int entryY = flowRow(BUTTON_HEIGHT);
        flowWidget(uiButton("交互说明", columnX(0, 3, 4), entryY, columnWidth(3, 4),
                this::toggleHelp, "查看规划器手持与菜单交互说明。", UiButton.Kind.SECONDARY), entryY);
        flowWidget(uiButton("完整教程", columnX(1, 3, 4), entryY, columnWidth(3, 4),
                () -> MapToolsTutorialScreen.open(this), "打开地图工具完整教程。", UiButton.Kind.SECONDARY), entryY);
        flowWidget(uiButton("刷新状态", columnX(2, 3, 4), entryY, columnWidth(3, 4),
                () -> request(MapEditorAction.REQUEST, ""),
                "重新读取服务器编辑状态；如草稿已失效，这会确认丢弃旧草稿。", UiButton.Kind.SECONDARY), entryY);
        if (!helpLines.isEmpty()) {
            helpY = flowRow(helpLines.size() * 12 + 10);
        }

        addSection("观察视角", "切换后立即生效，关闭菜单也会保留当前选择。",
                "第一人称适合精确指向；背后/正面观察适合检查区域轮廓。",
                "“恢复进入前视角”才会还原打开菜单前的设置。");
        int cameraY = flowRow(BUTTON_HEIGHT);
        addCameraButtons(cameraY);

        addSection("基础区域", "地图边界限制活动范围；重置区域决定赛后恢复哪些方块。",
                "两者都是必需区域，缺失时地图不能完整保存。",
                "点击区域进入属性页；悬停名称会让对应边框发光。");
        addRegionGroup(MapRegion.Type.BOUNDS, MapRegion.Type.RESET);
        addMissingFoundationButtons();

        addSection("玩法区域", "爆破区、占领区、热点区和目标区服务不同模式。",
                "每个区域都可以设置名称、颜色、显示方式、出现逻辑和优先级。",
                "重叠区域会按优先级和出现逻辑决定实际显示与生效。");
        addRegionGroup(MapRegion.Type.BOMB, MapRegion.Type.CAPTURE,
                MapRegion.Type.HOTSPOT, MapRegion.Type.OBJECTIVE);

        addSection("自定义与其他", "自定义区域可当装饰、禁区、路线或任务提示使用。",
                "其他区域用于存放暂时不参与判定的范围。",
                "属性页可随时调整显示与生效逻辑。");
        addRegionGroup(MapRegion.Type.CUSTOM, MapRegion.Type.OTHER);

        int createY = flowRow(BUTTON_HEIGHT);
        flowWidget(uiButton("新增自定义区域", innerLeft, createY, innerWidth - 30, this::createRegion,
                "创建一个新的可编辑区域。", UiButton.Kind.PRIMARY), createY);
        flowWidget(helpButton(innerLeft + innerWidth - 24, createY,
                "新增区域从脚下一个方块开始，并成为画笔目标。",
                "创建后可用画笔在区域内重新圈定两个端点。",
                "区域名称、类型、显示与生效逻辑可在编辑页调整。"), createY);

        addSection("出生点工具", "先选择队伍或观战目标；菜单会保持打开，方便继续切换。",
                "关闭菜单后可用画笔右键添加、左键移除点。",
                "脚下按钮是备用精确操作；清空操作会先确认。",
                "观战点可放在地图边界外，但必须与地图同维度。");
        int spawnY = flowRow(BUTTON_HEIGHT);
        spawnToolButton = flowWidget(new UiCycleButton<>(innerLeft, spawnY, innerWidth, BUTTON_HEIGHT,
                List.of(MapTool.SPAWN_A, MapTool.SPAWN_B, MapTool.SPAWN_C,
                        MapTool.SPAWN_D, MapTool.SPECTATOR), spawnTool,
                tool -> "编辑出生点：" + tool.displayName(), tool -> {
                    spawnTool = tool;
                    request(MapEditorAction.SELECT_TOOL, tool.id());
                    rebuildWidgets();
                }, "选择队伍或观战目标；菜单保持打开。", UiButton.Kind.SECONDARY), spawnY);
        addCurrentSpawnActions();
        addRegionGroup(MapRegion.Type.SPAWN_A, MapRegion.Type.SPAWN_B,
                MapRegion.Type.SPAWN_C, MapRegion.Type.SPAWN_D);

        footerButton("地图工作台", 0, 3, 0, this::openWorkbench,
                "打开地图工作台。", UiButton.Kind.SECONDARY);
        footerButton("地图画笔", 1, 3, 0, () -> Minecraft.getInstance().setScreen(new MapBrushScreen()),
                "打开画笔设置；关闭画笔菜单会回到游戏。", UiButton.Kind.SECONDARY);
        footerButton("继续编辑", 2, 3, 0, this::onClose, "返回游戏，保留当前视角。", UiButton.Kind.PRIMARY);
        footerButton("恢复进入前视角", 0, 1, 1, this::exitEditMode,
                "恢复打开本菜单之前的视角，并返回游戏。", UiButton.Kind.SECONDARY);
        updateSpawnControls();
    }

    private void addCurrentSpawnActions() {
        int addActionY = flowRow(BUTTON_HEIGHT);
        spawnAddButton = flowWidget(uiButton(currentAddLabel(), innerLeft, addActionY, innerWidth - 30,
                this::addCurrentSpawn, "把脚下位置写入当前出生点目标。", UiButton.Kind.PRIMARY), addActionY);
        flowWidget(helpButton(innerLeft + innerWidth - 24, addActionY, currentAddHelp()), addActionY);

        int clearActionY = flowRow(BUTTON_HEIGHT);
        spawnClearButton = flowWidget(uiButton("清空当前目标", innerLeft, clearActionY, innerWidth - 30,
                this::clearCurrentSpawn, "清空当前目标出生点，会先确认。", UiButton.Kind.DANGER), clearActionY);
        flowWidget(helpButton(innerLeft + innerWidth - 24, clearActionY,
                "清空当前选择的队伍或观战出生点。",
                "操作不可撤销，会先弹出确认框。",
                "建议只在整个布局需要重做时使用。"), clearActionY);
        spawnActionButtons.add(spawnAddButton);
        spawnActionButtons.add(spawnClearButton);
    }

    private void addCameraButtons(int y) {
        CameraType[] cameras = {CameraType.FIRST_PERSON, CameraType.THIRD_PERSON_BACK, CameraType.THIRD_PERSON_FRONT};
        String[] labels = {"第一人称", "背后观察", "正面观察"};
        for (int i = 0; i < cameras.length; i++) {
            CameraType camera = cameras[i];
            UiButton button = uiButton(labels[i], columnX(i, 3, 4), y, columnWidth(3, 4),
                    () -> {
                        Minecraft.getInstance().options.setCameraType(camera);
                        rebuildWidgets();
                    }, "切换到" + labels[i] + "，关闭菜单后保留此选择。", UiButton.Kind.SECONDARY);
            button.setSelected(Minecraft.getInstance().options.getCameraType() == camera);
            flowWidget(button, y);
        }
    }

    private void addSection(String title, String... lines) {
        int y = flowRow(BUTTON_HEIGHT);
        sections.add(new SectionLabel(title, y));
        flowWidget(helpButton(innerLeft + innerWidth - 24, y, lines), y);
    }

    private void addRegionGroup(MapRegion.Type... types) {
        boolean found = false;
        for (MapRegion region : regions) {
            if (!isType(region, types)) continue;
            found = true;
            int y = flowRow(BUTTON_HEIGHT);
            UiButton button = uiButton(region.displayName() + " · " + region.type().displayName(),
                    innerLeft, y, innerWidth - 30, () -> edit(region),
                    "选中并在编辑器中打开这个区域。", UiButton.Kind.SECONDARY);
            flowWidget(button, y);
            buttons.add(new RegionButton(button, region));
            flowWidget(regionHelpButton(innerLeft + innerWidth - 24, y, region), y);
        }
        if (!found) {
            int y = flowRow(14);
            emptyLabels.add(new EmptyLabel("此分组暂无区域", y));
        }
    }

    private void addMissingFoundationButtons() {
        MapEditorView view = ClientMapEditorData.view();
        if (!view.bounds().present() && !hasRegionType(MapRegion.Type.BOUNDS)) {
            addFoundationButton("创建地图边界", MapRegion.Type.BOUNDS);
        }
        if (!view.resetRegion().present() && !hasRegionType(MapRegion.Type.RESET)) {
            addFoundationButton("创建重置区域", MapRegion.Type.RESET);
        }
    }

    private void addFoundationButton(String label, MapRegion.Type type) {
        int y = flowRow(BUTTON_HEIGHT);
        flowWidget(uiButton(label, innerLeft, y, innerWidth, () -> createFoundation(type),
                "在脚下创建初始区域；随后用画笔选择两个角。", UiButton.Kind.PRIMARY), y);
    }

    private boolean isType(MapRegion region, MapRegion.Type... types) {
        for (MapRegion.Type type : types) {
            if (region.type() == type) return true;
        }
        return false;
    }

    private boolean hasRegionType(MapRegion.Type type) {
        return regions.stream().anyMatch(region -> region.type() == type);
    }

    private void createFoundation(MapRegion.Type type) {
        if (Minecraft.getInstance().player == null) return;
        BlockPos point = Minecraft.getInstance().player.blockPosition();
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
        FpsTdmNetwork.sendToServer(new MapRegionPacket(MapRegionAction.SELECT, region, 0));
        Minecraft.getInstance().setScreen(new MapRegionEditScreen(region));
    }

    private void createRegion() {
        if (Minecraft.getInstance().player == null) return;
        MapDefinition.Region bounds = new MapDefinition.Region(
                Minecraft.getInstance().player.blockPosition(), Minecraft.getInstance().player.blockPosition());
        String id = MapRegion.uniqueId("", MapRegion.Type.CUSTOM.id(), usedRegionIds(regions));
        MapRegion region = MapRegion.custom(id, "自定义区域", MapRegion.Type.CUSTOM, bounds);
        FpsTdmNetwork.sendToServer(new MapRegionPacket(MapRegionAction.CREATE, region, 0));
        onClose();
    }

    private String currentAddLabel() {
        return spawnTool == MapTool.SPECTATOR ? "脚下设置观战点" : "脚下添加当前出生点";
    }

    private String[] currentAddHelp() {
        if (spawnTool == MapTool.SPECTATOR) {
            return new String[]{
                    "保存你脚下位置为观战出生点。",
                    "观战点可以放在地图边界外或空中。",
                    "唯一要求是位置与地图在同一维度。",
                    "重复设置会替换旧观战点。"
            };
        }
        return new String[]{
                "把你脚下位置加入当前选择的队伍出生点。",
                "当前目标显示在上方循环按钮中。",
                "关闭菜单后也可用画笔右键添加、左键移除。",
                "脚下按钮适合精确站位或临时补点。"
        };
    }

    private void addCurrentSpawn() {
        request(currentAddAction(), "");
    }

    private void clearCurrentSpawn() {
        String targetId = ClientMapEditorData.view().mapId();
        String label = "清空" + spawnTool.displayName();
        confirmAction(label, "地图「" + ClientMapEditorData.view().displayName() + "」：清空全部"
                + spawnTool.displayName() + "。此操作不可撤销。",
                () -> {
                    if (targetId.equals(ClientMapEditorData.view().mapId())) {
                        request(currentClearAction(), "");
                    }
                });
    }

    private MapEditorAction currentAddAction() {
        return switch (spawnTool) {
            case SPAWN_A -> MapEditorAction.ADD_TEAM_A;
            case SPAWN_B -> MapEditorAction.ADD_TEAM_B;
            case SPAWN_C -> MapEditorAction.ADD_TEAM_C;
            case SPAWN_D -> MapEditorAction.ADD_TEAM_D;
            case SPECTATOR -> MapEditorAction.SET_SPECTATOR;
            default -> MapEditorAction.ADD_TEAM_A;
        };
    }

    private MapEditorAction currentClearAction() {
        return switch (spawnTool) {
            case SPAWN_A -> MapEditorAction.CLEAR_TEAM_A;
            case SPAWN_B -> MapEditorAction.CLEAR_TEAM_B;
            case SPAWN_C -> MapEditorAction.CLEAR_TEAM_C;
            case SPAWN_D -> MapEditorAction.CLEAR_TEAM_D;
            case SPECTATOR -> MapEditorAction.CLEAR_SPECTATOR;
            default -> MapEditorAction.CLEAR_TEAM_A;
        };
    }

    private void updateSpawnControls() {
        MapEditorView view = ClientMapEditorData.view();
        boolean available = view.canEdit() && !view.locked()
                && !view.draftInvalidated() && view.hasTarget();
        if (spawnToolButton != null) {
            spawnToolButton.active = available;
        }
        for (UiButton button : spawnActionButtons) {
            button.active = available;
        }
        if (spawnAddButton != null) {
            spawnAddButton.setMessage(Component.literal(currentAddLabel()));
        }
        if (spawnClearButton != null) {
            spawnClearButton.setMessage(Component.literal("清空当前目标"));
        }
    }

    private void request(MapEditorAction action, String value) {
        if (minecraft != null && minecraft.getConnection() != null) {
            FpsTdmNetwork.sendToServer(new MapEditorActionPacket(action, value));
        }
    }

    private void toggleHelp() {
        showHelp(
                "右键打开菜单，左键直接选定准星命中的区域。",
                "站在区域 A 内指向区域 B 时，会优先选择 B。",
                "多个区域重叠时，菜单会列出全部可编辑目标。",
                "悬停菜单中的区域名称，对应边框会呼吸发光。",
                "打开菜单不改变视角，可按需要切换观察方式。",
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
        MapLibraryScreen.open(this);
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        updateSpawnControls();
        if (ticks % 20 == 0) {
            request(MapEditorAction.POLL, "");
        }
        if (lastContentRevision != ClientMapEditorData.contentRevision()) {
            lastContentRevision = ClientMapEditorData.contentRevision();
            regions = orderRegions(ClientMapEditorData.view().regions(), priorityRegionIds);
            rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderShell(graphics, "右键打开 · 左键选定");
        MapEditorView view = ClientMapEditorData.view();
        paintBand(graphics, contextY, 58, () -> renderContext(graphics, view));
        paintBand(graphics, nextY, 22, () -> badge(graphics, nextHint(view), innerLeft, nextY, innerWidth, nextColor(view)));
        for (SectionLabel section : sections) {
            paintBand(graphics, section.y(), BUTTON_HEIGHT, () ->
                    section(graphics, section.title(), innerLeft, section.y() + 5, innerWidth - 30));
        }
        for (EmptyLabel empty : emptyLabels) {
            paintBand(graphics, empty.y(), 14, () -> graphics.drawString(font, empty.text(),
                    innerLeft + 8, empty.y(), UiTheme.SUBTLE, false));
        }
        for (RegionButton entry : buttons) {
            if (entry.button().isHoveredOrFocused()) {
                MapToolClientState.setHoveredRegion(entry.region().id());
            }
        }
        if (buttons.stream().noneMatch(entry -> entry.button().isHoveredOrFocused())) {
            MapToolClientState.setHoveredRegion("");
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

    private void renderContext(GuiGraphics graphics, MapEditorView view) {
        divider(graphics, innerLeft, contextY + 57, innerWidth);
        graphics.drawString(font, fit("地图  " + dash(view.displayName()), innerWidth - 16),
                innerLeft + 8, contextY, UiTheme.TEXT, false);
        graphics.drawString(font, fit("目标  " + selectedTarget(view), innerWidth - 16),
                innerLeft + 8, contextY + 14, UiTheme.ACCENT, false);
        graphics.drawString(font, fit("区域  " + regions.size() + " 个 · 选中后可编辑属性", innerWidth - 16),
                innerLeft + 8, contextY + 28, UiTheme.MUTED, false);
        graphics.drawString(font, fit("缺失  " + missingFoundations(view), innerWidth - 16),
                innerLeft + 8, contextY + 42, missingFoundations(view).equals("无") ? UiTheme.SUBTLE : UiTheme.ERROR, false);
    }

    private String selectedTarget(MapEditorView view) {
        if (!view.selectedRegionId().isBlank()) {
            return regions.stream().filter(region -> region.id().equals(view.selectedRegionId()))
                    .findFirst().map(region -> region.displayName() + " · " + region.type().displayName())
                    .orElse(view.selectedRegionId());
        }
        if (view.selectedTool().kind() == MapTool.Kind.POINT) {
            return view.selectedTool().displayName();
        }
        return "未选择";
    }

    private String missingFoundations(MapEditorView view) {
        List<String> missing = new ArrayList<>();
        if (!view.bounds().present() && !hasRegionType(MapRegion.Type.BOUNDS)) missing.add("地图边界");
        if (!view.resetRegion().present() && !hasRegionType(MapRegion.Type.RESET)) missing.add("重置区域");
        return missing.isEmpty() ? "无" : String.join("、", missing);
    }

    private String nextHint(MapEditorView view) {
        if (!view.hasTarget()) return "下一步：返回地图工作台选择或新建地图。";
        if (!missingFoundations(view).equals("无")) return "下一步：先创建地图边界和重置区域。";
        if (!view.selectedRegionId().isBlank()) return "下一步：关闭菜单，用画笔框选两个角，再回到属性页调整。";
        if (view.selectedTool().kind() == MapTool.Kind.POINT) return "下一步：关闭菜单，画笔右键添加、左键移除出生点。";
        return "下一步：点击一个区域进入属性页，或新增自定义区域。";
    }

    private int nextColor(MapEditorView view) {
        return !view.hasTarget() || !missingFoundations(view).equals("无")
                ? UiTheme.WARNING : UiTheme.INFO;
    }

    private static String dash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static Set<String> usedRegionIds(List<MapRegion> regions) {
        Set<String> used = new HashSet<>();
        used.add("bounds");
        used.add("reset");
        if (regions != null) {
            for (MapRegion region : regions) {
                if (region != null) used.add(region.id());
            }
        }
        return used;
    }

    private static List<MapRegion> orderRegions(List<MapRegion> regions, List<String> priorityIds) {
        List<MapRegion> ordered = new ArrayList<>(regions == null ? List.of() : regions);
        ordered.sort(Comparator.comparingInt(region -> {
            int index = priorityIds.indexOf(region.id());
            return index < 0 ? Integer.MAX_VALUE : index;
        }));
        return List.copyOf(ordered);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record RegionButton(UiButton button, MapRegion region) {
    }

    private record SectionLabel(String title, int y) {
    }

    private record EmptyLabel(String text, int y) {
    }
}
