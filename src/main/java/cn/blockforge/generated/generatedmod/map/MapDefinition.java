package cn.blockforge.generated.generatedmod.map;

import cn.blockforge.generated.generatedmod.match.Team;
import cn.blockforge.generated.generatedmod.spawn.SpawnPoint;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 一张地图的 JSON 定义；建筑由世界本身提供，本类不生成建筑。 */
public final class MapDefinition {
    private final String id;
    private final String displayName;
    private final ResourceKey<Level> world;
    private final Region bounds;
    private final Region resetRegion;
    private final List<SpawnPoint> teamASpawns;
    private final List<SpawnPoint> teamBSpawns;
    private final List<SpawnPoint> spectatorSpawns;
    private final List<SpawnPoint> teamCSpawns;
    private final List<SpawnPoint> teamDSpawns;

    public MapDefinition(String id, String displayName, ResourceKey<Level> world,
                         Region bounds, Region resetRegion,
                         List<SpawnPoint> teamASpawns, List<SpawnPoint> teamBSpawns,
                         List<SpawnPoint> spectatorSpawns) {
        this(id, displayName, world, bounds, resetRegion, teamASpawns, teamBSpawns,
                spectatorSpawns, List.of(), List.of());
    }

    public MapDefinition(String id, String displayName, ResourceKey<Level> world,
                         Region bounds, Region resetRegion, List<SpawnPoint> teamASpawns,
                         List<SpawnPoint> teamBSpawns, List<SpawnPoint> spectatorSpawns,
                         List<SpawnPoint> teamCSpawns, List<SpawnPoint> teamDSpawns) {
        this.id = normalizeId(id);
        if (!isValidId(this.id)) {
            throw new IllegalArgumentException("地图 id 不能为空，且只能使用字母、数字、点、下划线和短横线");
        }
        this.displayName = displayName == null || displayName.isBlank() ? this.id : displayName;
        this.world = world == null ? Level.OVERWORLD : world;
        if (resetRegion == null) {
            throw new IllegalArgumentException("地图必须配置 resetRegion");
        }
        this.resetRegion = resetRegion;
        this.bounds = bounds == null ? resetRegion : bounds;
        this.teamASpawns = copy(teamASpawns);
        this.teamBSpawns = copy(teamBSpawns);
        this.spectatorSpawns = copy(spectatorSpawns);
        this.teamCSpawns = copy(teamCSpawns);
        this.teamDSpawns = copy(teamDSpawns);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public ResourceKey<Level> world() {
        return world;
    }

    public Region bounds() {
        return bounds;
    }

    public Region resetRegion() {
        return resetRegion;
    }

    public List<SpawnPoint> teamASpawns() {
        return teamASpawns;
    }

    public List<SpawnPoint> teamBSpawns() {
        return teamBSpawns;
    }

    public List<SpawnPoint> spectatorSpawns() {
        return spectatorSpawns;
    }

    public List<SpawnPoint> spawns(Team team) {
        return switch (team) {
            case TEAM_A -> teamASpawns;
            case TEAM_B -> teamBSpawns;
            case TEAM_C -> teamCSpawns;
            case TEAM_D -> teamDSpawns;
            case SPECTATOR -> spectatorSpawns;
        };
    }

    public MapDefinition withTeamSpawns(Team team, List<SpawnPoint> points) {
        return new MapDefinition(id, displayName, world, bounds, resetRegion,
                team == Team.TEAM_A ? points : teamASpawns, team == Team.TEAM_B ? points : teamBSpawns,
                team == Team.SPECTATOR ? points : spectatorSpawns,
                team == Team.TEAM_C ? points : teamCSpawns, team == Team.TEAM_D ? points : teamDSpawns);
    }

    public MapDefinition withRegions(Region updatedBounds, Region updatedResetRegion) {
        return new MapDefinition(id, displayName, world, updatedBounds, updatedResetRegion,
                teamASpawns, teamBSpawns, spectatorSpawns, teamCSpawns, teamDSpawns);
    }

    /**
     * 比较地图定义中会影响区域草稿的内容，而不是只比较地图 id。
     * 出生点发生变化不会让区域草稿失效，因为草稿只负责边界和重置区域。
     */
    public boolean sameRegionConfiguration(MapDefinition other) {
        return other != null
                && id.equals(other.id)
                && displayName.equals(other.displayName)
                && world.equals(other.world)
                && bounds.equals(other.bounds)
                && resetRegion.equals(other.resetRegion);
    }

    /** 比较地图定义的完整内容，包含三组出生点。 */
    public boolean sameConfiguration(MapDefinition other) {
        return sameRegionConfiguration(other)
                && teamASpawns.equals(other.teamASpawns)
                && teamBSpawns.equals(other.teamBSpawns)
                && teamCSpawns.equals(other.teamCSpawns)
                && teamDSpawns.equals(other.teamDSpawns)
                && spectatorSpawns.equals(other.spectatorSpawns);
    }

    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("id", id);
        object.addProperty("displayName", displayName);
        object.addProperty("world", world.location().toString());
        object.add("bounds", bounds.toJson());
        object.add("resetRegion", resetRegion.toJson());
        JsonObject spawns = new JsonObject();
        spawns.add("teamA", spawnArray(teamASpawns));
        spawns.add("teamB", spawnArray(teamBSpawns));
        spawns.add("teamC", spawnArray(teamCSpawns));
        spawns.add("teamD", spawnArray(teamDSpawns));
        spawns.add("spectator", spawnArray(spectatorSpawns));
        object.add("spawns", spawns);
        return object;
    }

    public static MapDefinition fromJson(JsonObject object, String fallbackId) {
        String id = text(object, "id", fallbackId);
        String displayName = text(object, "displayName", id);
        ResourceKey<Level> world = SpawnPoint.parseDimension(object.get("world"), Level.OVERWORLD);
        Region resetRegion = region(object.get("resetRegion"));
        if (resetRegion == null) {
            resetRegion = region(object.get("reset_region"));
        }
        Region bounds = region(object.get("bounds"));
        if (resetRegion == null) {
            throw new IllegalArgumentException("缺少 resetRegion");
        }
        JsonObject spawnObject = object.has("spawns") && object.get("spawns").isJsonObject()
                ? object.getAsJsonObject("spawns") : object;
        List<SpawnPoint> teamA = spawnList(spawnObject, world,
                "teamA", "team_a", "teamASpawns", "teamA spawns", "team_a_spawns");
        List<SpawnPoint> teamB = spawnList(spawnObject, world,
                "teamB", "team_b", "teamBSpawns", "teamB spawns", "team_b_spawns");
        List<SpawnPoint> spectator = spawnList(spawnObject, world,
                "spectator", "spectatorSpawn", "spectatorSpawns", "spectator spawn", "spectator_spawns");
        return new MapDefinition(id, displayName, world, bounds, resetRegion, teamA, teamB, spectator,
                spawnList(spawnObject, world, "teamC", "team_c"), spawnList(spawnObject, world, "teamD", "team_d"));
    }

    public static String normalizeId(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /** 新建地图使用的文件名校验；读取旧配置仍由构造器保持兼容。 */
    public static String creationIdError(String value) {
        String id = normalizeId(value);
        if (id.isEmpty()) {
            return "地图 ID 不能为空。";
        }
        if (id.length() > 48) {
            return "地图 ID 最多 48 个字符。";
        }
        if (!isValidId(id) || ".".equals(id) || "..".equals(id)) {
            return "地图 ID 只能使用小写字母、数字、点、下划线和短横线。";
        }
        return null;
    }

    private static boolean isValidId(String value) {
        return !value.isEmpty() && value.matches("[a-z0-9._-]+");
    }

    private static List<SpawnPoint> copy(List<SpawnPoint> points) {
        return List.copyOf(points == null ? List.of() : points);
    }

    private static JsonArray spawnArray(List<SpawnPoint> points) {
        JsonArray array = new JsonArray();
        for (SpawnPoint point : points) {
            array.add(point.toJson());
        }
        return array;
    }

    private static List<SpawnPoint> spawnList(JsonObject object, ResourceKey<Level> world, String... names) {
        JsonElement element = null;
        for (String name : names) {
            if (object.has(name)) {
                element = object.get(name);
                break;
            }
        }
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        List<SpawnPoint> points = new ArrayList<>();
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                points.add(SpawnPoint.fromJson(child, world));
            }
        } else {
            points.add(SpawnPoint.fromJson(element, world));
        }
        return points;
    }

    private static Region region(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject object = element.getAsJsonObject();
        if (hasAll(object, "minX", "minY", "minZ", "maxX", "maxY", "maxZ")) {
            return new Region(
                    new BlockPos(object.get("minX").getAsInt(), object.get("minY").getAsInt(),
                            object.get("minZ").getAsInt()),
                    new BlockPos(object.get("maxX").getAsInt(), object.get("maxY").getAsInt(),
                            object.get("maxZ").getAsInt()));
        }
        if (object.has("min") && object.has("max")) {
            return new Region(position(object.getAsJsonObject("min")),
                    position(object.getAsJsonObject("max")));
        }
        return null;
    }

    private static BlockPos position(JsonObject object) {
        return new BlockPos(object.get("x").getAsInt(), object.get("y").getAsInt(), object.get("z").getAsInt());
    }

    private static boolean hasAll(JsonObject object, String... names) {
        for (String name : names) {
            if (!object.has(name)) {
                return false;
            }
        }
        return true;
    }

    private static String text(JsonObject object, String name, String fallback) {
        return object.has(name) && !object.get(name).isJsonNull()
                ? object.get(name).getAsString() : fallback;
    }

    public record Region(BlockPos min, BlockPos max) {
        public Region {
            int minX = Math.min(min.getX(), max.getX());
            int minY = Math.min(min.getY(), max.getY());
            int minZ = Math.min(min.getZ(), max.getZ());
            int maxX = Math.max(min.getX(), max.getX());
            int maxY = Math.max(min.getY(), max.getY());
            int maxZ = Math.max(min.getZ(), max.getZ());
            min = new BlockPos(minX, minY, minZ);
            max = new BlockPos(maxX, maxY, maxZ);
        }

        public long volume() {
            long xSpan = (long) max.getX() - min.getX() + 1L;
            long ySpan = (long) max.getY() - min.getY() + 1L;
            long zSpan = (long) max.getZ() - min.getZ() + 1L;
            if (xSpan <= 0L || ySpan <= 0L || zSpan <= 0L
                    || xSpan > Long.MAX_VALUE / ySpan
                    || xSpan * ySpan > Long.MAX_VALUE / zSpan) {
                return Long.MAX_VALUE;
            }
            return xSpan * ySpan * zSpan;
        }

        public boolean contains(double x, double y, double z) {
            return x >= min.getX() && x < (double) max.getX() + 1.0D
                    && y >= min.getY() && y < (double) max.getY() + 1.0D
                    && z >= min.getZ() && z < (double) max.getZ() + 1.0D;
        }

        public boolean contains(BlockPos pos) {
            return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                    && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                    && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
        }

        public JsonObject toJson() {
            JsonObject object = new JsonObject();
            object.addProperty("minX", min.getX());
            object.addProperty("minY", min.getY());
            object.addProperty("minZ", min.getZ());
            object.addProperty("maxX", max.getX());
            object.addProperty("maxY", max.getY());
            object.addProperty("maxZ", max.getZ());
            return object;
        }

        @Override
        public String toString() {
            return "[" + min.getX() + ", " + min.getY() + ", " + min.getZ() + "] 到 ["
                    + max.getX() + ", " + max.getY() + ", " + max.getZ() + "]";
        }
    }
}
