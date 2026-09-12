package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.client.network.ClientPacketHandler;
import cn.blockforge.generated.generatedmod.map.MapDefinition;
import cn.blockforge.generated.generatedmod.map.MapRegion;
import cn.blockforge.generated.generatedmod.map.MapRegionAction;
import cn.blockforge.generated.generatedmod.match.MatchManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端提交规划器选中的区域或区域属性修改。 */
public final class MapRegionPacket {
    private static final int MAX_TEXT = 256;
    private final MapRegionAction action;
    private final MapRegion region;
    private final int requestId;

    public MapRegionPacket(MapRegionAction action, MapRegion region, int requestId) {
        this.action = action == null ? MapRegionAction.SELECT : action;
        this.region = region;
        this.requestId = Math.max(0, requestId);
    }

    public MapRegionPacket(FriendlyByteBuf buffer) {
        action = MapRegionAction.valueOf(buffer.readUtf(MAX_TEXT));
        region = buffer.readBoolean() ? readRegion(buffer) : null;
        requestId = Math.max(0, buffer.readVarInt());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(action.name(), MAX_TEXT);
        buffer.writeBoolean(region != null);
        if (region != null) writeRegion(buffer, region);
        buffer.writeVarInt(requestId);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            MatchManager manager = MatchManager.get();
            if (manager != null && sender != null) {
                manager.mapEditor().handleRegion(sender, action, region, requestId);
            }
        });
        context.setPacketHandled(true);
    }

    public MapRegion region() { return region; }
    public MapRegionAction action() { return action; }
    public int requestId() { return requestId; }

    public static void writeRegion(FriendlyByteBuf buffer, MapRegion region) {
        buffer.writeUtf(region.id(), MAX_TEXT);
        buffer.writeUtf(region.displayName(), MAX_TEXT);
        buffer.writeUtf(region.type().id(), MAX_TEXT);
        buffer.writeInt(region.region().min().getX());
        buffer.writeInt(region.region().min().getY());
        buffer.writeInt(region.region().min().getZ());
        buffer.writeInt(region.region().max().getX());
        buffer.writeInt(region.region().max().getY());
        buffer.writeInt(region.region().max().getZ());
        buffer.writeVarInt(region.region().parts().size());
        for (var part : region.region().parts()) {
            buffer.writeBlockPos(part.min());
            buffer.writeBlockPos(part.max());
        }
        buffer.writeBoolean(region.visibleInMatch());
        buffer.writeVarInt(region.displayRange());
        buffer.writeUtf(region.appearance().id(), MAX_TEXT);
        buffer.writeUtf(region.activation().id(), MAX_TEXT);
        buffer.writeUtf(region.activationValue(), MAX_TEXT);
        buffer.writeInt(region.color());
        buffer.writeBoolean(region.outline());
        buffer.writeBoolean(region.fill());
        buffer.writeVarInt(region.priority());
        buffer.writeUtf(region.notes(), MAX_TEXT);
    }

    public static MapRegion readRegion(FriendlyByteBuf buffer) {
        String id = buffer.readUtf(MAX_TEXT);
        String displayName = buffer.readUtf(MAX_TEXT);
        MapRegion.Type type = MapRegion.Type.parse(buffer.readUtf(MAX_TEXT));
        BlockPos min = new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
        BlockPos max = new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
        int count = buffer.readVarInt();
        if (count < 0 || count > 1024) throw new IllegalArgumentException("Invalid region part count");
        var parts = new java.util.ArrayList<MapDefinition.Region>();
        for (int i = 0; i < count; i++) parts.add(new MapDefinition.Region(buffer.readBlockPos(), buffer.readBlockPos()));
        var shape = parts.isEmpty() ? new MapDefinition.Region(min, max) : MapDefinition.Region.composite(parts);
        return new MapRegion(id, displayName, type, shape,
                buffer.readBoolean(), buffer.readVarInt(),
                MapRegion.Appearance.parse(buffer.readUtf(MAX_TEXT)),
                MapRegion.Activation.parse(buffer.readUtf(MAX_TEXT)),
                buffer.readUtf(MAX_TEXT), buffer.readInt(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readVarInt(), buffer.readUtf(MAX_TEXT));
    }
}
