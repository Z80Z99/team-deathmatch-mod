package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.match.MatchManager;
import cn.blockforge.generated.generatedmod.weapon.WeaponRepositoryAction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class WeaponRepositoryActionPacket {
    private static final int MAX_TEXT = 96;
    private static final AtomicInteger NEXT_REQUEST_ID = new AtomicInteger();
    private final WeaponRepositoryAction action;
    private final String catalogId;
    private final String entryId;
    private final int requestId;

    public WeaponRepositoryActionPacket(WeaponRepositoryAction action, String catalogId, String entryId) {
        this.action = action;
        this.catalogId = limit(catalogId);
        this.entryId = limit(entryId);
        this.requestId = nextRequestId();
    }

    public WeaponRepositoryActionPacket(FriendlyByteBuf buffer) {
        action = buffer.readEnum(WeaponRepositoryAction.class);
        catalogId = buffer.readUtf(MAX_TEXT);
        entryId = buffer.readUtf(MAX_TEXT);
        requestId = Math.max(0, buffer.readVarInt());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(action == null ? WeaponRepositoryAction.REQUEST : action);
        buffer.writeUtf(catalogId, MAX_TEXT);
        buffer.writeUtf(entryId, MAX_TEXT);
        buffer.writeVarInt(requestId);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            MatchManager manager = MatchManager.get();
            if (manager != null && sender != null) {
                manager.weapons().handleAction(sender, action, catalogId, entryId, requestId);
            }
        });
        context.setPacketHandled(true);
    }

    public int requestId() {
        return requestId;
    }

    private static int nextRequestId() {
        return NEXT_REQUEST_ID.updateAndGet(current -> current >= Integer.MAX_VALUE ? 1 : current + 1);
    }

    private static String limit(String value) {
        return value == null ? "" : value.substring(0, Math.min(MAX_TEXT, value.length()));
    }
}
