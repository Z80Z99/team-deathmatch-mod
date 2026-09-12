package cn.blockforge.generated.generatedmod.client;

import net.minecraft.client.Minecraft;

import java.util.LinkedHashMap;
import java.util.Map;

/** One parameter vocabulary for both examples and user-created elements. */
public final class HudParameters {
    private static final java.util.regex.Pattern PARAMETER = java.util.regex.Pattern.compile("\\{([a-zA-Z0-9_:.]+)\\}");
    private HudParameters() { }

    public static Map<String, String> values(boolean editor) {
        return values(editor, HudContext.GLOBAL, false);
    }

    /**
     * Parameter vocabulary for one scene.
     *
     * @param blankUnavailable live rendering mode: unavailable sources become empty strings
     *                         instead of leaking zero or stale values into the HUD
     */
    public static Map<String, String> values(boolean editor, HudContext context, boolean blankUnavailable) {
        Map<String, String> values = new LinkedHashMap<>();
        HudContext effective = context == null ? HudContext.GLOBAL : context;
        for (HudStats.Source source : HudStats.sourcesFor(effective)) {
            if (!source.availableIn(effective)) {
                continue;
            }
            if (blankUnavailable && !editor && !source.isLive()) {
                values.put(source.id(), "");
                continue;
            }
            values.put(source.id(), source.display(editor, effective));
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
        return switch (ClientMatchData.mode) {
            case TEAM_DEATHMATCH -> Math.max(0, ClientMatchData.targetKills);
            case SEARCH_DESTROY -> Math.max(1, ClientMatchData.roundsToWin);
            case LAST_STANDING -> 0;
        };
    }

    public static int teamCount(boolean editor) {
        var room = SceneHudOverlay.ownRoom();
        return Math.max(2, room != null && editor ? room.teamCount() : ClientMatchData.teamStats.size());
    }

    public static boolean visible(String condition, boolean editor) {
        return visible(condition, editor, HudContext.GLOBAL);
    }

    public static boolean visible(String condition, boolean editor, HudContext context) {
        if (editor) return true;
        Boolean external = HudConditions.external(condition);
        if (external != null) return external;
        if (condition != null && condition.startsWith("event:")) {
            return ClientHudEventData.active(condition.substring("event:".length()));
        }
        Boolean threshold = thresholdCondition(condition);
        if (threshold != null) return threshold;
        if (condition == null || condition.isBlank()) return true;
        if (condition.startsWith("api:")) return false;
        return switch (condition) {
            case "respawning" -> ClientMatchData.awaitingRespawn;
            case "death" -> RespawnOverlay.deathActive();
            case "respawn_waiting" -> ClientMatchData.awaitingRespawn && ClientMatchData.respawnRemainingTicks > 0;
            case "respawn_ready" -> ClientMatchData.awaitingRespawn && ClientMatchData.respawnRemainingTicks <= 0;
            case "returned" -> RespawnOverlay.returning();
            case "alive" -> !ClientMatchData.awaitingRespawn && ClientMatchData.myTeam.isPlayable()
                    && !ClientMatchData.pending;
            case "playing" -> ClientMatchData.state == cn.blockforge.generated.generatedmod.match.MatchState.PLAYING;
            case "warmup" -> ClientMatchData.state == cn.blockforge.generated.generatedmod.match.MatchState.WARMUP;
            case "warmup_waiting" -> ClientMatchData.state == cn.blockforge.generated.generatedmod.match.MatchState.WARMUP
                    && ClientMatchData.phaseRemainingTicks <= 0;
            case "warmup_countdown" -> ClientMatchData.state == cn.blockforge.generated.generatedmod.match.MatchState.WARMUP
                    && ClientMatchData.phaseRemainingTicks > 0;
            case "buying" -> ClientMatchData.state == cn.blockforge.generated.generatedmod.match.MatchState.BUYING;
            case "frozen" -> ClientMatchData.state == cn.blockforge.generated.generatedmod.match.MatchState.FROZEN;
            case "round_end" -> ClientMatchData.state == cn.blockforge.generated.generatedmod.match.MatchState.ROUND_END;
            case "terrain_restoring" -> ClientMatchData.state == cn.blockforge.generated.generatedmod.match.MatchState.TERRAIN_RESTORING;
            case "map_resetting" -> ClientMatchData.state == cn.blockforge.generated.generatedmod.match.MatchState.MAP_RESETTING;
            case "match_end" -> ClientMatchData.state == cn.blockforge.generated.generatedmod.match.MatchState.MATCH_END;
            case "notice" -> ClientMatchData.inMatch()
                    && (ClientMatchData.state != cn.blockforge.generated.generatedmod.match.MatchState.PLAYING
                    || MatchHudNotice.urgent());
            case "notice_urgent" -> MatchHudNotice.urgent();
            case "event_active" -> ClientHudEventData.active();
            case "c4_active", "c4_carried", "c4_dropped", "c4_planting", "c4_planted",
                    "c4_defusing", "c4_exploded", "c4_defused" -> false;
            case "outside" -> ClientMatchData.boundaryOutside;
            case "round_odd" -> ClientMatchData.roundNumber % 2 == 1;
            case "round_even" -> ClientMatchData.roundNumber % 2 == 0;
            case "team_leading" -> scoreState() > 0;
            case "team_trailing" -> scoreState() < 0;
            case "score_tied" -> scoreState() == 0;
            case "spectator" -> !ClientMatchData.myTeam.isPlayable();
            case "feed" -> editor || ClientMatchData.killFeedActive();
            case "team_c" -> teamCount(editor) >= 3;
            case "team_d" -> teamCount(editor) >= 4;
            case "forming" -> ClientLobbyData.dynamicReadySeconds() > 0;
            case "queued" -> ClientLobbyData.matchmaking().queued()
                    && ClientLobbyData.dynamicReadySeconds() <= 0;
            default -> false;
        };
    }

    private static Boolean thresholdCondition(String condition) {
        if (condition == null || !condition.contains(":")) return null;
        String[] parts = condition.split(":", 2);
        int value;
        try {
            value = Integer.parseInt(parts[1]);
        } catch (NumberFormatException error) {
            return null;
        }
        return switch (parts[0]) {
            case "health_below" -> (int) Math.ceil(healthPercent()) < value;
            case "health_above" -> (int) Math.floor(healthPercent()) > value;
            case "armor_below" -> armor() < value;
            case "money_below" -> ClientMatchData.matchBalance < value;
            case "money_at_least" -> ClientMatchData.matchBalance >= value;
            case "kills_at_least" -> ClientMatchData.myMatchKills >= value;
            case "deaths_at_least" -> ClientMatchData.myMatchDeaths >= value;
            case "phase_remaining_below" -> ClientMatchData.phaseRemainingTicks > 0
                    && ClientMatchData.phaseRemainingTicks <= value * 20;
            case "boundary_below" -> ClientMatchData.boundaryOutside
                    && ClientMatchData.boundaryTicks > 0 && ClientMatchData.boundaryTicks <= value * 20;
            default -> null;
        };
    }

    private static double healthPercent() {
        var player = Minecraft.getInstance().player;
        return player == null ? 0.0D : player.getHealth() / player.getMaxHealth() * 100.0D;
    }

    private static int armor() {
        var player = Minecraft.getInstance().player;
        return player == null ? 0 : player.getArmorValue();
    }

    private static int scoreState() {
        int mine = metric(ClientMatchData.myTeam);
        int highest = ClientMatchData.teamStats.stream().mapToInt(stats -> metric(stats.team())).max().orElse(mine);
        int lowest = ClientMatchData.teamStats.stream().mapToInt(stats -> metric(stats.team())).min().orElse(mine);
        if (mine >= highest && highest > lowest) return 1;
        if (mine <= lowest && highest > lowest) return -1;
        return 0;
    }

    private static int metric(cn.blockforge.generated.generatedmod.match.Team team) {
        var stats = ClientMatchData.stats(team);
        return ClientMatchData.mode == cn.blockforge.generated.generatedmod.match.GameMode.TEAM_DEATHMATCH
                ? stats.score() : stats.wins();
    }

    private static void alias(Map<String, String> values, String key, String source) {
        values.put(key, values.getOrDefault(source, ""));
    }

    public static String render(String template, boolean editor) {
        return render(template, HudContext.GLOBAL, editor);
    }

    public static String render(String template, HudContext context, boolean editor) {
        if (template == null || template.indexOf('{') < 0) return template == null ? "" : template;
        Map<String, String> selected = new LinkedHashMap<>();
        Map<String, String> available = values(editor, context, true);
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
            if (HudStats.hudRestricted(sourceId)) {
                selected.put(key, "");
                continue;
            }
            var source = HudStats.byId(sourceId);
            String value = switch (key) {
                case "round" -> editor && !ClientMatchData.inMatch() ? "1" : Integer.toString(ClientMatchData.roundNumber);
                case "target" -> editor && !ClientMatchData.inMatch() ? "25" : Integer.toString(target());
                case "rounds_to_win" -> editor && !ClientMatchData.inMatch() ? "3" : Integer.toString(ClientMatchData.roundsToWin);
                case "team_count" -> Integer.toString(teamCount(editor));
                case "killer" -> editor ? "Steve" : ClientMatchData.killFeedKiller();
                case "victim" -> editor ? "Alex" : ClientMatchData.killFeedVictim();
                default -> available.containsKey(key) ? available.get(key)
                        : source == null ? null : source.display(editor, context);
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
            case "notice_title" -> "阶段或事件标题"; case "notice_detail" -> "阶段或事件说明";
            case "notice_timer" -> "阶段、C4、越界或复活倒计时";
            case "held_weapon" -> "当前主手武器或物品名称"; case "held_ammo" -> "TACZ 当前弹匣/备用弹药";
            case "held_durability" -> "当前主手武器耐久百分比";
            case "sizes" -> "兼容：队伍人数汇总"; default -> key;
        };
    }
}
