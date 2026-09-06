package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.lobby.LobbyConfigValues;
import cn.blockforge.generated.generatedmod.lobby.MatchmakingStatus;
import cn.blockforge.generated.generatedmod.lobby.RoomView;
import cn.blockforge.generated.generatedmod.network.packet.LobbyConfigSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.MatchmakingSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.RoomMapListSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.RoomSyncPacket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 房间、匹配和大厅配置的客户端缓存，各界面共享只读同步结果。 */
public final class ClientLobbyData {
    /** 可作为房间用图的一项（含所有玩家制作的地图）。 */
    public record MapOption(String id, String displayName, String ownerName) {
    }

    private static List<RoomView> rooms = List.of();
    private static String ownRoomId = "";
    private static boolean ownOwner;
    private static boolean matchActive;
    private static MatchmakingStatus matchmaking = new MatchmakingStatus(false, 0, 0, 0,
            0, "", false);
    private static LobbyConfigValues config = LobbyConfigValues.defaults();
    private static boolean configCanEdit;
    private static boolean configLocked;
    private static String roomMessage = "";
    private static boolean roomError;
    private static String matchmakingMessage = "";
    private static boolean matchmakingError;
    private static String configMessage = "";
    private static boolean configError;
    private static int revision;
    private static int roomRevision;
    private static int matchmakingRevision;
    private static int configRevision;
    private static List<MapOption> mapOptions = List.of();
    private static final Map<String, String> mapDisplayNames = new LinkedHashMap<>();
    private static int mapOptionsRevision;
    /** 最近一次匹配快照到达客户端的本地 tick，用于在两次同步之间平滑推进计时。 */
    private static long clientTick;
    private static long matchmakingSnapshotTick;

    private ClientLobbyData() {
    }

    public static void apply(RoomSyncPacket packet) {
        rooms = List.copyOf(packet.rooms());
        ownRoomId = packet.ownRoomId();
        ownOwner = packet.ownOwner();
        matchActive = packet.matchActive();
        roomMessage = clean(packet.message());
        roomError = packet.error();
        revision++;
        roomRevision++;
    }

    public static void apply(MatchmakingSyncPacket packet) {
        matchmaking = packet.status();
        matchmakingSnapshotTick = clientTick;
        matchmakingMessage = clean(packet.status().message());
        matchmakingError = packet.status().error();
        revision++;
        matchmakingRevision++;
    }

    public static void apply(LobbyConfigSyncPacket packet) {
        config = packet.values();
        configCanEdit = packet.canEdit();
        configLocked = packet.locked();
        configMessage = clean(packet.message());
        configError = packet.error();
        revision++;
        configRevision++;
    }

    public static void apply(RoomMapListSyncPacket packet) {
        List<MapOption> options = new ArrayList<>();
        mapDisplayNames.clear();
        for (String entry : packet.maps()) {
            String[] parts = entry.split("\\|", -1);
            String id = parts.length > 0 ? parts[0] : "";
            String name = parts.length > 1 && !parts[1].isBlank() ? parts[1] : id;
            String owner = parts.length > 2 ? parts[2] : "";
            if (id.isBlank()) {
                continue;
            }
            options.add(new MapOption(id, name, owner));
            mapDisplayNames.put(id, name);
        }
        mapOptions = List.copyOf(options);
        mapOptionsRevision++;
        revision++;
    }

    /** 地图显示名（客户端缓存的房间用图列表）；未知 id 原样返回。 */
    public static String mapDisplayName(String mapId) {
        if (mapId == null || mapId.isBlank()) {
            return "未选择";
        }
        return mapDisplayNames.getOrDefault(mapId, mapId);
    }

    /** 客户端每 tick 调用一次，让 HUD 在服务器同步间隔内继续走时钟。 */
    public static void tick() {
        clientTick++;
    }

    public static int dynamicWaitedTicks() {
        int extra = matchmaking.queued() ? (int) Math.min(Integer.MAX_VALUE,
                Math.max(0L, clientTick - matchmakingSnapshotTick)) : 0;
        return Math.max(0, matchmaking.waitedTicks() + extra);
    }

    public static int dynamicReadySeconds() {
        if (matchmaking.readySeconds() <= 0) return 0;
        long elapsedTicks = Math.max(0L, clientTick - matchmakingSnapshotTick);
        long remainingTicks = Math.max(0L, matchmaking.readySeconds() * 20L - elapsedTicks);
        return remainingTicks <= 0L ? 0 : (int) Math.max(1L, (remainingTicks + 19L) / 20L);
    }

    public static void clear() {
        rooms = List.of();
        ownRoomId = "";
        ownOwner = false;
        matchActive = false;
        matchmaking = new MatchmakingStatus(false, 0, 0, 0, 0, "", false);
        config = LobbyConfigValues.defaults();
        configCanEdit = false;
        configLocked = false;
        roomMessage = "";
        roomError = false;
        matchmakingMessage = "";
        matchmakingError = false;
        configMessage = "";
        configError = false;
        mapOptions = List.of();
        mapDisplayNames.clear();
        clientTick = 0L;
        matchmakingSnapshotTick = 0L;
        revision++;
        roomRevision++;
        matchmakingRevision++;
        configRevision++;
        mapOptionsRevision++;
    }

    private static String clean(String value) {
        return value == null ? "" : value;
    }

    public static List<RoomView> rooms() { return rooms; }
    public static String ownRoomId() { return ownRoomId; }
    public static boolean ownOwner() { return ownOwner; }
    public static boolean matchActive() { return matchActive; }
    public static MatchmakingStatus matchmaking() { return matchmaking; }
    public static LobbyConfigValues config() { return config; }
    public static boolean configCanEdit() { return configCanEdit; }
    public static boolean configLocked() { return configLocked; }
    public static String roomMessage() { return roomMessage; }
    public static boolean roomError() { return roomError; }
    public static String matchmakingMessage() { return matchmakingMessage; }
    public static boolean matchmakingError() { return matchmakingError; }
    public static String configMessage() { return configMessage; }
    public static boolean configError() { return configError; }
    /** 兼容旧界面调用，新的界面应读取各自领域的消息。 */
    public static String message() { return roomMessage; }
    public static boolean error() { return roomError; }
    public static int revision() { return revision; }
    public static int roomRevision() { return roomRevision; }
    public static int matchmakingRevision() { return matchmakingRevision; }
    public static int configRevision() { return configRevision; }
    public static List<MapOption> mapOptions() { return mapOptions; }
    public static int mapOptionsRevision() { return mapOptionsRevision; }
}
