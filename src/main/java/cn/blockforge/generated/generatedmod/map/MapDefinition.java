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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 一张地图的 JSON 定义；建筑由世界本身提供，本类不生成建筑。 */
public final class MapDefinition {
    private final String id;
    private final String displayName;
    private final ResourceKey<Level> world;
    private final Region bounds;
    private final Region resetRegion;
    private final boolean boundsConfigured;
    private final boolean resetConfigured;
    private final boolean resetUsesBounds;
    private final List<SpawnPoint> teamASpawns;
    private final List<SpawnPoint> teamBSpawns;
    private final List<SpawnPoint> spectatorSpawns;
    private final List<SpawnPoint> teamCSpawns;
    private final List<SpawnPoint> teamDSpawns;
    private final List<MapRegion> customRegions;

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
        this(id, displayName, world, bounds, resetRegion, teamASpawns, teamBSpawns, spectatorSpawns,
                teamCSpawns, teamDSpawns, List.of());
    }

    public MapDefinition(String id, String displayName, ResourceKey<Level> world,
                         Region bounds, Region resetRegion, List<SpawnPoint> teamASpawns,
                         List<SpawnPoint> teamBSpawns, List<SpawnPoint> spectatorSpawns,
                         List<SpawnPoint> teamCSpawns, List<SpawnPoint> teamDSpawns,
                         List<MapRegion> customRegions) {
        this(id, displayName, world, bounds, resetRegion, teamASpawns, teamBSpawns, spectatorSpawns,
                teamCSpawns, teamDSpawns, customRegions, true, true);
    }

    private MapDefinition(String id, String displayName, ResourceKey<Level> world,
                          Region bounds, Region resetRegion, List<SpawnPoint> teamASpawns,
                          List<SpawnPoint> teamBSpawns, List<SpawnPoint> spectatorSpawns,
                          List<SpawnPoint> teamCSpawns, List<SpawnPoint> teamDSpawns,
                          List<MapRegion> customRegions, boolean boundsConfigured,
                          boolean resetConfigured) {
        this(id, displayName, world, bounds, resetRegion, teamASpawns, teamBSpawns, spectatorSpawns,
                teamCSpawns, teamDSpawns, customRegions, boundsConfigured, resetConfigured, false);
    }

    private MapDefinition(String id, String displayName, ResourceKey<Level> world,
                          Region bounds, Region resetRegion, List<SpawnPoint> teamASpawns,
                          List<SpawnPoint> teamBSpawns, List<SpawnPoint> spectatorSpawns,
                          List<SpawnPoint> teamCSpawns, List<SpawnPoint> teamDSpawns,
                          List<MapRegion> customRegions, boolean boundsConfigured,
                          boolean resetConfigured, boolean resetUsesBounds) {
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
        this.boundsConfigured = boundsConfigured;
        this.resetConfigured = resetConfigured;
        this.resetUsesBounds = resetUsesBounds;
        this.teamASpawns = copy(teamASpawns);
        this.teamBSpawns = copy(teamBSpawns);
        this.spectatorSpawns = copy(spectatorSpawns);
        this.teamCSpawns = copy(teamCSpawns);
        this.teamDSpawns = copy(teamDSpawns);
        this.customRegions = copyRegions(customRegions);
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
        return resetUsesBounds ? bounds : resetRegion;
    }

    public boolean hasBounds() { return boundsConfigured; }
    public boolean hasResetRegion() { return resetUsesBounds ? boundsConfigured : resetConfigured; }
    public boolean isComplete() { return hasBounds() && hasResetRegion(); }
    public boolean resetUsesBounds() { return resetUsesBounds; }
    public MapDefinition withResetUsesBounds(boolean enabled) {
        return new MapDefinition(id, displayName, world, bounds, resetRegion,
                teamASpawns, teamBSpawns, spectatorSpawns, teamCSpawns, teamDSpawns, customRegions,
                boundsConfigured, resetConfigured, enabled);
    }

    public static MapDefinition incomplete(String id, String displayName, ResourceKey<Level> world,
                                           Region placeholder) {
        return new MapDefinition(id, displayName, world, placeholder, placeholder,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), false, false);
    }

    public MapDefinition copyAs(String newId) {
        return new MapDefinition(newId, displayName, world, bounds, resetRegion,
                teamASpawns, teamBSpawns, spectatorSpawns, teamCSpawns, teamDSpawns, customRegions,
                boundsConfigured, resetConfigured, resetUsesBounds);
    }

    public List<MapRegion> customRegions() {
        return customRegions;
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
                team == Team.TEAM_C ? points : teamCSpawns, team == Team.TEAM_D ? points : teamDSpawns,
                customRegions, boundsConfigured, resetConfigured, resetUsesBounds);
    }

    public MapDefinition withRegions(Region updatedBounds, Region updatedResetRegion) {
        return new MapDefinition(id, displayName, world, updatedBounds, updatedResetRegion,
                teamASpawns, teamBSpawns, spectatorSpawns, teamCSpawns, teamDSpawns, customRegions,
                true, true, resetUsesBounds);
    }

    public List<MapRegion> regions() {
        List<MapRegion> result = new ArrayList<>();
        if (boundsConfigured) result.add(MapRegion.builtIn("bounds", resetUsesBounds ? "地图边界（兼作重置区）" : "地图边界", MapRegion.Type.BOUNDS, bounds,
                MapRegion.Type.BOUNDS.defaultColor()));
        if (resetConfigured && !resetUsesBounds) result.add(MapRegion.builtIn("reset", "重置区域", MapRegion.Type.RESET, resetRegion,
                MapRegion.Type.RESET.defaultColor()));
        result.addAll(customRegions);
        return result;
    }

    public MapRegion region(String id) {
        String normalized = MapRegion.normalizeId(id);
        for (MapRegion region : regions()) if (region.id().equals(normalized)) return region;
        return null;
    }

    public MapDefinition withRegion(MapRegion updated) {
        if (updated == null) return this;
        if ("bounds".equals(updated.id()) && updated.type() == MapRegion.Type.BOUNDS) {
            return new MapDefinition(id, displayName, world, updated.region(), resetRegion,
                    teamASpawns, teamBSpawns, spectatorSpawns, teamCSpawns, teamDSpawns, customRegions,
                    true, resetConfigured, resetUsesBounds);
        }
        if ("reset".equals(updated.id()) && updated.type() == MapRegion.Type.RESET) {
            return new MapDefinition(id, displayName, world, bounds, updated.region(),
                    teamASpawns, teamBSpawns, spectatorSpawns, teamCSpawns, teamDSpawns, customRegions,
                    boundsConfigured, true, false);
        }
        List<MapRegion> next = new ArrayList<>();
        boolean replaced = false;
        for (MapRegion region : customRegions) {
            if (region.id().equals(updated.id())) {
                next.add(updated);
                replaced = true;
            } else {
                next.add(region);
            }
        }
        if (!replaced) next.add(updated);
        return new MapDefinition(id, displayName, world, bounds, resetRegion,
                teamASpawns, teamBSpawns, spectatorSpawns, teamCSpawns, teamDSpawns, next,
                boundsConfigured, resetConfigured, resetUsesBounds);
    }

    public MapDefinition withoutRegion(String regionId) {
        String normalized = MapRegion.normalizeId(regionId);
        if ("bounds".equals(normalized)) return new MapDefinition(id, displayName, world, bounds, resetRegion,
                teamASpawns, teamBSpawns, spectatorSpawns, teamCSpawns, teamDSpawns, customRegions,
                false, resetConfigured, resetUsesBounds);
        if ("reset".equals(normalized)) return new MapDefinition(id, displayName, world, bounds, resetRegion,
                teamASpawns, teamBSpawns, spectatorSpawns, teamCSpawns, teamDSpawns, customRegions,
                boundsConfigured, false, false);
        List<MapRegion> next = new ArrayList<>();
        for (MapRegion region : customRegions) if (!region.id().equals(normalized)) next.add(region);
        return new MapDefinition(id, displayName, world, bounds, resetRegion,
                teamASpawns, teamBSpawns, spectatorSpawns, teamCSpawns, teamDSpawns, next,
                boundsConfigured, resetConfigured, resetUsesBounds);
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
                && resetRegion.equals(other.resetRegion)
                && boundsConfigured == other.boundsConfigured
                && resetConfigured == other.resetConfigured
                && resetUsesBounds == other.resetUsesBounds
                && customRegions.equals(other.customRegions);
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
        object.addProperty("boundsConfigured", boundsConfigured);
        object.addProperty("resetConfigured", resetConfigured);
        object.addProperty("resetUsesBounds", resetUsesBounds);
        JsonObject spawns = new JsonObject();
        spawns.add("teamA", spawnArray(teamASpawns));
        spawns.add("teamB", spawnArray(teamBSpawns));
        spawns.add("teamC", spawnArray(teamCSpawns));
        spawns.add("teamD", spawnArray(teamDSpawns));
        spawns.add("spectator", spawnArray(spectatorSpawns));
        object.add("spawns", spawns);
        JsonArray regions = new JsonArray();
        for (MapRegion region : customRegions) regions.add(region.toJson());
        object.add("regions", regions);
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
        boolean hasBounds = !object.has("boundsConfigured") || object.get("boundsConfigured").getAsBoolean();
        boolean hasReset = !object.has("resetConfigured") || object.get("resetConfigured").getAsBoolean();
        return new MapDefinition(id, displayName, world, bounds, resetRegion, teamA, teamB, spectator,
                spawnList(spawnObject, world, "teamC", "team_c"), spawnList(spawnObject, world, "teamD", "team_d"),
                parseRegions(object.get("regions")), hasBounds, hasReset,
                object.has("resetUsesBounds") && object.get("resetUsesBounds").getAsBoolean());
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

    private static List<MapRegion> copyRegions(List<MapRegion> regions) {
        List<MapRegion> source = regions == null ? List.of() : regions;
        Set<String> usedIds = new HashSet<>();
        usedIds.add("bounds");
        usedIds.add("reset");
        List<MapRegion> normalized = new ArrayList<>(source.size());
        for (MapRegion region : source) {
            if (region == null) {
                continue;
            }
            String id = MapRegion.uniqueId(region.id(), region.type().id(), usedIds);
            MapRegion normalizedRegion = id.equals(region.id()) ? region : region.withId(id);
            usedIds.add(normalizedRegion.id());
            normalized.add(normalizedRegion);
        }
        return List.copyOf(normalized);
    }

    private static List<MapRegion> parseRegions(JsonElement element) {
        if (element == null || !element.isJsonArray()) return List.of();
        List<MapRegion> regions = new ArrayList<>();
        for (JsonElement child : element.getAsJsonArray()) {
            MapRegion region = MapRegion.fromJson(child);
            if (region != null && !region.id().isBlank()) regions.add(region);
        }
        return regions;
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
        if (object.has("parts")) {
            var children = object.getAsJsonArray("parts");
            if (children.isEmpty() || children.size() > 1024) throw new IllegalArgumentException("Invalid region parts");
            List<Region> parts = new ArrayList<>();
            for (var child : children) {
                if (!child.isJsonObject() || child.getAsJsonObject().has("parts")) throw new IllegalArgumentException("Nested region parts");
                Region part = region(child);
                if (part == null) throw new IllegalArgumentException("Invalid region part");
                parts.add(part);
            }
            return Region.composite(parts);
        }
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

    public static Region regionFromJson(JsonElement element) {
        return region(element);
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

    public record Region(BlockPos min, BlockPos max, List<Region> parts) {
        public Region(BlockPos min, BlockPos max) { this(min, max, List.of()); }
        public Region {
            parts = parts == null ? List.of() : List.copyOf(parts);
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
            if (!parts.isEmpty()) return parts.stream().anyMatch(part -> part.contains(x, y, z));
            return x >= min.getX() && x < (double) max.getX() + 1.0D
                    && y >= min.getY() && y < (double) max.getY() + 1.0D
                    && z >= min.getZ() && z < (double) max.getZ() + 1.0D;
        }

        public boolean contains(BlockPos pos) {
            if (!parts.isEmpty()) return parts.stream().anyMatch(part -> part.contains(pos));
            return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                    && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                    && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
        }

        public JsonObject toJson() {
            JsonObject object = new JsonObject();
            if (!parts.isEmpty()) {
                JsonArray array = new JsonArray();
                parts.forEach(part -> array.add(part.toJson()));
                object.add("parts", array);
            }
            object.addProperty("minX", min.getX());
            object.addProperty("minY", min.getY());
            object.addProperty("minZ", min.getZ());
            object.addProperty("maxX", max.getX());
            object.addProperty("maxY", max.getY());
            object.addProperty("maxZ", max.getZ());
            return object;
        }

        public List<Region> boxes() { return parts.isEmpty() ? List.of(this) : parts; }

        public static Region composite(List<Region> boxes) {
            if (boxes.isEmpty()) return null;
            if (boxes.size() == 1) return boxes.get(0);
            return new Region(new BlockPos(boxes.stream().mapToInt(b -> b.min().getX()).min().orElseThrow(),
                    boxes.stream().mapToInt(b -> b.min().getY()).min().orElseThrow(), boxes.stream().mapToInt(b -> b.min().getZ()).min().orElseThrow()),
                    new BlockPos(boxes.stream().mapToInt(b -> b.max().getX()).max().orElseThrow(),
                            boxes.stream().mapToInt(b -> b.max().getY()).max().orElseThrow(), boxes.stream().mapToInt(b -> b.max().getZ()).max().orElseThrow()), boxes);
        }

        /** Split only the touched box; never materialize every voxel of a large map. */
        public Region withBlock(BlockPos pos, boolean include) {
            if (contains(pos) == include) return this;
            List<Region> boxes = new ArrayList<>();
            for (Region box : boxes()) {
                if (include || !box.contains(pos)) { boxes.add(box); continue; }
                int x = pos.getX(), y = pos.getY(), z = pos.getZ();
                addBox(boxes, box.min().getX(), box.min().getY(), box.min().getZ(), x - 1, box.max().getY(), box.max().getZ());
                addBox(boxes, x + 1, box.min().getY(), box.min().getZ(), box.max().getX(), box.max().getY(), box.max().getZ());
                addBox(boxes, x, box.min().getY(), box.min().getZ(), x, y - 1, box.max().getZ());
                addBox(boxes, x, y + 1, box.min().getZ(), x, box.max().getY(), box.max().getZ());
                addBox(boxes, x, y, box.min().getZ(), x, y, z - 1);
                addBox(boxes, x, y, z + 1, x, y, box.max().getZ());
            }
            if (include) boxes.add(new Region(pos, pos));
            if (boxes.size() > 1024) throw new IllegalArgumentException("区域过于复杂，请拆分为多个区域。");
            return composite(boxes);
        }
        private static void addBox(List<Region> boxes, int x, int y, int z, int xx, int yy, int zz) {
            if (x <= xx && y <= yy && z <= zz) boxes.add(new Region(new BlockPos(x, y, z), new BlockPos(xx, yy, zz)));
        }

        @Override
        public String toString() {
            return "[" + min.getX() + ", " + min.getY() + ", " + min.getZ() + "] 到 ["
                    + max.getX() + ", " + max.getY() + ", " + max.getZ() + "]"
                    + (parts.isEmpty() ? "" : "（不规则区域，" + parts.size() + " 片段）");
        }
    }
}
