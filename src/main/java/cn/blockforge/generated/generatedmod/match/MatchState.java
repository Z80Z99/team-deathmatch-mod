package cn.blockforge.generated.generatedmod.match;

public enum MatchState {
    WAITING,
    WARMUP,
    PLAYING,
    ROUND_END,
    MAP_RESETTING,
    MATCH_END;

    public boolean isActive() {
        return this != WAITING;
    }

    public boolean isCombat() {
        return this == PLAYING;
    }
}
