package cn.blockforge.generated.generatedmod.map;

import cn.blockforge.generated.generatedmod.config.FpsTdmConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** 在 Dedicated Server 主线程上按 tick 捕获和恢复地图方块。 */
public final class MapResetManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("generated_mod_map_reset");

    private final MinecraftServer server;
    private final Path snapshotFile;
    private MapDefinition definition;
    private MapSnapshot snapshot;
    private Operation operation = Operation.IDLE;
    private RestorePhase restorePhase = RestorePhase.NONE;
    private Cursor cursor;
    private MapSnapshot.Builder captureBuilder;
    private int restoreIndex;
    private long processedBlocks;
    private int errorCount;
    private boolean resetRequested;
    private boolean restoreComplete;
    private boolean restoreFailed;
    private String lastError = "";
    private CompletableFuture<Void> saveTask;

    public MapResetManager(MinecraftServer server, MapDefinition definition, Path snapshotFile) {
        this.server = server;
        this.definition = definition;
        this.snapshotFile = snapshotFile;
        loadPersistedSnapshot();
    }

    /** 开始一次分 tick 初始快照捕获。 */
    public boolean capture() {
        if (operation != Operation.IDLE) {
            return false;
        }
        ServerLevel level = resetLevel();
        if (level == null) {
            failCapture("地图维度未加载：" + definition.world().location());
            return false;
        }
        long volume = definition.resetRegion().volume();
        int maximum = FpsTdmConfig.COMMON.maxSnapshotBlocks.get();
        if (volume <= 0L || volume > maximum || volume > Integer.MAX_VALUE) {
            failCapture("resetRegion 方块数 " + volume + " 超出 maxSnapshotBlocks=" + maximum);
            return false;
        }
        snapshot = null;
        resetRequested = false;
        restoreComplete = false;
        restoreFailed = false;
        lastError = "";
        errorCount = 0;
        processedBlocks = 0L;
        captureBuilder = new MapSnapshot.Builder((int) volume);
        cursor = new Cursor(definition.resetRegion());
        operation = Operation.CAPTURING;
        LOGGER.info("开始捕获地图 {} 的初始快照，共 {} 个方块，分批上限 {} / tick",
                definition.id(), volume, FpsTdmConfig.COMMON.maxBlocksPerTick.get());
        return true;
    }

    /** 开始一次分 tick 地图恢复。 */
    public boolean reset() {
        if (operation == Operation.RESTORING) {
            return true;
        }
        if (operation != Operation.IDLE || snapshot == null) {
            return false;
        }
        ServerLevel level = resetLevel();
        if (level == null) {
            lastError = "地图维度未加载：" + definition.world().location();
            resetRequested = true;
            restoreFailed = true;
            restoreComplete = false;
            return false;
        }
        BoundingBox resetBox = resetBox();
        level.clearBlockEvents(resetBox);
        level.getBlockTicks().clearArea(resetBox);
        level.getFluidTicks().clearArea(resetBox);
        restoreIndex = 0;
        processedBlocks = 0L;
        errorCount = 0;
        lastError = "";
        resetRequested = true;
        restoreComplete = false;
        restoreFailed = false;
        restorePhase = RestorePhase.BLOCKS;
        operation = Operation.RESTORING;
        LOGGER.info("开始恢复地图 {}，共 {} 个方块，分批上限 {} / tick",
                definition.id(), snapshot.size(), FpsTdmConfig.COMMON.maxBlocksPerTick.get());
        return true;
    }

    /** 与需求中的 restore 名称对应，执行同一套恢复流程。 */
    public boolean restore() {
        return reset();
    }

    public void tick() {
        if (operation == Operation.CAPTURING) {
            tickCapture();
        } else if (operation == Operation.RESTORING) {
            tickRestore();
        }
    }

    public boolean isResetComplete() {
        return resetRequested && restoreComplete && operation == Operation.IDLE;
    }

    public boolean isResetFailed() {
        return resetRequested && restoreFailed && operation == Operation.IDLE;
    }

    public boolean isReady() {
        return snapshot != null && operation == Operation.IDLE && !restoreFailed;
    }

    public boolean isCapturing() {
        return operation == Operation.CAPTURING;
    }

    public boolean isResetting() {
        return operation == Operation.RESTORING;
    }

    public long totalBlocks() {
        return definition.resetRegion().volume();
    }

    public long processedBlocks() {
        return processedBlocks;
    }

    public int errorCount() {
        return errorCount;
    }

    public String lastError() {
        return lastError;
    }

    public MapDefinition definition() {
        return definition;
    }

    public MapSnapshot snapshot() {
        return snapshot;
    }

    public void updateDefinition(MapDefinition updated) {
        boolean regionChanged = !definition.world().equals(updated.world())
                || !definition.resetRegion().equals(updated.resetRegion());
        if (regionChanged && operation == Operation.CAPTURING) {
            abortCapture();
        }
        if (operation == Operation.RESTORING) {
            return;
        }
        this.definition = updated;
        if (regionChanged) {
            waitForSnapshotSave();
            snapshot = null;
            resetRequested = false;
            restoreComplete = false;
            restoreFailed = false;
            loadPersistedSnapshot();
        }
    }

    /** 仅用于等待阶段切换地图时放弃尚未完成的捕获。 */
    public void abortCapture() {
        if (operation == Operation.CAPTURING) {
            operation = Operation.IDLE;
            cursor = null;
            captureBuilder = null;
            snapshot = null;
            resetRequested = false;
            restoreComplete = false;
            restoreFailed = false;
        }
    }

    public void close() {
        abortCapture();
        waitForSnapshotSave();
    }

    private void tickCapture() {
        ServerLevel level = resetLevel();
        if (level == null || cursor == null || captureBuilder == null) {
            failCapture("捕获期间地图维度不可用");
            return;
        }
        int budget = Math.max(1, FpsTdmConfig.COMMON.maxBlocksPerTick.get());
        for (int i = 0; i < budget && cursor != null; i++) {
            BlockPos pos = cursor.position();
            try {
                LevelChunk chunk = level.getChunkAt(pos);
                BlockState state = chunk.getBlockState(pos);
                BlockEntity blockEntity = chunk.getBlockEntity(pos);
                captureBuilder.add(state, blockEntity == null ? null : blockEntity.saveWithFullMetadata());
            } catch (Throwable error) {
                failCapture("捕获方块 " + pos + " 失败：" + describe(error));
                return;
            }
            processedBlocks++;
            if (!cursor.advance()) {
                try {
                    snapshot = captureBuilder.build();
                } catch (Throwable error) {
                    failCapture("完成快照失败：" + describe(error));
                    return;
                }
                captureBuilder = null;
                cursor = null;
                operation = Operation.IDLE;
                LOGGER.info("地图 {} 初始快照完成：{} 个方块，{} 个读取错误",
                        definition.id(), snapshot.size(), errorCount);
                if (errorCount > 0) {
                    LOGGER.warn("地图 {} 的快照存在 {} 个读取错误，恢复时可能有缺口", definition.id(), errorCount);
                }
                saveSnapshotAsync();
                break;
            }
        }
    }

    private void tickRestore() {
        ServerLevel level = resetLevel();
        if (level == null || snapshot == null) {
            lastError = "恢复期间地图维度或快照不可用";
            finishRestore(true);
            return;
        }
        int budget = Math.max(1, FpsTdmConfig.COMMON.maxBlocksPerTick.get());
        if (restorePhase == RestorePhase.BLOCKS) {
            for (int i = 0; i < budget && restoreIndex < snapshot.size(); i++, restoreIndex++) {
                BlockPos pos = positionForIndex(restoreIndex);
                try {
                    level.removeBlockEntity(pos);
                    level.setBlock(pos, snapshot.stateAt(restoreIndex), Block.UPDATE_ALL);
                } catch (Throwable error) {
                    recordRestoreError(pos, error);
                }
                processedBlocks++;
            }
            if (restoreIndex >= snapshot.size()) {
                restoreIndex = 0;
                restorePhase = RestorePhase.BLOCK_ENTITIES;
            }
        }
        if (restorePhase == RestorePhase.BLOCK_ENTITIES) {
            var blockEntities = snapshot.blockEntities();
            for (int i = 0; i < budget && restoreIndex < blockEntities.size(); i++, restoreIndex++) {
                MapSnapshot.BlockEntitySnapshot entry = blockEntities.get(restoreIndex);
                BlockPos pos = positionForIndex(entry.index());
                try {
                    BlockState state = snapshot.stateAt(entry.index());
                    BlockEntity restored = BlockEntity.loadStatic(pos, state, entry.nbt().copy());
                    if (restored != null) {
                        level.setBlockEntity(restored);
                        restored.setChanged();
                    }
                } catch (Throwable error) {
                    recordRestoreError(pos, error);
                }
            }
            if (restoreIndex >= blockEntities.size()) {
                finishRestore(false);
            }
        }
    }

    private BlockPos positionForIndex(int index) {
        MapDefinition.Region region = definition.resetRegion();
        int xSpan = region.max().getX() - region.min().getX() + 1;
        int zSpan = region.max().getZ() - region.min().getZ() + 1;
        int layerSize = xSpan * zSpan;
        int yOffset = index / layerSize;
        int withinLayer = index % layerSize;
        int zOffset = withinLayer / xSpan;
        int xOffset = withinLayer % xSpan;
        return new BlockPos(region.min().getX() + xOffset, region.min().getY() + yOffset,
                region.min().getZ() + zOffset);
    }

    private void recordRestoreError(BlockPos pos, Throwable error) {
        errorCount++;
        lastError = error.getClass().getSimpleName() + ": " + error.getMessage();
        LOGGER.warn("恢复地图 {} 的方块 {} 失败", definition.id(), pos, error);
    }

    private void finishRestore(boolean failed) {
        operation = Operation.IDLE;
        restorePhase = RestorePhase.NONE;
        restoreIndex = 0;
        restoreFailed = failed;
        restoreComplete = !failed;
        LOGGER.info("地图 {} 恢复完成，{} 个方块，{} 个恢复错误",
                definition.id(), processedBlocks, errorCount);
        if (failed) {
            LOGGER.error("地图 {} 恢复未完成：{}", definition.id(), lastError);
        } else if (errorCount > 0) {
            LOGGER.warn("地图 {} 恢复存在 {} 个错误：{}", definition.id(), errorCount, lastError);
        }
    }

    private void loadPersistedSnapshot() {
        if (!Files.isRegularFile(snapshotFile)) {
            return;
        }
        try {
            CompoundTag root = NbtIo.readCompressed(snapshotFile.toFile());
            snapshot = MapSnapshot.fromNbt(root, definition,
                    server.registryAccess().lookupOrThrow(Registries.BLOCK));
            lastError = "";
            LOGGER.info("已加载地图 {} 的持久化初始快照：{} 个方块", definition.id(), snapshot.size());
        } catch (Exception error) {
            snapshot = null;
            lastError = "持久化快照不可用：" + error.getMessage();
            LOGGER.warn("读取地图 {} 的持久化快照失败，将重新捕获：{}",
                    definition.id(), snapshotFile, error);
        }
    }

    private void saveSnapshotAsync() {
        MapSnapshot captured = snapshot;
        MapDefinition capturedDefinition = definition;
        if (captured == null) {
            return;
        }
        waitForSnapshotSave();
        saveTask = CompletableFuture.runAsync(() -> {
            Path temporary = snapshotFile.resolveSibling(snapshotFile.getFileName() + ".tmp");
            try {
                Files.createDirectories(snapshotFile.getParent());
                NbtIo.writeCompressed(captured.toNbt(capturedDefinition), temporary.toFile());
                try {
                    Files.move(temporary, snapshotFile, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, snapshotFile, StandardCopyOption.REPLACE_EXISTING);
                }
                LOGGER.info("已保存地图 {} 的持久化初始快照：{}", capturedDefinition.id(), snapshotFile);
            } catch (Exception error) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // 保留原始写入错误。
                }
                throw new CompletionException(error);
            }
        });
    }

    private void waitForSnapshotSave() {
        if (saveTask == null) {
            return;
        }
        try {
            saveTask.join();
        } catch (CompletionException error) {
            LOGGER.error("保存地图 {} 的持久化快照失败：{}", definition.id(), snapshotFile,
                    error.getCause() == null ? error : error.getCause());
        } finally {
            saveTask = null;
        }
    }

    private void failCapture(String message) {
        operation = Operation.IDLE;
        snapshot = null;
        cursor = null;
        captureBuilder = null;
        resetRequested = false;
        restoreComplete = false;
        restoreFailed = false;
        lastError = message;
        LOGGER.error("地图 {} 初始快照不可用：{}", definition.id(), message);
    }

    private BoundingBox resetBox() {
        MapDefinition.Region region = definition.resetRegion();
        return new BoundingBox(
                region.min().getX(), region.min().getY(), region.min().getZ(),
                region.max().getX(), region.max().getY(), region.max().getZ());
    }

    private static String describe(Throwable error) {
        String message = error.getMessage();
        return error.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private ServerLevel resetLevel() {
        return server.getLevel(definition.world());
    }

    private enum Operation {
        IDLE,
        CAPTURING,
        RESTORING
    }

    private enum RestorePhase {
        NONE,
        BLOCKS,
        BLOCK_ENTITIES
    }

    private static final class Cursor {
        private final MapDefinition.Region region;
        private int x;
        private int y;
        private int z;

        private Cursor(MapDefinition.Region region) {
            this.region = region;
            this.x = region.min().getX();
            this.y = region.min().getY();
            this.z = region.min().getZ();
        }

        private BlockPos position() {
            return new BlockPos(x, y, z);
        }

        private boolean advance() {
            if (x < region.max().getX()) {
                x++;
                return true;
            }
            x = region.min().getX();
            if (z < region.max().getZ()) {
                z++;
                return true;
            }
            z = region.min().getZ();
            if (y < region.max().getY()) {
                y++;
                return true;
            }
            return false;
        }
    }
}
