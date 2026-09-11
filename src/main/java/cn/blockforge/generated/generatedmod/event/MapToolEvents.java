package cn.blockforge.generated.generatedmod.event;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 服务器侧地图道具输入。 */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MapToolEvents {
    private MapToolEvents() {
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND
                || !isEditorTool(event.getItemStack())) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer) {
            // The validated custom packet owns editing; vanilla events only suppress world interaction.
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && isEditorTool(player.getMainHandItem())) {
            // Creative-mode block breaking does not reliably pass through LeftClickBlock on every client.
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND
                || !isEditorTool(event.getItemStack())) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer)) {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND
                || !isEditorTool(event.getItemStack())) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer) {
            event.setCanceled(true);
        }
    }

    private static boolean isEditorTool(ItemStack stack) {
        return stack.is(ModItems.MAP_PLANNER.get()) || stack.is(ModItems.MAP_BRUSH.get());
    }
}
