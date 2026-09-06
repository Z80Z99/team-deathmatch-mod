package cn.blockforge.generated.generatedmod.client.network;

import cn.blockforge.generated.generatedmod.client.ClientConfigData;
import cn.blockforge.generated.generatedmod.client.ClientHudDynamicStats;
import cn.blockforge.generated.generatedmod.client.ClientLobbyData;
import cn.blockforge.generated.generatedmod.client.ClientMapEditorData;
import cn.blockforge.generated.generatedmod.client.ClientMatchData;
import cn.blockforge.generated.generatedmod.network.packet.ConfigSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.HudStatSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.LobbyConfigSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.MapEditorSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.MatchmakingSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.MatchSyncPacket;
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
}
