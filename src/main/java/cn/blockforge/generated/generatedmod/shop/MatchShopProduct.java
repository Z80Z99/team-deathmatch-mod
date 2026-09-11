package cn.blockforge.generated.generatedmod.shop;

import cn.blockforge.generated.generatedmod.weapon.WeaponSnapshot;

public record MatchShopProduct(String id, String categoryId, String title, int price,
                               WeaponSnapshot snapshot, MatchShopService service) {
    public static MatchShopProduct item(String id, String categoryId, String title,
                                        int price, WeaponSnapshot snapshot) {
        return new MatchShopProduct(id, categoryId, title, price, snapshot, null);
    }
}
