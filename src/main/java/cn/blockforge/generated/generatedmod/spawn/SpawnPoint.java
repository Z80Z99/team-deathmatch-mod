package cn.blockforge.generated.generatedmod.spawn;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** 地图配置中的一个出生点，保留精确位置、朝向和维度。 */
public record SpawnPoint(ResourceKey<Level> dimension, double x, double y, double z,
                         float yaw, float pitch) {
    public SpawnPoint {
        if (dimension == null) {
            dimension = Level.OVERWORLD;
        }
    }

    public SpawnPoint(ResourceKey<Level> dimension, BlockPos pos, float yaw) {
        this(dimension, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
    }

    public BlockPos blockPosition() {
        return BlockPos.containing(x, y, z);
    }

    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("world", dimension.location().toString());
        object.addProperty("x", x);
        object.addProperty("y", y);
        object.addProperty("z", z);
        object.addProperty("yaw", yaw);
        object.addProperty("pitch", pitch);
        return object;
    }

    public String description() {
        return dimension.location() + " [" + format(x) + ", " + format(y) + ", " + format(z)
                + "]，朝向 " + format(yaw) + "° / " + format(pitch) + "°";
    }

    public static SpawnPoint fromJson(JsonElement element, ResourceKey<Level> defaultDimension) {
        if (element == null || element.isJsonNull()) {
            throw new IllegalArgumentException("出生点不能为空");
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (array.size() < 3) {
                throw new IllegalArgumentException("出生点数组至少需要 x、y、z");
            }
            return new SpawnPoint(
                    defaultDimension,
                    array.get(0).getAsDouble(),
                    array.get(1).getAsDouble(),
                    array.get(2).getAsDouble(),
                    array.size() > 3 ? array.get(3).getAsFloat() : 0.0F,
                    array.size() > 4 ? array.get(4).getAsFloat() : 0.0F);
        }
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("出生点必须是对象或数组");
        }
        JsonObject object = element.getAsJsonObject();
        ResourceKey<Level> dimension = parseDimension(
                first(object, "world", "dimension"), defaultDimension);
        return new SpawnPoint(
                dimension,
                requiredNumber(object, "x"),
                requiredNumber(object, "y"),
                requiredNumber(object, "z"),
                number(object, 0.0F, "yaw", "rotation", "yRot"),
                number(object, 0.0F, "pitch", "xRot"));
    }

    public static ResourceKey<Level> parseDimension(JsonElement element, ResourceKey<Level> fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback == null ? Level.OVERWORLD : fallback;
        }
        String value = element.getAsString().trim().toLowerCase(java.util.Locale.ROOT);
        if (value.isEmpty() || "world".equals(value) || "overworld".equals(value)) {
            return Level.OVERWORLD;
        }
        if ("nether".equals(value) || "the_nether".equals(value)) {
            return Level.NETHER;
        }
        if ("end".equals(value) || "the_end".equals(value)) {
            return Level.END;
        }
        ResourceLocation location = ResourceLocation.tryParse(value.contains(":") ? value : "minecraft:" + value);
        if (location == null) {
            return fallback == null ? Level.OVERWORLD : fallback;
        }
        return ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, location);
    }

    private static JsonElement first(JsonObject object, String... names) {
        for (String name : names) {
            if (object.has(name)) {
                return object.get(name);
            }
        }
        return null;
    }

    private static double requiredNumber(JsonObject object, String name) {
        if (!object.has(name)) {
            throw new IllegalArgumentException("出生点缺少 " + name);
        }
        return object.get(name).getAsDouble();
    }

    private static float number(JsonObject object, float fallback, String... names) {
        JsonElement element = first(object, names);
        return element == null || element.isJsonNull() ? fallback : element.getAsFloat();
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
