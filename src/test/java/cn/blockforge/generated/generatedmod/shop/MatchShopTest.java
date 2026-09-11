package cn.blockforge.generated.generatedmod.shop;

import cn.blockforge.generated.generatedmod.MinecraftTestBootstrap;
import cn.blockforge.generated.generatedmod.network.packet.MatchShopSyncPacket;
import cn.blockforge.generated.generatedmod.weapon.WeaponSnapshot;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MatchShopTest {
    @BeforeAll static void bootstrap() {
        MinecraftTestBootstrap.initialize();
    }

    @Test void genericDamageableStackCanBeReadAndRepaired() {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        stack.setDamageValue(100);

        assertEquals(100, GunDurabilityAdapter.damage(stack));
        assertTrue(GunDurabilityAdapter.maxDamage(stack) > 100);
        assertTrue(GunDurabilityAdapter.repairGun(stack));
        assertEquals(0, stack.getDamageValue());
    }

    @Test void shopPacketRoundTripsProductsAndDurabilityState() {
        WeaponSnapshot snapshot = WeaponSnapshot.from(new ItemStack(Items.IRON_SWORD));
        MatchShopProduct weapon = MatchShopProduct.item("repo:test", "gun_rifle",
                "测试步枪", 2700, snapshot);
        MatchShopProduct repair = new MatchShopProduct("service:repair_gun", "service_repair_gun",
                "维修当前枪械", 300, WeaponSnapshot.empty(), MatchShopService.REPAIR_GUN);
        MatchShopView view = new MatchShopView(true, 12, 800, true,
                45, 1000, 20, List.of(weapon, repair), "ok", false, 9);

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new MatchShopSyncPacket(view).encode(buffer);
            MatchShopView decoded = new MatchShopSyncPacket(buffer).view();
            assertEquals(0, buffer.readableBytes());
            assertTrue(decoded.open());
            assertEquals(800, decoded.matchBalance());
            assertEquals(45, decoded.gunDamage());
            assertEquals(2, decoded.products().size());
            assertEquals(MatchShopService.REPAIR_GUN, decoded.products().get(1).service());
            assertEquals(300, decoded.products().get(1).price());
        } finally {
            buffer.release();
        }
    }
}
