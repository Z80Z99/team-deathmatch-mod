package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.network.packet.WeaponRepositorySyncPacket;
import cn.blockforge.generated.generatedmod.weapon.WeaponRepositoryView;

public final class ClientWeaponRepositoryData {
    private static WeaponRepositoryView view = WeaponRepositoryView.empty();
    private static int revision;

    private ClientWeaponRepositoryData() { }

    public static void apply(WeaponRepositorySyncPacket packet) {
        view = packet.view();
        revision++;
    }

    public static void clear() {
        view = WeaponRepositoryView.empty();
        revision++;
    }

    public static WeaponRepositoryView view() {
        return view;
    }

    public static int revision() {
        return revision;
    }
}
