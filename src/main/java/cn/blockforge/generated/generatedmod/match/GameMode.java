package cn.blockforge.generated.generatedmod.match;

import java.util.Locale;

/**
 * 游戏玩法模式。房间规则按模式展示不同的设置项：
 * <ul>
 *   <li>团队死斗：可复活、拼击杀目标或回合时长；</li>
 *   <li>爆破模式：回合歼灭制，回合内不可复活，按间隔回合换边，先胜 N 个回合赢下整场；</li>
 *   <li>歼灭竞技：单局歼灭，全程不复活，最后站着的队伍获胜。</li>
 * </ul>
 */
public enum GameMode {
    TEAM_DEATHMATCH("团队死斗"),
    SEARCH_DESTROY("爆破模式"),
    LAST_STANDING("歼灭竞技");

    private final String displayName;

    GameMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** 爆破与歼灭都是“阵亡即退出本回合/本局”的歼灭制。 */
    public boolean elimination() {
        return this == SEARCH_DESTROY || this == LAST_STANDING;
    }

    /** 只有爆破模式按回合换边、累计回合胜场。 */
    public boolean roundSwapping() {
        return this == SEARCH_DESTROY;
    }

    /** 击杀目标与复活参数只对团队死斗生效。 */
    public boolean respawnRules() {
        return this == TEAM_DEATHMATCH;
    }

    public static GameMode byOrdinal(int ordinal) {
        GameMode[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : TEAM_DEATHMATCH;
    }

    public static GameMode parse(String value) {
        if (value == null || value.isBlank()) {
            return TEAM_DEATHMATCH;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (GameMode mode : values()) {
            if (mode.name().equals(normalized)) {
                return mode;
            }
        }
        return TEAM_DEATHMATCH;
    }
}
