package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.client.network.ClientPacketHandler;
import cn.blockforge.generated.generatedmod.config.FpsTdmConfigValues;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务器向客户端发送当前有效配置及编辑权限。 */
public final class ConfigSyncPacket {
    private static final int MAX_MESSAGE_LENGTH = 256;

    private final FpsTdmConfigValues values;
    private final boolean canEdit;
    private final boolean configLocked;
    private final String message;
    private final boolean error;

    public ConfigSyncPacket(FpsTdmConfigValues values, boolean canEdit, boolean configLocked,
                            String message, boolean error) {
        this.values = values;
        this.canEdit = canEdit;
        this.configLocked = configLocked;
        this.message = message == null ? "" : message;
        this.error = error;
    }

    public ConfigSyncPacket(FriendlyByteBuf buffer) {
        values = FpsTdmConfigValues.read(buffer);
        canEdit = buffer.readBoolean();
        configLocked = buffer.readBoolean();
        message = buffer.readUtf(MAX_MESSAGE_LENGTH);
        error = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        values.write(buffer);
        buffer.writeBoolean(canEdit);
        buffer.writeBoolean(configLocked);
        buffer.writeUtf(message, MAX_MESSAGE_LENGTH);
        buffer.writeBoolean(error);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handle(this)));
        context.setPacketHandled(true);
    }

    public FpsTdmConfigValues values() {
        return values;
    }

    public boolean canEdit() {
        return canEdit;
    }

    public boolean configLocked() {
        return configLocked;
    }

    public String message() {
        return message;
    }

    public boolean error() {
        return error;
    }
}
