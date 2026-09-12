package cn.blockforge.generated.generatedmod.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * 所有 Mod Screen 共用的布局与交互基类。
 *
 * <p>布局契约（结构上排除重叠、错位）：
 * <ul>
 *   <li>面板纵向划分为四个互不相交的区带：标题区（面板顶部固定）、内容视口、状态条（固定）、底部操作区（固定）；</li>
 *   <li>内容一律通过 {@link #flowRow(int)} 领取一条独占高度的横带，带与带之间由游标推进，永不共享 y 区间；</li>
 *   <li>控件经 {@link #flowWidget} 登记后随视口滚动，滚出视口的带连同文字整行隐藏（不做半截裁切），
 *       内容超高时以滚轮滚动代替挤压，因此窗口再小也不会出现元素互相顶掉；</li>
 *   <li>横向一律用 {@link #columnWidth}/{@link #columnX} 或底部列辅助测量切分，文字绘制前必须经
 *       {@link #fit} 按所在单元格宽度裁剪。</li>
 * </ul>
 */
public abstract class UiScreen extends Screen implements UiChoiceHost {
    private final UiChoicePopup choices = new UiChoicePopup();
    @Override public final UiChoicePopup choicePopup() { return choices; }
    public static final int HEADER_HEIGHT = 36;
    public static final int STATUS_HEIGHT = 22;
    /** 所有界面按钮、输入框和底部操作按钮统一使用的高度。 */
    public static final int BUTTON_HEIGHT = 22;
    public static final int CONTENT_PADDING = 10;
    public static final int ROW_GAP = 4;

    protected int panelLeft;
    protected int panelTop;
    protected int panelWidth;
    protected int panelBottom;
    protected int innerLeft;
    protected int innerWidth;
    protected int contentTop;
    protected int contentBottom;
    protected int statusTop;
    protected int footerTop;
    /** 是否保留底部状态条区带；不需要文本提示条的界面传 false，省下的空间归内容区。 */
    protected boolean hasStatusBand = true;
    /** 是否保留顶部标题带；一级菜单等“无标题”界面传 false，标题 / 副标题都不再绘制。 */
    protected boolean hasHeaderBand = true;

    private final List<Band> bands = new ArrayList<>();
    private int cursor;
    private int scroll;
    private boolean draggingScrollbar;
    private int scrollbarDragOffset;

    private record Band(AbstractWidget widget, int bandY, int bandHeight, boolean fixed,
                        BooleanSupplier extraVisible) {
    }

    protected UiScreen(Component title) {
        super(title);
    }

    @Override
    protected void rebuildWidgets() {
        int previousScroll = scroll;
        super.rebuildWidgets();
        scroll = clamp(previousScroll, 0, maxScroll());
        applyScroll();
    }

    /** 锁定面板几何与标题 / 内容 / 状态 / 底部四个区带；preferredHeight ≤ 0 表示占满可用高度。 */
    protected final void beginLayout(int preferredWidth, int preferredHeight, int footerHeight) {
        beginLayout(preferredWidth, preferredHeight, footerHeight, true);
    }

    /** withStatusBand=false 时删除底部状态条区带（界面不需要底部文本框），内容区直接顶到操作区上方。 */
    protected final void beginLayout(int preferredWidth, int preferredHeight, int footerHeight,
                                     boolean withStatusBand) {
        beginLayout(preferredWidth, preferredHeight, footerHeight, withStatusBand, true);
    }

    /** withHeader=false 时删除顶部标题带：不画标题、副标题与分隔线（大厅一级菜单用）。 */
    protected final void beginLayout(int preferredWidth, int preferredHeight, int footerHeight,
                                     boolean withStatusBand, boolean withHeader) {
        hasStatusBand = withStatusBand;
        choices.close();
        hasHeaderBand = withHeader;
        panelWidth = Math.max(1, Math.min(preferredWidth, width - 16));
        panelLeft = (width - panelWidth) / 2;
        int headerHeight = withHeader ? HEADER_HEIGHT : 10;
        int chrome = headerHeight + (withStatusBand ? STATUS_HEIGHT + 8 : 6) + footerHeight + CONTENT_PADDING + BUTTON_HEIGHT;
        int available = Math.max(chrome, height - 8);
        int panelHeight = preferredHeight <= 0
                ? available
                : clamp(preferredHeight, chrome, available);
        panelTop = Math.max(4, (height - panelHeight) / 2);
        panelBottom = Math.min(height - 4, panelTop + panelHeight);
        innerLeft = panelLeft + CONTENT_PADDING;
        innerWidth = Math.max(1, panelWidth - CONTENT_PADDING * 2 - 12);
        contentTop = panelTop + headerHeight;
        footerTop = panelBottom - CONTENT_PADDING - footerHeight;
        statusTop = footerTop - STATUS_HEIGHT - 4;
        contentBottom = hasStatusBand ? statusTop - 4 : footerTop - 6;
        cursor = contentTop;
        scroll = 0;
        bands.clear();
    }

    /** 领取下一条内容横带（流坐标），游标自动推进；任何两行都不会拿到同一段 y 区间。 */
    protected final int flowRow(int rowHeight) {
        int y = cursor;
        cursor += rowHeight + ROW_GAP;
        return y;
    }

    /** 手动推进游标，用于行组之间额外的视觉分组间距。 */
    protected final void flowSpace(int extra) {
        cursor += Math.max(0, extra);
    }

    protected final int flowCursor() {
        return cursor;
    }

    /** 加入随内容滚动的控件；所在带滚出视口时整体隐藏，不会被状态条或底部区截半。 */
    protected final <T extends AbstractWidget> T flowWidget(T widget, int bandY) {
        return flowWidget(widget, bandY, () -> true);
    }

    /** 加入随内容滚动的控件，并叠加界面自身的显示条件（如页签切换）；两个条件同时满足才可见。 */
    protected final <T extends AbstractWidget> T flowWidget(T widget, int bandY, BooleanSupplier extraVisible) {
        Band band = new Band(widget, bandY, widget.getHeight(), false, extraVisible);
        bands.add(band);
        addRenderableWidget(widget);
        placeScrollable(band);
        return widget;
    }

    /** 加入固定在某个屏幕坐标的控件（底部按钮等），不参与滚动。 */
    protected final <T extends AbstractWidget> T fixedWidget(T widget, int bandY, int bandHeight) {
        bands.add(new Band(widget, bandY, bandHeight, true, () -> true));
        widget.setY(bandY);
        addRenderableWidget(widget);
        return widget;
    }

    /** 页签等外部条件变化后重新套用显隐规则。 */
    protected final void refreshFlowVisibility() {
        applyScroll();
    }

    protected final int maxScroll() {
        return Math.max(0, cursor - ROW_GAP - contentBottom);
    }

    /** 当前内容滚动位置，供重建后恢复或测试布局稳定性。 */
    protected final int scrollPosition() {
        return scroll;
    }

    /** 流坐标横带是否完整落在视口内（与 {@link #flowWidget} 的显隐规则一致）。 */
    protected final boolean bandFits(int bandY, int bandHeight) {
        int y = bandY - scroll;
        return y >= contentTop && y + bandHeight <= contentBottom;
    }

    /** 流坐标转屏幕坐标。 */
    protected final int bandScreenY(int bandY) {
        return bandY - scroll;
    }

    /** 以流坐标绘制一段文字 / 图形：自动平移滚动量，带不完整可见时整带跳过。 */
    protected final void paintBand(GuiGraphics graphics, int bandY, int bandHeight, Runnable painter) {
        if (!bandFits(bandY, bandHeight)) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0.0D, -scroll, 0.0D);
        painter.run();
        graphics.pose().popPose();
    }

    private void placeScrollable(Band band) {
        AbstractWidget widget = band.widget();
        int y = band.bandY() - scroll;
        widget.setY(y);
        widget.visible = y >= contentTop && y + band.bandHeight() <= contentBottom
                && band.extraVisible().getAsBoolean();
    }

    private void applyScroll() {
        for (Band band : bands) {
            if (!band.fixed()) {
                placeScrollable(band);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (choices.scroll(amount)) return true;
        if (super.mouseScrolled(mouseX, mouseY, amount)) {
            return true;
        }
        int maximum = maxScroll();
        if (maximum > 0 && mouseY >= contentTop && mouseY <= panelBottom - 4) {
            int next = clamp(scroll - (int) Math.round(amount * 24), 0, maximum);
            if (next != scroll) {
                scroll = next;
                applyScroll();
            }
            return true;
        }
        return false;
    }

    private void renderUiBackground(GuiGraphics graphics) {
        renderBackground(graphics);
        UiTheme.tintScreen(graphics, width, height);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        choices.render(graphics, font, mouseX, mouseY);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (choices.click(x, y, button)) return true;
        if (button == 0 && beginScrollbarDrag(x, y)) return true;
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
        if (button == 0 && draggingScrollbar) {
            dragScrollbar(y);
            return true;
        }
        return super.mouseDragged(x, y, button, dragX, dragY);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(x, y, button);
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        return choices.key(key) || super.keyPressed(key, scan, modifiers);
    }

    /** 绘制背景、面板、面板内标题区和滚动条；标题永远不会压到内容区，无标题带界面只画面板。 */
    protected final void renderShell(GuiGraphics graphics, String subtitle) {
        renderUiBackground(graphics);
        UiTheme.panel(graphics, panelLeft, panelTop, panelWidth, panelBottom - panelTop);
        if (!hasHeaderBand) {
            renderScrollbar(graphics);
            return;
        }
        graphics.drawString(font, fit(getTitle().getString(), innerWidth), innerLeft,
                panelTop + 7, UiTheme.TEXT, false);
        if (subtitle != null && !subtitle.isBlank()) {
            graphics.drawString(font, fit(subtitle, innerWidth), innerLeft, panelTop + 20,
                    UiTheme.MUTED, false);
        }
        UiTheme.divider(graphics, panelLeft + 1, panelTop + HEADER_HEIGHT - 3, panelWidth - 2);
        UiTheme.divider(graphics, innerLeft, footerTop - 5, innerWidth);
        renderScrollbar(graphics);
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int maximum = maxScroll();
        int trackHeight = contentBottom - contentTop;
        if (maximum <= 0 || trackHeight < 12) {
            return;
        }
        int x = scrollbarX();
        graphics.fill(x, contentTop, x + 8, contentBottom, UiTheme.BORDER_SUBTLE);
        int thumbHeight = scrollbarThumbHeight(trackHeight, maximum);
        int range = Math.max(1, trackHeight - thumbHeight);
        int thumbTop = contentTop + (int) ((long) scroll * range / maximum);
        graphics.fill(x + 1, thumbTop, x + 7, thumbTop + thumbHeight, UiTheme.ACCENT);
        graphics.renderOutline(x, thumbTop, 8, thumbHeight, UiTheme.BORDER);
    }

    /** 固定状态条：位于内容视口与底部操作区之间，永远完整可见；界面关闭状态区带时不绘制。 */
    protected final void renderStatus(GuiGraphics graphics, String message, int color) {
        if (!hasStatusBand || message == null || message.isBlank()) {
            return;
        }
        UiTheme.status(graphics, font, fit(message, innerWidth - 18), innerLeft, statusTop,
                innerWidth, color);
    }

    /** 历史遗留的标准按钮宽度；自本轮起底部按钮改为平分整行宽度，此值仅供非底部控件参考。 */
    public static final int FOOTER_BUTTON_WIDTH = 150;

    /**
     * 底部区列宽：所有底部按钮平分整行内容宽度（列数一致则宽度一致），
     * 不再使用统一固定 150 像素——这就是「所有底部UI按钮平分底部宽度」标准。
     */
    protected final int footerColumnWidth(int columns) {
        int gap = footerGap();
        int safeColumns = Math.max(1, columns);
        return Math.max(1, (innerWidth - gap * (safeColumns - 1)) / safeColumns);
    }

    /** 底部列起点：平分后天然占满内容区，仅保留整除余数带来的水平居中修正。 */
    protected final int footerColumnX(int column, int columns) {
        int gap = footerGap();
        int safeColumns = Math.max(1, columns);
        int colWidth = footerColumnWidth(safeColumns);
        int rowWidth = colWidth * safeColumns + gap * (safeColumns - 1);
        int startX = innerLeft + Math.max(0, (innerWidth - rowWidth) / 2);
        return startX + Math.max(0, column) * (colWidth + gap);
    }

    protected final int footerRowY(int row) {
        return footerTop + Math.max(0, row) * (BUTTON_HEIGHT + 4);
    }

    /** 添加一个固定底部按钮；footerHeight 必须按行数预留：rows * 20 + (rows - 1) * 4。 */
    protected final UiButton footerButton(String label, int column, int columns, int row,
                                          Runnable action, String tooltip, UiButton.Kind kind) {
        UiButton button = uiButton(label, footerColumnX(column, columns), footerRowY(row),
                footerColumnWidth(columns), action, tooltip, kind);
        return fixedWidget(button, footerRowY(row), BUTTON_HEIGHT);
    }

    private int footerGap() {
        return clamp(innerWidth / 120, 2, 6);
    }

    /** 内容区列宽（两列表单等）。 */
    protected final int columnWidth(int columns, int gap) {
        int safeColumns = Math.max(1, columns);
        int safeGap = Math.max(0, gap);
        return Math.max(1, (innerWidth - safeGap * (safeColumns - 1)) / safeColumns);
    }

    protected final int columnX(int column, int columns, int gap) {
        int safeColumns = Math.max(1, columns);
        int safeGap = Math.max(0, gap);
        return innerLeft + Math.max(0, column) * (columnWidth(columns, gap) + safeGap);
    }

    protected UiButton uiButton(String label, int x, int y, int buttonWidth, Runnable action, String tooltip) {
        return uiButton(label, x, y, buttonWidth, action, tooltip, UiButton.Kind.SECONDARY);
    }

    protected UiButton uiButton(String label, int x, int y, int buttonWidth, Runnable action,
                                String tooltip, UiButton.Kind kind) {
        if (label.startsWith("返回")) kind = UiButton.Kind.SECONDARY;
        UiButton button = new UiButton(x, y, buttonWidth, BUTTON_HEIGHT, Component.literal(label),
                ignored -> action.run(), kind);
        if (tooltip != null && !tooltip.isBlank()) {
            button.setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
        return button;
    }

    protected final void confirmAction(String title, String message, Runnable action) {
        if (minecraft != null) minecraft.setScreen(new UiConfirmScreen(this, title, message, action));
    }

    protected String fit(String value, int maximumWidth) {
        return UiTheme.fit(font, value, maximumWidth);
    }

    protected void section(GuiGraphics graphics, String label, int x, int y, int sectionWidth) {
        UiTheme.section(graphics, font, label, x, y, sectionWidth);
    }

    protected void divider(GuiGraphics graphics, int x, int y, int dividerWidth) {
        UiTheme.divider(graphics, x, y, dividerWidth);
    }

    protected void badge(GuiGraphics graphics, String label, int x, int y, int badgeWidth, int color) {
        UiTheme.badge(graphics, font, label, x, y, badgeWidth, color);
    }

    protected void progress(GuiGraphics graphics, int x, int y, int progressWidth, int progressHeight,
                            float amount, int color) {
        UiTheme.progress(graphics, x, y, progressWidth, progressHeight, amount, color);
    }

    protected static int clamp(int value, int minimum, int maximum) {
        return maximum < minimum ? minimum : Math.max(minimum, Math.min(maximum, value));
    }

    private boolean beginScrollbarDrag(double mouseX, double mouseY) {
        int maximum = maxScroll();
        if (maximum <= 0 || mouseX < scrollbarX() || mouseX > scrollbarX() + 8
                || mouseY < contentTop || mouseY > contentBottom) return false;
        int trackHeight = contentBottom - contentTop;
        int thumbHeight = scrollbarThumbHeight(trackHeight, maximum);
        int thumbTop = contentTop + (int) ((long) scroll * (trackHeight - thumbHeight) / maximum);
        if (mouseY >= thumbTop && mouseY <= thumbTop + thumbHeight) {
            scrollbarDragOffset = (int) mouseY - thumbTop;
        } else {
            scrollbarDragOffset = thumbHeight / 2;
            dragScrollbar(mouseY);
        }
        draggingScrollbar = true;
        return true;
    }

    private void dragScrollbar(double mouseY) {
        int maximum = maxScroll();
        int trackHeight = contentBottom - contentTop;
        int thumbHeight = scrollbarThumbHeight(trackHeight, maximum);
        int range = Math.max(1, trackHeight - thumbHeight);
        int thumbTop = clamp((int) Math.round(mouseY) - scrollbarDragOffset, contentTop, contentTop + range);
        int next = clamp((int) Math.round((thumbTop - contentTop) * (double) maximum / range), 0, maximum);
        if (next != scroll) {
            scroll = next;
            applyScroll();
        }
    }

    private int scrollbarX() {
        return panelLeft + panelWidth - 10;
    }

    private static int scrollbarThumbHeight(int trackHeight, int maximum) {
        return Math.max(16, (int) ((long) trackHeight * trackHeight / Math.max(1, trackHeight + maximum)));
    }
}
