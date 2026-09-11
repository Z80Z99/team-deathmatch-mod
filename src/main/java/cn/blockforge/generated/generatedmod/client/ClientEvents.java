package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientEvents {
    /** K：地图工作台独立入口。 */
    public static final KeyMapping OPEN_MAP_WORKBENCH_KEY = new KeyMapping(
            "key.generated_mod.open_map_workbench",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            "key.categories.generated_mod");
    /** J：游戏大厅选项菜单。 */
    public static final KeyMapping OPEN_LOBBY_KEY = new KeyMapping(
            "key.generated_mod.open_lobby",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_J,
            "key.categories.generated_mod");
    /** H：HUD 配置窗。 */
    public static final KeyMapping OPEN_HUD_KEY = new KeyMapping(
            "key.generated_mod.open_hud",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            "key.categories.generated_mod");
    /** B：对局内商店。 */
    public static final KeyMapping OPEN_MATCH_SHOP_KEY = new KeyMapping(
            "key.generated_mod.open_match_shop",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_B,
            "key.categories.generated_mod");

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MAP_WORKBENCH_KEY);
        event.register(OPEN_LOBBY_KEY);
        event.register(OPEN_HUD_KEY);
        event.register(OPEN_MATCH_SHOP_KEY);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "generated_mod_match_hud", MatchHudOverlay::render);
        event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "generated_mod_scene_hud", SceneHudOverlay::render);
        event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "generated_mod_map_tool_hud", MapToolHud::render);
        event.registerAboveAll("generated_mod_respawn", RespawnOverlay::render);
    }
}
