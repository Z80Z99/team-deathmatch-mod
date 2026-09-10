package cn.blockforge.generated.generatedmod.client;

/** Timing curves shared by the out-of-bounds world camera and HUD filters. */
public final class BoundaryEffects {
    private BoundaryEffects() { }

    public static float elapsed(int remainingTicks) {
        return clamp(1.0F - Math.max(0, Math.min(200, remainingTicks)) / 200.0F);
    }

    /** Red reaches 25% opacity during the first two seconds and then remains there. */
    public static float redOpacity(int remainingTicks) {
        return 0.25F * clamp(elapsed(remainingTicks) / 0.20F);
    }

    /** Black starts after two seconds and rises non-linearly to 50% opacity. */
    public static float blackOpacity(int remainingTicks) {
        float phase = clamp((elapsed(remainingTicks) - 0.20F) / 0.80F);
        return 0.50F * phase * phase;
    }

    /** Last-five-second phase used by the safe neutral warning wash. */
    public static float finalPhase(int remainingTicks) {
        float phase = clamp((elapsed(remainingTicks) - 0.50F) / 0.50F);
        return phase * phase;
    }

    /** Camera movement begins at 30% elapsed and accelerates toward the deadline. */
    public static float shake(int remainingTicks) {
        float phase = clamp((elapsed(remainingTicks) - 0.30F) / 0.70F);
        return 1.55F * phase * phase;
    }

    /** Oscillation speeds up with danger instead of only increasing displacement. */
    public static float shakeFrequency(int remainingTicks) {
        float phase = clamp((elapsed(remainingTicks) - 0.30F) / 0.70F);
        return 10.0F + 24.0F * phase * phase;
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
