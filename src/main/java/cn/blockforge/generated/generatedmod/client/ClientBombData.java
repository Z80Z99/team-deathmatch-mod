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
        actionRemainingTicks = packet.actionRemainingTicks();
        x = packet.x(); y = packet.y(); z = packet.z();
        revision++;
    }

    public static void clear() {
        active = false;
        phase = ClassicBombState.Phase.INACTIVE;
        carrierName = operatorName = bombSiteName = "";
        detonationRemainingTicks = actionRemainingTicks = 0;
        revision++;
    }

    public static int revision() { return revision; }
}
