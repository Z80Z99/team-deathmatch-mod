package cn.blockforge.generated.generatedmod.network;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.network.packet.ConfigRequestPacket;
import cn.blockforge.generated.generatedmod.network.packet.ConfigSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.HudStatSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.LobbyConfigSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.MapEditorActionPacket;
import cn.blockforge.generated.generatedmod.network.packet.MapEditorSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.MapBrushClickPacket;
import cn.blockforge.generated.generatedmod.network.packet.MapRegionPacket;
import cn.blockforge.generated.generatedmod.network.packet.MatchmakingActionPacket;
import cn.blockforge.generated.generatedmod.network.packet.MatchmakingSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.MatchSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.RoomActionPacket;
import cn.blockforge.generated.generatedmod.network.packet.RoomMapListSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.RoomRulesPacket;
import cn.blockforge.generated.generatedmod.network.packet.RoomSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.RespawnRequestPacket;
import cn.blockforge.generated.generatedmod.network.packet.WeaponRepositoryActionPacket;
import cn.blockforge.generated.generatedmod.network.packet.WeaponRepositorySyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.MatchShopActionPacket;
import cn.blockforge.generated.generatedmod.network.packet.MatchShopSyncPacket;
import cn.blockforge.generated.generatedmod.network.packet.BombSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class FpsTdmNetwork {
    private static final String PROTOCOL_VERSION = "22";
    private static SimpleChannel channel;
    private static int nextId;

    private FpsTdmNetwork() {
    }

    public static void register() {
        if (channel != null) {
            return;
        }
        channel = NetworkRegistry.newSimpleChannel(
                ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals);
        channel.messageBuilder(MatchSyncPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MatchSyncPacket::encode)
                .decoder(MatchSyncPacket::new)
                .consumerMainThread(MatchSyncPacket::handle)
                .add();
        channel.messageBuilder(ConfigSyncPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ConfigSyncPacket::encode)
                .decoder(ConfigSyncPacket::new)
                .consumerMainThread(ConfigSyncPacket::handle)
                .add();
        channel.messageBuilder(ConfigRequestPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(ConfigRequestPacket::encode)
                .decoder(ConfigRequestPacket::new)
                .consumerMainThread(ConfigRequestPacket::handle)
                .add();
        channel.messageBuilder(RoomActionPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(RoomActionPacket::encode)
                .decoder(RoomActionPacket::new)
                .consumerMainThread(RoomActionPacket::handle)
                .add();
        channel.messageBuilder(RoomSyncPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RoomSyncPacket::encode)
                .decoder(RoomSyncPacket::new)
                .consumerMainThread(RoomSyncPacket::handle)
                .add();
        channel.messageBuilder(MatchmakingActionPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(MatchmakingActionPacket::encode)
                .decoder(MatchmakingActionPacket::new)
                .consumerMainThread(MatchmakingActionPacket::handle)
                .add();
        channel.messageBuilder(MatchmakingSyncPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MatchmakingSyncPacket::encode)
                .decoder(MatchmakingSyncPacket::new)
                .consumerMainThread(MatchmakingSyncPacket::handle)
                .add();
        channel.messageBuilder(MapEditorActionPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(MapEditorActionPacket::encode)
                .decoder(MapEditorActionPacket::new)
                .consumerMainThread(MapEditorActionPacket::handle)
                .add();
        channel.messageBuilder(MapEditorSyncPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MapEditorSyncPacket::encode)
                .decoder(MapEditorSyncPacket::new)
                .consumerMainThread(MapEditorSyncPacket::handle)
                .add();
        channel.messageBuilder(MapRegionPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(MapRegionPacket::encode)
                .decoder(MapRegionPacket::new)
                .consumerMainThread(MapRegionPacket::handle)
                .add();
        channel.messageBuilder(MapBrushClickPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(MapBrushClickPacket::encode)
                .decoder(MapBrushClickPacket::new)
                .consumerMainThread(MapBrushClickPacket::handle)
                .add();
        channel.messageBuilder(LobbyConfigSyncPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(LobbyConfigSyncPacket::encode)
                .decoder(LobbyConfigSyncPacket::new)
                .consumerMainThread(LobbyConfigSyncPacket::handle)
                .add();
        channel.messageBuilder(RoomRulesPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(RoomRulesPacket::encode)
                .decoder(RoomRulesPacket::new)
                .consumerMainThread(RoomRulesPacket::handle)
                .add();
        channel.messageBuilder(RoomMapListSyncPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RoomMapListSyncPacket::encode)
                .decoder(RoomMapListSyncPacket::new)
                .consumerMainThread(RoomMapListSyncPacket::handle)
                .add();
        channel.messageBuilder(HudStatSyncPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HudStatSyncPacket::encode)
                .decoder(HudStatSyncPacket::new)
                .consumerMainThread(HudStatSyncPacket::handle)
                .add();
        channel.messageBuilder(RespawnRequestPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(RespawnRequestPacket::encode)
                .decoder(RespawnRequestPacket::new)
                .consumerMainThread(RespawnRequestPacket::handle)
                .add();
        channel.messageBuilder(WeaponRepositoryActionPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(WeaponRepositoryActionPacket::encode)
                .decoder(WeaponRepositoryActionPacket::new)
                .consumerMainThread(WeaponRepositoryActionPacket::handle)
                .add();
        channel.messageBuilder(WeaponRepositorySyncPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(WeaponRepositorySyncPacket::encode)
                .decoder(WeaponRepositorySyncPacket::new)
                .consumerMainThread(WeaponRepositorySyncPacket::handle)
                .add();
        channel.messageBuilder(MatchShopActionPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(MatchShopActionPacket::encode)
                .decoder(MatchShopActionPacket::new)
                .consumerMainThread(MatchShopActionPacket::handle)
                .add();
        channel.messageBuilder(MatchShopSyncPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MatchShopSyncPacket::encode)
                .decoder(MatchShopSyncPacket::new)
                .consumerMainThread(MatchShopSyncPacket::handle)
                .add();
        channel.messageBuilder(BombSyncPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BombSyncPacket::encode)
                .decoder(BombSyncPacket::new)
                .consumerMainThread(BombSyncPacket::handle)
                .add();
    }

    public static void sendToPlayer(Object packet, ServerPlayer player) {
        if (channel != null) {
            channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    public static void sendToServer(Object packet) {
        if (channel != null) {
            channel.sendToServer(packet);
        }
    }

    private static int nextId() {
        return nextId++;
    }
}
