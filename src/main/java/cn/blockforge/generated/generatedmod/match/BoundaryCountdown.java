package cn.blockforge.generated.generatedmod.match;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BoundaryCountdown {
    public static final int DURATION = 200;
    private final Map<UUID, State> states = new HashMap<>();

    public int update(UUID player, boolean outside, long now) {
        State state = states.get(player);
        if (state == null) {
            if (!outside) return 0;
            state = new State(DURATION, now, true);
            states.put(player, state);
        }
        long elapsed = Math.max(0L, now - state.lastTick);
        state.lastTick = now;
        state.outside = outside;
        state.remaining = outside ? Math.max(0, state.remaining - elapsed)
                : Math.min(DURATION, state.remaining + elapsed);
        // Once fully recovered there is no state to synchronize until the next boundary exit.
        if (!outside && state.remaining >= DURATION) {
            states.remove(player);
            return 0;
        }
        return (int) state.remaining;
    }

    public int remaining(UUID player, long now) {
        State state = states.get(player);
        if (state == null) return 0;
        long elapsed = Math.max(0L, now - state.lastTick);
        return (int) (state.outside ? Math.max(0, state.remaining - elapsed)
                : Math.min(DURATION, state.remaining + elapsed));
    }

    public boolean isOutside(UUID player) {
        State state = states.get(player);
        return state != null && state.outside;
    }

    public void clear() { states.clear(); }

    private static final class State {
        private long remaining;
        private long lastTick;
        private boolean outside;
        private State(long remaining, long lastTick, boolean outside) {
            this.remaining = remaining;
            this.lastTick = lastTick;
            this.outside = outside;
        }
    }
}
