package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.match.MatchState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.lang.reflect.Method;
import java.util.List;

/** Lightweight death treatment over the live world, without camera or shader mutation. */
public final class RespawnOverlay {
    private static final RespawnTimeline TIMELINE = new RespawnTimeline();
    private RespawnOverlay() { }
    private static CameraType previousCamera;
    private static boolean cameraLocked;
    private static boolean ragdollSpawned;
    private static int requestCooldown;
    private static double now() { return System.nanoTime() / 1_000_000_000.0; }
    public static boolean eligible() {
        return ClientMatchData.state == MatchState.PLAYING && ClientMatchData.myTeam.isPlayable()
                && !ClientMatchData.pending;
    }
    public static boolean active() { return TIMELINE.phase() != RespawnTimeline.Phase.HIDDEN; }
    /** The mouse handler consumes look deltas while the death camera owns the view. */
    public static boolean shouldLockViewInput() {
        return ClientMatchData.movementFrozen()
                || (eligible() && (ClientMatchData.awaitingRespawn || (active() && !returning())));
    }
    public static void onDeathScreen() {
        TIMELINE.provisionalDeath(now());
        spawnPhysicsRagdoll();
    }
    public static boolean deathActive() { return TIMELINE.deathActive(); }
    public static boolean returning() { return TIMELINE.returning(); }
    public static void clear() {
        if (cameraLocked) restoreCamera(Minecraft.getInstance());
        TIMELINE.clear();
        requestCooldown = 0;
        ragdollSpawned = false;
    }
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) { clear(); return; }
        TIMELINE.update(eligible(), ClientMatchData.awaitingRespawn,
                mc.player.isAlive() && !mc.player.isSpectator(), now());
        boolean observingDeath = ClientMatchData.awaitingRespawn || (active() && !returning());
        if (observingDeath) {
            lockCamera(mc);
            boolean movement = mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
                    || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown()
                    || mc.options.keyJump.isDown();
            if (requestCooldown > 0) requestCooldown--;
            if (ClientMatchData.awaitingRespawn && ClientMatchData.respawnRemainingTicks <= 0
                    && movement && requestCooldown == 0) {
                cn.blockforge.generated.generatedmod.network.FpsTdmNetwork.sendToServer(
                        new cn.blockforge.generated.generatedmod.network.packet.RespawnRequestPacket());
                requestCooldown = 10;
            }
            mc.player.setDeltaMovement(0, 0, 0);
        } else {
            restoreCamera(mc);
            ragdollSpawned = false;
        }
    }

    private static void lockCamera(Minecraft mc) {
        if (cameraLocked || mc.player == null) return;
        previousCamera = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        cameraLocked = true;
    }

    private static void restoreCamera(Minecraft mc) {
        if (!cameraLocked) return;
        if (mc != null && mc.options != null && previousCamera != null) mc.options.setCameraType(previousCamera);
        previousCamera = null;
        cameraLocked = false;
    }

    /**
     * Physics Mod creates its ragdoll from the client-side death call. The local player normally
     * becomes a spectator immediately in this mode, so invoke its public renderer hook once and
     * hide only the standing local model while the fixed death camera is active.
     */
    private static void spawnPhysicsRagdoll() {
        Minecraft minecraft = Minecraft.getInstance();
        if (ragdollSpawned || minecraft.player == null || minecraft.level == null) return;
        try {
            Class<?> physics = Class.forName("net.diebuddies.physics.PhysicsMod");
            Method blockify = physics.getMethod("blockifyEntity", net.minecraft.world.level.Level.class,
                    net.minecraft.world.entity.LivingEntity.class);
            blockify.invoke(null, minecraft.level, minecraft.player);
            ragdollSpawned = true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Physics Mod is optional. Vanilla death handling stays unchanged when it is absent.
        }
    }

    /** Hide the live local body after Physics Mod has captured it for its ragdoll. */
    public static boolean shouldHideLocalPlayer(net.minecraft.world.entity.player.Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == player) {
            return net.minecraftforge.fml.ModList.get().isLoaded("physicsmod")
                    && ragdollSpawned && active() && !returning();
        }
        return ClientMatchData.inMatch() && ClientMatchData.downedPlayerIds.contains(player.getUUID());
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partial, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (!active() || mc.screen != null || width < 80 || height < 80) return;
        HudContext context = HudContext.match(ClientMatchData.mode);
        var sceneElements = ClientHudLayout.customElements(context);
        List<ClientHudLayout.CustomElement> configured = sceneElements.stream()
                .filter(element -> element.type().equals("respawn") && element.visible()
                        && HudConditions.visible(element, sceneElements, false,
                        context)).toList();
        if (configured.isEmpty()) return;
        ClientHudLayout.CustomElement element = configured.get(0);
        double time = now();
        float fade = TIMELINE.fade(time);
        if (fade < 0.02F) return;
        boolean returning = TIMELINE.phase() == RespawnTimeline.Phase.RETURNING;
        float impact = (float) Math.max(0, 1 - TIMELINE.age(time) / 1.2);
        int opacity = Math.max(0, Math.min(100, element.opacityPercent()));
        int effectRgb = element.color() & 0xFFFFFF;
        // Brief desaturation-like darkening on impact, then settle into the readable respawn overlay.
        graphics.fill(0, 0, width, height, color((int) ((42 + impact * 85) * fade * opacity / 100F), 0x080B0D));
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
        for (ClientHudLayout.CustomElement configuredElement : configured) {
            renderCard(graphics, mc.font, configuredElement, context, width, height,
                    fade, impact, returning, partial, time);
        }
    }

    private static void renderCard(GuiGraphics graphics, Font font,
                                   ClientHudLayout.CustomElement element, HudContext context,
                                   int width, int height, float fade, float impact,
                                   boolean returning, float partial, double time) {
        double amount = HudAnimation.frame(HudAnimation.key(element.id()), true,
                element.placement());
        if (amount <= 0.0D) return;
        HudGeometry.Rect rect = HudGeometry.custom(element, width, height);
        float alpha = fade * (float) amount;
        int opacity = Math.max(0, Math.min(100, element.opacityPercent()));
        int effectRgb = element.color() & 0xFFFFFF;
        int accent = returning ? 0xA7DDD1 : effectRgb;
        int white = color((int) (255 * alpha), 0xF2F4F5);
        int muted = color((int) (235 * alpha), 0xBBC2C7);
        int left = rect.left();
        int top = rect.top();
        int panelWidth = rect.width();
        if (element.shadow()) graphics.fill(left + 2, top - 2, rect.right() + 2, rect.bottom() + 2,
                color((int) (95 * alpha * opacity / 100F),
                        element.placement().shadowColor() == 0 ? 0x000000
                                : element.placement().shadowColor()));
        if (element.background()) graphics.fill(left, top - 4, rect.right(), rect.bottom(),
                color((int) (155 * alpha * opacity / 100F),
                        element.placement().backgroundColor() == 0 ? 0x080B0D
                                : element.placement().backgroundColor()));
        if (element.border()) graphics.renderOutline(left, top - 4, panelWidth, rect.height(),
                color((int) (255 * alpha), element.placement().borderColor() == 0
                        ? accent : element.placement().borderColor()));
        graphics.fill(left, top, left + 24, top + 2, color((int) (255 * alpha), accent));
        String title = HudCustomRenderer.resolveText(element, context, false);
        if (title.isBlank()) title = returning ? "重返战场" : "已阵亡";
        float textScale = Math.max(0.5F, Math.min(3.0F, element.scalePercent() / 100.0F));
        drawAligned(graphics, font, title, rect.left() + 4, rect.right() - 4, top + 9,
                element.placement().alignment(), textScale, white);
        String detail = returning ? "" : ClientMatchData.deathLabel;
        drawAligned(graphics, font, detail, rect.left() + 4, rect.right() - 4, top + 24,
                element.placement().alignment(), textScale, muted);
        int remaining = ClientMatchData.respawnRemainingTicks;
        boolean elimination = !ClientMatchData.mode.respawnRules();
        String status = returning ? "部署完成" : elimination ? "本回合已阵亡 · 等待下一回合"
                : remaining > 0 ? "重新部署"
                : "复活已就绪 · 按移动键或跳跃键回归";
        int statusY = Math.max(top + 40, rect.bottom() - 20);
        drawAligned(graphics, font, status, rect.left() + 4, rect.right() - 4, statusY,
                element.placement().alignment(), textScale, white);
        if (!returning && !elimination && remaining > 0) {
            String seconds = Integer.toString(remaining / 20 + (remaining % 20 == 0 ? 0 : 1));
            drawAligned(graphics, font, seconds, rect.left() + 4, rect.right() - 4, statusY - 2,
                    "right", textScale * 1.45F, white);
        }
        int barY = Math.max(top + 4, rect.bottom() - 5);
        int barBottom = Math.min(rect.bottom(), barY + 3);
        if (!elimination) {
            float fallback = ClientMatchData.respawnTotalTicks <= 0 ? 1.0F
                    : Math.max(0, Math.min(1, remaining / (float) ClientMatchData.respawnTotalTicks));
            double ratio = HudCustomRenderer.resolveRatio(element, context, false, fallback);
            int inner = Math.max(1, panelWidth);
            int filled = returning ? inner : (int) Math.round(inner * ratio);
            graphics.fill(left, barY, left + panelWidth, barBottom,
                    color((int) (115 * alpha), 0xFFFFFF));
            if ("drain".equals(element.placement().progressDirection())) {
                graphics.fill(rect.right() - filled, barY, rect.right(), barBottom,
                        color((int) (255 * alpha), 0xA7DDD1));
            } else {
                graphics.fill(left, barY, left + filled, barBottom,
                        color((int) (255 * alpha), 0xA7DDD1));
            }
            if (!returning && remaining <= 0) {
                int segment = Math.max(8, panelWidth / 6);
                int offset = (int) ((panelWidth - segment) * (0.5 - 0.5 * Math.cos(time * 2)));
                graphics.fill(left, barY, left + panelWidth, barBottom,
                        color((int) (200 * alpha), 0x343F43));
                graphics.fill(left + offset, barY, left + offset + segment, barBottom,
                        color((int) (255 * alpha), 0xA7DDD1));
            }
        }
        HudCustomRenderer.glow(graphics, element, rect, (int) (255 * alpha));
    }

    private static void drawAligned(GuiGraphics graphics, Font font, String text,
                                    int left, int right, int y, String alignment,
                                    float scale, int color) {
        if (text == null || text.isBlank()) return;
        int maximum = Math.max(1, Math.round((right - left) / Math.max(0.5F, scale)));
        String fitted = font.plainSubstrByWidth(text, maximum);
        float width = font.width(fitted) * scale;
        float x = switch (alignment == null ? "center" : alignment) {
            case "left" -> left;
            case "right" -> right - width;
            default -> (left + right - width) / 2.0F;
        };
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, fitted, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static int color(int alpha, int rgb) { return (Math.max(0, Math.min(255, alpha)) << 24) | rgb; }
}
