package cn.blockforge.generated.generatedmod.client;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HudAnimation {
    private record Frame(double amount, long time) { }
    private record ContentFrame(String value, long changedAt, double amount) { }
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
        double amount = placement.animation().equals("none") || old == null
                ? (visible ? 1 : 0)
                : advance(old.amount(), visible, (now - old.time()) / 1_000_000D,
                placement.animationMillis());
        trim(FRAMES);
        FRAMES.put(key, new Frame(amount, now));
        return amount;
    }

    public static double contentFrame(String id, String value, ClientHudLayout.Placement placement) {
        String safe = value == null ? "" : value;
        long now = System.nanoTime();
        ContentFrame old = CONTENT.get(id);
        if (old == null || !old.value().equals(safe)) {
            double carried = old == null ? 0.0D : Math.min(1.0D,
                    (now - old.changedAt()) / 1_000_000D
                            / Math.max(1, placement.contentAnimationMillis()));
            CONTENT.put(id, new ContentFrame(safe, now, carried));
            trim(CONTENT);
            return placement.contentAnimation().equals("none") ? 1.0D : carried;
        }
        if (placement.contentAnimation().equals("none")) return 1.0D;
        double amount = Math.min(1.0D, (now - old.changedAt()) / 1_000_000D
                / Math.max(1, placement.contentAnimationMillis()));
        if (amount > old.amount()) {
            CONTENT.put(id, new ContentFrame(safe, old.changedAt(), amount));
        }
        return amount;
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
        trim(PROGRESS);
        return value;
    }

    private static void trim(Map<?, ?> map) {
        while (map.size() > 1024) {
            var iterator = map.entrySet().iterator();
            if (!iterator.hasNext()) return;
            iterator.next();
            iterator.remove();
        }
    }
}
