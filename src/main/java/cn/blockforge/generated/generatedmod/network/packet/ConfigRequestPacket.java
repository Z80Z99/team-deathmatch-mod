package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.config.FpsTdmConfigController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端请求服务器当前配置。 */
public final class ConfigRequestPacket {
    public ConfigRequestPacket() {
    }

    public ConfigRequestPacket(FriendlyByteBuf buffer) {
    }

    public void encode(FriendlyByteBuf buffer) {
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> FpsTdmConfigController.sendCurrent(sender, "", false));
        context.setPacketHandled(true);
    }
}
