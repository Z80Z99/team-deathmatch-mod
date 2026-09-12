package cn.blockforge.generated.generatedmod.match;

import net.minecraft.core.Direction;

import java.util.UUID;

/** Classic bomb objective state machine; world interaction lives in ClassicBombManager. */
public final class ClassicBombState {
    public static final int PLANT_DURATION_TICKS = 80;
    public static final int DEFUSE_DURATION_TICKS = 100;
    public static final int DETONATION_DURATION_TICKS = 800;

    public enum Phase {
        INACTIVE,
        CARRIED,
        DROPPED,
        PLANTING,
        PLANTED,
        DEFUSING,
        EXPLODED,
        DEFUSED
    }

    private Phase phase = Phase.INACTIVE;
    private UUID carrierId;
    private UUID operatorId;
    private UUID planterId;
    private UUID defuserId;
    private double x;
    private double y;
    private double z;
    private String bombSiteId = "";
    private String bombSiteName = "";
    private long actionStartTick;
    private long droppedTick;
    private long plantedTick;
    private long detonateTick;
    private int actionProgress;
    private int defuseDuration = DEFUSE_DURATION_TICKS;
    private Direction plantFace = Direction.UP;
    private float plantYaw;
    private float plantPitch;
    private boolean droppedGlowing;
    private boolean plantedGlowing;

    public void startRound(UUID playerId, long now) {
        if (phase != Phase.INACTIVE) {
            throw new IllegalStateException("Bomb round already started");
        }
        phase = Phase.CARRIED;
        carrierId = playerId;
        operatorId = null;
        planterId = null;
        defuserId = null;
        x = 0.0D;
        y = 0.0D;
        z = 0.0D;
        bombSiteId = "";
        bombSiteName = "";
        actionStartTick = 0L;
        droppedTick = 0L;
        plantedTick = 0L;
        detonateTick = 0L;
        actionProgress = 0;
        defuseDuration = DEFUSE_DURATION_TICKS;
        plantFace = Direction.UP;
        plantYaw = 0.0F;
        plantPitch = 0.0F;
        droppedGlowing = false;
        plantedGlowing = false;
    }

    public void dropAt(UUID playerId, double posX, double posY, double posZ, long now) {
        if (phase != Phase.CARRIED && phase != Phase.PLANTING) {
            return;
        }
        if (phase == Phase.PLANTING && operatorId != null && !operatorId.equals(playerId)) {
            return;
        }
        phase = Phase.DROPPED;
        carrierId = null;
        operatorId = null;
        x = posX;
        y = posY;
        z = posZ;
        droppedTick = now;
        actionProgress = 0;
    }

    public void pickup(UUID playerId) {
        if (phase != Phase.DROPPED) {
            return;
        }
        phase = Phase.CARRIED;
        carrierId = playerId;
        droppedTick = 0L;
    }

    public void startPlanting(UUID playerId, long now, double posX, double posY, double posZ) {
        startPlanting(playerId, now, posX, posY, posZ, Direction.UP, 0.0F, 0.0F);
    }

    public void startPlanting(UUID playerId, long now, double posX, double posY, double posZ,
                              Direction face, float yaw, float pitch) {
        if (phase != Phase.CARRIED || !playerId.equals(carrierId)) {
            return;
        }
        phase = Phase.PLANTING;
        operatorId = playerId;
        x = posX;
        y = posY;
        z = posZ;
        plantFace = face == null ? Direction.UP : face;
        plantYaw = yaw;
        plantPitch = pitch;
        actionStartTick = now;
        actionProgress = 0;
    }

    public void startDefusing(UUID playerId, long now) {
        startDefusing(playerId, now, DEFUSE_DURATION_TICKS);
    }

    public void startDefusing(UUID playerId, long now, int durationTicks) {
        if (phase != Phase.PLANTED) {
            return;
        }
        phase = Phase.DEFUSING;
        operatorId = playerId;
        defuseDuration = Math.max(1, durationTicks);
        actionStartTick = Math.max(0L, now - actionProgress);
    }

    public boolean advanceAction(long now, int duration) {
        if (phase != Phase.PLANTING && phase != Phase.DEFUSING) {
            return false;
        }
        int effective = phase == Phase.DEFUSING ? defuseDuration : Math.max(1, duration);
        actionProgress = (int) Math.max(0L, Math.min(effective, now - actionStartTick));
        return actionProgress >= effective;
    }

    public void finishPlanting(long now, String siteId, String siteName) {
        finishPlanting(now, siteId, siteName, DETONATION_DURATION_TICKS);
    }

    public void finishPlanting(long now, String siteId, String siteName, int detonationDurationTicks) {
        if (phase != Phase.PLANTING) {
            return;
        }
        phase = Phase.PLANTED;
        planterId = operatorId;
        operatorId = null;
        bombSiteId = siteId == null ? "" : siteId;
        bombSiteName = siteName == null ? "" : siteName;
        plantedTick = now;
        detonateTick = now + Math.max(1, detonationDurationTicks);
        actionProgress = 0;
    }

    public void finishDefusing() {
        finishDefusing(DEFUSE_DURATION_TICKS);
    }

    public void finishDefusing(int durationTicks) {
        if (phase != Phase.DEFUSING) {
            return;
        }
        phase = Phase.DEFUSED;
        defuserId = operatorId;
        operatorId = null;
        defuseDuration = Math.max(1, durationTicks);
        actionProgress = Math.max(1, defuseDuration);
    }

    public void explode(long now) {
        if (phase != Phase.PLANTED && phase != Phase.DEFUSING) {
            return;
        }
        phase = Phase.EXPLODED;
        operatorId = null;
        detonateTick = now;
    }

    public void cancelAction() {
        cancelAction(true);
    }

    public void cancelAction(boolean resumeDefuse) {
        Phase previous = phase;
        if (phase == Phase.PLANTING) {
            phase = Phase.CARRIED;
        } else if (phase == Phase.DEFUSING) {
            phase = Phase.PLANTED;
        } else {
            return;
        }
        operatorId = null;
        actionStartTick = 0L;
        if (previous == Phase.PLANTING || !resumeDefuse) {
            actionProgress = 0;
        } else if (previous == Phase.DEFUSING) {
            actionProgress = Math.min(actionProgress, defuseDuration);
        }
    }

    public void recoverDefuseProgress() {
        if (phase == Phase.PLANTED && actionProgress > 0) {
            actionProgress = Math.max(0, actionProgress - 1);
        }
    }

    public void reset() {
        phase = Phase.INACTIVE;
        carrierId = null;
        operatorId = null;
        planterId = null;
        defuserId = null;
        x = 0.0D;
        y = 0.0D;
        z = 0.0D;
        bombSiteId = "";
        bombSiteName = "";
        actionStartTick = 0L;
        droppedTick = 0L;
        plantedTick = 0L;
        detonateTick = 0L;
        actionProgress = 0;
        defuseDuration = DEFUSE_DURATION_TICKS;
        plantFace = Direction.UP;
        plantYaw = 0.0F;
        plantPitch = 0.0F;
        droppedGlowing = false;
        plantedGlowing = false;
    }

    public Phase phase() {
        return phase;
    }

    public UUID carrierId() {
        return carrierId;
    }

    public UUID operatorId() {
        return operatorId;
    }

    public UUID planterId() {
        return planterId;
    }

    public UUID defuserId() {
        return defuserId;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public String bombSiteId() {
        return bombSiteId;
    }

    public String bombSiteName() {
        return bombSiteName;
    }

    public long droppedTick() {
        return droppedTick;
    }

    public long plantedTick() {
        return plantedTick;
    }

    public long detonateTick() {
        return detonateTick;
    }

    public int actionProgress() {
        return actionProgress;
    }

    public int actionRemainingTicks() {
        return actionRemainingTicks(PLANT_DURATION_TICKS, DEFUSE_DURATION_TICKS);
    }

    public int actionRemainingTicks(int plantDurationTicks, int defuseDurationTicks) {
        int duration = phase == Phase.PLANTING ? Math.max(1, plantDurationTicks)
                : phase == Phase.DEFUSING || phase == Phase.PLANTED ? Math.max(1, defuseDuration) : 0;
        return Math.max(0, duration - actionProgress);
    }

    public int defuseDuration() {
        return defuseDuration;
    }

    public Direction plantFace() {
        return plantFace;
    }

    public float plantYaw() {
        return plantYaw;
    }

    public float plantPitch() {
        return plantPitch;
    }

    public boolean droppedGlowing() {
        return droppedGlowing;
    }

    public void markDroppedGlowing() {
        droppedGlowing = true;
    }

    public boolean plantedGlowing() {
        return plantedGlowing;
    }

    public void markPlantedGlowing() {
        plantedGlowing = true;
    }

    public int detonationRemainingTicks(long now) {
        return phase == Phase.PLANTED || phase == Phase.DEFUSING
                ? (int) Math.max(0L, detonateTick - now) : 0;
    }

    public boolean isActionActive() {
        return phase == Phase.PLANTING || phase == Phase.DEFUSING;
    }

    public boolean isPlantedOrTerminal() {
        return phase == Phase.PLANTED || phase == Phase.DEFUSING
                || phase == Phase.EXPLODED || phase == Phase.DEFUSED;
    }

    public boolean isTerminal() {
        return phase == Phase.EXPLODED || phase == Phase.DEFUSED;
    }
}
