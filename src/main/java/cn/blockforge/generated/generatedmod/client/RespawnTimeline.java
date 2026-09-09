package cn.blockforge.generated.generatedmod.client;

/** Cosmetic timing only. Server state alone authorizes a return to combat. */
public final class RespawnTimeline {
    public enum Phase { HIDDEN, WAITING, RETURNING }
    private Phase phase = Phase.HIDDEN;
    private double started;
    private double returning;
    private boolean confirmed;
    private float returnOpacity;

    public void provisionalDeath(double now) {
        if (phase != Phase.WAITING) {
            phase = Phase.WAITING;
            started = now;
            confirmed = false;
        }
    }

    public void update(boolean inCombat, boolean waiting, boolean alive, double now) {
        if (!inCombat) { clear(); return; }
        if (waiting) {
            provisionalDeath(now);
            confirmed = true;
        } else if (phase == Phase.WAITING && alive && (confirmed || now - started > 1)) {
            returnOpacity = fade(now);
            phase = Phase.RETURNING;
            returning = now;
        }
        if (phase == Phase.RETURNING && now - returning >= 0.7) clear();
    }

    public Phase phase() { return phase; }
    public double age(double now) { return Math.max(0, now - started); }
    public float fade(double now) {
        if (phase == Phase.HIDDEN) return 0;
        if (phase == Phase.RETURNING) return returnOpacity * (1 - smooth((now - returning) / 0.7));
        return smooth((now - started) / 0.18);
    }
    public static float progress(int remaining, int total) {
        return total <= 0 ? 1 : Math.max(0, Math.min(1, 1F - (float) remaining / total));
    }
    private static float smooth(double value) {
        double x = Math.max(0, Math.min(1, value));
        return (float) (x * x * (3 - 2 * x));
    }
    public void clear() { phase = Phase.HIDDEN; confirmed = false; }
}
