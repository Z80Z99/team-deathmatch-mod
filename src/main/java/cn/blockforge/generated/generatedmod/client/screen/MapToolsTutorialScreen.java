package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ui.UiButton;
import cn.blockforge.generated.generatedmod.client.ui.UiCycleButton;
import cn.blockforge.generated.generatedmod.client.ui.UiScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** 地图工具的完整分章教程。 */
public final class MapToolsTutorialScreen extends UiScreen {
    private static final int MENU_WIDTH = 520;

    private final Screen parent;
    private Chapter chapter = Chapter.QUICK_START;
    private List<RenderLine> renderLines = List.of();
    private UiCycleButton<Chapter> chapterButton;

    public MapToolsTutorialScreen(Screen parent) {
        super(Component.literal("地图工具教程"));
        this.parent = parent;
    }

    public static void open(Screen parent) {
        Minecraft.getInstance().setScreen(new MapToolsTutorialScreen(parent));
    }

    @Override
    protected void init() {
        beginLayout(MENU_WIDTH, 0, BUTTON_HEIGHT, true, true);

        int navigationY = flowRow(BUTTON_HEIGHT);
        int navigationGap = 6;
        int navigationWidth = columnWidth(3, navigationGap);
        flowWidget(uiButton("上一章", columnX(0, 3, navigationGap), navigationY, navigationWidth,
                this::previousChapter, "阅读上一章。", UiButton.Kind.SECONDARY), navigationY);
        chapterButton = flowWidget(new UiCycleButton<>(columnX(1, 3, navigationGap), navigationY,
                navigationWidth, BUTTON_HEIGHT, List.of(Chapter.values()), chapter,
                Chapter::title, this::selectChapter, "选择要阅读的教程章节。", UiButton.Kind.PRIMARY), navigationY);
        chapterButton.setSelected(true);
        flowWidget(uiButton("下一章", columnX(2, 3, navigationGap), navigationY, navigationWidth,
                this::nextChapter, "阅读下一章。", UiButton.Kind.SECONDARY), navigationY);

        flowSpace(8);
        renderLines = new ArrayList<>();
        for (Section section : chapter.sections()) {
            int headingY = flowRow(LineKind.HEADING.height);
            renderLines.add(new RenderLine(LineKind.HEADING, section.title(), headingY));
            for (TutorialLine line : section.lines()) {
                for (String wrapped : wrap(line.text(), innerWidth - 14)) {
                    int lineY = flowRow(line.kind().height);
                    renderLines.add(new RenderLine(line.kind(), wrapped, lineY));
                }
            }
            flowSpace(10);
        }

        footerButton("打开地图工作台", 0, 2, 0, () -> MapLibraryScreen.open(this),
                "返回地图列表，可新建、编辑或分享地图。", UiButton.Kind.SECONDARY);
        footerButton("关闭教程", 1, 2, 0, this::onClose,
                "返回打开教程前的界面。", UiButton.Kind.PRIMARY);
    }

    private void previousChapter() {
        Chapter[] chapters = Chapter.values();
        selectChapter(chapters[(chapter.ordinal() + chapters.length - 1) % chapters.length]);
    }

    private void nextChapter() {
        Chapter[] chapters = Chapter.values();
        selectChapter(chapters[(chapter.ordinal() + 1) % chapters.length]);
    }

    private void selectChapter(Chapter next) {
        if (next == chapter) {
            return;
        }
        chapter = next;
        rebuildWidgets();
    }

    private List<String> wrap(String text, int maximumWidth) {
        if (text == null || text.isBlank()) {
            return List.of("");
        }
        List<String> result = new ArrayList<>();
        int start = 0;
        int width = 0;
        int index = 0;
        while (index < text.length()) {
            int codePoint = text.codePointAt(index);
            int codePointWidth = font.width(new String(Character.toChars(codePoint)));
            int codePointCount = Character.charCount(codePoint);
            if (width > 0 && width + codePointWidth > maximumWidth) {
                result.add(text.substring(start, index));
                start = index;
                width = 0;
            }
            width += codePointWidth;
            index += codePointCount;
        }
        result.add(text.substring(start));
        return result;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Chapter[] chapters = Chapter.values();
        renderShell(graphics, "第 " + (chapter.ordinal() + 1) + " / " + chapters.length + " 章");
        for (RenderLine line : renderLines) {
            paintBand(graphics, line.y(), line.kind().height, () -> renderLine(graphics, line));
        }
        renderStatus(graphics, "滚轮滚动阅读；顶部中间按钮可切换章节。", UiTheme.INFO);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderLine(GuiGraphics graphics, RenderLine line) {
        int textY = line.y() + Math.max(0, (line.kind().height - 9) / 2);
        switch (line.kind()) {
            case HEADING -> UiTheme.section(graphics, font, line.text(), innerLeft, line.y(), innerWidth);
            case BODY -> graphics.drawString(font, line.text(), innerLeft + 8, textY, UiTheme.TEXT, false);
            case NOTE -> graphics.drawString(font, line.text(), innerLeft + 8, textY, UiTheme.MUTED, false);
            case CODE -> {
                graphics.fill(innerLeft + 8, line.y(), innerLeft + innerWidth - 4,
                        line.y() + line.kind().height, UiTheme.withAlpha(UiTheme.INFO, 10));
                graphics.drawString(font, line.text(), innerLeft + 14, textY, UiTheme.INFO, false);
            }
        }
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

    private enum LineKind {
        HEADING(16),
        BODY(12),
        NOTE(12),
        CODE(14);

        private final int height;

        LineKind(int height) {
            this.height = height;
        }
    }

    private record TutorialLine(LineKind kind, String text) {
    }

    private record RenderLine(LineKind kind, String text, int y) {
    }

    private record Section(String title, List<TutorialLine> lines) {
    }

    private static TutorialLine body(String text) {
        return new TutorialLine(LineKind.BODY, text);
    }

    private static TutorialLine note(String text) {
        return new TutorialLine(LineKind.NOTE, text);
    }

    private static TutorialLine code(String text) {
        return new TutorialLine(LineKind.CODE, text);
    }

    private static List<TutorialLine> lines(TutorialLine... values) {
        return List.of(values);
    }

    private enum Chapter {
        QUICK_START("快速开始", List.of(
                new Section("这一章会解决什么问题", lines(
                        body("不用记复杂按键：从拿到工具到测试地图，按顺序照做即可。"),
                        body("地图工具只修改“地图配置”，不会破坏、挖掘或补放真实方块。"),
                        note("推荐先在测试世界练习，熟悉后再制作正式地图。"))),
                new Section("推荐建图流程", lines(
                        body("1. 按 K 打开地图工作台，或从暂停菜单进入“我的地图”。"),
                        body("2. 点击“获得工具”，领取地图规划器和地图画笔。"),
                        body("3. 新建地图，填写名称和世界信息，然后进入编辑。"),
                        body("4. 手持规划器右键打开菜单，创建地图边界和重置区域。"),
                        body("5. 用画笔圈出边界与重置区域，再创建 A/B 队复活区域。"),
                        body("6. 需要多模式复用出生点时，在区域编辑页设置出现逻辑。"),
                        body("7. 分别用团队竞技和爆破模式测试，再生成邀请码分享。"))),
                new Section("最少需要做什么", lines(
                        body("地图边界和重置区域是硬性要求，缺少任意一项地图会显示未完成。"),
                        body("固定出生策略至少需要 A/B 队出生点或复活区域；C/D 队只在三队、四队房间需要。"),
                        body("使用随机出生时可以没有预设出生点，但仍必须有边界和重置区域。"))))),
        REQUIRED_REGIONS("必需区域", List.of(
                new Section("地图边界", lines(
                        body("定义比赛允许活动的空间；越界会触发警告、倒计时和相应判罚。"),
                        body("边界也会影响方块保护范围，建议把观众区、装饰区和不参与战斗的建筑放在边界外。"),
                        note("边界不需要贴住每个墙角，稍留安全余量更好。"))),
                new Section("重置区域", lines(
                        body("记录赛后需要恢复的地图范围。比赛中的破坏会在结束后按快照恢复。"),
                        body("如果重置范围与边界完全一致，可在边界编辑页选择“同时作为重置范围”。"),
                        body("如果只有爆破点附近会被破坏，可以使用更小的独立重置区域，减少快照体积。"))),
                new Section("出生点", lines(
                        body("固定出生需要每个参战队伍至少一个可用出生点或复活区域。"),
                        body("A/B 队是最低要求；C/D 队只在三队、四队房间启用。"),
                        body("观战出生点可选；没有设置时系统会使用其他安全回退逻辑。"),
                        note("推荐使用规划器创建“A队复活区域”等区域型出生点，后续调整更方便。"))))),
        PLANNER("规划器", List.of(
                new Section("打开与选择", lines(
                        body("手持地图规划器，单按右键打开菜单。"),
                        body("准星指向区域时，该区域边框会出现彩色呼吸光。"),
                        body("左键点击准星指向的区域，直接把它设为画笔目标。"),
                        body("右键打开菜单时，菜单会优先列出准星命中的区域。"),
                        body("站在区域 A 内指向区域 B 时，会优先选择 B，不会永远选中自己所在的区域。"))),
                new Section("重叠区域", lines(
                        body("多个区域重合或交错时，右键菜单会同时列出可编辑目标。"),
                        body("鼠标停留在菜单内区域名称上时，对应区域边框会发光。"),
                        body("点击名称即可选定该区域，然后用画笔或属性页继续编辑。"))),
                new Section("视角与退出", lines(
                        body("菜单顶部可切换第一人称、背后观察、正面观察，方便查看区域全貌。"),
                        body("打开菜单后不能切换手持物品，避免误操作。"),
                        body("“继续编辑”关闭菜单并保留当前视角。"),
                        body("“恢复进入前视角”会还原打开规划器前的视角并退出编辑菜单。"))),
                new Section("创建区域", lines(
                        body("缺少地图边界或重置区域时，菜单会显示对应创建按钮。"),
                        body("“新增自定义区域”会从脚下一个方块开始，并自动成为画笔目标。"),
                        body("创建后先用画笔圈范围，再进入属性页设置名称、类型和出现逻辑。"))))),
        BRUSH("画笔", List.of(
                new Section("开始之前", lines(
                        body("画笔必须先有目标：在规划器中创建或选定一个区域。"),
                        body("手持画笔蹲下并按右键，可打开画笔菜单。"),
                        body("画笔菜单可切换模式、调整空气选择距离，也能返回地图工作台。"))),
                new Section("两角框选模式", lines(
                        body("左键选择端点 A，右键选择端点 B。"),
                        body("两个端点可以是方块，也可以是空气；区域会按两个对角生成长方体。"),
                        body("第二个端点确认后自动保存，边框和填充会立即更新。"),
                        note("适合先快速圈出大范围，再用逐格模式修细节。"))),
                new Section("逐格增删模式", lines(
                        body("左键移除准星指向的一格区域，不会破坏真实方块。"),
                        body("右键在相邻位置添加一格区域，可用来修补或挖出内部空洞。"),
                        body("区域允许不连续；移除最后一格后区域会被删除，需要重新选择目标。"))),
                new Section("空气与距离", lines(
                        body("右键可以选择空气，默认取玩家前方 2 格的位置。"),
                        body("如果前方 2 格内有方块阻挡，会优先命中方块。"),
                        body("画笔菜单中的“空气距离”可调整为 1～64 格，方便远距离取点。"),
                        body("左键点击实际方块时不受空气距离限制。"))),
                new Section("屏幕提示", lines(
                        body("会被左右键影响的目标会显示类似玻璃块的指示标识。"),
                        body("手持工具时，左下角显示当前操作提示。"),
                        body("右下角显示目标坐标、模式和距离等信息。"),
                        note("保存成功后有短暂过渡效果：红色表示移除，青色表示添加。"))))),
        SPAWNS("出生点", List.of(
                new Section("推荐做法", lines(
                        body("用规划器创建“A队复活区域”“B队复活区域”“C队复活区域”“D队复活区域”。"),
                        body("区域型出生点可以框出完整空间，系统会在活动区域内寻找安全位置。"),
                        body("多个同队区域会合并参与选择，适合多出口基地或大型地图。"))),
                new Section("旧出生点标记", lines(
                        body("旧版“作者出生点标记”仍保留，用于兼容已有地图。"),
                        body("如果当前队伍存在可用复活区域，系统优先使用复活区域。"),
                        body("只有没有可用复活区域时，才回退到旧出生点标记。"),
                        note("新地图建议不再依赖旧标记，直接使用区域型出生点。"))),
                new Section("多模式共用", lines(
                        body("同一片 A/B 队复活区域可以被多个模式使用。"),
                        body("如果只在某些模式生效，把区域“出现逻辑”设为条件表达式。"),
                        body("团队竞技和爆破模式共用时，可填写："),
                        code("mode == \"TEAM_DEATHMATCH\" || mode == \"SEARCH_DESTROY\""))))),
        REGION_PROPERTIES("区域属性", List.of(
                new Section("基础信息", lines(
                        body("名称：显示在规划器、属性页和后续玩法提示中，建议简短明确。"),
                        body("类型：区分自定义、爆破、占领、热点、目标、其他和四队复活区域。"),
                        body("备注：只保存在地图 JSON 中，供作者记录设计意图，不会显示在对局 HUD。"))),
                new Section("显示设置", lines(
                        body("对局内显示：决定区域是否允许在对局中出现提示。"),
                        body("显示范围：玩家距离区域中心超过设定格数后隐藏提示。"),
                        body("外观策略：始终显示、靠近显示、仅编辑器显示或隐藏。"),
                        body("颜色、边框、填充：控制区域视觉效果；编辑器中仍可按需隐藏。"))),
                new Section("逻辑与排序", lines(
                        body("出现逻辑：决定这个区域何时参与玩法，当前对复活区域实时生效。"),
                        body("模式 / 条件：根据出现逻辑填写模式 ID 或条件表达式。"),
                        body("优先级：区域重叠时数字越大越优先，便于选择和提示。"),
                        note("范围本身不在属性页修改，请关闭属性页后用画笔调整。"))))),
        ACTIVATION("出现逻辑", List.of(
                new Section("四种规则", lines(
                        body("始终生效：不检查比赛状态，任何情况下都参与逻辑。"),
                        body("仅比赛中：热身和正式比赛阶段生效，比赛结束或未开赛时不生效。"),
                        body("指定模式：只支持一个模式；多个模式请改用条件表达式。"),
                        body("条件表达式：按当前比赛上下文计算，适合复杂规则。"))),
                new Section("模式写法", lines(
                        body("团队竞技：TEAM_DEATHMATCH、TDM、团队竞技。"),
                        body("爆破模式：SEARCH_DESTROY、SD、bomb、爆破模式。"),
                        body("歼灭竞技：LAST_STANDING、歼灭竞技。"),
                        note("capture 和 hotspot 当前是区域类型，不是可填写的比赛模式。"))),
                new Section("条件表达式", lines(
                        body("条件表达式是一行小规则，用来回答“这个区域现在要不要生效”。"),
                        body("它像一句话：变量提供当前比赛信息，比较符提出问题，逻辑词把多个问题连起来。"),
                        body("例如下面这句的意思是：现在是比赛，并且模式是爆破模式。"),
                        code("match && mode == \"SEARCH_DESTROY\""),
                        body("更复杂的写法、逐词解析和排错方法见下一章“表达式语法”。"))),
                new Section("注意事项", lines(
                        body("表达式为空或解析失败时，这个区域不会生效。"),
                        body("比赛模式改变、回合推进或激活上下文变化时，会重新筛选区域。"),
                        body("固定出生点缓存会在激活上下文变化时清空，避免旧模式的结果影响新模式。"))))),
        CONDITION_GRAMMAR("表达式语法", List.of(
                new Section("先看懂一行表达式", lines(
                        body("目标：只在爆破模式、第 1 回合显示 A 爆破点。"),
                        code("match && mode == \"SEARCH_DESTROY\" && round == 1"),
                        body("从左到右拆开："),
                        body("match：当前是否处于热身或正式比赛阶段。"),
                        body("&&：并且；左右两边都必须为真。"),
                        body("mode：当前比赛模式。"),
                        body("==：等于；这里比较 mode 和引号里的模式名。"),
                        body("\"SEARCH_DESTROY\"：爆破模式的正式英文名。"),
                        body("round：当前回合数，从 1 开始。"),
                        body("整句翻译：是比赛，并且模式是爆破，并且回合是 1。"))),
                new Section("变量完全解析", lines(
                        body("match / match_active：比赛是否处于热身或正式比赛阶段；值为 true 或 false。"),
                        body("mode：当前模式，值为 TEAM_DEATHMATCH、SEARCH_DESTROY 或 LAST_STANDING。"),
                        body("round：当前回合数；团队竞技通常为 1，爆破模式会随回合推进变化。"),
                        body("teams / team_count：本场参战队伍数量，可用 2、3、4 比较。"),
                        body("players / player_count：本场玩家数量，可用于人数门槛。"),
                        note("同一变量有两个名字是为了方便输入；例如 teams 与 team_count 完全等价。"))),
                new Section("值和引号", lines(
                        body("true / false：固定布尔值，表示“是 / 否”。"),
                        body("数字：直接写 1、2、8，不要加引号；用于回合、队伍数和人数。"),
                        body("文本：模式名要放在英文双引号中，例如 \"SEARCH_DESTROY\"。"),
                        body("中文双引号、单引号或缺少引号都会导致解析失败。"),
                        body("模式比较支持别名，正式名、缩写和中文都会被识别为同一个模式。"))),
                new Section("比较符完全解析", lines(
                        body("==：等于。数字比数字，布尔比布尔，模式名按别名等价比较。"),
                        body("!=：不等于；与 == 相反。"),
                        body("<：小于，例如 round < 3 表示第 1、2 回合。"),
                        body("<=：小于或等于，例如 round <= 2 表示第 1、2 回合。"),
                        body(">：大于，例如 players > 7 表示至少 8 名玩家。"),
                        body(">=：大于或等于，例如 teams >= 3 表示三队或四队。"),
                        note("比较符两边类型必须匹配；不能用 round == \"1\"，因为 round 是数字。"))),
                new Section("逻辑词完全解析", lines(
                        body("&&：并且；左右都为真才为真，常用于叠加模式、回合、人数条件。"),
                        body("||：或者；左右任意一边为真就为真，常用于多模式或多回合。"),
                        body("!：非；把 true 变 false，把 false 变 true，后面必须是一个布尔结果。"),
                        body("(...)：括号；强制优先计算括号内的内容，适合组合“或”和“并且”。"),
                        body("默认优先级：! 最先，然后比较符，然后 &&，最后 ||。"),
                        code("match && (mode == \"SD\" || mode == \"LAST_STANDING\")"),
                        body("这句表示：是比赛，并且模式是爆破或歼灭。括号避免逻辑被错误分组。"))),
                new Section("逐行解析示例", lines(
                        body("示例 1：只要正式比赛阶段，不限模式和回合。"),
                        code("match"),
                        body("解析：match 为 true 时区域生效；为 false 时不生效。"),
                        body("示例 2：爆破模式任意回合。"),
                        code("mode == \"SEARCH_DESTROY\""),
                        body("解析：mode 与爆破模式别名比较；相等时生效。"),
                        body("示例 3：第 1 回合或第 2 回合。"),
                        code("round == 1 || round == 2"),
                        body("解析：两个数字比较用“或者”连接；任意一个相等即生效。"),
                        body("示例 4：三队以上且人数不少于 8。"),
                        code("teams >= 3 && players >= 8"),
                        body("解析：队伍条件与人数条件必须同时满足。"),
                        body("示例 5：非团队竞技，也就是爆破或歼灭。"),
                        code("mode != \"TEAM_DEATHMATCH\""),
                        body("解析：模式不等于团队竞技时生效。"))),
                new Section("排错清单", lines(
                        body("引号必须成对，并使用英文双引号。"),
                        body("变量名不能拼错；未知变量会让表达式失效。"),
                        body("数字不要加引号，模式名必须加引号。"),
                        body("&& 和 || 不要写成中文“并且”“或者”。"),
                        body("括号必须左右成对。"),
                        body("表达式为空或解析失败时区域不生效，不会弹出报错窗口。"),
                        body("测试时先简化表达式，确认模式条件有效，再逐段加回合和人数条件。"))))),
        EXAMPLES("示例集", List.of(
                new Section("初阶 1：同一片出生区域给两个模式", lines(
                        body("需求：A/B 队出生区域在团队竞技和爆破模式都使用。"),
                        code("mode == \"TEAM_DEATHMATCH\" || mode == \"SEARCH_DESTROY\""),
                        body("解析：模式等于团队竞技，或者等于爆破模式，任一成立即可。"),
                        note("同一片区域只需要画一次；把表达式分别填在 A、B 队复活区域上。"))),
                new Section("初阶 2：只在比赛中生效", lines(
                        body("需求：区域不参与编辑器外闲逛，只在热身和正式比赛阶段生效。"),
                        code("match"),
                        body("解析：match 为 true 时生效。"),
                        note("如果只想限定模式，还要继续加 mode 条件。"))),
                new Section("初阶 3：三队或四队房间才显示提示区", lines(
                        body("需求：四队专用提示区，两队房间不显示。"),
                        code("teams >= 3"),
                        body("解析：参战队伍数大于或等于 3 时生效。"))),
                new Section("中阶 1：爆破模式第 1 回合 A 点", lines(
                        body("需求：第 1 回合使用爆破点 A。"),
                        code("match && mode == \"SEARCH_DESTROY\" && round == 1"),
                        body("解析：是比赛，并且是爆破模式，并且当前回合为 1。"),
                        body("第 1 回合结束后，round 变为 2，这个区域自动失效。"))),
                new Section("中阶 2：爆破模式第 2 回合 B 点", lines(
                        body("需求：第 2 回合使用完全不同的爆破点 B。"),
                        code("match && mode == \"SEARCH_DESTROY\" && round == 2"),
                        body("解析：是比赛，并且是爆破模式，并且当前回合为 2。"),
                        body("A、B 两个爆破区域分别填这两条表达式，位置可以完全不重叠。"))),
                new Section("中阶 3：第 3 回合及以后使用 C 点", lines(
                        body("需求：前两回合使用 A/B，第 3 回合起固定用 C。"),
                        code("match && mode == \"SEARCH_DESTROY\" && round >= 3"),
                        body("解析：爆破模式下，回合数大于或等于 3 时生效。"))),
                new Section("中阶 4：人数门槛", lines(
                        body("需求：8 人以上才启用额外出生区域，避免小房间出生过散。"),
                        code("match && mode == \"TEAM_DEATHMATCH\" && players >= 8"),
                        body("解析：比赛、团队竞技、玩家数至少 8 三个条件同时满足。"))),
                new Section("高阶 1：多模式与人数组合", lines(
                        body("需求：团队竞技或爆破模式均可使用，但爆破模式要求第 2 回合之后。"),
                        code("match && (mode == \"TEAM_DEATHMATCH\" || (mode == \"SEARCH_DESTROY\" && round > 1))"),
                        body("解析：先看括号；团队竞技直接通过，爆破模式还必须满足回合大于 1。"),
                        note("外层 match 确保非比赛阶段不生效。"))),
                new Section("高阶 2：排除某个模式", lines(
                        body("需求：区域用于所有模式，但团队竞技不显示。"),
                        code("match && mode != \"TEAM_DEATHMATCH\""),
                        body("解析：比赛阶段且模式不是团队竞技；当前等价于爆破或歼灭。"))),
                new Section("高阶 3：复杂分组", lines(
                        body("需求：两队房间用 A 区域；三队或四队且人数不少于 8 时用 B 区域。"),
                        code("match && ((teams == 2) || (teams >= 3 && players >= 8))"),
                        body("解析：括号先算内层；两队直接通过，多队还要人数达标。"),
                        note("实际使用时通常拆成两个区域和两条简单表达式，更容易维护。"))),
                new Section("当前限制", lines(
                        body("当前表达式没有加减乘除和取余，不能写 round % 2 == 1。"),
                        body("因此不能直接表达“奇数回合 A、偶数回合 B”的自动循环。"),
                        body("可用 round == 1、round == 2、round >= 3 等条件为每轮指定明确区域。"),
                        body("也不支持自定义变量、区域之间的相对位置或随机数。"))))),
        TESTING("测试地图", List.of(
                new Section("基础检查", lines(
                        body("确认地图状态不是未完成：地图边界和重置区域都已保存。"),
                        body("进入编辑页查看边界、重置区域、出生点数量和编辑权状态。"),
                        body("保存区域后等待快照就绪，再开始正式测试。"))),
                new Section("模式测试", lines(
                        body("团队竞技：确认复活后能出现在正确队伍区域，且不会卡进方块。"),
                        body("爆破模式：确认回合开始、换边后出生区域仍按逻辑生效。"),
                        body("歼灭竞技：确认阵亡后不会复活，并检查观战体验。"),
                        body("三队、四队房间：确认 C/D 队也有可用出生位置。"))),
                new Section("边界与恢复", lines(
                        body("走出边界，确认警告、倒计时和返回逻辑正常。"),
                        body("在边界内破坏可恢复方块，结束比赛后确认地图恢复。"),
                        body("如果重置范围较小，确认所有会被破坏的目标都在范围内。"))))),
        SHARING("分享导入", List.of(
                new Section("生成邀请码", lines(
                        body("在地图工作台选中自己的地图，点击“生成邀请码”。"),
                        body("邀请码会显示在列表中，可点击“复制邀请码”发给其他玩家。"),
                        body("“撤销邀请码”只停止后续导入，已导入的副本会保留。"))),
                new Section("导入副本", lines(
                        body("其他玩家在地图工作台输入 6 位邀请码，点击导入。"),
                        body("导入会生成一个独立副本，之后双方修改互不影响。"),
                        body("副本包含区域、出生点、显示和出现逻辑等地图配置。"))),
                new Section("删除地图", lines(
                        body("只有地图拥有者或管理员可以删除可管理的地图。"),
                        body("删除会移除地图配置和关联数据，操作不可撤销。"),
                        note("分享副本不会被同步删除。"))))),
        FAQ("常见问题", List.of(
                new Section("地图显示未完成", lines(
                        body("通常是缺少地图边界或重置区域。"),
                        body("在规划器中重新创建缺失的基础区域，并用画笔圈定范围。"),
                        body("如果两者都存在，返回编辑页刷新状态，确认草稿已保存。"))),
                new Section("画笔无法编辑", lines(
                        body("先确认已经在规划器中创建或选定目标区域。"),
                        body("确认当前地图有编辑权，且没有比赛或地图任务锁定。"),
                        body("如果删除了最后一格区域，需要重新创建或选择区域。"))),
                new Section("区域不生效", lines(
                        body("检查出现逻辑是否为条件表达式，以及表达式是否为空或写错。"),
                        body("指定模式只能填一个模式；多模式请使用 || 条件表达式。"),
                        body("A/B/C/D 队复活区域还要确认类型与队伍对应。"))),
                new Section("出生点仍用旧标记", lines(
                        body("这是兼容设计：没有可用复活区域时才回退旧标记。"),
                        body("为对应队伍创建正确类型的复活区域并保存后，就会优先使用区域。"),
                        body("如果仍回退，检查区域出现逻辑是否在当前模式下生效。"))),
                new Section("其他常见疑问", lines(
                        body("画笔不会挖掘或放置真实方块，只改变区域配置。"),
                        body("地图边界和重置区域是开赛必需项；随机出生不能替代它们。"),
                        body("区域优先级主要影响重叠时的选择与提示，不会让区域互相抵消。")))));

        private final String title;
        private final List<Section> sections;

        Chapter(String title, List<Section> sections) {
            this.title = title;
            this.sections = sections;
        }

        private String title() {
            return title;
        }

        private List<Section> sections() {
            return sections;
        }
    }
}
