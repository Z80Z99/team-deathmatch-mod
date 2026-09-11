package cn.blockforge.generated.generatedmod.economy;

import cn.blockforge.generated.generatedmod.integration.IntegrationManager;
import cn.blockforge.generated.generatedmod.match.Team;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative dual-ledger economy: persistent lobby account and per-match account. */
public final class EconomyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("generated_mod_economy");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final MinecraftServer server;
    private final Path configFile;
    private final Path walletFile;
    private final Map<UUID, Integer> globalBalances = new HashMap<>();
    private final Map<UUID, Integer> matchBalances = new HashMap<>();
    private final Map<UUID, Float> gd656Scores = new HashMap<>();
    private final Map<UUID, Integer> lossStreaks = new HashMap<>();
    private EconomyConfig config = EconomyConfig.defaults();
    private boolean matchActive;

    public EconomyManager(MinecraftServer server) {
        this.server = server;
        this.configFile = server.getServerDirectory().toPath().resolve("config")
                .resolve("fpsmod").resolve("economy.json");
        this.walletFile = server.getServerDirectory().toPath().resolve("config")
                .resolve("fpsmod").resolve("economy_wallets.json");
        load();
    }

    public EconomyConfig config() {
        return config;
    }

    public boolean enabled() {
        return config.enabled();
    }

    public boolean matchActive() {
        return matchActive;
    }

    public int globalBalance(UUID playerId) {
        return clamp(globalBalances.getOrDefault(playerId, config.initialGlobalBalance()));
    }

    public int matchBalance(UUID playerId) {
        return clamp(matchBalances.getOrDefault(playerId, 0));
    }

    public void beginMatch(Collection<ServerPlayer> players) {
        matchActive = true;
        matchBalances.clear();
        lossStreaks.clear();
        gd656Scores.clear();
        for (ServerPlayer player : players) {
            award(player.getUUID(), evaluate(config.matchStartFormula(),
                    variables(player.getUUID(), 0)), "match_start");
            snapshotGd656(player);
        }
        saveWallets();
    }

    public void finishMatch(Collection<ServerPlayer> players, Team winner, Map<UUID, Team> teams) {
        for (ServerPlayer player : players) {
            if (winner != null && teams.get(player.getUUID()) == winner) {
                award(player.getUUID(), evaluate(config.matchWinFormula(),
                        variables(player.getUUID(), 0)), "match_win");
            }
            int payout = evaluate(config.matchEndPayoutFormula(), variables(player.getUUID(), 0));
            if (payout > 0) globalBalances.merge(player.getUUID(), payout, Integer::sum);
        }
        matchActive = false;
        saveWallets();
    }

    public void abortMatch() {
        matchActive = false;
        gd656Scores.clear();
        lossStreaks.clear();
        saveWallets();
    }

    public void recordKill(ServerPlayer killer, ServerPlayer victim) {
        if (!config.enabled() || !matchActive) return;
        award(killer.getUUID(), evaluate(config.killFormula(), variables(killer.getUUID(), 0)), "kill");
        award(victim.getUUID(), evaluate(config.deathFormula(), variables(victim.getUUID(), 0)), "death");
        checkGd656(List.of(killer, victim));
    }

    public void recordDamage(ServerPlayer attacker, float amount) {
        if (!config.enabled() || !matchActive) return;
        Map<String, Double> variables = variables(attacker.getUUID(), 0);
        variables.put("damage", (double) Math.max(0, Math.round(amount)));
        award(attacker.getUUID(), evaluate(config.damageFormula(), variables), "damage");
    }

    public void finishRound(Team winner, Map<UUID, Team> teams) {
        if (!config.enabled() || !matchActive) return;
        for (var entry : teams.entrySet()) {
            boolean won = winner != null && winner == entry.getValue();
            boolean lost = winner != null && winner != entry.getValue();
            if (won) {
                lossStreaks.put(entry.getKey(), 0);
                award(entry.getKey(), evaluate(config.roundWinFormula(), variables(entry.getKey(), 0)), "round_win");
            } else if (lost) {
                int streak = lossStreaks.merge(entry.getKey(), 1, Integer::sum);
                Map<String, Double> variables = variables(entry.getKey(), 0);
                variables.put("loss_streak", (double) streak);
                award(entry.getKey(), evaluate(config.roundLossFormula(), variables), "round_loss");
            } else {
                award(entry.getKey(), evaluate(config.roundDrawFormula(), variables(entry.getKey(), 0)), "round_draw");
            }
        }
    }

    public void tick(Collection<ServerPlayer> players) {
        if (matchActive && config.enabled() && server.getTickCount() % 20 == 0) {
            checkGd656(players);
        }
    }

    public int price(String itemId, String categoryId, int count) {
        return price(itemId, categoryId, count, Map.of());
    }

    public int price(String itemId, String categoryId, int count, Map<String, Double> extraVariables) {
        Integer override = config.itemPriceOverrides().get(itemId);
        if (override != null) return Math.max(0, override);
        String formula = config.categoryPriceFormulas().getOrDefault(categoryId,
                config.categoryPriceFormulas().getOrDefault("other", "0"));
        Map<String, Double> variables = new LinkedHashMap<>();
        variables.put("count", (double) Math.max(1, count));
        if (extraVariables != null) variables.putAll(extraVariables);
        return Math.max(0, evaluate(formula, variables));
    }

    public PurchaseResult purchase(ServerPlayer player, String itemId, String categoryId,
                                   int count, boolean useMatchBalance) {
        return purchase(player, itemId, categoryId, count, useMatchBalance, Map.of());
    }

    public PurchaseResult purchase(ServerPlayer player, String itemId, String categoryId,
                                   int count, boolean useMatchBalance,
                                   Map<String, Double> extraVariables) {
        if (!config.enabled()) return new PurchaseResult(true, 0, "经济系统未启用，物品免费发放。");
        if (useMatchBalance && !matchActive) {
            return new PurchaseResult(false, 0, "当前没有进行中的比赛，不能使用比赛资金购买。");
        }
        int price = price(itemId, categoryId, count, extraVariables);
        UUID playerId = player.getUUID();
        Map<UUID, Integer> ledger = useMatchBalance ? matchBalances : globalBalances;
        int balance = ledger.getOrDefault(playerId, useMatchBalance ? 0 : config.initialGlobalBalance());
        if (balance < price) {
            return new PurchaseResult(false, price, useMatchBalance
                    ? "比赛资金不足，需要 $" + price + "，当前 $" + balance + "。"
                    : "账户余额不足，需要 $" + price + "，当前 $" + balance + "。");
        }
        ledger.put(playerId, clamp(balance - price));
        saveWallets();
        return new PurchaseResult(true, price, "已购买，扣除 $" + price + "。");
    }

    public int setBalance(UUID playerId, int amount, boolean matchBalance) {
        Map<UUID, Integer> ledger = matchBalance ? matchBalances : globalBalances;
        int value = clamp(amount);
        ledger.put(playerId, value);
        saveWallets();
        return value;
    }

    public int addBalance(UUID playerId, int amount, boolean matchBalance) {
        Map<UUID, Integer> ledger = matchBalance ? matchBalances : globalBalances;
        int value = clamp(ledger.getOrDefault(playerId, matchBalance ? 0
                : config.initialGlobalBalance()) + amount);
        ledger.put(playerId, value);
        saveWallets();
        return value;
    }

    public void reload() {
        load();
    }

    public boolean setItemPrice(String itemId, int price) {
        Map<String, Integer> overrides = new LinkedHashMap<>(config.itemPriceOverrides());
        boolean changed = price >= 0 ? overrides.put(itemId, Math.max(0, price)) != null
                : overrides.remove(itemId) != null;
        if (!changed && price < 0) return false;
        config = new EconomyConfig(config.enabled(), config.initialGlobalBalance(),
                config.initialMatchBalance(), config.maxBalance(), config.buyWindowSeconds(),
                config.matchStartFormula(),
                config.killFormula(), config.assistFormula(), config.deathFormula(),
                config.damageFormula(), config.roundWinFormula(), config.roundLossFormula(),
                config.roundDrawFormula(), config.matchWinFormula(), config.matchEndPayoutFormula(),
                config.gd656ScoreFormula(), config.categoryPriceFormulas(), overrides);
        writeJson(configFile, config.toJson());
        return true;
    }

    public void syncNow() {
        saveWallets();
    }

    private int award(UUID playerId, int amount, String reason) {
        if (amount == 0) return amount;
        matchBalances.merge(playerId, amount, Integer::sum);
        matchBalances.put(playerId, clamp(matchBalances.get(playerId)));
        return amount;
    }

    private void checkGd656(Collection<ServerPlayer> players) {
        if (!IntegrationManager.isGd656Loaded() || !matchActive || !config.enabled()) return;
        for (ServerPlayer player : players) {
            Float score = readGd656Score(player.getUUID());
            if (score == null) continue;
            float previous = gd656Scores.getOrDefault(player.getUUID(), 0.0F);
            float delta = score - previous;
            gd656Scores.put(player.getUUID(), score);
            if (delta <= 0.0F) continue;
            Map<String, Double> variables = variables(player.getUUID(), 0);
            variables.put("score_delta", (double) delta);
            award(player.getUUID(), evaluate(config.gd656ScoreFormula(), variables), "gd656_score");
        }
    }

    private void snapshotGd656(ServerPlayer player) {
        Float score = readGd656Score(player.getUUID());
        gd656Scores.put(player.getUUID(), score == null ? 0.0F : score);
    }

    private Float readGd656Score(UUID playerId) {
        try {
            Class<?> managerClass = Class.forName(
                    "org.mods.gd656killicon.server.data.PlayerDataManager");
            Object manager = managerClass.getMethod("get").invoke(null);
            Method score = managerClass.getMethod("getScore", UUID.class);
            Object value = score.invoke(manager, playerId);
            return value instanceof Number number ? number.floatValue() : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private Map<String, Double> variables(UUID playerId, int ignored) {
        Map<String, Double> values = new LinkedHashMap<>();
        values.put("initial_match_balance", (double) config.initialMatchBalance());
        values.put("match_balance", (double) matchBalance(playerId));
        values.put("global_balance", (double) globalBalance(playerId));
        values.put("loss_streak", (double) lossStreaks.getOrDefault(playerId, 0));
        values.put("count", 1.0D);
        return values;
    }

    private int evaluate(String formula, Map<String, Double> variables) {
        try {
            return MoneyFormula.evaluate(formula, variables);
        } catch (RuntimeException error) {
            LOGGER.warn("经济公式无效，已按 0 处理：{} -> {}", formula, error.getMessage());
            return 0;
        }
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(config.maxBalance(), value));
    }

    private void load() {
        config = loadConfig();
        loadWallets();
    }

    private EconomyConfig loadConfig() {
        if (!Files.isRegularFile(configFile)) {
            EconomyConfig defaults = EconomyConfig.defaults();
            writeJson(configFile, defaults.toJson());
            return defaults;
        }
        try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            return EconomyConfig.fromJson(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (Exception error) {
            LOGGER.warn("读取经济配置失败，将使用默认配置：{}", configFile, error);
            return EconomyConfig.defaults();
        }
    }

    private void loadWallets() {
        globalBalances.clear();
        matchBalances.clear();
        if (!Files.isRegularFile(walletFile)) return;
        try (BufferedReader reader = Files.newBufferedReader(walletFile, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            readBalances(root, "global", globalBalances);
            readBalances(root, "match", matchBalances);
        } catch (Exception error) {
            LOGGER.warn("读取经济钱包失败：{}", walletFile, error);
        }
    }

    private void saveWallets() {
        JsonObject root = new JsonObject();
        writeBalances(root, "global", globalBalances);
        writeBalances(root, "match", matchBalances);
        writeJson(walletFile, root);
    }

    private static void readBalances(JsonObject root, String key, Map<UUID, Integer> output) {
        if (!root.has(key) || !root.get(key).isJsonObject()) return;
        for (var entry : root.getAsJsonObject(key).entrySet()) {
            try {
                output.put(UUID.fromString(entry.getKey()), entry.getValue().getAsInt());
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static void writeBalances(JsonObject root, String key, Map<UUID, Integer> values) {
        JsonObject object = new JsonObject();
        values.forEach((playerId, value) -> object.addProperty(playerId.toString(), value));
        root.add(key, object);
    }

    private static void writeJson(Path path, JsonObject object) {
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(object, writer);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException error) {
            LOGGER.error("写入经济数据失败：{}", path, error);
        }
    }

    public record PurchaseResult(boolean success, int price, String message) { }
}
