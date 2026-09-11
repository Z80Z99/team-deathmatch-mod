package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.client.screen.HudLayoutScreen;
import cn.blockforge.generated.generatedmod.client.screen.LobbyMenuScreen;
import cn.blockforge.generated.generatedmod.client.screen.LobbyScreen;
import cn.blockforge.generated.generatedmod.client.screen.MapCreateScreen;
import cn.blockforge.generated.generatedmod.client.screen.MapLibraryScreen;
import cn.blockforge.generated.generatedmod.client.screen.MatchmakingScreen;
import cn.blockforge.generated.generatedmod.client.screen.RoomMapSelectScreen;
import cn.blockforge.generated.generatedmod.client.screen.RoomRulesScreen;
import cn.blockforge.generated.generatedmod.client.screen.RoomScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge 客户端事件：按键入口、暂停菜单入口和连接生命周期。 */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientForgeEvents {
    private ClientForgeEvents() {
    }

    private static boolean previousInMatch;
    private static boolean previousSyncApplied;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && ClientMatchData.movementFrozen()) {
            minecraft.player.input.leftImpulse = 0.0F;
            minecraft.player.input.forwardImpulse = 0.0F;
            minecraft.player.input.up = false;
            minecraft.player.input.down = false;
            minecraft.player.input.left = false;
            minecraft.player.input.right = false;
            minecraft.player.input.jumping = false;
            minecraft.player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        }
        ClientMatchData.tick();
        RespawnOverlay.tick();
        ClientLobbyData.tick();
        MapPreview.tick(minecraft);
        closeLobbyScreensWhenMatchStarts(minecraft);
        if (ClientMatchData.consumeKillFeedSound() && minecraft.player != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                    SoundEvents.PLAYER_ATTACK_CRIT, 1.35F, 0.45F));
        }
        boolean available = minecraft.player != null && minecraft.getConnection() != null;
        while (ClientEvents.OPEN_MAP_WORKBENCH_KEY.consumeClick()) {
            if (available && minecraft.screen == null) {
                MapLibraryScreen.open(null);
            }
        }
        while (ClientEvents.OPEN_LOBBY_KEY.consumeClick()) {
            if (available && minecraft.screen == null) {
                LobbyMenuScreen.open(null);
            }
        }
        while (ClientEvents.OPEN_HUD_KEY.consumeClick()) {
            if (available && minecraft.screen == null) {
                minecraft.setScreen(new HudLayoutScreen(null));
            }
        }
    }

    /** 匹配成功后比赛状态一旦激活，自动关闭房间/匹配等大厅界面，交还视角控制。 */
    private static void closeLobbyScreensWhenMatchStarts(Minecraft minecraft) {
        boolean inMatch = ClientMatchData.inMatch();
        boolean syncApplied = ClientMatchData.hasReceivedSync();
        if (syncApplied && !previousSyncApplied) {
            // 登录后的首个同步包只记录基线，避免中途进服就被弹界面。
            previousInMatch = inMatch;
        }
        previousSyncApplied = syncApplied;
        if (syncApplied && inMatch && !previousInMatch
                && minecraft.screen != null && isLobbyScreen(minecraft.screen)) {
            minecraft.setScreen(null);
            if (minecraft.mouseHandler != null) {
                minecraft.mouseHandler.grabMouse();
            }
        }
        previousInMatch = inMatch;
    }

    private static boolean isLobbyScreen(Screen screen) {
        return screen instanceof cn.blockforge.generated.generatedmod.client.ui.UiScreen || screen instanceof LobbyMenuScreen
                || screen instanceof MatchmakingScreen || screen instanceof RoomScreen
                || screen instanceof RoomRulesScreen || screen instanceof RoomMapSelectScreen
                || screen instanceof MapCreateScreen || screen instanceof MapLibraryScreen;
    }

    @SubscribeEvent
    public static void onPauseScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen pauseScreen)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            return;
        }
        int left = Math.max(4, pauseScreen.width - 108);
        Button lobbyButton = Button.builder(Component.literal("游戏大厅"),
                        ignored -> LobbyMenuScreen.open(pauseScreen))
                .bounds(left, 8, 100, 20)
                .tooltip(Tooltip.create(Component.literal("选择入口：房间大厅或快速匹配，两者彼此独立。")))
                .build();
        Button mapButton = Button.builder(Component.literal("地图工作台"),
                        ignored -> MapLibraryScreen.open(pauseScreen))
                .bounds(left, 32, 100, 20)
                .tooltip(Tooltip.create(Component.literal("独立制图入口：制作、导入并编辑属于自己的地图。")))
                .build();
        Button hudButton = Button.builder(Component.literal("HUD 配置窗"),
                        ignored -> minecraft.setScreen(new HudLayoutScreen(pauseScreen)))
                .bounds(left, 56, 100, 20)
                .tooltip(Tooltip.create(Component.literal("可伸缩实时配置：拖动位置、参考线、背景图。")))
                .build();
        event.addListener(lobbyButton);
        event.addListener(mapButton);
        event.addListener(hudButton);
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof net.minecraft.client.gui.screens.DeathScreen
                && RespawnOverlay.eligible()) {
            RespawnOverlay.onDeathScreen();
            event.setNewScreen(null);
        }
    }

    @SubscribeEvent
    public static void onVanillaOverlay(net.minecraftforge.client.event.RenderGuiOverlayEvent.Pre event) {
        if (!RespawnOverlay.active() || Minecraft.getInstance().screen != null) return;
        var id = event.getOverlay().id();
        if (id.equals(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.CROSSHAIR.id())
                || id.equals(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.HOTBAR.id())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (RespawnOverlay.shouldHideLocalPlayer(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    /** Shakes the rendered world itself; HUD text remains readable and no player rotation is changed. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!ClientMatchData.boundaryOutside) return;
        float strength = BoundaryEffects.shake(ClientMatchData.boundaryTicks);
        if (strength <= 0.0F) return;
        double time = System.nanoTime() / 1_000_000_000.0D;
        double frequency = BoundaryEffects.shakeFrequency(ClientMatchData.boundaryTicks);
        event.setYaw(event.getYaw() + (float) Math.sin(time * frequency) * strength);
        event.setPitch(event.getPitch() + (float) Math.sin(time * frequency * 1.19D + 1.2D) * strength * 0.72F);
        event.setRoll(event.getRoll() + (float) Math.sin(time * frequency * 0.83D + 2.4D) * strength * 0.48F);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientConfigData.clear();
        ClientLobbyData.clear();
        ClientMapEditorData.clear();
        ClientMatchData.clear();
        ClientHudDynamicStats.clear();
        HudBackground.clear();
    }
}
