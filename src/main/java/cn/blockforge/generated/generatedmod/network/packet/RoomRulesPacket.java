package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.lobby.RoomRules;
import cn.blockforge.generated.generatedmod.match.MatchManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 房主在大厅里保存本房间的比赛规则；服务器端强制校验只有房主本人可改。 */
public final class RoomRulesPacket {
    private final RoomRules rules;

    public RoomRulesPacket(RoomRules rules) {
        this.rules = (rules == null ? RoomRules.fallback() : rules).normalized();
    }

    public RoomRulesPacket(FriendlyByteBuf buffer) {
        rules = RoomRules.read(buffer);
    }

    public void encode(FriendlyByteBuf buffer) {
        rules.write(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            MatchManager manager = MatchManager.get();
            if (manager != null && sender != null) {
                manager.rooms().saveRoomRules(sender, rules);
            }
        });
        context.setPacketHandled(true);
    }

    public RoomRules rules() {
        return rules;
    }
}
