package cn.blockforge.generated.generatedmod.weapon;

import java.util.List;
import java.util.Map;

public record WeaponRepositoryView(
        boolean taczLoaded,
        List<WeaponCategory> categories,
        List<WeaponCatalogItem> catalog,
        List<WeaponRepositoryItem> repository,
        String message,
        boolean error,
        int requestId,
        boolean economyEnabled,
        boolean matchActive,
        boolean canManage,
        int globalBalance,
        int matchBalance,
        Map<String, Integer> prices) {

    public static WeaponRepositoryView empty() {
        return new WeaponRepositoryView(false, List.of(), List.of(), List.of(), "", false, 0,
                false, false, false, 0, 0, Map.of());
    }
}
