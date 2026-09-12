package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.match.MatchManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client-authoritative hold state for left/right-click bomb interactions. */
public final class BombInteractPacket {
    private final boolean held;

    public BombInteractPacket(boolean held) {
        this.held = held;
    }

    public BombInteractPacket(FriendlyByteBuf buffer) {
        held = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(held);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            MatchManager manager = MatchManager.get();
            if (player != null && manager != null) manager.bomb().setInteractionHeld(player, held);
        });
        context.setPacketHandled(true);
    }
}
