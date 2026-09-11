package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.match.MatchManager;
import cn.blockforge.generated.generatedmod.shop.MatchShopAction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class MatchShopActionPacket {
    private static final int MAX_TEXT = 96;
    private static final AtomicInteger NEXT_REQUEST_ID = new AtomicInteger();
    private final MatchShopAction action;
    private final String productId;
    private final int requestId;

    public MatchShopActionPacket(MatchShopAction action, String productId) {
        this.action = action;
        this.productId = productId == null ? "" : productId.substring(0, Math.min(MAX_TEXT, productId.length()));
        this.requestId = NEXT_REQUEST_ID.updateAndGet(value -> value >= Integer.MAX_VALUE ? 1 : value + 1);
    }

    public MatchShopActionPacket(FriendlyByteBuf buffer) {
        action = buffer.readEnum(MatchShopAction.class);
        productId = buffer.readUtf(MAX_TEXT);
        requestId = Math.max(0, buffer.readVarInt());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(action == null ? MatchShopAction.REQUEST : action);
        buffer.writeUtf(productId, MAX_TEXT);
        buffer.writeVarInt(requestId);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            MatchManager manager = MatchManager.get();
            if (manager != null && sender != null) {
                manager.matchShop().handleAction(sender, action, productId, requestId);
            }
        });
        context.setPacketHandled(true);
    }
}
