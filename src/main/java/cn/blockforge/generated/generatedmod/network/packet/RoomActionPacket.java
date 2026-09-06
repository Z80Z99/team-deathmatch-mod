package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.lobby.RoomAction;
import cn.blockforge.generated.generatedmod.match.MatchManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端操作房间大厅。 */
public final class RoomActionPacket {
    private static final int MAX_TEXT = 64;
    private final RoomAction action;
    private final String roomId;
    private final String roomName;
    private final String mapId;
    private final int maxPlayers;
    /** 创建房间时的访问密码，或加入密码房时提交的密码。 */
    private final String password;

    public RoomActionPacket(RoomAction action, String roomId, String roomName, String mapId,
                            int maxPlayers, String password) {
        this.action = action;
        this.roomId = limit(roomId);
        this.roomName = limit(roomName);
        this.mapId = limit(mapId);
        this.maxPlayers = maxPlayers;
        this.password = limit(password);
    }

    public RoomActionPacket(FriendlyByteBuf buffer) {
        action = readEnum(buffer, RoomAction.class);
        roomId = buffer.readUtf(MAX_TEXT);
        roomName = buffer.readUtf(MAX_TEXT);
        mapId = buffer.readUtf(MAX_TEXT);
        maxPlayers = buffer.readVarInt();
        password = buffer.readUtf(MAX_TEXT);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(action == null ? -1 : action.ordinal());
        buffer.writeUtf(roomId, MAX_TEXT);
        buffer.writeUtf(roomName, MAX_TEXT);
        buffer.writeUtf(mapId, MAX_TEXT);
        buffer.writeVarInt(maxPlayers);
        buffer.writeUtf(password, MAX_TEXT);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            MatchManager manager = MatchManager.get();
            if (manager != null && sender != null) {
                manager.rooms().handleAction(sender, action, roomId, roomName, mapId, maxPlayers, password);
            }
        });
        context.setPacketHandled(true);
    }

    public RoomAction action() { return action; }
    public String roomId() { return roomId; }
    public String password() { return password; }

    private static <E extends Enum<E>> E readEnum(FriendlyByteBuf buffer, Class<E> type) {
        int ordinal = buffer.readVarInt();
        E[] values = type.getEnumConstants();
        return ordinal < 0 || ordinal >= values.length ? null : values[ordinal];
    }

    private static String limit(String value) {
        if (value == null) {
            return "";
        }
        return value.substring(0, Math.min(MAX_TEXT, value.length()));
    }
}
