package cn.blockforge.generated.generatedmod.map;

import cn.blockforge.generated.generatedmod.match.Team;
import cn.blockforge.generated.generatedmod.spawn.SpawnPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/** 地图生命周期管理器，不处理武器、伤害、击杀或 TDM 计分。 */
public final class MapManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("generated_mod_map_manager");

    private final MinecraftServer server;
    private final MapRegistry registry;
    private MapDefinition current;
    private MapResetManager resetManager;

    public MapManager(MinecraftServer server) {
        this.server = server;
        this.registry = new MapRegistry(server);
        registry.reload();
    }

    public MinecraftServer server() {
        return server;
    }

    public MapRegistry registry() {
        return registry;
    }

    public Optional<MapDefinition> currentMap() {
        return Optional.ofNullable(current);
    }

    public String currentMapId() {
        return current == null ? "未选择" : current.id();
    }

    public MapResetManager resetManager() {
        return resetManager;
    }

    public void tick() {
        if (resetManager != null) {
            resetManager.tick();
        }
    }

    public void shutdown() {
        if (resetManager != null) {
            resetManager.close();
        }
    }

    /** 在游戏内创建一张以管理员当前位置为中心的当前比赛地图（管理员命令/旧流程用）。 */
    public CreateResult createMap(ServerPlayer creator, String rawId, String rawDisplayName) {
        String id = MapDefinition.normalizeId(rawId);
        if (MapDefinition.creationIdError(rawId) != null) {
            return CreateResult.INVALID_ID;
        }
        if (registry.get(id).isPresent() || registry.definitionFileExists(id)) {
            return CreateResult.ALREADY_EXISTS;
        }
        if (resetManager != null && (resetManager.isCapturing() || resetManager.isResetting())) {
            return CreateResult.BUSY;
        }
        if (creator == null || creator.serverLevel() == null) {
            return CreateResult.INVALID_POSITION;
        }

        ServerLevel level = creator.serverLevel();
        BlockPos center = creator.blockPosition();
        int maximum = cn.blockforge.generated.generatedmod.config.FpsTdmConfig.COMMON.maxSnapshotBlocks.get();
        MapDefinition.Region region = defaultRegion(level, center, maximum);
        if (region.volume() <= 0L || region.volume() > maximum
                || !level.isInWorldBounds(region.min()) || !level.isInWorldBounds(region.max())) {
            return CreateResult.INVALID_POSITION;
        }

        int xOffset = Math.min(5, Math.max(0,
                (region.max().getX() - region.min().getX()) / 3));
        int spawnY = Math.max(region.min().getY(), Math.min(region.max().getY(), center.getY()));
        int teamAX = Math.max(region.min().getX(), center.getX() - xOffset);
        int teamBX = Math.min(region.max().getX(), center.getX() + xOffset);
        String displayName = sanitizeDisplayName(rawDisplayName, id);
        List<SpawnPoint> teamA = List.of(new SpawnPoint(level.dimension(), teamAX + 0.5D, spawnY,
                center.getZ() + 0.5D, 0.0F, 0.0F));
        List<SpawnPoint> teamB = List.of(new SpawnPoint(level.dimension(), teamBX + 0.5D, spawnY,
                center.getZ() + 0.5D, 180.0F, 0.0F));
        List<SpawnPoint> spectator = List.of(new SpawnPoint(level.dimension(), center.getX() + 0.5D, spawnY,
                center.getZ() + 0.5D, 180.0F, 0.0F));

        MapDefinition definition;
        try {
            definition = MapDefinition.incomplete(id, displayName, level.dimension(),
                    new MapDefinition.Region(center, center));
        } catch (RuntimeException error) {
            LOGGER.warn("构造新地图 {} 失败", id, error);
            return CreateResult.INVALID_POSITION;
        }

        if (!registerMap(definition)) {
            return registry.get(id).isPresent() ? CreateResult.ALREADY_EXISTS : CreateResult.IO_ERROR;
        }

        return CreateResult.CREATED;
    }

    public LoadResult loadMap(String id) {
        Optional<MapDefinition> found = registry.get(id);
        if (found.isEmpty()) {
            return LoadResult.NOT_FOUND;
        }
        if (resetManager != null && resetManager.isResetting()) {
            return LoadResult.BUSY;
        }
        if (!found.get().isComplete()) return LoadResult.INCOMPLETE;
        MapDefinition definition = sanitizeSpawns(found.get());
        if (current != null && current.id().equals(definition.id())) {
            resetManager.updateDefinition(definition);
            current = definition;
            if (resetManager.isCapturing()) {
                return LoadResult.LOADING;
            }
            if (resetManager.isReady()) {
                return LoadResult.ALREADY_CURRENT;
            }
            return resetManager.capture() ? LoadResult.STARTED : LoadResult.FAILED;
        }
        if (resetManager != null) {
            resetManager.close();
        }
        current = definition;
        resetManager = new MapResetManager(server, definition, registry.snapshotFile(definition.id()));
        if (resetManager.isReady()) {
            return LoadResult.ALREADY_CURRENT;
        }
        if (!resetManager.capture()) {
            LOGGER.warn("地图 {} 已选择，但初始快照未能开始：{}", definition.id(), resetManager.lastError());
            return LoadResult.FAILED;
        }
        return LoadResult.STARTED;
    }

    public int reloadMaps() {
        if (resetManager != null && resetManager.isResetting()) {
            return registry.definitions().size();
        }
        MapDefinition previousDefinition = current;
        String previous = current == null ? null : current.id();
        int count = registry.reload();
        MapDefinition reloaded = previous == null ? null : sanitizeSpawns(registry.get(previous).orElse(null));
        current = previousDefinition != null && previousDefinition.sameConfiguration(reloaded)
                ? previousDefinition : reloaded;
        if (current == null) {
            if (resetManager != null) {
                resetManager.close();
                resetManager = null;
            }
            return count;
        }
        if (resetManager == null) {
            resetManager = new MapResetManager(server, current, registry.snapshotFile(current.id()));
        } else {
            resetManager.updateDefinition(current);
        }
        if (!resetManager.isReady() && !resetManager.isCapturing()) {
            resetManager.capture();
        }
        return count;
    }

    public boolean isReady() {
        return resetManager != null && resetManager.isReady();
    }

    public boolean isLoading() {
        return resetManager != null && resetManager.isCapturing();
    }

    public boolean isResetting() {
        return resetManager != null && resetManager.isResetting();
    }

    public boolean isResetComplete() {
        return resetManager != null && resetManager.isResetComplete();
    }

    public boolean isResetFailed() {
        return resetManager != null && resetManager.isResetFailed();
    }

    public boolean beginReset() {
        return resetManager != null && resetManager.reset();
    }

    /** 注册一张新地图定义，不改变比赛用的当前地图、不启动快照捕获。 */
    public boolean registerMap(MapDefinition definition) {
        if (definition == null) {
            return false;
        }
        registry.deleteSnapshot(definition.id());
        return registry.create(definition) == MapRegistry.CreateResult.CREATED;
    }

    /**
     * 保存一份地图定义：目标恰好是比赛当前地图时同步运行时状态；
     * 否则只落盘（区域变化由调用方删旧快照，下次载入时重新捕获）。
     */
    public boolean saveDefinition(MapDefinition updated) {
        if (updated == null) {
            return false;
        }
        if (updated.regions().stream().mapToInt(region -> region.region().boxes().size()).sum() > 8192) return false;
        if (updated.hasResetRegion() && updated.resetRegion().volume() >
                cn.blockforge.generated.generatedmod.config.FpsTdmConfig.COMMON.maxSnapshotBlocks.get()) return false;
        if (!registry.save(updated)) {
            return false;
        }
        if (current != null && current.id().equals(updated.id())) {
            current = updated;
            if (resetManager != null) {
                resetManager.updateDefinition(updated);
            }
        }
        return true;
    }

    /** 删除地图：若它正是当前比赛地图，先释放运行时快照状态。 */
    public boolean forgetMap(String mapId) {
        String normalized = MapDefinition.normalizeId(mapId);
        if (current != null && current.id().equals(normalized)) {
            if (resetManager != null) {
                resetManager.close();
                resetManager = null;
            }
            current = null;
        }
        return registry.delete(normalized);
    }

    /** 供编辑器在玩家当前位置生成初始区域。 */
    public MapDefinition.Region initialRegion(ServerLevel level, BlockPos center) {
        return defaultRegion(level, center,
                cn.blockforge.generated.generatedmod.config.FpsTdmConfig.COMMON.maxSnapshotBlocks.get());
    }

    /** 出生点位置是否可安全站立（维度已加载、脚部与头部无碰撞）。 */
    public boolean isSpawnSafe(SpawnPoint point) {
        return isSafeSpawnLocation(point);
    }

    public boolean isInsideMap(ServerPlayer player) {
        if (current == null || !current.world().equals(player.serverLevel().dimension())) {
            return false;
        }
        MapDefinition.Region bounds = current.bounds();
        // 玩家站在边界顶层方块表面上时脚部坐标为 max.y + 1，属于合法站位。
        return player.getX() >= bounds.min().getX()
                && player.getX() < bounds.max().getX() + 1.0D
                && player.getY() >= bounds.min().getY()
                && player.getY() < bounds.max().getY() + 2.0D
                && player.getZ() >= bounds.min().getZ()
                && player.getZ() < bounds.max().getZ() + 1.0D;
    }

    public boolean isMapDimension(SpawnPoint point) {
        return current != null && current.world().equals(point.dimension());
    }
    public boolean isValidTeamSpawn(SpawnPoint point) {
        return current != null && isValidSpawnFor(current, point);
    }

    public boolean isValidSpectatorSpawn(SpawnPoint point) {
        return isValidSpectatorSpawnFor(current, point);
    }

    /** 针对指定的地图定义校验出生点；区域保存时用它对照新边界而不是旧边界。 */
    public boolean isValidSpawnFor(MapDefinition definition, SpawnPoint point) {
        if (definition == null || !definition.world().equals(point.dimension())
                || !definition.bounds().contains(point.x(), point.y(), point.z())) {
            return false;
        }
        return isSafeSpawnLocation(point);
    }

    /** 出生点是否落在地图边界和维度内（不做方块安全性检查）。 */
    public static boolean isSpawnInsideBounds(MapDefinition definition, SpawnPoint point) {
        return definition != null && definition.world().equals(point.dimension())
                && definition.bounds().contains(point.x(), point.y(), point.z());
    }

    /** 观战点只要求与地图同维度；允许放在边界外、高处或空中。 */
    public static boolean isValidSpectatorSpawnFor(MapDefinition definition, SpawnPoint point) {
        return definition != null && definition.world().equals(point.dimension());
    }

    /**
     * 加载地图时清理历史遗留的越界出生点（旧版本区域保存未过滤导致），
     * 有变动时直接写回地图定义，避免坏数据反复回到运行时。
     */
    private MapDefinition sanitizeSpawns(MapDefinition definition) {
        if (definition == null || !definition.isComplete()) {
            return definition;
        }
        List<SpawnPoint> teamA = definition.teamASpawns().stream()
                .filter(point -> isSpawnInsideBounds(definition, point)).toList();
        List<SpawnPoint> teamB = definition.teamBSpawns().stream()
                .filter(point -> isSpawnInsideBounds(definition, point)).toList();
        List<SpawnPoint> spectator = definition.spectatorSpawns().stream()
                .filter(point -> isValidSpectatorSpawnFor(definition, point)).toList();
        MapDefinition cleaned = definition.withTeamSpawns(Team.TEAM_A, teamA)
                .withTeamSpawns(Team.TEAM_B, teamB).withTeamSpawns(Team.SPECTATOR, spectator)
                .withTeamSpawns(Team.TEAM_C, definition.spawns(Team.TEAM_C).stream().filter(point -> isSpawnInsideBounds(definition, point)).toList())
                .withTeamSpawns(Team.TEAM_D, definition.spawns(Team.TEAM_D).stream().filter(point -> isSpawnInsideBounds(definition, point)).toList());
        if (cleaned.sameConfiguration(definition)) {
            return definition;
        }
        LOGGER.warn("地图 {} 定义中存在越界出生点，加载时已移除 {} 个",
                definition.id(),
                (definition.teamASpawns().size() - teamA.size())
                        + (definition.teamBSpawns().size() - teamB.size())
                        + (definition.spectatorSpawns().size() - spectator.size())
                        + definition.spawns(Team.TEAM_C).size() - cleaned.spawns(Team.TEAM_C).size()
                        + definition.spawns(Team.TEAM_D).size() - cleaned.spawns(Team.TEAM_D).size());
        if (!registry.save(cleaned)) {
            LOGGER.warn("地图 {} 的出生点清理结果写回失败，本次运行仍按清理后的定义处理", definition.id());
        }
        return cleaned;
    }

    private boolean isSafeSpawnLocation(SpawnPoint point) {
        ServerLevel level = server.getLevel(point.dimension());
        if (level == null) {
            return false;
        }
        BlockPos feet = point.blockPosition();
        AABB playerSpace = new AABB(feet.getX() + 0.08D, feet.getY(), feet.getZ() + 0.08D,
                feet.getX() + 0.92D, feet.getY() + 1.81D, feet.getZ() + 0.92D);
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                && level.noCollision(null, playerSpace);
    }

    public boolean isInResetRegion(ServerLevel level, BlockPos pos) {
        return current != null && current.world().equals(level.dimension())
                && current.resetRegion().contains(pos);
    }

    public boolean isInMapBounds(ServerLevel level, BlockPos pos) {
        return current != null && current.world().equals(level.dimension())
                && current.bounds().contains(pos);
    }

    public boolean updateTeamSpawns(Team team, List<SpawnPoint> points) {
        if (current == null || team == Team.SPECTATOR) {
            return false;
        }
        MapDefinition updated = current.withTeamSpawns(team, points);
        if (!registry.save(updated)) {
            return false;
        }
        current = updated;
        if (resetManager != null) {
            resetManager.updateDefinition(updated);
        }
        return true;
    }

    public boolean updateSpectatorSpawns(List<SpawnPoint> points) {
        if (current == null) {
            return false;
        }
        MapDefinition updated = current.withTeamSpawns(Team.SPECTATOR, points);
        if (!registry.save(updated)) {
            return false;
        }
        current = updated;
        if (resetManager != null) {
            resetManager.updateDefinition(updated);
        }
        return true;
    }

    /**
     * 保存地图边界和重置区域，并让新区域重新生成初始快照。
     * 返回值为因新边界而移除的出生点数量；-1 表示保存失败。
     */
    public int updateRegions(MapDefinition.Region bounds, MapDefinition.Region resetRegion) {
        return updateRegionsFor(current, bounds, resetRegion);
    }

    /** 针对任意一张已注册地图保存区域；目标不是比赛当前地图时只作废旧快照。 */
    public int updateRegionsFor(MapDefinition target, MapDefinition.Region bounds,
                                MapDefinition.Region resetRegion) {
        if (target == null || bounds == null || resetRegion == null) {
            return -1;
        }
        long volume = resetRegion.volume();
        int maximum = cn.blockforge.generated.generatedmod.config.FpsTdmConfig.COMMON.maxSnapshotBlocks.get();
        if (volume <= 0L || volume > maximum || volume > Integer.MAX_VALUE) {
            return -1;
        }
        MapDefinition candidate = target.withRegions(bounds, resetRegion);
        // 队伍出生点必须对照“新”边界过滤；观战点允许在边界外，只校验维度。
        List<SpawnPoint> teamA = candidate.teamASpawns().stream()
                .filter(point -> isValidSpawnFor(candidate, point)).toList();
        List<SpawnPoint> teamB = candidate.teamBSpawns().stream()
                .filter(point -> isValidSpawnFor(candidate, point)).toList();
        List<SpawnPoint> spectator = candidate.spectatorSpawns().stream()
                .filter(point -> isValidSpectatorSpawnFor(candidate, point)).toList();
        int removed = (candidate.teamASpawns().size() - teamA.size())
                + (candidate.teamBSpawns().size() - teamB.size())
                + (candidate.spectatorSpawns().size() - spectator.size());
        MapDefinition updated = candidate.withTeamSpawns(Team.TEAM_A, teamA)
                .withTeamSpawns(Team.TEAM_B, teamB).withTeamSpawns(Team.SPECTATOR, spectator)
                .withTeamSpawns(Team.TEAM_C, candidate.spawns(Team.TEAM_C).stream().filter(point -> isValidSpawnFor(candidate, point)).toList())
                .withTeamSpawns(Team.TEAM_D, candidate.spawns(Team.TEAM_D).stream().filter(point -> isValidSpawnFor(candidate, point)).toList());
        removed += candidate.spawns(Team.TEAM_C).size() - updated.spawns(Team.TEAM_C).size()
                + candidate.spawns(Team.TEAM_D).size() - updated.spawns(Team.TEAM_D).size();
        if (!saveDefinition(updated)) {
            return -1;
        }
        if (removed > 0) {
            LOGGER.info("地图 {} 区域更新后移除了 {} 个越界或不安全的出生点", updated.id(), removed);
        }
        boolean isCurrent = current != null && current.id().equals(updated.id());
        if (isCurrent) {
            if (resetManager != null && !resetManager.isReady() && !resetManager.isCapturing()
                    && !resetManager.isResetting()) {
                resetManager.capture();
            }
        } else {
            // 非当前地图：直接作废快照文件，下一次载入时重新捕获。
            registry.deleteSnapshot(updated.id());
        }
        return removed;
    }

    public String statusLine() {
        if (current == null) {
            return "当前地图：未选择；已注册 " + registry.definitions().size() + " 张";
        }
        if (!current.isComplete()) {
            String missing = !current.hasBounds() && !current.hasResetRegion() ? "地图边界、重置区域"
                    : !current.hasBounds() ? "地图边界" : "重置区域";
            return current.id() + "（" + current.displayName() + "），未完成：缺少" + missing;
        }
        String reset = resetManager == null ? "无快照" : resetManager.isReady() ? "快照就绪"
                : resetManager.isCapturing() ? "快照捕获 " + progress(resetManager.processedBlocks(), resetManager.totalBlocks())
                : "不可用：" + resetManager.lastError();
        return current.id() + "（" + current.displayName() + "），维度="
                + current.world().location() + "，边界=" + current.bounds() + "，重置区域="
                + current.resetRegion() + "，快照=" + reset;
    }

    public String resetProgress() {
        if (resetManager == null) {
            return "无地图";
        }
        return progress(resetManager.processedBlocks(), resetManager.totalBlocks());
    }

    private static MapDefinition.Region defaultRegion(ServerLevel level, BlockPos center, int maximumBlocks) {
        int allowed = Math.max(1, maximumBlocks);
        int worldHeight = Math.max(1, level.dimensionType().height());
        int ySpan = Math.min(Math.min(8, worldHeight), allowed);
        int side = Math.max(1, Math.min(17, (int) Math.floor(Math.sqrt((double) allowed / ySpan))));
        ySpan = Math.max(1, Math.min(ySpan, allowed / (side * side)));

        int worldMinY = level.dimensionType().minY();
        int worldMaxY = worldMinY + worldHeight - 1;
        int minY = Math.max(worldMinY, Math.min(center.getY() - 2, worldMaxY - ySpan + 1));
        int maxY = minY + ySpan - 1;
        int minX = horizontalMinimum(level, center, side, true, minY);
        int minZ = horizontalMinimum(level, center, side, false, minY);
        return new MapDefinition.Region(new BlockPos(minX, minY, minZ),
                new BlockPos(minX + side - 1, maxY, minZ + side - 1));
    }

    private static int horizontalMinimum(ServerLevel level, BlockPos center, int span,
                                         boolean xAxis, int validY) {
        int coordinate = xAxis ? center.getX() : center.getZ();
        int minimum = coordinate - (span - 1) / 2;
        int maximum = minimum + span - 1;
        BlockPos minimumPos = xAxis
                ? new BlockPos(minimum, validY, center.getZ())
                : new BlockPos(center.getX(), validY, minimum);
        if (!level.isInWorldBounds(minimumPos)) {
            minimum = coordinate;
            maximum = minimum + span - 1;
        }
        BlockPos maximumPos = xAxis
                ? new BlockPos(maximum, validY, center.getZ())
                : new BlockPos(center.getX(), validY, maximum);
        if (!level.isInWorldBounds(maximumPos)) {
            minimum = coordinate - span + 1;
        }
        return minimum;
    }

    private static String sanitizeDisplayName(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String cleaned = value.trim().replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
        if (cleaned.isBlank()) {
            return fallback;
        }
        return cleaned.substring(0, Math.min(48, cleaned.length()));
    }

    private static String progress(long done, long total) {
        if (total <= 0L) {
            return "0/0";
        }
        return done + "/" + total;
    }

    public enum CreateResult {
        CREATED,
        CREATED_NOT_READY,
        INVALID_ID,
        ALREADY_EXISTS,
        BUSY,
        INVALID_POSITION,
        IO_ERROR
    }

    public enum LoadResult {
        STARTED,
        ALREADY_CURRENT,
        LOADING,
        NOT_FOUND,
        INCOMPLETE,
        BUSY,
        FAILED
    }
}
