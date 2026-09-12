package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.shop.GunDurabilityAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/** Client-side weapon, TACZ ammo and durability provider used by HUD data sources. */
public final class HeldWeaponHudData {
    private static boolean reflectionReady;
    private static Method gunOrNull;
    private static Method currentAmmoCount;
    private static Method ammoOrNull;
    private static Method ammoOfGun;

    private HeldWeaponHudData() { }

    public static String name() {
        ItemStack held = held();
        return held == null || held.isEmpty() ? "空手" : held.getHoverName().getString();
    }

    public static String ammoText() {
        ItemStack held = held();
        if (held == null || held.isEmpty()) return "空手";
        Object gun = gun(held);
        if (gun == null) return "x" + held.getCount();
        int current = invokeInt(currentAmmoCount, gun, held);
        int reserve = reserveAmmo(held);
        return current + " / " + reserve;
    }

    public static double durabilityPercent() {
        ItemStack held = held();
        if (held == null || held.isEmpty()) return 100.0D;
        int maximum = GunDurabilityAdapter.maxDamage(held);
        if (maximum <= 0) return 100.0D;
        return Math.max(0.0D, Math.min(100.0D,
                (maximum - GunDurabilityAdapter.damage(held)) * 100.0D / maximum));
    }

    public static boolean hasWeapon() {
        ItemStack held = held();
        return held != null && !held.isEmpty();
    }

    private static ItemStack held() {
        var player = Minecraft.getInstance().player;
        return player == null ? null : player.getMainHandItem();
    }

    private static Object gun(ItemStack stack) {
        prepare();
        if (gunOrNull == null) return null;
        try {
            return gunOrNull.invoke(null, stack);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static int reserveAmmo(ItemStack gunStack) {
        var player = Minecraft.getInstance().player;
        if (player == null || ammoOrNull == null || ammoOfGun == null) return 0;
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (candidate.isEmpty() || candidate == gunStack) continue;
            try {
                Object ammo = ammoOrNull.invoke(null, candidate);
                if (ammo == null) continue;
                Object matches = ammoOfGun.invoke(ammo, candidate, gunStack);
                if (Boolean.TRUE.equals(matches)) total += candidate.getCount();
            } catch (ReflectiveOperationException | LinkageError ignored) {
                return total;
            }
        }
        return total;
    }

    private static int invokeInt(Method method, Object target, ItemStack stack) {
        if (method == null) return 0;
        try {
            return ((Number) method.invoke(target, stack)).intValue();
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return 0;
        }
    }

    private static void prepare() {
        if (reflectionReady) return;
        reflectionReady = true;
        try {
            if (!ModList.get().isLoaded("tacz")) return;
            Class<?> gunClass = Class.forName("com.tacz.guns.api.item.IGun");
            Class<?> ammoClass = Class.forName("com.tacz.guns.api.item.IAmmo");
            gunOrNull = gunClass.getMethod("getIGunOrNull", ItemStack.class);
            currentAmmoCount = gunClass.getMethod("getCurrentAmmoCount", ItemStack.class);
            ammoOrNull = ammoClass.getMethod("getIAmmoOrNull", ItemStack.class);
            ammoOfGun = ammoClass.getMethod("isAmmoOfGun", ItemStack.class, ItemStack.class);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            gunOrNull = currentAmmoCount = ammoOrNull = ammoOfGun = null;
        }
    }
}
