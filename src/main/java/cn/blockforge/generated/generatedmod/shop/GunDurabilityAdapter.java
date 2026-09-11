package cn.blockforge.generated.generatedmod.shop;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/** Reflective GunDB/TACZ durability bridge; no compile-time dependency on either mod. */
public final class GunDurabilityAdapter {
    private GunDurabilityAdapter() { }

    public static boolean loaded() {
        return ModList.get().isLoaded("gundb") && ModList.get().isLoaded("tacz");
    }

    public static int damage(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.isDamageableItem()
                ? Math.max(0, stack.getDamageValue()) : 0;
    }

    public static int maxDamage(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.isDamageableItem()
                ? Math.max(0, stack.getMaxDamage()) : 0;
    }

    public static boolean repairGun(ItemStack gun) {
        int damage = damage(gun);
        if (damage <= 0) return false;
        gun.setDamageValue(0);
        return true;
    }

    public static int attachmentDamage(ItemStack gun) {
        return forEachAttachment(gun, null);
    }

    public static boolean repairAttachments(ItemStack gun) {
        return forEachAttachment(gun, "repair") > 0;
    }

    private static int forEachAttachment(ItemStack gun, String operation) {
        if (!loaded() || gun == null || gun.isEmpty()) return 0;
        try {
            Class<?> igunClass = Class.forName("com.tacz.guns.api.item.IGun");
            Object igun = igunClass.getMethod("getIGunOrNull", ItemStack.class).invoke(null, gun);
            if (igun == null) return 0;
            Class<?> typeClass = Class.forName("com.tacz.guns.api.item.attachment.AttachmentType");
            Method getAttachment = igunClass.getMethod("getAttachment", ItemStack.class, typeClass);
            Method installAttachment = igunClass.getMethod("installAttachment", ItemStack.class, ItemStack.class);
            int total = 0;
            for (Object type : typeClass.getEnumConstants()) {
                Object value = getAttachment.invoke(igun, gun, type);
                if (!(value instanceof ItemStack attachment) || attachment.isEmpty()) continue;
                int damage = damage(attachment);
                if (damage <= 0) continue;
                total += damage;
                if ("repair".equals(operation)) {
                    attachment.setDamageValue(0);
                    installAttachment.invoke(igun, gun, attachment);
                }
            }
            return total;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return 0;
        }
    }
}
