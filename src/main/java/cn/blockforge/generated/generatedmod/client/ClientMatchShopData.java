package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.network.packet.MatchShopSyncPacket;
import cn.blockforge.generated.generatedmod.shop.MatchShopView;

public final class ClientMatchShopData {
    private static MatchShopView view = MatchShopView.empty();
    private static int revision;

    private ClientMatchShopData() { }

    public static void apply(MatchShopSyncPacket packet) {
        view = packet.view();
        revision++;
    }

    public static void clear() {
        view = MatchShopView.empty();
        revision++;
    }

    public static MatchShopView view() { return view; }
    public static int revision() { return revision; }
}
