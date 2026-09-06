package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.client.network.ClientPacketHandler;
import cn.blockforge.generated.generatedmod.lobby.RoomRules;
import cn.blockforge.generated.generatedmod.lobby.RoomState;
import cn.blockforge.generated.generatedmod.lobby.RoomView;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** 服务器同步房间列表和当前玩家的房间状态。 */
public final class RoomSyncPacket {
    /** 房间规则摘要需要较长消息文本，从 64 放宽到 128。 */
    private static final int MAX_TEXT = 128;
    private static final int MAX_ROOMS = 64;
    private static final int MAX_MEMBERS = 64;
    private final List<RoomView> rooms;
    private final String ownRoomId;
    private final boolean ownOwner;
    private final boolean matchActive;
    private final String message;
    private final boolean error;

    public RoomSyncPacket(List<RoomView> rooms, String ownRoomId, boolean ownOwner,
                          boolean matchActive, String message, boolean error) {
        this.rooms = List.copyOf(rooms == null ? List.of() : rooms);
        this.ownRoomId = limit(ownRoomId);
        this.ownOwner = ownOwner;
        this.matchActive = matchActive;
        this.message = limit(message);
        this.error = error;
    }

    public RoomSyncPacket(FriendlyByteBuf buffer) {
        int count = Math.min(MAX_ROOMS, Math.max(0, buffer.readVarInt()));
        List<RoomView> loaded = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String id = buffer.readUtf(MAX_TEXT);
            String name = buffer.readUtf(MAX_TEXT);
            String owner = buffer.readUtf(MAX_TEXT);
            int memberCount = buffer.readVarInt();
            int maxPlayers = buffer.readVarInt();
            String mapId = buffer.readUtf(MAX_TEXT);
            RoomState state = readEnum(buffer, RoomState.class);
            int memberSize = Math.min(MAX_MEMBERS, Math.max(0, buffer.readVarInt()));
            List<String> members = new ArrayList<>(memberSize);
            for (int member = 0; member < memberSize; member++) {
                members.add(buffer.readUtf(MAX_TEXT));
            }
            loaded.add(new RoomView(id, name, owner, memberCount, maxPlayers,
                    mapId, state == null ? RoomState.OPEN : state, members,
                    RoomRules.read(buffer), buffer.readBoolean(), buffer.readBoolean()));
        }
        rooms = List.copyOf(loaded);
        ownRoomId = buffer.readUtf(MAX_TEXT);
        ownOwner = buffer.readBoolean();
        matchActive = buffer.readBoolean();
        message = buffer.readUtf(MAX_TEXT);
        error = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        int count = Math.min(MAX_ROOMS, rooms.size());
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            RoomView room = rooms.get(i);
            buffer.writeUtf(limit(room.id()), MAX_TEXT);
            buffer.writeUtf(limit(room.name()), MAX_TEXT);
            buffer.writeUtf(limit(room.owner()), MAX_TEXT);
            buffer.writeVarInt(Math.max(0, room.memberCount()));
            buffer.writeVarInt(Math.max(0, room.maxPlayers()));
            buffer.writeUtf(limit(room.mapId()), MAX_TEXT);
            buffer.writeVarInt(room.state() == null ? -1 : room.state().ordinal());
            int memberCount = Math.min(MAX_MEMBERS, room.members().size());
            buffer.writeVarInt(memberCount);
            for (int member = 0; member < memberCount; member++) {
                buffer.writeUtf(limit(room.members().get(member)), MAX_TEXT);
            }
            (room.rules() == null ? RoomRules.fallback() : room.rules()).write(buffer);
            buffer.writeBoolean(room.matchmaking());
            buffer.writeBoolean(room.locked());
        }
        buffer.writeUtf(ownRoomId, MAX_TEXT);
        buffer.writeBoolean(ownOwner);
        buffer.writeBoolean(matchActive);
        buffer.writeUtf(message, MAX_TEXT);
        buffer.writeBoolean(error);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handle(this)));
        context.setPacketHandled(true);
    }

    public List<RoomView> rooms() { return rooms; }
    public String ownRoomId() { return ownRoomId; }
    public boolean ownOwner() { return ownOwner; }
    public boolean matchActive() { return matchActive; }
    public String message() { return message; }
    public boolean error() { return error; }

    private static <E extends Enum<E>> E readEnum(FriendlyByteBuf buffer, Class<E> type) {
        int ordinal = buffer.readVarInt();
        E[] values = type.getEnumConstants();
        return ordinal < 0 || ordinal >= values.length ? null : values[ordinal];
    }

    private static String limit(String value) {
        if (value == null) return "";
        return value.substring(0, Math.min(MAX_TEXT, value.length()));
    }
}
