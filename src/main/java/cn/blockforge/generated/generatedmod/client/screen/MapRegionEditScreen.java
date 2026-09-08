package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ui.UiButton;
import cn.blockforge.generated.generatedmod.client.ui.UiCycleButton;
import cn.blockforge.generated.generatedmod.client.ui.UiEditBox;
import cn.blockforge.generated.generatedmod.client.ui.UiScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import cn.blockforge.generated.generatedmod.map.MapRegion;
import cn.blockforge.generated.generatedmod.map.MapRegionAction;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.MapRegionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/** 规划器选中的区域属性编辑界面。 */
public final class MapRegionEditScreen extends UiScreen {
    private static final int MENU_WIDTH = 460;
    private final MapRegion region;
    private final boolean builtIn;
    private UiEditBox name;
    private UiEditBox activationValue;
    private UiEditBox notes;
    private UiCycleButton<MapRegion.Type> type;
    private UiCycleButton<Boolean> visible;
    private UiCycleButton<Integer> range;
    private UiCycleButton<MapRegion.Appearance> appearance;
    private UiCycleButton<MapRegion.Activation> activation;
    private UiCycleButton<Integer> color;
    private UiCycleButton<Boolean> outline;
    private UiCycleButton<Boolean> fill;
    private UiCycleButton<Integer> priority;
    private List<String> helpLines = List.of();
    private int helpY;
    private int builtInInfoY;

    public MapRegionEditScreen(MapRegion region) {
        super(Component.literal("区域编辑"));
        this.region = region;
        this.builtIn = "bounds".equals(region.id()) || "reset".equals(region.id());
    }

    @Override
    protected void init() {
        beginLayout(MENU_WIDTH, 0, BUTTON_HEIGHT * 2 + 8, true, true);
        int titleY = flowRow(18);
        flowWidget(uiButton("？", innerLeft + innerWidth - 24, titleY, 22, this::toggleHelp,
                "查看区域属性的全部说明。", UiButton.Kind.SECONDARY), titleY);
        if (!helpLines.isEmpty()) {
            helpY = flowRow(helpLines.size() * 12 + 10);
        }

        if (builtIn) {
            builtInInfoY = flowRow(58);
            footerButton("关闭", 0, 2, 0, this::onClose, "返回规划器菜单。", UiButton.Kind.SECONDARY);
            footerButton("用画笔调整范围", 1, 2, 0, this::closeToGame,
                    "关闭菜单并使用地图画笔调整这个区域的范围。", UiButton.Kind.PRIMARY);
            return;
        }

        int controlWidth = innerWidth - 30;
        int helpX = innerLeft + innerWidth - 24;
        int nameY = flowRow(BUTTON_HEIGHT);
        name = flowWidget(new UiEditBox(font, innerLeft, nameY, controlWidth, BUTTON_HEIGHT,
                Component.literal("区域名称")), nameY);
        name.setMaxLength(64);
        name.setValue(region.displayName());
        flowWidget(helpButton(helpX, nameY,
                "名称会显示在规划器菜单、HUD 和后续玩法提示中。",
                "建议使用简短、可辨认的中文名称。"), nameY);

        int typeY = flowRow(BUTTON_HEIGHT);
        type = flowWidget(new UiCycleButton<>(innerLeft, typeY, controlWidth, BUTTON_HEIGHT,
                List.of(MapRegion.Type.CUSTOM, MapRegion.Type.BOMB, MapRegion.Type.CAPTURE,
                        MapRegion.Type.HOTSPOT, MapRegion.Type.OBJECTIVE, MapRegion.Type.OTHER),
                region.type(), MapRegion.Type::displayName, ignored -> { },
                "区域在玩法中的用途。", UiButton.Kind.SECONDARY), typeY);
        flowWidget(helpButton(helpX, typeY,
                "类型用于区分自定义、爆破、占领、热点、目标和普通区域。",
                "内置边界和重置区域不能修改类型。",
                "当前版本保存类型，后续玩法逻辑会按类型接入。"), typeY);

        int visibleY = flowRow(BUTTON_HEIGHT);
        visible = flowWidget(new UiCycleButton<>(innerLeft, visibleY, controlWidth, BUTTON_HEIGHT,
                List.of(Boolean.TRUE, Boolean.FALSE), region.visibleInMatch(),
                value -> value ? "对局内显示" : "对局内隐藏", ignored -> { },
                "是否在对局中显示这个区域。", UiButton.Kind.SECONDARY), visibleY);
        flowWidget(helpButton(helpX, visibleY,
                "决定区域提示是否允许在对局内出现。",
                "编辑器中仍会显示边框，便于继续调整。"), visibleY);

        int rangeY = flowRow(BUTTON_HEIGHT);
        range = flowWidget(new UiCycleButton<>(innerLeft, rangeY, controlWidth, BUTTON_HEIGHT,
                List.of(16, 32, 48, 64, 96, 128, 192, 256), region.displayRange(),
                value -> value + " 格", ignored -> { },
                "对局内显示这个区域的有效距离。", UiButton.Kind.SECONDARY), rangeY);
        flowWidget(helpButton(helpX, rangeY,
                "玩家与区域中心距离超过该值时，对局提示会隐藏。",
                "边界和重置区域不受此项影响。"), rangeY);

        int appearanceY = flowRow(BUTTON_HEIGHT);
        appearance = flowWidget(new UiCycleButton<>(innerLeft, appearanceY, controlWidth, BUTTON_HEIGHT,
                List.of(MapRegion.Appearance.ALWAYS, MapRegion.Appearance.NEARBY,
                        MapRegion.Appearance.EDITOR_ONLY, MapRegion.Appearance.HIDDEN),
                region.appearance(), MapRegion.Appearance::displayName, ignored -> { },
                "区域外观的显示策略。", UiButton.Kind.SECONDARY), appearanceY);
        flowWidget(helpButton(helpX, appearanceY,
                "始终显示：不限制；靠近显示：受显示距离限制。",
                "仅编辑器：只拿工具时显示；隐藏：不绘制区域。"), appearanceY);

        int activationY = flowRow(BUTTON_HEIGHT);
        activation = flowWidget(new UiCycleButton<>(innerLeft, activationY, controlWidth, BUTTON_HEIGHT,
                List.of(MapRegion.Activation.ALWAYS, MapRegion.Activation.MATCH_ONLY,
                        MapRegion.Activation.MODE, MapRegion.Activation.CONDITION),
                region.activation(), MapRegion.Activation::displayName, ignored -> { },
                "区域什么时候参与玩法逻辑。", UiButton.Kind.SECONDARY), activationY);
        flowWidget(helpButton(helpX, activationY,
                "始终生效：不限制；仅比赛：进入对局后生效。",
                "指定模式：填写模式 ID 后生效。",
                "条件表达式：为后续玩法脚本预留。"), activationY);

        int activationValueY = flowRow(BUTTON_HEIGHT);
        activationValue = flowWidget(new UiEditBox(font, innerLeft, activationValueY, controlWidth,
                BUTTON_HEIGHT, Component.literal("模式 / 条件")), activationValueY);
        activationValue.setMaxLength(128);
        activationValue.setValue(region.activationValue());
        flowWidget(helpButton(helpX, activationValueY,
                "指定模式时填写模式 ID，例如 bomb、capture、hotspot。",
                "条件表达式目前只保存文本，后续版本接入解析。"), activationValueY);

        int colorY = flowRow(BUTTON_HEIGHT);
        color = flowWidget(new UiCycleButton<>(innerLeft, colorY, controlWidth, BUTTON_HEIGHT,
                List.of(0xFF70C7E8, 0xFF63D39A, 0xFFFFC857, 0xFFFF7070, 0xFF5599FF,
                        0xFF9CE5AF, 0xFFB8BCC2, 0xFFE05555),
                region.color(), value -> "颜色 " + Integer.toHexString(value & 0xFFFFFF),
                ignored -> { }, "区域边框和填充色。", UiButton.Kind.SECONDARY), colorY);
        flowWidget(helpButton(helpX, colorY,
                "颜色同时作用于边框和半透明填充。",
                "悬停或选中时颜色会带呼吸光效果。"), colorY);

        int outlineY = flowRow(BUTTON_HEIGHT);
        outline = flowWidget(new UiCycleButton<>(innerLeft, outlineY, controlWidth, BUTTON_HEIGHT,
                List.of(Boolean.TRUE, Boolean.FALSE), region.outline(),
                value -> value ? "显示边框" : "隐藏边框", ignored -> { },
                "是否绘制区域边框。", UiButton.Kind.SECONDARY), outlineY);
        flowWidget(helpButton(helpX, outlineY,
                "关闭边框后仍可显示填充。",
                "至少保留边框或填充之一，编辑时更容易定位。"), outlineY);

        int fillY = flowRow(BUTTON_HEIGHT);
        fill = flowWidget(new UiCycleButton<>(innerLeft, fillY, controlWidth, BUTTON_HEIGHT,
                List.of(Boolean.TRUE, Boolean.FALSE), region.fill(),
                value -> value ? "显示填充" : "隐藏填充", ignored -> { },
                "是否绘制半透明填充。", UiButton.Kind.SECONDARY), fillY);
        flowWidget(helpButton(helpX, fillY,
                "填充是半透明效果，不会阻挡视线。",
                "关闭填充后仍可显示边框。"), fillY);

        int priorityY = flowRow(BUTTON_HEIGHT);
        priority = flowWidget(new UiCycleButton<>(innerLeft, priorityY, controlWidth, BUTTON_HEIGHT,
                List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10), region.priority(),
                value -> "优先级 " + value, ignored -> { },
                "区域重叠时的优先级。", UiButton.Kind.SECONDARY), priorityY);
        flowWidget(helpButton(helpX, priorityY,
                "数字越大越靠前，用于处理重叠区域的选择和提示。",
                "相同优先级时，先按准星距离，再按区域体积排序。"), priorityY);

        int notesY = flowRow(BUTTON_HEIGHT);
        notes = flowWidget(new UiEditBox(font, innerLeft, notesY, controlWidth, BUTTON_HEIGHT,
                Component.literal("备注")), notesY);
        notes.setMaxLength(128);
        notes.setValue(region.notes());
        flowWidget(helpButton(helpX, notesY,
                "备注只保存在地图 JSON 中，供地图作者记录设计意图。",
                "不会在对局 HUD 中显示。"), notesY);

        footerButton("保存", 0, 3, 0, this::save, "保存这个区域的属性。", UiButton.Kind.PRIMARY);
        footerButton("删除", 1, 3, 0, this::delete, "删除这个自定义区域。", UiButton.Kind.DANGER);
        footerButton("关闭", 2, 3, 0, this::onClose, "不保存并返回。", UiButton.Kind.SECONDARY);
    }

    private UiButton helpButton(int x, int y, String... lines) {
        return uiButton("？", x, y, 24, () -> showHelp(lines),
                "查看这一项的详细使用说明。", UiButton.Kind.SECONDARY);
    }

    private void save() {
        MapRegion updated = new MapRegion(region.id(), name.getValue(), type.getValue(), region.region(),
                visible.getValue(), range.getValue(), appearance.getValue(), activation.getValue(),
                activationValue.getValue(), color.getValue(), outline.getValue(), fill.getValue(),
                priority.getValue(), notes.getValue());
        FpsTdmNetwork.sendToServer(new MapRegionPacket(MapRegionAction.SAVE, updated, 0));
        onClose();
    }

    private void delete() {
        FpsTdmNetwork.sendToServer(new MapRegionPacket(MapRegionAction.DELETE, region, 0));
        onClose();
    }

    private void toggleHelp() {
        showHelp(
                "区域属性会完整写入地图 JSON，并可随邀请码副本导入。",
                "范围需要用画笔调整；这里编辑名称、类型和显示策略。",
                "出现逻辑为后续玩法接入保留，当前先完成配置与同步。",
                "保存前可随时关闭，未保存修改不会写入服务器。");
    }

    private void showHelp(String... lines) {
        List<String> next = List.of(lines);
        helpLines = helpLines.equals(next) ? List.of() : next;
        rebuildWidgets();
    }

    private void closeToGame() {
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderShell(graphics, region.displayName());
        if (!helpLines.isEmpty()) {
            int y = helpY + 4;
            for (String line : helpLines) {
                graphics.drawString(font, fit(line, innerWidth - 16),
                        innerLeft + 8, y, UiTheme.TEXT, false);
                y += 12;
            }
        }
        if (builtIn) {
            int y = builtInInfoY + 4;
            graphics.drawString(font, fit("这是内置区域，范围请用地图画笔调整。", innerWidth - 16),
                    innerLeft + 8, y, UiTheme.TEXT, false);
            graphics.drawString(font, fit("当前范围：" + region.region(), innerWidth - 16),
                    innerLeft + 8, y + 18, UiTheme.MUTED, false);
            graphics.drawString(font, fit("内置区域不能删除，也不能修改类型。", innerWidth - 16),
                    innerLeft + 8, y + 36, UiTheme.MUTED, false);
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
