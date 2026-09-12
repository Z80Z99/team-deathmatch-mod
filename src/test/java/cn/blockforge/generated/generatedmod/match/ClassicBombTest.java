package cn.blockforge.generated.generatedmod.match;

import cn.blockforge.generated.generatedmod.MinecraftTestBootstrap;
import cn.blockforge.generated.generatedmod.network.packet.BombSyncPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClassicBombTest {
    @BeforeAll static void bootstrap() {
        MinecraftTestBootstrap.initialize();
    }

    @Test void stateMachineCarriesPlantsDefusesAndResets() {
        ClassicBombState state = new ClassicBombState();
        UUID carrier = UUID.randomUUID();
        UUID defender = UUID.randomUUID();
        state.startRound(carrier, 100);
        assertEquals(ClassicBombState.Phase.CARRIED, state.phase());

        state.startPlanting(carrier, 120, 1.0D, 64.0D, 2.0D);
        assertTrue(state.advanceAction(200, ClassicBombState.PLANT_DURATION_TICKS));
        state.finishPlanting(200, "site_a", "A 点");
        assertEquals(ClassicBombState.Phase.PLANTED, state.phase());
        assertEquals(800, state.detonationRemainingTicks(200));

        state.startDefusing(defender, 300);
        assertTrue(state.advanceAction(400, ClassicBombState.DEFUSE_DURATION_TICKS));
        state.finishDefusing();
        assertEquals(ClassicBombState.Phase.DEFUSED, state.phase());
        assertEquals(defender, state.defuserId());

        state.reset();
        assertEquals(ClassicBombState.Phase.INACTIVE, state.phase());
    }

    @Test void bombSyncPacketRoundTripsCompleteObjectiveState() {
        UUID carrier = UUID.randomUUID();
        UUID operator = UUID.randomUUID();
        BombSyncPacket packet = new BombSyncPacket(true, ClassicBombState.Phase.DEFUSING,
                Team.TEAM_A, Team.TEAM_B, carrier, "Attacker", operator, "Defender",
                "site_a", "A 点", 52, 48, 620, 10.0D, 64.0D, 20.0D);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.encode(buffer);
            BombSyncPacket decoded = new BombSyncPacket(buffer);
            assertEquals(0, buffer.readableBytes());
            assertEquals(ClassicBombState.Phase.DEFUSING, decoded.phase());
            assertEquals(carrier, decoded.carrierId());
            assertEquals(operator, decoded.operatorId());
            assertEquals("A 点", decoded.bombSiteName());
            assertEquals(620, decoded.detonationRemainingTicks());
            assertEquals(20.0D, decoded.z());
        } finally {
            buffer.release();
        }
    }

    @Test void interruptedDefuseRecoversGraduallyAndCanResume() {
        ClassicBombState state = new ClassicBombState();
        UUID attacker = UUID.randomUUID();
        UUID defender = UUID.randomUUID();
        state.startRound(attacker, 0);
        state.startPlanting(attacker, 0, 1.0D, 64.0D, 2.0D);
        state.finishPlanting(80, "a", "A点", 800);
        state.startDefusing(defender, 100);
        state.advanceAction(140, 100);
        assertEquals(40, state.actionProgress());
        assertEquals(100, state.defuseDuration());
        state.cancelAction(true);
        assertEquals(ClassicBombState.Phase.PLANTED, state.phase());
        for (int i = 0; i < 10; i++) state.recoverDefuseProgress();
        assertEquals(30, state.actionProgress());
        state.startDefusing(defender, 200, 100);
        assertTrue(state.advanceAction(270, 100));
        state.finishDefusing(100);
        assertEquals(ClassicBombState.Phase.DEFUSED, state.phase());
    }
}
