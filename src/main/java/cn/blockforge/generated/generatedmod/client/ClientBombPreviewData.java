package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.network.packet.BombPreviewPacket;
import net.minecraft.core.Direction;

public final class ClientBombPreviewData {
    public static boolean present;
    public static double x, y, z;
    public static Direction face = Direction.UP;
    public static boolean valid;

    private ClientBombPreviewData() { }

    public static void apply(BombPreviewPacket packet) {
        present = packet.present();
        x = packet.x();
        y = packet.y();
        z = packet.z();
        face = packet.face() == null ? Direction.UP : packet.face();
        valid = packet.valid();
    }

    public static void clear() {
        present = false;
    }
}
