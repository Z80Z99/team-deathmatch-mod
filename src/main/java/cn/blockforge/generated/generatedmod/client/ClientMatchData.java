package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.match.GameMode;
import cn.blockforge.generated.generatedmod.match.MatchState;
import cn.blockforge.generated.generatedmod.match.Team;
import cn.blockforge.generated.generatedmod.network.packet.MatchSyncPacket;

import java.util.Locale;

/** 服务器比赛同步的客户端只读镜像，并缓存 HUD 展示文本。 */
public final class ClientMatchData {
    public static java.util.List<cn.blockforge.generated.generatedmod.match.TeamMatchStats> teamStats = java.util.List.of();
    public static cn.blockforge.generated.generatedmod.match.TeamMatchStats stats(Team team) {
        return teamStats.stream().filter(value -> value.team() == team).findFirst().orElseGet(() ->
                new cn.blockforge.generated.generatedmod.match.TeamMatchStats(team,
                        team == Team.TEAM_A ? teamAScore : team == Team.TEAM_B ? teamBScore : 0,
                        team == Team.TEAM_A ? teamAWins : team == Team.TEAM_B ? teamBWins : 0,
                        team == Team.TEAM_A ? teamASize : team == Team.TEAM_B ? teamBSize : 0,
                        team == Team.TEAM_A ? teamAMatchKills : team == Team.TEAM_B ? teamBMatchKills : 0,
                        team == Team.TEAM_A ? teamADamageDealt : team == Team.TEAM_B ? teamBDamageDealt : 0));
    }
    public static MatchState state = MatchState.WAITING;
    public static int teamAScore;
    public static int teamBScore;
    public static int teamAWins;
    public static int teamBWins;
    public static int roundNumber;
    public static int targetKills;
    public static int phaseRemainingTicks;
    public static int respawnRemainingTicks;
    public static int boundaryTicks;
    public static boolean boundaryOutside;
    public static boolean boundaryBoxPresent;
    public static int boundaryMinX, boundaryMinY, boundaryMinZ, boundaryMaxX, boundaryMaxY, boundaryMaxZ;
    public static int respawnTotalTicks;
    public static boolean awaitingRespawn;
    public static String deathLabel = "";
    public static java.util.Set<java.util.UUID> downedPlayerIds = java.util.Set.of();
    public static boolean economyEnabled;
    public static int globalBalance;
    public static int matchBalance;
    public static Team myTeam = Team.SPECTATOR;
    public static int teamASize;
    public static int teamBSize;
    public static int spectatorSize;
    public static boolean pending;
    public static Team winner;
    /** 当前比赛模式（服务器规则快照）与整场胜利回合数。 */
    public static GameMode mode = GameMode.TEAM_DEATHMATCH;
    public static cn.blockforge.generated.generatedmod.match.PlayerPerspective perspective =
            cn.blockforge.generated.generatedmod.match.PlayerPerspective.FIRST_PERSON;
    public static boolean allowViewSwitch = true;
    public static int roundsToWin = 1;
    /** 整场统计镜像：个人击杀 / 阵亡 / 造伤 / 承伤、队伍伤害与整场击杀。 */
    public static int myMatchKills;
    public static int myMatchDeaths;
    public static int myDamageDealt;
    public static int myDamageTaken;
    public static int teamADamageDealt;
    public static int teamBDamageDealt;
    public static int teamAMatchKills;
    public static int teamBMatchKills;
    /** 整场已打时长：以快照为基准，由本地时钟逐 tick 平滑推进。 */
    private static int elapsedBaseTicks;
    private static long elapsedSnapshotTick;
    private static long clientTick;

    /** 击杀公告：序列号、双方名字与剩余展示时间。 */
    private static int killFeedSequence;
    private static String killFeedKiller = "";
    private static String killFeedVictim = "";
    private static int killFeedTicks;
    private static boolean killFeedSoundPending;
    /** 是否收到过任何比赛同步包；客户端据此区分“中途观赛登录”与“开赛瞬间”。 */
    private static boolean syncApplied;

    private static int cachedPhaseSecond = -1;
    private static int cachedRespawnSecond = -1;
    private static String phaseTimerText = "00:00";
    private static String respawnTimerText = "00:00";
    private static String phaseText = "等待中";
    private static String modeText = "团队竞技";
    private static String targetText = "按时间判定";
    private static String roundText = "回合 0    胜场 0:0";
    private static String teamSizesText = "A队 0人  ·  B队 0人";
    private static int pulseTicks;

    private ClientMatchData() {
    }

    public static void apply(MatchSyncPacket packet) {
        awaitingRespawn = packet.awaitingRespawn();
        deathLabel = packet.deathLabel();
        downedPlayerIds = packet.downedPlayerIds();
        economyEnabled = packet.economyEnabled();
        globalBalance = packet.globalBalance();
        matchBalance = packet.matchBalance();
        syncApplied = true;
        int previousPhaseSecond = Math.max(0, phaseRemainingTicks) / 20;
        int previousRespawnSecond = Math.max(0, respawnRemainingTicks) / 20;
        boolean importantChange = state != packet.state()
                || !teamStats.equals(packet.teamStats())
                || teamAScore != packet.teamAScore()
                || teamBScore != packet.teamBScore()
                || teamAWins != packet.teamAWins()
                || teamBWins != packet.teamBWins()
                || roundNumber != packet.roundNumber()
                || myTeam != packet.myTeam();
        boolean modeChanged = mode != GameMode.byOrdinal(packet.gameModeOrdinal());
        boolean cachedTextChange = importantChange
                || modeChanged
                || targetKills != packet.targetKills()
                || teamASize != packet.teamASize()
                || teamBSize != packet.teamBSize()
                || previousPhaseSecond != Math.max(0, packet.phaseRemainingTicks()) / 20
                || previousRespawnSecond != Math.max(0, packet.respawnRemainingTicks()) / 20;
        state = packet.state();
        teamStats = packet.teamStats();
        teamAScore = packet.teamAScore();
        teamBScore = packet.teamBScore();
        teamAWins = packet.teamAWins();
        teamBWins = packet.teamBWins();
        roundNumber = packet.roundNumber();
        targetKills = packet.targetKills();
        phaseRemainingTicks = Math.max(0, packet.phaseRemainingTicks());
        respawnRemainingTicks = Math.max(0, packet.respawnRemainingTicks());
        boundaryTicks = Math.max(0, packet.boundaryTicks());
        boundaryOutside = packet.boundaryOutside();
        boundaryBoxPresent = packet.boundaryBoxPresent();
        boundaryMinX = packet.boundaryMinX(); boundaryMinY = packet.boundaryMinY(); boundaryMinZ = packet.boundaryMinZ();
        boundaryMaxX = packet.boundaryMaxX(); boundaryMaxY = packet.boundaryMaxY(); boundaryMaxZ = packet.boundaryMaxZ();
        respawnTotalTicks = Math.max(0, packet.respawnTotalTicks());
        myTeam = packet.myTeam();
        teamASize = packet.teamASize();
        teamBSize = packet.teamBSize();
        spectatorSize = packet.spectatorSize();
        pending = packet.pending();
        winner = packet.winner();
        mode = GameMode.byOrdinal(packet.gameModeOrdinal());
        perspective = packet.perspective() == null
                ? cn.blockforge.generated.generatedmod.match.PlayerPerspective.FIRST_PERSON
                : packet.perspective();
        allowViewSwitch = packet.allowViewSwitch();
        roundsToWin = Math.max(1, packet.roundsToWin());
        myMatchKills = packet.myMatchKills();
        myMatchDeaths = packet.myMatchDeaths();
        myDamageDealt = packet.myDamageDealt();
        myDamageTaken = packet.myDamageTaken();
        teamADamageDealt = packet.teamADamageDealt();
        teamBDamageDealt = packet.teamBDamageDealt();
        teamAMatchKills = packet.teamAMatchKills();
        teamBMatchKills = packet.teamBMatchKills();
        elapsedBaseTicks = Math.max(0, packet.matchElapsedTicks());
        elapsedSnapshotTick = clientTick;
        if (packet.killFeedSequence() != 0 && packet.killFeedSequence() != killFeedSequence) {
            killFeedSequence = packet.killFeedSequence();
            killFeedKiller = packet.killFeedKiller();
            killFeedVictim = packet.killFeedVictim();
            killFeedTicks = 120;
            killFeedSoundPending = true;
        }
        if (importantChange) {
            pulseTicks = 12;
        }
        if (cachedTextChange) {
            refreshCachedText();
        }
    }

    /** 客户端只推进显示用倒计时，不参与服务器比赛判定。 */
    public static void tick() {
        clientTick++;
        if (boundaryOutside && boundaryTicks > 1) boundaryTicks--;
        if (phaseRemainingTicks > 0) {
            phaseRemainingTicks--;
        }
        if (respawnRemainingTicks > 0) {
            respawnRemainingTicks--;
        }
        if (pulseTicks > 0) {
            pulseTicks--;
        }
        if (killFeedTicks > 0) {
            killFeedTicks--;
        }
        int phaseSecond = phaseRemainingTicks / 20;
        int respawnSecond = respawnRemainingTicks / 20;
        if (phaseSecond != cachedPhaseSecond || respawnSecond != cachedRespawnSecond) {
            refreshCachedText();
        }
    }

    public static void clear() {
        ClientHudEventData.clear();
        ClientBombPreviewData.clear();
        awaitingRespawn = false;
        deathLabel = "";
        downedPlayerIds = java.util.Set.of();
        economyEnabled = false;
        globalBalance = 0;
        matchBalance = 0;
        RespawnOverlay.clear();
        teamStats = java.util.List.of();
        state = MatchState.WAITING;
        teamAScore = 0;
        teamBScore = 0;
        teamAWins = 0;
        teamBWins = 0;
        roundNumber = 0;
        targetKills = 0;
        mode = GameMode.TEAM_DEATHMATCH;
        perspective = cn.blockforge.generated.generatedmod.match.PlayerPerspective.FIRST_PERSON;
        allowViewSwitch = true;
        roundsToWin = 1;
        myMatchKills = 0;
        myMatchDeaths = 0;
        myDamageDealt = 0;
        myDamageTaken = 0;
        teamADamageDealt = 0;
        teamBDamageDealt = 0;
        teamAMatchKills = 0;
        teamBMatchKills = 0;
        elapsedBaseTicks = 0;
        elapsedSnapshotTick = 0L;
        phaseRemainingTicks = 0;
        respawnRemainingTicks = 0;
        boundaryTicks = 0;
        boundaryOutside = false;
        boundaryBoxPresent = false;
        boundaryMinX = boundaryMinY = boundaryMinZ = boundaryMaxX = boundaryMaxY = boundaryMaxZ = 0;
        respawnTotalTicks = 0;
        myTeam = Team.SPECTATOR;
        teamASize = 0;
        teamBSize = 0;
        spectatorSize = 0;
        pending = false;
        winner = null;
        killFeedSequence = 0;
        killFeedKiller = "";
        killFeedVictim = "";
        killFeedTicks = 0;
        killFeedSoundPending = false;
        syncApplied = false;
        cachedPhaseSecond = -1;
        cachedRespawnSecond = -1;
        phaseTimerText = "00:00";
        respawnTimerText = "00:00";
        phaseText = "等待中";
        modeText = "团队竞技";
        targetText = "按时间判定";
        roundText = "回合 0    胜场 0:0";
        teamSizesText = "A队 0人  ·  B队 0人";
        pulseTicks = 0;
    }

    public static boolean inMatch() {
        return state != MatchState.WAITING;
    }

    /** Server-owned phases ask the local client to suppress movement input as well as interactions. */
    public static boolean movementFrozen() {
        return state == MatchState.TERRAIN_RESTORING
                || state == MatchState.FROZEN;
    }

    /** 整场已打时长（tick）：服务器快照 + 本地时钟推进，两包之间也不会停。 */
    public static int elapsedTicks() {
        if (elapsedBaseTicks <= 0 && !inMatch()) {
            return 0;
        }
        return elapsedBaseTicks + (int) Math.max(0L, clientTick - elapsedSnapshotTick);
    }

    public static String phaseTimerText() {
        return phaseTimerText;
    }

    public static String respawnTimerText() {
        return respawnTimerText;
    }

    public static String phaseText() {
        return phaseText;
    }

    public static String targetText() {
        return targetText;
    }

    public static String modeText() {
        return modeText;
    }

    public static String roundText() {
        return roundText;
    }

    public static String teamSizesText() {
        return teamSizesText;
    }

    public static boolean pulseActive() {
        return pulseTicks > 0;
    }

    public static boolean killFeedActive() {
        return killFeedTicks > 0 && !killFeedKiller.isEmpty() && !killFeedVictim.isEmpty();
    }

    public static int killFeedTicksLeft() {
        return killFeedTicks;
    }

    public static String killFeedText() {
        return killFeedKiller + "  击杀  " + killFeedVictim;
    }

    public static String killFeedKiller() {
        return killFeedKiller;
    }

    public static String killFeedVictim() {
        return killFeedVictim;
    }

    /** 取走一次“新击杀”标记，用于播放提示音。 */
    public static boolean consumeKillFeedSound() {
        if (!killFeedSoundPending) {
            return false;
        }
        killFeedSoundPending = false;
        return true;
    }

    public static boolean hasReceivedSync() {
        return syncApplied;
    }

    private static void refreshCachedText() {
        cachedPhaseSecond = Math.max(0, phaseRemainingTicks) / 20;
        cachedRespawnSecond = Math.max(0, respawnRemainingTicks) / 20;
        phaseTimerText = state == MatchState.WARMUP && phaseRemainingTicks == 0 ? "等待玩家" : formatTicks(phaseRemainingTicks);
        respawnTimerText = formatTicks(respawnRemainingTicks);
        phaseText = switch (state) {
            case WAITING -> "等待中";
            case WARMUP -> phaseRemainingTicks == 0 ? "热身 · 等待玩家" : "即将开始";
            case FROZEN -> "阶段冻结";
            case BUYING -> "购买装备";
            case PLAYING -> mode == GameMode.SEARCH_DESTROY ? "行动阶段" : "进行中";
            case ROUND_END -> "回合结束";
            case TERRAIN_RESTORING -> "地形恢复";
            case MAP_RESETTING -> "地图恢复";
            case MATCH_END -> "比赛结束";
        };
        modeText = mode.displayName();
        targetText = switch (mode) {
            case TEAM_DEATHMATCH -> targetKills > 0 ? "目标 " + targetKills : "按时间判定";
            case SEARCH_DESTROY -> "先胜 " + roundsToWin + " 回合";
            case LAST_STANDING -> "歼灭判定";
        };
        roundText = "回合 " + roundNumber + "    胜场 " + teamAWins + ":" + teamBWins;
        teamSizesText = "A队 " + teamASize + "人  ·  B队 " + teamBSize + "人";
        if (teamStats.size() > 2) {
            roundText = "回合 " + roundNumber + "  胜场 " + teamStats.stream()
                    .map(value -> String.valueOf(value.wins())).collect(java.util.stream.Collectors.joining(":"));
            teamSizesText = teamStats.stream().map(value -> value.team().displayName() + " " + value.size() + "人")
                    .collect(java.util.stream.Collectors.joining(" · "));
        }
    }

    private static String formatTicks(int ticks) {
        int seconds = Math.max(0, ticks) / 20;
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }
}
