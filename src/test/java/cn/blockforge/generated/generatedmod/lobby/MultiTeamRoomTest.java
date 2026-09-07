package cn.blockforge.generated.generatedmod.lobby;

import cn.blockforge.generated.generatedmod.match.Team;
import cn.blockforge.generated.generatedmod.network.packet.RoomSyncPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MultiTeamRoomTest {
    @org.junit.jupiter.api.BeforeAll static void bootstrap() {
        cn.blockforge.generated.generatedmod.MinecraftTestBootstrap.initialize();
    }

    @Test void serverRejectsForgedRoomInvalidTeamAndNonOwnerCountChanges() throws Exception {
        RoomRules defaults = RoomRules.fallback();
        try (var rules = mockStatic(RoomRules.class)) {
            rules.when(RoomRules::serverDefaults).thenReturn(defaults);
            UUID owner = UUID.randomUUID(), member = UUID.randomUUID();
            Room room = new Room("room", "test", owner, 8, "arena", false);
            room.add(member);
            var player = mock(net.minecraft.server.level.ServerPlayer.class);
            when(player.getUUID()).thenReturn(member);
            when(player.getGameProfile()).thenReturn(new com.mojang.authlib.GameProfile(member, "member"));
            RoomManager manager = mock(RoomManager.class, CALLS_REAL_METHODS);
            var field = RoomManager.class.getDeclaredField("rooms"); field.setAccessible(true);
            field.set(manager, new LinkedHashMap<>(Map.of("room", room)));
            doNothing().when(manager).sendAll(anyString(), anyBoolean());
            doNothing().when(manager).sendSync(any(), anyString(), anyBoolean());
            manager.handleAction(player, RoomAction.SET_TEAM_COUNT, "room", "", "", 4, "");
            assertEquals(2, room.teamCount());
            Team before = room.team(member);
            manager.handleAction(player, RoomAction.CHANGE_TEAM, "forged", "a", "", 0, "");
            manager.handleAction(player, RoomAction.CHANGE_TEAM, "room", "invalid", "", 0, "");
            manager.handleAction(player, RoomAction.CHANGE_TEAM, "room", "d", "", 0, "");
            assertEquals(before, room.team(member));
            manager.handleAction(player, RoomAction.CHANGE_TEAM, "room", "a", "", 0, "");
            assertEquals(Team.TEAM_A, room.team(member));
            room.state(RoomState.COUNTDOWN);
            manager.handleAction(player, RoomAction.CHANGE_TEAM, "room", "b", "", 0, "");
            assertEquals(Team.TEAM_A, room.team(member));
            verify(manager, times(5)).sendSync(eq(player), anyString(), eq(true));
        }
    }
    @Test void roomAssignsSwitchesAndRemovesMembersWithoutLosingAnyone() {
        RoomRules defaults = RoomRules.fallback();
        try (var rules = mockStatic(RoomRules.class)) {
            rules.when(RoomRules::serverDefaults).thenReturn(defaults);
            UUID owner = UUID.randomUUID();
            Room room = new Room("test", "test", owner, 12, "arena", false);
            assertTrue(room.teamCount(4));
            for (int i = 0; i < 7; i++) assertTrue(room.add(UUID.randomUUID()));
            for (Team team : Team.playing(4)) assertEquals(2, room.teamSize(team));
            assertFalse(room.changeTeam(UUID.randomUUID(), Team.TEAM_C));
            assertFalse(room.changeTeam(owner, Team.SPECTATOR));
            assertFalse(room.changeTeam(owner, null));
            assertTrue(room.changeTeam(owner, Team.TEAM_D));
            assertEquals(Team.TEAM_D, room.team(owner));
            room.state(RoomState.COUNTDOWN);
            assertFalse(room.changeTeam(owner, Team.TEAM_B));
            assertFalse(room.teamCount(2));
            room.state(RoomState.OPEN);
            assertFalse(room.teamCount(5));
            assertTrue(room.teamCount(2));
            assertEquals(8, room.memberCount());
            assertTrue(room.members().stream().allMatch(id -> Team.playing(2).contains(room.team(id))));
            room.remove(owner);
            assertNull(room.team(owner));
            assertEquals(7, room.teamSize(Team.TEAM_A) + room.teamSize(Team.TEAM_B));
        }
    }

    @Test void roomPacketPreservesFourTeamsAndMemberAssignments() {
        Map<String, Team> assignments = new LinkedHashMap<>();
        for (Team team : Team.playing(4)) assignments.put(team.key(), team);
        RoomView view = new RoomView("room", "四队房间", "team_a", 4, 16, "map", RoomState.OPEN,
                List.copyOf(assignments.keySet()), RoomRules.fallback().normalized(), false, false, 4, assignments);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new RoomSyncPacket(List.of(view), "room", true, false, "", false).encode(buffer);
            var decoded = new RoomSyncPacket(buffer);
            assertEquals(view, decoded.rooms().get(0));
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }
}
