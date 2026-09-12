package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import cn.blockforge.generated.generatedmod.lobby.MatchmakingManager;
import cn.blockforge.generated.generatedmod.lobby.RoomView;
import cn.blockforge.generated.generatedmod.match.GameMode;
import cn.blockforge.generated.generatedmod.match.Team;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * HUD 统计接口中枢：把比赛、正在匹配、房间中三个场景的全部可同步数据
 * （比分、击杀、伤害、时间、队列、房间信息……）注册成统一「数据源」，
 * HUD 配置窗的进度条 / 计数文字模块可以任选其中一个作为自己的内容来源。
 *
 * <p>另有 {@code dyn:} 前缀的动态数据源：管理员通过 {@code /fps hudstat set}
 * 推送的自定义数值（如 C4 安装进度）会自动出现在这里。
 *
 * <p>配置窗预览时，数据源在没有实时数据时回退到示例值（demo），
 * 保证编辑器里总能看到模块绑上数据源后的效果。
 */
public final class HudStats {
    /** 数据源分组，决定它在配置窗列表里的归类。 */
    public enum Group {
        PROGRESS("比赛进度"), NOTICE("对局公告"), SCORE("比分与回合"), SELF("我的统计"), TEAM("队伍统计"),
        STATUS("玩家状态"), TEXT("比赛文本"), MATCHING("正在匹配 HUD"), ROOM("房间中 HUD"),
        SYSTEM("系统"), DYNAMIC("自定义通道");

        private final String displayName;

        Group(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    /** 数据值的呈现方式。 */
    public enum Kind {
        NUMBER, DECIMAL, TIME, TEXT, PROGRESS
    }

    /** 一个可调用的统计接口。 */
    public record Source(String id, String name, Group group, Kind kind,
                         DoubleSupplier value, IntSupplier maximum, Supplier<String> text,
                         BooleanSupplier live, double demo, int demoMaximum, String demoText,
                         Set<HudContext> contexts) {

        /** Compatibility constructor for callers that do not need scene restrictions. */
        public Source(String id, String name, Group group, Kind kind,
                      DoubleSupplier value, IntSupplier maximum, Supplier<String> text,
                      BooleanSupplier live, double demo, int demoMaximum, String demoText) {
            this(id, name, group, kind, value, maximum, text, live, demo, demoMaximum, demoText,
                    allContexts());
        }

        public Source {
            contexts = contexts == null || contexts.isEmpty()
                    ? allContexts() : Set.copyOf(contexts);
        }

        /** Whether this source is meaningful in the given scene. */
        public boolean availableIn(HudContext context) {
            return context == null || context == HudContext.GLOBAL || contexts.contains(context);
        }

        /** Whether the source should be drawn or used in editor/live rendering. */
        public boolean usable(HudContext context, boolean editor) {
            return availableIn(context) && (editor || isLive());
        }

        /** 配置窗里是否拿得到真实数据（拿不到时编辑器回退示例值）。 */
        public boolean isLive() {
            try {
                return live.getAsBoolean();
            } catch (RuntimeException error) {
                return false;
            }
        }

        /** 原始数值（时间类返回 tick 数；文本类返回 0）。 */
        public double number(boolean editor, HudContext context) {
            if (!usable(context, editor)) {
                return editor ? demo : 0;
            }
            try {
                return kind == Kind.TEXT ? 0 : value.getAsDouble();
            } catch (Exception error) {
                return editor ? demo : 0;
            }
        }

        /** 进度上限；无上限（普通数值）返回 0。 */
        public int maximum(boolean editor, HudContext context) {
            if (kind != Kind.PROGRESS && kind != Kind.TIME) {
                return 0;
            }
            if (!usable(context, editor)) {
                return editor ? Math.max(kind == Kind.TIME ? 0 : 1, demoMaximum) : 0;
            }
            try {
                return Math.max(kind == Kind.TIME ? 0 : 1, maximum.getAsInt());
            } catch (Exception error) {
                return Math.max(1, demoMaximum);
            }
        }

        /** 0..1 的进度比例；普通数值按“百分数”解释。 */
        public double ratio(boolean editor, HudContext context) {
            if (kind == Kind.TEXT) {
                return 0;
            }
            if (kind == Kind.PROGRESS || kind == Kind.TIME) {
                int maximum = maximum(editor, context);
                return maximum <= 0 ? 0 : clamp01(number(editor, context) / maximum);
            }
            return clamp01(number(editor, context) / 100.0);
        }

        /** 已格式化好的展示文本（数字取整、时间 mm:ss、进度显示 x/y）。 */
        public String display(boolean editor, HudContext context) {
            if (!editor && !isLive()) {
                return "";
            }
            switch (kind) {
                case TEXT: {
                    if (!availableIn(context)) {
                        return editor ? demoText : "";
                    }
                    if (editor && !isLive()) {
                        return demoText;
                    }
                    String resolved;
                    try {
                        resolved = text.get();
                    } catch (Exception error) {
                        resolved = "";
                    }
                    return resolved == null || resolved.isEmpty()
                            ? (editor ? demoText : "") : resolved;
                }
                case TIME:
                    return UiTheme.formatTicks((int) number(editor, context));
                case DECIMAL:
                    return String.format(Locale.ROOT, "%.1f", number(editor, context));
                case PROGRESS:
                    return Long.toString(Math.round(number(editor, context))) + "/" + maximum(editor, context);
                default:
                    return Long.toString(Math.round(number(editor, context)));
            }
        }

        /** Compatibility overload for older integrations that have no scene context. */
        public double number(boolean editor) {
            return number(editor, HudContext.GLOBAL);
        }

        public int maximum(boolean editor) {
            return maximum(editor, HudContext.GLOBAL);
        }

        public double ratio(boolean editor) {
            return ratio(editor, HudContext.GLOBAL);
        }

        public String display(boolean editor) {
            return display(editor, HudContext.GLOBAL);
        }

        /** 编辑器下拉里展示的名字。 */
        public String pickerLabel() {
            return group.displayName() + " · " + name;
        }

        private static double clamp01(double value) {
            return Math.max(0.0, Math.min(1.0, value));
        }
    }

    private static final List<Source> SOURCES = new ArrayList<>();
    private static final Map<String, Source> BY_ID = new LinkedHashMap<>();
    private static final Map<String, Source> EXTERNAL_SOURCES = new LinkedHashMap<>();

    private HudStats() {
    }

    // ------------------------------------------------------------- 注册表

    /** 全部静态数据源（不含 dyn: 动态源）。 */
    public static List<Source> sources() {
        ensureBuilt();
        return List.copyOf(SOURCES);
    }

    /** 含自定义通道条目在内的全部可选数据源（配置窗列表用）。 */
    public static List<Source> sourcesWithDynamic() {
        ensureBuilt();
        List<Source> all = new ArrayList<>(SOURCES);
        all.addAll(EXTERNAL_SOURCES.values());
        all.addAll(dynamicSources());
        all.addAll(externalEventSources());
        return all;
    }

    public static List<Source> sourcesFor(HudContext context) {
        List<Source> all = sourcesWithDynamic();
        if (context == null || context == HudContext.GLOBAL) return all;
        return all.stream().filter(source -> source.availableIn(context)).toList();
    }

    public static String primaryCategory(Source source) {
        if (source == null) return "其他";
        String name = source.name();
        String id = source.id();
        if (source.group() == Group.MATCHING) return "匹配";
        if (source.group() == Group.ROOM) return "房间";
        if (source.group() == Group.DYNAMIC) return "自定义";
        if (source.group() == Group.NOTICE) return "公告";
        if (source.group() == Group.STATUS) return "玩家状态";
        if (source.group() == Group.SYSTEM) return "系统";
        if (source.group() == Group.TEXT) return "比赛信息";
        if (id.contains("bomb") || name.contains("C4")) return "爆破";
        if (id.contains("money") || name.contains("资金") || name.contains("账户")) return "金钱";
        if (name.contains("得分") || name.contains("比分")) return "得分";
        if (name.contains("击杀")) return "击杀数";
        if (name.contains("伤害")) return "伤害";
        if (name.contains("KD") || name.contains("击杀死亡")) return "击杀/死亡比";
        if (name.contains("胜场") || name.contains("胜利")) return "胜场";
        if (name.contains("时间") || name.contains("倒计时") || name.contains("计时")) return "时间";
        if (name.contains("进度") || name.contains("百分比")) return "进度";
        if (name.contains("生命") || name.contains("护甲") || name.contains("氧气")
                || name.contains("饥饿") || name.contains("状态")) return "玩家状态";
        if (name.contains("坐标") || name.contains("维度") || name.contains("高度")
                || name.contains("朝向")) return "位置";
        if (name.contains("模式") || name.contains("阶段") || name.contains("回合")) return "比赛信息";
        if (source.group() == Group.TEAM) return "队伍";
        if (source.group() == Group.SELF) return "个人";
        if (source.group() == Group.SCORE) return "比赛信息";
        if (source.group() == Group.PROGRESS) return "进度";
        return "其他";
    }

    public static String secondaryCategory(Source source) {
        if (source == null) return "全部";
        String id = source.id();
        String name = source.name();
        if (id.equals("my_match_money")) return "局内";
        if (id.equals("my_global_money")) return "局外";
        if (name.contains("本轮") || name.contains("当前回合")) return "本轮";
        if (name.contains("整场") || name.contains("本局")) return "整场";
        if (name.contains("倒计时")) return "倒计时";
        if (name.contains("百分比") || name.contains("进度")) return "进度";
        if (name.contains("状态")) return "状态";
        return source.group().displayName();
    }

    public static String tertiaryCategory(Source source) {
        if (source == null) return "全部";
        String name = source.name();
        if (name.contains("A队")) return "A队";
        if (name.contains("B队")) return "B队";
        if (name.contains("C队")) return "C队";
        if (name.contains("D队")) return "D队";
        if (name.contains("我方")) return "我方";
        if (name.contains("敌方")) return "敌方";
        if (name.contains("双方")) return "双方";
        if (name.contains("我的") || name.contains("自己")) return "自己";
        if (source.group() == Group.NOTICE) return "公告";
        if (source.group() == Group.MATCHING) return "队列";
        if (source.group() == Group.ROOM) return "房间";
        if (source.group() == Group.SYSTEM) return "全局";
        return name;
    }

    /** 第三方玩法注册的实时数据源；重复 id 会替换旧定义。 */
    public static synchronized void register(Source source) {
        if (source == null || source.id() == null || source.id().isBlank()
                || source.id().startsWith("dyn:") || source.value() == null || source.maximum() == null
                || source.text() == null || source.live() == null
                || source.name() == null || source.name().isBlank()
                || source.group() == null || source.kind() == null) {
            throw new IllegalArgumentException("HUD 数据源参数无效");
        }
        ensureBuilt();
        if (BY_ID.containsKey(source.id())) {
            throw new IllegalArgumentException("HUD 数据源 ID 与内置接口冲突：" + source.id());
        }
        if (externalEventSources().stream().anyMatch(existing -> existing.id().equals(source.id()))) {
            throw new IllegalArgumentException("HUD 数据源 ID 与事件接口冲突：" + source.id());
        }
        if (!EXTERNAL_SOURCES.containsKey(source.id()) && EXTERNAL_SOURCES.size() >= 512) {
            throw new IllegalArgumentException("HUD 数据源数量已达上限");
        }
        EXTERNAL_SOURCES.put(source.id(), source);
    }

    /** 删除第三方玩法注册的数据源；内置和服务器动态源不受影响。 */
    public static synchronized boolean unregister(String id) {
        return id != null && EXTERNAL_SOURCES.remove(id) != null;
    }

    /** 批量清理第三方数据源，适合玩法卸载或重载时调用。 */
    public static synchronized void clearExternalSources() {
        EXTERNAL_SOURCES.clear();
    }

    /** 按 id 取数据源；找不到返回 null。 */
    public static Source byId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        ensureBuilt();
        if (id.startsWith("dyn:")) {
            for (Source source : dynamicSources()) {
                if (source.id().equals(id)) {
                    return source;
                }
            }
            return null;
        }
        Source external = EXTERNAL_SOURCES.get(id);
        if (external != null) return external;
        for (Source source : externalEventSources()) {
            if (source.id().equals(id)) return source;
        }
        return BY_ID.get(id);
    }

    /** 自定义通道（/fps hudstat）推来的动态源。 */
    public static List<Source> dynamicSources() {
        List<Source> list = new ArrayList<>();
        for (ClientHudDynamicStats.Entry entry : ClientHudDynamicStats.entries()) {
            int demoMax = entry.maximum() > 0 ? entry.maximum() : 100;
            BooleanSupplier live = () -> ClientHudDynamicStats.byId(entry.id()) != null;
            DoubleSupplier liveValue = () -> {
                ClientHudDynamicStats.Entry current = ClientHudDynamicStats.byId(entry.id());
                return current == null ? 0 : current.value();
            };
            IntSupplier liveMax = () -> {
                ClientHudDynamicStats.Entry current = ClientHudDynamicStats.byId(entry.id());
                return current == null ? 0 : current.maximum();
            };
            list.add(new Source("dyn:" + entry.id(), entry.label(), Group.DYNAMIC,
                    entry.maximum() > 0 ? Kind.PROGRESS : Kind.NUMBER, liveValue, liveMax,
                    () -> Long.toString(Math.round(liveValue.getAsDouble())), live,
                    Math.min(entry.value(), demoMax), demoMax,
                    entry.label() + " 60"));
        }
        return list;
    }

    private static List<Source> externalEventSources() {
        List<Source> list = new ArrayList<>();
        for (ClientHudEventData.Descriptor descriptor : ClientHudEventData.descriptors()) {
            String prefix = "event:" + descriptor.id();
            list.add(new Source(prefix + ":title", descriptor.title() + " · 标题",
                    Group.NOTICE, Kind.TEXT, () -> 0, () -> 0,
                    () -> eventText(descriptor.id(), true),
                    () -> ClientHudEventData.active(descriptor.id()), 0, 0,
                    descriptor.title(), descriptor.contexts()));
            list.add(new Source(prefix + ":detail", descriptor.title() + " · 说明",
                    Group.NOTICE, Kind.TEXT, () -> 0, () -> 0,
                    () -> eventText(descriptor.id(), false),
                    () -> ClientHudEventData.active(descriptor.id()), 0, 0,
                    descriptor.defaultDetail(), descriptor.contexts()));
            list.add(new Source(prefix + ":timer", descriptor.title() + " · 剩余时间",
                    Group.NOTICE, Kind.TEXT, () -> 0, () -> 0,
                    () -> eventTimer(descriptor.id()),
                    () -> ClientHudEventData.active(descriptor.id()), 0, 0, "2.0 秒",
                    descriptor.contexts()));
            list.add(new Source(prefix + ":progress", descriptor.title() + " · 进度",
                    Group.NOTICE, Kind.PROGRESS, () -> eventRemaining(descriptor.id()),
                    () -> eventDuration(descriptor.id()),
                    () -> "",
                    () -> ClientHudEventData.active(descriptor.id()), 60, 100,
                    "", descriptor.contexts()));
        }
        return list;
    }

    // ------------------------------------------------------------- 场景可用性

    private static boolean inMatch() {
        return ClientMatchData.inMatch();
    }

    private static boolean forming() {
        return ClientLobbyData.dynamicReadySeconds() > 0;
    }

    private static boolean inQueue() {
        return ClientLobbyData.matchmaking().queued() || forming();
    }

    private static RoomView roomOrNull() {
        return SceneHudOverlay.ownRoom();
    }

    private static boolean inRoom() {
        return roomOrNull() != null;
    }

    private static Team mySide() {
        return ClientMatchData.myTeam.isPlayable() ? ClientMatchData.myTeam : Team.TEAM_A;
    }

    private static int myTeamScore() {
        return ClientMatchData.stats(mySide()).score();
    }

    private static int enemyTeamScore() {
        return enemyMaximum(cn.blockforge.generated.generatedmod.match.TeamMatchStats::score);
    }

    private static int myTeamWins() {
        return ClientMatchData.stats(mySide()).wins();
    }

    private static int enemyTeamWins() {
        return enemyMaximum(cn.blockforge.generated.generatedmod.match.TeamMatchStats::wins);
    }

    private static int myTeamMatchKills() {
        return ClientMatchData.stats(mySide()).kills();
    }

    private static int enemyTeamMatchKills() {
        return enemyMaximum(cn.blockforge.generated.generatedmod.match.TeamMatchStats::kills);
    }

    private static int myTeamDamage() {
        return ClientMatchData.stats(mySide()).damage();
    }

    private static int enemyTeamDamage() {
        return enemyMaximum(cn.blockforge.generated.generatedmod.match.TeamMatchStats::damage);
    }

    private static java.util.stream.Stream<cn.blockforge.generated.generatedmod.match.TeamMatchStats> allStats() {
        return Team.playing(Math.max(2, ClientMatchData.teamStats.size())).stream().map(ClientMatchData::stats);
    }
    private static int enemyMaximum(java.util.function.ToIntFunction<cn.blockforge.generated.generatedmod.match.TeamMatchStats> value) {
        return allStats().filter(stats -> stats.team() != mySide()).mapToInt(value).max().orElse(0);
    }

    /** 领先方距离胜利的度量：死斗看本轮击杀/目标，其余看胜场/胜利回合数。 */
    private static boolean killTargetMode() {
        return ClientMatchData.mode == GameMode.TEAM_DEATHMATCH && ClientMatchData.targetKills > 0;
    }

    private static int leadScore() {
        return allStats().mapToInt(stats -> killTargetMode() ? stats.score() : stats.wins()).max().orElse(0);
    }

    private static int leadTarget() {
        return killTargetMode() ? ClientMatchData.targetKills : Math.max(1, ClientMatchData.roundsToWin);
    }

    private static ClientHudEventData.ActiveEvent primaryEvent() {
        return ClientHudEventData.primary();
    }

    private static String eventText(cn.blockforge.generated.generatedmod.match.MatchHudEventType type,
                                    boolean title) {
        var event = ClientHudEventData.get(type.id());
        if (event == null) return "";
        return title ? event.title() : event.detail();
    }

    private static String eventText(String id, boolean title) {
        var event = ClientHudEventData.get(id);
        if (event == null) return "";
        return title ? event.title() : event.detail();
    }

    private static String eventTimer(cn.blockforge.generated.generatedmod.match.MatchHudEventType type) {
        var event = ClientHudEventData.get(type.id());
        return event == null ? "" : String.format(Locale.ROOT, "%.1f 秒",
                event.remainingTicks() / 20.0D);
    }

    private static String eventTimer(String id) {
        var event = ClientHudEventData.get(id);
        return event == null ? "" : String.format(Locale.ROOT, "%.1f 秒",
                event.remainingTicks() / 20.0D);
    }

    private static int eventRemaining(cn.blockforge.generated.generatedmod.match.MatchHudEventType type) {
        var event = ClientHudEventData.get(type.id());
        return event == null ? 0 : event.remainingTicks();
    }

    private static int eventRemaining(String id) {
        var event = ClientHudEventData.get(id);
        return event == null ? 0 : event.remainingTicks();
    }

    private static int eventDuration(cn.blockforge.generated.generatedmod.match.MatchHudEventType type) {
        var event = ClientHudEventData.get(type.id());
        return event == null ? 1 : Math.max(1, event.totalTicks());
    }

    private static int eventDuration(String id) {
        var event = ClientHudEventData.get(id);
        return event == null ? 1 : Math.max(1, event.totalTicks());
    }

    // ------------------------------------------------------------- 构建

    private static void ensureBuilt() {
        if (!BY_ID.isEmpty()) {
            return;
        }
        // ---- 比分与回合
        number("score_a", "A队本轮得分", Group.SCORE, () -> ClientMatchData.teamAScore, HudStats::inMatch, 12);
        number("score_b", "B队本轮得分", Group.SCORE, () -> ClientMatchData.teamBScore, HudStats::inMatch, 9);
        for (Team team : List.of(Team.TEAM_C, Team.TEAM_D)) {
            String key = team.key().substring(5);
            number("score_" + key, team.displayName() + "本轮得分", Group.SCORE, () -> ClientMatchData.stats(team).score(), HudStats::inMatch, 8);
            number("wins_" + key, team.displayName() + "胜场", Group.SCORE, () -> ClientMatchData.stats(team).wins(), HudStats::inMatch, 1);
            number("team_" + key + "_size", team.displayName() + "人数", Group.SCORE, () -> ClientMatchData.stats(team).size(), HudStats::inMatch, 4);
            number("match_kills_" + key, team.displayName() + "整场击杀", Group.TEAM, () -> ClientMatchData.stats(team).kills(), HudStats::inMatch, 12);
            number("team_" + key + "_damage", team.displayName() + "造成伤害", Group.TEAM, () -> ClientMatchData.stats(team).damage(), HudStats::inMatch, 600);
            progress("team_" + key + "_progress", team.displayName() + "击杀进度", Group.PROGRESS,
                    () -> ClientMatchData.stats(team).score(),
                    () -> Math.max(1, ClientMatchData.targetKills), HudStats::inMatch, 8, 25,
                    Set.of(HudContext.TEAM_DEATHMATCH));
        }
        number("score_sum", "双方本轮总分", Group.SCORE,
                () -> allStats().mapToInt(value -> value.score()).sum(), HudStats::inMatch, 21);
        number("score_max", "领先方本轮分", Group.SCORE,
                () -> allStats().mapToInt(value -> value.score()).max().orElse(0), HudStats::inMatch, 12);
        number("my_team_score", "我方本轮得分", Group.SCORE, HudStats::myTeamScore, HudStats::inMatch, 12);
        number("enemy_team_score", "敌方本轮得分", Group.SCORE, HudStats::enemyTeamScore, HudStats::inMatch, 9);
        number("wins_a", "A队胜场", Group.SCORE, () -> ClientMatchData.teamAWins, HudStats::inMatch, 1);
        number("wins_b", "B队胜场", Group.SCORE, () -> ClientMatchData.teamBWins, HudStats::inMatch, 0);
        number("my_wins", "我方胜场", Group.SCORE, HudStats::myTeamWins, HudStats::inMatch, 1);
        number("enemy_wins", "敌方胜场", Group.SCORE, HudStats::enemyTeamWins, HudStats::inMatch, 0);
        number("round", "当前回合数", Group.SCORE, () -> ClientMatchData.roundNumber, HudStats::inMatch, 1);
        number("target_kills", "目标击杀数", Group.SCORE, () -> ClientMatchData.targetKills, HudStats::inMatch, 25);
        number("team_a_size", "A队人数", Group.SCORE, () -> ClientMatchData.teamASize, HudStats::inMatch, 4);
        number("team_b_size", "B队人数", Group.SCORE, () -> ClientMatchData.teamBSize, HudStats::inMatch, 4);
        number("spectator_size", "观战人数", Group.SCORE, () -> ClientMatchData.spectatorSize, HudStats::inMatch, 1);

        // ---- 对局公告：把原本只在聊天栏出现的阶段信息提供给 HUD。
        text("notice_title", "阶段公告标题", Group.NOTICE, MatchHudNotice::title,
                HudStats::inMatch, "热身阶段");
        text("notice_detail", "阶段公告详情", Group.NOTICE, MatchHudNotice::detail,
                HudStats::inMatch, "比赛将在 30 秒后开始 · A队 4人 · B队 4人");
        text("notice_timer", "阶段公告倒计时", Group.NOTICE, MatchHudNotice::timer,
                HudStats::inMatch, "00:10");
        text("event_id", "HUD 事件 ID", Group.NOTICE,
                () -> primaryEvent() == null ? "" : primaryEvent().id(),
                HudStats::inMatch, "bomb_planting");
        text("event_title", "HUD 事件标题", Group.NOTICE,
                () -> primaryEvent() == null ? "" : primaryEvent().title(),
                HudStats::inMatch, "正在安装 C4");
        text("event_detail", "HUD 事件说明", Group.NOTICE,
                () -> primaryEvent() == null ? "" : primaryEvent().detail(),
                HudStats::inMatch, "保持安装动作直到进度完成");
        text("event_timer", "HUD 事件剩余时间", Group.NOTICE,
                () -> {
                    var event = primaryEvent();
                    return event == null ? "" : String.format(Locale.ROOT, "%.1f 秒",
                            event.remainingTicks() / 20.0D);
                }, HudStats::inMatch, "2.0 秒");
        progress("event_progress", "HUD 事件进度", Group.NOTICE,
                () -> primaryEvent() == null ? 0 : primaryEvent().remainingTicks(),
                () -> primaryEvent() == null ? 1 : Math.max(1, primaryEvent().totalTicks()),
                HudStats::inMatch, 60, 100);
        for (var type : cn.blockforge.generated.generatedmod.match.MatchHudEventType.values()) {
            String prefix = "event:" + type.id();
            text(prefix + ":title", type.displayName() + " · 标题", Group.NOTICE,
                    () -> eventText(type, true), () -> ClientHudEventData.active(type.id()),
                    type.displayName());
            text(prefix + ":detail", type.displayName() + " · 说明", Group.NOTICE,
                    () -> eventText(type, false), () -> ClientHudEventData.active(type.id()),
                    type.defaultDetail());
            text(prefix + ":timer", type.displayName() + " · 剩余时间", Group.NOTICE,
                    () -> eventTimer(type), () -> ClientHudEventData.active(type.id()), "2.0 秒");
            progress(prefix + ":progress", type.displayName() + " · 进度", Group.NOTICE,
                    () -> eventRemaining(type), () -> eventDuration(type),
                    () -> ClientHudEventData.active(type.id()), 60, 100, eventContexts(type));
        }

        // ---- 我的统计（整场累计，服务器同步）
        number("my_kills", "我的击杀数", Group.SELF, () -> ClientMatchData.myMatchKills, HudStats::inMatch, 7);
        number("my_deaths", "我的阵亡数", Group.SELF, () -> ClientMatchData.myMatchDeaths, HudStats::inMatch, 4);
        number("my_damage", "我对敌人伤害", Group.SELF, () -> ClientMatchData.myDamageDealt, HudStats::inMatch, 236);
        number("my_damage_taken", "我承受伤害", Group.SELF, () -> ClientMatchData.myDamageTaken, HudStats::inMatch, 154);
        decimal("my_kd", "我的KD", Group.SELF,
                () -> ClientMatchData.myMatchDeaths == 0
                        ? ClientMatchData.myMatchKills
                        : ClientMatchData.myMatchKills / (double) ClientMatchData.myMatchDeaths,
                HudStats::inMatch, 1.8);
        time("respawn_left", "我的恢复倒计时", Group.SELF,
                () -> ClientMatchData.respawnRemainingTicks,
                () -> Math.max(1, ClientMatchData.respawnTotalTicks),
                HudStats::inMatch, 5 * 20, Set.of(HudContext.TEAM_DEATHMATCH));
        text("my_match_money", "局内资金（比赛钱包）", Group.SELF,
                () -> "$" + ClientMatchData.matchBalance, HudStats::inMatch, "$800");
        text("my_global_money", "局外资金（大厅账户）", Group.SELF,
                () -> "$" + ClientMatchData.globalBalance, () -> true, "$1000", allContexts());
        text("held_weapon", "手持武器名称", Group.STATUS, HeldWeaponHudData::name,
                HeldWeaponHudData::hasWeapon, "AK-47");
        text("held_ammo", "手持武器弹药", Group.STATUS, HeldWeaponHudData::ammoText,
                HeldWeaponHudData::hasGun, "30 / 120");
        progress("held_durability", "手持武器耐久", Group.STATUS,
                HeldWeaponHudData::durabilityPercent, () -> 100,
                HeldWeaponHudData::hasGun, 85, 100);
        text("bomb_phase", "爆破阶段", Group.TEXT, () -> switch (ClientBombData.phase) {
            case CARRIED -> "C4 已携带";
            case DROPPED -> "C4 已掉落";
            case PLANTING -> "正在安装 C4";
            case PLANTED -> "C4 已安装";
            case DEFUSING -> "正在拆除 C4";
            case EXPLODED -> "C4 已引爆";
            case DEFUSED -> "C4 已拆除";
            default -> "";
        }, () -> ClientBombData.active, "C4 已安装", Set.of(HudContext.SEARCH_DESTROY));
        text("bomb_site", "爆破地点", Group.TEXT,
                () -> ClientBombData.bombSiteName, () -> ClientBombData.active, "A 点",
                Set.of(HudContext.SEARCH_DESTROY));
        time("bomb_countdown", "C4 倒计时", Group.PROGRESS,
                () -> ClientBombData.detonationRemainingTicks,
                () -> Math.max(1, ClientBombData.detonationTotalTicks),
                () -> ClientBombData.active, 40 * 20,
                Set.of(HudContext.SEARCH_DESTROY));

        // ---- 队伍统计
        number("match_kills_a", "A队整场击杀", Group.TEAM, () -> ClientMatchData.teamAMatchKills, HudStats::inMatch, 15);
        number("match_kills_b", "B队整场击杀", Group.TEAM, () -> ClientMatchData.teamBMatchKills, HudStats::inMatch, 11);
        number("match_kills_sum", "双方整场击杀", Group.TEAM,
                () -> allStats().mapToInt(value -> value.kills()).sum(), HudStats::inMatch, 26);
        number("my_team_match_kills", "我方整场击杀", Group.TEAM, HudStats::myTeamMatchKills, HudStats::inMatch, 15);
        number("enemy_team_match_kills", "敌方整场击杀", Group.TEAM, HudStats::enemyTeamMatchKills, HudStats::inMatch, 11);
        number("team_a_damage", "A队造成伤害", Group.TEAM, () -> ClientMatchData.teamADamageDealt, HudStats::inMatch, 980);
        number("team_b_damage", "B队造成伤害", Group.TEAM, () -> ClientMatchData.teamBDamageDealt, HudStats::inMatch, 760);
        number("my_team_damage", "我方造成伤害", Group.TEAM, HudStats::myTeamDamage, HudStats::inMatch, 980);
        number("enemy_team_damage", "敌方造成伤害", Group.TEAM, HudStats::enemyTeamDamage, HudStats::inMatch, 760);
        number("total_damage", "双方伤害合计", Group.TEAM,
                () -> allStats().mapToInt(value -> value.damage()).sum(), HudStats::inMatch, 1740);
        decimal("my_team_share", "我方伤害占比%", Group.TEAM, () -> {
            int total = allStats().mapToInt(value -> value.damage()).sum();
            return total <= 0 ? 0 : myTeamDamage() * 100.0 / total;
        }, HudStats::inMatch, 56.0);

        // ---- 时间类
        time("phase_remaining", "回合倒计时", Group.PROGRESS,
                () -> ClientMatchData.phaseRemainingTicks,
                () -> Math.max(1, ClientMatchData.phaseTotalTicks),
                HudStats::inMatch, 225 * 20, matchContexts());
        time("buy_phase_remaining", "购买阶段倒计时", Group.PROGRESS,
                () -> ClientMatchData.phaseRemainingTicks,
                () -> Math.max(1, ClientMatchData.phaseTotalTicks),
                () -> ClientMatchData.state == cn.blockforge.generated.generatedmod.match.MatchState.BUYING,
                15 * 20, Set.of(HudContext.SEARCH_DESTROY));
        time("boundary_remaining", "出界倒计时", Group.PROGRESS,
                () -> ClientMatchData.boundaryTicks, HudStats::inMatch, 10 * 20);
        time("elapsed", "本局已进行时间", Group.PROGRESS, ClientMatchData::elapsedTicks,
                () -> Math.max(1, ClientMatchData.matchTotalTicks), HudStats::inMatch,
                165 * 20, Set.of(HudContext.TEAM_DEATHMATCH, HudContext.LAST_STANDING));

        // ---- 进度类
        progress("win_progress", "胜利进度（领先方）", Group.PROGRESS,
                HudStats::leadScore, HudStats::leadTarget, HudStats::inMatch, 6, 10);
        progress("team_a_progress", "A队击杀进度", Group.PROGRESS,
                () -> ClientMatchData.teamAScore, () -> Math.max(1, ClientMatchData.targetKills),
                HudStats::inMatch, 12, 25, Set.of(HudContext.TEAM_DEATHMATCH));
        progress("team_b_progress", "B队击杀进度", Group.PROGRESS,
                () -> ClientMatchData.teamBScore, () -> Math.max(1, ClientMatchData.targetKills),
                HudStats::inMatch, 9, 25, Set.of(HudContext.TEAM_DEATHMATCH));
        progress("total_kill_progress", "房间总击杀进度", Group.PROGRESS,
                () -> allStats().mapToInt(value -> value.score()).sum(),
                () -> Math.max(1, ClientMatchData.targetKills), HudStats::inMatch, 21, 25,
                Set.of(HudContext.TEAM_DEATHMATCH));
        progress("my_wins_progress", "我方胜场进度", Group.PROGRESS,
                HudStats::myTeamWins, () -> Math.max(1, ClientMatchData.roundsToWin), HudStats::inMatch,
                1, 3, Set.of(HudContext.SEARCH_DESTROY, HudContext.LAST_STANDING));
        for (Team team : Team.playing(4)) {
            String key = team.key().substring(5);
            progress("team_" + key + "_wins_progress", team.displayName() + "胜场进度", Group.PROGRESS,
                    () -> ClientMatchData.stats(team).wins(),
                    () -> Math.max(1, ClientMatchData.roundsToWin), HudStats::inMatch, 1, 3,
                    Set.of(HudContext.SEARCH_DESTROY, HudContext.LAST_STANDING));
        }
        progress("health_percent", "我的血量百分比", Group.STATUS,
                () -> {
                    var player = Minecraft.getInstance().player;
                    return player == null ? 0 : player.getHealth() / player.getMaxHealth() * 100.0;
                }, () -> 100, () -> Minecraft.getInstance().player != null, 76, 100);
        number("my_armor_percent", "我的护甲百分比", Group.STATUS, () -> {
            var player = Minecraft.getInstance().player;
            return player == null ? 0 : player.getArmorValue() * 5.0D;
        }, () -> Minecraft.getInstance().player != null, 60);
        progress("my_armor_percent_progress", "我的护甲进度", Group.STATUS, () -> {
            var player = Minecraft.getInstance().player;
            return player == null ? 0 : player.getArmorValue();
        }, () -> 20, () -> Minecraft.getInstance().player != null, 12, 20);
        number("my_hunger", "我的饥饿值", Group.STATUS, () -> {
            var player = Minecraft.getInstance().player;
            return player == null ? 0 : player.getFoodData().getFoodLevel();
        }, () -> Minecraft.getInstance().player != null, 18);
        number("my_air", "我的氧气 tick", Group.STATUS, () -> {
            var player = Minecraft.getInstance().player;
            return player == null ? 0 : player.getAirSupply();
        }, () -> Minecraft.getInstance().player != null, 240);
        number("my_xp_level", "我的经验等级", Group.STATUS, () -> {
            var player = Minecraft.getInstance().player;
            return player == null ? 0 : player.experienceLevel;
        }, () -> Minecraft.getInstance().player != null, 12);
        number("my_effect_count", "我的药水效果数", Group.STATUS,
                () -> Minecraft.getInstance().player == null ? 0
                        : Minecraft.getInstance().player.getActiveEffects().size(),
                () -> Minecraft.getInstance().player != null, 2);
        text("my_position", "我的坐标", Group.SYSTEM, () -> {
            var player = Minecraft.getInstance().player;
            return player == null ? "" : String.format(Locale.ROOT, "%.1f %.1f %.1f",
                    player.getX(), player.getY(), player.getZ());
        }, () -> Minecraft.getInstance().player != null, "12.5 64.0 -8.0");
        text("my_dimension", "我的维度", Group.SYSTEM, () -> {
            var player = Minecraft.getInstance().player;
            return player == null ? "" : player.level().dimension().location().toString();
        }, () -> Minecraft.getInstance().player != null, "minecraft:overworld");
        number("world_day_time", "世界时间 tick", Group.SYSTEM,
                () -> Minecraft.getInstance().level == null ? 0
                        : Minecraft.getInstance().level.getDayTime() % 24000L,
                () -> Minecraft.getInstance().level != null, 12000);
        text("world_weather", "世界天气", Group.SYSTEM, () -> {
            var level = Minecraft.getInstance().level;
            if (level == null) return "";
            return level.isThundering() ? "雷雨" : level.isRaining() ? "下雨" : "晴朗";
        }, () -> Minecraft.getInstance().level != null, "晴朗");

        // ---- 比赛文本
        text("mode_text", "模式名", Group.TEXT, ClientMatchData::modeText, HudStats::inMatch, "团队竞技");
        text("phase_text", "比赛阶段", Group.TEXT, ClientMatchData::phaseText, HudStats::inMatch, "进行中");
        text("my_team_text", "我的队伍", Group.TEXT,
                () -> ClientMatchData.myTeam.displayName(), HudStats::inMatch, "A队");
        text("round_text", "回合数（兼容别名）", Group.TEXT, () -> Integer.toString(ClientMatchData.roundNumber), HudStats::inMatch, "1");
        text("target_text", "目标数（兼容别名）", Group.TEXT, ClientMatchData::targetText,
                HudStats::inMatch, "25");
        text("team_sizes_text", "兼容整句：队伍人数汇总", Group.TEXT, ClientMatchData::teamSizesText, HudStats::inMatch,
                "A队 4人  ·  B队 4人");
        text("kill_feed_text", "最近击杀公告", Group.TEXT, () -> {
            if (!ClientMatchData.killFeedActive()) {
                return "";
            }
            return "⚔ " + ClientMatchData.killFeedText();
        }, HudStats::inMatch, "⚔ Steve  击杀  Alex");
        text("winner_text", "胜者", Group.TEXT, () -> ClientMatchData.winner == null
                ? "" : ClientMatchData.winner.displayName(), HudStats::inMatch, "A队");
        text("hint_text", "底部状态提示", Group.TEXT, MatchHudOverlay::hintText, HudStats::inMatch,
                "队伍：A队  ·  A队 4人  ·  B队 4人");

        // ---- 正在匹配 HUD 全量内容
        text("matching_state", "匹配阶段", Group.MATCHING,
                () -> forming() ? "已匹配" : "正在匹配", HudStats::inQueue, "正在匹配");
        number("queue_size", "队列人数", Group.MATCHING, () -> ClientLobbyData.matchmaking().queueSize(),
                HudStats::inQueue, 3);
        number("queue_need", "成局需要人数", Group.MATCHING,
                () -> MatchmakingManager.MIN_PLAYERS_TO_FORM, HudStats::inQueue, 6);
        number("queue_position", "我的序位", Group.MATCHING,
                () -> Math.max(1, ClientLobbyData.matchmaking().position()), HudStats::inQueue, 2);
        time("queue_wait", "已等待时间", Group.MATCHING, ClientLobbyData::dynamicWaitedTicks,
                () -> 0, HudStats::inQueue, 18 * 20, Set.of(HudContext.MATCHING));
        number("ready_seconds", "开赛倒计时（秒）", Group.MATCHING, ClientLobbyData::dynamicReadySeconds,
                HudStats::forming, 3);
        progress("queue_progress", "成局人数进度", Group.MATCHING,
                () -> ClientLobbyData.matchmaking().queueSize(), () -> MatchmakingManager.MIN_PLAYERS_TO_FORM,
                HudStats::inQueue, 3, 6);
        progress("ready_progress", "开赛准备进度", Group.MATCHING, ClientLobbyData::dynamicReadySeconds,
                () -> MatchmakingManager.READY_SECONDS, HudStats::forming, 15,
                MatchmakingManager.READY_SECONDS);
        text("matching_line1", "兼容整句：匹配横幅第一行", Group.MATCHING, () -> {
            RoomView room = roomOrNull();
            return SceneHudOverlay.matchingLineOne(ClientLobbyData.matchmaking(), forming(), room);
        }, HudStats::inQueue, "正在匹配  3/6 人成局");
        text("matching_line2", "兼容整句：匹配横幅第二行", Group.MATCHING, () -> {
            RoomView room = roomOrNull();
            return SceneHudOverlay.matchingLineTwo(ClientLobbyData.matchmaking(), forming(), room);
        }, HudStats::inQueue, "已等待 00:18  ·  序位 2");
        text("queue_text", "队列人数文本", Group.MATCHING,
                () -> ClientLobbyData.matchmaking().queueSize() + "/"
                        + MatchmakingManager.MIN_PLAYERS_TO_FORM,
                HudStats::inQueue, "3/6");
        text("matching_message_text", "匹配提示消息", Group.MATCHING, ClientLobbyData::matchmakingMessage,
                HudStats::inQueue, "已进入匹配队列");

        // ---- 房间中 HUD 全量内容
        text("room_name", "房间名", Group.ROOM, () -> {
            RoomView room = roomOrNull();
            return room == null ? "" : (room.name().isBlank() ? room.id() : room.name());
        }, HudStats::inRoom, "样例作战大厅");
        text("room_mode", "房间模式", Group.ROOM, () -> {
            RoomView room = roomOrNull();
            return room == null ? "" : room.rules().mode().displayName();
        }, HudStats::inRoom, "团队竞技");
        text("room_map", "房间地图", Group.ROOM, () -> {
            RoomView room = roomOrNull();
            return room == null ? "" : ClientLobbyData.mapDisplayName(room.mapId());
        }, HudStats::inRoom, "沙漠哨站");
        number("room_members", "房间人数", Group.ROOM, () -> {
            RoomView room = roomOrNull();
            return room == null ? 0 : room.memberCount();
        }, HudStats::inRoom, 4);
        number("room_max", "房间上限", Group.ROOM, () -> {
            RoomView room = roomOrNull();
            return room == null ? 0 : Math.max(0, room.maxPlayers());
        }, HudStats::inRoom, 12);
        text("room_players_text", "人数文本", Group.ROOM, () -> {
            RoomView room = roomOrNull();
            if (room == null) {
                return "";
            }
            return room.memberCount() + (room.maxPlayers() <= 0 ? "/不限" : "/" + room.maxPlayers());
        }, HudStats::inRoom, "4/12");
        progress("room_players_progress", "房间人数进度", Group.ROOM, () -> {
            RoomView room = roomOrNull();
            return room == null ? 0 : room.memberCount();
        }, () -> {
            RoomView room = roomOrNull();
            return room == null || room.maxPlayers() <= 0 ? 6 : room.maxPlayers();
        }, HudStats::inRoom, 4, 12);
        text("room_owner", "房主", Group.ROOM, () -> {
            RoomView room = roomOrNull();
            return room == null || room.owner() == null || room.owner().isBlank() ? "—" : room.owner();
        }, HudStats::inRoom, "Steve");
        text("room_state_text", "房间状态", Group.ROOM, () -> {
            RoomView room = roomOrNull();
            if (room == null) {
                return "";
            }
            return switch (room.state()) {
                case OPEN -> "开放中";
                case COUNTDOWN -> "开赛倒计时";
                case WAITING_MAP -> "等待地图";
                case RUNNING -> "比赛进行中";
            };
        }, HudStats::inRoom, "开放中");
        text("room_line1", "兼容整句：房间横幅第一行", Group.ROOM, () -> {
            RoomView room = roomOrNull();
            return room == null ? "" : SceneHudOverlay.roomLineOne(room);
        }, HudStats::inRoom, "房间  样例作战大厅  ·  团队竞技");
        text("room_line2", "兼容整句：房间横幅第二行", Group.ROOM, () -> {
            RoomView room = roomOrNull();
            return room == null ? "" : SceneHudOverlay.roomLineTwo(room);
        }, HudStats::inRoom, "人数 4/12  ·  房主 Steve  ·  开放中");
        text("room_message_text", "房间提示消息", Group.ROOM, ClientLobbyData::roomMessage,
                HudStats::inRoom, "已加入房间");

        // ---- 系统与玩家状态
        text("player_name", "我的名字", Group.SYSTEM, () -> {
            var player = Minecraft.getInstance().player;
            return player == null ? "" : player.getGameProfile().getName();
        }, () -> Minecraft.getInstance().player != null, "Steve");
        number("ping", "延迟（毫秒）", Group.SYSTEM, () -> {
            var connection = Minecraft.getInstance().getConnection();
            var player = Minecraft.getInstance().player;
            if (connection == null || player == null) {
                return 0.0;
            }
            var info = connection.getPlayerInfo(player.getUUID());
            return info == null ? 0.0 : (double) Math.max(0, info.getLatency());
        }, () -> Minecraft.getInstance().getConnection() != null, 38);
        number("fps", "帧率 FPS", Group.SYSTEM, Minecraft.getInstance()::getFps,
                () -> Minecraft.getInstance().level != null, 120);
        number("health", "生命值", Group.STATUS, () -> {
            var player = Minecraft.getInstance().player;
            return player == null ? 0 : player.getHealth();
        }, () -> Minecraft.getInstance().player != null, 14);
        number("armor", "护甲值", Group.STATUS, () -> {
            var player = Minecraft.getInstance().player;
            return player == null ? 0 : player.getArmorValue();
        }, () -> Minecraft.getInstance().player != null, 10);
        number("air", "氧气（气泡）", Group.STATUS, () -> {
            var player = Minecraft.getInstance().player;
            return player == null ? 0 : player.getAirSupply();
        }, () -> Minecraft.getInstance().player != null, 240);

        for (Source source : SOURCES) {
            BY_ID.put(source.id(), source);
        }
    }

    // ------------------------------------------------------------- 辅助注册

    private static void number(String id, String name, Group group, DoubleSupplier value,
                               BooleanSupplier live, double demo) {
        number(id, name, group, value, live, demo, defaultContexts(group));
    }

    private static void number(String id, String name, Group group, DoubleSupplier value,
                               BooleanSupplier live, double demo, Set<HudContext> contexts) {
        SOURCES.add(new Source(id, name, group, Kind.NUMBER, value, () -> 0, () -> "", live,
                demo, 0, "", contexts));
    }

    private static void decimal(String id, String name, Group group, DoubleSupplier value,
                                BooleanSupplier live, double demo) {
        SOURCES.add(new Source(id, name, group, Kind.DECIMAL, value, () -> 0, () -> "", live,
                demo, 0, "", defaultContexts(group)));
    }

    private static void time(String id, String name, Group group, IntSupplier ticks,
                             BooleanSupplier live, int demoTicks) {
        time(id, name, group, ticks, () -> Math.max(1, demoTicks), live, demoTicks,
                defaultContexts(group));
    }

    private static void time(String id, String name, Group group, IntSupplier ticks,
                             IntSupplier maximum, BooleanSupplier live, int demoTicks,
                             Set<HudContext> contexts) {
        SOURCES.add(new Source(id, name, group, Kind.TIME, () -> ticks.getAsInt(),
                maximum, () -> "", live, demoTicks, Math.max(1, demoTicks), "", contexts));
    }

    private static void text(String id, String name, Group group, Supplier<String> text,
                             BooleanSupplier live, String demoText) {
        text(id, name, group, text, live, demoText, defaultContexts(group));
    }

    private static void text(String id, String name, Group group, Supplier<String> text,
                             BooleanSupplier live, String demoText, Set<HudContext> contexts) {
        SOURCES.add(new Source(id, name, group, Kind.TEXT, () -> 0, () -> 0, text, live,
                0, 0, demoText, contexts));
    }

    private static void progress(String id, String name, Group group, DoubleSupplier value,
                                 IntSupplier maximum, BooleanSupplier live, double demoValue, int demoMax) {
        progress(id, name, group, value, maximum, live, demoValue, demoMax, defaultContexts(group));
    }

    private static void progress(String id, String name, Group group, DoubleSupplier value,
                                 IntSupplier maximum, BooleanSupplier live, double demoValue,
                                 int demoMax, Set<HudContext> contexts) {
        SOURCES.add(new Source(id, name, group, Kind.PROGRESS, value,
                () -> Math.max(1, maximum.getAsInt()), () -> "", live, demoValue, demoMax, "",
                contexts));
    }

    private static Set<HudContext> allContexts() {
        return EnumSet.allOf(HudContext.class);
    }

    private static Set<HudContext> matchContexts() {
        return EnumSet.of(HudContext.TEAM_DEATHMATCH, HudContext.SEARCH_DESTROY,
                HudContext.LAST_STANDING);
    }

    private static Set<HudContext> defaultContexts(Group group) {
        return switch (group) {
            case MATCHING -> EnumSet.of(HudContext.MATCHING);
            case ROOM -> EnumSet.of(HudContext.ROOM);
            case SCORE, PROGRESS, SELF, TEAM, NOTICE, TEXT -> matchContexts();
            case STATUS, SYSTEM, DYNAMIC -> allContexts();
        };
    }

    private static Set<HudContext> eventContexts(
            cn.blockforge.generated.generatedmod.match.MatchHudEventType type) {
        return switch (type) {
            case BOMB_SITE_REQUIRED, BOMB_PLANTING, BOMB_DEFUSING, BOMB_ACTION_INTERRUPTED,
                    BUY_PHASE_START, BUY_AREA_ONLY -> Set.of(HudContext.SEARCH_DESTROY);
            default -> matchContexts();
        };
    }

    /**
     * 模块文字模板：{@code %s}=格式化值，{@code %v}=原始数值，{@code %m}=上限；
     * 不含占位符时原样输出；旧配置加载时显式补上 %s。
     */
    public static String applyTemplate(String template, String formatted, String raw, String max) {
        if (template == null) {
            template = "";
        }
        var matcher = java.util.regex.Pattern.compile("%[svm]").matcher(template);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String value = switch (matcher.group()) { case "%s" -> formatted; case "%v" -> raw; default -> max; };
            matcher.appendReplacement(output, java.util.regex.Matcher.quoteReplacement(value == null ? "" : value));
        }
        return matcher.appendTail(output).toString();
    }

    /** 把公开统计值替换进内置 HUD 模板；未知占位符会原样保留，方便第三方扩展。 */
    public static String resolveTemplate(String template, Map<String, String> values) {
        String result = template == null ? "" : template;
        if (values == null) {
            return result;
        }
        var matcher = java.util.regex.Pattern.compile("\\{([a-zA-Z0-9_:.]+)\\}").matcher(result);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String replacement = values.getOrDefault(matcher.group(1), matcher.group());
            matcher.appendReplacement(output, java.util.regex.Matcher.quoteReplacement(
                    replacement == null ? "" : replacement));
        }
        return matcher.appendTail(output).toString();
    }

    /** 进度条标签：绑定源后按模板渲染百分比。 */
    public static String percentLabel(String template, double ratio) {
        int percent = (int) Math.round(Math.max(0.0, Math.min(1.0, ratio)) * 100);
        return applyTemplate(template, percent + "%", Integer.toString(percent), "");
    }
}
