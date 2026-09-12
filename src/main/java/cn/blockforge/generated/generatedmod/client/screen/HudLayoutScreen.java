package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientHudLayout;
import cn.blockforge.generated.generatedmod.client.HudBackground;
import cn.blockforge.generated.generatedmod.client.HudContext;
import cn.blockforge.generated.generatedmod.client.HudCustomRenderer;
import cn.blockforge.generated.generatedmod.client.HudGeometry;
import cn.blockforge.generated.generatedmod.client.HudParameters;
import cn.blockforge.generated.generatedmod.client.HudStats;
import cn.blockforge.generated.generatedmod.client.MatchHudOverlay;
import cn.blockforge.generated.generatedmod.client.ui.UiButton;
import cn.blockforge.generated.generatedmod.client.ui.UiColorPalette;
import cn.blockforge.generated.generatedmod.client.ui.UiCycleButton;
import cn.blockforge.generated.generatedmod.client.ui.UiEditBox;
import cn.blockforge.generated.generatedmod.client.ui.UiScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiSlider;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * HUD 配置窗（完全重做版）：顶部固定页签选场景，右侧固定宽度的属性坞按
 * 「组件列表 → 组件属性」两段式排版，每一行占据独立横带、两列行严格等宽切分，
 * 从结构上排除旧版滑条标签互相错位的问题。
 *
 * <p>能力：内置组件（记分板 / 状态文字 / 击杀播报 / 场景横幅）与独立模块
 * （文字 / 色块 / 进度条 / 图片）的增删改；文字与进度条模块可绑定
 * {@link HudStats} 的任意统计接口作为内容来源；颜色用展开式色板挑选；
 * 画面中的面板和模块都可直接拖动，配置实时生效，关窗写入 client-hud.json。
 */
public final class HudLayoutScreen extends Screen implements cn.blockforge.generated.generatedmod.client.ui.UiChoiceHost {
    private final cn.blockforge.generated.generatedmod.client.ui.UiChoicePopup choices = new cn.blockforge.generated.generatedmod.client.ui.UiChoicePopup();
    @Override public cn.blockforge.generated.generatedmod.client.ui.UiChoicePopup choicePopup() { return choices; }
    private static final int TAB_HEIGHT = UiScreen.BUTTON_HEIGHT;
    private static final int TAB_TOP = 6;
    private static final int DOCK_HEADER_HEIGHT = 22;
    private static final int CONTROL = UiScreen.BUTTON_HEIGHT;
    private static final int SLIDER_ROW = 26;
    private static final int HEADER_ROW = 14;
    private static final int NOTE_ROW = 12;
    private static final int ROW_GAP = 6;
    /** 视口顶部留白：滑条的标签画在轨道上方 9px，第一行必须留出这个高度。 */
    private static final int TOP_INSET = 13;
    private static final int DOCK_MIN_WIDTH = 220;
    private static final int DOCK_MAX_WIDTH = 520;
    private static final int DOCK_MIN_HEIGHT = 180;
    private static final int FOOTER_ROWS = 2;
    private static final long HISTORY_IDLE_MILLIS = 450L;
    private static final String FIXED_SOURCE = "固定内容（不绑定）";
    /** 0 表示单独占一行，正数表示同一横向布局行。 */
    private static final int UNGROUPED_LINE = 0;
    private int nextLayoutLine;

    private enum Drag { NONE, SCORE, TEXT, FEED, BANNER, CUSTOM }

    private enum ResizeEdge {
        NONE(false, false, false, false),
        LEFT(true, false, false, false),
        RIGHT(false, true, false, false),
        TOP(false, false, true, false),
        BOTTOM(false, false, false, true),
        TOP_LEFT(true, false, true, false),
        TOP_RIGHT(false, true, true, false),
        BOTTOM_LEFT(true, false, false, true),
        BOTTOM_RIGHT(false, true, false, true);

        private final boolean left;
        private final boolean right;
        private final boolean top;
        private final boolean bottom;

        ResizeEdge(boolean left, boolean right, boolean top, boolean bottom) {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
        }
    }

    private enum RowKind { HEADER, NOTE, WIDGET }

    private record Row(AbstractWidget widget, String text, RowKind kind, int height, int line,
                       int slot, int columns) {
        static Row header(String text) {
            return new Row(null, text, RowKind.HEADER, HEADER_ROW, UNGROUPED_LINE, 0, 1);
        }

        static Row note(String text) {
            return note(text, NOTE_ROW);
        }

        static Row note(String text, int height) {
            return new Row(null, text, RowKind.NOTE, height, UNGROUPED_LINE, 0, 1);
        }
    }

    private final Screen parent;
    private ClientHudLayout.Draft draft;
    private HudContext activeContext;
    private boolean globalTab;
    private boolean propertyTab;
    private boolean dockHidden;
    private String selected = "score";
    private boolean examplesExpanded;
    private boolean paletteOpen;
    private String paletteTarget = "";
    private boolean exitPromptOpen;
    private boolean dirty;
    private ClientHudLayout.Snapshot savedSnapshot;
    private final Deque<ClientHudLayout.Snapshot> undoStack = new ArrayDeque<>();
    private final Deque<ClientHudLayout.Snapshot> redoStack = new ArrayDeque<>();
    /** 当前连续编辑事务开始前的快照；一次拖动或连续输入只产生一个撤销点。 */
    private ClientHudLayout.Snapshot historyBaseline;
    private long lastHistoryChange;
    private boolean historySuspended;
    private boolean historyPending;
    private String sourceFilterKey = "";
    private String sourcePrimary = "全部";
    private String sourceSecondary = "全部";
    private String sourceTertiary = "全部";

    private int dockWidth;
    private int dockX;
    private int dockTop;
    private int dockBottom;
    private int scroll;

    private Drag drag = Drag.NONE;
    private double grabX;
    private double grabY;
    private String dragCustomId = "";
    private boolean draggingDock;
    private double dockGrabX;
    private double dockGrabY;
    private ResizeEdge resizeEdge = ResizeEdge.NONE;
    private double dockResizeGrabX;
    private double dockResizeGrabY;
    private int resizeOriginX;
    private int resizeOriginTop;
    private int resizeOriginWidth;
    private int resizeOriginBottom;
    private boolean sidePanelNeedsRebuild;
    private boolean draggingPanelScrollbar;
    private int panelScrollbarDragOffset;

    private String status = "";
    private int statusColor = UiTheme.INFO;
    private int statusTicks;
    private int marqueeTicks;

    private UiEditBox activeEditor;
    private UiEditBox conditionEditor;
    private UiButton colorToggle;
    private UiButton closeButton;

    private final List<Row> rows = new ArrayList<>();
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private final List<UiButton> footerButtons = new ArrayList<>();
    private final List<UiEditBox> editors = new ArrayList<>();

    public HudLayoutScreen(Screen parent) {
        super(Component.literal("HUD 配置窗"));
        this.parent = parent;
        this.savedSnapshot = ClientHudLayout.snapshot();
        this.draft = savedSnapshot.draft();
        this.historyBaseline = savedSnapshot;
        this.lastHistoryChange = System.currentTimeMillis();
        this.activeContext = ClientHudLayout.activeMatchContext();
        if (this.activeContext == null) {
            this.activeContext = HudContext.MATCHING;
        }
        this.selected = defaultSelection();
    }

    // ---------------------------------------------------------------- 页签与布局

    @Override
    protected void init() {
        for (HudContext context : HudContext.values()) {
            cn.blockforge.generated.generatedmod.client.HudAssemblies.splitAll(draft, context);
        }
        if (selectedCustom() == null) selected = defaultSelection();
        choices.close();
        rows.clear();
        widgets.clear();
        footerButtons.clear();
        editors.clear();
        nextLayoutLine = 0;
        activeEditor = null;
        conditionEditor = null;
        colorToggle = null;
        closeButton = null;
        int defaultWidth = Math.max(DOCK_MIN_WIDTH, Math.min(DOCK_MAX_WIDTH, width * 2 / 5));
        int maxWidth = Math.max(200, width - 40);
        if (dockWidth < DOCK_MIN_WIDTH || dockWidth > maxWidth) {
            dockWidth = Math.min(defaultWidth, maxWidth);
        }
        int minimumTop = TAB_TOP + TAB_HEIGHT + 2;
        int maxHeight = Math.max(DOCK_MIN_HEIGHT, height - minimumTop - 10);
        if (dockX < 0 || dockX + dockWidth > width || dockTop < minimumTop
                || dockBottom > height || dockBottom - dockTop < DOCK_MIN_HEIGHT) {
            dockX = width - dockWidth - 6;
            dockTop = TAB_HEIGHT + 12;
            dockBottom = dockTop + maxHeight;
        }
        dockWidth = clamp(dockWidth, Math.min(DOCK_MIN_WIDTH, maxWidth), maxWidth);
        dockX = clamp(dockX, 6, Math.max(6, width - dockWidth - 6));
        dockTop = clamp(dockTop, minimumTop, Math.max(minimumTop, height - DOCK_MIN_HEIGHT - 10));
        dockBottom = clamp(dockBottom, dockTop + DOCK_MIN_HEIGHT, height - 10);

        addTabs();
        if (dockHidden) return;
        int toolY = dockTop + DOCK_HEADER_HEIGHT + 4;
        int toolGap = 6;
        int toolWidth = (dockInnerWidth() - toolGap) / 2;
        for (int i = 0; i < 2; i++) {
            boolean properties = i == 1;
            UiButton tab = new UiButton(dockX + 8 + i * (toolWidth + toolGap), toolY,
                    toolWidth, CONTROL, Component.literal(properties ? "属性" : "组件"), ignored -> {
                commitHistoryEdit(); propertyTab = properties; scroll = 0; rebuildWidgets();
            }, UiButton.Kind.SECONDARY);
            tab.setSelected(propertyTab == properties);
            addRenderableWidget(tab);
        }
        if (globalTab && !propertyTab) addGlobalControls();
        if (propertyTab) addPropertyControls(); else addComponentList();
        addFooter();
        clampScroll();
        layoutRows();
    }

    private void addTabs() {
        if (dockHidden) {
            UiButton restore = new UiButton(width - 28, height - 28, 22, TAB_HEIGHT, Component.literal("<"),
                    ignored -> { dockHidden = false; rebuildWidgets(); }, UiButton.Kind.SECONDARY);
            restore.setTooltip(Tooltip.create(Component.literal("返回 HUD 编辑")));
            addRenderableWidget(restore);
            return;
        }
        int gap = 4;
        int columns = HudContext.values().length;
        int tabWidth = (width - 40 - gap * (columns - 1)) / columns;
        int index = 0;
        for (HudContext context : HudContext.values()) {
            HudContext target = context;
            UiButton tab = new UiButton(6 + index * (tabWidth + gap), TAB_TOP, tabWidth, TAB_HEIGHT,
                    Component.literal(context.tabLabel()), ignored -> {
                        commitHistoryEdit();
                        globalTab = target == HudContext.GLOBAL;
                        if (activeContext != target) {
                            activeContext = target;
                            selected = defaultSelection();
                            paletteOpen = false;
                            scroll = 0;
                        }
                        rebuildWidgets();
                    }, globalTab != (context == HudContext.GLOBAL) || activeContext != context
                            ? UiButton.Kind.SECONDARY : UiButton.Kind.PRIMARY);
            tab.setSelected(activeContext == context);
            addRenderableWidget(tab);
            index++;
        }
        UiButton toggleDock = new UiButton(width - 28, TAB_TOP, 22, TAB_HEIGHT,
                Component.literal(dockHidden ? "<" : ">"), ignored -> {
                    commitHistoryEdit(); dockHidden = !dockHidden; rebuildWidgets();
                }, UiButton.Kind.SECONDARY);
        toggleDock.setTooltip(Tooltip.create(Component.literal(dockHidden ? "显示属性面板" : "隐藏属性面板")));
        addRenderableWidget(toggleDock);
    }

    private String defaultSelection() {
        return draft.customElements(activeContext).stream().filter(e -> !e.type().equals("block"))
                .map(e -> "custom:" + e.id()).findFirst().orElse("");
    }

    // ---------------------------------------------------------------- 组件列表

    private void addComponentList() {
        rows.add(Row.note(globalTab
                ? "全局组件会在比赛、匹配和房间界面常驻。\n只有组件自身的触发条件会影响显示。"
                : "示例由独立元素拼成。\n文字、数值、底板均可单独删除。"));
        if (!globalTab) addLayoutPresets();
        rows.add(Row.header(globalTab ? "全局组件种类" : "组件种类"));
        int kindLine = beginLayoutLine();
        registerAddButton("+文字", 0, 4, kindLine, "text", "文字模块：固定文字或插入多个实时参数。");
        registerAddButton("+色块", 1, 4, kindLine, "block", "纯色块：常用作底板、比分底或装饰。");
        registerAddButton("+进度条", 2, 4, kindLine, "progress", "进度条：绑定一个数值或时间数据源。");
        registerAddButton("+图片", 3, 4, kindLine, "image", "图片模块：使用 config/fpsmod/hud_images 里的 PNG。");
        if (activeContext.isMatch()) {
            int effectLine = beginLayoutLine();
            registerAddButton("+阵亡与部署", 0, 2, effectLine, "respawn", "可完全编辑的阵亡与回归效果。");
            registerAddButton("+越界警告", 1, 2, effectLine, "boundary", "可完全编辑的出界倒计时效果。");
        }
        if (!globalTab) {
            rows.add(Row.header("场景组件"));
            if (activeContext.isMatch()) {
                addBuiltInRow("score", "记分板（内置）", () -> current().scoreVisible);
                addBuiltInRow("text", "状态文字（内置）", () -> current().textVisible);
                addBuiltInRow("feed", "击杀播报（内置）", () -> current().feedVisible);
            } else {
                addBuiltInRow("banner", "场景横幅（内置）", () -> current().bannerVisible);
            }
        }
        List<ClientHudLayout.CustomElement> elements = draft.customElements(activeContext);
        if (elements.isEmpty()) {
            rows.add(Row.note("还没有独立模块，用下面的按钮添加。"));
        }
        for (ClientHudLayout.CustomElement element : elements) {
            String id = element.id();
            UiButton button = new UiButton(0, 0, 10, CONTROL,
                    Component.literal(element.displayName() + (element.visible() ? "" : "〔关〕")),
                    ignored -> {
                        commitHistoryEdit();
                        selected = "custom:" + id;
                        paletteOpen = false;
                        rebuildWidgets();
                    }, selected.equals("custom:" + id) ? UiButton.Kind.PRIMARY : UiButton.Kind.SECONDARY);
            button.setSelected(selected.equals("custom:" + id));
            register(button, CONTROL);
        }
        UiButton remove = new UiButton(0, 0, 10, CONTROL, Component.literal("删除当前模块"), ignored -> {
            ClientHudLayout.CustomElement chosen = selectedCustom();
            if (chosen != null) {
                String id = chosen.id();
                editDiscrete(() -> draft.removeCustomElement(activeContext, id));
                selected = defaultSelection();
                paletteOpen = false;
                rebuildWidgets();
                setStatus("已删除模块。", UiTheme.SUCCESS);
            }
        }, UiButton.Kind.DANGER);
        remove.active = selectedCustom() != null;
        register(remove, CONTROL);
    }

    private void addBuiltInRow(String key, String label, java.util.function.BooleanSupplier visible) {
        if (!current().builtInEnabled(HudContext.BuiltIn.valueOf(key.toUpperCase(Locale.ROOT)))) return;
        UiButton button = new UiButton(0, 0, 10, CONTROL,
                Component.literal(label + (visible.getAsBoolean() ? "" : "〔关〕")),
                ignored -> {
                    commitHistoryEdit();
                    selected = key;
                    paletteOpen = false;
                    rebuildWidgets();
                }, selected.equals(key) ? UiButton.Kind.PRIMARY : UiButton.Kind.SECONDARY);
        button.setSelected(selected.equals(key));
        register(button, CONTROL);
    }

    private void registerAddButton(String label, int slot, int columns, int line,
                                   String type, String tooltip) {
        UiButton button = new UiButton(0, 0, 10, CONTROL, Component.literal(label),
                ignored -> addElement(type), UiButton.Kind.SECONDARY);
        button.setTooltip(Tooltip.create(Component.literal(tooltip)));
        register(button, CONTROL, line, slot, columns);
    }

    // ---------------------------------------------------------------- 组件属性

    private ClientHudLayout.Mutable current() {
        return draft.of(activeContext);
    }

    private ClientHudLayout.CustomElement selectedCustom() {
        if (!selected.startsWith("custom:")) {
            return null;
        }
        return customById(selected.substring("custom:".length()));
    }

    private String selectedCustomId() {
        ClientHudLayout.CustomElement element = selectedCustom();
        return element == null ? "" : element.id();
    }

    private ClientHudLayout.CustomElement customById(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        for (ClientHudLayout.CustomElement element : draft.customElements(activeContext)) {
            if (element.id().equals(id)) {
                return element;
            }
        }
        return null;
    }

    private void addPropertyControls() {
        ClientHudLayout.CustomElement chosen = selectedCustom();
        if (chosen != null) {
            addCustomProperties(chosen);
        } else if (!selected.isBlank()) {
            addBuiltInProperties();
        } else {
            rows.add(Row.header("未选择元素"));
        }
        rows.add(new Row(null, "拖动画面中的面板/模块即可摆位置；滚轮在右侧列内滚动。",
                RowKind.NOTE, NOTE_ROW * 2, UNGROUPED_LINE, 0, 1));
    }

    /** 参考模组的预设入口：一键整理当前位置，随后仍可逐项微调。 */
    private void addLayoutPresets() {
        register(new UiButton(0, 0, 10, CONTROL, Component.literal((examplesExpanded ? "v " : "> ") + "HUD 示例"),
                ignored -> { examplesExpanded = !examplesExpanded; rebuildWidgets(); }, UiButton.Kind.SECONDARY), CONTROL);
        if (!examplesExpanded) return;
        rows.add(Row.header("内置 HUD 模板"));
        for (cn.blockforge.generated.generatedmod.client.HudPreset preset
                : cn.blockforge.generated.generatedmod.client.HudPreset.values()) {
            UiButton button = new UiButton(0, 0, 10, CONTROL, Component.literal(preset.label()), ignored -> {
                editDiscrete(() -> preset.apply(draft, activeContext));
                selected = defaultSelection();
                rebuildWidgets();
                setStatus("已应用“" + preset.label() + "”模板，可撤销；保存后保留。", UiTheme.SUCCESS);
            }, UiButton.Kind.SECONDARY);
            button.setTooltip(Tooltip.create(Component.literal(preset.description()
                    + "替换当前场景的示例元素，保留自行添加的元素与背景；可撤销。")));
            register(button, CONTROL);
        }
        rows.add(Row.header("添加组件示例"));
        for (HudContext.BuiltIn component : HudContext.BuiltIn.values()) {
            if (component == HudContext.BuiltIn.NOTICE && !activeContext.isMatch()) continue;
            if (activeContext.isMatch() == (component == HudContext.BuiltIn.BANNER)) continue;
            UiButton sample = new UiButton(0, 0, 10, CONTROL, Component.literal("+ " + component.displayName()), ignored -> {
                editDiscrete(() -> {
                    current().setBuiltInEnabled(component, true);
                    cn.blockforge.generated.generatedmod.client.HudAssemblies.split(draft, activeContext, component);
                });
                selected = defaultSelection();
                rebuildWidgets();
            }, UiButton.Kind.SECONDARY);
            sample.setTooltip(Tooltip.create(Component.literal("添加由独立元素拼成的示例，各元素可分别编辑和删除。")));
            register(sample, CONTROL);
        }
        if (activeContext.isMatch()) {
            rows.add(Row.header("对局事件组件"));
            for (var event : cn.blockforge.generated.generatedmod.match.MatchHudEventType.values()) {
                UiButton eventButton = new UiButton(0, 0, 10, CONTROL,
                        Component.literal("+ " + event.displayName()), ignored -> {
                    editDiscrete(() -> cn.blockforge.generated.generatedmod.client.HudAssemblies
                            .addEventComponent(draft, activeContext, event));
                    selected = defaultSelection();
                    rebuildWidgets();
                    setStatus("已添加事件组件“" + event.displayName() + "”，可单独编辑和删除。", UiTheme.SUCCESS);
                }, UiButton.Kind.SECONDARY);
                eventButton.setTooltip(Tooltip.create(Component.literal(
                        "仅在事件 " + event.id() + " 有效时显示；可改位置、文字、颜色、动画和条件。")));
                register(eventButton, CONTROL);
            }
            for (var descriptor :
                    cn.blockforge.generated.generatedmod.client.ClientHudEventData.descriptors()) {
                if (activeContext != HudContext.GLOBAL
                        && !descriptor.contexts().contains(activeContext)) continue;
                UiButton eventButton = new UiButton(0, 0, 10, CONTROL,
                        Component.literal("+ " + descriptor.title()), ignored -> {
                    editDiscrete(() -> cn.blockforge.generated.generatedmod.client.HudAssemblies
                            .addEventComponent(draft, activeContext, descriptor.id(),
                                    descriptor.title()));
                    selected = defaultSelection();
                    rebuildWidgets();
                    setStatus("已添加事件组件“" + descriptor.title() + "”，可单独编辑和删除。",
                            UiTheme.SUCCESS);
                }, UiButton.Kind.SECONDARY);
                eventButton.setTooltip(Tooltip.create(Component.literal(
                        "仅在事件 " + descriptor.id() + " 有效时显示；可改位置、文字、颜色、动画和条件。")));
                register(eventButton, CONTROL);
            }
        }
    }

    private void addLegacyPositionPresets() {
        rows.add(Row.header("快速布局预设"));
        UiButton compact = new UiButton(0, 0, 10, CONTROL, Component.literal("紧凑"),
                ignored -> applyPreset("compact"), UiButton.Kind.SECONDARY);
        compact.setTooltip(Tooltip.create(Component.literal("适合小屏：顶部记分板、底部状态条，模块保持紧凑。")));
        int presetLine = beginLayoutLine();
        register(compact, CONTROL, presetLine, 0, 3);
        UiButton centered = new UiButton(0, 0, 10, CONTROL, Component.literal("居中"),
                ignored -> applyPreset("center"), UiButton.Kind.SECONDARY);
        centered.setTooltip(Tooltip.create(Component.literal("把常用内置 HUD 放回屏幕安全区中央。")));
        register(centered, CONTROL, presetLine, 1, 3);
        UiButton wide = new UiButton(0, 0, 10, CONTROL, Component.literal("宽屏"),
                ignored -> applyPreset("wide"), UiButton.Kind.SECONDARY);
        wide.setTooltip(Tooltip.create(Component.literal("适合宽屏：放大记分板并拉开底部信息。")));
        register(wide, CONTROL, presetLine, 2, 3);
        rows.add(Row.note("预设只改变当前场景的内置 HUD 位置与大小，不会删除独立模块。"));
    }

    private void addBuiltInProperties() {
        switch (selected) {
            case "score" -> {
                rows.add(Row.header("属性 · 记分板"));
                addToggle("显示记分板", () -> current().scoreVisible, value -> current().scoreVisible = value);
                addSliderPair("横向位置 %", "纵向位置 %", 0, 100,
                        () -> current().scoreXPercent, value -> current().scoreXPercent = value,
                        () -> current().scoreYPercent, value -> current().scoreYPercent = value);
                addSlider("面板宽度", 220, 600, () -> current().scoreWidth, value -> current().scoreWidth = value);
                addSlider("界面缩放 %", 50, 160, () -> current().scoreScalePercent,
                        value -> current().scoreScalePercent = value);
                addSlider("面板不透明度 %", 20, 100, () -> current().scoreOpacityPercent,
                        value -> current().scoreOpacityPercent = value);
                addTemplateEditor("标题模板", () -> current().scoreHeaderTemplate,
                        value -> current().scoreHeaderTemplate = value,
                        "可用：{mode} {phase} {score_a} {score_b} {time} {round} {target}");
                addTemplateEditor("A队模板", () -> current().scoreTeamATemplate,
                        value -> current().scoreTeamATemplate = value, "可用：{score_a} {team}");
                addTemplateEditor("B队模板", () -> current().scoreTeamBTemplate,
                        value -> current().scoreTeamBTemplate = value, "可用：{score_b}");
                addTemplateEditor("计时模板", () -> current().scoreTimerTemplate,
                        value -> current().scoreTimerTemplate = value, "可用：{time}");
                addTemplateEditor("详情模板", () -> current().scoreDetailsTemplate,
                        value -> current().scoreDetailsTemplate = value, "可用：{round} {target} {sizes}");
                addBuiltInColor("记分板颜色", () -> current().scoreColor,
                        value -> current().scoreColor = value);
            }
            case "text" -> {
                rows.add(Row.header("属性 · 状态文字"));
                addToggle("显示状态文字", () -> current().textVisible, value -> current().textVisible = value);
                addSliderPair("横向位置 %", "纵向位置 %", 0, 100,
                        () -> current().textXPercent, value -> current().textXPercent = value,
                        () -> current().textYPercent, value -> current().textYPercent = value);
                addSlider("面板宽度", 140, 500, () -> current().textWidth, value -> current().textWidth = value);
                addSlider("界面缩放 %", 50, 160, () -> current().textScalePercent,
                        value -> current().textScalePercent = value);
                addSlider("面板不透明度 %", 20, 100, () -> current().textOpacityPercent,
                        value -> current().textOpacityPercent = value);
                addTemplateEditor("状态文字模板", () -> current().textTemplate,
                        value -> current().textTemplate = value,
                        "可用：{team} {sizes} {hint} {mode} {phase}");
                addBuiltInColor("状态文字颜色", () -> current().textColor,
                        value -> current().textColor = value);
            }
            case "feed" -> {
                rows.add(Row.header("属性 · 击杀播报"));
                addToggle("显示击杀播报", () -> current().feedVisible, value -> current().feedVisible = value);
                addSliderPair("横向位置 %", "纵向位置 %", 0, 100,
                        () -> current().feedXPercent, value -> current().feedXPercent = value,
                        () -> current().feedYPercent, value -> current().feedYPercent = value);
                addSlider("界面缩放 %", 50, 160, () -> current().feedScalePercent,
                        value -> current().feedScalePercent = value);
                addSlider("面板不透明度 %", 20, 100, () -> current().feedOpacityPercent,
                        value -> current().feedOpacityPercent = value);
                addTemplateEditor("击杀播报模板", () -> current().feedTemplate,
                        value -> current().feedTemplate = value,
                        "可用：{killer} {victim} {feed}");
                addBuiltInColor("击杀播报颜色", () -> current().feedColor,
                        value -> current().feedColor = value);
            }
            default -> {
                rows.add(Row.header("属性 · 场景横幅"));
                addToggle("显示场景横幅", () -> current().bannerVisible, value -> current().bannerVisible = value);
                addSliderPair("横向位置 %", "纵向位置 %", 0, 100,
                        () -> current().bannerXPercent, value -> current().bannerXPercent = value,
                        () -> current().bannerYPercent, value -> current().bannerYPercent = value);
                addSlider("界面缩放 %", 50, 160, () -> current().bannerScalePercent,
                        value -> current().bannerScalePercent = value);
                addSlider("面板不透明度 %", 20, 100, () -> current().bannerOpacityPercent,
                        value -> current().bannerOpacityPercent = value);
                addTemplateEditor("横幅第一行模板", () -> current().bannerLineOneTemplate,
                        value -> current().bannerLineOneTemplate = value,
                        activeContext == HudContext.MATCHING
                                ? "可用：{matching_line1} {queue} {position} {wait} {ready}"
                                : "可用：{room_line1} {room} {map} {members} {owner} {state}");
                addTemplateEditor("横幅第二行模板", () -> current().bannerLineTwoTemplate,
                        value -> current().bannerLineTwoTemplate = value,
                        activeContext == HudContext.MATCHING
                                ? "可用：{matching_line2} {queue} {position} {wait} {ready}"
                                : "可用：{room_line2} {room} {map} {members} {owner} {state}");
                addBuiltInColor("横幅颜色", () -> current().bannerColor,
                        value -> current().bannerColor = value);
                rows.add(Row.note(activeContext == HudContext.MATCHING
                        ? "横幅内容 = 匹配队列 / 开赛倒计时，全部字段也可绑定给独立模块。"
                        : "横幅内容 = 房间名 / 模式 / 地图 / 人数，全部字段也可绑定给独立模块。"));
            }
        }
    }

    private void addCustomProperties(ClientHudLayout.CustomElement chosen) {
        String id = chosen.id();
        rows.add(Row.header("属性 · " + chosen.displayName()));
        UiButton remove = new UiButton(0, 0, 10, CONTROL, Component.literal("删除此元素"), ignored -> {
            editDiscrete(() -> draft.removeCustomElement(activeContext, id));
            selected = defaultSelection();
            rebuildWidgets();
        }, UiButton.Kind.DANGER);
        register(remove, CONTROL);
        addToggle("显示该模块", () -> intOf(id, e -> e.visible() ? 1 : 0, 1) == 1,
                value -> replaceById(id, e -> e.visible = value));

        boolean textLike = "text".equals(chosen.type()) || "respawn".equals(chosen.type()) || "boundary".equals(chosen.type());
        boolean progress = "progress".equals(chosen.type());
        boolean image = "image".equals(chosen.type());
        boolean statusEffect = "respawn".equals(chosen.type()) || "boundary".equals(chosen.type());

        if (textLike || progress) {
            addParameterHelp();
            addTextEditor(chosen);
            if (progress) addSourcePicker(chosen);
            else addParameterInserter(chosen);
            if (progress) {
                rows.add(Row.note("进度来源：绑定后按数值/上限填充；普通数值按百分数解释。"));
            } else {
                rows.add(Row.note("模板：%s=值 %v=原始数 %m=上限；固定文字不追加数值。"));
            }
        }
        if (textLike) {
            addSlider("文字大小 %", 50, 300,
                    () -> intOf(id, ClientHudLayout.CustomElement::scalePercent, 100),
                    value -> replaceById(id, e -> e.scalePercent = value));
        }
        if (image) {
            addImageFileCycle(chosen);
        }
        if (!image) {
            addColorRow(chosen, id);
        }
        if (statusEffect) rows.add(Row.note("状态效果和普通模块一样可删除、改文字、位置、尺寸、颜色与触发条件。"));
        addToggle("显示背景", () -> boolOf(id, ClientHudLayout.CustomElement::background),
                value -> replaceById(id, e -> e.background = value));
        addToggle("显示边框", () -> boolOf(id, ClientHudLayout.CustomElement::border),
                value -> replaceById(id, e -> e.border = value));
        addToggle("显示阴影", () -> boolOf(id, ClientHudLayout.CustomElement::shadow),
                value -> replaceById(id, e -> e.shadow = value));
        addToggle("发光效果", () -> boolOf(id, e -> e.placement().glow()),
                value -> replaceById(id, e -> e.placement = e.placement.withGlow(value)));
        rows.add(Row.header("装饰颜色"));
        addDecorationColor("背景颜色", id + ":background", chosen.placement().backgroundColor(), UiTheme.PANEL_RAISED,
                value -> replaceById(id, e -> e.placement = e.placement.withColors(value, e.placement.borderColor(), e.placement.shadowColor(), e.placement.glowColor())));
        addDecorationColor("边框颜色", id + ":border", chosen.placement().borderColor(), chosen.color(),
                value -> replaceById(id, e -> e.placement = e.placement.withColors(e.placement.backgroundColor(), value, e.placement.shadowColor(), e.placement.glowColor())));
        addDecorationColor("阴影颜色", id + ":shadow", chosen.placement().shadowColor(), UiTheme.SHADOW,
                value -> replaceById(id, e -> e.placement = e.placement.withColors(e.placement.backgroundColor(), e.placement.borderColor(), value, e.placement.glowColor())));
        addDecorationColor("发光颜色", id + ":glow", chosen.placement().glowColor(), chosen.color(),
                value -> replaceById(id, e -> e.placement = e.placement.withColors(e.placement.backgroundColor(), e.placement.borderColor(), e.placement.shadowColor(), value)));
        register(new UiCycleButton<>(0, 0, 10, CONTROL, List.of("left", "center", "right"), chosen.placement().alignment(),
                value -> "水平对齐：" + ("left".equals(value) ? "居左" : "right".equals(value) ? "居右" : "居中"),
                value -> replaceByIdDiscrete(id, e -> e.placement = e.placement.withAlignment(value)),
                "模块锚点与文字/进度条标签一起居左、居中或居右。", UiButton.Kind.SECONDARY), CONTROL);
        addSliderPair("横向位置 %", "纵向位置 %", 0, 100,
                () -> intOf(id, ClientHudLayout.CustomElement::xPercent, 50),
                value -> replaceById(id, e -> e.xPercent = value),
                () -> intOf(id, ClientHudLayout.CustomElement::yPercent, 50),
                value -> replaceById(id, e -> e.yPercent = value));
        if (chosen.placement().referenceWidth() > 0) {
            addSliderPair("横向偏移 px", "纵向偏移 px", -1000, 1000,
                    () -> intOf(id, e -> e.placement().offsetX(), 0),
                    value -> replaceById(id, e -> e.placement = e.placement.withOffset(value, e.placement.offsetY())),
                    () -> intOf(id, e -> e.placement().offsetY(), 0),
                    value -> replaceById(id, e -> e.placement = e.placement.withOffset(e.placement.offsetX(), value)));
        }
        addSliderPair("宽度 px", "高度 px", 1, 1000, 1, 400,
                () -> intOf(id, ClientHudLayout.CustomElement::width, 180),
                value -> replaceById(id, e -> e.width = value),
                () -> intOf(id, ClientHudLayout.CustomElement::height, 24),
                value -> replaceById(id, e -> e.height = value));
        addSlider("不透明度 %", 0, 100,
                () -> intOf(id, ClientHudLayout.CustomElement::opacityPercent, 85),
                value -> replaceById(id, e -> e.opacityPercent = value));
        boolean global = activeContext == HudContext.GLOBAL;
        boolean match = activeContext.isMatch() || global;
        boolean search = activeContext == HudContext.SEARCH_DESTROY || global;
        boolean teamDeathmatch = activeContext == HudContext.TEAM_DEATHMATCH || global;
        boolean matching = activeContext == HudContext.MATCHING || global;
        List<String> conditions = new ArrayList<>();
        conditions.add("");
        if (match) {
            conditions.addAll(List.of("feed", "notice", "notice_urgent", "event_active",
                    "team_c", "team_d", "death", "respawning", "respawn_waiting", "respawn_ready",
                    "returned", "alive", "playing", "warmup", "warmup_waiting", "warmup_countdown",
                    "frozen", "round_end", "terrain_restoring", "map_resetting", "match_end",
                    "outside", "spectator", "round_odd", "round_even", "team_leading",
                    "team_trailing", "score_tied", "health_below:50", "health_above:50",
                    "armor_below:10", "kills_at_least:10", "deaths_at_least:5",
                    "phase_remaining_below:10", "boundary_below:5"));
        }
        if (search) {
            conditions.addAll(List.of("buying", "money_below:1000", "money_at_least:1000",
                    "c4_active", "c4_carried", "c4_dropped", "c4_planting", "c4_planted",
                    "c4_defusing", "c4_exploded", "c4_defused"));
        } else if (teamDeathmatch) {
            conditions.add("health_below:50");
        }
        if (matching) {
            conditions.addAll(List.of("forming", "queued"));
        }
        conditions = conditions.stream().distinct().toList();
        conditions = new ArrayList<>(conditions);
        for (var event : cn.blockforge.generated.generatedmod.match.MatchHudEventType.values()) {
            if (match && (search || !isBombEvent(event))) conditions.add("event:" + event.id());
        }
        if (match) {
            for (var descriptor :
                    cn.blockforge.generated.generatedmod.client.ClientHudEventData.descriptors()) {
                if (activeContext != HudContext.GLOBAL
                        && !descriptor.contexts().contains(activeContext)) continue;
                conditions.add("event:" + descriptor.id());
            }
        }
        for (var other : draft.customElements(activeContext)) if (!other.id().equals(id)) conditions.add("hidden:" + other.id());
        conditions.addAll(cn.blockforge.generated.generatedmod.client.HudConditions.ids());
        if (!conditions.contains(chosen.placement().condition())) conditions.add(chosen.placement().condition());
        UiCycleButton<String> condition = new UiCycleButton<>(0, 0, 10, CONTROL,
                conditions, chosen.placement().condition(),
                value -> "显示条件：" + switch (value) {
                    case "feed" -> "击杀播报有效期"; case "team_c" -> "至少三队";
                    case "team_d" -> "至少四队"; case "forming" -> "已成局倒计时";
                    case "notice" -> "比赛内阶段公告"; case "notice_urgent" -> "紧急阶段或 C4";
                    case "event_active" -> "任意 HUD 事件有效";
                    case "c4_active" -> "C4 已发放或安装"; case "c4_carried" -> "C4 正在被携带";
                    case "c4_dropped" -> "C4 已掉落"; case "c4_planting" -> "正在安装 C4";
                    case "c4_planted" -> "C4 已安装"; case "c4_defusing" -> "正在拆除 C4";
                    case "c4_exploded" -> "C4 已引爆"; case "c4_defused" -> "C4 已拆除";
                    case "queued" -> "等待成局"; case "death" -> "死亡后";
                    case "respawning" -> "死亡至回归期间"; case "respawn_waiting" -> "复活倒计时中";
                    case "respawn_ready" -> "复活已就绪"; case "returned" -> "刚刚回归";
                    case "alive" -> "可作战"; case "playing" -> "比赛进行中"; case "warmup" -> "热身中";
                    case "buying" -> "购买阶段";
                    case "frozen" -> "冻结阶段";
                    case "warmup_waiting" -> "热身等待玩家"; case "warmup_countdown" -> "热身开赛倒计时";
                    case "round_end" -> "回合结束"; case "terrain_restoring" -> "地形恢复阶段";
                    case "map_resetting" -> "地图恢复阶段"; case "match_end" -> "比赛结束";
                    case "outside" -> "越界警告中"; case "spectator" -> "观战中";
                    case "round_odd" -> "奇数回合"; case "round_even" -> "偶数回合";
                    case "team_leading" -> "我方领先"; case "team_trailing" -> "我方落后";
                    case "score_tied" -> "比分持平";
                    default -> conditionLabel(value);
                }, value -> replaceByIdDiscrete(id, e -> e.placement = e.placement.withCondition(value)),
                "条件不满足时不绘制此元素；选择始终可取消条件。", UiButton.Kind.SECONDARY);
        register(condition, CONTROL);
        UiEditBox customCondition = new UiEditBox(font, 0, 0, 10, CONTROL,
                Component.literal("自定义条件"));
        customCondition.setMaxLength(128);
        customCondition.setValue(chosen.placement().condition());
        customCondition.setTooltip(Tooltip.create(Component.literal(
                "可直接输入 health_below:25、money_at_least:800、event:... 或 API 条件。")));
        customCondition.setResponder(value -> {
            ClientHudLayout.CustomElement live = customById(id);
            if (live != null && !live.placement().condition().equals(value)) {
                replace(live, e -> e.placement = e.placement.withCondition(value));
            }
        });
        conditionEditor = register(customCondition, CONTROL);
        editors.add(customCondition);
        rows.add(Row.header("出现 / 消失动画"));
        register(new UiCycleButton<>(0, 0, 10, CONTROL, List.of("none", "fade", "slide", "zoom"),
                chosen.placement().animation(), value -> switch (value) {
                    case "fade" -> "淡入淡出"; case "slide" -> "滑入滑出"; case "zoom" -> "缩放渐变"; default -> "无动画";
                }, value -> replaceByIdDiscrete(id, e -> e.placement = e.placement.withAnimation(value, e.placement.animationMillis())),
                "显示条件切换时播放动画。", UiButton.Kind.SECONDARY), CONTROL);
        addSlider("动画时长 ms", 50, 2000, () -> intOf(id, e -> e.placement().animationMillis(), 250),
                value -> replaceById(id, e -> e.placement = e.placement.withAnimation(e.placement.animation(), value)));
        if (textLike || progress) {
            rows.add(Row.header("内容变化过渡"));
            register(new UiCycleButton<>(0, 0, 10, CONTROL, List.of("none", "fade", "slide", "zoom"),
                    chosen.placement().contentAnimation(), value -> switch (value) {
                        case "fade" -> "内容淡入"; case "slide" -> "内容滑入";
                        case "zoom" -> "内容缩放"; default -> "内容无过渡";
                    }, value -> replaceByIdDiscrete(id, e -> e.placement = e.placement
                            .withContentAnimation(value, e.placement.contentAnimationMillis())),
                    "数据值或文字变化时播放的内容过渡。", UiButton.Kind.SECONDARY), CONTROL);
            addSlider("内容过渡 ms", 50, 2000,
                    () -> intOf(id, e -> e.placement().contentAnimationMillis(), 250),
                    value -> replaceById(id, e -> e.placement = e.placement
                            .withContentAnimation(e.placement.contentAnimation(), value)));
        }
        if (chosen.type().equals("progress")) {
            register(new UiCycleButton<>(0, 0, 10, CONTROL, List.of("fill", "drain"),
                    chosen.placement().progressDirection(),
                    value -> "进度效果：" + ("drain".equals(value) ? "消耗（从右向左）" : "填充（从左向右）"),
                    value -> replaceByIdDiscrete(id, e -> e.placement =
                            e.placement.withProgressDirection(value)),
                    "填充适合安装进度，消耗适合复活减伤等倒计时。", UiButton.Kind.SECONDARY), CONTROL);
            addSlider(chosen.boundSource() != null && chosen.boundSource().kind() == HudStats.Kind.TIME
                    ? "进度上限 秒（0=来源）" : "进度上限（0=来源）", 0, 10000,
                    () -> intOf(id, e -> e.placement().progressMaximum(), 0),
                    value -> replaceById(id, e -> e.placement = e.placement.withMaximum(value)));
        }
    }

    private void addParameterHelp() {
        UiButton help = new UiButton(0, 0, 10, CONTROL, Component.literal("内容与参数 · 帮助"), ignored -> {
            commitHistoryEdit();
            minecraft.setScreen(new HudParameterHelpScreen(this, activeContext));
        }, UiButton.Kind.SECONDARY) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                graphics.drawString(font, "内容与参数", getX(), getY() + 7, UiTheme.TEXT, false);
                int x = getX() + Math.min(getWidth() - 22, font.width("内容与参数") + 8);
                graphics.fill(x, getY(), x + 22, getY() + 22, UiTheme.PANEL_RAISED);
                graphics.renderOutline(x, getY(), 22, 22, isHoveredOrFocused() ? UiTheme.ACCENT : UiTheme.BORDER);
                graphics.drawCenteredString(font, "?", x + 11, getY() + 7, UiTheme.TEXT);
            }
        };
        help.setTooltip(Tooltip.create(Component.literal("先写文字，再点“+ 参数”插入实时数据。")));
        register(help, CONTROL);
    }

    /** 颜色行 + 可展开色板（预设网格 / HSV / 十六进制）。 */
    private void addColorRow(ClientHudLayout.CustomElement chosen, String id) {
        colorToggle = new UiButton(0, 0, 10, CONTROL,
                Component.literal(String.format(Locale.ROOT, "颜色 · #%06X（点开色板）", chosen.color() & 0xFFFFFF)),
                ignored -> {
                    commitHistoryEdit();
                    if (paletteTarget.equals(id)) {
                        paletteOpen = !paletteOpen;
                    } else {
                        paletteTarget = id;
                        paletteOpen = true;
                    }
                    rebuildWidgets();
                }, UiButton.Kind.SECONDARY);
        colorToggle.setTooltip(Tooltip.create(Component.literal("色块填充色 / 文字与边框色 / 进度条填充色。")));
        register(colorToggle, CONTROL);
        if (paletteOpen) {
            UiColorPalette palette = new UiColorPalette(font, 0, 0, 10, chosen.color(), value -> {
                replaceById(id, e -> e.color = value);
                if (colorToggle != null) {
                    colorToggle.setMessage(Component.literal(
                            String.format(Locale.ROOT, "颜色 · #%06X（收起色板）", value & 0xFFFFFF)));
                }
            });
            register(palette, UiColorPalette.HEIGHT);
        }
    }

    private void addDecorationColor(String label, String target, int configured, int fallback, IntConsumer setter) {
        int value = configured == 0 ? fallback : configured;
        UiButton button = new UiButton(0, 0, 10, CONTROL,
                Component.literal(String.format(Locale.ROOT, "%s · #%06X", label, value & 0xFFFFFF)), ignored -> {
            paletteTarget = paletteTarget.equals(target) ? "" : target;
            paletteOpen = !paletteTarget.isBlank();
            rebuildWidgets();
        }, UiButton.Kind.SECONDARY);
        register(button, CONTROL);
        if (paletteOpen && paletteTarget.equals(target)) {
            UiColorPalette palette = new UiColorPalette(font, 0, 0, 10, value, selectedColor -> {
                setter.accept(selectedColor);
                applyTransient();
                button.setMessage(Component.literal(String.format(Locale.ROOT, "%s · #%06X", label, selectedColor & 0xFFFFFF)));
            });
            register(palette, UiColorPalette.HEIGHT);
        }
    }

    /** Text modules resolve each {parameter} directly, so they do not need one global data source. */
    private void addParameterInserter(ClientHudLayout.CustomElement chosen) {
        List<HudStats.Source> sources = HudStats.sourcesFor(activeContext);
        if (sources.isEmpty()) return;
        String filterKey = "parameter:" + chosen.id();
        if (!filterKey.equals(sourceFilterKey)) {
            sourceFilterKey = filterKey;
            sourcePrimary = sourceSecondary = sourceTertiary = "全部";
        }
        addSourceFilterControls(sources);
        List<HudStats.Source> filtered = filterSources(sources);
        if (filtered.isEmpty()) {
            rows.add(Row.note("当前三级筛选没有可用参数。"));
            return;
        }
        List<String> options = filtered.stream().map(HudStats.Source::id).toList();
        Map<String, String> labels = new LinkedHashMap<>();
        for (HudStats.Source source : filtered) labels.put(source.id(), sourceHierarchyLabel(source));
        String id = chosen.id();
        register(new UiCycleButton<>(0, 0, 10, CONTROL, options, options.get(0), labels::get,
                value -> {
                    ClientHudLayout.CustomElement live = customById(id);
                    if (live != null && !live.text().contains("{" + value + "}")) {
                        replaceByIdDiscrete(id, e -> e.text = e.text + " {" + value + "}");
                    }
                }, "点击即可把所选数据插入文字；每个参数都能来自不同的数据源。", UiButton.Kind.SECONDARY), CONTROL);
    }

    /** 数据源选择：固定内容或 HudStats 里的任一统计接口（含自定义通道）。 */
    private void addSourcePicker(ClientHudLayout.CustomElement chosen) {
        rows.add(Row.header("数据来源"));
        List<HudStats.Source> sources = HudStats.sourcesFor(activeContext);
        String filterKey = "source:" + chosen.id();
        HudStats.Source currentSource = chosen.boundSource();
        if (!filterKey.equals(sourceFilterKey)) {
            sourceFilterKey = filterKey;
            sourcePrimary = currentSource == null ? "全部" : HudStats.primaryCategory(currentSource);
            sourceSecondary = currentSource == null ? "全部" : HudStats.secondaryCategory(currentSource);
            sourceTertiary = currentSource == null ? "全部" : HudStats.tertiaryCategory(currentSource);
        }
        addSourceFilterControls(sources);
        List<HudStats.Source> filtered = filterSources(sources);
        List<String> options = new ArrayList<>();
        Map<String, String> labelsByOption = new LinkedHashMap<>();
        options.add(FIXED_SOURCE);
        labelsByOption.put(FIXED_SOURCE, "固定内容");
        String wanted = chosen.source();
        for (HudStats.Source source : filtered) {
            if ("progress".equals(chosen.type()) && source.kind() == HudStats.Kind.TEXT) {
                continue;
            }
            // 用稳定 id 作为循环值，避免两个统计源同名时互相覆盖。
            if (labelsByOption.containsKey(source.id())) {
                continue;
            }
            options.add(source.id());
            labelsByOption.put(source.id(), sourceHierarchyLabel(source));
        }
        // 配置文件里如果还绑定着已下线的源，也要保留这一项，不能第一次点击就清空。
        if (!wanted.isBlank() && !labelsByOption.containsKey(wanted)) {
            options.add(wanted);
            labelsByOption.put(wanted, "（源缺失）" + wanted);
        }
        String id = chosen.id();
        String selectedOption = wanted.isBlank() ? FIXED_SOURCE : wanted;
        UiCycleButton<String> cycle = new UiCycleButton<>(0, 0, 10, CONTROL, options, selectedOption,
                value -> labelsByOption.getOrDefault(value, value),
                value -> replaceByIdDiscrete(id,
                        e -> e.source = FIXED_SOURCE.equals(value) ? "" : value),
                "选择当前场景的数据；固定内容不绑定统计值。",
                UiButton.Kind.SECONDARY);
        register(cycle, CONTROL);
    }

    private void addSourceFilterControls(List<HudStats.Source> sources) {
        List<String> primary = new ArrayList<>();
        primary.add("全部");
        sources.stream().map(HudStats::primaryCategory).distinct().sorted().forEach(primary::add);
        if (!primary.contains(sourcePrimary)) sourcePrimary = "全部";
        register(new UiCycleButton<>(0, 0, 10, CONTROL, primary, sourcePrimary,
                value -> "一级 · 指标：" + value,
                value -> {
                    sourcePrimary = value;
                    sourceSecondary = "全部";
                    sourceTertiary = "全部";
                    rebuildWidgets();
                }, "先按数据指标筛选。", UiButton.Kind.SECONDARY), CONTROL);

        List<HudStats.Source> primaryFiltered = sources.stream()
                .filter(source -> sourcePrimary.equals("全部")
                        || HudStats.primaryCategory(source).equals(sourcePrimary)).toList();
        List<String> secondary = new ArrayList<>();
        secondary.add("全部");
        primaryFiltered.stream().map(HudStats::secondaryCategory).distinct().sorted().forEach(secondary::add);
        if (!secondary.contains(sourceSecondary)) sourceSecondary = "全部";
        register(new UiCycleButton<>(0, 0, 10, CONTROL, secondary, sourceSecondary,
                value -> "二级 · 范围：" + value,
                value -> {
                    sourceSecondary = value;
                    sourceTertiary = "全部";
                    rebuildWidgets();
                }, "再按本轮、整场、自己等范围筛选。", UiButton.Kind.SECONDARY), CONTROL);

        List<HudStats.Source> secondaryFiltered = primaryFiltered.stream()
                .filter(source -> sourceSecondary.equals("全部")
                        || HudStats.secondaryCategory(source).equals(sourceSecondary)).toList();
        List<String> tertiary = new ArrayList<>();
        tertiary.add("全部");
        secondaryFiltered.stream().map(HudStats::tertiaryCategory).distinct().sorted().forEach(tertiary::add);
        if (!tertiary.contains(sourceTertiary)) sourceTertiary = "全部";
        register(new UiCycleButton<>(0, 0, 10, CONTROL, tertiary, sourceTertiary,
                value -> "三级 · 对象：" + value,
                value -> {
                    sourceTertiary = value;
                    rebuildWidgets();
                }, "最后按A队、B队、自己、敌方等对象筛选。", UiButton.Kind.SECONDARY), CONTROL);
    }

    private List<HudStats.Source> filterSources(List<HudStats.Source> sources) {
        return sources.stream().filter(source -> sourcePrimary.equals("全部")
                        || HudStats.primaryCategory(source).equals(sourcePrimary))
                .filter(source -> sourceSecondary.equals("全部")
                        || HudStats.secondaryCategory(source).equals(sourceSecondary))
                .filter(source -> sourceTertiary.equals("全部")
                        || HudStats.tertiaryCategory(source).equals(sourceTertiary)).toList();
    }

    private static String sourceHierarchyLabel(HudStats.Source source) {
        return "【" + HudStats.primaryCategory(source) + "】→【"
                + HudStats.secondaryCategory(source) + "】→【"
                + HudStats.tertiaryCategory(source) + "】";
    }

    private void addTemplateEditor(String label, java.util.function.Supplier<String> getter,
                                   Consumer<String> setter, String tooltip) {
        UiEditBox editor = new UiEditBox(font, 0, 0, 10, CONTROL, Component.literal(label));
        editor.setMaxLength(128);
        editor.setValue(getter.get() == null ? "" : getter.get());
        editor.setResponder(value -> {
            if (!value.equals(getter.get())) {
                setter.accept(value);
                applyTransient();
            }
        });
        editor.setTooltip(Tooltip.create(Component.literal(tooltip)));
        register(editor, CONTROL);
        editors.add(editor);
    }

    private void addBuiltInColor(String label, IntSupplier getter, IntConsumer setter) {
        int value = getter.getAsInt();
        String target = "builtin:" + selected;
        UiButton button = new UiButton(0, 0, 10, CONTROL,
                Component.literal(String.format(Locale.ROOT, "%s · #%06X", label, value & 0xFFFFFF)),
                ignored -> {
                    if (paletteTarget.equals(target)) {
                        paletteOpen = !paletteOpen;
                    } else {
                        paletteTarget = target;
                        paletteOpen = true;
                    }
                    rebuildWidgets();
                }, UiButton.Kind.SECONDARY);
        register(button, CONTROL);
        if (paletteOpen && paletteTarget.equals(target)) {
            UiColorPalette palette = new UiColorPalette(font, 0, 0, 10, value, selectedColor -> {
                setter.accept(selectedColor);
                applyTransient();
                button.setMessage(Component.literal(String.format(Locale.ROOT, "%s · #%06X", label,
                        selectedColor & 0xFFFFFF)));
            });
            register(palette, UiColorPalette.HEIGHT);
        }
    }

    private void addTextEditor(ClientHudLayout.CustomElement chosen) {
        UiEditBox editor = new UiEditBox(font, 0, 0, 10, CONTROL, Component.literal("模块文字"));
        editor.setMaxLength(256);
        editor.setValue(chosen.text());
        editor.setResponder(value -> {
            ClientHudLayout.CustomElement live = customById(chosen.id());
            if (live != null && !live.text().equals(value)) {
                replace(live, e -> e.text = value);
            }
        });
        editor.setTooltip(Tooltip.create(Component.literal("输入：回合 {round}，显示：回合 1。固定文字不追加数值；点 ? 查看参数。")));
        activeEditor = register(editor, CONTROL);
        editors.add(editor);
    }

    private void addImageFileCycle(ClientHudLayout.CustomElement chosen) {
        List<String> options = new ArrayList<>();
        Map<String, String> labelsByOption = new LinkedHashMap<>();
        options.add("");
        labelsByOption.put("", "未选择");
        String wanted = chosen.text();
        for (String fileName : HudBackground.availableImages()) {
            if (!labelsByOption.containsKey(fileName)) {
                options.add(fileName);
                labelsByOption.put(fileName, fileName);
            }
        }
        // 配置里若仍指向已经删除的图片，保留这个值，避免第一次点击循环按钮时误清空。
        if (!wanted.isBlank() && !labelsByOption.containsKey(wanted)) {
            options.add(wanted);
            labelsByOption.put(wanted, "（缺图）" + wanted);
        }
        String id = chosen.id();
        UiCycleButton<String> cycle = new UiCycleButton<>(0, 0, 10, CONTROL, options, wanted,
                value -> "图片：" + labelsByOption.getOrDefault(value, value),
                value -> replaceByIdDiscrete(id, e -> e.text = value),
                "把 PNG 放进 .minecraft/config/fpsmod/hud_images，底部「刷新图片」后可选。",
                UiButton.Kind.SECONDARY);
        register(cycle, CONTROL);
    }

    // ---------------------------------------------------------------- 背景与全局

    private void addGlobalControls() {
        rows.add(Row.header("背景与参考线"));
        addToggle("显示参考线", () -> draft.global.guidesVisible, value -> draft.global.guidesVisible = value);
        addBackgroundCycle();
        addSlider("背景不透明度 %", 0, 100,
                () -> draft.global.backgroundOpacityPercent,
                value -> draft.global.backgroundOpacityPercent = value);
        rows.add(Row.note("背景 PNG 放入 .minecraft/config/fpsmod/hud_images 后点「刷新图片」。"));
        rows.add(new Row(null, "每个场景（含正在匹配 / 房间中）都可另外添加「图片模块」作局部背景。",
                RowKind.NOTE, NOTE_ROW * 2, UNGROUPED_LINE, 0, 1));
        rows.add(Row.header("统计接口（供模块绑定）"));
        rows.add(Row.note("进度条与文字模块的属性里可任选下列数据源："));
        rows.add(Row.note("· 比赛：比分 / 双方与个人击杀 / 对敌人伤害 / 承伤 / KD / 时间 / 胜利进度"));
        rows.add(Row.note("· 正在匹配：队列人数、序位、等待时间、开赛倒计时与横幅两行原文"));
        rows.add(Row.note("· 房间中：房间名、模式、地图、人数进度、房主、状态与横幅两行原文"));
        rows.add(Row.note("· 玩家状态：生命 / 护甲 / 氧气 / 延迟 / FPS / 名字"));
        rows.add(Row.note("· 自定义通道：/fps hudstat set <标识> <数值> <上限>（如 C4 安装进度）"));
    }

    private void addBackgroundCycle() {
        List<String> options = new ArrayList<>();
        options.add("无");
        options.addAll(HudBackground.availableImages());
        String selected = options.contains(draft.global.backgroundFile) ? draft.global.backgroundFile : "无";
        UiCycleButton<String> cycle = new UiCycleButton<>(0, 0, 10, CONTROL, options, selected,
                name -> "背景图：" + name, value -> editDiscrete(() ->
                draft.global.backgroundFile = "无".equals(value) ? "" : value),
                "整屏背景，五个场景共用。", UiButton.Kind.SECONDARY);
        register(cycle, CONTROL);
    }

    // ---------------------------------------------------------------- 底部：参考模组式的安全操作栏

    private void addFooter() {
        int columns = 3;
        int firstRow = dockBottom - CONTROL * FOOTER_ROWS - 14;
        addFooterButton("撤销", 0, firstRow, this::undo, UiButton.Kind.SECONDARY,
                "撤回最近一次设置变化（Ctrl+Z）。").historyIcon(false);
        addFooterButton("重做", 1, firstRow, this::redo, UiButton.Kind.SECONDARY,
                "恢复刚才撤回的设置（Ctrl+Y）。").historyIcon(true);
        addFooterButton("当前页默认", 2, firstRow, this::resetCurrentContext, UiButton.Kind.WARNING,
                "只恢复当前页，不影响其他场景。");

        int secondRow = firstRow + CONTROL + 4;
        addFooterButton("刷新图片", 0, secondRow, () -> {
            rebuildWidgets();
            setStatus("图片与数据源列表已刷新。", UiTheme.INFO);
        }, UiButton.Kind.SECONDARY, "重新扫描 hud_images 目录与自定义统计通道。");
        addFooterButton("全部默认", 1, secondRow, this::resetAll, UiButton.Kind.DANGER,
                "恢复所有场景、背景和独立模块；关闭前仍可撤销。");
        closeButton = addFooterButton(dirty ? "保存并关闭" : "完成", 2, secondRow, this::requestClose,
                UiButton.Kind.PRIMARY, "保存并关闭；如果有未保存改动会先询问。");
    }

    private UiButton addFooterButton(String label, int column, int y, Runnable action,
                                     UiButton.Kind kind, String tooltip) {
        int columns = 3;
        UiButton button = new UiButton(0, y, 10, CONTROL, Component.literal(label), ignored -> action.run(), kind);
        button.setTooltip(Tooltip.create(Component.literal(tooltip)));
        button.setX(footerColumnX(column, columns));
        button.setWidth(footerColumnWidth(columns));
        footerButtons.add(button);
        addRenderableWidget(button);
        return button;
    }

    private int footerColumnWidth(int columns) {
        int gap = 6;
        return Math.max(24, (dockInnerWidth() - gap * (columns - 1)) / columns);
    }

    private int footerColumnX(int column, int columns) {
        int gap = 6;
        return dockX + 8 + column * (footerColumnWidth(columns) + gap);
    }

    /** 面板拖动后，底部固定操作栏也必须跟着面板走。 */
    private void layoutFooterButtons() {
        int columns = 3;
        int firstRow = dockBottom - CONTROL * FOOTER_ROWS - 14;
        int secondRow = firstRow + CONTROL + 4;
        for (int index = 0; index < footerButtons.size(); index++) {
            UiButton button = footerButtons.get(index);
            int row = index / columns;
            int column = index % columns;
            button.setX(footerColumnX(column, columns));
            button.setY((row == 0 ? firstRow : secondRow));
            button.setWidth(footerColumnWidth(columns));
        }
    }

    // ---------------------------------------------------------------- 行登记与排版

    private <T extends AbstractWidget> T register(AbstractWidget widget, int height) {
        register(widget, height, 0, 1);
        @SuppressWarnings("unchecked") T typed = (T) widget;
        return typed;
    }

    private void register(AbstractWidget widget, int height, int line, int slot, int columns) {
        rows.add(new Row(widget, null, RowKind.WIDGET, height, line, slot, Math.max(1, columns)));
        widgets.add(widget);
        addRenderableWidget(widget);
    }

    private void register(AbstractWidget widget, int height, int slot, int columns) {
        rows.add(new Row(widget, null, RowKind.WIDGET, height, UNGROUPED_LINE, slot, Math.max(1, columns)));
        widgets.add(widget);
        addRenderableWidget(widget);
    }

    private int beginLayoutLine() {
        return ++nextLayoutLine;
    }

    private int dockInnerWidth() {
        // Reserve a dedicated gutter for the scrollbar so controls never overlap it.
        return Math.max(40, dockWidth - 30);
    }

    private int viewportTop() {
        return dockTop + DOCK_HEADER_HEIGHT + CONTROL + 8;
    }

    /** 内容视口止于底栏分隔线，避免最后一行控件压到固定操作区。 */
    private int viewportBottom() {
        return dockBottom - CONTROL * FOOTER_ROWS - 18;
    }

    private int contentHeight() {
        int total = TOP_INSET;
        int previousLine = Integer.MIN_VALUE;
        for (Row row : rows) {
            if (row.line() != UNGROUPED_LINE && row.line() == previousLine) {
                continue;
            }
            total += row.height() + ROW_GAP;
            previousLine = row.line();
        }
        return total;
    }

    private void clampScroll() {
        int overflow = Math.max(0, contentHeight() - (viewportBottom() - viewportTop()));
        scroll = Math.max(0, Math.min(scroll, overflow));
    }

    private void layoutRows() {
        clampScroll();
        int y = viewportTop() + TOP_INSET - scroll;
        int gap = 6;
        int previousLine = Integer.MIN_VALUE;
        int previousLineY = y;
        for (Row row : rows) {
            boolean sharesPreviousLine = row.line() != UNGROUPED_LINE && row.line() == previousLine;
            int rowY = sharesPreviousLine ? previousLineY : y;
            if (row.widget() != null) {
                AbstractWidget widget = row.widget();
                int columns = Math.max(1, row.columns());
                int each = columns == 1 ? dockInnerWidth()
                        : (dockInnerWidth() - gap * (columns - 1)) / columns;
                widget.setX(dockX + 8 + row.slot() * (each + gap));
                widget.setWidth(Math.max(24, row.slot() == columns - 1
                        ? dockInnerWidth() - (each + gap) * (columns - 1) : each));
                widget.setY(row.kind() == RowKind.WIDGET && row.height() == SLIDER_ROW
                        ? rowY + 13 : rowY);
                widget.visible = rowY >= viewportTop() - 1
                        && rowY + row.height() <= viewportBottom() + 1;
                if (widget instanceof UiColorPalette palette) {
                    palette.setViewport(viewportTop(), viewportBottom());
                    widget.visible = rowY < viewportBottom() && rowY + row.height() > viewportTop();
                }
            }
            if (!sharesPreviousLine) {
                previousLineY = rowY;
                y += row.height() + ROW_GAP;
            }
            previousLine = row.line();
        }
    }

    private void addSlider(String name, int minimum, int maximum, IntSupplier getter, IntConsumer setter) {
        UiSlider slider = new UiSlider(font, 0, 0, 10, 13, minimum, maximum, getter, value -> {
            setter.accept(value);
            applyTransient();
        }, name, "");
        register(slider, SLIDER_ROW);
    }

    /** 两个滑条并排一行：横向 / 纵向位置，宽 / 高等成对设置。 */
    private void addSliderPair(String firstName, String secondName, int firstMinimum, int firstMaximum,
                               int secondMinimum, int secondMaximum,
                               IntSupplier firstGetter, IntConsumer firstSetter,
                               IntSupplier secondGetter, IntConsumer secondSetter) {
        UiSlider first = new UiSlider(font, 0, 0, 10, 13, firstMinimum, firstMaximum, firstGetter, value -> {
            firstSetter.accept(value);
            applyTransient();
        }, firstName, "");
        UiSlider second = new UiSlider(font, 0, 0, 10, 13, secondMinimum, secondMaximum, secondGetter, value -> {
            secondSetter.accept(value);
            applyTransient();
        }, secondName, "");
        int line = beginLayoutLine();
        register(first, SLIDER_ROW, line, 0, 2);
        register(second, SLIDER_ROW, line, 1, 2);
    }

    /** 单参数版滑条对（上下限相同位置的常用情况）。 */
    private void addSliderPair(String firstName, String secondName, int minimum, int maximum,
                               IntSupplier firstGetter, IntConsumer firstSetter,
                               IntSupplier secondGetter, IntConsumer secondSetter) {
        addSliderPair(firstName, secondName, minimum, maximum, minimum, maximum,
                firstGetter, firstSetter, secondGetter, secondSetter);
    }

    private void addToggle(String label, java.util.function.BooleanSupplier getter, Consumer<Boolean> setter) {
        UiCycleButton<Boolean> toggle = new UiCycleButton<>(0, 0, 10, CONTROL,
                List.of(Boolean.TRUE, Boolean.FALSE), getter.getAsBoolean(),
                flag -> label + "：" + (flag ? "开" : "关"), value -> {
            editDiscrete(() -> setter.accept(Boolean.TRUE.equals(value)));
            rebuildWidgets();
        }, null, UiButton.Kind.SECONDARY);
        register(toggle, CONTROL);
    }

    // ---------------------------------------------------------------- 模块草稿

    private interface Edit {
        void apply(CustomDraft draft);
    }

    private static final class CustomDraft {
        String text;
        int xPercent;
        int yPercent;
        int width;
        int height;
        int opacityPercent;
        int color;
        boolean visible;
        String source;
        int scalePercent;
        boolean background;
        boolean border;
        boolean shadow;
        ClientHudLayout.Placement placement;

        CustomDraft(ClientHudLayout.CustomElement source) {
            text = source.text();
            xPercent = source.xPercent();
            yPercent = source.yPercent();
            width = source.width();
            height = source.height();
            opacityPercent = source.opacityPercent();
            color = source.color();
            visible = source.visible();
            this.source = source.source();
            scalePercent = source.scalePercent();
            background = source.background();
            border = source.border();
            shadow = source.shadow();
            placement = source.placement();
        }
    }

    private void replace(ClientHudLayout.CustomElement old, Edit edit) {
        if (replaceRaw(old, edit)) {
            applyTransient();
        }
    }

    /** 替换模块但不触发运行态更新，供需要“一次操作一个撤销点”的控件使用。 */
    private boolean replaceRaw(ClientHudLayout.CustomElement old, Edit edit) {
        if (old == null) {
            return false;
        }
        CustomDraft mirror = new CustomDraft(old);
        edit.apply(mirror);
        boolean placementChanged = mirror.placement.offsetX() != old.placement().offsetX()
                || mirror.placement.offsetY() != old.placement().offsetY()
                || !mirror.placement.condition().equals(old.placement().condition());
        if (placementChanged) mirror.placement = mirror.placement.withMaximum(old.placement().progressMaximum());
        if (mirror.placement.animation().equals("none") && !old.placement().animation().equals("none")
                && (mirror.placement.offsetX() != old.placement().offsetX() || mirror.placement.offsetY() != old.placement().offsetY()
                || !mirror.placement.condition().equals(old.placement().condition()))) {
            mirror.placement = mirror.placement.withAnimation(old.placement().animation(), old.placement().animationMillis());
        }
        draft.replaceCustomElement(activeContext, new ClientHudLayout.CustomElement(old.id(),
                old.type(), mirror.text, mirror.xPercent, mirror.yPercent, mirror.width,
                mirror.height, mirror.opacityPercent, mirror.color, mirror.visible,
                mirror.source, mirror.scalePercent, mirror.background, mirror.border, mirror.shadow, mirror.placement));
        return true;
    }

    private void replaceById(String id, Edit edit) {
        replace(customById(id), edit);
    }

    private void replaceByIdDiscrete(String id, Edit edit) {
        editDiscrete(() -> replaceRaw(customById(id), edit));
    }

    private int intOf(String id, java.util.function.ToIntFunction<ClientHudLayout.CustomElement> reader,
                      int fallback) {
        ClientHudLayout.CustomElement live = customById(id);
        return live == null ? fallback : reader.applyAsInt(live);
    }

    private boolean boolOf(String id, java.util.function.Predicate<ClientHudLayout.CustomElement> reader) {
        ClientHudLayout.CustomElement live = customById(id);
        return live != null && reader.test(live);
    }

    private void addElement(String type) {
        String id = uniqueId(type);
        ClientHudLayout.CustomElement element = switch (type) {
            case "block" -> ClientHudLayout.CustomElement.block(id);
            case "progress" -> ClientHudLayout.CustomElement.progress(id);
            case "image" -> ClientHudLayout.CustomElement.image(id,
                    HudBackground.availableImages().isEmpty() ? ""
                            : HudBackground.availableImages().get(0));
            case "respawn" -> ClientHudLayout.CustomElement.respawn(id);
            case "boundary" -> ClientHudLayout.CustomElement.boundary(id);
            default -> ClientHudLayout.CustomElement.text(id);
        };
        editDiscrete(() -> draft.addCustomElement(activeContext, element));
        selected = "custom:" + id;
        propertyTab = true;
        paletteOpen = false;
        rebuildWidgets();
        setStatus("已添加 " + element.displayName() + "，在画面中拖动它。", UiTheme.SUCCESS);
    }

    private String uniqueId(String type) {
        int index = 1;
        while (customById(type + "_" + index) != null) {
            index++;
        }
        return type + "_" + index;
    }

    private void applyTransient() {
        ClientHudLayout.Snapshot next = draft.build();
        String error = ClientHudLayout.updateTransient(next);
        if (error != null) {
            setStatus(error, UiTheme.ERROR);
            return;
        }
        dirty = !sameSnapshot(next, savedSnapshot);
        if (closeButton != null) {
            closeButton.setMessage(Component.literal(dirty ? "保存并关闭" : "完成"));
        }
        if (!historySuspended && !sameSnapshot(next, historyBaseline)) {
            historyPending = true;
        }
        lastHistoryChange = System.currentTimeMillis();
    }

    /** 把当前连续编辑事务收口为一个撤销点；没有实际变化时不会制造空记录。 */
    private void commitHistoryEdit() {
        ClientHudLayout.Snapshot current = draft.build();
        if (!historySuspended && historyPending && !sameSnapshot(current, historyBaseline)) {
            undoStack.push(historyBaseline);
            while (undoStack.size() > 40) {
                undoStack.removeLast();
            }
            redoStack.clear();
            historyBaseline = current;
        } else if (sameSnapshot(current, historyBaseline)) {
            historyBaseline = current;
        }
        historyPending = false;
        lastHistoryChange = System.currentTimeMillis();
    }

    /** 执行一次离散设置并立刻收口，保证开关、预设等操作各占一个撤销点。 */
    private void editDiscrete(Runnable mutation) {
        commitHistoryEdit();
        mutation.run();
        applyTransient();
        commitHistoryEdit();
    }

    private boolean sameSnapshot(ClientHudLayout.Snapshot first, ClientHudLayout.Snapshot second) {
        if (first == null || second == null) return first == second;
        if (!first.global().equals(second.global())) return false;
        for (HudContext context : HudContext.values()) {
            if (!first.elements(context).equals(second.elements(context))) return false;
            if (!first.customElements(context).equals(second.customElements(context))) return false;
        }
        return true;
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();
        init();
    }

    private void restoreSnapshot(ClientHudLayout.Snapshot snapshot, String message) {
        if (snapshot == null) return;
        draft = snapshot.draft();
        paletteOpen = false;
        historySuspended = true;
        applyTransient();
        historySuspended = false;
        historyPending = false;
        historyBaseline = snapshot;
        dirty = !sameSnapshot(snapshot, savedSnapshot);
        rebuildWidgets();
        setStatus(message, UiTheme.SUCCESS);
    }

    private void undo() {
        commitHistoryEdit();
        if (undoStack.isEmpty()) {
            setStatus("没有可撤销的改动。", UiTheme.SUBTLE);
            return;
        }
        redoStack.push(draft.build());
        restoreSnapshot(undoStack.pop(), "已撤销最近一次改动。");
    }

    private void redo() {
        commitHistoryEdit();
        if (redoStack.isEmpty()) {
            setStatus("没有可重做的改动。", UiTheme.SUBTLE);
            return;
        }
        undoStack.push(draft.build());
        restoreSnapshot(redoStack.pop(), "已重做改动。");
    }

    private void resetCurrentContext() {
        editDiscrete(() -> {
            EnumMap<HudContext, ClientHudLayout.Elements> contexts = new EnumMap<>(HudContext.class);
            EnumMap<HudContext, List<ClientHudLayout.CustomElement>> modules = new EnumMap<>(HudContext.class);
            for (HudContext context : HudContext.values()) {
                contexts.put(context, draft.elements(context));
                modules.put(context, draft.customElements(context));
            }
            contexts.put(activeContext, ClientHudLayout.Elements.defaultsFor(activeContext));
            modules.put(activeContext, List.of());
            draft = new ClientHudLayout.Snapshot(draft.global.build(), contexts, modules).draft();
            cn.blockforge.generated.generatedmod.client.HudAssemblies.splitAll(draft, activeContext);
        });
        rebuildWidgets();
        setStatus("当前场景已恢复默认。可以用撤销退回。", UiTheme.SUCCESS);
    }

    private void resetAll() {
        editDiscrete(() -> {
            draft = new ClientHudLayout.Snapshot(ClientHudLayout.Global.defaults(), Map.of(), Map.of()).draft();
            for (HudContext context : HudContext.values()) {
                cn.blockforge.generated.generatedmod.client.HudAssemblies.splitAll(draft, context);
            }
        });
        rebuildWidgets();
        setStatus("全部场景已恢复默认。可以用撤销退回。", UiTheme.SUCCESS);
    }

    private void applyPreset(String preset) {
        editDiscrete(() -> {
            ClientHudLayout.Mutable values = current();
            switch (preset) {
                case "compact" -> {
                    if (activeContext.isMatch()) {
                        values.scoreXPercent = 50; values.scoreYPercent = 3; values.scoreWidth = 280; values.scoreScalePercent = 85;
                        values.textXPercent = 50; values.textYPercent = 78; values.textScalePercent = 85;
                        values.feedXPercent = 82; values.feedYPercent = 42; values.feedScalePercent = 85;
                    } else {
                        values.bannerXPercent = 50; values.bannerYPercent = 10; values.bannerScalePercent = 85;
                    }
                }
                case "wide" -> {
                    if (activeContext.isMatch()) {
                        values.scoreXPercent = 50; values.scoreYPercent = 3; values.scoreWidth = 480; values.scoreScalePercent = 110;
                        values.textXPercent = 50; values.textYPercent = 78; values.textScalePercent = 110;
                        values.feedXPercent = 78; values.feedYPercent = 42; values.feedScalePercent = 100;
                    } else {
                        values.bannerXPercent = 50; values.bannerYPercent = 16; values.bannerScalePercent = 110;
                    }
                }
                default -> {
                    if (activeContext.isMatch()) {
                        values.scoreXPercent = 50; values.scoreYPercent = 3; values.scoreWidth = 340; values.scoreScalePercent = 100;
                        values.textXPercent = 50; values.textYPercent = 78; values.textScalePercent = 100;
                        values.feedXPercent = 80; values.feedYPercent = 42; values.feedScalePercent = 100;
                    } else {
                        values.bannerXPercent = 50; values.bannerYPercent = 12; values.bannerScalePercent = 100;
                    }
                }
            }
        });
        rebuildWidgets();
        setStatus("已应用“" + ("compact".equals(preset) ? "紧凑" : "wide".equals(preset) ? "宽屏" : "居中") + "”布局预设。", UiTheme.SUCCESS);
    }

    private void setStatus(String message, int color) {
        status = message;
        statusColor = color;
        statusTicks = 120;
    }

    // ---------------------------------------------------------------- 交互

    private boolean insideDock(double mouseX, double mouseY) {
        return !dockHidden && mouseX >= dockX && mouseX <= dockX + dockWidth
                && mouseY >= dockTop && mouseY <= dockBottom;
    }

    private boolean insideTabBar(double mouseX, double mouseY) {
        if (dockHidden) return mouseX >= width - 28 && mouseX <= width - 6
                && mouseY >= height - 28 && mouseY <= height - 6;
        return mouseX >= 6 && mouseX <= width - 6
                && mouseY >= TAB_TOP && mouseY <= TAB_TOP + TAB_HEIGHT;
    }

    /** 右侧面板只有标题栏负责移动，内容控件仍保留各自的点击和滚轮行为。 */
    private boolean insideDockHeader(double mouseX, double mouseY) {
        return insideDock(mouseX, mouseY)
                && mouseY >= dockTop && mouseY <= dockTop + DOCK_HEADER_HEIGHT;
    }

    private ResizeEdge dockResizeEdge(double mouseX, double mouseY) {
        if (dockHidden) return ResizeEdge.NONE;
        int cornerHandle = 8;
        boolean left = mouseX >= dockX - 2 && mouseX <= dockX + cornerHandle;
        boolean right = mouseX >= dockX + dockWidth - cornerHandle && mouseX <= dockX + dockWidth + 2;
        // 边缘只占外框附近几像素，不能覆盖标题栏和内容控件的点击区域。
        boolean top = mouseY >= dockTop - 2 && mouseY <= dockTop + 3;
        boolean bottom = mouseY >= dockBottom - 3 && mouseY <= dockBottom + 2;
        if (left && top) return ResizeEdge.TOP_LEFT;
        if (right && top) return ResizeEdge.TOP_RIGHT;
        if (left && bottom) return ResizeEdge.BOTTOM_LEFT;
        if (right && bottom) return ResizeEdge.BOTTOM_RIGHT;
        if (left && mouseY >= dockTop + cornerHandle && mouseY <= dockBottom - cornerHandle) return ResizeEdge.LEFT;
        if (right && mouseY >= dockTop + cornerHandle && mouseY <= dockBottom - cornerHandle) return ResizeEdge.RIGHT;
        if (top && mouseX >= dockX + cornerHandle && mouseX <= dockX + dockWidth - cornerHandle) return ResizeEdge.TOP;
        if (bottom && mouseX >= dockX + cornerHandle && mouseX <= dockX + dockWidth - cornerHandle) return ResizeEdge.BOTTOM;
        return ResizeEdge.NONE;
    }

    private void resizeDock(double mouseX, double mouseY) {
        if (resizeEdge == ResizeEdge.NONE) {
            return;
        }
        int minimumTop = TAB_TOP + TAB_HEIGHT + 2;
        int minimumWidth = Math.min(DOCK_MIN_WIDTH, Math.max(200, width - 40));
        int maximumBottom = height - 10;
        int minimumBottom = minimumTop + DOCK_MIN_HEIGHT;
        int startRight = resizeOriginX + resizeOriginWidth;
        int nextLeft = resizeOriginX;
        int nextRight = startRight;
        int nextTop = resizeOriginTop;
        int nextBottom = resizeOriginBottom;
        int maximumWidth = Math.max(minimumWidth, Math.min(DOCK_MAX_WIDTH, width - 12));
        if (resizeEdge.left) {
            nextLeft = clamp((int) Math.round(mouseX - dockResizeGrabX),
                    startRight - maximumWidth, startRight - minimumWidth);
        }
        if (resizeEdge.right) {
            nextRight = clamp((int) Math.round(mouseX + dockResizeGrabX),
                    resizeOriginX + minimumWidth, Math.min(width - 6, resizeOriginX + maximumWidth));
        }
        if (resizeEdge.top) {
            nextTop = clamp((int) Math.round(mouseY - dockResizeGrabY),
                    minimumTop, resizeOriginBottom - DOCK_MIN_HEIGHT);
        }
        if (resizeEdge.bottom) {
            nextBottom = clamp((int) Math.round(mouseY + dockResizeGrabY),
                    resizeOriginTop + DOCK_MIN_HEIGHT, maximumBottom);
        }
        dockX = nextLeft;
        dockWidth = nextRight - nextLeft;
        dockTop = nextTop;
        dockBottom = nextBottom;
        clampScroll();
        layoutRows();
        layoutFooterButtons();
    }

    private void moveDock(double mouseX, double mouseY) {
        int panelHeight = dockBottom - dockTop;
        int nextX = (int) Math.round(mouseX - dockGrabX);
        int nextTop = (int) Math.round(mouseY - dockGrabY);
        dockX = clamp(nextX, 6, Math.max(6, width - dockWidth - 6));
        int minimumTop = TAB_TOP + TAB_HEIGHT + 2;
        int maximumTop = Math.max(minimumTop, height - panelHeight - 10);
        dockTop = clamp(nextTop, minimumTop, maximumTop);
        dockBottom = dockTop + panelHeight;
        layoutRows();
        layoutFooterButtons();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (choices.click(mouseX, mouseY, button)) return true;
        if (exitPromptOpen) {
            if (button == 0 && handleExitPromptClick(mouseX, mouseY)) {
                return true;
            }
            return true;
        }
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        // 新的鼠标手势先收口上一个连续编辑，避免两次拖动/点击合并成一个撤销点。
        commitHistoryEdit();
        ResizeEdge edge = dockResizeEdge(mouseX, mouseY);
        if (edge != ResizeEdge.NONE) {
            resizeEdge = edge;
            resizeOriginX = dockX;
            resizeOriginTop = dockTop;
            resizeOriginWidth = dockWidth;
            resizeOriginBottom = dockBottom;
            dockResizeGrabX = edge.left ? mouseX - dockX : edge.right ? dockX + dockWidth - mouseX : 0;
            dockResizeGrabY = edge.top ? mouseY - dockTop : edge.bottom ? dockBottom - mouseY : 0;
            setStatus("正在调整 HUD 配置面板大小。", UiTheme.ACCENT);
            return true;
        }
        if (insideDockHeader(mouseX, mouseY)) {
            draggingDock = true;
            dockGrabX = mouseX - dockX;
            dockGrabY = mouseY - dockTop;
            setStatus("正在移动 HUD 配置面板。", UiTheme.ACCENT);
            return true;
        }
        if (beginPanelScrollbarDrag(mouseX, mouseY)) return true;
        if (insideTabBar(mouseX, mouseY) || insideDock(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        ClientHudLayout.Elements values = draft.elements(activeContext);
        if (pickSelectedWindow(values, mouseX, mouseY)) {
            return true;
        }
        if (pickCustomElement(mouseX, mouseY)) {
            return true;
        }
        if (pickBuiltInWindow(values, mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleExitPromptClick(double mouseX, double mouseY) {
        int boxWidth = Math.min(390, width - 24);
        int boxHeight = 112;
        int left = (width - boxWidth) / 2;
        int top = (height - boxHeight) / 2;
        int gap = 6;
        int buttonWidth = (boxWidth - 24 - gap * 2) / 3;
        int y = top + 68;
        if (mouseY < y || mouseY > y + CONTROL) return false;
        if (mouseX >= left + 12 && mouseX <= left + 12 + buttonWidth) {
            historySuspended = true;
            draft = savedSnapshot.draft();
            ClientHudLayout.updateTransient(savedSnapshot);
            historySuspended = false;
            dirty = false;
            closeWithoutPrompt();
            return true;
        }
        if (mouseX >= left + 12 + buttonWidth + gap
                && mouseX <= left + 12 + (buttonWidth + gap) * 2 - gap) {
            exitPromptOpen = false;
            rebuildWidgets();
            setStatus("已返回编辑。", UiTheme.INFO);
            return true;
        }
        if (mouseX >= left + 12 + (buttonWidth + gap) * 2
                && mouseX <= left + 12 + (buttonWidth + gap) * 2 + buttonWidth) {
            if (saveDraft()) {
                closeWithoutPrompt();
            }
            return true;
        }
        return false;
    }

    private boolean pickSelectedWindow(ClientHudLayout.Elements values, double mouseX, double mouseY) {
        if (selected.startsWith("custom:")) {
            return false;
        }
        return pickBuiltInByKey(values, selected, mouseX, mouseY);
    }

    private boolean pickBuiltInWindow(ClientHudLayout.Elements values, double mouseX, double mouseY) {
        if (globalTab) return false;
        String[] keys = activeContext.isMatch()
                ? new String[]{"feed", "text", "score"} : new String[]{"banner"};
        for (String key : keys) {
            if (pickBuiltInByKey(values, key, mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    private boolean pickBuiltInByKey(ClientHudLayout.Elements values, String key,
                                     double mouseX, double mouseY) {
        if (key.isBlank() || !values.builtInEnabled(HudContext.BuiltIn.valueOf(key.toUpperCase(Locale.ROOT)))) return false;
        HudGeometry.Rect rect;
        Drag nextDrag;
        boolean visible;
        switch (key) {
            case "score" -> {
                rect = HudGeometry.score(values, width, height);
                nextDrag = Drag.SCORE;
                visible = values.scoreVisible();
            }
            case "text" -> {
                String hint = MatchHudOverlay.hintText();
                rect = HudGeometry.text(values, width, height, values.textWidth());
                nextDrag = Drag.TEXT;
                visible = values.textVisible();
            }
            case "feed" -> {
                String feed = "⚔ Steve  击杀  Alex";
                rect = HudGeometry.feed(values, width, height, font.width(feed) + 20);
                nextDrag = Drag.FEED;
                visible = values.feedVisible();
            }
            case "banner" -> {
                rect = HudGeometry.banner(values, width, height, bannerWidth());
                nextDrag = Drag.BANNER;
                visible = values.bannerVisible();
            }
            default -> { return false; }
        }
        if (!visible || !rect.contains(mouseX, mouseY)) {
            return false;
        }
        drag = nextDrag;
        grabX = mouseX - (nextDrag == Drag.SCORE ? rect.left() : rect.centerX());
        grabY = mouseY - (nextDrag == Drag.SCORE ? rect.top() : rect.centerY());
        selectBuiltIn(key);
        return true;
    }

    /** 拖内置面板时同步右侧选中项；列表按钮等松手后再重建，避免拖拽中闪断。 */
    private void selectBuiltIn(String key) {
        if (!selected.equals(key)) {
            selected = key;
            paletteOpen = false;
            sidePanelNeedsRebuild = true;
        }
    }

    /** 画面中命中某个独立模块：选中它并开始拖动（最上层先命中，松手后再刷新属性列）。 */
    private boolean pickCustomElement(double mouseX, double mouseY) {
        List<ClientHudLayout.CustomElement> elements = draft.customElements(activeContext);
        ClientHudLayout.CustomElement active = selectedCustom();
        for (int index = elements.size() - 1; index >= 0; index--) {
            ClientHudLayout.CustomElement element = elements.get(index);
            if (!element.visible()) {
                continue;
            }
            if (pickCustomElement(element, mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    private boolean pickCustomElement(ClientHudLayout.CustomElement element, double mouseX, double mouseY) {
        if (!cn.blockforge.generated.generatedmod.client.HudParameters.visible(element.placement().condition(), true)) return false;
        HudGeometry.Rect rect = HudGeometry.custom(element, width, height);
        if (!rect.contains(mouseX, mouseY)) {
            return false;
        }
        boolean changedSelection = !("custom:" + element.id()).equals(selected);
        selected = "custom:" + element.id();
        dragCustomId = element.id();
        drag = Drag.CUSTOM;
        grabX = mouseX - rect.centerX();
        grabY = mouseY - rect.centerY();
        paletteOpen = false;
        setStatus("正在拖动 " + element.displayName() + "。", UiTheme.ACCENT);
        if (changedSelection) {
            sidePanelNeedsRebuild = true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        if (resizeEdge != ResizeEdge.NONE) {
            resizeDock(mouseX, mouseY);
            return true;
        }
        if (draggingDock) {
            moveDock(mouseX, mouseY);
            return true;
        }
        if (draggingPanelScrollbar) {
            dragPanelScrollbar(mouseY);
            return true;
        }
        switch (drag) {
            case SCORE -> {
                HudGeometry.Rect score = HudGeometry.score(draft.elements(activeContext), width, height);
                int centerX = (int) (mouseX - grabX + score.width() / 2.0D);
                int centerY = (int) (mouseY - grabY);
                current().scoreXPercent = clamp(Math.round(centerX * 100.0F / width), 0, 100);
                current().scoreYPercent = clamp(Math.round(centerY * 100.0F / height), 0, 100);
                applyTransient();
                return true;
            }
            case TEXT, FEED, BANNER -> {
                int centerX = (int) (mouseX - grabX);
                int centerY = (int) (mouseY - grabY);
                int x = clamp(Math.round(centerX * 100.0F / width), 0, 100);
                int y = clamp(Math.round(centerY * 100.0F / height), 0, 100);
                if (drag == Drag.TEXT) {
                    current().textXPercent = x;
                    current().textYPercent = y;
                } else if (drag == Drag.FEED) {
                    current().feedXPercent = x;
                    current().feedYPercent = y;
                } else {
                    current().bannerXPercent = x;
                    current().bannerYPercent = y;
                }
                applyTransient();
                return true;
            }
            case CUSTOM -> {
                ClientHudLayout.CustomElement live = customById(dragCustomId);
                if (live != null) {
                    if (live.placement().referenceWidth() > 0) {
                        HudGeometry.Rect rect = HudGeometry.custom(live, width, height);
                        var p = live.placement();
                        float fit = p.fit(width, height);
                        float alignmentShift = "left".equals(p.alignment()) ? rect.width() / 2F
                                : "right".equals(p.alignment()) ? -rect.width() / 2F : 0F;
                        int halfWidth = (rect.width() + 1) / 2;
                        int halfHeight = (rect.height() + 1) / 2;
                        int targetCenterX = clamp((int) Math.round(mouseX - grabX),
                                8 + rect.width() / 2, width - 8 - halfWidth);
                        int targetCenterY = clamp((int) Math.round(mouseY - grabY),
                                8 + rect.height() / 2, height - 8 - halfHeight);
                        int offsetX = Math.round((float) (targetCenterX - width * live.xPercent() / 100F
                                - alignmentShift) / fit);
                        int offsetY = Math.round((float) (targetCenterY - height * live.yPercent() / 100F) / fit);
                        replace(live, e -> e.placement = p.withOffset(offsetX, offsetY));
                        return true;
                    }
                    int x = clamp(Math.round((int) (mouseX - grabX) * 100.0F / width), 0, 100);
                    int y = clamp(Math.round((int) (mouseY - grabY) * 100.0F / height), 0, 100);
                    if (x != live.xPercent() || y != live.yPercent()) {
                        replace(live, e -> {
                            e.xPercent = x;
                            e.yPercent = y;
                        });
                    }
                }
                return true;
            }
            default -> {
                return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            }
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && resizeEdge != ResizeEdge.NONE) {
            resizeEdge = ResizeEdge.NONE;
            setStatus("HUD 配置面板大小已调整。", UiTheme.SUCCESS);
            return true;
        }
        if (button == 0 && draggingDock) {
            draggingDock = false;
            setStatus("HUD 配置面板位置已调整。", UiTheme.SUCCESS);
            return true;
        }
        if (button == 0 && draggingPanelScrollbar) {
            draggingPanelScrollbar = false;
            return true;
        }
        if (drag != Drag.NONE && button == 0) {
            drag = Drag.NONE;
            dragCustomId = "";
            commitHistoryEdit();
            if (sidePanelNeedsRebuild) {
                sidePanelNeedsRebuild = false;
                rebuildWidgets();
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** 只有右侧内容视口响应滚轮；标题栏和底部按钮区不抢滚轮事件。 */
    private boolean insideDockViewport(double mouseX, double mouseY) {
        return !dockHidden && mouseX >= dockX && mouseX <= dockX + dockWidth
                && mouseY >= viewportTop() && mouseY <= viewportBottom();
    }

    private int panelScrollbarX() {
        return dockX + dockWidth - 11;
    }

    private boolean beginPanelScrollbarDrag(double mouseX, double mouseY) {
        int overflow = Math.max(0, contentHeight() - (viewportBottom() - viewportTop()));
        if (dockHidden || overflow <= 0 || mouseX < panelScrollbarX()
                || mouseX > panelScrollbarX() + 8
                || mouseY < viewportTop() || mouseY > viewportBottom()) return false;
        int trackHeight = viewportBottom() - viewportTop();
        int thumb = Math.max(16, trackHeight * trackHeight / Math.max(1, contentHeight()));
        int thumbTop = viewportTop() + (trackHeight - thumb) * scroll / Math.max(1, overflow);
        if (mouseY >= thumbTop && mouseY <= thumbTop + thumb) {
            panelScrollbarDragOffset = (int) mouseY - thumbTop;
        } else {
            panelScrollbarDragOffset = thumb / 2;
            dragPanelScrollbar(mouseY);
        }
        draggingPanelScrollbar = true;
        return true;
    }

    private void dragPanelScrollbar(double mouseY) {
        int overflow = Math.max(0, contentHeight() - (viewportBottom() - viewportTop()));
        if (overflow <= 0) return;
        int trackHeight = viewportBottom() - viewportTop();
        int thumb = Math.max(16, trackHeight * trackHeight / Math.max(1, contentHeight()));
        int range = Math.max(1, trackHeight - thumb);
        int thumbTop = clamp((int) Math.round(mouseY) - panelScrollbarDragOffset,
                viewportTop(), viewportTop() + range);
        int next = clamp((int) Math.round((thumbTop - viewportTop()) * (double) overflow / range),
                0, overflow);
        if (next != scroll) {
            scroll = next;
            layoutRows();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (choices.scroll(amount)) return true;
        if (insideDockViewport(mouseX, mouseY)) {
            int maximum = Math.max(0, contentHeight() - (viewportBottom() - viewportTop()));
            int next = clamp(scroll - (int) Math.round(amount * 14), 0, maximum);
            if (next != scroll) {
                scroll = next;
                layoutRows();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (choices.key(keyCode)) return true;
        if (exitPromptOpen) {
            if (keyCode == 256) {
                exitPromptOpen = false;
                rebuildWidgets();
                setStatus("已返回编辑。", UiTheme.INFO);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if ((modifiers & 2) != 0 && (keyCode == 90 || keyCode == 89)) {
            if (keyCode == 90) undo(); else redo();
            return true;
        }
        if ((modifiers & 2) != 0 && keyCode == 83) {
            if (saveDraft()) {
                setStatus("已保存（Ctrl+S）。", UiTheme.SUCCESS);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        marqueeTicks++;
        if (statusTicks > 0) {
            statusTicks--;
        }
        if (historyPending && System.currentTimeMillis() - lastHistoryChange >= HISTORY_IDLE_MILLIS) {
            commitHistoryEdit();
        }
        for (UiEditBox editor : editors) {
            editor.tick();
        }
    }

    // ---------------------------------------------------------------- 渲染

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hoverX = mouseX;
        hoverY = mouseY;
        renderBackground(graphics);
        ClientHudLayout.Global global = draft.global.build();
        if (!global.backgroundFile().isBlank()) {
            HudBackground.drawStretch(graphics, global.backgroundFile(),
                    global.backgroundOpacityPercent(), width, height);
        }
        if (global.guidesVisible() && !dockHidden) {
            renderGuides(graphics);
        }
        renderPreview(graphics);
        layoutRows();
        layoutFooterButtons();
        // Keep preview glyph depth behind the opaque editor and its controls.
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 100);
        try {
            if (!dockHidden) renderDock(graphics);
            super.render(graphics, mouseX, mouseY, partialTick);
        } finally {
            graphics.pose().popPose();
        }
        renderStatus(graphics);
        choices.render(graphics, font, mouseX, mouseY);
        if (exitPromptOpen) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 500);
            try {
                renderExitPrompt(graphics);
            } finally {
                graphics.pose().popPose();
            }
        }
    }

    private void renderGuides(GuiGraphics graphics) {
        int centerX = width / 2;
        int centerY = height / 2;
        graphics.fill(centerX, 0, centerX + 1, height, 0x3350E0C8);
        graphics.fill(0, centerY, width, centerY + 1, 0x3350E0C8);
        graphics.fill(width / 3, 0, width / 3 + 1, height, 0x22FFFFFF);
        graphics.fill(width * 2 / 3, 0, width * 2 / 3 + 1, height, 0x22FFFFFF);
        graphics.fill(0, height / 3, width, height / 3 + 1, 0x22FFFFFF);
        graphics.fill(0, height * 2 / 3, width, height * 2 / 3 + 1, 0x22FFFFFF);
        int margin = HudGeometry.SCREEN_MARGIN;
        graphics.renderOutline(margin, margin, width - margin * 2, height - margin * 2, 0x5550E0C8);
        for (int x = 0; x < width; x += 6) {
            graphics.fill(x, height - HudGeometry.HOTBAR_SAFE_MARGIN, x + 3,
                    height - HudGeometry.HOTBAR_SAFE_MARGIN + 1, 0x55FFC857);
        }
    }

    private void renderPreview(GuiGraphics graphics) {
        ClientHudLayout.Elements values = draft.elements(activeContext);
        ClientHudLayout.CustomElement chosen = selectedCustom();
        if (globalTab) {
            HudCustomRenderer.render(graphics, font, draft.customElements(HudContext.GLOBAL),
                    width, height, true, true, "", HudContext.GLOBAL, selectedCustomId());
            if (chosen != null && chosen.visible()
                    && HudParameters.visible(chosen.placement().condition(), true)) {
                HudGeometry.Rect rect = HudGeometry.custom(chosen, width, height);
                graphics.renderOutline(rect.left() - 2, rect.top() - 2, rect.width() + 4,
                        rect.height() + 4, 0xFFFFD27A);
            }
            return;
        }
        HudCustomRenderer.render(graphics, font, draft.customElements(activeContext),
                width, height, true, true, "", activeContext, selectedCustomId());
        if (values.builtInMask() != 0) {
        if (activeContext.isMatch()) {
            HudGeometry.Rect score = HudGeometry.score(values, width, height);
            if (values.scoreVisible() && !selected.equals("score")) {
                renderScorePreview(graphics, values, score);
            } else if (!values.scoreVisible() || !selected.equals("score")) {
                drawGhostRect(graphics, score, "记分板（已隐藏）");
            }
            String hint = HudStats.resolveTemplate(values.textTemplate(), previewTemplateValues());
            HudGeometry.Rect text = HudGeometry.text(values, width, height, values.textWidth());
            if (values.textVisible() && !selected.equals("text")) {
                renderTextPreview(graphics, values, text, hint);
            } else if (!values.textVisible() || !selected.equals("text")) {
                drawGhostRect(graphics, text, "状态文字（已隐藏）");
            }
            String feed = HudStats.resolveTemplate(values.feedTemplate(), previewTemplateValues());
            HudGeometry.Rect feedRect = HudGeometry.feed(values, width, height, font.width(feed) + 20);
            if (values.feedVisible() && !selected.equals("feed")) {
                renderFeedPreview(graphics, values, feedRect, feed);
            } else if (!values.feedVisible() || !selected.equals("feed")) {
                drawGhostRect(graphics, feedRect, "击杀播报（已隐藏）");
            }
        } else {
            HudGeometry.Rect banner = HudGeometry.banner(values, width, height, bannerWidth());
            if (values.bannerVisible() && !selected.equals("banner")) {
                renderBannerPreview(graphics, values, banner);
            } else if (!values.bannerVisible() || !selected.equals("banner")) {
                drawGhostRect(graphics, banner, "场景横幅（已隐藏）");
            }
        }
        renderSelectedBuiltIn(graphics, values);
        }
        if (chosen != null && chosen.visible()
                && cn.blockforge.generated.generatedmod.client.HudParameters.visible(chosen.placement().condition(), true)) {
            HudGeometry.Rect rect = HudGeometry.custom(chosen, width, height);
            graphics.renderOutline(rect.left() - 2, rect.top() - 2, rect.width() + 4,
                    rect.height() + 4, 0xFFFFD27A);
            graphics.drawString(font, "拖动该模块", rect.left(), Math.max(2, rect.top() - 20),
                    UiTheme.WARNING, false);
        }
    }

    private void renderSelectedBuiltIn(GuiGraphics graphics, ClientHudLayout.Elements values) {
        if (selected.equals("score") && activeContext.isMatch() && values.scoreVisible()) {
            renderScorePreview(graphics, values, HudGeometry.score(values, width, height));
        } else if (selected.equals("text") && activeContext.isMatch() && values.textVisible()) {
            String hint = HudStats.resolveTemplate(values.textTemplate(), previewTemplateValues());
            renderTextPreview(graphics, values,
                    HudGeometry.text(values, width, height, values.textWidth()), hint);
        } else if (selected.equals("feed") && activeContext.isMatch() && values.feedVisible()) {
            String feed = HudStats.resolveTemplate(values.feedTemplate(), previewTemplateValues());
            renderFeedPreview(graphics, values,
                    HudGeometry.feed(values, width, height, font.width(feed) + 20), feed);
        } else if (selected.equals("banner") && !activeContext.isMatch() && values.bannerVisible()) {
            renderBannerPreview(graphics, values, HudGeometry.banner(values, width, height, bannerWidth()));
        }
    }

    private void renderBannerPreview(GuiGraphics graphics, ClientHudLayout.Elements values,
                                     HudGeometry.Rect banner) {
        Map<String, String> sample = previewTemplateValues();
        sample.put("matching_line1", "正在匹配  3/6 人成局");
        sample.put("matching_line2", "已等待 00:18  ·  序位 2");
        sample.put("room_line1", "房间  样例作战大厅  ·  团队竞技");
        sample.put("room_line2", "人数 4/12  ·  房主 Steve  ·  开放中");
        String one = HudStats.resolveTemplate(values.bannerLineOneTemplate(), sample);
        String two = HudStats.resolveTemplate(values.bannerLineTwoTemplate(), sample);
        MatchHudOverlay.drawPanel(graphics, font, banner, HudGeometry.BANNER_BASE_HEIGHT,
                UiTheme.fit(font, one, banner.baseWidth() - 16),
                UiTheme.fit(font, two, banner.baseWidth() - 16), values.bannerColor(),
                values.bannerOpacityPercent());
        renderDragBadge(graphics, banner, "拖动场景横幅");
    }

    private int bannerWidth() {
        return 340;
    }

    private Map<String, String> previewTemplateValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("mode", "团队竞技");
        values.put("phase", "进行中");
        values.put("score_a", "12");
        values.put("score_b", "9");
        var room = cn.blockforge.generated.generatedmod.client.SceneHudOverlay.ownRoom();
        int teamCount = room == null ? Math.max(2, cn.blockforge.generated.generatedmod.client.ClientMatchData.teamStats.size()) : room.teamCount();
        values.put("team_count", Integer.toString(teamCount));
        values.put("score_c", "8"); values.put("score_d", "5");
        values.put("wins_a", "1"); values.put("wins_b", "0");
        values.put("wins_c", "0"); values.put("wins_d", "0");
        values.put("time", "03:45");
        values.put("round", "1");
        values.put("target", "25");
        values.put("team", "A队");
        values.put("sizes", "A队 4人 · B队 4人");
        values.put("hint", "队伍：A队 · A队 4人 · B队 4人");
        if (teamCount > 2) {
            String sizes = cn.blockforge.generated.generatedmod.match.Team.playing(teamCount).stream()
                    .map(team -> team.displayName() + " 4人").collect(java.util.stream.Collectors.joining(" · "));
            values.put("sizes", sizes); values.put("hint", "队伍：A队 · " + sizes);
        }
        values.put("killer", "Steve");
        values.put("victim", "Alex");
        values.put("feed", "Steve 击杀 Alex");
        return values;
    }

    private void renderScorePreview(GuiGraphics graphics, ClientHudLayout.Elements values,
                                    HudGeometry.Rect rect) {
        cn.blockforge.generated.generatedmod.client.ui.ScoreHudRenderer.draw(graphics, font, rect, values, previewTemplateValues(), false);
        if (selected.equals("score") && !dockHidden) {
            graphics.renderOutline(rect.left() - 1, rect.top() - 1, rect.width() + 2, rect.height() + 2, UiTheme.WARNING);
        }
    }

    private void renderTextPreview(GuiGraphics graphics, ClientHudLayout.Elements values,
                                   HudGeometry.Rect rect, String text) {
        drawSimplePanel(graphics, rect, text, values.textOpacityPercent(), values.textColor());
        renderDragBadge(graphics, rect, "拖动状态文字");
        if (selected.equals("text")) {
            graphics.renderOutline(rect.left() - 1, rect.top() - 1, rect.width() + 2,
                    rect.height() + 2, 0xAAFFD27A);
        }
    }

    private void renderFeedPreview(GuiGraphics graphics, ClientHudLayout.Elements values,
                                   HudGeometry.Rect rect, String text) {
        drawSimplePanel(graphics, rect, text, values.feedOpacityPercent(), values.feedColor());
        renderDragBadge(graphics, rect, "拖动击杀播报");
        if (selected.equals("feed")) {
            graphics.renderOutline(rect.left() - 1, rect.top() - 1, rect.width() + 2,
                    rect.height() + 2, 0xAAFFD27A);
        }
    }

    private void drawSimplePanel(GuiGraphics graphics, HudGeometry.Rect rect, String text,
                                 int opacity, int accent) {
        MatchHudOverlay.drawPanel(graphics, font, rect, rect.baseHeight(), text, "", accent, opacity);
    }

    private void renderDragBadge(GuiGraphics graphics, HudGeometry.Rect rect, String label) {
        if (rect.contains(hoverX, hoverY)) {
            graphics.renderOutline(rect.left() - 1, rect.top() - 1, rect.width() + 2,
                    rect.height() + 2, 0xAA50E0C8);
            graphics.drawString(font, label, rect.left(), Math.max(2, rect.top() - 10), UiTheme.ACCENT, false);
        }
    }

    /** 1.20.1 的 Screen 不自带鼠标坐标字段，渲染时记一份供悬停高亮使用。 */
    private double hoverX;
    private double hoverY;

    private void drawGhostRect(GuiGraphics graphics, HudGeometry.Rect rect, String label) {
        for (int x = rect.left(); x < rect.right(); x += 5) {
            graphics.fill(x, rect.top(), Math.min(x + 2, rect.right()), rect.top() + 1, UiTheme.BORDER_SUBTLE);
            graphics.fill(x, rect.bottom() - 1, Math.min(x + 2, rect.right()), rect.bottom(), UiTheme.BORDER_SUBTLE);
        }
        for (int y = rect.top(); y < rect.bottom(); y += 5) {
            graphics.fill(rect.left(), y, rect.left() + 1, y + 1, UiTheme.BORDER_SUBTLE);
            graphics.fill(rect.right() - 1, y, rect.right(), y + 1, UiTheme.BORDER_SUBTLE);
        }
        graphics.drawString(font, label, rect.left(), Math.max(2, rect.top() - 10), UiTheme.SUBTLE, false);
    }

    private void renderDock(GuiGraphics graphics) {
        graphics.fill(dockX, dockTop, dockX + dockWidth, dockBottom, UiTheme.PANEL | 0xFF000000);
        graphics.renderOutline(dockX, dockTop, dockWidth, dockBottom - dockTop, UiTheme.BORDER);
        graphics.fill(dockX, dockTop, dockX + dockWidth, dockTop + DOCK_HEADER_HEIGHT, UiTheme.PANEL_RAISED);
        String title = globalTab ? "背景与全局设置" : "配置：" + activeContext.displayName();
        int instructionWidth = dockWidth >= 300 ? 82 : 0;
        int titleWidth = Math.max(40, dockWidth - 16 - instructionWidth);
        graphics.drawString(font, UiTheme.fit(font, title, titleWidth), dockX + 8, dockTop + 6,
                UiTheme.TEXT, false);
        if (instructionWidth > 0) {
            graphics.drawString(font, UiTheme.fit(font, dirty ? "未保存" : "已保存", instructionWidth),
                    dockX + dockWidth - instructionWidth - 8, dockTop + 7, UiTheme.SUBTLE, false);
        }
        graphics.fill(dockX, dockTop + DOCK_HEADER_HEIGHT, dockX + dockWidth, dockTop + DOCK_HEADER_HEIGHT + 1,
                UiTheme.BORDER_SUBTLE);
        graphics.fill(dockX, dockBottom - CONTROL * FOOTER_ROWS - 18, dockX + dockWidth,
                dockBottom - CONTROL * FOOTER_ROWS - 17, UiTheme.BORDER_SUBTLE);
        graphics.fill(dockX + dockWidth - 12, dockBottom - 3, dockX + dockWidth - 3, dockBottom - 2,
                UiTheme.ACCENT);
        graphics.fill(dockX + dockWidth - 3, dockBottom - 12, dockX + dockWidth - 2, dockBottom - 3,
                UiTheme.ACCENT);
        int y = viewportTop() + TOP_INSET - scroll;
        int previousLine = Integer.MIN_VALUE;
        for (Row row : rows) {
            // 与 layoutRows 保持一致：同一横向布局行只占一次纵向高度。
            if (row.line() != UNGROUPED_LINE && row.line() == previousLine) {
                continue;
            }
            if (row.kind() == RowKind.HEADER && fits(row, y)) {
                graphics.drawString(font, UiTheme.fit(font, "· " + row.text(), dockInnerWidth()),
                        dockX + 8, y, UiTheme.ACCENT, false);
            } else if (row.kind() == RowKind.NOTE && fits(row, y)) {
                String[] lines = row.text().split("\n");
                for (int line = 0; line < lines.length; line++) {
                    graphics.drawString(font, UiTheme.fit(font, lines[line], dockInnerWidth()),
                            dockX + 8, y + line * 10, UiTheme.SUBTLE, false);
                }
            }
            previousLine = row.line();
            y += row.height() + ROW_GAP;
        }
        int overflow = contentHeight() - (viewportBottom() - viewportTop());
        if (overflow > 0) {
            int trackTop = viewportTop();
            int trackBottom = viewportBottom();
            int scrollbarX = dockX + dockWidth - 11;
            graphics.fill(scrollbarX, trackTop, scrollbarX + 8, trackBottom,
                    UiTheme.BORDER_SUBTLE);
            int thumb = Math.max(16, (trackBottom - trackTop) * (trackBottom - trackTop)
                    / Math.max(1, contentHeight()));
            int thumbTop = trackTop + (trackBottom - trackTop - thumb) * scroll / Math.max(1, overflow);
            graphics.fill(scrollbarX + 1, thumbTop, scrollbarX + 7, thumbTop + thumb, UiTheme.ACCENT);
            graphics.renderOutline(scrollbarX, thumbTop, 8, thumb, UiTheme.BORDER);
        }
    }

    private boolean fits(Row row, int y) {
        return y >= viewportTop() - 1 && y + row.height() <= viewportBottom() + 1;
    }

    private void renderStatus(GuiGraphics graphics) {
        int availableWidth = Math.max(0, dockX - 24);
        graphics.drawString(font, UiTheme.fit(font, statusTicks > 0 ? status : "", availableWidth), 12, height - 14,
                statusTicks > 0 ? statusColor : UiTheme.SUBTLE);
    }

    /** 状态说明超过左侧可用区域时循环显示，避免缩小时只剩半句。 */
    private String marquee(String value, int maximumWidth) {
        if (value == null || value.isBlank() || maximumWidth <= 0 || font.width(value) <= maximumWidth) {
            return value == null ? "" : value;
        }
        String padded = value + "    " + value;
        int start = (marqueeTicks / 3) % Math.max(1, padded.length() - 1);
        String visible = padded.substring(start) + padded.substring(0, start);
        return font.plainSubstrByWidth(visible, maximumWidth);
    }

    private boolean saveDraft() {
        commitHistoryEdit();
        ClientHudLayout.Snapshot next = draft.build();
        String error = ClientHudLayout.apply(next);
        if (error == null) {
            savedSnapshot = next;
            dirty = false;
            historyBaseline = next;
            historyPending = false;
            undoStack.clear();
            redoStack.clear();
            if (closeButton != null) {
                closeButton.setMessage(Component.literal("完成"));
            }
            return true;
        }
        setStatus(error, UiTheme.ERROR);
        return false;
    }

    private void requestClose() {
        if (!dirty) {
            closeWithoutPrompt();
            return;
        }
        exitPromptOpen = true;
        clearWidgets();
        setStatus("存在未保存改动，请选择如何退出。", UiTheme.WARNING);
    }

    private void closeWithoutPrompt() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private void renderExitPrompt(GuiGraphics graphics) {
        graphics.fill(0, 0, width, height, 0x99000000);
        int boxWidth = Math.min(390, width - 24);
        int boxHeight = 112;
        int left = (width - boxWidth) / 2;
        int top = (height - boxHeight) / 2;
        UiTheme.panel(graphics, left, top, boxWidth, boxHeight, UiTheme.WARNING);
        graphics.drawCenteredString(font, "HUD 配置尚未保存", width / 2, top + 12, UiTheme.TEXT);
        graphics.drawCenteredString(font, UiTheme.fit(font, "要保存改动后退出，还是放弃本次改动？", boxWidth - 24),
                width / 2, top + 31, UiTheme.MUTED);
        int gap = 6;
        int buttonWidth = (boxWidth - 24 - gap * 2) / 3;
        int y = top + 68;
        drawPromptButton(graphics, left + 12, y, buttonWidth, "放弃", UiTheme.ERROR);
        drawPromptButton(graphics, left + 12 + buttonWidth + gap, y, buttonWidth, "返回编辑", UiTheme.INFO);
        drawPromptButton(graphics, left + 12 + (buttonWidth + gap) * 2, y, buttonWidth, "保存退出", UiTheme.SUCCESS);
    }

    private void drawPromptButton(GuiGraphics graphics, int x, int y, int buttonWidth, String label, int color) {
        graphics.fill(x, y, x + buttonWidth, y + CONTROL, UiTheme.PANEL_RAISED);
        graphics.renderOutline(x, y, buttonWidth, CONTROL, color);
        graphics.drawCenteredString(font, label, x + buttonWidth / 2, y + 7, color);
    }

    @Override
    public void onClose() {
        requestClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String conditionLabel(String value) {
        if (value == null || value.isBlank()) return "始终";
        if (value.startsWith("hidden:")) return value.substring(7) + " 隐藏后";
        if (value.startsWith("event:")) {
            String id = value.substring("event:".length());
            var event = cn.blockforge.generated.generatedmod.match.MatchHudEventType.byId(id);
            if (event != null) return "事件 · " + event.displayName();
            var descriptor = cn.blockforge.generated.generatedmod.client.ClientHudEventData
                    .descriptor(id);
            return descriptor == null ? value : "事件 · " + descriptor.title();
        }
        String[] parts = value.split(":", 2);
        if (parts.length != 2) return value;
        return switch (parts[0]) {
            case "health_below" -> "生命低于 " + parts[1] + "%";
            case "health_above" -> "生命高于 " + parts[1] + "%";
            case "armor_below" -> "护甲低于 " + parts[1];
            case "money_below" -> "局内资金低于 $" + parts[1];
            case "money_at_least" -> "局内资金至少 $" + parts[1];
            case "kills_at_least" -> "击杀至少 " + parts[1];
            case "deaths_at_least" -> "阵亡至少 " + parts[1];
            case "phase_remaining_below" -> "阶段剩余低于 " + parts[1] + " 秒";
            case "boundary_below" -> "出界剩余低于 " + parts[1] + " 秒";
            default -> value;
        };
    }

    private static boolean isBombEvent(
            cn.blockforge.generated.generatedmod.match.MatchHudEventType type) {
        return switch (type) {
            case BOMB_SITE_REQUIRED, BOMB_PLANTING, BOMB_DEFUSING, BOMB_ACTION_INTERRUPTED,
                    BUY_PHASE_START, BUY_AREA_ONLY -> true;
            default -> false;
        };
    }

    private static int clamp(int value, int minimum, int maximum) {
        return maximum < minimum ? minimum : Math.max(minimum, Math.min(maximum, value));
    }
}
