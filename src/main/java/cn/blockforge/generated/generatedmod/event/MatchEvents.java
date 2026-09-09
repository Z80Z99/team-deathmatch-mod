package cn.blockforge.generated.generatedmod.event;

import cn.blockforge.generated.generatedmod.match.MatchManager;
import cn.blockforge.generated.generatedmod.match.HudStatStore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 将比赛管理器接到 Forge 的服务器事件总线上。
 */
public final class MatchEvents {
    private MatchEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MatchManager.init(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MatchManager.shutdown();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MatchManager manager = MatchManager.get();
        if (manager != null) {
            manager.tick();
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MatchManager manager = MatchManager.get();
        if (manager != null) {
            manager.onPlayerLogin(player);
        }
        HudStatStore.get().sendTo(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MatchManager manager = MatchManager.get();
        if (manager != null) {
            manager.onPlayerLogout(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MatchManager manager = MatchManager.get();
        if (manager != null) {
            manager.handlePlayerRespawn(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MatchManager manager = MatchManager.get();
        if (manager != null) {
            manager.onPlayerKilled(player, event.getSource(), event::isCanceled);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        MatchManager manager = MatchManager.get();
        if (manager != null && manager.shouldCancelAttack(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MatchManager manager = MatchManager.get();
        if (manager == null) {
            return;
        }
        // Preserve lethal damage so vanilla death listeners receive the real event.
        manager.recordDamage(player, event.getSource(), event.getAmount());
        if (manager.handleFatalDamage(player, event.getSource(), event.getAmount())) {
            event.setAmount(0.0F);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MatchManager manager = MatchManager.get();
        if (manager != null && manager.shouldCancelInteraction(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MatchManager manager = MatchManager.get();
        if (manager != null && manager.shouldCancelInteraction(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer)) {
            return;
        }
        MatchManager manager = MatchManager.get();
        if (manager != null && (manager.shouldCancelBlockAction((ServerPlayer) event.getPlayer())
                || manager.shouldProtectBlock(event.getLevel(), event.getPos()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MatchManager manager = MatchManager.get();
        if (manager != null && manager.shouldCancelBlockAction(player)) {
            event.setCanceled(true);
        }
        if (manager != null && manager.shouldProtectBlock(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        MatchManager manager = MatchManager.get();
        if (manager == null || !manager.isMapResetting()) {
            return;
        }
        manager.removeResetRegionExplosionBlocks(event);
    }
}
