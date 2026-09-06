package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.client.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 服务器向玩家下发“可作为房间用图”的完整列表（含所有玩家制作的地图）。
 * 每条格式为 {@code id|显示名|属主名}，属主名为空表示官方/服务器地图。
 */
public final class RoomMapListSyncPacket {
    private static final int MAX_TEXT = 160;
    private static final int MAX_ENTRIES = 128;
    private final List<String> maps;

    public RoomMapListSyncPacket(List<String> maps) {
        this.maps = List.copyOf(maps == null ? List.of() : maps);
    }

    public RoomMapListSyncPacket(FriendlyByteBuf buffer) {
        int count = Math.min(MAX_ENTRIES, Math.max(0, buffer.readVarInt()));
        List<String> collected = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            collected.add(buffer.readUtf(MAX_TEXT));
        }
        maps = List.copyOf(collected);
    }

    public void encode(FriendlyByteBuf buffer) {
        int count = Math.min(MAX_ENTRIES, maps.size());
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buffer.writeUtf(maps.get(i), MAX_TEXT);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handle(this)));
        context.setPacketHandled(true);
    }

    public List<String> maps() {
        return maps;
    }
}
