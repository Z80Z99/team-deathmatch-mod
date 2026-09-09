package cn.blockforge.generated.generatedmod.match;

import cn.blockforge.generated.generatedmod.spawn.SpawnManager;
import cn.blockforge.generated.generatedmod.team.TeamManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VanillaRespawnTest {
    @Test void lethalDamageIsNotReplacedWithFakeDeath() throws Exception {
        Fixture f = new Fixture();
        assertFalse(f.match.handleFatalDamage(f.oldPlayer, mock(DamageSource.class), Float.MAX_VALUE));
        verify(f.oldPlayer, never()).setHealth(anyFloat());
    }

    @Test void cancelledDeathDoesNotRespawnOrScore() throws Exception {
        Fixture f = new Fixture();
        f.match.onPlayerKilled(f.oldPlayer, mock(DamageSource.class));
        when(f.oldPlayer.isDeadOrDying()).thenReturn(false);
        invoke(f.match, "processVanillaDeaths");
        verifyNoInteractions(f.connection, f.spawns);
        assertFalse(f.match.isDowned(f.oldPlayer));
    }

    @Test void cancellationByLaterListenerIsRespectedEvenAtZeroHealth() throws Exception {
        Fixture f = new Fixture();
        var cancelled = new java.util.concurrent.atomic.AtomicBoolean();
        f.match.onPlayerKilled(f.oldPlayer, mock(DamageSource.class), cancelled::get);
        when(f.oldPlayer.isDeadOrDying()).thenReturn(true);
        cancelled.set(true);
        invoke(f.match, "processVanillaDeaths");
        verifyNoInteractions(f.connection, f.spawns);
        assertFalse(f.match.isDowned(f.oldPlayer));
    }

    @Test void duplicateDeathUsesVanillaHandlerOnceAndNeverHealsOldEntity() throws Exception {
        Fixture f = new Fixture();
        var source = mock(DamageSource.class);
        f.match.onPlayerKilled(f.oldPlayer, source);
        f.match.onPlayerKilled(f.oldPlayer, source);
        when(f.oldPlayer.isDeadOrDying()).thenReturn(true);
        // A round can end before the queued death is processed; revival must still complete.
        set(f.match, "state", MatchState.WAITING);
        doAnswer(call -> {
            f.match.handlePlayerRespawn(f.newPlayer);
            f.connection.player = f.newPlayer;
            when(f.list.getPlayer(f.id)).thenReturn(f.newPlayer);
            return null;
        }).when(f.connection).handleClientCommand(any());
        invoke(f.match, "processVanillaDeaths");
        invoke(f.match, "processVanillaDeaths");
        verify(f.connection, times(1)).handleClientCommand(any());
        assertSame(f.newPlayer, f.connection.player);
        verify(f.oldPlayer, never()).setHealth(anyFloat());
        verify(f.oldPlayer, never()).setGameMode(any());
    }

    @Test void respawnCallbackDefersStateChangesUntilConnectionReplacement() throws Exception {
        Fixture f = new Fixture();
        f.waiting.put(f.id, new MatchManager.DownedEntry(f.id, Team.TEAM_A, 0, 64, 0, 0, 0, 100));
        f.match.handlePlayerRespawn(f.newPlayer);
        verify(f.newPlayer, never()).setGameMode(any());
        f.connection.player = f.newPlayer;
        when(f.list.getPlayer(f.id)).thenReturn(f.newPlayer);
        invoke(f.match, "processVanillaDeaths");
        verify(f.newPlayer).setGameMode(GameType.SPECTATOR);
        assertTrue(f.match.isDowned(f.newPlayer));
        assertEquals(100, f.match.respawnRemainingTicks(f.newPlayer));
        verify(f.spawns, never()).tryTeleportToTeamSpawn(any(), any(), any());
    }

    @Test void noSafeSpawnKeepsPlayerWaitingAndInvulnerable() throws Exception {
        Fixture f = new Fixture();
        f.waiting.put(f.id, new MatchManager.DownedEntry(f.id, Team.TEAM_A, 0, 64, 0, 0, 0, 0));
        when(f.server.getTickCount()).thenReturn(20);
        doReturn(SpawnSelectionStrategy.RANDOM).when(f.match).rulesSpawnStrategy();
        doReturn(false).when(f.match).sidesSwappedThisRound();
        invoke(f.match, "processDownedPlayers");
        assertTrue(f.match.isDowned(f.oldPlayer));
        verify(f.oldPlayer).setGameMode(GameType.SPECTATOR);
        verify(f.oldPlayer, never()).setInvulnerable(false);
    }

    @Test void safeSpawnRestoresCombatOnlyOnCurrentPlayer() throws Exception {
        Fixture f = new Fixture();
        f.waiting.put(f.id, new MatchManager.DownedEntry(f.id, Team.TEAM_A, 0, 64, 0, 0, 0, 0));
        f.connection.player = f.newPlayer;
        when(f.list.getPlayer(f.id)).thenReturn(f.newPlayer);
        when(f.server.getTickCount()).thenReturn(20);
        when(f.newPlayer.getFoodData()).thenReturn(mock(net.minecraft.world.food.FoodData.class));
        when(f.newPlayer.getMaxHealth()).thenReturn(20F);
        doReturn(SpawnSelectionStrategy.RANDOM).when(f.match).rulesSpawnStrategy();
        doReturn(false).when(f.match).sidesSwappedThisRound();
        doNothing().when(f.match).sendMatchSync(any());
        when(f.spawns.tryTeleportToTeamSpawn(f.newPlayer, Team.TEAM_A, SpawnSelectionStrategy.RANDOM))
                .thenReturn(true);
        invoke(f.match, "processDownedPlayers");
        assertFalse(f.match.isDowned(f.newPlayer));
        var order = inOrder(f.spawns, f.newPlayer);
        order.verify(f.spawns).tryTeleportToTeamSpawn(f.newPlayer, Team.TEAM_A, SpawnSelectionStrategy.RANDOM);
        order.verify(f.newPlayer).setCamera(f.newPlayer);
        order.verify(f.newPlayer).setGameMode(GameType.SURVIVAL);
        order.verify(f.newPlayer).setInvulnerable(false);
        verify(f.newPlayer).onUpdateAbilities();
        verify(f.newPlayer).setHealth(20F);
        verify(f.oldPlayer, never()).setGameMode(any());
    }

    private static void set(Object target, String name, Object value) throws Exception {
        var field = MatchManager.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void invoke(MatchManager target, String name) throws Exception {
        var method = MatchManager.class.getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(target);
    }

    private static final class Fixture {
        final MatchManager match = mock(MatchManager.class, CALLS_REAL_METHODS);
        final MinecraftServer server = mock(MinecraftServer.class);
        final PlayerList list = mock(PlayerList.class);
        final TeamManager teams = mock(TeamManager.class);
        final SpawnManager spawns = mock(SpawnManager.class);
        final ServerPlayer oldPlayer = mock(ServerPlayer.class), newPlayer = mock(ServerPlayer.class);
        final ServerGamePacketListenerImpl connection = mock(ServerGamePacketListenerImpl.class);
        final UUID id = UUID.randomUUID();
        final HashMap<UUID, MatchManager.DownedEntry> waiting = new HashMap<>();

        Fixture() throws Exception {
            set(match, "server", server); set(match, "teams", teams); set(match, "spawns", spawns);
            set(match, "downedPlayers", waiting); set(match, "pendingDeaths", new HashMap<>());
            set(match, "respawnedPlayers", new HashSet<>()); set(match, "state", MatchState.PLAYING);
            when(server.getPlayerList()).thenReturn(list);
            when(list.getPlayer(id)).thenReturn(oldPlayer);
            when(oldPlayer.getUUID()).thenReturn(id); when(newPlayer.getUUID()).thenReturn(id);
            when(oldPlayer.isAlive()).thenReturn(true); when(newPlayer.isAlive()).thenReturn(true);
            when(teams.getTeam(any(ServerPlayer.class))).thenReturn(Team.TEAM_A);
            oldPlayer.connection = connection; newPlayer.connection = connection; connection.player = oldPlayer;
            doReturn(false).when(match).isMatchActive();
            doNothing().when(match).broadcastMatchState();
        }
    }
}
