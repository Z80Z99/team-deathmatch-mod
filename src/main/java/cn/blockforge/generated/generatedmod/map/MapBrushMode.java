package cn.blockforge.generated.generatedmod.map;

/** 画笔的两种输入语义。 */
public enum MapBrushMode {
    REGION("区域"),
    BLOCK("方块");

    private final String displayName;

    MapBrushMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() { return displayName; }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static MapBrushMode parse(String value) {
        if (value == null || value.isBlank()) return REGION;
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return REGION;
        }
    }
}
