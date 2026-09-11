package cn.blockforge.generated.generatedmod.shop;

import cn.blockforge.generated.generatedmod.economy.EconomyManager;
import cn.blockforge.generated.generatedmod.match.MatchManager;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.MatchShopSyncPacket;
import cn.blockforge.generated.generatedmod.weapon.WeaponRepositoryItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** CS-style opening-window shop using the match wallet and the shared repository. */
public final class MatchShopManager {
    private static final String REPAIR_GUN_ID = "service:repair_gun";
    private static final String REPAIR_ATTACHMENTS_ID = "service:repair_attachments";
    private final MinecraftServer server;
    private final MatchManager match;

    public MatchShopManager(MinecraftServer server, MatchManager match) {
        this.server = server;
        this.match = match;
    }

    public void handleAction(ServerPlayer player, MatchShopAction action,
                             String productId, int requestId) {
        if (player == null || action == null) return;
        if (action == MatchShopAction.BUY) purchase(player, productId);
        sendView(player, requestId);
    }

    public void sendView(ServerPlayer player, int requestId) {
        EconomyManager economy = match.economy();
        int window = economy.config().buyWindowSeconds();
        boolean open = match.economy().matchActive() && match.isBuyPhaseOpen(window);
        int remaining = open ? match.buyPhaseRemainingSeconds(window) : 0;
        ItemStack held = player.getMainHandItem();
        int gunDamage = GunDurabilityAdapter.damage(held);
        int gunMax = GunDurabilityAdapter.maxDamage(held);
        int attachmentDamage = GunDurabilityAdapter.attachmentDamage(held);
        List<MatchShopProduct> products = open
                ? products(player, economy, held, gunDamage, gunMax, attachmentDamage) : List.of();
        FpsTdmNetwork.sendToPlayer(new MatchShopSyncPacket(new MatchShopView(open, remaining,
                economy.matchBalance(player.getUUID()), GunDurabilityAdapter.loaded(),
                gunDamage, gunMax, attachmentDamage, products,
                open ? "" : "购买阶段已结束。", false, requestId)), player);
    }

    private List<MatchShopProduct> products(ServerPlayer player, EconomyManager economy,
                                            ItemStack held, int gunDamage, int gunMax,
                                            int attachmentDamage) {
        List<MatchShopProduct> products = new ArrayList<>();
        for (WeaponRepositoryItem item : match.weapons().repository()) {
            if (!item.enabled() || !shopCategory(item.categoryId())) continue;
            ItemStack stack = item.snapshot().stack();
            if (stack.isEmpty()) continue;
            int price = economy.price(item.id(), item.categoryId(), Math.max(1, stack.getCount()));
            products.add(MatchShopProduct.item("repo:" + item.id(), item.categoryId(),
                    item.title(), price, item.snapshot()));
        }
        if (GunDurabilityAdapter.loaded()) {
            if (gunDamage > 0 && gunMax > 0) {
                int price = repairPrice(economy, "service_repair_gun", gunDamage, gunMax);
                products.add(new MatchShopProduct(REPAIR_GUN_ID, "service_repair_gun",
                        "维修当前枪械", price, null, MatchShopService.REPAIR_GUN));
            }
            if (attachmentDamage > 0) {
                int max = Math.max(attachmentDamage, 100);
                int price = repairPrice(economy, "service_repair_attachments", attachmentDamage, max);
                products.add(new MatchShopProduct(REPAIR_ATTACHMENTS_ID,
                        "service_repair_attachments", "维修当前配件", price,
                        null, MatchShopService.REPAIR_ATTACHMENTS));
            }
        }
        return List.copyOf(products);
    }

    private int repairPrice(EconomyManager economy, String category, int lost, int maximum) {
        Map<String, Double> variables = new LinkedHashMap<>();
        variables.put("durability_lost", (double) lost);
        variables.put("durability_percent", maximum <= 0 ? 0.0D : lost * 100.0D / maximum);
        return economy.price(category, category, 1, variables);
    }

    private void purchase(ServerPlayer player, String productId) {
        int window = match.economy().config().buyWindowSeconds();
        if (!match.economy().matchActive() || !match.isBuyPhaseOpen(window)) {
            message(player, "当前不在购买阶段。", true);
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (REPAIR_GUN_ID.equals(productId)) {
            purchaseRepair(player, held, MatchShopService.REPAIR_GUN);
        } else if (REPAIR_ATTACHMENTS_ID.equals(productId)) {
            purchaseRepair(player, held, MatchShopService.REPAIR_ATTACHMENTS);
        } else if (productId != null && productId.startsWith("repo:")) {
            purchaseItem(player, productId.substring("repo:".length()));
        } else {
            message(player, "未知商店商品。", true);
        }
    }

    private void purchaseRepair(ServerPlayer player, ItemStack held, MatchShopService service) {
        if (!GunDurabilityAdapter.loaded()) {
            message(player, "未检测到 GunDB/TACZ，无法购买耐久服务。", true);
            return;
        }
        int damage = service == MatchShopService.REPAIR_GUN
                ? GunDurabilityAdapter.damage(held) : GunDurabilityAdapter.attachmentDamage(held);
        if (damage <= 0) {
            message(player, "当前没有需要维修的耐久。", true);
            return;
        }
        int max = service == MatchShopService.REPAIR_GUN
                ? GunDurabilityAdapter.maxDamage(held) : Math.max(damage, 100);
        String category = service == MatchShopService.REPAIR_GUN
                ? "service_repair_gun" : "service_repair_attachments";
        int price = repairPrice(match.economy(), category, damage, max);
        Map<String, Double> variables = new LinkedHashMap<>();
        variables.put("durability_lost", (double) damage);
        variables.put("durability_percent", max <= 0 ? 0.0D : damage * 100.0D / max);
        EconomyManager.PurchaseResult result = match.economy()
                .purchase(player, category, category, 1, true, variables);
        if (!result.success()) {
            message(player, result.message(), true);
            return;
        }
        boolean repaired = service == MatchShopService.REPAIR_GUN
                ? GunDurabilityAdapter.repairGun(held) : GunDurabilityAdapter.repairAttachments(held);
        if (!repaired) {
            match.economy().addBalance(player.getUUID(), price, true);
            message(player, "维修失败，费用已退回。", true);
            return;
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        message(player, "维修完成，扣除 $" + price + "。", false);
    }

    private void purchaseItem(ServerPlayer player, String entryId) {
        WeaponRepositoryItem item = match.weapons().repository().stream()
                .filter(value -> value.id().equals(entryId)).findFirst().orElse(null);
        if (item == null || !item.enabled()) {
            message(player, "商品不存在或已下架。", true);
            return;
        }
        ItemStack stack = item.snapshot().stack();
        if (stack.isEmpty()) {
            message(player, "商品依赖的模组未安装。", true);
            return;
        }
        EconomyManager.PurchaseResult result = match.economy().purchase(player,
                item.id(), item.categoryId(), Math.max(1, stack.getCount()), true);
        if (!result.success()) {
            message(player, result.message(), true);
            return;
        }
        if (!player.getInventory().add(stack.copy())) player.drop(stack, false);
        message(player, result.message() + " " + item.title(), false);
    }

    private static boolean shopCategory(String category) {
        return category != null && (category.startsWith("gun_") || category.startsWith("attachment_")
                || category.equals("ammo") || category.equals("other") || category.equals("external"));
    }

    private static void message(ServerPlayer player, String text, boolean error) {
        player.displayClientMessage(Component.literal(text), false);
    }
}
