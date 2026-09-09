package cn.blockforge.generated.generatedmod.match;

import cn.blockforge.generated.generatedmod.network.packet.MatchSyncPacket;
import cn.blockforge.generated.generatedmod.team.TeamManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MultiTeamMatchTest {
    @org.junit.jupiter.api.BeforeAll static void bootstrap() {
        cn.blockforge.generated.generatedmod.MinecraftTestBootstrap.initialize();
    }
    @Test void fourthTeamCanWinAndTiedLeadersDraw() {
        assertEquals(Team.TEAM_D, MatchManager.resolveWinner(Team.playing(4), team -> 0,
                team -> team == Team.TEAM_D ? 25 : 12));
        assertNull(MatchManager.resolveWinner(Team.playing(4), team -> 0,
                team -> team == Team.TEAM_C || team == Team.TEAM_D ? 25 : 12));
        assertEquals(Team.TEAM_C, MatchManager.resolveWinner(Team.playing(4),
                team -> team == Team.TEAM_C ? 2 : 1, team -> team == Team.TEAM_D ? 99 : 1));
    }

    @Test void wipingOneTeamDoesNotEndFourTeamElimination() throws Exception {
        MatchManager match = mock(MatchManager.class, CALLS_REAL_METHODS);
        MinecraftServer server = mock(MinecraftServer.class);
        PlayerList list = mock(PlayerList.class);
        when(server.getPlayerList()).thenReturn(list);
        TeamManager teams = mock(TeamManager.class);
        List<ServerPlayer> players = new ArrayList<>();
        for (Team team : Team.playing(4)) {
            var player = mock(ServerPlayer.class);
            when(player.getUUID()).thenReturn(UUID.randomUUID()); when(player.isAlive()).thenReturn(true);
            when(teams.getTeam(player)).thenReturn(team); players.add(player);
        }
        when(list.getPlayers()).thenReturn(players);
        set(match, "server", server); set(match, "teams", teams); set(match, "downedPlayers", new HashMap<>());
        set(match, "state", MatchState.WAITING);
        match.configureRoomTeams(4);
        var method = MatchManager.class.getDeclaredMethod("finishEliminationIfDecided", Team.class, ServerPlayer.class);
        method.setAccessible(true);
        assertEquals(false, method.invoke(match, Team.TEAM_A, players.get(0)));
        when(players.get(1).isAlive()).thenReturn(false);
        assertEquals(false, method.invoke(match, Team.TEAM_A, players.get(0)));
        when(teams.isPending(players.get(2))).thenReturn(true);
        assertEquals(true, method.invoke(match, Team.TEAM_A, players.get(0)));
    }

    private static void set(MatchManager match, String name, Object value) throws Exception {
        var field = MatchManager.class.getDeclaredField(name); field.setAccessible(true); field.set(match, value);
    }

    @Test void thirdAndFourthTeamScoresSurviveRoundResetInMatchTotals() {
        MatchScoreTracker scores = new MatchScoreTracker();
        ServerPlayer killer = mock(ServerPlayer.class), victim = mock(ServerPlayer.class);
        when(killer.getUUID()).thenReturn(UUID.randomUUID()); when(victim.getUUID()).thenReturn(UUID.randomUUID());
        scores.addKill(Team.TEAM_C, killer, victim);
        scores.addKill(Team.TEAM_D, killer, victim);
        scores.addDamage(Team.TEAM_D, killer, victim, 15);
        assertEquals(1, scores.getTeamScore(Team.TEAM_C));
        assertEquals(1, scores.getTeamScore(Team.TEAM_D));
        assertEquals(0, scores.getTeamScore(Team.TEAM_A));
        scores.resetRound();
        assertEquals(0, scores.getTeamScore(Team.TEAM_C));
        assertEquals(1, scores.getTeamMatchKills(Team.TEAM_C));
        assertEquals(15, scores.getTeamDamage(Team.TEAM_D));
        scores.resetMatch();
        assertEquals(0, scores.getTeamMatchKills(Team.TEAM_D));
    }

    @Test void missingFourthSpawnPreventsStartingBeforeRosterChanges() throws Exception {
        MatchManager match = mock(MatchManager.class, CALLS_REAL_METHODS);
        doReturn(SpawnSelectionStrategy.SEQUENTIAL).when(match).rulesSpawnStrategy();
        var maps = mock(cn.blockforge.generated.generatedmod.map.MapManager.class);
        var spawns = mock(cn.blockforge.generated.generatedmod.spawn.SpawnManager.class);
        var teams = mock(TeamManager.class);
        var definition = mock(cn.blockforge.generated.generatedmod.map.MapDefinition.class);
        when(definition.isComplete()).thenReturn(true);
        when(maps.currentMap()).thenReturn(Optional.of(definition));
        when(maps.isReady()).thenReturn(true);
        for (Team team : Team.playing(3)) when(spawns.findFixedSpawn(team))
                .thenReturn(Optional.of(mock(cn.blockforge.generated.generatedmod.spawn.SpawnPoint.class)));
        set(match, "maps", maps); set(match, "spawns", spawns); set(match, "teams", teams);
        set(match, "state", MatchState.WAITING); match.configureRoomTeams(4);
        assertEquals(MatchManager.StartResult.NO_TEAM_SPAWNS, match.startMatch(List.of(UUID.randomUUID())));
        verifyNoInteractions(teams);
    }

    @Test void roomRosterKeepsSelectedTeamsEvenWhenUneven() {
        MinecraftServer server = mock(MinecraftServer.class);
        PlayerList list = mock(PlayerList.class);
        ServerScoreboard scoreboard = mock(ServerScoreboard.class);
        when(server.getPlayerList()).thenReturn(list); when(server.getScoreboard()).thenReturn(scoreboard);
        when(scoreboard.getPlayerTeam(anyString())).thenReturn(mock(PlayerTeam.class));
        MatchManager match = mock(MatchManager.class);
        when(match.activeTeams()).thenReturn(Team.playing(4));
        when(match.hasRoomTeamSelection()).thenReturn(true);
        when(match.rulesAutoBalanceMode()).thenReturn(AutoBalanceMode.OFF);
        List<ServerPlayer> players = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ServerPlayer player = mock(ServerPlayer.class); UUID id = UUID.randomUUID();
            when(player.getUUID()).thenReturn(id); when(player.getScoreboardName()).thenReturn("p" + i);
            when(list.getPlayer(id)).thenReturn(player); players.add(player);
        }
        when(list.getPlayers()).thenReturn(players);
        TeamManager teams = spy(new TeamManager(server, match));
        doNothing().when(teams).rememberGameMode(any());
        for (int i = 0; i < players.size(); i++) teams.setPreference(players.get(i), Team.playing(4).get(Math.min(i, 3)));
        teams.prepareRoster(players.stream().map(ServerPlayer::getUUID).toList());
        for (int i = 0; i < players.size(); i++) assertEquals(Team.playing(4).get(Math.min(i, 3)), teams.getTeam(players.get(i)));
        assertEquals(6, teams.totalParticipants());
        when(match.rulesAutoBalanceMode()).thenReturn(AutoBalanceMode.ON_MATCH_START);
        teams.prepareForMatch();
        var sizes = Team.playing(4).stream().mapToInt(teams::teamSize).summaryStatistics();
        assertTrue(sizes.getMax() - sizes.getMin() <= 1);
        assertEquals(6, sizes.getSum());
    }

    @Test void randomSpawningChecksSafetyInsteadOfConfiguredTeamPoints() throws Exception {
        MatchManager match = mock(MatchManager.class, CALLS_REAL_METHODS);
        var maps = mock(cn.blockforge.generated.generatedmod.map.MapManager.class);
        var spawns = mock(cn.blockforge.generated.generatedmod.spawn.SpawnManager.class);
        var teams = mock(TeamManager.class);
        var definition = mock(cn.blockforge.generated.generatedmod.map.MapDefinition.class);
        when(definition.isComplete()).thenReturn(true);
        when(maps.currentMap()).thenReturn(Optional.of(definition));
        when(maps.isReady()).thenReturn(true);
        set(match, "maps", maps); set(match, "spawns", spawns); set(match, "teams", teams);
        set(match, "state", MatchState.WAITING);
        doReturn(SpawnSelectionStrategy.RANDOM).when(match).rulesSpawnStrategy();
        when(spawns.findRandomSpawn()).thenReturn(Optional.empty());
        assertEquals(MatchManager.StartResult.NO_SAFE_RANDOM_SPAWN, match.startMatch());
        when(spawns.findRandomSpawn()).thenReturn(Optional.of(mock(cn.blockforge.generated.generatedmod.spawn.SpawnPoint.class)));
        doReturn(0).when(match).rulesTargetKills();
        doReturn(0).when(match).rulesMatchDurationSeconds();
        assertEquals(MatchManager.StartResult.NO_END_CONDITION, match.startMatch());
        verify(spawns, never()).getSpawns(any());
        verifyNoInteractions(teams);
    }

    @Test void matchPacketSynchronizesAllFourScoresAndWinner() {
        var stats = Team.playing(4).stream().map(team -> new TeamMatchStats(team, 9, 2, 3, 12, 100)).toList();
        MatchSyncPacket packet = new MatchSyncPacket(MatchState.ROUND_END, 9, 9, 2, 2, 3, 25, 100, 0,
                Team.TEAM_D, 3, 3, 0, false, Team.TEAM_D, 1, "killer", "victim", 0, 3,
                1, 2, 3, 4, 100, 100, 12, 12, 200).withTeamStats(stats).withTimers(160, 100)
                .withRespawn(true, "击杀者  PlayerOne");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.encode(buffer);
            var decoded = new MatchSyncPacket(buffer);
            assertTrue(decoded.awaitingRespawn());
            assertEquals("击杀者  PlayerOne", decoded.deathLabel());
            assertEquals(0, buffer.readableBytes());
            assertEquals(160, decoded.boundaryTicks()); assertEquals(100, decoded.respawnTotalTicks());
            assertEquals(stats, decoded.teamStats()); assertEquals(Team.TEAM_D, decoded.winner());
            cn.blockforge.generated.generatedmod.client.ClientMatchData.apply(decoded);
            assertEquals(Team.TEAM_D, cn.blockforge.generated.generatedmod.client.ClientMatchData.myTeam);
            assertEquals(9, cn.blockforge.generated.generatedmod.client.ClientMatchData.stats(Team.TEAM_C).score());
            assertTrue(cn.blockforge.generated.generatedmod.client.ClientMatchData.teamSizesText().contains("D队"));
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); cn.blockforge.generated.generatedmod.client.ClientMatchData.clear(); }
    }
}
