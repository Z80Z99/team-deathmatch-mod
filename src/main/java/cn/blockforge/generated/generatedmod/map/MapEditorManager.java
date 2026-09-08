package cn.blockforge.generated.generatedmod.map;

import cn.blockforge.generated.generatedmod.match.MatchManager;
import cn.blockforge.generated.generatedmod.match.Team;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.MapEditorSyncPacket;
import cn.blockforge.generated.generatedmod.item.ModItems;
import cn.blockforge.generated.generatedmod.spawn.SpawnPoint;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 每个玩家独立的地图编辑器。
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>所有玩家都可以创建并编辑“自己的地图”；每张地图登记拥有者，非拥有者只能编辑自己的
 *       （服务器管理员可编辑任意地图作为运维兜底）；</li>
 *   <li>邀请码（6 位）把地图共享给别人：导入者获得一份归属自己、之后独立编辑的副本，
 *       原图不受影响；</li>
 *   <li>编辑目标按玩家会话维护，区域与出生点直接写回对应地图定义，
 *       只有当目标恰好是比赛当前地图时才同步运行时快照状态；</li>
 *   <li>比赛、房间开赛流程或快照任务进行中时统一锁定写操作。</li>
 * </ul>
 */
public final class MapEditorManager {
    private static final int ADMIN_PERMISSION = 2;
    private static final String DRAFT_INVALIDATED_MESSAGE =
            "地图定义已在服务器端更新，未保存的区域草稿已失效并重新载入，请核对后再编辑。";
    private final MinecraftServer server;
    private final MatchManager matchManager;
    private final MapManager maps;
    private final MapOwnershipStore ownership;
    private final Map<UUID, Draft> drafts = new HashMap<>();
    private final Map<UUID, String> editingMapIds = new HashMap<>();
    private final Set<UUID> invalidatedDrafts = new HashSet<>();
    private final Map<UUID, String> messages = new HashMap<>();
    private final Map<UUID, Boolean> errors = new HashMap<>();
    private final Map<UUID, MapTool> selectedTools = new HashMap<>();
    private final Map<UUID, MapBrushMode> brushModes = new HashMap<>();
    private final Map<UUID, BlockPos> brushFirstPoints = new HashMap<>();
    private final Map<UUID, BlockPos> brushSecondPoints = new HashMap<>();
    private final Map<UUID, String> selectedRegionIds = new HashMap<>();
    private final Map<UUID, Integer> brushRanges = new HashMap<>();

    public MapEditorManager(MinecraftServer server, MatchManager matchManager, MapManager maps) {
        this.server = server;
        this.matchManager = matchManager;
        this.maps = maps;
        this.ownership = new MapOwnershipStore(server);
        this.ownership.pruneTo(maps.registry().ids());
    }

    public MapOwnershipStore ownership() {
        return ownership;
    }

    // ------------------------------------------------------------------ tick

    public void tick() {
        if (server.getTickCount() % 10 != 0) {
            return;
        }
        for (UUID playerId : new ArrayList<>(drafts.keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                continue;
            }
            MapDefinition definition = targetDefinition(player);
            boolean wasInvalidated = invalidatedDrafts.contains(playerId);
            draftFor(playerId, definition);
            boolean newlyInvalidated = !wasInvalidated && invalidatedDrafts.contains(playerId);
            if (newlyInvalidated) {
                setMessage(player, DRAFT_INVALIDATED_MESSAGE, true);
            }
            if (newlyInvalidated) {
                sendView(player);
            }
        }
    }

    // -------------------------------------------------------------- actions

    public void handleAction(ServerPlayer player, MapEditorAction action, String mapId, String displayName,
                             int requestId) {
        if (player == null || action == null) {
            return;
        }
        UUID playerId = player.getUUID();
        switch (action) {
            case POLL -> {
                sendView(player, requestId);
                return;
            }
            case REQUEST -> {
                if (invalidatedDrafts.remove(playerId)) {
                    setMessage(player, "地图定义已更新，旧的未保存草稿已丢弃，当前状态已重新载入。", false);
                } else {
                    Draft draft = drafts.get(playerId);
                    setMessage(player, draft != null && draft.dirty
                            ? "状态已刷新，未保存的地图草稿已保留。"
                            : "地图编辑状态已刷新。", false);
                }
                sendView(player, requestId);
                return;
            }
            case CREATE_MAP -> {
                String lock = editLockMessage();
                if (lock != null) {
                    setMessage(player, lock, true);
                } else {
                    createOwnMap(player, displayName);
                }
                sendView(player, requestId);
                return;
            }
            case IMPORT_MAP -> {
                String lock = editLockMessage();
                if (lock != null) {
                    setMessage(player, lock, true);
                } else {
                    importByCode(player, mapId);
                }
                sendView(player, requestId);
                return;
            }
            case EDIT_MAP -> {
                selectTarget(player, mapId);
                sendView(player, requestId);
                return;
            }
            case DELETE_MAP -> {
                String lock = editLockMessage();
                if (lock != null) {
                    setMessage(player, lock, true);
                } else {
                    deleteMap(player, mapId);
                }
                sendView(player, requestId);
                return;
            }
            case GENERATE_SHARE -> {
                manageShare(player, mapId, true, requestId);
                return;
            }
            case REVOKE_SHARE -> {
                manageShare(player, mapId, false, requestId);
                return;
            }
            case SELECT_TOOL -> {
                MapTool tool = MapTool.parse(mapId);
                if (tool == null) {
                    setMessage(player, "未知的地形工具：" + mapId, true);
                } else {
                    selectTool(player, tool);
                }
                sendView(player, requestId);
                return;
            }
            case GIVE_TOOLS -> {
                giveTools(player);
                sendView(player, requestId);
                return;
            }
            case SET_BRUSH_RANGE -> {
                setBrushRange(player, parseRange(mapId));
                sendView(player, requestId);
                return;
            }
            case SET_BRUSH_MODE -> {
                MapBrushMode mode = MapBrushMode.parse(mapId);
                brushModes.put(playerId, mode);
                saveBrushPreferences(player);
                brushFirstPoints.remove(playerId);
                brushSecondPoints.remove(playerId);
                feedback(player, "画笔模式：" + mode.displayName(), false);
                sendView(player, requestId);
                return;
            }
            default -> { }
        }

        // 其余都是写操作：需要编辑目标、权限、未锁定、维度正确。
        MapDefinition target = targetDefinition(player);
        if (target == null) {
            setMessage(player, "你还没有地图：先新建一张，或向拥有者索要邀请码导入。", true);
            sendView(player, requestId);
            return;
        }
        if (!canManage(player, target.id())) {
            setMessage(player, "只能编辑自己的地图；可用拥有者的邀请码导入一份独立副本。", true);
            sendView(player, requestId);
            return;
        }
        if (invalidatedDrafts.contains(playerId)) {
            setMessage(player, DRAFT_INVALIDATED_MESSAGE, true);
            sendView(player, requestId);
            return;
        }
        String lock = editLockMessage();
        if (lock != null) {
            setMessage(player, lock, true);
            sendView(player, requestId);
            return;
        }
        if (isPositionAction(action) && !player.serverLevel().dimension().equals(target.world())) {
            setMessage(player, "请先传送到该地图所在维度，再设置当前位置。", true);
            sendView(player, requestId);
            return;
        }
        Draft draft = draftFor(playerId, target);
        switch (action) {
            case SET_BOUNDS_MIN -> {
                draft.boundsMin = player.blockPosition().immutable();
                draft.dirty = true;
                setMessage(player, "已用当前位置设置地图边界最小点。", false);
            }
            case SET_BOUNDS_MAX -> {
                draft.boundsMax = player.blockPosition().immutable();
                draft.dirty = true;
                setMessage(player, "已用当前位置设置地图边界最大点。", false);
            }
            case SET_RESET_MIN -> {
                draft.resetMin = player.blockPosition().immutable();
                draft.dirty = true;
                setMessage(player, "已用当前位置设置重置区域最小点。", false);
            }
            case SET_RESET_MAX -> {
                draft.resetMax = player.blockPosition().immutable();
                draft.dirty = true;
                setMessage(player, "已用当前位置设置重置区域最大点。", false);
            }
            case APPLY_REGIONS -> applyRegions(player, target, draft);
            case ADD_TEAM_A -> addSpawn(player, target, Team.TEAM_A);
            case ADD_TEAM_B -> addSpawn(player, target, Team.TEAM_B);
            case ADD_TEAM_C -> addSpawn(player, target, Team.TEAM_C);
            case ADD_TEAM_D -> addSpawn(player, target, Team.TEAM_D);
            case SET_SPECTATOR -> setSpectator(player, target);
            case CLEAR_TEAM_A -> clearSpawns(player, target, Team.TEAM_A);
            case CLEAR_TEAM_B -> clearSpawns(player, target, Team.TEAM_B);
            case CLEAR_TEAM_C -> clearSpawns(player, target, Team.TEAM_C);
            case CLEAR_TEAM_D -> clearSpawns(player, target, Team.TEAM_D);
            case CLEAR_SPECTATOR -> clearSpawns(player, target, Team.SPECTATOR);
            default -> setMessage(player, "不支持的编辑动作。", true);
        }
        sendView(player, requestId);
    }

    // ------------------------------------------------------ map CRUD & share

    private void createOwnMap(ServerPlayer player, String rawName) {
        String name = sanitizeDisplayName(rawName, player.getGameProfile().getName() + " 的地图");
        String id = uniqueMapId(player, name);
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        MapDefinition.Region region = maps.initialRegion(level, center);
        if (region.volume() <= 0L || !level.isInWorldBounds(region.min()) || !level.isInWorldBounds(region.max())) {
            setMessage(player, "当前位置无法生成合法的初始地图区域。", true);
            return;
        }
        int xOffset = Math.min(5, Math.max(0, (region.max().getX() - region.min().getX()) / 3));
        int spawnY = Math.max(region.min().getY(), Math.min(region.max().getY(), center.getY()));
        List<SpawnPoint> teamA = List.of(new SpawnPoint(level.dimension(),
                Math.max(region.min().getX(), center.getX() - xOffset) + 0.5D, spawnY,
                center.getZ() + 0.5D, 0.0F, 0.0F));
        List<SpawnPoint> teamB = List.of(new SpawnPoint(level.dimension(),
                Math.min(region.max().getX(), center.getX() + xOffset) + 0.5D, spawnY,
                center.getZ() + 0.5D, 180.0F, 0.0F));
        MapDefinition definition;
        try {
            definition = new MapDefinition(id, name, level.dimension(), region, region,
                    teamA, teamB, List.of());
        } catch (RuntimeException error) {
            setMessage(player, "地图创建失败：" + error.getMessage(), true);
            return;
        }
        if (!maps.registerMap(definition)) {
            setMessage(player, "地图配置写入失败，请检查服务器日志。", true);
            return;
        }
        ownership.setOwner(id, player.getUUID(), player.getGameProfile().getName());
        editingMapIds.put(player.getUUID(), id);
        drafts.remove(player.getUUID());
        invalidatedDrafts.remove(player.getUUID());
        setMessage(player, "地图 “" + name + "”（id：" + id + "）已创建并设为当前编辑目标。"
                + "先移动站位设置边界，再保存区域。", false);
    }

    private void importByCode(ServerPlayer player, String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
        if (code.isEmpty()) {
            setMessage(player, "请先输入 6 位地图邀请码。", true);
            return;
        }
        String sourceId = ownership.mapIdForCode(code);
        MapDefinition source = sourceId.isEmpty() ? null : maps.registry().get(sourceId).orElse(null);
        if (source == null) {
            setMessage(player, "邀请码无效或对应地图已被删除：" + code, true);
            return;
        }
        if (ownership.isOwnedBy(source.id(), player.getUUID())) {
            setMessage(player, "这是你自己的地图，直接编辑即可，无需导入。", true);
            return;
        }
        String newId = uniqueMapId(player, friendlyPart(source));
        MapDefinition copy = new MapDefinition(newId, source.displayName(), source.world(),
                source.bounds(), source.resetRegion(),
                source.teamASpawns(), source.teamBSpawns(), source.spectatorSpawns(),
                source.spawns(Team.TEAM_C), source.spawns(Team.TEAM_D), source.customRegions());
        if (!maps.registerMap(copy)) {
            setMessage(player, "导入副本写入失败，请检查服务器日志。", true);
            return;
        }
        ownership.setOwner(newId, player.getUUID(), player.getGameProfile().getName());
        editingMapIds.put(player.getUUID(), newId);
        drafts.remove(player.getUUID());
        invalidatedDrafts.remove(player.getUUID());
        setMessage(player, "已通过邀请码导入 “" + source.displayName() + "”（你的副本 id：" + newId
                + "）。副本完全独立，之后的编辑不会影响原图。", false);
    }

    private void deleteMap(ServerPlayer player, String mapId) {
        MapDefinition target = maps.registry().get(mapId).orElse(null);
        if (target == null) {
            setMessage(player, "地图不存在或已被删除。", true);
            return;
        }
        if (!canManage(player, target.id())) {
            setMessage(player, "只能删除自己的地图。", true);
            return;
        }
        if (!maps.forgetMap(target.id())) {
            setMessage(player, "地图删除失败，请稍后重试。", true);
            return;
        }
        ownership.removeMap(target.id());
        clearSessionsFor(target.id());
        setMessage(player, "地图 “" + target.displayName() + "” 已删除。", false);
    }

    private void manageShare(ServerPlayer player, String mapId, boolean generate, int requestId) {
        MapDefinition target = maps.registry().get(mapId).orElse(null);
        if (target == null) {
            setMessage(player, "请先选择一张自己的地图再生成邀请码。", true);
            sendView(player, requestId);
            return;
        }
        if (!canManage(player, target.id())) {
            setMessage(player, "只能分享自己的地图；别人的地图需要对方给你邀请码。", true);
            sendView(player, requestId);
            return;
        }
        if (generate) {
            String code = ownership.issueCode(target.id());
            setMessage(player, code.isEmpty()
                    ? "邀请码空间已满，暂时无法生成，请先撤销其他地图的邀请码。"
                    : "地图 “" + target.displayName() + "” 的邀请码：" + code
                            + "。其他玩家导入后获得独立副本。", code.isEmpty());
        } else if (ownership.revokeCode(target.id())) {
            setMessage(player, "地图 “" + target.displayName() + "” 的邀请码已撤销。", false);
        } else {
            setMessage(player, "该地图当前没有生效中的邀请码。", true);
        }
        sendView(player, requestId);
    }

    // ---------------------------------------------------------- region edits

    private void applyRegions(ServerPlayer player, MapDefinition target, Draft draft) {
        if (draft.boundsMin == null || draft.boundsMax == null
                || draft.resetMin == null || draft.resetMax == null) {
            setMessage(player, "请先设置边界和重置区域的四个角点。", true);
            return;
        }
        MapDefinition.Region bounds = new MapDefinition.Region(draft.boundsMin, draft.boundsMax);
        MapDefinition.Region reset = new MapDefinition.Region(draft.resetMin, draft.resetMax);
        if (reset.volume() > cn.blockforge.generated.generatedmod.config.FpsTdmConfig.COMMON
                .maxSnapshotBlocks.get()) {
            setMessage(player, "重置区域超过最大快照方块数限制。", true);
            return;
        }
        int removedSpawns = maps.updateRegionsFor(target, bounds, reset);
        if (removedSpawns < 0) {
            setMessage(player, "地图区域保存失败，请检查地图维度和服务器日志。", true);
            return;
        }
        drafts.remove(player.getUUID());
        if (removedSpawns > 0) {
            setMessage(player, "地图区域已保存；旧快照已作废，载入比赛时重新捕获。"
                    + "本次共移除了 " + removedSpawns + " 个新边界外的出生点，请重新设置。", false);
        } else {
            setMessage(player, "地图区域已保存；旧快照已作废，载入比赛时重新捕获。", false);
        }
    }

    private void addSpawn(ServerPlayer player, MapDefinition target, Team team) {
        SpawnPoint point = spawnFromPlayer(player);
        if (!maps.isValidSpawnFor(target, point)) {
            setMessage(player, "出生点保存失败：请确认位置在地图边界内且可安全站立。", true);
            return;
        }
        List<SpawnPoint> updated = new ArrayList<>(target.spawns(team));
        updated.add(point);
        if (maps.saveDefinition(target.withTeamSpawns(team, updated))) {
            setMessage(player, "已添加 " + team.displayName() + " 出生点。", false);
        } else {
            setMessage(player, "出生点保存失败，请检查服务器日志。", true);
        }
    }

    private void setSpectator(ServerPlayer player, MapDefinition target) {
        SpawnPoint point = spawnFromPlayer(player);
        if (!maps.isValidSpawnFor(target, point)) {
            setMessage(player, "观战出生点保存失败：请确认位置在地图边界内且可安全站立。", true);
            return;
        }
        if (maps.saveDefinition(target.withTeamSpawns(Team.SPECTATOR, List.of(point)))) {
            setMessage(player, "已设置观战出生点。", false);
        } else {
            setMessage(player, "观战出生点保存失败。", true);
        }
    }

    private void clearSpawns(ServerPlayer player, MapDefinition target, Team team) {
        if (maps.saveDefinition(target.withTeamSpawns(team, List.of()))) {
            setMessage(player, "已清除 " + team.displayName() + " 出生点。", false);
        } else {
            setMessage(player, "出生点清除失败。", true);
        }
    }

    private SpawnPoint spawnFromPlayer(ServerPlayer player) {
        return new SpawnPoint(player.serverLevel().dimension(), player.getX(), player.getY(),
                player.getZ(), player.getYRot(), player.getXRot());
    }

    // ------------------------------------------------------- session helpers

    private boolean isAdmin(ServerPlayer player) {
        return player.hasPermissions(ADMIN_PERMISSION);
    }

    /** 拥有者或服务器管理员可编辑；没有归属登记的旧地图视为服务器地图，仅管理员可编辑。 */
    private boolean canManage(ServerPlayer player, String mapId) {
        return isAdmin(player) || ownership.isOwnedBy(mapId, player.getUUID());
    }

    private String editLockMessage() {
        if (matchManager.state().isActive()) {
            return "比赛进行中不能编辑地图。";
        }
        if (matchManager.rooms().hasRunningRoom()) {
            return "房间正在准备、倒计时或运行，暂时不能编辑地图。";
        }
        if (maps.isLoading() || maps.isResetting()) {
            return "地图快照任务进行中，暂时不能编辑地图。";
        }
        return null;
    }

    /** 当前玩家的编辑目标定义；目标丢失时回退到自己的第一张地图，管理员回退到任意地图。 */
    private MapDefinition targetDefinition(ServerPlayer player) {
        UUID playerId = player.getUUID();
        String id = editingMapIds.get(playerId);
        MapDefinition definition = id == null ? null : maps.registry().get(id).orElse(null);
        if (definition != null) {
            return definition;
        }
        String fallback = "";
        for (String owned : ownership.mapsOf(playerId)) {
            if (maps.registry().get(owned).isPresent()) {
                fallback = owned;
                break;
            }
        }
        if (fallback.isEmpty() && isAdmin(player) && !maps.registry().ids().isEmpty()) {
            // 管理员兜底：没有任何自己的地图时，先指向注册表里的第一张图便于运维。
            fallback = maps.registry().ids().get(0);
        }
        if (!fallback.equals(id)) {
            editingMapIds.put(playerId, fallback);
            drafts.remove(playerId);
            if (!fallback.isEmpty()) {
                invalidatedDrafts.remove(playerId);
            }
        }
        return fallback.isEmpty() ? null : maps.registry().get(fallback).orElse(null);
    }

    private void selectTarget(ServerPlayer player, String mapId) {
        MapDefinition target = maps.registry().get(mapId).orElse(null);
        if (target == null) {
            setMessage(player, "未找到地图：" + mapId, true);
            return;
        }
        if (!canManage(player, target.id())) {
            setMessage(player, "只能编辑自己的地图；导入别人的地图需要邀请码。", true);
            return;
        }
        editingMapIds.put(player.getUUID(), target.id());
        drafts.remove(player.getUUID());
        invalidatedDrafts.remove(player.getUUID());
        setMessage(player, "编辑目标已切换为 “" + target.displayName() + "”（" + target.id() + "）。", false);
    }

    private void clearSessionsFor(String mapId) {
        editingMapIds.entrySet().removeIf(entry -> mapId.equals(entry.getValue()));
        drafts.entrySet().removeIf(entry -> entry.getValue().sourceDefinition != null
                && mapId.equals(entry.getValue().sourceDefinition.id()));
    }

    public void sendView(ServerPlayer player) {
        sendView(player, 0);
    }

    private void sendView(ServerPlayer player, int responseRequestId) {
        if (player == null) {
            return;
        }
        MapEditorView base = view(player);
        UUID playerId = player.getUUID();
        boolean invalidated = invalidatedDrafts.contains(playerId);
        String message = invalidated ? DRAFT_INVALIDATED_MESSAGE
                : messages.getOrDefault(playerId, "");
        boolean error = invalidated || errors.getOrDefault(playerId, false);
        FpsTdmNetwork.sendToPlayer(new MapEditorSyncPacket(new MapEditorView(
                base.hasTarget(), base.mapId(), base.displayName(), base.world(),
                base.bounds(), base.resetRegion(), base.draftBounds(), base.draftResetRegion(),
                base.teamACount(), base.teamBCount(), base.spectatorCount(), base.snapshotStatus(),
                base.canEdit(), base.isAdmin(), base.locked(), base.draftInvalidated(),
                base.ownedMaps(), base.serverMaps(), base.shareCode(),
                responseRequestId, message, error, base.teamCCount(), base.teamDCount(),
                base.regions(), base.selectedTool(), base.brushMode(),
                base.selectedRegionId(), base.brushRange())), player);
    }

    public void onLogout(ServerPlayer player) {
        drafts.remove(player.getUUID());
        editingMapIds.remove(player.getUUID());
        invalidatedDrafts.remove(player.getUUID());
        messages.remove(player.getUUID());
        errors.remove(player.getUUID());
        selectedTools.remove(player.getUUID());
        brushModes.remove(player.getUUID());
        brushFirstPoints.remove(player.getUUID());
        brushSecondPoints.remove(player.getUUID());
        selectedRegionIds.remove(player.getUUID());
        brushRanges.remove(player.getUUID());
    }

    // ------------------------------------------------------------ tool items

    public MapTool selectedTool(ServerPlayer player) {
        return selectedTools.getOrDefault(player.getUUID(), MapTool.BOUNDS);
    }

    public MapBrushMode brushMode(ServerPlayer player) {
        return brushModes.computeIfAbsent(player.getUUID(), id -> MapBrushMode.parse(
                brushPreferences(player).getString("mode")));
    }

    public int brushRange(ServerPlayer player) {
        return brushRanges.computeIfAbsent(player.getUUID(), id -> {
            int saved = brushPreferences(player).getInt("range");
            return saved <= 0 ? 2 : Math.min(64, saved);
        });
    }

    private net.minecraft.nbt.CompoundTag brushPreferences(ServerPlayer player) {
        var data = player.getPersistentData();
        String key = net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG;
        if (!data.contains(key, 10)) data.put(key, new net.minecraft.nbt.CompoundTag());
        var persisted = data.getCompound(key);
        if (!persisted.contains("tdmBrush", 10)) persisted.put("tdmBrush", new net.minecraft.nbt.CompoundTag());
        return persisted.getCompound("tdmBrush");
    }

    private void saveBrushPreferences(ServerPlayer player) {
        var preferences = brushPreferences(player);
        preferences.putString("mode", brushMode(player).id());
        preferences.putInt("range", brushRange(player));
    }

    public void setBrushRange(ServerPlayer player, int range) {
        int safe = Math.max(1, Math.min(64, range));
        brushRanges.put(player.getUUID(), safe);
        saveBrushPreferences(player);
        feedback(player, "画笔右键选择距离：" + safe + " 格。", false);
    }

    public String selectedRegionId(ServerPlayer player) {
        return selectedRegionIds.getOrDefault(player.getUUID(), "bounds");
    }

    public MapRegion selectedRegion(ServerPlayer player, MapDefinition definition) {
        if (definition == null) return null;
        return definition.region(selectedRegionId(player));
    }

    public void selectRegion(ServerPlayer player, MapRegion region) {
        if (region == null) return;
        UUID playerId = player.getUUID();
        selectedRegionIds.put(playerId, region.id());
        selectedTools.put(playerId, region.type() == MapRegion.Type.BOUNDS
                ? MapTool.BOUNDS : region.type() == MapRegion.Type.RESET
                ? MapTool.RESET : MapTool.CUSTOM);
        brushFirstPoints.remove(playerId);
        brushSecondPoints.remove(playerId);
        feedback(player, "已选择区域：" + region.displayName(), false);
    }

    public void handleRegion(ServerPlayer player, MapRegionAction action, MapRegion region, int requestId) {
        if (player == null || action == null) return;
        MapDefinition target = targetDefinition(player);
        if (target == null) {
            feedback(player, "你还没有地图：先新建一张，或向拥有者索要邀请码导入。", true);
            sendView(player, requestId);
            return;
        }
        if (!canManage(player, target.id())) {
            feedback(player, "只能编辑自己的地图；可用拥有者的邀请码导入一份独立副本。", true);
            sendView(player, requestId);
            return;
        }
        String lock = editLockMessage();
        if (lock != null) {
            feedback(player, lock, true);
            sendView(player, requestId);
            return;
        }
        switch (action) {
            case SELECT -> selectRegion(player, region == null ? target.region("bounds") : region);
            case CREATE -> {
                MapRegion created = region == null ? MapRegion.custom(uniqueRegionId(target),
                        "自定义区域", MapRegion.Type.CUSTOM, target.bounds()) : region;
                if (created.id().isBlank()) created = new MapRegion(uniqueRegionId(target),
                        created.displayName(), created.type(), created.region(), created.visibleInMatch(),
                        created.displayRange(), created.appearance(), created.activation(),
                        created.activationValue(), created.color(), created.outline(), created.fill(),
                        created.priority(), created.notes());
                if (maps.saveDefinition(target.withRegion(created))) {
                    selectedRegionIds.put(player.getUUID(), created.id());
                    selectedTools.put(player.getUUID(), MapTool.CUSTOM);
                    feedback(player, "已创建区域：" + created.displayName(), false);
                } else {
                    feedback(player, "区域创建失败，请检查服务器日志。", true);
                }
            }
            case SAVE -> {
                if (region == null || region.id().isBlank()) {
                    feedback(player, "区域保存失败：缺少区域 ID。", true);
                } else if (!player.serverLevel().dimension().equals(target.world())) {
                    feedback(player, "请先传送到该地图所在维度，再保存区域。", true);
                } else if (!target.bounds().contains(region.region().min())
                        || !target.bounds().contains(region.region().max())) {
                    feedback(player, "区域必须在地图边界内。", true);
                } else if (maps.saveDefinition(target.withRegion(region))) {
                    selectedRegionIds.put(player.getUUID(), region.id());
                    feedback(player, "区域已保存：" + region.displayName(), false);
                } else {
                    feedback(player, "区域保存失败，请检查服务器日志。", true);
                }
            }
            case DELETE -> {
                if (region == null || "bounds".equals(region.id()) || "reset".equals(region.id())) {
                    feedback(player, "地图边界和重置区域不能删除。", true);
                } else if (maps.saveDefinition(target.withoutRegion(region.id()))) {
                    selectedRegionIds.put(player.getUUID(), "bounds");
                    selectedTools.put(player.getUUID(), MapTool.BOUNDS);
                    feedback(player, "区域已删除：" + region.displayName(), false);
                } else {
                    feedback(player, "区域删除失败，请检查服务器日志。", true);
                }
            }
            default -> feedback(player, "不支持的区域操作。", true);
        }
        sendView(player, requestId);
    }

    private void giveTools(ServerPlayer player) {
        player.getInventory().add(new ItemStack(ModItems.MAP_PLANNER.get()));
        player.getInventory().add(new ItemStack(ModItems.MAP_BRUSH.get()));
        feedback(player, "已发放地图规划器和地图画笔。", false);
    }

    private static int parseRange(String value) {
        try {
            return Integer.parseInt(value == null ? "2" : value.trim());
        } catch (NumberFormatException ignored) {
            return 2;
        }
    }

    private String uniqueRegionId(MapDefinition definition) {
        String base = "region";
        int index = 1;
        while (definition.region(base + "_" + index) != null) index++;
        return base + "_" + index;
    }

    public void selectTool(ServerPlayer player, MapTool tool) {
        if (tool == null) {
            return;
        }
        UUID playerId = player.getUUID();
        selectedTools.put(playerId, tool);
        brushFirstPoints.remove(playerId);
        brushSecondPoints.remove(playerId);
        feedback(player, "已选择：" + tool.displayName(), false);
    }

    public void toggleBrushMode(ServerPlayer player) {
        UUID playerId = player.getUUID();
        MapBrushMode next = brushMode(player) == MapBrushMode.REGION
                ? MapBrushMode.BLOCK : MapBrushMode.REGION;
        brushModes.put(playerId, next);
        saveBrushPreferences(player);
        brushFirstPoints.remove(playerId);
        brushSecondPoints.remove(playerId);
        feedback(player, "画笔模式：" + next.displayName(), false);
    }

    public void brushLeft(ServerPlayer player, BlockPos position) {
        handleBrushClick(player, position, true);
    }

    public void brushRight(ServerPlayer player, BlockPos position) {
        handleBrushClick(player, position, false);
    }

    private void handleBrushClick(ServerPlayer player, BlockPos position, boolean leftClick) {
        MapDefinition target = editableTarget(player);
        if (target == null) {
            sendView(player);
            return;
        }
        UUID playerId = player.getUUID();
        MapTool tool = selectedTool(player);
        MapBrushMode mode = brushMode(player);
        if (tool.kind() == MapTool.Kind.POINT) mode = MapBrushMode.BLOCK;
        if (tool.kind() == MapTool.Kind.POINT) {
            position = position.above();
        }
        if (mode == MapBrushMode.REGION) {
            if (leftClick) {
                brushFirstPoints.put(playerId, position.immutable());
                feedback(player, "已记录第一个角。右键选择对角，完成后自动保存。", false);
            } else {
                brushSecondPoints.put(playerId, position.immutable());
                feedback(player, "已记录第二个角。左键选择另一角，完成后自动保存。", false);
            }
            BlockPos first = brushFirstPoints.get(playerId);
            BlockPos second = brushSecondPoints.get(playerId);
            if (first != null && second != null) {
                applyRegionEndpoints(player, target, tool, first, second);
                brushFirstPoints.remove(playerId);
                brushSecondPoints.remove(playerId);
            }
        } else if (tool.kind() == MapTool.Kind.REGION) {
            adjustRegion(player, target, tool, position, !leftClick);
        } else {
            if (leftClick) {
                removeSpawnAt(player, target, tool, position);
            } else {
                addSpawnAt(player, target, tool, position);
            }
        }
        sendView(player);
    }

    private MapDefinition editableTarget(ServerPlayer player) {
        UUID playerId = player.getUUID();
        MapDefinition target = targetDefinition(player);
        if (target == null) {
            feedback(player, "你还没有地图：先新建一张，或向拥有者索要邀请码导入。", true);
            return null;
        }
        if (!canManage(player, target.id())) {
            feedback(player, "只能编辑自己的地图；可用拥有者的邀请码导入一份独立副本。", true);
            return null;
        }
        if (invalidatedDrafts.contains(playerId)) {
            feedback(player, DRAFT_INVALIDATED_MESSAGE, true);
            return null;
        }
        String lock = editLockMessage();
        if (lock != null) {
            feedback(player, lock, true);
            return null;
        }
        if (!player.serverLevel().dimension().equals(target.world())) {
            feedback(player, "请先传送到该地图所在维度，再使用画笔。", true);
            return null;
        }
        return target;
    }

    private void applyRegionEndpoints(ServerPlayer player, MapDefinition target, MapTool tool,
                                      BlockPos first, BlockPos second) {
        Draft draft = draftFor(player.getUUID(), target);
        BlockPos min = new BlockPos(
                Math.min(first.getX(), second.getX()),
                Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ()));
        BlockPos max = new BlockPos(
                Math.max(first.getX(), second.getX()),
                Math.max(first.getY(), second.getY()),
                Math.max(first.getZ(), second.getZ()));
        if (tool == MapTool.CUSTOM) {
            saveCustomRegionBounds(player, target, new MapDefinition.Region(min, max));
            return;
        }
        if (tool == MapTool.BOUNDS) {
            draft.boundsMin = min;
            draft.boundsMax = max;
        } else {
            draft.resetMin = min;
            draft.resetMax = max;
        }
        draft.dirty = true;
        applyRegions(player, target, draft);
    }

    private void adjustRegion(ServerPlayer player, MapDefinition target, MapTool tool,
                              BlockPos position, boolean include) {
        if (tool == MapTool.CUSTOM) {
            MapRegion selected = selectedRegion(player, target);
            if (selected == null || selected.type() == MapRegion.Type.BOUNDS
                    || selected.type() == MapRegion.Type.RESET) {
                feedback(player, "请先在规划器中创建或选择自定义区域。", true);
                return;
            }
            MapDefinition.Region next = adjustBounds(selected.region(), position, include);
            if (next != null) saveCustomRegionBounds(player, target, next);
            return;
        }
        Draft draft = draftFor(player.getUUID(), target);
        BlockPos min = tool == MapTool.BOUNDS ? draft.boundsMin : draft.resetMin;
        BlockPos max = tool == MapTool.BOUNDS ? draft.boundsMax : draft.resetMax;
        if (min == null || max == null) {
            feedback(player, "请先用区域模式设置初始区域。", true);
            return;
        }
        BlockPos newMin = min;
        BlockPos newMax = max;
        if (include) {
            newMin = new BlockPos(Math.min(min.getX(), position.getX()),
                    Math.min(min.getY(), position.getY()), Math.min(min.getZ(), position.getZ()));
            newMax = new BlockPos(Math.max(max.getX(), position.getX()),
                    Math.max(max.getY(), position.getY()), Math.max(max.getZ(), position.getZ()));
        } else {
            if (!contains(min, max, position)) {
                feedback(player, "该方块已经在区域外。", true);
                return;
            }
            long[] distances = {
                    position.getX() - min.getX(), max.getX() - position.getX(),
                    position.getY() - min.getY(), max.getY() - position.getY(),
                    position.getZ() - min.getZ(), max.getZ() - position.getZ()};
            int nearest = 0;
            for (int i = 1; i < distances.length; i++) {
                if (distances[i] < distances[nearest]) {
                    nearest = i;
                }
            }
            switch (nearest) {
                case 0 -> newMin = new BlockPos(position.getX() + 1, min.getY(), min.getZ());
                case 1 -> newMax = new BlockPos(position.getX() - 1, max.getY(), max.getZ());
                case 2 -> newMin = new BlockPos(min.getX(), position.getY() + 1, min.getZ());
                case 3 -> newMax = new BlockPos(max.getX(), position.getY() - 1, max.getZ());
                case 4 -> newMin = new BlockPos(min.getX(), min.getY(), position.getZ() + 1);
                case 5 -> newMax = new BlockPos(max.getX(), max.getY(), position.getZ() - 1);
                default -> { }
            }
            if (newMin.getX() > newMax.getX() || newMin.getY() > newMax.getY()
                    || newMin.getZ() > newMax.getZ()) {
                feedback(player, "区域至少要保留一个方块。", true);
                return;
            }
        }
        if (tool == MapTool.BOUNDS) {
            draft.boundsMin = newMin;
            draft.boundsMax = newMax;
        } else {
            draft.resetMin = newMin;
            draft.resetMax = newMax;
        }
        draft.dirty = true;
        applyRegions(player, target, draft);
    }

    private MapDefinition.Region adjustBounds(MapDefinition.Region region, BlockPos position, boolean include) {
        BlockPos min = region.min();
        BlockPos max = region.max();
        BlockPos newMin = min;
        BlockPos newMax = max;
        if (include) {
            newMin = new BlockPos(Math.min(min.getX(), position.getX()), Math.min(min.getY(), position.getY()),
                    Math.min(min.getZ(), position.getZ()));
            newMax = new BlockPos(Math.max(max.getX(), position.getX()), Math.max(max.getY(), position.getY()),
                    Math.max(max.getZ(), position.getZ()));
        } else {
            if (!contains(min, max, position)) return null;
            long[] distances = {
                    position.getX() - min.getX(), max.getX() - position.getX(),
                    position.getY() - min.getY(), max.getY() - position.getY(),
                    position.getZ() - min.getZ(), max.getZ() - position.getZ()};
            int nearest = 0;
            for (int index = 1; index < distances.length; index++) {
                if (distances[index] < distances[nearest]) nearest = index;
            }
            switch (nearest) {
                case 0 -> newMin = new BlockPos(position.getX() + 1, min.getY(), min.getZ());
                case 1 -> newMax = new BlockPos(position.getX() - 1, max.getY(), max.getZ());
                case 2 -> newMin = new BlockPos(min.getX(), position.getY() + 1, min.getZ());
                case 3 -> newMax = new BlockPos(max.getX(), max.getY() - 1, max.getZ());
                case 4 -> newMin = new BlockPos(min.getX(), min.getY(), position.getZ() + 1);
                case 5 -> newMax = new BlockPos(max.getX(), max.getY(), position.getZ() - 1);
                default -> { }
            }
        }
        if (newMin.getX() > newMax.getX() || newMin.getY() > newMax.getY() || newMin.getZ() > newMax.getZ()) {
            return null;
        }
        return new MapDefinition.Region(newMin, newMax);
    }

    private void saveCustomRegionBounds(ServerPlayer player, MapDefinition target,
                                        MapDefinition.Region region) {
        MapRegion selected = selectedRegion(player, target);
        if (selected == null || selected.type() == MapRegion.Type.BOUNDS
                || selected.type() == MapRegion.Type.RESET) {
            feedback(player, "请先在规划器中创建或选择自定义区域。", true);
            return;
        }
        MapRegion updated = new MapRegion(selected.id(), selected.displayName(), selected.type(), region,
                selected.visibleInMatch(), selected.displayRange(), selected.appearance(),
                selected.activation(), selected.activationValue(), selected.color(), selected.outline(),
                selected.fill(), selected.priority(), selected.notes());
        if (maps.saveDefinition(target.withRegion(updated))) {
            feedback(player, "区域范围已保存：" + updated.displayName(), false);
        } else {
            feedback(player, "区域范围保存失败，请检查服务器日志。", true);
        }
    }

    private void addSpawnAt(ServerPlayer player, MapDefinition target, MapTool tool, BlockPos position) {
        Team team = tool.team();
        SpawnPoint point = new SpawnPoint(player.serverLevel().dimension(), position, player.getYRot());
        if (!maps.isValidSpawnFor(target, point)) {
            feedback(player, "出生点保存失败：请确认位置在地图边界内且可安全站立。", true);
            return;
        }
        List<SpawnPoint> updated = new ArrayList<>(target.spawns(team));
        updated.add(point);
        if (maps.saveDefinition(target.withTeamSpawns(team, updated))) {
            feedback(player, "已添加 " + team.displayName() + " 出生点。", false);
        } else {
            feedback(player, "出生点保存失败，请检查服务器日志。", true);
        }
    }

    private void removeSpawnAt(ServerPlayer player, MapDefinition target, MapTool tool, BlockPos position) {
        Team team = tool.team();
        List<SpawnPoint> current = target.spawns(team);
        List<SpawnPoint> updated = new ArrayList<>();
        for (SpawnPoint point : current) {
            if (!point.blockPosition().equals(position)) {
                updated.add(point);
            }
        }
        if (updated.size() == current.size()) {
            feedback(player, "这个方块没有对应的出生点。", true);
            return;
        }
        if (maps.saveDefinition(target.withTeamSpawns(team, updated))) {
            feedback(player, "已移除 " + team.displayName() + " 出生点。", false);
        } else {
            feedback(player, "出生点移除失败，请检查服务器日志。", true);
        }
    }

    private boolean contains(BlockPos min, BlockPos max, BlockPos position) {
        return position.getX() >= min.getX() && position.getX() <= max.getX()
                && position.getY() >= min.getY() && position.getY() <= max.getY()
                && position.getZ() >= min.getZ() && position.getZ() <= max.getZ();
    }

    private void feedback(ServerPlayer player, String message, boolean error) {
        setMessage(player, message, error);
        player.displayClientMessage(Component.literal(message), true);
    }

    private MapEditorView view(ServerPlayer player) {
        UUID playerId = player.getUUID();
        MapDefinition definition = targetDefinition(player);
        Draft draft = definition == null ? null : draftFor(playerId, definition);
        boolean admin = isAdmin(player);
        boolean canEdit = definition != null && canManage(player, definition.id());
        String lockedMessage = editLockMessage();
        List<String> ownedMaps = new ArrayList<>();
        List<String> serverMaps = new ArrayList<>();
        for (MapDefinition candidate : maps.registry().definitions()) {
            String code = ownership.codeFor(candidate.id());
            if (ownership.isOwnedBy(candidate.id(), playerId)) {
                ownedMaps.add(candidate.id() + "|" + candidate.displayName() + "|" + code);
            } else if (admin) {
                serverMaps.add(candidate.id() + "|" + candidate.displayName() + "|"
                        + ownership.ownerName(candidate.id()));
            }
        }
        return new MapEditorView(
                definition != null,
                definition == null ? "" : definition.id(),
                definition == null ? "" : definition.displayName(),
                definition == null ? "" : definition.world().location().toString(),
                definition == null ? MapEditorView.RegionData.empty() : regionData(definition.bounds()),
                definition == null ? MapEditorView.RegionData.empty() : regionData(definition.resetRegion()),
                draft == null ? MapEditorView.RegionData.empty() : regionData(draft.regionBounds()),
                draft == null ? MapEditorView.RegionData.empty() : regionData(draft.regionReset()),
                definition == null ? 0 : definition.teamASpawns().size(),
                definition == null ? 0 : definition.teamBSpawns().size(),
                definition == null ? 0 : definition.spectatorSpawns().size(),
                snapshotStatus(definition),
                canEdit, admin, lockedMessage != null,
                invalidatedDrafts.contains(playerId),
                ownedMaps, serverMaps,
                definition == null ? "" : ownership.codeFor(definition.id()), 0,
                messages.getOrDefault(playerId, ""),
                errors.getOrDefault(playerId, false),
                definition == null ? 0 : definition.spawns(Team.TEAM_C).size(),
                definition == null ? 0 : definition.spawns(Team.TEAM_D).size(),
                definition == null ? List.of() : definition.regions(),
                selectedTool(player), brushMode(player), selectedRegionId(player), brushRange(player));
    }

    private String snapshotStatus(MapDefinition definition) {
        if (definition == null) {
            return "尚未选择编辑目标";
        }
        if (maps.currentMap().map(current -> current.id().equals(definition.id())).orElse(false)) {
            return maps.statusLine();
        }
        return maps.registry().hasSnapshot(definition.id())
                ? "已有初始快照（载入比赛时校验）"
                : "未捕获（房间载入比赛时自动捕获）";
    }

    // ---------------------------------------------------------------- utils

    private static MapEditorView.RegionData regionData(MapDefinition.Region region) {
        return region == null ? MapEditorView.RegionData.empty()
                : new MapEditorView.RegionData(true, region.min().getX(), region.min().getY(), region.min().getZ(),
                region.max().getX(), region.max().getY(), region.max().getZ());
    }

    private static boolean isPositionAction(MapEditorAction action) {
        return switch (action) {
            case SET_BOUNDS_MIN, SET_BOUNDS_MAX, SET_RESET_MIN, SET_RESET_MAX,
                    ADD_TEAM_A, ADD_TEAM_B, ADD_TEAM_C, ADD_TEAM_D, SET_SPECTATOR -> true;
            default -> false;
        };
    }

    /** 保证 id 全局唯一：`拥有者-名称slug`，冲突时追加序号。 */
    private String uniqueMapId(ServerPlayer player, String preferredName) {
        String ownerSlug = slugify(player.getGameProfile().getName(), "map");
        if (ownerSlug.length() > 12) {
            ownerSlug = ownerSlug.substring(0, 12);
        }
        String base = slugify(preferredName, "map");
        if (base.length() > 24) {
            base = base.substring(0, 24);
        }
        String candidate = ownerSlug + "-" + base;
        int suffix = 2;
        while (!maps.registry().get(candidate).isEmpty()) {
            candidate = ownerSlug + "-" + base + "-" + suffix++;
        }
        return candidate;
    }

    private static String friendlyPart(MapDefinition source) {
        // 用源图显示名做副本命名基础；slug 化后为空时 uniqueMapId 会退回 “map”+序号。
        return source.displayName();
    }

    private static String slugify(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        StringBuilder builder = new StringBuilder();
        boolean pendingSeparator = false;
        for (char c : raw.toLowerCase(Locale.ROOT).toCharArray()) {
            boolean keep = c >= 'a' && c <= 'z' || c >= '0' && c <= '9';
            if (keep) {
                if (pendingSeparator && builder.length() > 0) {
                    builder.append('-');
                }
                pendingSeparator = false;
                builder.append(c);
            } else if (builder.length() > 0) {
                pendingSeparator = true;
            }
        }
        String slug = builder.toString();
        return slug.isEmpty() ? fallback : slug;
    }

    private static String sanitizeDisplayName(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String cleaned = value.trim().replace('\n', ' ').replace('\r', ' ').replace('\t', ' ')
                .replace('|', '¦');
        if (cleaned.isBlank()) {
            return fallback;
        }
        return cleaned.substring(0, Math.min(48, cleaned.length()));
    }

    private void setMessage(ServerPlayer player, String message, boolean error) {
        messages.put(player.getUUID(), message == null ? "" : message);
        errors.put(player.getUUID(), error);
    }

    /**
     * 获取编辑目标地图的区域草稿；定义变化时旧草稿失效并重新载入。
     * 出生点更新不影响区域草稿。
     */
    private Draft draftFor(UUID playerId, MapDefinition definition) {
        Draft draft = drafts.get(playerId);
        if (draft == null) {
            draft = Draft.from(definition);
            drafts.put(playerId, draft);
        } else if (!draft.belongsTo(definition)) {
            if (draft.dirty) {
                invalidatedDrafts.add(playerId);
            }
            draft = Draft.from(definition);
            drafts.put(playerId, draft);
        }
        return draft;
    }

    private static final class Draft {        /** 创建草稿时绑定的完整定义快照，而不是只有地图 id。 */
        private final MapDefinition sourceDefinition;
        private BlockPos boundsMin;
        private BlockPos boundsMax;
        private BlockPos resetMin;
        private BlockPos resetMax;
        private boolean dirty;

        private Draft(MapDefinition sourceDefinition) {
            this.sourceDefinition = sourceDefinition;
        }

        private static Draft from(MapDefinition definition) {
            Draft draft = new Draft(definition);
            if (definition != null) {
                draft.boundsMin = definition.bounds().min();
                draft.boundsMax = definition.bounds().max();
                draft.resetMin = definition.resetRegion().min();
                draft.resetMax = definition.resetRegion().max();
            }
            return draft;
        }

        private boolean belongsTo(MapDefinition definition) {
            if (sourceDefinition == null || definition == null) {
                return sourceDefinition == definition;
            }
            return sourceDefinition.sameRegionConfiguration(definition);
        }

        private MapDefinition.Region regionBounds() {
            return boundsMin == null || boundsMax == null ? null : new MapDefinition.Region(boundsMin, boundsMax);
        }

        private MapDefinition.Region regionReset() {
            return resetMin == null || resetMax == null ? null : new MapDefinition.Region(resetMin, resetMax);
        }
    }
}
