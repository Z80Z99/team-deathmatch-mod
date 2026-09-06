package cn.blockforge.generated.generatedmod.lobby;

import cn.blockforge.generated.generatedmod.match.MatchManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashMap;

/**
 * 快速匹配队列。匹配参数按产品约定固定，不再提供配置入口：
 * <ul>
 *   <li>最少 6 人即可成局，没有人数上限（整个队列一起进房）；</li>
 *   <li>等待时长无限，不会因超时降格开赛；</li>
 *   <li>条件达成后进入 10 秒准备时间，然后开赛；</li>
 *   <li>成局即开一间匹配房，中途可通过大厅加入，比赛结束后房间自动解散。</li>
 * </ul>
 */
public final class MatchmakingManager {
    /** 成局所需最少人数。 */
    public static final int MIN_PLAYERS_TO_FORM = 6;
    /** 匹配条件达成后的准备时间（秒）。 */
    public static final int READY_SECONDS = 10;

    private final MinecraftServer server;
    private final MatchManager matchManager;
    private final RoomManager rooms;
    private final LinkedHashMap<UUID, Long> queue = new LinkedHashMap<>();
    /** 正在成局（房间倒计时期）的匹配房 id，用于给客户端展示准备倒计时。 */
    private String formingRoomId;
    private long retryNotBeforeTick;

    public MatchmakingManager(MinecraftServer server, MatchManager matchManager, RoomManager rooms) {
        this.server = server;
        this.matchManager = matchManager;
        this.rooms = rooms;
    }

    public void tick() {
        removeUnavailablePlayers();
        advanceFormingCountdown();
        if (queue.size() < MIN_PLAYERS_TO_FORM
                || matchManager.isMatchActive() || rooms.hasRunningRoom()
                || !rooms.roomCapacityAvailable()
                || server.getTickCount() < retryNotBeforeTick) {
            return;
        }
        List<UUID> selected = new ArrayList<>(queue.keySet());
        queue.clear();
        rooms.createMatchmakingRoom(selected);
    }

    /** 准备阶段结束后清理成局标记；开赛与解散由房间管理器接管。 */
    private void advanceFormingCountdown() {
        if (formingRoomId == null) {
            return;
        }
        Room room = rooms.get(formingRoomId).orElse(null);
        if (room == null || !room.matchmaking() || room.state() == RoomState.RUNNING) {
            formingRoomId = null;
        }
    }

    /** 房间管理器在创建匹配房（进入 10 秒准备）时登记。 */
    public void beginForming(String roomId) {
        formingRoomId = roomId;
    }

    public void cancelForming(String roomId) {
        if (roomId != null && roomId.equals(formingRoomId)) {
            formingRoomId = null;
        }
    }

    /** 匹配房准备倒计时的剩余秒数；不在成局阶段返回 0。 */
    public int readySecondsLeft() {
        if (formingRoomId == null) {
            return 0;
        }
        Room room = rooms.get(formingRoomId).orElse(null);
        if (room == null || room.state() != RoomState.COUNTDOWN) {
            return 0;
        }
        long remaining = room.countdownEndTick() - server.getTickCount();
        return remaining <= 0 ? 0 : (int) Math.max(1L, (remaining + 19L) / 20L);
    }

    public boolean join(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (matchManager.isMatchActive() || rooms.hasRunningRoom()
                || rooms.roomFor(player.getUUID()).isPresent()) {
            return false;
        }
        if (queue.containsKey(player.getUUID())) {
            return true;
        }
        queue.put(player.getUUID(), (long) server.getTickCount());
        return true;
    }

    public boolean leave(ServerPlayer player) {
        return queue.remove(player.getUUID()) != null;
    }

    public void remove(UUID playerId) {
        queue.remove(playerId);
    }

    public boolean queued(UUID playerId) {
        return queue.containsKey(playerId);
    }

    /** 成局失败（地图不可用、被外部比赛占用等）时把玩家放回队列继续等待。 */
    public void requeue(List<UUID> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return;
        }
        retryNotBeforeTick = Math.max(retryNotBeforeTick, server.getTickCount() + 20L);
        long now = server.getTickCount();
        for (UUID playerId : playerIds) {
            if (playerId != null && server.getPlayerList().getPlayer(playerId) != null
                    && rooms.roomFor(playerId).isEmpty()) {
                queue.putIfAbsent(playerId, now);
            }
        }
    }

    public MatchmakingStatus status(ServerPlayer player, String message, boolean error) {
        UUID playerId = player.getUUID();
        Long joined = queue.get(playerId);
        int position = 0;
        if (joined != null) {
            for (UUID queuedId : queue.keySet()) {
                position++;
                if (queuedId.equals(playerId)) {
                    break;
                }
            }
        }
        int waited = joined == null ? 0 : (int) Math.min(Integer.MAX_VALUE,
                Math.max(0L, server.getTickCount() - joined));
        int readySeconds = readySecondsLeft();
        boolean inFormingRoom = readySeconds > 0 && rooms.roomFor(playerId)
                .map(room -> room.matchmaking() && room.state() == RoomState.COUNTDOWN)
                .orElse(false);
        return new MatchmakingStatus(joined != null, position, queue.size(), waited,
                (joined != null || inFormingRoom) ? readySeconds : 0, message, error);
    }

    public void sendSync(ServerPlayer player, String message, boolean error) {
        cn.blockforge.generated.generatedmod.network.FpsTdmNetwork.sendToPlayer(
                new cn.blockforge.generated.generatedmod.network.packet.MatchmakingSyncPacket(
                        status(player, message, error)), player);
    }

    public void sendAll(String message, boolean error) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendSync(player, message, error);
        }
    }

    public int queueSize() {
        return queue.size();
    }

    private void removeUnavailablePlayers() {
        Iterator<Map.Entry<UUID, Long>> iterator = queue.entrySet().iterator();
        while (iterator.hasNext()) {
            UUID playerId = iterator.next().getKey();
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null || rooms.roomFor(playerId).isPresent()) {
                iterator.remove();
            }
        }
    }
}
