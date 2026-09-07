package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.map.MapEditorView;
import cn.blockforge.generated.generatedmod.network.packet.MapEditorSyncPacket;

/** 地图规划界面的客户端缓存。 */
public final class ClientMapEditorData {
    private static MapEditorView view = empty();
    private static int revision;

    private ClientMapEditorData() {
    }

    public static void apply(MapEditorSyncPacket packet) {
        view = packet.view();
        revision++;
    }

    public static void clear() {
        view = empty();
        revision++;
    }

    public static MapEditorView view() {
        return view;
    }

    public static int revision() {
        return revision;
    }

    private static MapEditorView empty() {
        return new MapEditorView(false, "", "", "", MapEditorView.RegionData.empty(),
                MapEditorView.RegionData.empty(), MapEditorView.RegionData.empty(),
                MapEditorView.RegionData.empty(), 0, 0, 0, "", false, false, false,
                false, java.util.List.of(), java.util.List.of(), "", 0, "", false, 0, 0);
    }
}
