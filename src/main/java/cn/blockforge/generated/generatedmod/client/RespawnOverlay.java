package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.match.MatchState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.Optional;

/** Lightweight death treatment over the live world, without camera or shader mutation. */
public final class RespawnOverlay {
    private static final RespawnTimeline TIMELINE = new RespawnTimeline();
    private RespawnOverlay() { }
    private static CameraType previousCamera;
    private static boolean cameraLocked;
    private static float lockedYaw;
    private static float lockedPitch;
    private static int requestCooldown;
    private static double now() { return System.nanoTime() / 1_000_000_000.0; }
    public static boolean eligible() {
        return ClientMatchData.state == MatchState.PLAYING && ClientMatchData.myTeam.isPlayable()
                && !ClientMatchData.pending;
    }
    public static boolean active() { return TIMELINE.phase() != RespawnTimeline.Phase.HIDDEN; }
    public static void onDeathScreen() { TIMELINE.provisionalDeath(now()); }
    public static boolean deathActive() { return TIMELINE.deathActive(); }
    public static boolean returning() { return TIMELINE.returning(); }
    public static void clear() {
        if (cameraLocked) restoreCamera(Minecraft.getInstance());
        TIMELINE.clear();
        requestCooldown = 0;
    }
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) { clear(); return; }
        TIMELINE.update(eligible(), ClientMatchData.awaitingRespawn,
                mc.player.isAlive() && !mc.player.isSpectator(), now());
        if (ClientMatchData.awaitingRespawn) {
            lockCamera(mc);
            boolean movement = mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
                    || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown()
                    || mc.options.keyJump.isDown();
            if (requestCooldown > 0) requestCooldown--;
            if (ClientMatchData.respawnRemainingTicks <= 0 && movement && requestCooldown == 0) {
                cn.blockforge.generated.generatedmod.network.FpsTdmNetwork.sendToServer(
                        new cn.blockforge.generated.generatedmod.network.packet.RespawnRequestPacket());
                requestCooldown = 10;
            }
            mc.player.setYRot(lockedYaw);
            mc.player.setXRot(lockedPitch);
            mc.player.setDeltaMovement(0, 0, 0);
        } else {
            restoreCamera(mc);
        }
    }

    private static void lockCamera(Minecraft mc) {
        if (cameraLocked || mc.player == null) return;
        previousCamera = mc.options.getCameraType();
        lockedYaw = mc.player.getYRot();
        lockedPitch = mc.player.getXRot();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        cameraLocked = true;
    }

    private static void restoreCamera(Minecraft mc) {
        if (!cameraLocked) return;
        if (mc != null && mc.options != null && previousCamera != null) mc.options.setCameraType(previousCamera);
        previousCamera = null;
        cameraLocked = false;
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partial, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (!active() || mc.screen != null || width < 80 || height < 80) return;
        var sceneElements = ClientHudLayout.customElements(HudContext.match(ClientMatchData.mode));
        Optional<ClientHudLayout.CustomElement> configured = sceneElements.stream()
                .filter(element -> element.type().equals("respawn") && element.visible()
                        && HudConditions.visible(element, sceneElements, false)).findFirst();
        if (configured.isEmpty()) return;
        ClientHudLayout.CustomElement element = configured.get();
        HudGeometry.Rect rect = HudGeometry.custom(element, width, height);
        double time = now();
        float fade = TIMELINE.fade(time);
        if (fade < 0.02F) return;
        boolean returning = TIMELINE.phase() == RespawnTimeline.Phase.RETURNING;
        float impact = (float) Math.max(0, 1 - TIMELINE.age(time) / 1.2);
        int opacity = Math.max(0, Math.min(100, element.opacityPercent()));
        int effectRgb = element.color() & 0xFFFFFF;
        graphics.fill(0, 0, width, height, color((int) ((35 + impact * 55) * fade * opacity / 100F), 0x080B0D));
        int edge = Math.max(8, Math.min(width, height) / 5);
        int edgeColor = color((int) ((75 + impact * 85) * fade * opacity / 100F), effectRgb);
        graphics.fillGradient(0, 0, width, edge, edgeColor, 0x006D111A);
        graphics.fillGradient(0, height - edge, width, height, 0x006D111A, edgeColor);
        for (int i = 0; i < 20; i++) {
            int x0 = edge * i / 20, x1 = edge * (i + 1) / 20;
            int c = color((int) ((55 + impact * 60) * fade * opacity / 100F * (1F - i / 20F)), effectRgb);
            graphics.fill(x0, 0, x1, height, c);
            graphics.fill(width - x1, 0, width - x0, height, c);
        }
        int panelWidth = rect.width();
        int left = rect.left();
        int top = rect.top();
        int white = color((int) (255 * fade), 0xF2F4F5);
        int muted = color((int) (235 * fade), 0xBBC2C7);
        int accent = returning ? 0xA7DDD1 : effectRgb;
        // A local scrim keeps labels readable against bright terrain without framing a card.
        if (element.background()) graphics.fill(left, top - 4, rect.right(), rect.bottom(),
                color((int) (155 * fade * opacity / 100F), 0x080B0D));
        if (element.border()) graphics.renderOutline(left, top - 4, panelWidth, rect.height(),
                color((int) (255 * fade), accent));
        graphics.fill(left, top, left + 24, top + 2, color((int) (255 * fade), accent));
        graphics.drawString(mc.font, returning ? "重返战场" : "已阵亡", left, top + 10, white, false);
        String detail = mc.font.plainSubstrByWidth(ClientMatchData.deathLabel, panelWidth);
        if (returning) detail = "";
        graphics.drawString(mc.font, detail, left, top + 25, muted, false);
        int remaining = ClientMatchData.respawnRemainingTicks;
        boolean elimination = !ClientMatchData.mode.respawnRules();
        String status = returning ? "部署完成" : elimination ? "本回合已阵亡 · 等待下一回合"
                : remaining > 0 ? "重新部署"
                : "复活已就绪 · 按移动键或跳跃键回归";
        graphics.drawString(mc.font, mc.font.plainSubstrByWidth(status, panelWidth), left, top + 48, white, false);
        if (!returning && !elimination && remaining > 0) {
            String seconds = Integer.toString(remaining / 20 + (remaining % 20 == 0 ? 0 : 1));
            float scale = 1.6F;
            int numberWidth = (int) Math.ceil(mc.font.width(seconds) * scale);
            graphics.pose().pushPose();
            graphics.pose().translate(left + panelWidth - numberWidth, top + 43, 0);
            graphics.pose().scale(scale, scale, 1);
            graphics.drawString(mc.font, seconds, 0, 0, white, false);
            graphics.pose().popPose();
        }
        if (!elimination) {
            graphics.fill(left, top + 64, left + panelWidth, top + 67, color((int) (115 * fade), 0xFFFFFF));
            int total = ClientMatchData.respawnTotalTicks;
            float progress = total <= 0 ? 1 : Math.max(0, Math.min(1, 1 - (remaining - partial) / total));
            int filled = returning ? panelWidth : (int) (panelWidth * progress);
            graphics.fill(left, top + 64, left + filled, top + 67, color((int) (255 * fade), 0xA7DDD1));
            if (!returning && remaining <= 0) {
                // Back-and-forth movement signals activity without pretending a spawn is ready.
                int segment = Math.max(8, panelWidth / 6);
                int offset = (int) ((panelWidth - segment) * (0.5 - 0.5 * Math.cos(time * 2)));
                graphics.fill(left, top + 64, left + panelWidth, top + 67, color((int) (200 * fade), 0x343F43));
                graphics.fill(left + offset, top + 64, left + offset + segment, top + 67,
                        color((int) (255 * fade), 0xA7DDD1));
            }
        }
    }

    private static int color(int alpha, int rgb) { return (Math.max(0, Math.min(255, alpha)) << 24) | rgb; }
}
