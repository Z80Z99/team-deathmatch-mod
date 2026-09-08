package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.client.screen.MapBrushScreen;
import cn.blockforge.generated.generatedmod.client.screen.MapPlannerScreen;
import cn.blockforge.generated.generatedmod.item.ModItems;
import cn.blockforge.generated.generatedmod.map.MapEditorView;
import cn.blockforge.generated.generatedmod.map.MapRegion;
import cn.blockforge.generated.generatedmod.map.MapRegionAction;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.MapBrushClickPacket;
import cn.blockforge.generated.generatedmod.network.packet.MapRegionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** 客户端侧地图道具交互入口。 */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MapToolClientEvents {
    private MapToolClientEvents() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        handleRightClick(event);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        handleRightClick(event);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        handleLeftClick(event);
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        handleLeftClick(event);
    }

    private static void handleRightClick(PlayerInteractEvent event) {
        if (!event.getLevel().isClientSide()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        boolean planner = event.getItemStack().is(ModItems.MAP_PLANNER.get());
        boolean brush = event.getItemStack().is(ModItems.MAP_BRUSH.get());
        if (!planner && !brush) return;

        if (planner) {
            MapEditorView view = ClientMapEditorData.view();
            List<MapRegion> hits = MapRegionPicker.pick(minecraft, view, 128.0D);
            minecraft.setScreen(new MapPlannerScreen(hits.isEmpty() ? view.regions() : hits));
            if (event.isCancelable()) event.setCanceled(true);
            return;
        }

        if (event.getEntity().isShiftKeyDown()) {
            minecraft.setScreen(new MapBrushScreen());
        } else {
            MapEditorView view = ClientMapEditorData.view();
            FpsTdmNetwork.sendToServer(new MapBrushClickPacket(
                    MapRegionPicker.brushTarget(minecraft, view.brushRange()), false));
        }
        if (event.isCancelable()) event.setCanceled(true);
    }

    private static void handleLeftClick(PlayerInteractEvent event) {
        if (!event.getLevel().isClientSide()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        if (event.getItemStack().is(ModItems.MAP_PLANNER.get())) {
            MapEditorView view = ClientMapEditorData.view();
            List<MapRegion> hits = MapRegionPicker.pick(minecraft, view, 128.0D);
            if (!hits.isEmpty()) {
                MapRegion first = hits.get(0);
                FpsTdmNetwork.sendToServer(new MapRegionPacket(MapRegionAction.SELECT, first, 0));
                minecraft.player.displayClientMessage(Component.literal("已选择区域：" + first.displayName()), true);
            }
            if (event.isCancelable()) event.setCanceled(true);
            return;
        }
        if (event.getItemStack().is(ModItems.MAP_BRUSH.get())) {
            MapEditorView view = ClientMapEditorData.view();
            BlockPos target = MapRegionPicker.brushTarget(minecraft, view.brushRange(), false);
            if (target != null) {
                FpsTdmNetwork.sendToServer(new MapBrushClickPacket(target, true));
            }
            if (event.isCancelable()) event.setCanceled(true);
        }
    }
}
