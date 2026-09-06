package cn.blockforge.generated.generatedmod.lobby;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 一间临时房间；房间数据只存在当前服务器会话中。
 *
 * <p>房间采用“人数即条件”的开赛模型，不再维护准备状态；
 * {@code maxPlayers <= 0} 表示人数无上限（匹配比赛房使用）。
 */
public final class Room {
    private final String id;
    private String name;
    private UUID owner;
    private final int maxPlayers;
    private String mapId;
    private final boolean matchmaking;
    /** 房间访问密码；空串表示公开房间。只存服务端，绝不下发。 */
    private String password = "";
    private final LinkedHashSet<UUID> members = new LinkedHashSet<>();
    private RoomState state = RoomState.OPEN;
    private long countdownEndTick;
    private RoomRules rules;

    public Room(String id, String name, UUID owner, int maxPlayers, String mapId, boolean matchmaking) {
        this(id, name, owner, maxPlayers, mapId, matchmaking, "");
    }

    public Room(String id, String name, UUID owner, int maxPlayers, String mapId,
                boolean matchmaking, String password) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.maxPlayers = maxPlayers;
        this.mapId = mapId == null ? "" : mapId;
        this.matchmaking = matchmaking;
        this.password = password == null ? "" : password;
        this.rules = RoomRules.serverDefaults();
        members.add(owner);
    }

    public boolean locked() {
        return !password.isEmpty();
    }

    public String password() {
        return password;
    }

    public boolean acceptsPassword(String candidate) {
        return password.isEmpty() || password.equals(candidate == null ? "" : candidate.trim());
    }

    public RoomRules rules() {
        return rules;
    }

    public void rules(RoomRules rules) {
        this.rules = rules == null ? RoomRules.serverDefaults() : rules.normalized();
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        this.name = name;
    }

    public UUID owner() {
        return owner;
    }

    public void owner(UUID owner) {
        this.owner = owner;
    }

    public int maxPlayers() {
        return maxPlayers;
    }

    public boolean unlimitedCapacity() {
        return maxPlayers <= 0;
    }

    public String mapId() {
        return mapId;
    }

    public void mapId(String mapId) {
        this.mapId = mapId == null ? "" : mapId;
    }

    public boolean matchmaking() {
        return matchmaking;
    }

    public List<UUID> members() {
        return List.copyOf(members);
    }

    public boolean contains(UUID playerId) {
        return members.contains(playerId);
    }

    public boolean add(UUID playerId) {
        if (!unlimitedCapacity() && members.size() >= maxPlayers) {
            return false;
        }
        return members.add(playerId);
    }

    public boolean remove(UUID playerId) {
        return members.remove(playerId);
    }

    public int memberCount() {
        return members.size();
    }

    public RoomState state() {
        return state;
    }

    public void state(RoomState state) {
        this.state = state;
    }

    public long countdownEndTick() {
        return countdownEndTick;
    }

    public void countdownEndTick(long countdownEndTick) {
        this.countdownEndTick = countdownEndTick;
    }
}
