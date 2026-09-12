package cn.blockforge.generated.generatedmod.match;

public enum MatchState {
    /** Normal matches restore terrain in their own phase; forced stop keeps MAP_RESETTING. */
    WAITING,
    WARMUP,
    /** Fully locked transition stage; no movement, damage, interaction or item use. */
    FROZEN,
    /** Bomb mode pre-round preparation: store purchases and team-spawn barrier. */
    BUYING,
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
