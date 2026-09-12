package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.match.GameMode;

/**
 * HUD 配置场景：配置窗按场景分开调整，互不影响。
 * 比赛按游戏模式各一份（团队竞技 / 爆破 / 歼灭），另有“正在匹配”“房间中”两个大厅场景。
 */
public enum HudContext {
    TEAM_DEATHMATCH("比赛 · 团队竞技", "match:TEAM_DEATHMATCH"),
    SEARCH_DESTROY("比赛 · 爆破模式", "match:SEARCH_DESTROY"),
    LAST_STANDING("比赛 · 歼灭竞技", "match:LAST_STANDING"),
    MATCHING("正在匹配", "scene:MATCHING"),
    ROOM("房间中", "scene:ROOM"),
    GLOBAL("背景/全局", "global");

    private final String displayName;
    private final String key;

    HudContext(String displayName, String key) {
        this.displayName = displayName;
        this.key = key;
    }

    public String displayName() {
        return displayName;
    }

    /** 配置窗顶部页签用的短名。 */
    public String tabLabel() {
        return switch (this) {
            case TEAM_DEATHMATCH -> "死斗";
            case SEARCH_DESTROY -> "爆破";
            case LAST_STANDING -> "歼灭";
            case MATCHING -> "正在匹配";
            case ROOM -> "房间中";
            case GLOBAL -> "背景/全局";
        };
    }

    /** client-hud.json 里 contexts 对象使用的稳定键。 */
    public String key() {
        return key;
    }

    public boolean isMatch() {
        return this == TEAM_DEATHMATCH || this == SEARCH_DESTROY || this == LAST_STANDING;
    }

    /** 场景内置组件；每项都可以单独关闭或交给独立模块替代。 */
    public enum BuiltIn {
        SCORE("记分板"), NOTICE("阶段公告"), TEXT("状态文字"), FEED("击杀播报"), BANNER("场景横幅");

        private final String displayName;

        BuiltIn(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    public static HudContext match(GameMode mode) {
        return switch (mode) {
            case TEAM_DEATHMATCH -> TEAM_DEATHMATCH;
            case SEARCH_DESTROY -> SEARCH_DESTROY;
            case LAST_STANDING -> LAST_STANDING;
        };
    }

    public static HudContext fromKey(String key) {
        for (HudContext context : values()) {
            if (context.key.equals(key)) {
                return context;
            }
        }
        return null;
    }
}
