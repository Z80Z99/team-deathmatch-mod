package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.match.MatchState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Lightweight death treatment over the live world, without camera or shader mutation. */
public final class RespawnOverlay {
    private static final RespawnTimeline TIMELINE = new RespawnTimeline();
    private RespawnOverlay() { }
    private static double now() { return System.nanoTime() / 1_000_000_000.0; }
    public static boolean eligible() {
        return ClientMatchData.state == MatchState.PLAYING && ClientMatchData.myTeam.isPlayable()
                && !ClientMatchData.pending;
    }
    public static boolean active() { return TIMELINE.phase() != RespawnTimeline.Phase.HIDDEN; }
    public static void onDeathScreen() { TIMELINE.provisionalDeath(now()); }
    public static void clear() { TIMELINE.clear(); }
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) { clear(); return; }
        TIMELINE.update(eligible(), ClientMatchData.awaitingRespawn,
                mc.player.isAlive() && !mc.player.isSpectator(), now());
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partial, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (!active() || mc.screen != null || width < 80 || height < 80) return;
        double time = now();
        float fade = TIMELINE.fade(time);
        if (fade < 0.02F) return;
        boolean returning = TIMELINE.phase() == RespawnTimeline.Phase.RETURNING;
        float impact = (float) Math.max(0, 1 - TIMELINE.age(time) / 1.2);
        graphics.fill(0, 0, width, height, color((int) ((60 + impact * 60) * fade), 0x080B0D));
        int edge = Math.max(8, Math.min(width, height) / 5);
        int edgeColor = color((int) ((75 + impact * 85) * fade), 0x6D111A);
        graphics.fillGradient(0, 0, width, edge, edgeColor, 0x006D111A);
        graphics.fillGradient(0, height - edge, width, height, 0x006D111A, edgeColor);
        for (int i = 0; i < 20; i++) {
            int x0 = edge * i / 20, x1 = edge * (i + 1) / 20;
            int c = color((int) ((55 + impact * 60) * fade * (1F - i / 20F)), 0x6D111A);
            graphics.fill(x0, 0, x1, height, c);
            graphics.fill(width - x1, 0, width - x0, height, c);
        }
        int center = width / 2;
        int panelWidth = Math.min(300, width - 32);
        int left = center - panelWidth / 2;
        int top = Math.max(8, height - 116);
        int white = color((int) (255 * fade), 0xF2F4F5);
        int muted = color((int) (235 * fade), 0xBBC2C7);
        int accent = returning ? 0xA7DDD1 : 0xE65861;
        // A local scrim keeps labels readable against bright terrain without framing a card.
        graphics.fillGradient(0, Math.max(0, top - 18), width, height,
                0x00080B0D, color((int) (150 * fade), 0x080B0D));
        graphics.fill(left, top, left + 24, top + 2, color((int) (255 * fade), accent));
        graphics.drawString(mc.font, returning ? "重返战场" : "已阵亡", left, top + 10, white, false);
        String detail = mc.font.plainSubstrByWidth(ClientMatchData.deathLabel, panelWidth);
        if (returning) detail = "";
        graphics.drawString(mc.font, detail, left, top + 25, muted, false);
        int remaining = ClientMatchData.respawnRemainingTicks;
        boolean elimination = !ClientMatchData.mode.respawnRules();
        String status = returning ? "部署完成" : elimination ? "本回合已阵亡 · 等待下一回合"
                : remaining > 0 ? "重新部署"
                : "正在寻找安全复活位置";
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
