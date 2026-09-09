package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.client.network.ClientPacketHandler;
import cn.blockforge.generated.generatedmod.match.MatchState;
import cn.blockforge.generated.generatedmod.match.Team;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.function.Supplier;

/**
 * 比赛同步包：既有比分 / 阶段字段之后追加整场统计字段
 * （个人击杀、阵亡、造伤、承伤、队伍伤害与整场击杀、整场已打时长），
 * 供 HUD 统计接口与自定义模块绑定。
 */
public final class MatchSyncPacket {
    private int boundaryTicks;
    private int respawnTotalTicks;
    public int boundaryTicks() { return boundaryTicks; }
    public int respawnTotalTicks() { return respawnTotalTicks; }
    public MatchSyncPacket withTimers(int boundary, int respawnTotal) {
        boundaryTicks = Math.max(0, boundary);
        respawnTotalTicks = Math.max(0, respawnTotal);
        return this;
    }
    private java.util.List<cn.blockforge.generated.generatedmod.match.TeamMatchStats> teamStats = java.util.List.of();
    public java.util.List<cn.blockforge.generated.generatedmod.match.TeamMatchStats> teamStats() { return teamStats; }
    public MatchSyncPacket withTeamStats(java.util.List<cn.blockforge.generated.generatedmod.match.TeamMatchStats> stats) {
        teamStats = java.util.List.copyOf(stats);
        return this;
    }
    private final MatchState state;
    private final int teamAScore;
    private final int teamBScore;
    private final int teamAWins;
    private final int teamBWins;
    private final int roundNumber;
    private final int targetKills;
    private final int phaseRemainingTicks;
    private final int respawnRemainingTicks;
    private final Team myTeam;
    private final int teamASize;
    private final int teamBSize;
    private final int spectatorSize;
    private final boolean pending;
    private final Team winner;
    private final int killFeedSequence;
    private final String killFeedKiller;
    private final String killFeedVictim;
    /** 本局生效的玩法模式（GameMode 序号）与整场胜利回合数，供 HUD 展示。 */
    private final int gameModeOrdinal;
    private final int roundsToWin;
    /** 整场统计：个人击杀 / 阵亡 / 造伤 / 承伤、队伍伤害与队伍整场击杀、整场时长。 */
    private final int myMatchKills;
    private final int myMatchDeaths;
    private final int myDamageDealt;
    private final int myDamageTaken;
    private final int teamADamageDealt;
    private final int teamBDamageDealt;
    private final int teamAMatchKills;
    private final int teamBMatchKills;
    private final int matchElapsedTicks;

    public MatchSyncPacket(MatchState state, int teamAScore, int teamBScore, int teamAWins, int teamBWins,
                            int roundNumber, int targetKills, int phaseRemainingTicks, int respawnRemainingTicks,
                            Team myTeam, int teamASize, int teamBSize, int spectatorSize, boolean pending,
                            Team winner, int killFeedSequence, String killFeedKiller, String killFeedVictim,
                            int gameModeOrdinal, int roundsToWin,
                            int myMatchKills, int myMatchDeaths, int myDamageDealt, int myDamageTaken,
                            int teamADamageDealt, int teamBDamageDealt, int teamAMatchKills,
                            int teamBMatchKills, int matchElapsedTicks) {
        this.state = state;
        this.teamAScore = teamAScore;
        this.teamBScore = teamBScore;
        this.teamAWins = teamAWins;
        this.teamBWins = teamBWins;
        this.roundNumber = roundNumber;
        this.targetKills = targetKills;
        this.phaseRemainingTicks = phaseRemainingTicks;
        this.respawnRemainingTicks = respawnRemainingTicks;
        this.myTeam = myTeam;
        this.teamASize = teamASize;
        this.teamBSize = teamBSize;
        this.spectatorSize = spectatorSize;
        this.pending = pending;
        this.winner = winner;
        this.killFeedSequence = killFeedSequence;
        this.killFeedKiller = killFeedKiller == null ? "" : killFeedKiller;
        this.killFeedVictim = killFeedVictim == null ? "" : killFeedVictim;
        this.gameModeOrdinal = gameModeOrdinal;
        this.roundsToWin = roundsToWin;
        this.myMatchKills = myMatchKills;
        this.myMatchDeaths = myMatchDeaths;
        this.myDamageDealt = myDamageDealt;
        this.myDamageTaken = myDamageTaken;
        this.teamADamageDealt = teamADamageDealt;
        this.teamBDamageDealt = teamBDamageDealt;
        this.teamAMatchKills = teamAMatchKills;
        this.teamBMatchKills = teamBMatchKills;
        this.matchElapsedTicks = matchElapsedTicks;
    }

    public MatchSyncPacket(FriendlyByteBuf buffer) {
        state = buffer.readEnum(MatchState.class);
        teamAScore = buffer.readVarInt();
        teamBScore = buffer.readVarInt();
        teamAWins = buffer.readVarInt();
        teamBWins = buffer.readVarInt();
        roundNumber = buffer.readVarInt();
        targetKills = buffer.readVarInt();
        phaseRemainingTicks = buffer.readVarInt();
        respawnRemainingTicks = buffer.readVarInt();
        myTeam = buffer.readEnum(Team.class);
        teamASize = buffer.readVarInt();
        teamBSize = buffer.readVarInt();
        spectatorSize = buffer.readVarInt();
        pending = buffer.readBoolean();
        int winnerOrdinal = buffer.readByte();
        winner = winnerOrdinal < 0 ? null : Team.values()[winnerOrdinal];
        killFeedSequence = buffer.readVarInt();
        killFeedKiller = buffer.readUtf(64);
        killFeedVictim = buffer.readUtf(64);
        gameModeOrdinal = buffer.readVarInt();
        roundsToWin = buffer.readVarInt();
        myMatchKills = buffer.readVarInt();
        myMatchDeaths = buffer.readVarInt();
        myDamageDealt = buffer.readVarInt();
        myDamageTaken = buffer.readVarInt();
        teamADamageDealt = buffer.readVarInt();
        teamBDamageDealt = buffer.readVarInt();
        teamAMatchKills = buffer.readVarInt();
        teamBMatchKills = buffer.readVarInt();
        matchElapsedTicks = buffer.readVarInt();
        boundaryTicks = buffer.readVarInt();
        respawnTotalTicks = buffer.readVarInt();
        int count = buffer.readVarInt();
        if (count < 0 || count > 4) throw new IllegalArgumentException("Invalid team count");
        var stats = new java.util.ArrayList<cn.blockforge.generated.generatedmod.match.TeamMatchStats>();
        for (int i = 0; i < count; i++) stats.add(new cn.blockforge.generated.generatedmod.match.TeamMatchStats(
                buffer.readEnum(Team.class), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt()));
        teamStats = java.util.List.copyOf(stats);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(state);
        buffer.writeVarInt(teamAScore);
        buffer.writeVarInt(teamBScore);
        buffer.writeVarInt(teamAWins);
        buffer.writeVarInt(teamBWins);
        buffer.writeVarInt(roundNumber);
        buffer.writeVarInt(targetKills);
        buffer.writeVarInt(phaseRemainingTicks);
        buffer.writeVarInt(respawnRemainingTicks);
        buffer.writeEnum(myTeam);
        buffer.writeVarInt(teamASize);
        buffer.writeVarInt(teamBSize);
        buffer.writeVarInt(spectatorSize);
        buffer.writeBoolean(pending);
        buffer.writeByte(winner == null ? -1 : winner.ordinal());
        buffer.writeVarInt(killFeedSequence);
        buffer.writeUtf(killFeedKiller, 64);
        buffer.writeUtf(killFeedVictim, 64);
        buffer.writeVarInt(gameModeOrdinal);
        buffer.writeVarInt(roundsToWin);
        buffer.writeVarInt(myMatchKills);
        buffer.writeVarInt(myMatchDeaths);
        buffer.writeVarInt(myDamageDealt);
        buffer.writeVarInt(myDamageTaken);
        buffer.writeVarInt(teamADamageDealt);
        buffer.writeVarInt(teamBDamageDealt);
        buffer.writeVarInt(teamAMatchKills);
        buffer.writeVarInt(teamBMatchKills);
        buffer.writeVarInt(matchElapsedTicks);
        buffer.writeVarInt(boundaryTicks);
        buffer.writeVarInt(respawnTotalTicks);
        buffer.writeVarInt(teamStats.size());
        for (var team : teamStats) {
            buffer.writeEnum(team.team()); buffer.writeVarInt(team.score()); buffer.writeVarInt(team.wins());
            buffer.writeVarInt(team.size()); buffer.writeVarInt(team.kills()); buffer.writeVarInt(team.damage());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handle(this)));
        context.setPacketHandled(true);
    }

    public MatchState state() {
        return state;
    }

    public int teamAScore() {
        return teamAScore;
    }

    public int teamBScore() {
        return teamBScore;
    }

    public int teamAWins() {
        return teamAWins;
    }

    public int teamBWins() {
        return teamBWins;
    }

    public int roundNumber() {
        return roundNumber;
    }

    public int targetKills() {
        return targetKills;
    }

    public int phaseRemainingTicks() {
        return phaseRemainingTicks;
    }

    public int respawnRemainingTicks() {
        return respawnRemainingTicks;
    }

    public Team myTeam() {
        return myTeam;
    }

    public int teamASize() {
        return teamASize;
    }

    public int teamBSize() {
        return teamBSize;
    }

    public int spectatorSize() {
        return spectatorSize;
    }

    public boolean pending() {
        return pending;
    }

    public Team winner() {
        return winner;
    }

    public int killFeedSequence() {
        return killFeedSequence;
    }

    public String killFeedKiller() {
        return killFeedKiller;
    }

    public String killFeedVictim() {
        return killFeedVictim;
    }

    public int gameModeOrdinal() {
        return gameModeOrdinal;
    }

    public int roundsToWin() {
        return roundsToWin;
    }

    public int myMatchKills() {
        return myMatchKills;
    }

    public int myMatchDeaths() {
        return myMatchDeaths;
    }

    public int myDamageDealt() {
        return myDamageDealt;
    }

    public int myDamageTaken() {
        return myDamageTaken;
    }

    public int teamADamageDealt() {
        return teamADamageDealt;
    }

    public int teamBDamageDealt() {
        return teamBDamageDealt;
    }

    public int teamAMatchKills() {
        return teamAMatchKills;
    }

    public int teamBMatchKills() {
        return teamBMatchKills;
    }

    public int matchElapsedTicks() {
        return matchElapsedTicks;
    }
}
