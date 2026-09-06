package cn.blockforge.generated.generatedmod.match;

public enum TeamChangePolicy {
    NEVER,
    ONLY_BEFORE_MATCH,
    ANYTIME;

    public boolean allowsChangeDuringMatch() {
        return this == ANYTIME;
    }
}
