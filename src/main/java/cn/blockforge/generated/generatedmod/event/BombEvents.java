package cn.blockforge.generated.generatedmod.event;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.item.ModItems;
import cn.blockforge.generated.generatedmod.match.MatchManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
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
        interact(event);
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        interact(event);
    }

    private static void interact(PlayerInteractEvent event) {
        if (event.getLevel().isClientSide()
                || !(event.getEntity() instanceof ServerPlayer player)) return;
        MatchManager manager = MatchManager.get();
        if (manager == null) return;
        ItemStack stack = event.getItemStack();
        if (stack.is(ModItems.C4.get())) {
            manager.bomb().requestPlant(player);
            event.setCanceled(true);
        } else if (stack.is(ModItems.JAMMER_TABLET.get())) {
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
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (isBombTool(player.getMainHandItem()) || isBombTool(player.getOffhandItem())) {
            event.setCanceled(true);
        }
    }

    private static boolean isBombTool(ItemStack stack) {
        return stack.is(ModItems.C4.get()) || stack.is(ModItems.JAMMER_TABLET.get());
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
