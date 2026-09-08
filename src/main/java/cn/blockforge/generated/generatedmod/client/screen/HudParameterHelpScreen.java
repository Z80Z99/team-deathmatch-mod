package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.HudParameters;
import cn.blockforge.generated.generatedmod.client.HudStats;
import cn.blockforge.generated.generatedmod.client.ui.UiButton;
import cn.blockforge.generated.generatedmod.client.ui.UiEditBox;
import cn.blockforge.generated.generatedmod.client.ui.UiScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HudParameterHelpScreen extends UiScreen {
    private record Entry(int y, String title, String detail) { }
    private final Screen parent;
    private final List<Entry> entries = new ArrayList<>();
    private String query = "";

    public HudParameterHelpScreen(Screen parent) {
        super(Component.literal("参数配置 · 说明书"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        beginLayout(640, 0, BUTTON_HEIGHT, false);
        entries.clear();
        int searchY = flowRow(BUTTON_HEIGHT);
        UiEditBox search = new UiEditBox(font, innerLeft, searchY, innerWidth, BUTTON_HEIGHT,
                Component.literal("搜索参数名称或含义"));
        search.setMaxLength(64);
        search.setHint(Component.literal("搜索参数名称或含义"));
        search.setValue(query);
        search.moveCursorToEnd();
        search.setResponder(value -> {
            query = value;
            rebuildWidgets();
        });
        flowWidget(search, searchY);
        setInitialFocus(search);
        if (query.isBlank()) {
            add("只替换值，不自动加标签", "回合 {round} → 回合 1；{round} → 1");
            add("固定文字", "输入 A队 114 → A队 114，不再追加分数。");
            add("单位写在参数外", "目标 {target}；延迟 {ping} ms；已等待 {wait}");
            add("绑定数据源（可选）", "%s 格式化值，%v 原始数，%m 上限。");
            add("进度条", "%s 为百分比；空模板只画进度条，不显示标签。");
            add("示例与实战", "说明书中的值仅作示例；实战使用当前同步数据。");
            add("独立示例元素", "文字、数值、底板可分别删除；模板在组件页。");
        }
        for (var entry : HudParameters.values(true).entrySet()) {
            String key = entry.getKey();
            HudStats.Source source = HudStats.byId(key);
            String meaning = source == null ? HudParameters.description(key) : source.name();
            if (!(key + " " + meaning).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) continue;
            add("{" + key + "}  " + meaning, "示例值：" + entry.getValue());
        }
        if (entries.isEmpty()) add("无匹配参数", "可搜索 round、比分、房间、血量等。");
        footerButton("返回编辑", 0, 1, 0, this::onClose, "返回 HUD 编辑器，保留未保存改动。", UiButton.Kind.PRIMARY);
    }

    private void add(String title, String detail) {
        int y = flowRow(34);
        entries.add(new Entry(y, title, detail));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderShell(graphics, "名称、含义与示例");
        for (Entry entry : entries) {
            paintBand(graphics, entry.y(), 34, () -> {
                graphics.drawString(font, fit(entry.title(), innerWidth), innerLeft, entry.y() + 3, UiTheme.TEXT, false);
                graphics.drawString(font, fit(entry.detail(), innerWidth), innerLeft, entry.y() + 18, UiTheme.MUTED, false);
            });
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
