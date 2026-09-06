package cn.blockforge.generated.generatedmod.match;

public enum AutoBalanceMode {
    OFF,
    ON_JOIN,
    ON_MATCH_START,
    ON_JOIN_AND_MATCH_START;

    public boolean balancesOnJoin() {
        return this == ON_JOIN || this == ON_JOIN_AND_MATCH_START;
    }

    public boolean balancesOnMatchStart() {
        return this == ON_MATCH_START || this == ON_JOIN_AND_MATCH_START;
    }
}
