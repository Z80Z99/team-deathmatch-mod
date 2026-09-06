package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.client.ClientHudDynamicStats;
import cn.blockforge.generated.generatedmod.client.network.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 自定义统计同步包（服务器 → 客户端）：每项一行
 * {@code 标识\u0001显示名\u0001数值\u0001上限}，整表替换客户端缓存。
 */
public final class HudStatSyncPacket {
    private final List<String> entries;

    public HudStatSyncPacket(List<String> entries) {
        this.entries = List.copyOf(entries);
    }

    public HudStatSyncPacket(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<String> read = new ArrayList<>(Math.max(0, count));
        for (int index = 0; index < count; index++) {
            read.add(buffer.readUtf(256));
        }
        entries = List.copyOf(read);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entries.size());
        for (String entry : entries) {
            buffer.writeUtf(entry, 256);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handle(this)));
        context.setPacketHandled(true);
    }

    public List<String> entries() {
        return entries;
    }
}
