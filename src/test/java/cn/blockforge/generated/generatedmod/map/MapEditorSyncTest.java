package cn.blockforge.generated.generatedmod.map;

import cn.blockforge.generated.generatedmod.MinecraftTestBootstrap;
import cn.blockforge.generated.generatedmod.client.ClientMapEditorData;
import cn.blockforge.generated.generatedmod.network.packet.MapEditorSyncPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MapEditorSyncTest {
    @BeforeAll static void bootstrap() { MinecraftTestBootstrap.initialize(); }
    @BeforeEach void resetClientCache() { ClientMapEditorData.clear(); }

    @Test void populatedViewAndSelectionRoundTripWithoutFieldShift() {
        verify(3, 7, 48, new BlockPos(-8, 30, 4), new BlockPos(12, 60, 9));
    }

    @Test void zeroTeamsAndSingleCornerRoundTrip() {
        verify(0, 0, 2, new BlockPos(1, 2, 3), null);
    }

    @Test void completedSelectionClearsBothCorners() {
        verify(1, 2, 64, null, null);
    }

    @Test void pollMetadataDoesNotInvalidatePlannerContentRevision() {
        ClientMapEditorData.apply(new MapEditorSyncPacket(view(1, "旧消息", List.of())));
        int contentRevision = ClientMapEditorData.contentRevision();

        ClientMapEditorData.apply(new MapEditorSyncPacket(view(2, "新消息", List.of())));
        assertEquals(contentRevision, ClientMapEditorData.contentRevision());

        var region = MapRegion.custom("custom", "自定义区域", MapRegion.Type.CUSTOM,
                new MapDefinition.Region(BlockPos.ZERO, new BlockPos(2, 2, 2)));
        ClientMapEditorData.apply(new MapEditorSyncPacket(view(3, "新消息", List.of(region))));
        assertEquals(contentRevision + 1, ClientMapEditorData.contentRevision());
    }

    private void verify(int c, int d, int range, BlockPos first, BlockPos second) {
        var bounds = new MapDefinition.Region(new BlockPos(-10, 0, -10), new BlockPos(20, 80, 20));
        var region = MapRegion.custom("zone", "zone", MapRegion.Type.CUSTOM, bounds.withBlock(new BlockPos(1, 2, 3), false));
        var data = new MapEditorView.RegionData(true, -10, 0, -10, 20, 80, 20);
        var view = new MapEditorView(true, "map", "example", "minecraft:overworld",
                data, data, data, data, 1, 2, 1, "ready", true, true, false, false,
                List.of("map|example|123456"), List.of(), "123456", 42, "saved", false, c, d,
                List.of(region), MapTool.CUSTOM, MapBrushMode.BLOCK, "zone", range);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new MapEditorSyncPacket(view, first, second).encode(buffer);
            var decoded = new MapEditorSyncPacket(buffer);
            assertEquals(view, decoded.view());
            assertEquals(first, decoded.firstPoint());
            assertEquals(second, decoded.secondPoint());
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }

    private static MapEditorView view(int requestId, String message, List<MapRegion> regions) {
        var bounds = new MapDefinition.Region(new BlockPos(-10, 0, -10), new BlockPos(20, 80, 20));
        var data = new MapEditorView.RegionData(true, -10, 0, -10, 20, 80, 20);
        return new MapEditorView(true, "map", "example", "minecraft:overworld",
                data, data, data, data, 1, 2, 1, "ready", true, true, false, false,
                List.of(), List.of(), "123456", requestId, message, false, 3, 4,
                regions, MapTool.CUSTOM, MapBrushMode.BLOCK, "", rangeOrDefault(regions));
    }

    private static int rangeOrDefault(List<MapRegion> regions) {
        return regions.isEmpty() ? 2 : 48;
    }
}
