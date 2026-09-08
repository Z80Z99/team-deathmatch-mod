package cn.blockforge.generated.generatedmod.map;

import cn.blockforge.generated.generatedmod.match.Team;

import java.util.Locale;

/** 规划器和画笔当前操作的地形对象。 */
public enum MapTool {
    BOUNDS("bounds", "地图边界", Kind.REGION),
    RESET("reset", "重置区域", Kind.REGION),
    CUSTOM("custom", "自定义区域", Kind.REGION),
    SPAWN_A("spawn_a", "A队出生点", Kind.POINT),
    SPAWN_B("spawn_b", "B队出生点", Kind.POINT),
    SPAWN_C("spawn_c", "C队出生点", Kind.POINT),
    SPAWN_D("spawn_d", "D队出生点", Kind.POINT),
    SPECTATOR("spectator", "观战出生点", Kind.POINT);

    private final String id;
    private final String displayName;
    private final Kind kind;

    MapTool(String id, String displayName, Kind kind) {
        this.id = id;
        this.displayName = displayName;
        this.kind = kind;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public Kind kind() { return kind; }

    public Team team() {
        return switch (this) {
            case SPAWN_A -> Team.TEAM_A;
            case SPAWN_B -> Team.TEAM_B;
            case SPAWN_C -> Team.TEAM_C;
            case SPAWN_D -> Team.TEAM_D;
            case SPECTATOR -> Team.SPECTATOR;
            default -> null;
        };
    }

    public static MapTool parse(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (MapTool tool : values()) {
            if (tool.id.equals(normalized)) return tool;
        }
        return null;
    }

    public enum Kind { REGION, POINT }
}
