package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.client.network.ClientPacketHandler;
import cn.blockforge.generated.generatedmod.lobby.MatchmakingStatus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务器同步快速匹配队列状态。 */
public final class MatchmakingSyncPacket {
    private static final int MAX_TEXT = 128;
    private final MatchmakingStatus status;

    public MatchmakingSyncPacket(MatchmakingStatus status) {
        this.status = status;
    }

    public MatchmakingSyncPacket(FriendlyByteBuf buffer) {
        status = new MatchmakingStatus(buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(MAX_TEXT), buffer.readBoolean());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(status.queued());
        buffer.writeVarInt(status.position());
        buffer.writeVarInt(status.queueSize());
        buffer.writeVarInt(status.waitedTicks());
        buffer.writeVarInt(status.readySeconds());
        buffer.writeUtf(status.message(), MAX_TEXT);
        buffer.writeBoolean(status.error());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handle(this)));
        context.setPacketHandled(true);
    }

    public MatchmakingStatus status() { return status; }
}
