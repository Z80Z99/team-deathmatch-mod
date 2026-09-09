package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.map.MapEditorView;
import cn.blockforge.generated.generatedmod.map.MapBrushMode;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.List;

public final class BrushTransitions {
    private record Change(String map, String region, BlockPos pos, boolean add, long time) { }
    private static final List<Change> PENDING = new ArrayList<>();
    private static final List<Change> ACTIVE = new ArrayList<>();
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.BufferBuilder(2048));
    private BrushTransitions() { }
    public static void clear() { PENDING.clear(); ACTIVE.clear(); }
    public static void request(MapEditorView view, BlockPos pos, boolean add) {
        if (view.brushMode() != MapBrushMode.BLOCK || view.selectedRegionId().isBlank()) return;
        if (PENDING.size() >= 32) PENDING.remove(0);
        PENDING.add(new Change(view.mapId(), view.selectedRegionId(), pos.immutable(), add, System.nanoTime()));
    }
    public static void acknowledge(MapEditorView old, MapEditorView next) {
        long now = System.nanoTime();
        if (!old.mapId().equals(next.mapId())) { clear(); return; }
        PENDING.removeIf(change -> {
            var before = old.regions().stream().filter(r -> r.id().equals(change.region())).findFirst();
            var after = next.regions().stream().filter(r -> r.id().equals(change.region())).findFirst();
            if (!next.error() && before.isPresent()
                    && before.get().region().contains(change.pos()) != change.add()
                    && after.map(r -> r.region().contains(change.pos())).orElse(false) == change.add()) {
                if (ACTIVE.size() >= 32) ACTIVE.remove(0);
                ACTIVE.add(new Change(change.map(), change.region(), change.pos(), change.add(), now));
                return true;
            }
            return now - change.time() > 2_000_000_000L || next.error();
        });
    }
    public static void render(PoseStack pose) {
        long now = System.nanoTime();
        ACTIVE.removeIf(change -> now - change.time() > 450_000_000L);
        for (var change : ACTIVE) {
            float alpha = .4F * (1 - (now - change.time()) / 450_000_000F);
            BlockPos pos = change.pos();
            LevelRenderer.addChainedFilledBoxVertices(pose, BUFFERS.getBuffer(RenderType.debugFilledBox()),
                    pos.getX() - .002, pos.getY() - .002, pos.getZ() - .002,
                    pos.getX() + 1.002, pos.getY() + 1.002, pos.getZ() + 1.002,
                    change.add() ? .15F : 1F, change.add() ? .9F : .15F, change.add() ? .9F : .15F, alpha);
            BUFFERS.endBatch();
        }
    }
}
