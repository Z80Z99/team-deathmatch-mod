package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import cn.blockforge.generated.generatedmod.lobby.MatchmakingManager;
import cn.blockforge.generated.generatedmod.lobby.RoomView;
import cn.blockforge.generated.generatedmod.match.GameMode;
import cn.blockforge.generated.generatedmod.match.Team;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;
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
        PROGRESS("比赛进度"), SCORE("比分与回合"), SELF("我的统计"), TEAM("队伍统计"),
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
                         BooleanSupplier live, double demo, int demoMaximum, String demoText) {

        /** 配置窗里是否拿得到真实数据（拿不到时编辑器回退示例值）。 */
        public boolean isLive() {
            return live.getAsBoolean();
        }

        /** 原始数值（时间类返回 tick 数；文本类返回 0）。 */
        public double number(boolean editor) {
            if (editor && !isLive()) {
                return demo;
            }
            try {
                return kind == Kind.TEXT ? 0 : value.getAsDouble();
            } catch (Exception error) {
                return editor ? demo : 0;
            }
        }

        /** 进度上限；无上限（普通数值）返回 0。 */
        public int maximum(boolean editor) {
            if (kind != Kind.PROGRESS && kind != Kind.TIME) {
                return 0;
            }
            if (editor && !isLive()) {
                return Math.max(kind == Kind.TIME ? 0 : 1, demoMaximum);
            }
            try {
                return Math.max(kind == Kind.TIME ? 0 : 1, maximum.getAsInt());
            } catch (Exception error) {
                return Math.max(1, demoMaximum);
            }
        }

        /** 0..1 的进度比例；普通数值按“百分数”解释。 */
        public double ratio(boolean editor) {
            if (kind == Kind.TEXT) {
                return 0;
            }
            if (kind == Kind.PROGRESS || kind == Kind.TIME) {
                return maximum(editor) <= 0 ? 0 : clamp01(number(editor) / maximum(editor));
            }
            return clamp01(number(editor) / 100.0);
        }

        /** 已格式化好的展示文本（数字取整、时间 mm:ss、进度显示 x/y）。 */
        public String display(boolean editor) {
            switch (kind) {
                case TEXT: {
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
                    return UiTheme.formatTicks((int) number(editor));
                case DECIMAL:
                    return String.format(Locale.ROOT, "%.1f", number(editor));
                case PROGRESS:
                    return Long.toString(Math.round(number(editor))) + "/" + maximum(editor);
                default:
                    return Long.toString(Math.round(number(editor)));
            }
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
        return all;
    }

    public static List<Source> sourcesFor(HudContext context) {
        return sourcesWithDynamic().stream().filter(source -> switch (source.group()) {
            case SYSTEM, DYNAMIC, STATUS -> true;
            case ROOM -> context == HudContext.ROOM;
            case MATCHING -> context == HudContext.MATCHING;
            default -> context.isMatch() && (!source.id().equals("respawn_left")
                    || context == HudContext.TEAM_DEATHMATCH);
        }).toList();
    }

    /** 第三方玩法注册的实时数据源；重复 id 会替换旧定义。 */
    public static synchronized void register(Source source) {
        if (source == null || source.id() == null || source.id().isBlank()
                || source.id().startsWith("dyn:") || source.value() == null || source.maximum() == null
                || source.text() == null || source.live() == null) {
            throw new IllegalArgumentException("HUD 数据源参数无效");
        }
        ensureBuilt();
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
        return external == null ? BY_ID.get(id) : external;
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
                    () -> ClientMatchData.stats(team).score(), () -> Math.max(1, ClientMatchData.targetKills), HudStats::inMatch, 8, 25);
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
                () -> ClientMatchData.respawnRemainingTicks, HudStats::inMatch, 5 * 20);

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
                () -> ClientMatchData.phaseRemainingTicks, HudStats::inMatch, 225 * 20);
        time("elapsed", "本局已进行时间", Group.PROGRESS, ClientMatchData::elapsedTicks, HudStats::inMatch, 165 * 20);

        // ---- 进度类
        progress("win_progress", "胜利进度（领先方）", Group.PROGRESS,
                HudStats::leadScore, HudStats::leadTarget, HudStats::inMatch, 6, 10);
        progress("team_a_progress", "A队击杀进度", Group.PROGRESS,
                () -> ClientMatchData.teamAScore, () -> Math.max(1, ClientMatchData.targetKills),
                HudStats::inMatch, 12, 25);
        progress("team_b_progress", "B队击杀进度", Group.PROGRESS,
                () -> ClientMatchData.teamBScore, () -> Math.max(1, ClientMatchData.targetKills),
                HudStats::inMatch, 9, 25);
        progress("total_kill_progress", "房间总击杀进度", Group.PROGRESS,
                () -> allStats().mapToInt(value -> value.score()).sum(),
                () -> Math.max(1, ClientMatchData.targetKills), HudStats::inMatch, 21, 25);
        progress("my_wins_progress", "我方胜场进度", Group.PROGRESS,
                HudStats::myTeamWins, () -> Math.max(1, ClientMatchData.roundsToWin), HudStats::inMatch, 1, 3);
        for (Team team : Team.playing(4)) {
            String key = team.key().substring(5);
            progress("team_" + key + "_wins_progress", team.displayName() + "胜场进度", Group.PROGRESS,
                    () -> ClientMatchData.stats(team).wins(), () -> Math.max(1, ClientMatchData.roundsToWin), HudStats::inMatch, 1, 3);
        }
        progress("health_percent", "我的血量百分比", Group.STATUS,
                () -> {
                    var player = Minecraft.getInstance().player;
                    return player == null ? 0 : player.getHealth() / player.getMaxHealth() * 100.0;
                }, () -> 100, () -> Minecraft.getInstance().player != null, 76, 100);

        // ---- 比赛文本
        text("mode_text", "模式名", Group.TEXT, ClientMatchData::modeText, HudStats::inMatch, "团队竞技");
        text("phase_text", "比赛阶段", Group.TEXT, ClientMatchData::phaseText, HudStats::inMatch, "进行中");
        text("my_team_text", "我的队伍", Group.TEXT,
                () -> ClientMatchData.myTeam.displayName(), HudStats::inMatch, "A队");
        text("round_text", "回合数（兼容别名）", Group.TEXT, () -> Integer.toString(ClientMatchData.roundNumber), HudStats::inMatch, "1");
        text("target_text", "目标数（兼容别名）", Group.TEXT, () -> Integer.toString(HudParameters.target()), HudStats::inMatch, "25");
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
                HudStats::inQueue, 18 * 20);
        number("ready_seconds", "开赛倒计时（秒）", Group.MATCHING, ClientLobbyData::dynamicReadySeconds,
                HudStats::forming, 3);
        progress("queue_progress", "成局人数进度", Group.MATCHING,
                () -> ClientLobbyData.matchmaking().queueSize(), () -> MatchmakingManager.MIN_PLAYERS_TO_FORM,
                HudStats::inQueue, 3, 6);
        progress("ready_progress", "开赛准备进度", Group.MATCHING, ClientLobbyData::dynamicReadySeconds,
                () -> 5, HudStats::forming, 3, 5);
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
        SOURCES.add(new Source(id, name, group, Kind.NUMBER, value, () -> 0, () -> "", live, demo, 0, ""));
    }

    private static void decimal(String id, String name, Group group, DoubleSupplier value,
                                BooleanSupplier live, double demo) {
        SOURCES.add(new Source(id, name, group, Kind.DECIMAL, value, () -> 0, () -> "", live, demo, 0, ""));
    }

    private static void time(String id, String name, Group group, IntSupplier ticks,
                             BooleanSupplier live, int demoTicks) {
        SOURCES.add(new Source(id, name, group, Kind.TIME, () -> ticks.getAsInt(),
                () -> id.equals("respawn_left") ? Math.max(1, ClientMatchData.respawnTotalTicks) : 0,
                () -> "", live, demoTicks, id.equals("respawn_left") ? Math.max(1, demoTicks) : 0, ""));
    }

    private static void text(String id, String name, Group group, Supplier<String> text,
                             BooleanSupplier live, String demoText) {
        SOURCES.add(new Source(id, name, group, Kind.TEXT, () -> 0, () -> 0, text, live, 0, 0, demoText));
    }

    private static void progress(String id, String name, Group group, DoubleSupplier value,
                                 IntSupplier maximum, BooleanSupplier live, double demoValue, int demoMax) {
        SOURCES.add(new Source(id, name, group, Kind.PROGRESS, value,
                () -> Math.max(1, maximum.getAsInt()), () -> "", live, demoValue, demoMax, ""));
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
