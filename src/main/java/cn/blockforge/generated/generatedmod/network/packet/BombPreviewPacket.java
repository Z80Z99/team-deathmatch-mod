package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.client.network.ClientPacketHandler;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server-validated C4 placement preview for the carrying player. */
public record BombPreviewPacket(boolean present, double x, double y, double z,
                                Direction face, boolean valid) {
    public BombPreviewPacket(FriendlyByteBuf buffer) {
        this(buffer.readBoolean(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                Direction.from3DDataValue(buffer.readVarInt()), buffer.readBoolean());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(present);
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
        buffer.writeVarInt(face == null ? Direction.UP.get3DDataValue() : face.get3DDataValue());
        buffer.writeBoolean(valid);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handle(this)));
        context.setPacketHandled(true);
    }
}
