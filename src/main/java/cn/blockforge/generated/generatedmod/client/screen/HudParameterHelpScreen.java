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
    private final cn.blockforge.generated.generatedmod.client.HudContext context;
    private final List<Entry> entries = new ArrayList<>();
    private String query = "";

    public HudParameterHelpScreen(Screen parent) {
        this(parent, cn.blockforge.generated.generatedmod.client.HudContext.TEAM_DEATHMATCH);
    }

    public HudParameterHelpScreen(Screen parent, cn.blockforge.generated.generatedmod.client.HudContext context) {
        super(Component.literal("参数帮助"));
        this.parent = parent;
        this.context = context;
    }

    @Override
    protected void init() {
        beginLayout(640, 0, BUTTON_HEIGHT, false);
        entries.clear();
        int searchY = flowRow(BUTTON_HEIGHT);
        UiEditBox search = new UiEditBox(font, innerLeft, searchY, innerWidth, BUTTON_HEIGHT,
                Component.literal("搜索数据，例如：击杀、倒计时、A队"));
        search.setMaxLength(64);
        search.setHint(Component.literal("搜索数据，例如：击杀、倒计时、A队"));
        search.setValue(query);
        search.moveCursorToEnd();
        search.setResponder(value -> {
            query = value;
            rebuildWidgets();
        });
        flowWidget(search, searchY);
        setInitialFocus(search);
        if (query.isBlank()) {
            add("1  写内容", "例如：A队击杀 {match_kills_a}，目标 {target_kills}");
            add("2  点“+ 参数”", "选择数据后自动插入到文字末尾；一段文字可放多个参数。");
            add("3  进度条单独绑定", "进度条选择一个数据来源；标签可写：%s、%v、%m。");
            add("当前场景：" + context.tabLabel(), "只显示这个场景可用的数据。下面每项都可插入。" );
        }
        for (var source : HudStats.sourcesFor(context)) {
            String key = source.id();
            String meaning = source.name();
            if (!(key + " " + meaning).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) continue;
            add(meaning, "{" + key + "}    预览：" + source.display(true));
        }
        if (entries.isEmpty()) add("没有找到", "换一个词试试，例如：击杀、倒计时、伤害、人数。");
        footerButton("返回编辑", 0, 1, 0, this::onClose, "返回 HUD 编辑器，保留未保存改动。", UiButton.Kind.PRIMARY);
    }

    private void add(String title, String detail) {
        int y = flowRow(30);
        entries.add(new Entry(y, title, detail));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderShell(graphics, "文字照写；需要变化的数据点“+ 参数”插入");
        for (Entry entry : entries) {
            paintBand(graphics, entry.y(), 30, () -> {
                graphics.drawString(font, fit(entry.title(), innerWidth), innerLeft, entry.y() + 3, UiTheme.TEXT, false);
                graphics.drawString(font, fit(entry.detail(), innerWidth), innerLeft, entry.y() + 16, UiTheme.MUTED, false);
            });
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
