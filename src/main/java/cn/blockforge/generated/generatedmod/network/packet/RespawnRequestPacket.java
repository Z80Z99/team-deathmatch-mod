package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.match.MatchManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Requests a ready downed player to return; the server validates every condition. */
public final class RespawnRequestPacket {
    public RespawnRequestPacket() { }
    public RespawnRequestPacket(FriendlyByteBuf buffer) { }
    public void encode(FriendlyByteBuf buffer) { }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            MatchManager manager = MatchManager.get();
            if (manager != null && sender != null) manager.requestReadyRespawn(sender);
        });
        context.setPacketHandled(true);
    }
}
