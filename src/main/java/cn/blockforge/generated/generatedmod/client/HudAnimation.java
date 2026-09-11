package cn.blockforge.generated.generatedmod.client;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HudAnimation {
    private record Frame(double amount, long time) { }
    private record ContentFrame(String value, long changedAt) { }
    private record ProgressFrame(double value, long time) { }
    private static final Map<String, Frame> FRAMES = new LinkedHashMap<>();
    private static final Map<String, ContentFrame> CONTENT = new LinkedHashMap<>();
    private static final Map<String, ProgressFrame> PROGRESS = new LinkedHashMap<>();
    private HudAnimation() { }
    public static String key(String id) {
        return (ClientMatchData.inMatch() ? ClientMatchData.mode.name()
                : ClientLobbyData.matchmaking().queued() || ClientLobbyData.dynamicReadySeconds() > 0 ? "matching" : "room") + ":" + id;
    }
    public static boolean disappeared(String id) {
        Frame frame = FRAMES.get(key(id));
        return frame == null || frame.amount() <= 0 || System.nanoTime() - frame.time() > 500_000_000L;
    }
    public static double advance(double current, boolean visible, double elapsedMillis, int duration) {
        double step = Math.max(0, elapsedMillis) / Math.max(1, duration);
        return visible ? Math.min(1, current + step) : Math.max(0, current - step);
    }
    public static double frame(String key, boolean visible, ClientHudLayout.Placement placement) {
        long now = System.nanoTime();
        Frame old = FRAMES.get(key);
        if (old != null && now - old.time() > 500_000_000L) old = null;
        double amount = placement.animation().equals("none") ? (visible ? 1 : 0)
                : advance(old == null ? 0 : old.amount(), visible,
                old == null ? 0 : (now - old.time()) / 1_000_000D, placement.animationMillis());
        if (FRAMES.size() > 1024) FRAMES.clear();
        FRAMES.put(key, new Frame(amount, now));
        return amount;
    }

    public static double contentFrame(String id, String value, ClientHudLayout.Placement placement) {
        String safe = value == null ? "" : value;
        long now = System.nanoTime();
        ContentFrame old = CONTENT.get(id);
        if (old == null || !old.value().equals(safe)) {
            CONTENT.put(id, new ContentFrame(safe, now));
            if (CONTENT.size() > 1024) CONTENT.clear();
            return placement.contentAnimation().equals("none") ? 1.0D : 0.0D;
        }
        if (placement.contentAnimation().equals("none")) return 1.0D;
        return Math.min(1.0D, (now - old.changedAt()) / 1_000_000D
                / Math.max(1, placement.contentAnimationMillis()));
    }

    public static double progressFrame(String id, double target, ClientHudLayout.Placement placement) {
        long now = System.nanoTime();
        ProgressFrame old = PROGRESS.get(id);
        if (old == null) {
            PROGRESS.put(id, new ProgressFrame(target, now));
            return target;
        }
        double elapsed = Math.max(0, now - old.time()) / 1_000_000D;
        double duration = placement.contentAnimation().equals("none") ? 1 : placement.contentAnimationMillis();
        double blend = Math.min(1.0D, elapsed / Math.max(1.0D, duration));
        double value = old.value() + (target - old.value()) * blend;
        PROGRESS.put(id, new ProgressFrame(value, now));
        if (PROGRESS.size() > 1024) PROGRESS.clear();
        return value;
    }
}
