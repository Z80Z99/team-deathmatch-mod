package cn.blockforge.generated.generatedmod.match;

import java.util.Arrays;

/** Structured in-match events that can trigger individual configurable HUD elements. */
public enum MatchHudEventType {
    START_COUNTDOWN("start_countdown", "10 秒后开始", "比赛将在 10 秒后开始", 200, 90),
    BUY_PHASE_START("buy_phase_start", "购买阶段开始", "购买装备并准备下一阶段", 100, 70),
    BUY_AREA_ONLY("buy_area_only", "只能在出生区活动", "离开出生区域会被送回", 80, 72),
    ACTION_PHASE("action_phase", "行动阶段", "正式对局已经开始", 70, 75),
    ROUND_DRAW("round_draw", "回合平局", "本回合没有队伍获胜", 80, 80),
    BOMB_SITE_REQUIRED("bomb_site_required", "必须站在爆破区才能安装", "进入激活的爆破区域后再安装 C4", 70, 95),
    BOMB_PLANTING("bomb_planting", "正在安装 C4", "保持安装动作直到进度完成", 40, 96),
    BOMB_DEFUSING("bomb_defusing", "正在拆除 C4", "保持拆除动作直到进度完成", 40, 96),
    BOMB_ACTION_INTERRUPTED("bomb_action_interrupted", "动作已中断", "安装或拆除没有完成", 60, 94);

    private final String id;
    private final String displayName;
    private final String defaultDetail;
    private final int defaultDurationTicks;
    private final int priority;

    MatchHudEventType(String id, String displayName, String defaultDetail,
                      int defaultDurationTicks, int priority) {
        this.id = id;
        this.displayName = displayName;
        this.defaultDetail = defaultDetail;
        this.defaultDurationTicks = defaultDurationTicks;
        this.priority = priority;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public String defaultDetail() { return defaultDetail; }
    public int defaultDurationTicks() { return defaultDurationTicks; }
    public int priority() { return priority; }

    public static MatchHudEventType byOrdinal(int ordinal) {
        return ordinal < 0 || ordinal >= values().length ? null : values()[ordinal];
    }

    public static MatchHudEventType byId(String id) {
        return Arrays.stream(values()).filter(value -> value.id.equals(id)).findFirst().orElse(null);
    }
}
