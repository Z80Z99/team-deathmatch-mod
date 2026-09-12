package cn.blockforge.generated.generatedmod.match;

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
        if (phase != Phase.CARRIED || !playerId.equals(carrierId)) {
            return;
        }
        phase = Phase.PLANTING;
        operatorId = playerId;
        x = posX;
        y = posY;
        z = posZ;
        actionStartTick = now;
        actionProgress = 0;
    }

    public void startDefusing(UUID playerId, long now) {
        startDefusing(playerId, now, false);
    }

    public void startDefusing(UUID playerId, long now, boolean resume) {
        if (phase != Phase.PLANTED) {
            return;
        }
        phase = Phase.DEFUSING;
        operatorId = playerId;
        if (!resume) {
            actionProgress = 0;
        }
        actionStartTick = Math.max(0L, now - actionProgress);
    }

    public boolean advanceAction(long now, int duration) {
        if (phase != Phase.PLANTING && phase != Phase.DEFUSING) {
            return false;
        }
        actionProgress = (int) Math.max(0L, Math.min(duration, now - actionStartTick));
        return actionProgress >= duration;
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
        actionProgress = PLANT_DURATION_TICKS;
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
        actionProgress = Math.max(1, durationTicks);
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
        cancelAction(false);
    }

    public void cancelAction(boolean resumeDefuse) {
        if (phase == Phase.PLANTING) {
            phase = Phase.CARRIED;
        } else if (phase == Phase.DEFUSING) {
            phase = Phase.PLANTED;
        } else {
            return;
        }
        operatorId = null;
        actionStartTick = 0L;
        if (!(resumeDefuse && phase == Phase.PLANTED)) {
            actionProgress = 0;
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
                : phase == Phase.DEFUSING ? Math.max(1, defuseDurationTicks) : 0;
        return Math.max(0, duration - actionProgress);
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
