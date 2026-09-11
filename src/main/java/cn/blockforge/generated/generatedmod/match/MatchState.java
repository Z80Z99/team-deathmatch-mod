package cn.blockforge.generated.generatedmod.match;

public enum MatchState {
    /** Normal matches restore terrain in their own phase; forced stop keeps MAP_RESETTING. */
    WAITING,
    WARMUP,
    PLAYING,
    ROUND_END,
    TERRAIN_RESTORING,
    MAP_RESETTING,
    MATCH_END;

    public boolean isActive() {
        return this != WAITING;
    }

    public boolean isCombat() {
        return this == PLAYING;
    }
}
