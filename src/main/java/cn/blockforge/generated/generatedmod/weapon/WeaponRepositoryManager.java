package cn.blockforge.generated.generatedmod.weapon;

import cn.blockforge.generated.generatedmod.match.GameMode;
import cn.blockforge.generated.generatedmod.match.MatchManager;
import cn.blockforge.generated.generatedmod.economy.EconomyManager;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.WeaponRepositorySyncPacket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Shared server arsenal. The catalog is derived from optional weapon mods'
 * creative tabs; persisted entries retain complete ItemStack NBT.
 */
public final class WeaponRepositoryManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("generated_mod_weapon_repository");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_CATALOG = 1024;
    private static final int MAX_REPOSITORY = 512;
    private static final Map<String, String> GUN_TAB_TYPES = Map.of(
            "gun_pistol", "PISTOL", "gun_rifle", "RIFLE", "gun_sniper", "SNIPER",
            "gun_shotgun", "SHOTGUN", "gun_smg", "SMG", "gun_rpg", "RPG", "gun_mg", "MG");
    private static final Map<String, String> ATTACHMENT_TYPES = Map.of(
            "attachment_scope", "SCOPE", "attachment_muzzle", "MUZZLE",
            "attachment_stock", "STOCK", "attachment_grip", "GRIP",
            "attachment_extended_mag", "EXTENDED_MAG", "attachment_laser", "LASER");

    private final MinecraftServer server;
    private final Path file;
    private final LinkedHashMap<String, WeaponCategory> categories = new LinkedHashMap<>();
    private final List<WeaponCatalogItem> catalog = new ArrayList<>();
    private final LinkedHashMap<String, WeaponRepositoryItem> repository = new LinkedHashMap<>();
    private final Map<UUID, String> messages = new LinkedHashMap<>();
    private final Map<UUID, Boolean> errors = new LinkedHashMap<>();
    private boolean catalogBuilt;
    private boolean taczLoaded;

    public WeaponRepositoryManager(MinecraftServer server) {
        this.server = server;
        this.file = server.getServerDirectory().toPath().resolve("config")
                .resolve("fpsmod").resolve("weapon_repository.json");
        this.taczLoaded = ModList.get().isLoaded("tacz");
        loadRepository();
    }

    public List<WeaponRepositoryItem> repository() {
        return List.copyOf(repository.values());
    }

    public List<WeaponCategory> categories() {
        return List.copyOf(categories.values());
    }

    public List<WeaponCatalogItem> catalog() {
        return List.copyOf(catalog);
    }

    public void handleAction(ServerPlayer player, WeaponRepositoryAction action,
                             String catalogId, String entryId, int requestId) {
        if (player == null || action == null) return;
        if (action != WeaponRepositoryAction.REQUEST) ensureCatalog();
        switch (action) {
            case REQUEST -> setMessage(player, "武器仓库状态已刷新。", false);
            case REFRESH_CATALOG -> {
                if (!player.hasPermissions(2)) {
                    setMessage(player, "只有管理员可以刷新武器目录。", true);
                } else {
                    buildCatalog(true);
                    setMessage(player, taczLoaded
                            ? "武器目录已重新读取，共 " + catalog.size() + " 项。"
                            : "未检测到 TACZ，因此没有可读取的枪械目录。", !taczLoaded);
                }
            }
            case ADD -> addCatalogItem(player, catalogId);
            case ADD_HELD -> addHeldItem(player);
            case REMOVE -> removeRepositoryItem(player, entryId);
            case GIVE -> giveRepositoryItem(player, entryId);
            case BUY -> buyRepositoryItem(player, entryId);
        }
        sendView(player, requestId);
    }

    private void addCatalogItem(ServerPlayer player, String catalogId) {
        if (!player.hasPermissions(2)) {
            setMessage(player, "只有管理员可以编辑服务器武器仓库。", true);
            return;
        }
        WeaponCatalogItem selected = catalog.stream()
                .filter(item -> item.catalogId().equals(catalogId)).findFirst().orElse(null);
        if (selected == null) {
            setMessage(player, "找不到所选目录物品，请刷新目录后重试。", true);
            return;
        }
        if (repository.size() >= MAX_REPOSITORY) {
            setMessage(player, "仓库已达到 " + MAX_REPOSITORY + " 项上限。", true);
            return;
        }
        String entryId = "w_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        repository.put(entryId, new WeaponRepositoryItem(entryId, selected.categoryId(),
                selected.snapshot(), "", true));
        saveRepository();
        setMessage(player, "已加入仓库：" + selected.title() + "。", false);
    }

    private void removeRepositoryItem(ServerPlayer player, String entryId) {
        if (!player.hasPermissions(2)) {
            setMessage(player, "只有管理员可以编辑服务器武器仓库。", true);
            return;
        }
        WeaponRepositoryItem removed = repository.remove(entryId);
        if (removed == null) {
            setMessage(player, "找不到要移除的仓库条目。", true);
            return;
        }
        saveRepository();
        setMessage(player, "已从仓库移除：" + removed.title() + "。", false);
    }

    private void addHeldItem(ServerPlayer player) {
        if (!player.hasPermissions(2)) {
            setMessage(player, "只有管理员可以编辑服务器商店仓库。", true);
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            setMessage(player, "请先把要加入商店的物品拿在主手。", true);
            return;
        }
        if (repository.size() >= MAX_REPOSITORY) {
            setMessage(player, "仓库已达到 " + MAX_REPOSITORY + " 项上限。", true);
            return;
        }
        WeaponSnapshot snapshot = WeaponSnapshot.from(held.copy());
        if (!snapshot.valid()) {
            setMessage(player, "无法保存手持物品。", true);
            return;
        }
        String entryId = "w_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        repository.put(entryId, new WeaponRepositoryItem(entryId, "external", snapshot, "", true));
        saveRepository();
        setMessage(player, "已把手持物品加入商店仓库：" + snapshot.displayName() + "。", false);
    }

    private void giveRepositoryItem(ServerPlayer player, String entryId) {
        if (!player.hasPermissions(2)) {
            setMessage(player, "只有管理员可以免费发放仓库物品。", true);
            return;
        }
        WeaponRepositoryItem item = repository.get(entryId);
        if (item == null) {
            setMessage(player, "找不到要领取的仓库物品。", true);
            return;
        }
        ItemStack stack = item.snapshot().stack();
        if (stack.isEmpty()) {
            setMessage(player, "该仓库条目已损坏或依赖模组未安装。", true);
            return;
        }
        if (!player.getInventory().add(stack.copy())) {
            player.drop(stack, false);
        }
        setMessage(player, "已发放：" + item.title() + " x" + stack.getCount() + "。", false);
    }

    private void buyRepositoryItem(ServerPlayer player, String entryId) {
        WeaponRepositoryItem item = repository.get(entryId);
        if (item == null) {
            setMessage(player, "找不到要购买的仓库物品。", true);
            return;
        }
        ItemStack stack = item.snapshot().stack();
        if (stack.isEmpty()) {
            setMessage(player, "该仓库条目已损坏或依赖模组未安装。", true);
            return;
        }
        MatchManager match = MatchManager.get();
        if (match == null) {
            setMessage(player, "经济系统不可用。", true);
            return;
        }
        boolean inMatch = match != null && match.isMatchActive();
        EconomyManager.PurchaseResult result = match.economy().purchase(player, item.id(),
                item.categoryId(), stack.getCount(), inMatch);
        if (!result.success()) {
            setMessage(player, result.message(), true);
            return;
        }
        if (!player.getInventory().add(stack.copy())) player.drop(stack, false);
        setMessage(player, result.message() + "：" + item.title() + "。", false);
    }

    public List<WeaponRepositoryItem> entriesForMode(GameMode mode) {
        // Mode policies can be added later without changing the persisted data model.
        return repository.values().stream().filter(WeaponRepositoryItem::enabled).toList();
    }

    public void giveLoadout(ServerPlayer player, List<WeaponRepositoryItem> loadout) {
        if (player == null || loadout == null) return;
        for (WeaponRepositoryItem item : loadout) {
            if (!item.enabled()) continue;
            ItemStack stack = item.snapshot().stack();
            if (stack.isEmpty()) continue;
            if (!player.getInventory().add(stack.copy())) player.drop(stack, false);
        }
    }

    public void sendView(ServerPlayer player, int requestId) {
        ensureCatalog();
        String message = messages.getOrDefault(player.getUUID(), "");
        boolean error = errors.getOrDefault(player.getUUID(), false);
        MatchManager match = MatchManager.get();
        EconomyManager economy = match.economy();
        Map<String, Integer> prices = new LinkedHashMap<>();
        for (WeaponRepositoryItem item : repository.values()) {
            prices.put(item.id(), economy.price(item.id(), item.categoryId(),
                    Math.max(1, item.snapshot().stack().getCount())));
        }
        FpsTdmNetwork.sendToPlayer(new WeaponRepositorySyncPacket(
                new WeaponRepositoryView(taczLoaded, categories(), catalog(), repository(),
                        message, error, requestId, economy.enabled(), match.isMatchActive(),
                        player.hasPermissions(2), economy.globalBalance(player.getUUID()),
                        economy.matchBalance(player.getUUID()), prices)), player);
        messages.remove(player.getUUID());
        errors.remove(player.getUUID());
    }

    private void ensureCatalog() {
        if (!catalogBuilt) buildCatalog(false);
    }

    private synchronized void buildCatalog(boolean force) {
        if (catalogBuilt && !force) return;
        categories.clear();
        catalog.clear();
        categories.put("external", new WeaponCategory("external", "其他商品", WeaponKind.EQUIPMENT));
        taczLoaded = ModList.get().isLoaded("tacz");
        if (!taczLoaded) {
            catalogBuilt = true;
            return;
        }
        Set<String> seen = new HashSet<>();
        int[] index = {0};
        boolean direct = buildDirectTaczCatalog(seen, index);
        if (!direct || catalog.isEmpty()) buildCatalogFromCreativeTabs(seen, index);
        catalogBuilt = true;
        LOGGER.info("武器目录已生成：{} 个分类，{} 个物品", categories.size(), catalog.size());
    }

    /** TACZ's common indexes are authoritative on both integrated and dedicated servers. */
    private boolean buildDirectTaczCatalog(Set<String> seen, int[] index) {
        boolean invoked = false;
        for (Map.Entry<String, String> entry : GUN_TAB_TYPES.entrySet()) {
            WeaponCategory category = new WeaponCategory(entry.getKey(),
                    WeaponCategory.fromTab(ResourceLocation.fromNamespaceAndPath("tacz",
                            entry.getKey().substring("gun_".length()))).name(), WeaponKind.GUN);
            categories.putIfAbsent(category.id(), category);
            invoked |= appendReflectedStacks("com.tacz.guns.api.item.gun.AbstractGunItem",
                    "com.tacz.guns.api.item.GunTabType", entry.getValue(), category.id(), seen, index);
        }
        for (Map.Entry<String, String> entry : ATTACHMENT_TYPES.entrySet()) {
            WeaponCategory category = new WeaponCategory(entry.getKey(),
                    WeaponCategory.fromTab(ResourceLocation.fromNamespaceAndPath("tacz",
                            entry.getKey().substring("attachment_".length()))).name(), WeaponKind.ATTACHMENT);
            categories.putIfAbsent(category.id(), category);
            invoked |= appendReflectedStacks("com.tacz.guns.item.AttachmentItem",
                    "com.tacz.guns.api.item.attachment.AttachmentType",
                    entry.getValue(), category.id(), seen, index);
        }
        WeaponCategory ammo = new WeaponCategory("ammo", "弹药", WeaponKind.AMMO);
        categories.putIfAbsent(ammo.id(), ammo);
        invoked |= appendReflectedStacks("com.tacz.guns.item.AmmoItem",
                null, null, ammo.id(), seen, index);
        return invoked;
    }

    @SuppressWarnings("unchecked")
    private boolean appendReflectedStacks(String ownerName, String enumName, String enumValue,
                                          String categoryId, Set<String> seen, int[] index) {
        try {
            Class<?> owner = Class.forName(ownerName);
            Object parameter = null;
            Class<?> parameterType = null;
            if (enumName != null) {
                parameterType = Class.forName(enumName);
                parameter = Enum.valueOf((Class<? extends Enum>) parameterType.asSubclass(Enum.class), enumValue);
            }
            var method = parameterType == null ? owner.getMethod("fillItemCategory")
                    : owner.getMethod("fillItemCategory", parameterType);
            Object result = parameter == null ? method.invoke(null) : method.invoke(null, parameter);
            if (!(result instanceof Collection<?> collection)) return false;
            appendStacks(categoryId, (Collection<ItemStack>) collection, seen, index);
            return true;
        } catch (Throwable error) {
            LOGGER.debug("TACZ 直接目录不可用：{}.{} -> {}", ownerName, enumValue, error.toString());
            return false;
        }
    }

    private void appendStacks(String categoryId, Collection<ItemStack> stacks,
                              Set<String> seen, int[] index) {
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty() || catalog.size() >= MAX_CATALOG) continue;
            WeaponSnapshot snapshot = WeaponSnapshot.from(stack.copy());
            if (!snapshot.valid()) continue;
            String signature = snapshot.itemId() + "|" + snapshot.tagBase64();
            if (!seen.add(signature)) continue;
            catalog.add(new WeaponCatalogItem(categoryId + "#" + index[0]++, categoryId, snapshot));
        }
    }

    private void buildCatalogFromCreativeTabs(Set<String> seen, int[] index) {
        FeatureFlagSet features = FeatureFlagSet.of();
        for (Map.Entry<ResourceKey<CreativeModeTab>, CreativeModeTab> entry
                : BuiltInRegistries.CREATIVE_MODE_TAB.entrySet()) {
            ResourceLocation tabId = entry.getKey().location();
            if (!"tacz".equals(tabId.getNamespace()) || catalog.size() >= MAX_CATALOG) continue;
            try {
                CreativeModeTab tab = entry.getValue();
                tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                        features, true, server.registryAccess()));
                WeaponCategory category = WeaponCategory.fromTab(tabId);
                categories.putIfAbsent(category.id(), category);
                appendStacks(category.id(), tab.getDisplayItems(), seen, index);
            } catch (Throwable error) {
                LOGGER.warn("无法读取 TACZ 创造分类 {}：{}", tabId, error.toString());
            }
        }
    }

    private void setMessage(ServerPlayer player, String message, boolean error) {
        messages.put(player.getUUID(), message == null ? "" : message);
        errors.put(player.getUUID(), error);
    }

    private void loadRepository() {
        if (!Files.isRegularFile(file)) return;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray entries = root.has("entries") ? root.getAsJsonArray("entries") : new JsonArray();
            for (JsonElement element : entries) {
                JsonObject object = element.getAsJsonObject();
                String id = string(object, "id");
                if (id.isBlank()) continue;
                String itemId = string(object, "item");
                String tag = string(object, "tag");
                int count = integer(object, "count", 1);
                WeaponSnapshot snapshot = new WeaponSnapshot(itemId, tag, count);
                if (!snapshot.valid()) continue;
                repository.put(id, new WeaponRepositoryItem(id, string(object, "category"),
                        snapshot, string(object, "label"), bool(object, "enabled", true)));
            }
        } catch (Exception error) {
            LOGGER.warn("读取武器仓库失败，将从空仓库启动：{}", file, error);
            repository.clear();
        }
    }

    private synchronized boolean saveRepository() {
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("version", 1);
            JsonArray entries = new JsonArray();
            for (WeaponRepositoryItem item : repository.values()) {
                JsonObject object = new JsonObject();
                object.addProperty("id", item.id());
                object.addProperty("category", item.categoryId());
                object.addProperty("item", item.snapshot().itemId());
                object.addProperty("tag", item.snapshot().tagBase64());
                object.addProperty("count", item.snapshot().count());
                object.addProperty("label", item.label());
                object.addProperty("enabled", item.enabled());
                entries.add(object);
            }
            root.add("entries", entries);
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException error) {
            LOGGER.error("保存武器仓库失败：{}", file, error);
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
            }
            return false;
        }
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    private static int integer(JsonObject object, String key, int fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : fallback;
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsBoolean() : fallback;
    }
}
