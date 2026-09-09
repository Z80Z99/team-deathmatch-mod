package cn.blockforge.generated.generatedmod.match;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BoundaryCountdown {
    public static final int DURATION = 200;
    private final Map<UUID, Long> deadlines = new HashMap<>();
    public int update(UUID player, boolean outside, long now) {
        if (!outside) { deadlines.remove(player); return 0; }
        return (int) Math.max(0, deadlines.computeIfAbsent(player, id -> now + DURATION) - now);
    }
    public int remaining(UUID player, long now) {
        return (int) Math.max(0, deadlines.getOrDefault(player, now) - now);
    }
    public void clear() { deadlines.clear(); }
}
