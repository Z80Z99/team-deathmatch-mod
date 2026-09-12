package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.match.MatchHudEventType;
import cn.blockforge.generated.generatedmod.network.packet.HudEventPacket;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Client-side active event set used by event-specific HUD triggers and data sources. */
public final class ClientHudEventData {
    private static final Map<String, ActiveEvent> ACTIVE = new LinkedHashMap<>();
    private static final Map<String, Descriptor> REGISTERED = new LinkedHashMap<>();
    private static long revision;

    public record Descriptor(String id, String title, String defaultDetail,
                             int durationTicks, int priority,
                             java.util.Set<HudContext> contexts) {
        public Descriptor(String id, String title, String defaultDetail,
                          int durationTicks, int priority) {
            this(id, title, defaultDetail, durationTicks, priority,
                    java.util.EnumSet.of(HudContext.TEAM_DEATHMATCH,
                            HudContext.SEARCH_DESTROY, HudContext.LAST_STANDING));
        }

        public Descriptor {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("event id is required");
            title = title == null || title.isBlank() ? id : title;
            defaultDetail = defaultDetail == null ? "" : defaultDetail;
            durationTicks = Math.max(1, durationTicks);
            priority = Math.max(0, Math.min(100, priority));
            contexts = contexts == null || contexts.isEmpty()
                    ? java.util.EnumSet.of(HudContext.TEAM_DEATHMATCH,
                    HudContext.SEARCH_DESTROY, HudContext.LAST_STANDING)
                    : java.util.Set.copyOf(contexts);
        }
    }

    private ClientHudEventData() { }

    public static synchronized void apply(HudEventPacket packet) {
        ACTIVE.put(packet.id(), new ActiveEvent(packet.id(), packet.title(), packet.detail(),
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

    public static synchronized void register(Descriptor descriptor) {
        if (descriptor == null) return;
        if (MatchHudEventType.byId(descriptor.id()) != null) {
            throw new IllegalArgumentException("HUD event ID conflicts with built-in event: "
                    + descriptor.id());
        }
        if (!REGISTERED.containsKey(descriptor.id()) && REGISTERED.size() >= 128) {
            throw new IllegalArgumentException("HUD custom event count limit reached");
        }
        REGISTERED.put(descriptor.id(), descriptor);
    }

    public static synchronized boolean unregister(String id) {
        return id != null && REGISTERED.remove(id) != null;
    }

    public static synchronized java.util.List<Descriptor> descriptors() {
        return java.util.List.copyOf(REGISTERED.values());
    }

    public static synchronized Descriptor descriptor(String id) {
        return id == null ? null : REGISTERED.get(id);
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
        private final String id;
        private final String title;
        private final String detail;
        private final int totalTicks;
        private final int priority;
        private int remainingTicks;

        private ActiveEvent(String id, String title, String detail, int totalTicks,
                            int remainingTicks, int priority) {
            this.id = id;
            this.title = title;
            this.detail = detail;
            this.totalTicks = totalTicks;
            this.remainingTicks = remainingTicks;
            this.priority = priority;
        }

        public String id() { return id; }
        public String title() { return title; }
        public MatchHudEventType type() { return MatchHudEventType.byId(id); }
        public String detail() { return detail; }
        public int totalTicks() { return totalTicks; }
        public int remainingTicks() { return Math.max(0, remainingTicks); }
        public int priority() { return priority; }
        public double ratio() { return Math.max(0.0D, Math.min(1.0D, remainingTicks / (double) totalTicks)); }
    }
}
