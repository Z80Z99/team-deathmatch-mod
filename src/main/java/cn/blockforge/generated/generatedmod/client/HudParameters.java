package cn.blockforge.generated.generatedmod.client;

import java.util.LinkedHashMap;
import java.util.Map;

/** One parameter vocabulary for both examples and user-created elements. */
public final class HudParameters {
    private static final java.util.regex.Pattern PARAMETER = java.util.regex.Pattern.compile("\\{([a-zA-Z0-9_:.]+)\\}");
    private HudParameters() { }

    public static Map<String, String> values(boolean editor) {
        Map<String, String> values = new LinkedHashMap<>();
        for (HudStats.Source source : HudStats.sourcesWithDynamic()) {
            values.put(source.id(), source.display(editor));
        }
        alias(values, "mode", "mode_text");
        alias(values, "phase", "phase_text");
        alias(values, "team", "my_team_text");
        alias(values, "time", "phase_remaining");
        alias(values, "queue", "queue_size");
        alias(values, "position", "queue_position");
        alias(values, "wait", "queue_wait");
        alias(values, "ready", "ready_seconds");
        alias(values, "room", "room_name");
        alias(values, "map", "room_map");
        alias(values, "members", "room_members");
        alias(values, "owner", "room_owner");
        alias(values, "state", "room_state_text");
        values.put("target", editor && !ClientMatchData.inMatch() ? "25"
                : Integer.toString(target()));
        values.put("round", editor && !ClientMatchData.inMatch() ? "1"
                : Integer.toString(ClientMatchData.roundNumber));
        values.put("rounds_to_win", editor && !ClientMatchData.inMatch() ? "3"
                : Integer.toString(ClientMatchData.roundsToWin));
        values.put("team_count", Integer.toString(teamCount(editor)));
        values.put("killer", editor ? "Steve" : ClientMatchData.killFeedKiller());
        values.put("victim", editor ? "Alex" : ClientMatchData.killFeedVictim());
        values.put("feed", editor ? "Steve 击杀 Alex" : ClientMatchData.killFeedText());
        values.put("hint", editor && !ClientMatchData.inMatch() ? "A队" : MatchHudOverlay.hintText());
        values.put("sizes", editor && !ClientMatchData.inMatch() ? "4:4" : ClientMatchData.teamSizesText());
        return values;
    }

    public static int target() {
        return ClientMatchData.mode == cn.blockforge.generated.generatedmod.match.GameMode.TEAM_DEATHMATCH
                ? Math.max(0, ClientMatchData.targetKills) : Math.max(1, ClientMatchData.roundsToWin);
    }

    public static int teamCount(boolean editor) {
        var room = SceneHudOverlay.ownRoom();
        return Math.max(2, room != null && editor ? room.teamCount() : ClientMatchData.teamStats.size());
    }

    public static boolean visible(String condition, boolean editor) {
        if (editor) return true;
        Boolean external = HudConditions.external(condition);
        if (external != null) return external;
        if (condition.startsWith("api:")) return false;
        return switch (condition) {
            case "respawning" -> ClientMatchData.awaitingRespawn;
            case "alive" -> !ClientMatchData.awaitingRespawn && ClientMatchData.myTeam.isPlayable()
                    && !ClientMatchData.pending;
            case "playing" -> ClientMatchData.state == cn.blockforge.generated.generatedmod.match.MatchState.PLAYING;
            case "warmup" -> ClientMatchData.state == cn.blockforge.generated.generatedmod.match.MatchState.WARMUP;
            case "outside" -> ClientMatchData.boundaryTicks > 0;
            case "spectator" -> !ClientMatchData.myTeam.isPlayable();
            case "feed" -> editor || ClientMatchData.killFeedActive();
            case "team_c" -> teamCount(editor) >= 3;
            case "team_d" -> teamCount(editor) >= 4;
            case "forming" -> ClientLobbyData.dynamicReadySeconds() > 0;
            case "queued" -> ClientLobbyData.dynamicReadySeconds() <= 0;
            default -> true;
        };
    }

    private static void alias(Map<String, String> values, String key, String source) {
        values.put(key, values.getOrDefault(source, ""));
    }

    public static String render(String template, boolean editor) {
        if (template == null || template.indexOf('{') < 0) return template == null ? "" : template;
        Map<String, String> selected = new LinkedHashMap<>();
        var matcher = PARAMETER.matcher(template);
        while (matcher.find()) {
            String key = matcher.group(1);
            if (selected.containsKey(key)) continue;
            String sourceId = switch (key) {
                case "mode" -> "mode_text"; case "phase" -> "phase_text";
                case "team" -> "my_team_text"; case "time" -> "phase_remaining";
                case "queue" -> "queue_size"; case "position" -> "queue_position";
                case "wait" -> "queue_wait"; case "ready" -> "ready_seconds";
                case "room" -> "room_name"; case "map" -> "room_map";
                case "members" -> "room_members"; case "owner" -> "room_owner";
                case "state" -> "room_state_text"; case "sizes" -> "team_sizes_text";
                case "hint" -> "hint_text"; case "feed" -> "kill_feed_text";
                default -> key;
            };
            var source = HudStats.byId(sourceId);
            String value = switch (key) {
                case "round" -> editor && !ClientMatchData.inMatch() ? "1" : Integer.toString(ClientMatchData.roundNumber);
                case "target" -> editor && !ClientMatchData.inMatch() ? "25" : Integer.toString(target());
                case "rounds_to_win" -> editor && !ClientMatchData.inMatch() ? "3" : Integer.toString(ClientMatchData.roundsToWin);
                case "team_count" -> Integer.toString(teamCount(editor));
                case "killer" -> editor ? "Steve" : ClientMatchData.killFeedKiller();
                case "victim" -> editor ? "Alex" : ClientMatchData.killFeedVictim();
                default -> source == null ? null : source.display(editor);
            };
            if (value != null) selected.put(key, value);
        }
        return HudStats.resolveTemplate(template, selected);
    }

    public static String migrateTemplate(String text) {
        // Only old scalar placeholders acquired labels implicitly. Unknown/user text is preserved.
        return text.replace("{round}", "回合 {round}").replace("{target}", "目标 {target}")
                .replaceAll("回合\\s*回合", "回合").replaceAll("目标\\s*目标", "目标");
    }

    public static String description(String key) {
        return switch (key) {
            case "mode" -> "比赛模式"; case "phase" -> "比赛阶段";
            case "team" -> "我的队伍名称"; case "time" -> "阶段倒计时，分:秒";
            case "queue" -> "匹配队列人数"; case "position" -> "匹配序位";
            case "wait" -> "等待时间，分:秒"; case "ready" -> "开赛倒计时，秒";
            case "room" -> "房间名称"; case "map" -> "地图名称";
            case "members" -> "房间人数"; case "owner" -> "房主名称";
            case "state" -> "房间状态"; case "target" -> "目标击杀或胜场数，0 表示按时间判定";
            case "rounds_to_win" -> "获胜所需回合数"; case "team_count" -> "参赛队伍数";
            case "killer" -> "击杀者名称"; case "victim" -> "被击杀者名称";
            case "feed" -> "兼容：完整击杀消息"; case "hint" -> "当前状态消息";
            case "sizes" -> "兼容：队伍人数汇总"; default -> key;
        };
    }
}
