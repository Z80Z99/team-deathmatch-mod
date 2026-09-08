package cn.blockforge.generated.generatedmod.event;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.item.ModItems;
import cn.blockforge.generated.generatedmod.match.MatchManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
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
                || !event.getItemStack().is(ModItems.MAP_BRUSH.get())) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && MatchManager.get() != null) {
            // The validated custom packet owns editing; vanilla events only suppress world interaction.
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND
                || !event.getItemStack().is(ModItems.MAP_BRUSH.get())) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || MatchManager.get() == null) {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND
                || !event.getItemStack().is(ModItems.MAP_BRUSH.get())) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && MatchManager.get() != null
                && player.isShiftKeyDown()) {
            event.setCanceled(true);
        }
    }
}
