package cn.blockforge.generated.generatedmod.weapon;

import java.util.List;

public record WeaponRepositoryView(
        boolean taczLoaded,
        List<WeaponCategory> categories,
        List<WeaponCatalogItem> catalog,
        List<WeaponRepositoryItem> repository,
        String message,
        boolean error,
        int requestId) {

    public static WeaponRepositoryView empty() {
        return new WeaponRepositoryView(false, List.of(), List.of(), List.of(), "", false, 0);
    }
}
