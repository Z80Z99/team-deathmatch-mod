package cn.blockforge.generated.generatedmod.lobby;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 房间大厅的服务器权威配置快照。
 *
 * <p>匹配参数不再可配置：固定为“最少 6 人成局、无人数上限、等待时长无限、
 * 达成后 30 秒准备时间”，房间也不再提供“要求准备”选项。
 */
public record LobbyConfigValues(
        int maxRooms,
        int defaultRoomMaxPlayers,
        int minPlayersToStartRoom,
        boolean allowOwnerMapSelection,
        int roomStartCountdownSeconds) {

    public static LobbyConfigValues defaults() {
        return new LobbyConfigValues(16, 16, 2, true, 30);
    }

    public String validationError() {
        String error;
        if ((error = range("最大房间数", maxRooms, 1, 64)) != null) return error;
        if ((error = range("房间最大人数", defaultRoomMaxPlayers, 2, 64)) != null) return error;
        if ((error = range("房间最少开赛人数", minPlayersToStartRoom, 1, defaultRoomMaxPlayers)) != null) return error;
        if ((error = range("房间倒计时", roomStartCountdownSeconds, 0, 60)) != null) return error;
        return null;
    }

    private static String range(String name, int value, int minimum, int maximum) {
        return value < minimum || value > maximum
                ? name + "必须在 " + minimum + " 到 " + maximum + " 之间。"
                : null;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(maxRooms);
        buffer.writeVarInt(defaultRoomMaxPlayers);
        buffer.writeVarInt(minPlayersToStartRoom);
        buffer.writeBoolean(allowOwnerMapSelection);
        buffer.writeVarInt(roomStartCountdownSeconds);
    }

    public static LobbyConfigValues read(FriendlyByteBuf buffer) {
        return new LobbyConfigValues(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readVarInt());
    }

    public Mutable mutable() {
        return new Mutable(this);
    }

    public static final class Mutable {
        public int maxRooms;
        public int defaultRoomMaxPlayers;
        public int minPlayersToStartRoom;
        public boolean allowOwnerMapSelection;
        public int roomStartCountdownSeconds;

        public Mutable(LobbyConfigValues values) {
            maxRooms = values.maxRooms;
            defaultRoomMaxPlayers = values.defaultRoomMaxPlayers;
            minPlayersToStartRoom = values.minPlayersToStartRoom;
            allowOwnerMapSelection = values.allowOwnerMapSelection;
            roomStartCountdownSeconds = values.roomStartCountdownSeconds;
        }

        public LobbyConfigValues build() {
            return new LobbyConfigValues(maxRooms, defaultRoomMaxPlayers, minPlayersToStartRoom,
                    allowOwnerMapSelection, roomStartCountdownSeconds);
        }
    }
}
