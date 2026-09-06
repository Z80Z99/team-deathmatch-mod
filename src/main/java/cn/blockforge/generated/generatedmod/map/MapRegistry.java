package cn.blockforge.generated.generatedmod.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/** 负责从 config/fpsmod/maps 读取和写回地图定义。 */
public final class MapRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("generated_mod_maps");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path directory;
    private final Path snapshotDirectory;
    private final Map<String, MapDefinition> definitions = new LinkedHashMap<>();

    public MapRegistry(MinecraftServer server) {
        this.directory = server.getServerDirectory().toPath()
                .resolve("config").resolve("fpsmod").resolve("maps");
        this.snapshotDirectory = directory.resolve(".snapshots");
    }

    public int reload() {
        Map<String, MapDefinition> loaded = new LinkedHashMap<>();
        try {
            Files.createDirectories(directory);
            List<Path> files;
            try (Stream<Path> stream = Files.list(directory)) {
                files = stream.filter(Files::isRegularFile)
                        .filter(path -> !path.getFileName().toString().startsWith("."))
                        .filter(path -> path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".json"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .toList();
            }
            for (Path file : files) {
                try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonElement element = JsonParser.parseReader(reader);
                    if (!element.isJsonObject()) {
                        throw new IllegalArgumentException("根元素必须是 JSON 对象");
                    }
                    String fallbackId = file.getFileName().toString();
                    fallbackId = fallbackId.substring(0, fallbackId.length() - ".json".length());
                    MapDefinition definition = MapDefinition.fromJson(element.getAsJsonObject(), fallbackId);
                    if (loaded.put(definition.id(), definition) != null) {
                        LOGGER.warn("重复的地图 id={}，后读取的文件覆盖了前一个定义", definition.id());
                    }
                } catch (Exception error) {
                    LOGGER.warn("读取地图配置失败：{}", file, error);
                }
            }
            definitions.clear();
            definitions.putAll(loaded);
            LOGGER.info("已加载 {} 张地图配置，目录：{}", definitions.size(), directory);
            return definitions.size();
        } catch (IOException error) {
            LOGGER.error("创建或读取地图配置目录失败：{}", directory, error);
            return 0;
        }
    }

    public Optional<MapDefinition> get(String id) {
        return Optional.ofNullable(definitions.get(MapDefinition.normalizeId(id)));
    }

    public List<MapDefinition> definitions() {
        return List.copyOf(definitions.values());
    }

    public List<String> ids() {
        return new ArrayList<>(definitions.keySet());
    }

    public Path directory() {
        return directory;
    }

    public Path snapshotFile(String mapId) {
        return snapshotDirectory.resolve(MapDefinition.normalizeId(mapId) + ".nbt");
    }

    public boolean definitionFileExists(String mapId) {
        return Files.isRegularFile(directory.resolve(MapDefinition.normalizeId(mapId) + ".json"));
    }

    public boolean save(MapDefinition definition) {
        Path file = directory.resolve(definition.id() + ".json");
        Path temporary = directory.resolve(definition.id() + ".json.tmp");
        try {
            Files.createDirectories(directory);
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(definition.toJson(), writer);
            }
            moveReplacing(temporary, file);
            definitions.put(definition.id(), definition);
            return true;
        } catch (IOException error) {
            deleteQuietly(temporary);
            LOGGER.error("写入地图配置失败：{}", definition.id(), error);
            return false;
        }
    }

    /** 创建新地图定义，不覆盖已有同名配置。 */
    public CreateResult create(MapDefinition definition) {
        if (definition == null) {
            return CreateResult.INVALID;
        }
        Path file = directory.resolve(definition.id() + ".json");
        Path temporary = directory.resolve(definition.id() + ".json.tmp-" + Long.toUnsignedString(System.nanoTime()));
        try {
            Files.createDirectories(directory);
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                GSON.toJson(definition.toJson(), writer);
            }
            try {
                moveNew(temporary, file);
            } catch (FileAlreadyExistsException error) {
                deleteQuietly(temporary);
                return CreateResult.ALREADY_EXISTS;
            }
            definitions.put(definition.id(), definition);
            return CreateResult.CREATED;
        } catch (FileAlreadyExistsException error) {
            deleteQuietly(temporary);
            return CreateResult.ALREADY_EXISTS;
        } catch (IOException error) {
            deleteQuietly(temporary);
            LOGGER.error("创建地图配置失败：{}", definition.id(), error);
            return CreateResult.IO_ERROR;
        }
    }

    /** 删除地图快照；配置重新创建时必须从当前世界重新捕获。 */
    public boolean deleteSnapshot(String mapId) {
        Path file = snapshotFile(mapId);
        try {
            return !Files.exists(file) || Files.deleteIfExists(file);
        } catch (IOException error) {
            LOGGER.warn("删除地图 {} 的旧快照失败：{}", mapId, file, error);
            return false;
        }
    }

    /** 该地图是否已有可用的持久快照。 */
    public boolean hasSnapshot(String mapId) {
        return Files.isRegularFile(snapshotFile(mapId));
    }

    /** 删除地图定义文件和快照文件，用于玩家删除自己的地图。 */
    public boolean delete(String mapId) {
        String normalized = MapDefinition.normalizeId(mapId);
        boolean ok = true;
        try {
            Path definition = directory.resolve(normalized + ".json");
            ok &= !Files.exists(definition) || Files.deleteIfExists(definition);
        } catch (IOException error) {
            LOGGER.warn("删除地图定义失败：{}", normalized, error);
            ok = false;
        }
        try {
            Path snapshot = snapshotFile(normalized);
            ok &= !Files.exists(snapshot) || Files.deleteIfExists(snapshot);
        } catch (IOException error) {
            LOGGER.warn("删除地图快照失败：{}", normalized, error);
            ok = false;
        }
        definitions.remove(normalized);
        return ok;
    }

    private static void moveReplacing(Path temporary, Path file) throws IOException {
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void moveNew(Path temporary, Path file) throws IOException {
        // 不提供 REPLACE_EXISTING，目标只要存在就必须失败，绝不覆盖已有地图。
        Files.move(temporary, file);
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 保留原始写入错误。
        }
    }

    public enum CreateResult {
        CREATED,
        ALREADY_EXISTS,
        INVALID,
        IO_ERROR
    }
}
