package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.client.network.ClientPacketHandler;
import cn.blockforge.generated.generatedmod.match.ClassicBombState;
import cn.blockforge.generated.generatedmod.match.Team;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

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
        int detonationTotalTicks,
        double x,
        double y,
        double z) {

    /** Compatibility constructor for older callers; unknown total falls back to remaining. */
    public BombSyncPacket(boolean active, ClassicBombState.Phase phase, Team attackingTeam,
                          Team defendingTeam, UUID carrierId, String carrierName, UUID operatorId,
                          String operatorName, String bombSiteId, String bombSiteName,
                          int actionProgress, int actionRemainingTicks, int detonationRemainingTicks,
                          double x, double y, double z) {
        this(active, phase, attackingTeam, defendingTeam, carrierId, carrierName, operatorId,
                operatorName, bombSiteId, bombSiteName, actionProgress, actionRemainingTicks,
                detonationRemainingTicks, detonationRemainingTicks, x, y, z);
    }

    public BombSyncPacket(FriendlyByteBuf buffer) {
        this(buffer.readBoolean(),
                ClassicBombState.Phase.values()[Math.max(0, Math.min(
                        ClassicBombState.Phase.values().length - 1, buffer.readVarInt()))],
                readTeam(buffer), readTeam(buffer),
                readUuid(buffer), buffer.readUtf(64),
                readUuid(buffer), buffer.readUtf(64),
                buffer.readUtf(96), buffer.readUtf(96),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(active);
        buffer.writeVarInt(phase == null ? 0 : phase.ordinal());
        writeTeam(buffer, attackingTeam);
        writeTeam(buffer, defendingTeam);
        writeUuid(buffer, carrierId);
        buffer.writeUtf(carrierName == null ? "" : carrierName, 64);
        writeUuid(buffer, operatorId);
        buffer.writeUtf(operatorName == null ? "" : operatorName, 64);
        buffer.writeUtf(bombSiteId == null ? "" : bombSiteId, 96);
        buffer.writeUtf(bombSiteName == null ? "" : bombSiteName, 96);
        buffer.writeVarInt(actionProgress);
        buffer.writeVarInt(actionRemainingTicks);
        buffer.writeVarInt(detonationRemainingTicks);
        buffer.writeVarInt(detonationTotalTicks);
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(z);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handle(this)));
        context.setPacketHandled(true);
    }

    private static void writeUuid(FriendlyByteBuf buffer, UUID value) {
        buffer.writeBoolean(value != null);
        if (value != null) buffer.writeUUID(value);
    }

    private static UUID readUuid(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? buffer.readUUID() : null;
    }

    private static void writeTeam(FriendlyByteBuf buffer, Team value) {
        buffer.writeBoolean(value != null);
        if (value != null) buffer.writeEnum(value);
    }

    private static Team readTeam(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? buffer.readEnum(Team.class) : null;
    }
}
