package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.match.MatchManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端把画笔左键/右键目标方块提交到服务器。 */
public final class MapBrushClickPacket {
    private final BlockPos position;
    private final boolean leftClick;

    public MapBrushClickPacket(BlockPos position, boolean leftClick) {
        this.position = position.immutable();
        this.leftClick = leftClick;
    }

    public MapBrushClickPacket(FriendlyByteBuf buffer) {
        position = BlockPos.of(buffer.readLong());
        leftClick = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeLong(position.asLong());
        buffer.writeBoolean(leftClick);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            MatchManager manager = MatchManager.get();
            if (manager == null || sender == null) return;
            if (!sender.getMainHandItem().is(cn.blockforge.generated.generatedmod.item.ModItems.MAP_BRUSH.get())
                    && !sender.getOffhandItem().is(cn.blockforge.generated.generatedmod.item.ModItems.MAP_BRUSH.get())) return;
            if (sender.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(position)) > 130 * 130
                    || !sender.serverLevel().hasChunkAt(position)) return;
            if (leftClick && sender.serverLevel().isEmptyBlock(position)) return;
            if (leftClick) {
                manager.mapEditor().brushLeft(sender, position);
            } else {
                manager.mapEditor().brushRight(sender, position);
            }
        });
        context.setPacketHandled(true);
    }
}
