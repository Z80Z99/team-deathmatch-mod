package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.map.MapEditorAction;
import cn.blockforge.generated.generatedmod.match.MatchManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/** 客户端提交地图规划动作，坐标由服务端从玩家当前位置读取。 */
public final class MapEditorActionPacket {
    private static final int MAX_TEXT = 64;
    private static final AtomicInteger NEXT_REQUEST_ID = new AtomicInteger();
    private final MapEditorAction action;
    private final String mapId;
    private final String displayName;
    private final int requestId;

    public MapEditorActionPacket(MapEditorAction action, String mapId) {
        this(action, mapId, "");
    }

    public MapEditorActionPacket(MapEditorAction action, String mapId, String displayName) {
        this(action, mapId, displayName, nextRequestId());
    }

    private MapEditorActionPacket(MapEditorAction action, String mapId, String displayName, int requestId) {
        this.action = action;
        this.mapId = limit(mapId);
        this.displayName = limit(displayName);
        this.requestId = Math.max(0, requestId);
    }

    public MapEditorActionPacket(FriendlyByteBuf buffer) {
        int ordinal = buffer.readVarInt();
        MapEditorAction[] values = MapEditorAction.values();
        action = ordinal < 0 || ordinal >= values.length ? null : values[ordinal];
        mapId = buffer.readUtf(MAX_TEXT);
        displayName = buffer.readUtf(MAX_TEXT);
        requestId = Math.max(0, buffer.readVarInt());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(action == null ? -1 : action.ordinal());
        buffer.writeUtf(mapId, MAX_TEXT);
        buffer.writeUtf(displayName, MAX_TEXT);
        buffer.writeVarInt(requestId);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            MatchManager manager = MatchManager.get();
            if (manager != null && sender != null) {
                manager.mapEditor().handleAction(sender, action, mapId, displayName, requestId);
            }
        });
        context.setPacketHandled(true);
    }

    public int requestId() {
        return requestId;
    }

    private static int nextRequestId() {
        return NEXT_REQUEST_ID.updateAndGet(current -> current >= Integer.MAX_VALUE ? 1 : current + 1);
    }

    private static String limit(String value) {
        if (value == null) {
            return "";
        }
        return value.substring(0, Math.min(MAX_TEXT, value.length()));
    }
}
