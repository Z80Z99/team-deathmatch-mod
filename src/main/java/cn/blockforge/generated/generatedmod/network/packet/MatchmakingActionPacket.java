package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.lobby.MatchmakingAction;
import cn.blockforge.generated.generatedmod.match.MatchManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端操作快速匹配队列。 */
public final class MatchmakingActionPacket {
    private final MatchmakingAction action;

    public MatchmakingActionPacket(MatchmakingAction action) {
        this.action = action;
    }

    public MatchmakingActionPacket(FriendlyByteBuf buffer) {
        int ordinal = buffer.readVarInt();
        MatchmakingAction[] values = MatchmakingAction.values();
        action = ordinal < 0 || ordinal >= values.length ? null : values[ordinal];
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(action == null ? -1 : action.ordinal());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            MatchManager manager = MatchManager.get();
            if (manager == null || sender == null || action == null) return;
            switch (action) {
                case JOIN -> {
                    if (manager.rooms().matchmaking().join(sender)) {
                        manager.rooms().matchmaking().sendSync(sender, "已进入匹配队列。", false);
                    } else {
                        manager.rooms().matchmaking().sendSync(sender, "当前无法进入匹配队列。", true);
                    }
                }
                case LEAVE -> {
                    manager.rooms().matchmaking().leave(sender);
                    manager.rooms().matchmaking().sendSync(sender, "已离开匹配队列。", false);
                }
                case REFRESH -> manager.rooms().matchmaking().sendSync(sender, "", false);
            }
        });
        context.setPacketHandled(true);
    }
}
