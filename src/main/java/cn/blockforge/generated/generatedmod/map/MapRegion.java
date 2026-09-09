package cn.blockforge.generated.generatedmod.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;

import java.util.Locale;

/** 地图中可被规划器选中并编辑的任意区域。 */
public record MapRegion(
        String id,
        String displayName,
        Type type,
        MapDefinition.Region region,
        boolean visibleInMatch,
        int displayRange,
        Appearance appearance,
        Activation activation,
        String activationValue,
        int color,
        boolean outline,
        boolean fill,
        int priority,
        String notes) {

    public MapRegion {
        id = normalizeId(id);
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        type = type == null ? Type.CUSTOM : type;
        region = region == null ? new MapDefinition.Region(new BlockPos(0, 0, 0), new BlockPos(0, 0, 0)) : region;
        visibleInMatch = visibleInMatch && type != Type.BOUNDS && type != Type.RESET;
        displayRange = Math.max(0, displayRange);
        appearance = appearance == null ? Appearance.ALWAYS : appearance;
        activation = activation == null ? Activation.ALWAYS : activation;
        activationValue = activationValue == null ? "" : activationValue;
        color = color == 0 ? type.defaultColor() : color;
        priority = Math.max(0, priority);
        notes = notes == null ? "" : notes;
    }

    public static MapRegion builtIn(String id, String displayName, Type type,
                                    MapDefinition.Region region, int color) {
        return new MapRegion(id, displayName, type, region, true, 64,
                Appearance.ALWAYS, Activation.ALWAYS, "", color, true, true, 0, "");
    }

    public static MapRegion custom(String id, String displayName, Type type, MapDefinition.Region region) {
        return new MapRegion(id, displayName, type, region, true, 64,
                Appearance.NEARBY, Activation.ALWAYS, "", 0, true, true, 0, "");
    }

    public static String normalizeId(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("id", id);
        object.addProperty("displayName", displayName);
        object.addProperty("type", type.id());
        object.add("region", region.toJson());
        object.addProperty("visibleInMatch", visibleInMatch);
        object.addProperty("displayRange", displayRange);
        object.addProperty("appearance", appearance.id());
        object.addProperty("activation", activation.id());
        object.addProperty("activationValue", activationValue);
        object.addProperty("color", color);
        object.addProperty("outline", outline);
        object.addProperty("fill", fill);
        object.addProperty("priority", priority);
        object.addProperty("notes", notes);
        return object;
    }

    public static MapRegion fromJson(JsonElement element) {
        if (element == null || !element.isJsonObject()) return null;
        JsonObject object = element.getAsJsonObject();
        return new MapRegion(
                text(object, "id", ""),
                text(object, "displayName", text(object, "id", "")),
                Type.parse(text(object, "type", Type.CUSTOM.id())),
                MapDefinition.regionFromJson(object.get("region")),
                bool(object, "visibleInMatch", true),
                integer(object, "displayRange", 64),
                Appearance.parse(text(object, "appearance", Appearance.ALWAYS.id())),
                Activation.parse(text(object, "activation", Activation.ALWAYS.id())),
                text(object, "activationValue", ""),
                integer(object, "color", 0),
                bool(object, "outline", true),
                bool(object, "fill", true),
                integer(object, "priority", 0),
                text(object, "notes", ""));
    }

    private static String text(JsonObject object, String name, String fallback) {
        return object.has(name) && !object.get(name).isJsonNull() ? object.get(name).getAsString() : fallback;
    }

    private static boolean bool(JsonObject object, String name, boolean fallback) {
        return object.has(name) && !object.get(name).isJsonNull() ? object.get(name).getAsBoolean() : fallback;
    }

    private static int integer(JsonObject object, String name, int fallback) {
        return object.has(name) && !object.get(name).isJsonNull() ? object.get(name).getAsInt() : fallback;
    }

    public enum Type {
        BOUNDS("bounds", "地图边界", 0xFF70C7E8),
        RESET("reset", "重置区域", 0xFFFFC857),
        CUSTOM("custom", "自定义区域", 0xFF63D39A),
        BOMB("bomb", "爆破区", 0xFFFF7070),
        CAPTURE("capture", "占领区", 0xFF5599FF),
        HOTSPOT("hotspot", "热点区", 0xFFFFC857),
        OBJECTIVE("objective", "目标区", 0xFF9CE5AF),
        OTHER("other", "其他区域", 0xFFB8BCC2),
        SPAWN_A("spawn_a", "A队复活区域", 0xFFFF7070),
        SPAWN_B("spawn_b", "B队复活区域", 0xFF5599FF),
        SPAWN_C("spawn_c", "C队复活区域", 0xFF63D39A),
        SPAWN_D("spawn_d", "D队复活区域", 0xFFFFC857);

        private final String id;
        private final String displayName;
        private final int defaultColor;

        Type(String id, String displayName, int defaultColor) {
            this.id = id;
            this.displayName = displayName;
            this.defaultColor = defaultColor;
        }

        public String id() { return id; }
        public String displayName() { return displayName; }
        public int defaultColor() { return defaultColor; }

        public static Type parse(String value) {
            String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            for (Type type : values()) if (type.id.equals(normalized)) return type;
            return CUSTOM;
        }
    }

    public enum Appearance {
        ALWAYS("always", "始终显示"),
        NEARBY("nearby", "靠近显示"),
        EDITOR_ONLY("editor_only", "仅编辑器显示"),
        HIDDEN("hidden", "隐藏");

        private final String id;
        private final String displayName;

        Appearance(String id, String displayName) { this.id = id; this.displayName = displayName; }
        public String id() { return id; }
        public String displayName() { return displayName; }

        public static Appearance parse(String value) {
            String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            for (Appearance appearance : values()) if (appearance.id.equals(normalized)) return appearance;
            return ALWAYS;
        }
    }

    public enum Activation {
        ALWAYS("always", "始终生效"),
        MATCH_ONLY("match_only", "仅比赛中"),
        MODE("mode", "指定模式"),
        CONDITION("condition", "条件表达式");

        private final String id;
        private final String displayName;

        Activation(String id, String displayName) { this.id = id; this.displayName = displayName; }
        public String id() { return id; }
        public String displayName() { return displayName; }

        public static Activation parse(String value) {
            String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
            for (Activation activation : values()) if (activation.id.equals(normalized)) return activation;
            return ALWAYS;
        }
    }
}
