package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.client.network.ClientPacketHandler;
import cn.blockforge.generated.generatedmod.shop.MatchShopProduct;
import cn.blockforge.generated.generatedmod.shop.MatchShopService;
import cn.blockforge.generated.generatedmod.shop.MatchShopView;
import cn.blockforge.generated.generatedmod.weapon.WeaponSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class MatchShopSyncPacket {
    private static final int MAX_TEXT = 256;
    private static final int MAX_PRODUCTS = 512;
    private final MatchShopView view;

    public MatchShopSyncPacket(MatchShopView view) {
        this.view = view;
    }

    public MatchShopSyncPacket(FriendlyByteBuf buffer) {
        boolean open = buffer.readBoolean();
        int buySecondsRemaining = buffer.readVarInt();
        int matchBalance = buffer.readVarInt();
        boolean gundbLoaded = buffer.readBoolean();
        int gunDamage = buffer.readVarInt();
        int gunMaxDamage = buffer.readVarInt();
        int attachmentDamage = buffer.readVarInt();
        int productCount = Math.max(0, Math.min(MAX_PRODUCTS, buffer.readVarInt()));
        List<MatchShopProduct> products = new ArrayList<>(productCount);
        for (int i = 0; i < productCount; i++) {
            String id = buffer.readUtf(MAX_TEXT);
            String category = buffer.readUtf(MAX_TEXT);
            String title = buffer.readUtf(MAX_TEXT);
            int price = buffer.readVarInt();
            int serviceOrdinal = buffer.readVarInt();
            MatchShopService service = serviceOrdinal < 0 || serviceOrdinal >= MatchShopService.values().length
                    ? null : MatchShopService.values()[serviceOrdinal];
            WeaponSnapshot snapshot = WeaponSnapshot.from(buffer.readItem());
            products.add(new MatchShopProduct(id, category, title, price, snapshot, service));
        }
        String message = buffer.readUtf(MAX_TEXT);
        boolean error = buffer.readBoolean();
        int requestId = buffer.readVarInt();
        view = new MatchShopView(open, buySecondsRemaining, matchBalance, gundbLoaded,
                gunDamage, gunMaxDamage, attachmentDamage, products, message, error, requestId);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(view.open());
        buffer.writeVarInt(view.buySecondsRemaining());
        buffer.writeVarInt(view.matchBalance());
        buffer.writeBoolean(view.gundbLoaded());
        buffer.writeVarInt(view.gunDamage());
        buffer.writeVarInt(view.gunMaxDamage());
        buffer.writeVarInt(view.attachmentDamage());
        int count = Math.min(MAX_PRODUCTS, view.products().size());
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            MatchShopProduct product = view.products().get(i);
            buffer.writeUtf(product.id(), MAX_TEXT);
            buffer.writeUtf(product.categoryId(), MAX_TEXT);
            buffer.writeUtf(product.title(), MAX_TEXT);
            buffer.writeVarInt(product.price());
            buffer.writeVarInt(product.service() == null ? -1 : product.service().ordinal());
            buffer.writeItem(product.snapshot() == null ? net.minecraft.world.item.ItemStack.EMPTY
                    : product.snapshot().stack());
        }
        buffer.writeUtf(view.message(), MAX_TEXT);
        buffer.writeBoolean(view.error());
        buffer.writeVarInt(view.requestId());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handle(this)));
        context.setPacketHandled(true);
    }

    public MatchShopView view() {
        return view;
    }
}
