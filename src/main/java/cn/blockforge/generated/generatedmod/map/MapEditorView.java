package cn.blockforge.generated.generatedmod.map;

import java.util.List;

/**
 * 地图编辑界面的服务器权威状态，每个玩家收到的视图互不相同：
 * 只包含自己的编辑目标、自己拥有的地图清单（含邀请码）以及管理员可见的服务器地图。
 */
public record MapEditorView(
        boolean hasTarget,
        String mapId,
        String displayName,
        String world,
        RegionData bounds,
        RegionData resetRegion,
        RegionData draftBounds,
        RegionData draftResetRegion,
        int teamACount,
        int teamBCount,
        int spectatorCount,
        String snapshotStatus,
        boolean canEdit,
        boolean isAdmin,
        boolean locked,
        boolean draftInvalidated,
        List<String> ownedMaps,
        List<String> serverMaps,
        String shareCode,
        int responseRequestId,
        String message,
        boolean error, int teamCCount, int teamDCount) {
    public MapEditorView {
        ownedMaps = List.copyOf(ownedMaps == null ? List.of() : ownedMaps);
        serverMaps = List.copyOf(serverMaps == null ? List.of() : serverMaps);
        responseRequestId = Math.max(0, responseRequestId);
        message = message == null ? "" : message;
        shareCode = shareCode == null ? "" : shareCode;
    }

    /** 兼容旧调用点的地图 id 列表：即自己拥有的地图。 */
    public List<String> mapIds() {
        return ownedMaps;
    }

    public record RegionData(boolean present, int minX, int minY, int minZ,
                             int maxX, int maxY, int maxZ) {
        public static RegionData empty() {
            return new RegionData(false, 0, 0, 0, 0, 0, 0);
        }
    }
}
