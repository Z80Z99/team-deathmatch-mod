package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.config.FpsTdmConfigValues;
import cn.blockforge.generated.generatedmod.network.packet.ConfigSyncPacket;

/** 当前连接服务器下发的配置副本；客户端不会直接写本地 common 配置。 */
public final class ClientConfigData {
    private static FpsTdmConfigValues values = FpsTdmConfigValues.defaults();
    private static boolean received;
    private static boolean canEdit;
    private static boolean configLocked;
    private static String message = "";
    private static boolean error;

    private ClientConfigData() {
    }

    public static void apply(ConfigSyncPacket packet) {
        values = packet.values();
        received = true;
        canEdit = packet.canEdit();
        configLocked = packet.configLocked();
        message = packet.message();
        error = packet.error();
    }

    public static void clear() {
        values = FpsTdmConfigValues.defaults();
        received = false;
        canEdit = false;
        configLocked = false;
        message = "";
        error = false;
    }

    public static FpsTdmConfigValues values() {
        return values;
    }

    public static boolean received() {
        return received;
    }

    public static boolean canEdit() {
        return canEdit;
    }

    public static boolean configLocked() {
        return configLocked;
    }

    public static String message() {
        return message;
    }

    public static boolean error() {
        return error;
    }
}
