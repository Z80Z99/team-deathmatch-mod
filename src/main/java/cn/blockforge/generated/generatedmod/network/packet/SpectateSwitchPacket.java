package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.match.MatchManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SpectateSwitchPacket {
    private final boolean next;

    public SpectateSwitchPacket(boolean next) {
        this.next = next;
    }

    public SpectateSwitchPacket(FriendlyByteBuf buffer) {
        next = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(next);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            MatchManager manager = MatchManager.get();
            if (player != null && manager != null) manager.switchSpectatorTarget(player, next);
        });
        context.setPacketHandled(true);
    }
}
