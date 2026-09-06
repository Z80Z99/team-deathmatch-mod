package cn.blockforge.generated.generatedmod.config;

import cn.blockforge.generated.generatedmod.match.AutoBalanceMode;
import cn.blockforge.generated.generatedmod.match.SpawnSelectionStrategy;
import cn.blockforge.generated.generatedmod.match.TeamChangePolicy;
import net.minecraft.network.FriendlyByteBuf;

/** 配置界面和网络协议共用的不可变配置快照。 */
public record FpsTdmConfigValues(
        int matchDurationSeconds,
        int targetKills,
        int roundWinTarget,
        int warmupDurationSeconds,
        int roundEndDelaySeconds,
        int matchEndDelaySeconds,
        int respawnDelaySeconds,
        int minPlayersPerTeam,
        int maxTeamImbalance,
        TeamChangePolicy teamChangePolicy,
        AutoBalanceMode autoBalanceMode,
        SpawnSelectionStrategy spawnSelectionStrategy,
        boolean requireBothTeams,
        boolean allowJoinDuringMatch,
        boolean joinDuringMatchAsSpectator,
        boolean friendlyFire,
        boolean keepInventoryOnDeath,
        boolean suppressDeathMessages,
        boolean autoRespawnEnabled,
        boolean autoReset,
        boolean protectArena,
        boolean enforceRegion,
        int maxBlocksPerTick,
        int maxSnapshotBlocks) {

    public static FpsTdmConfigValues current() {
        FpsTdmConfig.Common config = FpsTdmConfig.COMMON;
        return new FpsTdmConfigValues(
                config.matchDurationSeconds.get(),
                config.targetKills.get(),
                config.roundWinTarget.get(),
                config.warmupDurationSeconds.get(),
                config.roundEndDelaySeconds.get(),
                config.matchEndDelaySeconds.get(),
                config.respawnDelaySeconds.get(),
                config.minPlayersPerTeam.get(),
                config.maxTeamImbalance.get(),
                config.teamChangePolicy.get(),
                config.autoBalanceMode.get(),
                config.spawnSelectionStrategy.get(),
                config.requireBothTeams.get(),
                config.allowJoinDuringMatch.get(),
                config.joinDuringMatchAsSpectator.get(),
                config.friendlyFire.get(),
                config.keepInventoryOnDeath.get(),
                config.suppressDeathMessages.get(),
                config.autoRespawnEnabled.get(),
                config.autoReset.get(),
                config.protectArena.get(),
                config.enforceRegion.get(),
                config.maxBlocksPerTick.get(),
                config.maxSnapshotBlocks.get());
    }

    /** 客户端尚未连接服务器时使用的显示初值。 */
    public static FpsTdmConfigValues defaults() {
        return new FpsTdmConfigValues(
                600, 25, 1, 15, 5, 15, 5, 1, 1,
                TeamChangePolicy.ONLY_BEFORE_MATCH,
                AutoBalanceMode.ON_JOIN_AND_MATCH_START,
                SpawnSelectionStrategy.RANDOM,
                true, true, true, false, true, true, true, true, true, true,
                4096, 4000000);
    }

    /** 返回错误文本；返回 null 表示所有值都通过校验。 */
    public String validationError() {
        String error;
        if ((error = range("单回合时长", matchDurationSeconds, 0, 86400)) != null) return error;
        if ((error = range("击杀目标", targetKills, 0, 1000000)) != null) return error;
        if ((error = range("整场胜场目标", roundWinTarget, 1, 1000)) != null) return error;
        if ((error = range("热身时长", warmupDurationSeconds, 0, 3600)) != null) return error;
        if ((error = range("回合结束间隔", roundEndDelaySeconds, 1, 3600)) != null) return error;
        if ((error = range("比赛结束时长", matchEndDelaySeconds, 1, 3600)) != null) return error;
        if ((error = range("阵亡恢复时间", respawnDelaySeconds, 0, 3600)) != null) return error;
        if ((error = range("每队建议人数", minPlayersPerTeam, 1, 100)) != null) return error;
        if ((error = range("队伍人数差", maxTeamImbalance, 0, 64)) != null) return error;
        if (teamChangePolicy == null) return "队伍更换规则无效。";
        if (autoBalanceMode == null) return "自动平衡规则无效。";
        if (spawnSelectionStrategy == null) return "出生点选择策略无效。";
        if ((error = range("每 Tick 方块预算", maxBlocksPerTick, 1, 1000000)) != null) return error;
        if ((error = range("最大快照方块数", maxSnapshotBlocks, 1, 50000000)) != null) return error;
        if (targetKills == 0 && matchDurationSeconds == 0) {
            return "击杀目标和单回合时长不能同时为 0。";
        }
        return null;
    }

    private static String range(String name, int value, int minimum, int maximum) {
        return value < minimum || value > maximum
                ? name + "必须在 " + minimum + " 到 " + maximum + " 之间。"
                : null;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(matchDurationSeconds);
        buffer.writeVarInt(targetKills);
        buffer.writeVarInt(roundWinTarget);
        buffer.writeVarInt(warmupDurationSeconds);
        buffer.writeVarInt(roundEndDelaySeconds);
        buffer.writeVarInt(matchEndDelaySeconds);
        buffer.writeVarInt(respawnDelaySeconds);
        buffer.writeVarInt(minPlayersPerTeam);
        buffer.writeVarInt(maxTeamImbalance);
        writeEnum(buffer, teamChangePolicy);
        writeEnum(buffer, autoBalanceMode);
        writeEnum(buffer, spawnSelectionStrategy);
        buffer.writeBoolean(requireBothTeams);
        buffer.writeBoolean(allowJoinDuringMatch);
        buffer.writeBoolean(joinDuringMatchAsSpectator);
        buffer.writeBoolean(friendlyFire);
        buffer.writeBoolean(keepInventoryOnDeath);
        buffer.writeBoolean(suppressDeathMessages);
        buffer.writeBoolean(autoRespawnEnabled);
        buffer.writeBoolean(autoReset);
        buffer.writeBoolean(protectArena);
        buffer.writeBoolean(enforceRegion);
        buffer.writeVarInt(maxBlocksPerTick);
        buffer.writeVarInt(maxSnapshotBlocks);
    }

    public static FpsTdmConfigValues read(FriendlyByteBuf buffer) {
        return new FpsTdmConfigValues(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                readEnum(buffer, TeamChangePolicy.class),
                readEnum(buffer, AutoBalanceMode.class),
                readEnum(buffer, SpawnSelectionStrategy.class),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt());
    }

    private static void writeEnum(FriendlyByteBuf buffer, Enum<?> value) {
        buffer.writeVarInt(value == null ? -1 : value.ordinal());
    }

    private static <E extends Enum<E>> E readEnum(FriendlyByteBuf buffer, Class<E> type) {
        int ordinal = buffer.readVarInt();
        E[] values = type.getEnumConstants();
        return ordinal < 0 || ordinal >= values.length ? null : values[ordinal];
    }

    public Mutable mutable() {
        return new Mutable(this);
    }

    /** 配置界面使用的可变草稿。 */
    public static final class Mutable {
        public int matchDurationSeconds;
        public int targetKills;
        public int roundWinTarget;
        public int warmupDurationSeconds;
        public int roundEndDelaySeconds;
        public int matchEndDelaySeconds;
        public int respawnDelaySeconds;
        public int minPlayersPerTeam;
        public int maxTeamImbalance;
        public TeamChangePolicy teamChangePolicy;
        public AutoBalanceMode autoBalanceMode;
        public SpawnSelectionStrategy spawnSelectionStrategy;
        public boolean requireBothTeams;
        public boolean allowJoinDuringMatch;
        public boolean joinDuringMatchAsSpectator;
        public boolean friendlyFire;
        public boolean keepInventoryOnDeath;
        public boolean suppressDeathMessages;
        public boolean autoRespawnEnabled;
        public boolean autoReset;
        public boolean protectArena;
        public boolean enforceRegion;
        public int maxBlocksPerTick;
        public int maxSnapshotBlocks;

        public Mutable(FpsTdmConfigValues values) {
            matchDurationSeconds = values.matchDurationSeconds;
            targetKills = values.targetKills;
            roundWinTarget = values.roundWinTarget;
            warmupDurationSeconds = values.warmupDurationSeconds;
            roundEndDelaySeconds = values.roundEndDelaySeconds;
            matchEndDelaySeconds = values.matchEndDelaySeconds;
            respawnDelaySeconds = values.respawnDelaySeconds;
            minPlayersPerTeam = values.minPlayersPerTeam;
            maxTeamImbalance = values.maxTeamImbalance;
            teamChangePolicy = values.teamChangePolicy;
            autoBalanceMode = values.autoBalanceMode;
            spawnSelectionStrategy = values.spawnSelectionStrategy;
            requireBothTeams = values.requireBothTeams;
            allowJoinDuringMatch = values.allowJoinDuringMatch;
            joinDuringMatchAsSpectator = values.joinDuringMatchAsSpectator;
            friendlyFire = values.friendlyFire;
            keepInventoryOnDeath = values.keepInventoryOnDeath;
            suppressDeathMessages = values.suppressDeathMessages;
            autoRespawnEnabled = values.autoRespawnEnabled;
            autoReset = values.autoReset;
            protectArena = values.protectArena;
            enforceRegion = values.enforceRegion;
            maxBlocksPerTick = values.maxBlocksPerTick;
            maxSnapshotBlocks = values.maxSnapshotBlocks;
        }

        public FpsTdmConfigValues build() {
            return new FpsTdmConfigValues(
                    matchDurationSeconds, targetKills, roundWinTarget, warmupDurationSeconds,
                    roundEndDelaySeconds, matchEndDelaySeconds, respawnDelaySeconds,
                    minPlayersPerTeam, maxTeamImbalance, teamChangePolicy, autoBalanceMode,
                    spawnSelectionStrategy, requireBothTeams, allowJoinDuringMatch,
                    joinDuringMatchAsSpectator, friendlyFire, keepInventoryOnDeath,
                    suppressDeathMessages, autoRespawnEnabled, autoReset, protectArena,
                    enforceRegion, maxBlocksPerTick, maxSnapshotBlocks);
        }
    }
}
