package cn.blockforge.generated.generatedmod.network.packet;

import cn.blockforge.generated.generatedmod.client.network.ClientPacketHandler;
import cn.blockforge.generated.generatedmod.map.MapBrushMode;
import cn.blockforge.generated.generatedmod.map.MapDefinition;
import cn.blockforge.generated.generatedmod.map.MapEditorView;
import cn.blockforge.generated.generatedmod.map.MapRegion;
import cn.blockforge.generated.generatedmod.map.MapTool;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** 服务器向单个玩家同步其专属的地图编辑视图。 */
public final class MapEditorSyncPacket {
    private static final int MAX_TEXT = 256;
    private static final int MAX_MAPS = 64;
    private final MapEditorView view;

    public MapEditorSyncPacket(MapEditorView view) {
        this.view = view;
    }

    public MapEditorSyncPacket(FriendlyByteBuf buffer) {
        boolean hasTarget = buffer.readBoolean();
        String mapId = buffer.readUtf(MAX_TEXT);
        String displayName = buffer.readUtf(MAX_TEXT);
        String world = buffer.readUtf(MAX_TEXT);
        MapEditorView.RegionData bounds = readRegion(buffer);
        MapEditorView.RegionData reset = readRegion(buffer);
        MapEditorView.RegionData draftBounds = readRegion(buffer);
        MapEditorView.RegionData draftReset = readRegion(buffer);
        int teamA = buffer.readVarInt();
        int teamB = buffer.readVarInt();
        int spectator = buffer.readVarInt();
        String snapshot = buffer.readUtf(MAX_TEXT);
        boolean canEdit = buffer.readBoolean();
        boolean isAdmin = buffer.readBoolean();
        boolean locked = buffer.readBoolean();
        boolean draftInvalidated = buffer.readBoolean();
        List<String> ownedMaps = readStrings(buffer);
        List<String> serverMaps = readStrings(buffer);
        String shareCode = buffer.readUtf(MAX_TEXT);
        int responseRequestId = Math.max(0, buffer.readVarInt());
        String message = buffer.readUtf(MAX_TEXT);
        boolean error = buffer.readBoolean();
        List<MapRegion> regions = readRegions(buffer);
        MapTool selectedTool = MapTool.parse(buffer.readUtf(MAX_TEXT));
        MapBrushMode brushMode = MapBrushMode.parse(buffer.readUtf(MAX_TEXT));
        String selectedRegionId = buffer.readUtf(MAX_TEXT);
        int brushRange = buffer.readVarInt();
        view = new MapEditorView(hasTarget, mapId, displayName, world, bounds, reset, draftBounds, draftReset,
                teamA, teamB, spectator, snapshot, canEdit, isAdmin, locked, draftInvalidated,
                ownedMaps, serverMaps, shareCode, responseRequestId, message, error, buffer.readVarInt(), buffer.readVarInt(),
                regions, selectedTool, brushMode, selectedRegionId, brushRange);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(view.hasTarget());
        buffer.writeUtf(view.mapId(), MAX_TEXT);
        buffer.writeUtf(view.displayName(), MAX_TEXT);
        buffer.writeUtf(view.world(), MAX_TEXT);
        writeRegion(buffer, view.bounds());
        writeRegion(buffer, view.resetRegion());
        writeRegion(buffer, view.draftBounds());
        writeRegion(buffer, view.draftResetRegion());
        buffer.writeVarInt(Math.max(0, view.teamACount()));
        buffer.writeVarInt(Math.max(0, view.teamBCount()));
        buffer.writeVarInt(Math.max(0, view.spectatorCount()));
        buffer.writeUtf(view.snapshotStatus(), MAX_TEXT);
        buffer.writeBoolean(view.canEdit());
        buffer.writeBoolean(view.isAdmin());
        buffer.writeBoolean(view.locked());
        buffer.writeBoolean(view.draftInvalidated());
        writeStrings(buffer, view.ownedMaps());
        writeStrings(buffer, view.serverMaps());
        buffer.writeUtf(view.shareCode(), MAX_TEXT);
        buffer.writeVarInt(view.responseRequestId());
        buffer.writeUtf(view.message(), MAX_TEXT);
        buffer.writeBoolean(view.error());
        buffer.writeVarInt(view.teamCCount());
        buffer.writeVarInt(view.teamDCount());
        writeRegions(buffer, view.regions());
        buffer.writeUtf(view.selectedTool().id(), MAX_TEXT);
        buffer.writeUtf(view.brushMode().id(), MAX_TEXT);
        buffer.writeUtf(view.selectedRegionId(), MAX_TEXT);
        buffer.writeVarInt(view.brushRange());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.handle(this)));
        context.setPacketHandled(true);
    }

    public MapEditorView view() { return view; }

    private static void writeStrings(FriendlyByteBuf buffer, List<String> values) {
        int count = Math.min(MAX_MAPS, values == null ? 0 : values.size());
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buffer.writeUtf(values.get(i), MAX_TEXT);
        }
    }

    private static List<String> readStrings(FriendlyByteBuf buffer) {
        int count = Math.min(MAX_MAPS, Math.max(0, buffer.readVarInt()));
        List<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values.add(buffer.readUtf(MAX_TEXT));
        }
        return values;
    }

    private static void writeRegion(FriendlyByteBuf buffer, MapEditorView.RegionData region) {
        boolean present = region != null && region.present();
        buffer.writeBoolean(present);
        if (!present) return;
        buffer.writeInt(region.minX());
        buffer.writeInt(region.minY());
        buffer.writeInt(region.minZ());
        buffer.writeInt(region.maxX());
        buffer.writeInt(region.maxY());
        buffer.writeInt(region.maxZ());
    }

    private static MapEditorView.RegionData readRegion(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) return MapEditorView.RegionData.empty();
        return new MapEditorView.RegionData(true, buffer.readInt(), buffer.readInt(), buffer.readInt(),
                buffer.readInt(), buffer.readInt(), buffer.readInt());
    }

    private static void writeRegions(FriendlyByteBuf buffer, List<MapRegion> regions) {
        int count = Math.min(MAX_MAPS, regions == null ? 0 : regions.size());
        buffer.writeVarInt(count);
        for (int index = 0; index < count; index++) writeRegionDefinition(buffer, regions.get(index));
    }

    private static List<MapRegion> readRegions(FriendlyByteBuf buffer) {
        int count = Math.min(MAX_MAPS, Math.max(0, buffer.readVarInt()));
        List<MapRegion> regions = new ArrayList<>(count);
        for (int index = 0; index < count; index++) regions.add(readRegionDefinition(buffer));
        return regions;
    }

    private static void writeRegionDefinition(FriendlyByteBuf buffer, MapRegion region) {
        buffer.writeUtf(region.id(), MAX_TEXT);
        buffer.writeUtf(region.displayName(), MAX_TEXT);
        buffer.writeUtf(region.type().id(), MAX_TEXT);
        writeRegion(buffer, new MapEditorView.RegionData(true,
                region.region().min().getX(), region.region().min().getY(), region.region().min().getZ(),
                region.region().max().getX(), region.region().max().getY(), region.region().max().getZ()));
        buffer.writeBoolean(region.visibleInMatch());
        buffer.writeVarInt(region.displayRange());
        buffer.writeUtf(region.appearance().id(), MAX_TEXT);
        buffer.writeUtf(region.activation().id(), MAX_TEXT);
        buffer.writeUtf(region.activationValue(), MAX_TEXT);
        buffer.writeInt(region.color());
        buffer.writeBoolean(region.outline());
        buffer.writeBoolean(region.fill());
        buffer.writeVarInt(region.priority());
        buffer.writeUtf(region.notes(), MAX_TEXT);
    }

    private static MapRegion readRegionDefinition(FriendlyByteBuf buffer) {
        String id = buffer.readUtf(MAX_TEXT);
        String displayName = buffer.readUtf(MAX_TEXT);
        MapRegion.Type type = MapRegion.Type.parse(buffer.readUtf(MAX_TEXT));
        MapEditorView.RegionData data = readRegion(buffer);
        MapDefinition.Region region = new MapDefinition.Region(
                new BlockPos(data.minX(), data.minY(), data.minZ()),
                new BlockPos(data.maxX(), data.maxY(), data.maxZ()));
        return new MapRegion(id, displayName, type, region, buffer.readBoolean(), buffer.readVarInt(),
                MapRegion.Appearance.parse(buffer.readUtf(MAX_TEXT)),
                MapRegion.Activation.parse(buffer.readUtf(MAX_TEXT)),
                buffer.readUtf(MAX_TEXT), buffer.readInt(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readVarInt(), buffer.readUtf(MAX_TEXT));
    }
}
