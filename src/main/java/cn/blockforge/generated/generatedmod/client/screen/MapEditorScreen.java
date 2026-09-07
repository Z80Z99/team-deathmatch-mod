package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientMapEditorData;
import cn.blockforge.generated.generatedmod.client.ui.UiButton;
import cn.blockforge.generated.generatedmod.client.ui.UiScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import cn.blockforge.generated.generatedmod.map.MapEditorAction;
import cn.blockforge.generated.generatedmod.map.MapEditorView;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.MapEditorActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 地图编辑器：只编辑“当前编辑目标”地图（在地图工作台中选择或导入）。
 * 标题徽标、摘要卡片、页签、操作按钮、提示各占一条流式横带，纵向不可能重叠。
 */
public final class MapEditorScreen extends UiScreen {
    private static final int SUMMARY_HEIGHT = 68;

    private final Screen parent;
    private final List<UiButton> editButtons = new ArrayList<>();
    private UiButton regionTab;
    private UiButton spawnTab;
    private ActionPage page = ActionPage.REGIONS;
    private int targetY;
    private int summaryY;
    private int tabsY;
    private int actionsY1;
    private int actionsY2;
    private int hintY;
    private int ticks;
    private int lastRevision = -1;
    private String displayedSpawnTabLabel = "";

    public MapEditorScreen(Screen parent) {
        super(Component.literal("地图编辑"));
        this.parent = parent;
    }

    public static void open(Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.getConnection() != null) {
            MapEditorScreen screen = new MapEditorScreen(parent);
            minecraft.setScreen(screen);
            screen.request(MapEditorAction.REQUEST, "");
        }
    }

    @Override
    protected void init() {
        editButtons.clear();
        beginLayout(700, 360, BUTTON_HEIGHT);
        tabsY = flowRow(BUTTON_HEIGHT);
        actionsY1 = flowRow(BUTTON_HEIGHT);
        actionsY2 = flowRow(BUTTON_HEIGHT);
        targetY = flowRow(18);
        summaryY = flowRow(SUMMARY_HEIGHT);
        hintY = flowRow(6);
        lastRevision = ClientMapEditorData.revision();

        addActionTabs();
        addRegionActions();
        addSpawnActions();
        footerButton("返回地图工作台", 0, 2, 0, this::onClose,
                "返回“我的地图”列表，可切换目标或分享地图。", UiButton.Kind.DANGER);
        footerButton("刷新状态", 1, 2, 0, () -> request(MapEditorAction.REQUEST, ""),
                "重新读取服务器编辑状态；如草稿已失效，这会确认丢弃旧草稿。", UiButton.Kind.SECONDARY);
        updateActionVisibility();
        updateButtons();
    }

    private void addActionTabs() {
        int gap = 6;
        int tabWidth = columnWidth(2, gap);
        regionTab = flowWidget(uiButton("区域与快照", columnX(0, 2, gap), tabsY, tabWidth,
                () -> selectPage(ActionPage.REGIONS),
                "编辑当前目标地图的边界与重置区域；保存后载入比赛时重新捕获快照。",
                UiButton.Kind.SECONDARY), tabsY);
        spawnTab = flowWidget(uiButton("出生点", columnX(1, 2, gap), tabsY, tabWidth,
                () -> selectPage(ActionPage.SPAWNS),
                "添加或清理 A队、B队和观战出生点。", UiButton.Kind.SECONDARY), tabsY);
        regionTab.setSelected(true);
    }

    private void addRegionActions() {
        addAction(ActionPage.REGIONS, 0, actionsY1, "边界最小 ← 当前", MapEditorAction.SET_BOUNDS_MIN,
                "用服务器读取的当前位置设置地图 bounds 最小点。", UiButton.Kind.SECONDARY);
        addAction(ActionPage.REGIONS, 1, actionsY1, "边界最大 ← 当前", MapEditorAction.SET_BOUNDS_MAX,
                "用服务器读取的当前位置设置地图 bounds 最大点。", UiButton.Kind.SECONDARY);
        addAction(ActionPage.REGIONS, 2, actionsY1, "保存区域", MapEditorAction.APPLY_REGIONS,
                "保存草稿区域；旧快照作废，载入比赛时由服务器重新捕获。", UiButton.Kind.PRIMARY);
        addAction(ActionPage.REGIONS, 0, actionsY2, "重置区最小 ← 当前", MapEditorAction.SET_RESET_MIN,
                "用当前位置设置 resetRegion 最小点。", UiButton.Kind.SECONDARY);
        addAction(ActionPage.REGIONS, 1, actionsY2, "重置区最大 ← 当前", MapEditorAction.SET_RESET_MAX,
                "用当前位置设置 resetRegion 最大点。", UiButton.Kind.SECONDARY);
    }

    private void addSpawnActions() {
        addAction(ActionPage.SPAWNS, 0, actionsY1, "添加 A队出生点", MapEditorAction.ADD_TEAM_A,
                "把当前位置添加到 A队出生点列表。", UiButton.Kind.PRIMARY);
        addAction(ActionPage.SPAWNS, 1, actionsY1, "添加 B队出生点", MapEditorAction.ADD_TEAM_B,
                "把当前位置添加到 B队出生点列表。", UiButton.Kind.PRIMARY);
        addAction(ActionPage.SPAWNS, 2, actionsY1, "设置观战点", MapEditorAction.SET_SPECTATOR,
                "把当前位置保存为观战出生点。", UiButton.Kind.SECONDARY);
        addAction(ActionPage.SPAWNS, 0, actionsY2, "清空 A队出生点", MapEditorAction.CLEAR_TEAM_A,
                "清除当前目标地图的全部 A队出生点。", UiButton.Kind.DANGER);
        addAction(ActionPage.SPAWNS, 1, actionsY2, "清空 B队出生点", MapEditorAction.CLEAR_TEAM_B,
                "清除当前目标地图的全部 B队出生点。", UiButton.Kind.DANGER);
        addAction(ActionPage.SPAWNS, 2, actionsY2, "清空观战点", MapEditorAction.CLEAR_SPECTATOR,
                "清除观战出生点。", UiButton.Kind.DANGER);
    }

    private void addAction(ActionPage actionPage, int column, int y, String label,
                           MapEditorAction action, String tooltip, UiButton.Kind kind) {
        int gap = 6;
        UiButton button = uiButton(label, columnX(column, 3, gap), y, columnWidth(3, gap),
                () -> {
                    if (action == MapEditorAction.CLEAR_TEAM_A || action == MapEditorAction.CLEAR_TEAM_B
                            || action == MapEditorAction.CLEAR_SPECTATOR) {
                        String targetId = ClientMapEditorData.view().mapId();
                        confirmAction(label, "地图「" + ClientMapEditorData.view().displayName() + "」：" + tooltip + " 此操作不可撤销。",
                                () -> {
                                    if (targetId.equals(ClientMapEditorData.view().mapId())) request(action, "");
                                });
                    } else request(action, "");
                }, tooltip, kind);
        boolean region = actionPage == ActionPage.REGIONS;
        flowWidget(button, y, () -> region == (page == ActionPage.REGIONS));
        editButtons.add(button);
    }

    private void selectPage(ActionPage next) {
        page = next;
        updateActionVisibility();
    }

    private void updateActionVisibility() {
        if (regionTab != null) {
            regionTab.setSelected(page == ActionPage.REGIONS);
        }
        if (spawnTab != null) {
            spawnTab.setSelected(page == ActionPage.SPAWNS);
        }
        refreshFlowVisibility();
    }

    private void request(MapEditorAction action, String mapId) {
        if (minecraft != null && minecraft.getConnection() != null) {
            FpsTdmNetwork.sendToServer(new MapEditorActionPacket(action, mapId));
        }
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        updateButtons();
        if (ticks % 20 == 0) {
            request(MapEditorAction.POLL, "");
        }
    }

    private void updateButtons() {
        MapEditorView view = ClientMapEditorData.view();
        boolean available = view.canEdit() && !view.locked() && !view.draftInvalidated() && view.hasTarget();
        for (UiButton button : editButtons) {
            button.active = available;
        }
        if (spawnTab != null) {
            String label = "出生点 A" + view.teamACount() + " / B" + view.teamBCount()
                    + " / 观" + view.spectatorCount();
            if (!label.equals(displayedSpawnTabLabel)) {
                displayedSpawnTabLabel = label;
                spawnTab.setMessage(Component.literal(label));
            }
        }
        updateActionVisibility();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderShell(graphics, ClientMapEditorData.view().hasTarget() ? ClientMapEditorData.view().displayName() : "未选择地图");
        MapEditorView view = ClientMapEditorData.view();
        renderStatus(graphics, statusMessage(view), statusColor(view));

        final int targetBand = targetY;
        int badgeWidth = Math.min(150, Math.max(70, innerWidth / 4));
        String targetText = view.hasTarget()
                ? "目标  " + dash(view.displayName()) + "  ·  " + dash(view.mapId()) : "尚未选择编辑目标";
        paintBand(graphics, targetBand, 18, () -> {
            badge(graphics, fit(targetText, innerWidth - badgeWidth - 8), innerLeft, targetBand,
                    Math.max(1, innerWidth - badgeWidth - 6),
                    view.hasTarget() ? UiTheme.ACCENT : UiTheme.MUTED);
            String shareBadge = view.shareCode().isBlank() ? "无邀请码" : "码 " + view.shareCode();
            badge(graphics, shareBadge, innerLeft + innerWidth - badgeWidth, targetBand, badgeWidth,
                    view.shareCode().isBlank() ? UiTheme.SUBTLE : UiTheme.SUCCESS);
        });

        paintBand(graphics, summaryY, SUMMARY_HEIGHT, () -> renderSummary(graphics, view));
        final int hintBand = hintY;
        paintBand(graphics, hintBand, 6, () -> progress(graphics, innerLeft, hintBand, innerWidth, 3,
                ((view.teamACount() > 0 ? 1 : 0) + (view.teamBCount() > 0 ? 1 : 0)
                        + (view.spectatorCount() > 0 ? 1 : 0)) / 3.0F, UiTheme.SUCCESS));
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderSummary(GuiGraphics graphics, MapEditorView view) {
        int y = summaryY;
        divider(graphics, innerLeft, y + SUMMARY_HEIGHT - 1, innerWidth);
        String state = view.draftInvalidated() ? "草稿失效" : !view.hasTarget() ? "无目标"
                : !view.canEdit() ? "不可编辑" : view.locked() ? "任务锁定" : "可编辑";
        int stateColor = view.draftInvalidated() || !view.hasTarget() ? UiTheme.ERROR
                : !view.canEdit() ? UiTheme.SUBTLE
                : view.locked() ? UiTheme.WARNING : UiTheme.SUCCESS;
        int badgeWidth = Math.min(88, Math.max(54, innerWidth / 8));
        badge(graphics, state, innerLeft + innerWidth - badgeWidth - 4, y + 2, badgeWidth, stateColor);

        if (!view.hasTarget()) {
            graphics.drawString(font, fit("在地图工作台里新建一张地图，或用别人的邀请码导入一份副本。",
                    innerWidth - 16), innerLeft + 8, y + 28, UiTheme.MUTED, false);
            return;
        }
        int titleWidth = Math.max(1, innerWidth - badgeWidth - 14);
        graphics.drawString(font, fit("维度  " + dash(view.world()) + "  ·  快照  " + dash(view.snapshotStatus()),
                titleWidth), innerLeft + 8, y + 3, UiTheme.TEXT, false);

        if (innerWidth >= 520) {
            int gap = 14;
            int leftWidth = Math.max(1, (innerWidth - gap) / 2);
            int rightX = innerLeft + leftWidth + gap;
            int rightWidth = Math.max(1, innerWidth - leftWidth - gap - badgeWidth - 12);
            graphics.drawString(font, fit("正式边界  " + region(view.bounds()), leftWidth - 16),
                    innerLeft + 8, y + 17, UiTheme.MUTED, false);
            graphics.drawString(font, fit("草稿边界  " + region(view.draftBounds()), leftWidth - 16),
                    innerLeft + 8, y + 31, UiTheme.SUBTLE, false);
            graphics.drawString(font, fit("出生点  A " + view.teamACount() + " · B " + view.teamBCount()
                    + " · 观战 " + view.spectatorCount(), leftWidth - 16), innerLeft + 8, y + 45,
                    UiTheme.MUTED, false);
            graphics.drawString(font, fit("正式重置区  " + region(view.resetRegion()), rightWidth - 8),
                    rightX, y + 17, UiTheme.MUTED, false);
            graphics.drawString(font, fit("草稿重置区  " + region(view.draftResetRegion()), rightWidth - 8),
                    rightX, y + 31, UiTheme.SUBTLE, false);
            graphics.drawString(font, fit("编辑权  " + (view.canEdit() ? "拥有者/管理员" : "无"),
                    rightWidth - 8), rightX, y + 45, UiTheme.MUTED, false);
        } else {
            graphics.drawString(font, fit("边界  正式 " + region(view.bounds()) + "  ·  草稿 "
                            + region(view.draftBounds()), innerWidth - 16),
                    innerLeft + 8, y + 17, UiTheme.MUTED, false);
            graphics.drawString(font, fit("重置区  正式 " + region(view.resetRegion()) + "  ·  草稿 "
                            + region(view.draftResetRegion()), innerWidth - 16),
                    innerLeft + 8, y + 31, UiTheme.SUBTLE, false);
            graphics.drawString(font, fit("出生点  A " + view.teamACount() + " · B " + view.teamBCount()
                            + " · 观战 " + view.spectatorCount(), innerWidth - 16),
                    innerLeft + 8, y + 45, UiTheme.MUTED, false);
        }
    }

    private void renderActionHint(GuiGraphics graphics, MapEditorView view) {
        String hint = !view.hasTarget() ? "请先在地图工作台选择或新建地图。"
                : page == ActionPage.REGIONS
                        ? "区域更改先进入草稿；点击“保存区域”后才写入地图定义。"
                        : "添加操作记录你当前所在位置；清空操作不可从客户端撤销。";
        int color = !view.hasTarget() ? UiTheme.MUTED : page == ActionPage.REGIONS
                ? UiTheme.INFO : UiTheme.WARNING;
        badge(graphics, hint, innerLeft, hintY, innerWidth, color);
    }

    private String statusMessage(MapEditorView view) {
        if (view.draftInvalidated()) {
            return "草稿已失效：点击“刷新状态”确认载入最新地图定义。";
        }
        if (view.locked()) {
            return "地图任务或比赛进行中，编辑操作暂时锁定。";
        }
        if (!view.hasTarget()) {
            return "还没有编辑目标：返回地图工作台新建，或用邀请码导入。";
        }
        if (!view.canEdit()) {
            return "你没有这张地图的编辑权：只有拥有者（或管理员）能编辑。";
        }
        if (!safe(view.message()).isBlank()) {
            return view.message();
        }
        return "编辑状态已同步，可以开始设置区域与出生点。";
    }

    private int statusColor(MapEditorView view) {
        if (view.error() || view.draftInvalidated()) {
            return UiTheme.ERROR;
        }
        if (view.locked()) {
            return UiTheme.WARNING;
        }
        if (!view.hasTarget()) {
            return UiTheme.ERROR;
        }
        if (!view.canEdit()) {
            return UiTheme.MUTED;
        }
        return safe(view.message()).isBlank() ? UiTheme.INFO : UiTheme.SUCCESS;
    }

    private String region(MapEditorView.RegionData region) {
        if (region == null || !region.present()) {
            return "未设置";
        }
        return "[" + region.minX() + "," + region.minY() + "," + region.minZ() + "]→["
                + region.maxX() + "," + region.maxY() + "," + region.maxZ() + "]";
    }

    private static String dash(String value) {
        return safe(value).isBlank() ? "—" : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum ActionPage {
        REGIONS,
        SPAWNS
    }
}
