package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.match.ClassicBombState;
import cn.blockforge.generated.generatedmod.network.packet.BombSyncPacket;

public final class ClientBombData {
    public static boolean active;
    public static ClassicBombState.Phase phase = ClassicBombState.Phase.INACTIVE;
    public static String carrierName = "";
    public static String operatorName = "";
    public static String bombSiteName = "";
    public static int detonationRemainingTicks;
    public static int detonationTotalTicks;
    public static int actionRemainingTicks;
    public static double x, y, z;
    private static int revision;

    private ClientBombData() { }

    public static void apply(BombSyncPacket packet) {
        active = packet.active();
        phase = packet.phase();
        carrierName = packet.carrierName();
        operatorName = packet.operatorName();
        bombSiteName = packet.bombSiteName();
        detonationRemainingTicks = packet.detonationRemainingTicks();
        detonationTotalTicks = packet.detonationTotalTicks();
        actionRemainingTicks = packet.actionRemainingTicks();
        x = packet.x(); y = packet.y(); z = packet.z();
        revision++;
    }

    public static void clear() {
        active = false;
        phase = ClassicBombState.Phase.INACTIVE;
        carrierName = operatorName = bombSiteName = "";
        detonationRemainingTicks = detonationTotalTicks = actionRemainingTicks = 0;
        revision++;
    }

    public static int revision() { return revision; }

    /** 两个服务器同步包之间平滑推进显示用倒计时，不改变服务器判定。 */
    public static void tick() {
        if (!active) return;
        if (phase == ClassicBombState.Phase.PLANTED && detonationRemainingTicks > 0) {
            detonationRemainingTicks--;
        }
        if ((phase == ClassicBombState.Phase.PLANTING || phase == ClassicBombState.Phase.DEFUSING)
                && actionRemainingTicks > 0) {
            actionRemainingTicks--;
        }
    }
}
