package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.client.network.ClientPacketHandler;
import cn.blockforge.generated.generatedmod.weapon.WeaponCatalogItem;
import cn.blockforge.generated.generatedmod.weapon.WeaponCategory;
import cn.blockforge.generated.generatedmod.weapon.WeaponKind;
import cn.blockforge.generated.generatedmod.weapon.WeaponRepositoryItem;
import cn.blockforge.generated.generatedmod.weapon.WeaponRepositoryView;
import cn.blockforge.generated.generatedmod.weapon.WeaponSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class WeaponRepositorySyncPacket {
    private static final int MAX_TEXT = 256;
    private static final int MAX_CATEGORIES = 32;
    private static final int MAX_CATALOG = 1024;
    private static final int MAX_REPOSITORY = 512;
    private final WeaponRepositoryView view;

    public WeaponRepositorySyncPacket(WeaponRepositoryView view) {
        this.view = view;
    }

    public WeaponRepositorySyncPacket(FriendlyByteBuf buffer) {
        boolean taczLoaded = buffer.readBoolean();
        List<WeaponCategory> categories = readCategories(buffer);
        List<WeaponCatalogItem> catalog = readCatalog(buffer);
        List<WeaponRepositoryItem> repository = readRepository(buffer);
        String message = buffer.readUtf(MAX_TEXT);
        boolean error = buffer.readBoolean();
        int requestId = Math.max(0, buffer.readVarInt());
        view = new WeaponRepositoryView(taczLoaded, categories, catalog, repository,
                message, error, requestId);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(view.taczLoaded());
        writeCategories(buffer, view.categories());
        writeCatalog(buffer, view.catalog());
        writeRepository(buffer, view.repository());
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

    public WeaponRepositoryView view() {
        return view;
    }

    private static void writeCategories(FriendlyByteBuf buffer, List<WeaponCategory> values) {
        int count = Math.min(MAX_CATEGORIES, values == null ? 0 : values.size());
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            WeaponCategory value = values.get(i);
            buffer.writeUtf(value.id(), MAX_TEXT);
            buffer.writeUtf(value.name(), MAX_TEXT);
            buffer.writeEnum(value.kind());
        }
    }

    private static List<WeaponCategory> readCategories(FriendlyByteBuf buffer) {
        int count = clampCount(buffer.readVarInt(), MAX_CATEGORIES);
        List<WeaponCategory> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values.add(new WeaponCategory(buffer.readUtf(MAX_TEXT), buffer.readUtf(MAX_TEXT),
                    buffer.readEnum(WeaponKind.class)));
        }
        return values;
    }

    private static void writeCatalog(FriendlyByteBuf buffer, List<WeaponCatalogItem> values) {
        int count = Math.min(MAX_CATALOG, values == null ? 0 : values.size());
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            WeaponCatalogItem value = values.get(i);
            buffer.writeUtf(value.catalogId(), MAX_TEXT);
            buffer.writeUtf(value.categoryId(), MAX_TEXT);
            writeSnapshot(buffer, value.snapshot());
        }
    }

    private static List<WeaponCatalogItem> readCatalog(FriendlyByteBuf buffer) {
        int count = clampCount(buffer.readVarInt(), MAX_CATALOG);
        List<WeaponCatalogItem> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values.add(new WeaponCatalogItem(buffer.readUtf(MAX_TEXT), buffer.readUtf(MAX_TEXT),
                    readSnapshot(buffer)));
        }
        return values;
    }

    private static void writeRepository(FriendlyByteBuf buffer, List<WeaponRepositoryItem> values) {
        int count = Math.min(MAX_REPOSITORY, values == null ? 0 : values.size());
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            WeaponRepositoryItem value = values.get(i);
            buffer.writeUtf(value.id(), MAX_TEXT);
            buffer.writeUtf(value.categoryId(), MAX_TEXT);
            writeSnapshot(buffer, value.snapshot());
            buffer.writeUtf(value.label(), MAX_TEXT);
            buffer.writeBoolean(value.enabled());
        }
    }

    private static List<WeaponRepositoryItem> readRepository(FriendlyByteBuf buffer) {
        int count = clampCount(buffer.readVarInt(), MAX_REPOSITORY);
        List<WeaponRepositoryItem> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values.add(new WeaponRepositoryItem(buffer.readUtf(MAX_TEXT), buffer.readUtf(MAX_TEXT),
                    readSnapshot(buffer), buffer.readUtf(MAX_TEXT), buffer.readBoolean()));
        }
        return values;
    }

    private static void writeSnapshot(FriendlyByteBuf buffer, WeaponSnapshot snapshot) {
        buffer.writeItem(snapshot.stack());
    }

    private static WeaponSnapshot readSnapshot(FriendlyByteBuf buffer) {
        return WeaponSnapshot.from(buffer.readItem());
    }

    private static int clampCount(int value, int maximum) {
        return Math.max(0, Math.min(maximum, value));
    }
}
