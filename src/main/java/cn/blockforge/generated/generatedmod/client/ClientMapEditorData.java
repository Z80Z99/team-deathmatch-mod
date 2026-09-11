package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.map.MapEditorView;
import cn.blockforge.generated.generatedmod.network.packet.MapEditorSyncPacket;

/** 地图规划界面的客户端缓存。 */
public final class ClientMapEditorData {
    private static MapEditorView view = empty();
    private static int revision;
    private static int contentRevision;
    private static net.minecraft.core.BlockPos firstPoint;
    private static net.minecraft.core.BlockPos secondPoint;
    public static net.minecraft.core.BlockPos firstPoint() { return firstPoint; }
    public static net.minecraft.core.BlockPos secondPoint() { return secondPoint; }

    private ClientMapEditorData() {
    }

    public static void apply(MapEditorSyncPacket packet) {
        BrushTransitions.acknowledge(view, packet.view());
        if (plannerContentChanged(view, packet.view())) {
            contentRevision++;
        }
        view = packet.view();
        firstPoint = packet.firstPoint();
        secondPoint = packet.secondPoint();
        revision++;
    }

    public static void clear() {
        BrushTransitions.clear();
        view = empty();
        firstPoint = null;
        secondPoint = null;
        revision++;
        contentRevision++;
    }

    public static MapEditorView view() {
        return view;
    }

    public static int revision() {
        return revision;
    }

    /** 只在地图目标或区域列表变化时递增，避免每秒轮询重建整个规划器。 */
    public static int contentRevision() {
        return contentRevision;
    }

    private static boolean plannerContentChanged(MapEditorView before, MapEditorView after) {
        return before.hasTarget() != after.hasTarget()
                || !before.mapId().equals(after.mapId())
                || !before.regions().equals(after.regions());
    }

    private static MapEditorView empty() {
        return new MapEditorView(false, "", "", "", MapEditorView.RegionData.empty(),
                MapEditorView.RegionData.empty(), MapEditorView.RegionData.empty(),
                MapEditorView.RegionData.empty(), 0, 0, 0, "", false, false, false,
                false, java.util.List.of(), java.util.List.of(), "", 0, "", false, 0, 0,
                java.util.List.of(), cn.blockforge.generated.generatedmod.map.MapTool.BOUNDS,
                cn.blockforge.generated.generatedmod.map.MapBrushMode.REGION, "bounds", 2);
    }
}
