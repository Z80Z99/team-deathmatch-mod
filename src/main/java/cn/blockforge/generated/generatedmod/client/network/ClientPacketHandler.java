package cn.blockforge.generated.generatedmod.client.network;

import cn.blockforge.generated.generatedmod.client.ClientConfigData;
import cn.blockforge.generated.generatedmod.client.ClientHudDynamicStats;
import cn.blockforge.generated.generatedmod.client.ClientLobbyData;
import cn.blockforge.generated.generatedmod.client.ClientMapEditorData;
import cn.blockforge.generated.generatedmod.client.ClientMatchData;
import cn.blockforge.generated.generatedmod.client.ClientWeaponRepositoryData;
import cn.blockforge.generated.generatedmod.client.ClientMatchShopData;
import cn.blockforge.generated.generatedmod.client.ClientBombData;
import cn.blockforge.generated.generatedmod.client.ClientHudEventData;
import cn.blockforge.generated.generatedmod.client.ClientBombPreviewData;
import cn.blockforge.generated.generatedmod.network.packet.ConfigSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.HudStatSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.LobbyConfigSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.MapEditorSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.MatchmakingSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.MatchSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.WeaponRepositorySyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.MatchShopSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.BombSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.HudEventPacket;
import cn.blockforge.generated.generatedmod.network.packet.BombPreviewPacket;
import cn.blockforge.generated.generatedmod.network.packet.RoomMapListSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.RoomSyncPacket;

public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void handle(MatchSyncPacket packet) {
        ClientMatchData.apply(packet);
    }

    public static void handle(ConfigSyncPacket packet) {
        ClientConfigData.apply(packet);
    }

    public static void handle(RoomSyncPacket packet) {
        ClientLobbyData.apply(packet);
    }

    public static void handle(MatchmakingSyncPacket packet) {
        ClientLobbyData.apply(packet);
    }

    public static void handle(MapEditorSyncPacket packet) {
        ClientMapEditorData.apply(packet);
    }

    public static void handle(LobbyConfigSyncPacket packet) {
        ClientLobbyData.apply(packet);
    }

    public static void handle(RoomMapListSyncPacket packet) {
        ClientLobbyData.apply(packet);
    }

    public static void handle(HudStatSyncPacket packet) {
        ClientHudDynamicStats.apply(packet);
    }

    public static void handle(WeaponRepositorySyncPacket packet) {
        ClientWeaponRepositoryData.apply(packet);
    }

    public static void handle(MatchShopSyncPacket packet) {
        ClientMatchShopData.apply(packet);
    }

    public static void handle(BombSyncPacket packet) {
        ClientBombData.apply(packet);
    }

    public static void handle(HudEventPacket packet) {
        ClientHudEventData.apply(packet);
    }

    public static void handle(BombPreviewPacket packet) {
        ClientBombPreviewData.apply(packet);
    }
}
