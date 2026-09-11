package cn.blockforge.generated.generatedmod.weapon;

import net.minecraft.resources.ResourceLocation;

public enum WeaponKind {
    GUN,
    ATTACHMENT,
    AMMO,
    EQUIPMENT;

    public static WeaponKind fromTab(ResourceLocation tab) {
        String path = tab == null ? "" : tab.getPath();
        if (path.startsWith("gun_")) return GUN;
        if (path.startsWith("attachment_")) return ATTACHMENT;
        if (path.startsWith("ammo")) return AMMO;
        return EQUIPMENT;
    }
}
