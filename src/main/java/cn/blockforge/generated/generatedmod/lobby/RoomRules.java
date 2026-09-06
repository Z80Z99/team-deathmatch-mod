package cn.blockforge.generated.generatedmod.lobby;

import cn.blockforge.generated.generatedmod.config.FpsTdmConfig;
import cn.blockforge.generated.generatedmod.match.AutoBalanceMode;
import cn.blockforge.generated.generatedmod.match.GameMode;
import cn.blockforge.generated.generatedmod.match.SpawnSelectionStrategy;
import cn.blockforge.generated.generatedmod.match.TeamChangePolicy;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 房间级比赛规则：原“团队死斗配置”与“房间参数”里的比赛项已全部合并到这里。
 * 每间房间在创建时取服务器默认值作为快照，房主在“房间规则设置”里随时调整，
 * 开赛时整体生效，随房间解散（含匹配房赛终自动解散）一起销毁。
 *
 * <p>规则按 {@link GameMode} 展示不同设置项；不适用于当前模式的值由
 * {@link #normalized()} 统一收敛，保证服务器端行为与界面所见一致。
 */
public record RoomRules(GameMode mode,
                        int targetKills,
                        int matchDurationSeconds,
                        int roundWinTarget,
                        int warmupDurationSeconds,
                        int respawnDelaySeconds,
                        boolean autoRespawn,
                        boolean friendlyFire,
                        int switchSideEvery,
                        int minPlayersToStart,
                        int roundEndDelaySeconds,
                        int matchEndDelaySeconds,
                        boolean keepInventoryOnDeath,
                        boolean suppressDeathMessages,
                        boolean autoReset,
                        boolean requireBothTeams,
                        TeamChangePolicy teamChangePolicy,
                        AutoBalanceMode autoBalanceMode,
                        SpawnSelectionStrategy spawnSelectionStrategy,
                        int maxTeamImbalance) {

    public RoomRules {
        mode = mode == null ? GameMode.TEAM_DEATHMATCH : mode;
        teamChangePolicy = teamChangePolicy == null
                ? TeamChangePolicy.ONLY_BEFORE_MATCH : teamChangePolicy;
        autoBalanceMode = autoBalanceMode == null
                ? AutoBalanceMode.ON_JOIN_AND_MATCH_START : autoBalanceMode;
        spawnSelectionStrategy = spawnSelectionStrategy == null
                ? SpawnSelectionStrategy.RANDOM : spawnSelectionStrategy;
    }

    /** 以服务器当前配置作为默认规则快照。必须在服务端线程调用。 */
    public static RoomRules serverDefaults() {
        FpsTdmConfig.Common config = FpsTdmConfig.COMMON;
        return new RoomRules(GameMode.TEAM_DEATHMATCH,
                config.targetKills.get(), config.matchDurationSeconds.get(),
                config.roundWinTarget.get(), config.warmupDurationSeconds.get(),
                config.respawnDelaySeconds.get(), config.autoRespawnEnabled.get(),
                config.friendlyFire.get(), 1, 2,
                config.roundEndDelaySeconds.get(), config.matchEndDelaySeconds.get(),
                config.keepInventoryOnDeath.get(), config.suppressDeathMessages.get(),
                config.autoReset.get(), config.requireBothTeams.get(),
                config.teamChangePolicy.get(), config.autoBalanceMode.get(),
                config.spawnSelectionStrategy.get(), config.maxTeamImbalance.get()).normalized();
    }

    /** 从服务器配置快照构造（客户端用同步下来的配置副本恢复默认值）。 */
    public static RoomRules fromConfigValues(cn.blockforge.generated.generatedmod.config
            .FpsTdmConfigValues values) {
        if (values == null) {
            return fallback();
        }
        return new RoomRules(GameMode.TEAM_DEATHMATCH,
                values.targetKills(), values.matchDurationSeconds(), values.roundWinTarget(),
                values.warmupDurationSeconds(), values.respawnDelaySeconds(),
                values.autoRespawnEnabled(), values.friendlyFire(), 1, 2,
                values.roundEndDelaySeconds(), values.matchEndDelaySeconds(),
                values.keepInventoryOnDeath(), values.suppressDeathMessages(),
                values.autoReset(), values.requireBothTeams(), values.teamChangePolicy(),
                values.autoBalanceMode(), values.spawnSelectionStrategy(),
                values.maxTeamImbalance()).normalized();
    }

    /** 离线或配置未就绪时的安全兜底，与服务器默认配置初值一致。 */
    public static RoomRules fallback() {
        return new RoomRules(GameMode.TEAM_DEATHMATCH, 25, 600, 1, 15, 5, true, false, 1, 2,
                5, 15, true, true, true, true, TeamChangePolicy.ONLY_BEFORE_MATCH,
                AutoBalanceMode.ON_JOIN_AND_MATCH_START, SpawnSelectionStrategy.RANDOM, 1);
    }

    /** 切换模式时的推荐模板（共享项沿用当前值，模式专属项按典型值重置）。 */
    public static RoomRules templateFor(GameMode target, RoomRules base) {
        RoomRules safe = (base == null ? fallback() : base).normalized();
        GameMode mode = target == null ? GameMode.TEAM_DEATHMATCH : target;
        return new RoomRules(mode,
                mode == GameMode.TEAM_DEATHMATCH ? Math.max(1, safe.targetKills == 0
                        ? 25 : safe.targetKills) : 0,
                switch (mode) {
                    case TEAM_DEATHMATCH -> safe.matchDurationSeconds <= 0 ? 600
                            : safe.matchDurationSeconds;
                    case SEARCH_DESTROY -> 110;
                    case LAST_STANDING -> 600;
                },
                mode == GameMode.SEARCH_DESTROY ? 3 : 1,
                safe.warmupDurationSeconds,
                mode == GameMode.TEAM_DEATHMATCH ? Math.max(1, safe.respawnDelaySeconds) : 0,
                mode == GameMode.TEAM_DEATHMATCH, safe.friendlyFire,
                mode == GameMode.SEARCH_DESTROY ? 1 : 0,
                safe.minPlayersToStart,
                safe.roundEndDelaySeconds, safe.matchEndDelaySeconds,
                safe.keepInventoryOnDeath, safe.suppressDeathMessages, safe.autoReset,
                true, safe.teamChangePolicy, safe.autoBalanceMode,
                safe.spawnSelectionStrategy, safe.maxTeamImbalance);
    }

    /** 按模式把越界或不适用值收敛，保证任何来源的规则都能安全应用。 */
    public RoomRules normalized() {
        return new RoomRules(mode,
                mode.respawnRules() ? clamp(targetKills, 0, 1000) : 0,
                clamp(matchDurationSeconds, 0, 7200),
                mode == GameMode.SEARCH_DESTROY ? clamp(roundWinTarget, 1, 10) : 1,
                clamp(warmupDurationSeconds, 0, 300),
                mode.respawnRules() ? clamp(respawnDelaySeconds, 0, 60) : 0,
                mode.respawnRules() && autoRespawn,
                friendlyFire,
                mode.roundSwapping() ? clamp(switchSideEvery, 1, 10) : 0,
                clamp(minPlayersToStart, 1, 32),
                clamp(roundEndDelaySeconds, 1, 600),
                clamp(matchEndDelaySeconds, 1, 600),
                keepInventoryOnDeath, suppressDeathMessages, autoReset, requireBothTeams,
                teamChangePolicy, autoBalanceMode, spawnSelectionStrategy,
                clamp(maxTeamImbalance, 0, 8));
    }

    /** 返回错误文本；null 表示合法。 */
    public String validationError() {
        if (matchDurationSeconds < 0 || matchDurationSeconds > 7200) {
            return "回合时长必须在 0 到 7200 秒之间（0 表示不限）。";
        }
        if (minPlayersToStart < 1 || minPlayersToStart > 32) {
            return "最少开赛人数必须在 1 到 32 之间。";
        }
        if (mode.respawnRules()) {
            if (targetKills < 0 || targetKills > 1000) {
                return "击杀目标必须在 0 到 1000 之间（0 表示不限）。";
            }
            if (targetKills == 0 && matchDurationSeconds == 0) {
                return "击杀目标与回合时长不能同时为 0，否则比赛无法结束。";
            }
        } else if (matchDurationSeconds == 0) {
            return mode.displayName() + "以回合时长兜底判定，回合时长不能为 0。";
        }
        return null;
    }

    public String describe() {
        StringBuilder text = new StringBuilder(mode.displayName());
        switch (mode) {
            case TEAM_DEATHMATCH -> text.append(" · 击杀 ")
                    .append(targetKills <= 0 ? "不限" : Integer.toString(targetKills))
                    .append(" · 回合 ").append(formatSeconds(matchDurationSeconds))
                    .append(" · 复活 ").append(respawnDelaySeconds).append("s")
                    .append(autoRespawn ? "" : "（关闭）");
            case SEARCH_DESTROY -> text.append(" · 回合 ").append(formatSeconds(matchDurationSeconds))
                    .append(" · 先胜 ").append(roundWinTarget).append(" 回合")
                    .append(" · 每 ").append(switchSideEvery).append(" 回合换边");
            case LAST_STANDING -> text.append(" · 总时长 ")
                    .append(formatSeconds(matchDurationSeconds)).append(" · 阵亡不复活");
        }
        return text.append(" · 热身 ").append(warmupDurationSeconds).append("s")
                .append(" · 最少 ").append(minPlayersToStart).append(" 人")
                .append(" · 友伤 ").append(friendlyFire ? "开" : "关").toString();
    }

    private static String formatSeconds(int seconds) {
        if (seconds <= 0) {
            return "不限";
        }
        return String.format(java.util.Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }

    public RoomRules withMinPlayersToStart(int players) {
        return new RoomRules(mode, targetKills, matchDurationSeconds, roundWinTarget,
                warmupDurationSeconds, respawnDelaySeconds, autoRespawn, friendlyFire,
                switchSideEvery, players, roundEndDelaySeconds, matchEndDelaySeconds,
                keepInventoryOnDeath, suppressDeathMessages, autoReset, requireBothTeams,
                teamChangePolicy, autoBalanceMode, spawnSelectionStrategy,
                maxTeamImbalance).normalized();
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(mode.ordinal());
        buffer.writeVarInt(targetKills);
        buffer.writeVarInt(matchDurationSeconds);
        buffer.writeVarInt(roundWinTarget);
        buffer.writeVarInt(warmupDurationSeconds);
        buffer.writeVarInt(respawnDelaySeconds);
        buffer.writeBoolean(autoRespawn);
        buffer.writeBoolean(friendlyFire);
        buffer.writeVarInt(switchSideEvery);
        buffer.writeVarInt(minPlayersToStart);
        buffer.writeVarInt(roundEndDelaySeconds);
        buffer.writeVarInt(matchEndDelaySeconds);
        buffer.writeBoolean(keepInventoryOnDeath);
        buffer.writeBoolean(suppressDeathMessages);
        buffer.writeBoolean(autoReset);
        buffer.writeBoolean(requireBothTeams);
        buffer.writeVarInt(teamChangePolicy.ordinal());
        buffer.writeVarInt(autoBalanceMode.ordinal());
        buffer.writeVarInt(spawnSelectionStrategy.ordinal());
        buffer.writeVarInt(maxTeamImbalance);
    }

    public static RoomRules read(FriendlyByteBuf buffer) {
        return new RoomRules(
                GameMode.byOrdinal(buffer.readVarInt()),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                enumByOrdinal(buffer.readVarInt(), TeamChangePolicy.values(),
                        TeamChangePolicy.ONLY_BEFORE_MATCH),
                enumByOrdinal(buffer.readVarInt(), AutoBalanceMode.values(),
                        AutoBalanceMode.ON_JOIN_AND_MATCH_START),
                enumByOrdinal(buffer.readVarInt(), SpawnSelectionStrategy.values(),
                        SpawnSelectionStrategy.RANDOM),
                buffer.readVarInt()).normalized();
    }

    private static <E extends Enum<E>> E enumByOrdinal(int ordinal, E[] values, E fallbackValue) {
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallbackValue;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
