package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.match.ClassicBombState;
import cn.blockforge.generated.generatedmod.match.Team;

import java.util.UUID;

/** Data carrier retained only so the incomplete classic-bomb draft cannot block unrelated builds. */
public record BombSyncPacket(
        boolean active,
        ClassicBombState.Phase phase,
        Team attackingTeam,
        Team defendingTeam,
        UUID carrierId,
        String carrierName,
        UUID operatorId,
        String operatorName,
        String bombSiteId,
        String bombSiteName,
        int actionProgress,
        int actionRemainingTicks,
        int detonationRemainingTicks,
        double x,
        double y,
        double z) {
}
