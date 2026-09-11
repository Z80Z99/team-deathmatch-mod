package cn.blockforge.generated.generatedmod.shop;

import java.util.List;

public record MatchShopView(
        boolean open,
        int buySecondsRemaining,
        int matchBalance,
        boolean gundbLoaded,
        int gunDamage,
        int gunMaxDamage,
        int attachmentDamage,
        List<MatchShopProduct> products,
        String message,
        boolean error,
        int requestId) {

    public static MatchShopView empty() {
        return new MatchShopView(false, 0, 0, false, 0, 0, 0,
                List.of(), "", false, 0);
    }
}
