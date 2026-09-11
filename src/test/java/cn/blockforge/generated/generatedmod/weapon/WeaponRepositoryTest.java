package cn.blockforge.generated.generatedmod.weapon;

import cn.blockforge.generated.generatedmod.MinecraftTestBootstrap;
import cn.blockforge.generated.generatedmod.network.packet.WeaponRepositorySyncPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WeaponRepositoryTest {
    @BeforeAll static void bootstrap() {
        MinecraftTestBootstrap.initialize();
    }

    @Test void snapshotPreservesItemCountAndCompleteNbt() {
        ItemStack source = new ItemStack(Items.STICK, 3);
        source.getOrCreateTag().putString("gunId", "tacz:ak47");
        source.getOrCreateTag().putInt("ammo", 28);

        WeaponSnapshot snapshot = WeaponSnapshot.from(source);
        ItemStack restored = snapshot.stack();

        assertTrue(ItemStack.isSameItemSameTags(source, restored));
        assertEquals(3, restored.getCount());
        assertEquals("tacz:ak47", restored.getOrCreateTag().getString("gunId"));
        assertEquals(28, restored.getOrCreateTag().getInt("ammo"));
    }

    @Test void syncPacketRoundTripsCatalogAndRepository() {
        ItemStack stack = new ItemStack(Items.DIAMOND, 2);
        stack.getOrCreateTag().putString("attachmentId", "tacz:scope_x4");
        WeaponSnapshot snapshot = WeaponSnapshot.from(stack);
        WeaponCategory category = new WeaponCategory("gun_rifle", "步枪", WeaponKind.GUN);
        WeaponCatalogItem catalog = new WeaponCatalogItem("tacz:gun_rifle#0", "gun_rifle", snapshot);
        WeaponRepositoryItem item = new WeaponRepositoryItem("w_test", "gun_rifle", snapshot, "测试步枪", true);
        WeaponRepositoryView view = new WeaponRepositoryView(true, List.of(category),
                List.of(catalog), List.of(item), "ok", false, 7,
                true, true, true, 1200, 800, Map.of("w_test", 2700));

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new WeaponRepositorySyncPacket(view).encode(buffer);
            WeaponRepositoryView decoded = new WeaponRepositorySyncPacket(buffer).view();
            assertEquals(0, buffer.readableBytes());
            assertTrue(decoded.taczLoaded());
            assertEquals(category, decoded.categories().get(0));
            assertEquals("tacz:gun_rifle#0", decoded.catalog().get(0).catalogId());
            assertEquals("w_test", decoded.repository().get(0).id());
            assertEquals("tacz:scope_x4",
                    decoded.repository().get(0).snapshot().stack().getOrCreateTag().getString("attachmentId"));
            assertEquals(7, decoded.requestId());
            assertEquals(1200, decoded.globalBalance());
            assertEquals(800, decoded.matchBalance());
            assertEquals(2700, decoded.prices().get("w_test"));
        } finally {
            buffer.release();
        }
    }

    @Test void taczTabNamesAreNormalizedForEditorFilters() {
        assertEquals("gun_pistol", WeaponCategory.canonicalId("pistol"));
        assertEquals("gun_rifle", WeaponCategory.canonicalId("rifle"));
        assertEquals("attachment_scope", WeaponCategory.canonicalId("scope"));
        assertEquals("attachment_extended_mag", WeaponCategory.canonicalId("extended_mag"));
        assertEquals("ammo", WeaponCategory.canonicalId("ammo"));
    }
}
