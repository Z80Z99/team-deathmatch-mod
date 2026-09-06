package cn.blockforge.generated.generatedmod.map;

import cn.blockforge.generated.generatedmod.config.FpsTdmConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** resetRegion 的紧凑快照：状态调色板、连续索引和稀疏方块实体 NBT。 */
public final class MapSnapshot {
    private static final int FORMAT_VERSION = 1;

    private final List<BlockState> palette;
    private final int[] stateIndices;
    private final List<BlockEntitySnapshot> blockEntities;

    private MapSnapshot(List<BlockState> palette, int[] stateIndices,
                        List<BlockEntitySnapshot> blockEntities) {
        this.palette = List.copyOf(palette);
        this.stateIndices = stateIndices;
        this.blockEntities = List.copyOf(blockEntities);
    }

    public int size() {
        return stateIndices.length;
    }

    public BlockState stateAt(int index) {
        return palette.get(stateIndices[index]);
    }

    public List<BlockEntitySnapshot> blockEntities() {
        return blockEntities;
    }

    public CompoundTag toNbt(MapDefinition definition) {
        CompoundTag root = new CompoundTag();
        root.putInt("Format", FORMAT_VERSION);
        root.putString("MapId", definition.id());
        root.putString("World", definition.world().location().toString());
        root.put("Min", NbtUtils.writeBlockPos(definition.resetRegion().min()));
        root.put("Max", NbtUtils.writeBlockPos(definition.resetRegion().max()));

        ListTag paletteTag = new ListTag();
        for (BlockState state : palette) {
            paletteTag.add(NbtUtils.writeBlockState(state));
        }
        root.put("Palette", paletteTag);
        root.putIntArray("States", stateIndices);

        ListTag blockEntitiesTag = new ListTag();
        for (BlockEntitySnapshot blockEntity : blockEntities) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Index", blockEntity.index());
            entry.put("Data", blockEntity.nbt().copy());
            blockEntitiesTag.add(entry);
        }
        root.put("BlockEntities", blockEntitiesTag);
        return root;
    }

    public static MapSnapshot fromNbt(CompoundTag root, MapDefinition definition,
                                      HolderGetter<Block> blockLookup) {
        if (root.getInt("Format") != FORMAT_VERSION) {
            throw new IllegalArgumentException("不支持的快照格式版本：" + root.getInt("Format"));
        }
        if (!definition.id().equals(root.getString("MapId"))) {
            throw new IllegalArgumentException("快照地图 id 不匹配");
        }
        if (!definition.world().location().toString().equals(root.getString("World"))) {
            throw new IllegalArgumentException("快照维度不匹配");
        }
        BlockPos min = NbtUtils.readBlockPos(root.getCompound("Min"));
        BlockPos max = NbtUtils.readBlockPos(root.getCompound("Max"));
        if (!definition.resetRegion().min().equals(min) || !definition.resetRegion().max().equals(max)) {
            throw new IllegalArgumentException("快照 resetRegion 不匹配");
        }

        long expectedVolume = definition.resetRegion().volume();
        int maximum = FpsTdmConfig.COMMON.maxSnapshotBlocks.get();
        if (expectedVolume <= 0L || expectedVolume > maximum || expectedVolume > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("快照区域超出 maxSnapshotBlocks=" + maximum + "：" + expectedVolume);
        }

        ListTag paletteTag = root.getList("Palette", Tag.TAG_COMPOUND);
        if (paletteTag.isEmpty()) {
            throw new IllegalArgumentException("快照状态调色板为空");
        }
        List<BlockState> palette = new ArrayList<>(paletteTag.size());
        for (int i = 0; i < paletteTag.size(); i++) {
            palette.add(NbtUtils.readBlockState(blockLookup, paletteTag.getCompound(i)));
        }

        int[] states = root.getIntArray("States");
        if (states.length != (int) expectedVolume) {
            throw new IllegalArgumentException("快照方块数量不匹配：" + states.length + "/" + expectedVolume);
        }
        for (int paletteIndex : states) {
            if (paletteIndex < 0 || paletteIndex >= palette.size()) {
                throw new IllegalArgumentException("快照包含无效状态索引：" + paletteIndex);
            }
        }

        ListTag blockEntitiesTag = root.getList("BlockEntities", Tag.TAG_COMPOUND);
        List<BlockEntitySnapshot> blockEntities = new ArrayList<>(blockEntitiesTag.size());
        for (int i = 0; i < blockEntitiesTag.size(); i++) {
            CompoundTag entry = blockEntitiesTag.getCompound(i);
            int index = entry.getInt("Index");
            if (index < 0 || index >= states.length || !entry.contains("Data", Tag.TAG_COMPOUND)) {
                throw new IllegalArgumentException("快照包含无效方块实体索引：" + index);
            }
            blockEntities.add(new BlockEntitySnapshot(index, entry.getCompound("Data")));
        }
        return new MapSnapshot(palette, states, blockEntities);
    }

    public static final class Builder {
        private final int[] stateIndices;
        private final List<BlockState> palette = new ArrayList<>();
        private final Map<BlockState, Integer> paletteLookup = new HashMap<>();
        private final Map<Integer, CompoundTag> blockEntities = new HashMap<>();
        private int index;

        public Builder(int expectedSize) {
            this.stateIndices = new int[Math.max(0, expectedSize)];
        }

        public void add(BlockState state, CompoundTag blockEntityNbt) {
            if (index >= stateIndices.length) {
                throw new IllegalStateException("快照写入超出 resetRegion 大小");
            }
            Integer paletteIndex = paletteLookup.get(state);
            if (paletteIndex == null) {
                paletteIndex = palette.size();
                palette.add(state);
                paletteLookup.put(state, paletteIndex);
            }
            stateIndices[index] = paletteIndex;
            if (blockEntityNbt != null) {
                blockEntities.put(index, blockEntityNbt.copy());
            }
            index++;
        }

        public MapSnapshot build() {
            if (index != stateIndices.length) {
                throw new IllegalStateException("快照尚未完整捕获：" + index + "/" + stateIndices.length);
            }
            List<BlockEntitySnapshot> entries = blockEntities.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> new BlockEntitySnapshot(entry.getKey(), entry.getValue()))
                    .toList();
            return new MapSnapshot(palette, stateIndices, entries);
        }
    }

    public record BlockEntitySnapshot(int index, CompoundTag nbt) {
        public BlockEntitySnapshot {
            nbt = nbt.copy();
        }
    }
}
