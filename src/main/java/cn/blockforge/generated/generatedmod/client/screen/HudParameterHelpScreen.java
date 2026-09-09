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
            add("直接写文字", "输入：A队     显示：A队");
            add("文字里插入参数", "输入：回合 {round}     显示：回合 1");
            add("已经选择数据来源？", "文字填 %s 显示该数据；进度条填 %s 显示百分比。留空则不显示文字。");
            add("当前场景：" + context.tabLabel(), "下面是可用参数。示例数字仅用于说明。%v 是原始值，%m 是上限。");
        }
        for (var source : HudStats.sourcesFor(context)) {
            String key = source.id();
            String meaning = source.name();
            if (!(key + " " + meaning).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) continue;
            add(meaning, "输入：{" + key + "}     显示：" + source.display(true));
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
        renderShell(graphics, "文字照写，变量放在 { } 中");
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
