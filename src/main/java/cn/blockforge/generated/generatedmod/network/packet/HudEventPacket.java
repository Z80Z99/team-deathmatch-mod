package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.client.network.ClientPacketHandler;
import cn.blockforge.generated.generatedmod.match.MatchHudEventType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** One structured HUD event; multiple events may remain active at the same time. */
public final class HudEventPacket {
    private final String id;
    private final String title;
    private final String detail;
    private final int durationTicks;
    private final int priority;

    public HudEventPacket(MatchHudEventType type, String detail, int durationTicks) {
        this(type, detail, durationTicks, type == null ? 0 : type.priority());
    }

    public HudEventPacket(MatchHudEventType type, String detail, int durationTicks, int priority) {
        if (type == null) throw new IllegalArgumentException("HUD event type is required");
        this.id = type.id();
        this.title = type.displayName();
        this.detail = detail == null ? type.defaultDetail() : detail;
        this.durationTicks = Math.max(1, durationTicks);
        this.priority = Math.max(0, Math.min(100, priority));
    }

    /** Constructor for third-party structured HUD events. */
    public HudEventPacket(String id, String title, String detail, int durationTicks, int priority) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("HUD event id is required");
        this.id = id.length() > 96 ? id.substring(0, 96) : id;
        String resolvedTitle = title == null || title.isBlank() ? this.id : title;
        String resolvedDetail = detail == null ? "" : detail;
        this.title = resolvedTitle.length() > 96 ? resolvedTitle.substring(0, 96) : resolvedTitle;
        this.detail = resolvedDetail.length() > 256 ? resolvedDetail.substring(0, 256)
                : resolvedDetail;
        this.durationTicks = Math.max(1, durationTicks);
        this.priority = Math.max(0, Math.min(100, priority));
    }

    public HudEventPacket(FriendlyByteBuf buffer) {
        id = buffer.readUtf(96);
        title = buffer.readUtf(96);
        detail = buffer.readUtf(256);
        durationTicks = Math.max(1, buffer.readVarInt());
        priority = Math.max(0, Math.min(100, buffer.readVarInt()));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(id, 96);
        buffer.writeUtf(title, 96);
        buffer.writeUtf(detail, 256);
        buffer.writeVarInt(durationTicks);
        buffer.writeVarInt(priority);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handle(this)));
        context.setPacketHandled(true);
    }

    public MatchHudEventType type() { return MatchHudEventType.byId(id); }
    public String id() { return id; }
    public String title() { return title; }
    public String detail() { return detail; }
    public int durationTicks() { return durationTicks; }
    public int priority() { return priority; }
}
