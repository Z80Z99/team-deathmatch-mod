package cn.blockforge.generated.generatedmod.client;

import net.minecraft.core.BlockPos;

/** 客户端侧的准星拾取与道具状态缓存。 */
public final class MapToolClientState {
    private static String hoveredRegionId = "";
    private static BlockPos brushTarget = BlockPos.ZERO;
    private static boolean brushTargetAir;

    private MapToolClientState() {
    }

    public static void setHoveredRegion(String id) {
        hoveredRegionId = id == null ? "" : id;
    }

    public static String hoveredRegionId() {
        return hoveredRegionId;
    }

    public static void setBrushTarget(BlockPos pos, boolean air) {
        brushTarget = pos == null ? BlockPos.ZERO : pos.immutable();
        brushTargetAir = air;
    }

    public static BlockPos brushTarget() {
        return brushTarget;
    }

    public static boolean brushTargetAir() {
        return brushTargetAir;
    }
}
