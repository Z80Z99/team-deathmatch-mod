package cn.blockforge.generated.generatedmod.client;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HudAnimation {
    private record Frame(double amount, long time) { }
    private static final Map<String, Frame> FRAMES = new LinkedHashMap<>();
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
}
