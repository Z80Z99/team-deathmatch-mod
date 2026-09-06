package cn.blockforge.generated.generatedmod.lobby;

import cn.blockforge.generated.generatedmod.map.MapManager;
import cn.blockforge.generated.generatedmod.match.MatchManager;
import cn.blockforge.generated.generatedmod.match.Team;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.RoomSyncPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** CF 风格的房间大厅；当前版本一次只运行一场实际 TDM 比赛。 */
public final class RoomManager {
    private static final int ADMIN_PERMISSION = 2;
    private final MinecraftServer server;
    private final MatchManager matchManager;
    private final LobbyConfigManager config;
    private final Map<String, Room> rooms = new LinkedHashMap<>();
    private final MatchmakingManager matchmaking;
    private long nextRoomNumber = 1L;
    /** 启动流程从地图等待/倒计时到真正开赛期间的独占预留。 */
    private String reservedRoomId;
    private String activeRoomId;

    public RoomManager(MinecraftServer server, MatchManager matchManager) {
        this.server = server;
        this.matchManager = matchManager;
        this.config = new LobbyConfigManager(server);
        this.matchmaking = new MatchmakingManager(server, matchManager, this);
    }

    public LobbyConfigManager config() {
        return config;
    }

    public MatchmakingManager matchmaking() {
        return matchmaking;
    }

    public void tick() {
        cancelExternalMatchReservation();
        matchmaking.tick();
        removeOfflinePlayers();
        for (Room room : new ArrayList<>(rooms.values())) {
            if (room.state() == RoomState.RUNNING) {
                if (activeRoomId != null && activeRoomId.equals(room.id()) && !matchManager.isMatchActive()) {
                    activeRoomId = null;
                    if (room.matchmaking()) {
                        // 匹配比赛相当于临时房间：赛终即自动解散，成员回到自由状态。
                        rooms.remove(room.id());
                        sendAll("匹配比赛已结束，比赛房间已自动解散。", false);
                    } else {
                        room.state(RoomState.OPEN);
                        sendRoomMessage(room, "上一场比赛已结束，房间重新开放。", false);
                    }
                }
                continue;
            }
            if (room.state() == RoomState.WAITING_MAP) {
                startRoom(room);
            } else if (room.state() == RoomState.COUNTDOWN) {
                if (!canStart(room)) {
                    cancelStart(room, "房间条件变化，开赛倒计时已取消。", true);
                } else if (server.getTickCount() >= room.countdownEndTick()) {
                    launchRoom(room);
                }
            }
        }
    }

    public void handleAction(ServerPlayer player, RoomAction action, String roomId,
                             String roomName, String mapId, int requestedMaxPlayers, String password) {
        if (player == null || action == null) {
            return;
        }
        switch (action) {
            case CREATE -> create(player, roomName, requestedMaxPlayers, password);
            case JOIN -> join(player, roomId, password);
            case LEAVE -> leave(player);
            case START -> start(player);
            case DESTROY -> destroy(player);
            case SET_MAP -> setMap(player, roomId, mapId);
            case MAP_LIST -> sendMapList(player);
            case REFRESH -> sendSync(player, "", false);
        }
    }

    /** 下发“可作为房间用图”的完整列表：所有已注册地图（含玩家制作），格式 {@code id|显示名|属主}。 */
    private void sendMapList(ServerPlayer player) {
        List<String> options = new ArrayList<>();
        for (cn.blockforge.generated.generatedmod.map.MapDefinition definition
                : matchManager.maps().registry().definitions()) {
            String owner = matchManager.mapEditor().ownership().ownerName(definition.id());
            options.add(definition.id() + "|" + definition.displayName()
                    + "|" + (owner == null ? "" : owner));
        }
        FpsTdmNetwork.sendToPlayer(new cn.blockforge.generated.generatedmod.network.packet
                .RoomMapListSyncPacket(options), player);
    }

    public void sendSync(ServerPlayer player, String message, boolean error) {
        Room own = roomFor(player.getUUID()).orElse(null);
        List<RoomView> views = new ArrayList<>();
        for (Room room : rooms.values()) {
            views.add(toView(room));
        }
        FpsTdmNetwork.sendToPlayer(new RoomSyncPacket(views,
                own == null ? "" : own.id(),
                own != null && own.owner().equals(player.getUUID()),
                matchManager.isMatchActive(), message, error), player);
    }

    public void sendAll(String message, boolean error) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendSync(player, message, error);
        }
    }

    public Optional<Room> roomFor(UUID playerId) {
        return rooms.values().stream().filter(room -> room.contains(playerId)).findFirst();
    }

    public boolean hasRunningRoom() {
        return activeRoomId != null || reservedRoomId != null;
    }

    public boolean roomCapacityAvailable() {
        return rooms.size() < config.values().maxRooms();
    }

    private boolean hasOtherReservation(Room room) {
        return reservedRoomId != null && !reservedRoomId.equals(room.id());
    }

    private void releaseReservation(Room room) {
        if (room != null && room.id().equals(reservedRoomId)) {
            reservedRoomId = null;
        }
    }

    private void cancelStart(Room room, String message, boolean error) {
        releaseReservation(room);
        room.state(RoomState.OPEN);
        room.countdownEndTick(0L);
        if (room.matchmaking()) {
            rooms.remove(room.id());
            matchmaking.cancelForming(room.id());
            matchmaking.requeue(room.members());
            String notice = message + " 剩余玩家已返回匹配队列。";
            matchmaking.sendAll(notice, error);
            sendAll(notice, error);
        } else {
            sendRoomMessage(room, message, error);
        }
    }

    private void cancelExternalMatchReservation() {
        if (reservedRoomId == null || !matchManager.isMatchActive()) {
            return;
        }
        Room room = rooms.get(reservedRoomId);
        if (room != null && room.state() != RoomState.RUNNING) {
            releaseReservation(room);
            room.state(RoomState.OPEN);
            room.countdownEndTick(0L);
            if (room.matchmaking()) {
                List<UUID> members = new ArrayList<>(room.members());
                rooms.remove(room.id());
                matchmaking.requeue(members);
                sendAll("快速匹配被其他比赛占用，玩家已返回匹配队列。", true);
            }
        } else {
            reservedRoomId = null;
        }
    }

    public Optional<Room> get(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(rooms.get(id));
    }

    public void sendLobbyConfig(ServerPlayer player, String message, boolean error) {
        if (player == null) {
            return;
        }
        boolean admin = player.hasPermissions(ADMIN_PERMISSION);
        boolean locked = matchManager.isMatchActive() || hasRunningRoom();
        FpsTdmNetwork.sendToPlayer(new cn.blockforge.generated.generatedmod.network.packet.LobbyConfigSyncPacket(
                admin ? config.values() : LobbyConfigValues.defaults(), admin, locked,
                admin ? message : "权限不足，只有服务器管理员可以打开游戏配置。", admin || error ? error : true), player);
    }

    public void onLogout(ServerPlayer player) {
        matchmaking.remove(player.getUUID());
        leaveInternal(player.getUUID());
        sendAll("玩家离开后房间列表已更新。", false);
    }

    /** 由匹配队列调用：把整个队列开成一间匹配比赛房，进入 10 秒准备倒计时后开赛。 */
    public void createMatchmakingRoom(List<UUID> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return;
        }
        if (matchManager.isMatchActive() || hasRunningRoom()) {
            matchmaking.requeue(playerIds);
            return;
        }
        List<UUID> onlinePlayers = new ArrayList<>();
        for (UUID playerId : playerIds) {
            if (playerId != null && server.getPlayerList().getPlayer(playerId) != null
                    && roomFor(playerId).isEmpty()) {
                onlinePlayers.add(playerId);
            }
        }
        if (onlinePlayers.size() < MatchmakingManager.MIN_PLAYERS_TO_FORM) {
            matchmaking.requeue(onlinePlayers);
            return;
        }
        String mapId = matchManager.maps().currentMapId();
        if (mapId == null || mapId.isBlank() || "未选择".equals(mapId)
                || matchManager.maps().registry().get(mapId).isEmpty()) {
            matchmaking.requeue(onlinePlayers);
            sendAll("快速匹配暂时没有可用地图，玩家已返回匹配队列。", true);
            return;
        }
        String id = nextId();
        // 匹配房人数无上限（maxPlayers = 0）：中途可通过大厅继续加入。
        Room room = new Room(id, "快速匹配", onlinePlayers.get(0), 0, mapId, true);
        rooms.put(id, room);
        int index = 0;
        for (UUID playerId : onlinePlayers) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                continue;
            }
            if (!room.contains(playerId)) {
                room.add(playerId);
            }
            matchManager.teamManager().setPreference(player, index++ % 2 == 0 ? Team.TEAM_A : Team.TEAM_B);
        }
        matchmaking.beginForming(id);
        startRoom(room);
        if (rooms.containsKey(id)) {
            sendAll("快速匹配已凑满 " + MatchmakingManager.MIN_PLAYERS_TO_FORM + " 人，"
                    + MatchmakingManager.READY_SECONDS + " 秒后开赛，可在大厅查看。", false);
        }
    }

    private void create(ServerPlayer player, String rawName, int requestedMaxPlayers, String rawPassword) {
        if (roomFor(player.getUUID()).isPresent() || matchmaking.queued(player.getUUID())) {
            sendSync(player, "你已经在房间或匹配队列中。", true);
            return;
        }
        if (rooms.size() >= config.values().maxRooms()) {
            sendSync(player, "房间数量已达到服务器上限。", true);
            return;
        }
        int maxPlayers = requestedMaxPlayers <= 0 ? config.values().defaultRoomMaxPlayers()
                : Math.max(2, Math.min(64, requestedMaxPlayers));
        String name = sanitizeName(rawName, "未命名房间");
        String password = sanitizePassword(rawPassword);
        String mapId = matchManager.maps().currentMapId();
        Room room = new Room(nextId(), name, player.getUUID(), maxPlayers,
                "未选择".equals(mapId) ? "" : mapId, false, password);
        room.rules(room.rules().withMinPlayersToStart(config.values().minPlayersToStartRoom()));
        rooms.put(room.id(), room);
        sendAll(player.getGameProfile().getName() + " 创建了房间 " + room.name()
                + (password.isEmpty() ? "" : "（私有）") + "。", false);
    }

    private void join(ServerPlayer player, String roomId, String password) {
        Room target = rooms.get(roomId);
        if (target == null) {
            sendSync(player, "房间不存在。", true);
            return;
        }
        // 匹配比赛房全程开放：准备倒计时可加入，比赛进行中也能通过大厅中途加入。
        boolean joinable = target.state() == RoomState.OPEN
                || (target.matchmaking() && (target.state() == RoomState.COUNTDOWN
                        || target.state() == RoomState.WAITING_MAP
                        || (target.state() == RoomState.RUNNING
                        && roomId.equals(activeRoomId) && matchManager.isMatchActive())));
        if (!joinable) {
            sendSync(player, "房间当前不接受加入。", true);
            return;
        }
        if (roomFor(player.getUUID()).isPresent() || matchmaking.queued(player.getUUID())) {
            sendSync(player, "请先离开当前房间或匹配队列。", true);
            return;
        }
        if (!target.acceptsPassword(password)) {
            sendSync(player, target.locked() ? "房间密码不正确。" : "该房间需要密码。", true);
            return;
        }
        if (!target.add(player.getUUID())) {
            sendSync(player, "房间人数已满。", true);
            return;
        }
        if (target.state() == RoomState.RUNNING) {
            matchManager.joinRoomMember(player);
            sendRoomMessage(target, player.getGameProfile().getName() + " 中途加入了匹配比赛。", false);
            matchmaking.sendAll("匹配队列状态已更新。", false);
        } else {
            sendAll(player.getGameProfile().getName() + " 加入了房间 " + target.name() + "。", false);
        }
    }

    private void leave(ServerPlayer player) {
        Room room = roomFor(player.getUUID()).orElse(null);
        if (room == null) {
            sendSync(player, "你当前不在房间中。", true);
            return;
        }
        if (room.state() == RoomState.RUNNING) {
            matchManager.teamManager().leavePlayer(player);
            matchManager.broadcastMatchState();
        }
        leaveInternal(player.getUUID());
        sendAll("玩家离开了房间。", false);
    }

    private boolean leaveInternal(UUID playerId) {
        Room room = roomFor(playerId).orElse(null);
        if (room == null) {
            return false;
        }
        boolean owner = room.owner().equals(playerId);
        room.remove(playerId);
        if (room.memberCount() == 0) {
            if (room.state() == RoomState.RUNNING && room.id().equals(activeRoomId)) {
                activeRoomId = null;
                matchManager.stopMatch();
            }
            releaseReservation(room);
            rooms.remove(room.id());
            return true;
        }
        if (owner) {
            List<UUID> members = room.members();
            room.owner(members.get(ThreadLocalRandom.current().nextInt(members.size())));
        }
        if (room.state() != RoomState.OPEN && room.state() != RoomState.RUNNING) {
            if (needsCountdownCancellation(room.matchmaking(), canStart(room))) {
                cancelStart(room, "玩家离开，开赛准备已取消。", false);
            }
        }
        return true;
    }

    static boolean needsCountdownCancellation(boolean matchmakingRoom, boolean canStart) {
        return !matchmakingRoom || !canStart;
    }

    private void start(ServerPlayer player) {
        Room room = roomFor(player.getUUID()).orElse(null);
        if (room == null) {
            sendSync(player, "你不在房间中。", true);
            return;
        }
        if (!room.owner().equals(player.getUUID()) && !player.hasPermissions(ADMIN_PERMISSION)) {
            sendSync(player, "只有房主或管理员可以开始房间。", true);
            return;
        }
        if (room.state() != RoomState.OPEN) {
            sendSync(player, "房间当前不能开始。", true);
            return;
        }
        if (matchManager.isMatchActive() || hasRunningRoom()) {
            sendSync(player, "服务器当前已有比赛。", true);
            return;
        }
        if (!canStart(room)) {
            sendSync(player, "人数不足，暂不能开赛。", true);
            return;
        }
        startRoom(room);
    }

    private void startRoom(Room room) {
        if (room.state() == RoomState.RUNNING || matchManager.isMatchActive() || hasOtherReservation(room)) {
            return;
        }
        if (!canStart(room)) {
            cancelStart(room, "人数不足，开赛准备已取消。", false);
            return;
        }
        if (room.state() == RoomState.OPEN) {
            if (!reserveRoom(room)) {
                return;
            }
        }
        if (!prepareMap(room)) {
            cancelStart(room, "地图暂时不可用。", true);
            return;
        }
        if (!matchManager.maps().isReady()) {
            if (room.state() != RoomState.WAITING_MAP) {
                room.state(RoomState.WAITING_MAP);
                sendRoomMessage(room, "正在等待地图快照完成。", false);
            }
            return;
        }
        int seconds = room.matchmaking() ? MatchmakingManager.READY_SECONDS
                : config.values().roomStartCountdownSeconds();
        if (seconds <= 0) {
            launchRoom(room);
        } else {
            beginCountdown(room, seconds);
        }
    }

    private boolean reserveRoom(Room room) {
        if (reservedRoomId != null && !reservedRoomId.equals(room.id())) {
            return false;
        }
        reservedRoomId = room.id();
        return true;
    }

    private void beginCountdown(Room room, int seconds) {
        if (!canStart(room)) {
            cancelStart(room, "人数不足，开赛准备已取消。", false);
            return;
        }
        if (!reserveRoom(room)) {
            room.state(RoomState.OPEN);
            return;
        }
        room.state(RoomState.COUNTDOWN);
        room.countdownEndTick(server.getTickCount() + seconds * 20L);
        sendRoomMessage(room, "房间将在 " + seconds + " 秒后开始。", false);
    }

    private void launchRoom(Room room) {
        if (hasOtherReservation(room) || matchManager.isMatchActive()) {
            return;
        }
        // 房间规则整体覆盖服务器默认规则；比赛结束后由 resetToWaiting 恢复。
        matchManager.applyRoomRules(room.rules());
        MatchManager.StartResult result = matchManager.startMatch(room.members());
        if (result == MatchManager.StartResult.STARTED) {
            releaseReservation(room);
            room.state(RoomState.RUNNING);
            activeRoomId = room.id();
            sendRoomMessage(room, "比赛已启动（" + room.rules().describe() + "）。", false);
        } else {
            matchManager.clearRoomRules();
            cancelStart(room, "比赛启动失败：" + resultText(result), true);
        }
    }

    private boolean prepareMap(Room room) {
        if (room.mapId() == null || room.mapId().isBlank() || "未选择".equals(room.mapId())) {
            sendRoomMessage(room, "房间尚未选择地图。", true);
            room.state(RoomState.OPEN);
            return false;
        }
        MapManager.LoadResult result = matchManager.maps().loadMap(room.mapId());
        if (result == MapManager.LoadResult.NOT_FOUND || result == MapManager.LoadResult.FAILED
                || result == MapManager.LoadResult.BUSY) {
            sendRoomMessage(room, "地图无法加载：" + room.mapId(), true);
            room.state(RoomState.OPEN);
            return false;
        }
        return true;
    }

    private boolean canStart(Room room) {
        int minimum = room.matchmaking() ? MatchmakingManager.MIN_PLAYERS_TO_FORM
                : room.rules().minPlayersToStart();
        return room.memberCount() >= minimum;
    }

    private void setMap(ServerPlayer player, String roomId, String mapId) {
        Room room = roomFor(player.getUUID()).orElse(null);
        if (room == null || room.state() != RoomState.OPEN) {
            sendSync(player, "只有开放中的房间可以选择地图。", true);
            return;
        }
        if (!room.owner().equals(player.getUUID()) && !player.hasPermissions(ADMIN_PERMISSION)) {
            sendSync(player, "只有房主或管理员可以选择地图。", true);
            return;
        }
        if (matchManager.maps().registry().get(mapId).isEmpty()) {
            sendSync(player, "地图不存在：" + mapId, true);
            return;
        }
        if (!config.values().allowOwnerMapSelection() && !player.hasPermissions(ADMIN_PERMISSION)) {
            sendSync(player, "服务器未允许房主选择地图。", true);
            return;
        }
        room.mapId(mapId);
        sendAll("房间地图已切换为 " + mapId + "。", false);
    }

    private void destroy(ServerPlayer player) {
        Room room = roomFor(player.getUUID()).orElse(null);
        if (room == null) {
            sendSync(player, "你不在房间中。", true);
            return;
        }
        if (!room.owner().equals(player.getUUID()) && !player.hasPermissions(ADMIN_PERMISSION)) {
            sendSync(player, "只有房主或管理员可以解散房间。", true);
            return;
        }
        if (room.state() == RoomState.RUNNING) {
            sendSync(player, "比赛进行中不能解散房间。", true);
            return;
        }
        releaseReservation(room);
        rooms.remove(room.id());
        sendAll("房间 " + room.name() + " 已解散。", false);
    }

    private void removeOfflinePlayers() {
        for (Room room : new ArrayList<>(rooms.values())) {
            for (UUID playerId : new ArrayList<>(room.members())) {
                if (server.getPlayerList().getPlayer(playerId) == null) {
                    room.remove(playerId);
                    if (room.owner().equals(playerId) && room.memberCount() > 0) {
                        List<UUID> rest = room.members();
                        room.owner(rest.get(ThreadLocalRandom.current().nextInt(rest.size())));
                    }
                }
            }
            if (room.memberCount() == 0) {
                if (room.state() == RoomState.RUNNING && room.id().equals(activeRoomId)) {
                    activeRoomId = null;
                    matchManager.stopMatch();
                }
                releaseReservation(room);
                rooms.remove(room.id());
            }
        }
    }

    private RoomView toView(Room room) {
        List<String> members = new ArrayList<>();
        for (UUID playerId : room.members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            members.add(player == null ? playerId.toString().substring(0, 8)
                    : player.getGameProfile().getName());
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(room.owner());
        String ownerName = owner == null ? room.owner().toString().substring(0, 8)
                : owner.getGameProfile().getName();
        return new RoomView(room.id(), room.name(), ownerName, room.memberCount(), room.maxPlayers(),
                room.mapId().isBlank() ? "未选择" : room.mapId(), room.state(), members,
                room.rules(), room.matchmaking(), room.locked());
    }

    /**
     * 房主在大厅里修改本房间的比赛规则。需求约定：只有房主本人可以修改，
     * 管理员也不能越权代改（服务器默认规则仍由管理员在服务器配置里调整）。
     */
    public void saveRoomRules(ServerPlayer player, RoomRules rules) {
        if (player == null || rules == null) {
            return;
        }
        Room room = roomFor(player.getUUID()).orElse(null);
        if (room == null) {
            sendSync(player, "你不在任何房间中，无法修改规则。", true);
            return;
        }
        if (room.matchmaking()) {
            sendSync(player, "快速匹配对局使用服务器规则，不能手动修改。", true);
            return;
        }
        if (!room.owner().equals(player.getUUID())) {
            sendSync(player, "只有房主可以修改房间规则。", true);
            return;
        }
        if (room.state() != RoomState.OPEN) {
            sendSync(player, "房间已进入开赛流程，规则锁定；请先等待比赛结束。", true);
            return;
        }
        RoomRules normalized = rules.normalized();
        String error = normalized.validationError();
        if (error != null) {
            sendSync(player, error, true);
            return;
        }
        room.rules(normalized);
        sendRoomMessage(room, "房主更新了房间规则：" + normalized.describe(), false);
    }

    private void sendRoomMessage(Room room, String message, boolean error) {
        for (UUID playerId : room.members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                sendSync(player, message, error);
            }
        }
    }

    private String nextId() {
        String id;
        do {
            id = String.format(java.util.Locale.ROOT, "R%04d", nextRoomNumber++);
        } while (rooms.containsKey(id));
        return id;
    }

    /** 密码只保留可打印字符，最长 16 位；空串表示公开房间。 */
    private static String sanitizePassword(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (char c : value.trim().toCharArray()) {
            if (!Character.isWhitespace(c) && c >= 33 && c <= 126) {
                text.append(c);
            }
            if (text.length() >= 16) {
                break;
            }
        }
        return text.toString();
    }

    private static String sanitizeName(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String name = value.trim().replace('\n', ' ').replace('\r', ' ');
        if (name.isBlank()) {
            return fallback;
        }
        return name.substring(0, Math.min(24, name.length()));
    }

    private static String resultText(MatchManager.StartResult result) {
        return switch (result) {
            case STARTED -> "已开始";
            case ALREADY_ACTIVE -> "已有比赛进行中";
            case NEED_BOTH_TEAMS -> "需要两队都有玩家";
            case NO_PLAYERS -> "没有参赛玩家";
            case NO_END_CONDITION -> "没有设置结束条件";
            case NO_MAP -> "没有地图";
            case MAP_LOADING -> "地图快照仍在捕获";
            case MAP_NOT_READY -> "地图快照不可用";
            case NO_TEAM_SPAWNS -> "两队都需要出生点";
            case MAP_BUSY -> "地图正在恢复";
        };
    }
}
