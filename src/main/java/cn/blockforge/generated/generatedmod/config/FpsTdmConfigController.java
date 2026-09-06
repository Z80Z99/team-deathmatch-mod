package cn.blockforge.generated.generatedmod.config;

import cn.blockforge.generated.generatedmod.match.MatchManager;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.ConfigSyncPacket;
import net.minecraft.server.level.ServerPlayer;

/** 服务器权威的配置读取、权限检查和保存入口。 */
public final class FpsTdmConfigController {
    private static final int ADMIN_PERMISSION = 2;

    private FpsTdmConfigController() {
    }

    public static void sendCurrent(ServerPlayer player, String message, boolean error) {
        if (player == null) {
            return;
        }
        MatchManager manager = MatchManager.get();
        boolean admin = player.hasPermissions(ADMIN_PERMISSION);
        boolean configLocked = isConfigLocked(manager);
        FpsTdmNetwork.sendToPlayer(new ConfigSyncPacket(
                admin ? FpsTdmConfigValues.current() : FpsTdmConfigValues.defaults(),
                admin,
                configLocked,
                admin ? (message == null ? "" : message) : "权限不足，只有服务器管理员可以打开游戏配置。",
                admin ? error : true), player);
    }

    private static boolean isConfigLocked(MatchManager manager) {
        return manager != null && (manager.isMatchActive()
                || manager.maps().isLoading()
                || manager.maps().isResetting());
    }
}
