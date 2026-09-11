package cn.blockforge.generated.generatedmod.weapon;

public record WeaponCatalogItem(String catalogId, String categoryId, WeaponSnapshot snapshot) {
    public String title() {
        return snapshot.displayName();
    }
}
