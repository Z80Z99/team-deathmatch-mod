package cn.blockforge.generated.generatedmod.economy;

import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

public record EconomyConfig(
        boolean enabled,
        int initialGlobalBalance,
        int initialMatchBalance,
        int maxBalance,
        int buyWindowSeconds,
        String matchStartFormula,
        String killFormula,
        String assistFormula,
        String deathFormula,
        String damageFormula,
        String roundWinFormula,
        String roundLossFormula,
        String roundDrawFormula,
        String matchWinFormula,
        String matchEndPayoutFormula,
        String gd656ScoreFormula,
        Map<String, String> categoryPriceFormulas,
        Map<String, Integer> itemPriceOverrides) {

    public static EconomyConfig defaults() {
        Map<String, String> prices = new LinkedHashMap<>();
        prices.put("gun_pistol", "300");
        prices.put("gun_rifle", "2700");
        prices.put("gun_sniper", "4750");
        prices.put("gun_shotgun", "1200");
        prices.put("gun_smg", "1500");
        prices.put("gun_rpg", "5200");
        prices.put("gun_mg", "5200");
        prices.put("attachment_scope", "400");
        prices.put("attachment_muzzle", "300");
        prices.put("attachment_stock", "300");
        prices.put("attachment_grip", "250");
        prices.put("attachment_extended_mag", "350");
        prices.put("attachment_laser", "300");
        prices.put("ammo", "100");
        prices.put("other", "300");
        prices.put("service_repair_gun", "max(200, durability_lost * 3)");
        prices.put("service_repair_attachments", "max(100, durability_lost * 2)");
        prices.put("service_jammer", "400");
        return new EconomyConfig(true, 1000, 800, 1000000, 15,
                "initial_match_balance", "300", "100", "0", "0",
                "3250", "1400 + min(loss_streak, 4) * 500", "1500",
                "0", "0", "0", prices, new LinkedHashMap<>());
    }

    public static EconomyConfig fromJson(JsonObject root) {
        EconomyConfig defaults = defaults();
        if (root == null) return defaults;
        Map<String, String> categories = new LinkedHashMap<>(defaults.categoryPriceFormulas);
        if (root.has("categoryPriceFormulas") && root.get("categoryPriceFormulas").isJsonObject()) {
            for (var entry : root.getAsJsonObject("categoryPriceFormulas").entrySet()) {
                categories.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        Map<String, Integer> overrides = new LinkedHashMap<>();
        if (root.has("itemPriceOverrides") && root.get("itemPriceOverrides").isJsonObject()) {
            for (var entry : root.getAsJsonObject("itemPriceOverrides").entrySet()) {
                overrides.put(entry.getKey(), entry.getValue().getAsInt());
            }
        }
        return new EconomyConfig(
                bool(root, "enabled", defaults.enabled),
                integer(root, "initialGlobalBalance", defaults.initialGlobalBalance),
                integer(root, "initialMatchBalance", defaults.initialMatchBalance),
                integer(root, "maxBalance", defaults.maxBalance),
                integer(root, "buyWindowSeconds", defaults.buyWindowSeconds),
                text(root, "matchStartFormula", defaults.matchStartFormula),
                text(root, "killFormula", defaults.killFormula),
                text(root, "assistFormula", defaults.assistFormula),
                text(root, "deathFormula", defaults.deathFormula),
                text(root, "damageFormula", defaults.damageFormula),
                text(root, "roundWinFormula", defaults.roundWinFormula),
                text(root, "roundLossFormula", defaults.roundLossFormula),
                text(root, "roundDrawFormula", defaults.roundDrawFormula),
                text(root, "matchWinFormula", defaults.matchWinFormula),
                text(root, "matchEndPayoutFormula", defaults.matchEndPayoutFormula),
                text(root, "gd656ScoreFormula", defaults.gd656ScoreFormula),
                categories, overrides);
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("enabled", enabled);
        root.addProperty("initialGlobalBalance", initialGlobalBalance);
        root.addProperty("initialMatchBalance", initialMatchBalance);
        root.addProperty("maxBalance", maxBalance);
        root.addProperty("buyWindowSeconds", buyWindowSeconds);
        root.addProperty("matchStartFormula", matchStartFormula);
        root.addProperty("killFormula", killFormula);
        root.addProperty("assistFormula", assistFormula);
        root.addProperty("deathFormula", deathFormula);
        root.addProperty("damageFormula", damageFormula);
        root.addProperty("roundWinFormula", roundWinFormula);
        root.addProperty("roundLossFormula", roundLossFormula);
        root.addProperty("roundDrawFormula", roundDrawFormula);
        root.addProperty("matchWinFormula", matchWinFormula);
        root.addProperty("matchEndPayoutFormula", matchEndPayoutFormula);
        root.addProperty("gd656ScoreFormula", gd656ScoreFormula);
        JsonObject categories = new JsonObject();
        categoryPriceFormulas.forEach(categories::addProperty);
        root.add("categoryPriceFormulas", categories);
        JsonObject overrides = new JsonObject();
        itemPriceOverrides.forEach(overrides::addProperty);
        root.add("itemPriceOverrides", overrides);
        return root;
    }

    private static String text(JsonObject root, String key, String fallback) {
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsString() : fallback;
    }

    private static int integer(JsonObject root, String key, int fallback) {
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsInt() : fallback;
    }

    private static boolean bool(JsonObject root, String key, boolean fallback) {
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsBoolean() : fallback;
    }
}
