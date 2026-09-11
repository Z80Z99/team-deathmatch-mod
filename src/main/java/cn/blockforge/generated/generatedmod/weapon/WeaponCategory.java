package cn.blockforge.generated.generatedmod.weapon;

import net.minecraft.resources.ResourceLocation;

public record WeaponCategory(String id, String name, WeaponKind kind) {
    public static WeaponCategory fromTab(ResourceLocation tab) {
        String path = tab == null ? "" : tab.getPath();
        return new WeaponCategory(path, displayName(path), WeaponKind.fromTab(tab));
    }

    private static String displayName(String path) {
        return switch (path) {
            case "gun_pistol" -> "手枪";
            case "gun_rifle" -> "步枪";
            case "gun_sniper" -> "狙击枪";
            case "gun_shotgun" -> "霰弹枪";
            case "gun_smg" -> "冲锋枪";
            case "gun_rpg" -> "火箭筒";
            case "gun_mg" -> "机枪";
            case "attachment_scope" -> "瞄具";
            case "attachment_muzzle" -> "枪口";
            case "attachment_stock" -> "枪托";
            case "attachment_grip" -> "握把";
            case "attachment_extended_mag" -> "弹匣";
            case "attachment_laser" -> "镭射/战术灯";
            case "ammo" -> "弹药";
            case "other" -> "其他装备";
            default -> path;
        };
    }
}
