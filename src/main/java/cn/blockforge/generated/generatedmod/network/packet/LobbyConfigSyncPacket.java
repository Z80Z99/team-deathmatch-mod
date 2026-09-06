package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.client.network.ClientPacketHandler;
import cn.blockforge.generated.generatedmod.lobby.LobbyConfigValues;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务器同步房间和匹配配置。 */
public final class LobbyConfigSyncPacket {
    private static final int MAX_TEXT = 256;
    private final LobbyConfigValues values;
    private final boolean canEdit;
    private final boolean locked;
    private final String message;
    private final boolean error;
    public LobbyConfigSyncPacket(LobbyConfigValues values, boolean canEdit, boolean locked,
                                 String message, boolean error) {
        this.values = values;
        this.canEdit = canEdit;
        this.locked = locked;
        this.message = message == null ? "" : message;
        this.error = error;
    }
    public LobbyConfigSyncPacket(FriendlyByteBuf buffer) {
        values = LobbyConfigValues.read(buffer);
        canEdit = buffer.readBoolean();
        locked = buffer.readBoolean();
        message = buffer.readUtf(MAX_TEXT);
        error = buffer.readBoolean();
    }
    public void encode(FriendlyByteBuf buffer) {
        values.write(buffer);
        buffer.writeBoolean(canEdit);
        buffer.writeBoolean(locked);
        buffer.writeUtf(message, MAX_TEXT);
        buffer.writeBoolean(error);
    }
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handle(this)));
        context.setPacketHandled(true);
    }
    public LobbyConfigValues values() { return values; }
    public boolean canEdit() { return canEdit; }
    public boolean locked() { return locked; }
    public String message() { return message; }
    public boolean error() { return error; }
}
