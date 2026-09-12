package cn.blockforge.generated.generatedmod.event;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.item.ModItems;
import cn.blockforge.generated.generatedmod.match.MatchManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BombEvents {
    private BombEvents() { }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        interact(event);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        interact(event);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        capture(event);
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        capture(event);
    }

    private static void interact(PlayerInteractEvent event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)) return;
        MatchManager manager = MatchManager.get();
        if (manager == null) return;
        if (event.getItemStack().is(ModItems.C4.get())) {
            manager.bomb().requestPlant(player);
            event.setCanceled(true);
        } else if (event.getItemStack().is(ModItems.JAMMER_TABLET.get())) {
            manager.bomb().requestDefuse(player);
            event.setCanceled(true);
        }
    }

    private static void capture(PlayerInteractEvent event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)) return;
        MatchManager manager = MatchManager.get();
        if (manager != null && manager.bomb().shouldCaptureInteraction(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0) return;
        MatchManager manager = MatchManager.get();
        if (manager != null) manager.bomb().interruptIfOperator(player);
    }

    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MatchManager manager = MatchManager.get();
        if (manager != null) manager.bomb().onItemPickup(player, event.getStack());
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        MatchManager manager = MatchManager.get();
        if (manager != null) manager.bomb().onItemToss(player, event.getEntity());
    }
}
