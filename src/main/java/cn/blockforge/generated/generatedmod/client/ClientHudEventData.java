package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.match.MatchHudEventType;
import cn.blockforge.generated.generatedmod.network.packet.HudEventPacket;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Client-side active event set used by event-specific HUD triggers and data sources. */
public final class ClientHudEventData {
    private static final Map<String, ActiveEvent> ACTIVE = new LinkedHashMap<>();
    private static long revision;

    private ClientHudEventData() { }

    public static synchronized void apply(HudEventPacket packet) {
        ACTIVE.put(packet.type().id(), new ActiveEvent(packet.type(), packet.detail(),
                packet.durationTicks(), packet.durationTicks(), packet.priority()));
        revision++;
    }

    public static synchronized void tick() {
        boolean changed = false;
        var iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            ActiveEvent event = iterator.next().getValue();
            event.remainingTicks--;
            if (event.remainingTicks <= 0) {
                iterator.remove();
                changed = true;
            }
        }
        if (changed) revision++;
    }

    public static synchronized void clear() {
        if (!ACTIVE.isEmpty()) {
            ACTIVE.clear();
            revision++;
        }
    }

    public static synchronized boolean active(String id) {
        return id != null && ACTIVE.containsKey(id);
    }

    public static synchronized ActiveEvent get(String id) {
        return id == null ? null : ACTIVE.get(id);
    }

    public static synchronized boolean active() {
        return !ACTIVE.isEmpty();
    }

    public static synchronized ActiveEvent primary() {
        return ACTIVE.values().stream()
                .max(Comparator.comparingInt(ActiveEvent::priority)
                        .thenComparingInt(ActiveEvent::remainingTicks))
                .orElse(null);
    }

    public static synchronized long revision() {
        return revision;
    }

    public static final class ActiveEvent {
        private final MatchHudEventType type;
        private final String detail;
        private final int totalTicks;
        private final int priority;
        private int remainingTicks;

        private ActiveEvent(MatchHudEventType type, String detail, int totalTicks,
                            int remainingTicks, int priority) {
            this.type = type;
            this.detail = detail;
            this.totalTicks = totalTicks;
            this.remainingTicks = remainingTicks;
            this.priority = priority;
        }

        public MatchHudEventType type() { return type; }
        public String detail() { return detail; }
        public int totalTicks() { return totalTicks; }
        public int remainingTicks() { return Math.max(0, remainingTicks); }
        public int priority() { return priority; }
        public double ratio() { return Math.max(0.0D, Math.min(1.0D, remainingTicks / (double) totalTicks)); }
    }
}
