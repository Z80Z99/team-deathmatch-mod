package cn.blockforge.generated.generatedmod.weapon;

import net.minecraft.resources.ResourceLocation;

public enum WeaponKind {
    GUN,
    ATTACHMENT,
    AMMO,
    EQUIPMENT;

    public static WeaponKind fromTab(ResourceLocation tab) {
        String path = tab == null ? "" : tab.getPath();
        if (path.startsWith("gun_") || java.util.Set.of(
                "pistol", "rifle", "sniper", "shotgun", "smg", "rpg", "mg").contains(path)) return GUN;
        if (path.startsWith("attachment_") || java.util.Set.of(
                "scope", "muzzle", "stock", "grip", "extended_mag", "laser").contains(path)) return ATTACHMENT;
        if (path.startsWith("ammo")) return AMMO;
        return EQUIPMENT;
    }
}
