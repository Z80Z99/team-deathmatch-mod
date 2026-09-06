package cn.blockforge.generated.generatedmod.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 地图归属与邀请码索引，持久化在 config/fpsmod/maps/.index.json。
 *
 * <p>规则：地图编辑器对所有人开放，但每张地图只有拥有者（或服务器管理员）能编辑；
 * 邀请码把某张地图共享给其他玩家，导入者获得一份属于自己、之后独立编辑的副本。</p>
 */
public final class MapOwnershipStore {
    private static final Logger LOGGER = LoggerFactory.getLogger("generated_mod_map_index");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** mapId -> owner 元信息。 */
    private final Map<String, OwnerEntry> owners = new LinkedHashMap<>();
    /** 邀请码 -> mapId。 */
    private final Map<String, String> codes = new LinkedHashMap<>();
    private final Path file;

    public record OwnerEntry(String ownerUuid, String ownerName) {
    }

    public MapOwnershipStore(MinecraftServer server) {
        this.file = server.getServerDirectory().toPath()
                .resolve("config").resolve("fpsmod").resolve("maps").resolve(".index.json");
        load();
    }

    private void load() {
        owners.clear();
        codes.clear();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                return;
            }
            JsonObject object = root.getAsJsonObject();
            if (object.has("maps") && object.get("maps").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("maps").entrySet()) {
                    if (!entry.getValue().isJsonObject()) {
                        continue;
                    }
                    JsonObject value = entry.getValue().getAsJsonObject();
                    owners.put(MapDefinition.normalizeId(entry.getKey()), new OwnerEntry(
                            value.has("owner") ? value.get("owner").getAsString() : "",
                            value.has("ownerName") ? value.get("ownerName").getAsString() : "未知"));
                }
            }
            if (object.has("codes") && object.get("codes").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("codes").entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        codes.put(entry.getKey().toUpperCase(Locale.ROOT),
                                MapDefinition.normalizeId(entry.getValue().getAsString()));
                    }
                }
            }
        } catch (Exception error) {
            LOGGER.warn("读取地图归属索引失败，将按空索引继续：{}", file, error);
        }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            JsonObject object = new JsonObject();
            JsonObject maps = new JsonObject();
            for (Map.Entry<String, OwnerEntry> entry : owners.entrySet()) {
                JsonObject value = new JsonObject();
                value.addProperty("owner", entry.getValue().ownerUuid());
                value.addProperty("ownerName", entry.getValue().ownerName());
                maps.add(entry.getKey(), value);
            }
            object.add("maps", maps);
            JsonObject codeObject = new JsonObject();
            for (Map.Entry<String, String> entry : codes.entrySet()) {
                codeObject.addProperty(entry.getKey(), entry.getValue());
            }
            object.add("codes", codeObject);
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(object, writer);
            }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException error) {
            LOGGER.error("写入地图归属索引失败：{}", file, error);
        }
    }

    /** 拥有者 UUID 字符串；没有登记（旧地图）时返回空串，视为服务器地图，仅管理员可编辑。 */
    public String ownerUuid(String mapId) {
        OwnerEntry entry = owners.get(MapDefinition.normalizeId(mapId));
        return entry == null ? "" : entry.ownerUuid();
    }

    public String ownerName(String mapId) {
        OwnerEntry entry = owners.get(MapDefinition.normalizeId(mapId));
        return entry == null ? "服务器" : entry.ownerName();
    }

    public boolean isOwnedBy(String mapId, UUID playerId) {
        OwnerEntry entry = owners.get(MapDefinition.normalizeId(mapId));
        return entry != null && entry.ownerUuid().equals(playerId.toString());
    }

    public boolean hasOwner(String mapId) {
        return owners.containsKey(MapDefinition.normalizeId(mapId));
    }

    public synchronized void setOwner(String mapId, UUID playerId, String playerName) {
        owners.put(MapDefinition.normalizeId(mapId),
                new OwnerEntry(playerId.toString(), playerName == null ? "玩家" : playerName));
        save();
    }

    /** 该玩家拥有的全部地图 id（按登记顺序）。 */
    public synchronized List<String> mapsOf(UUID playerId) {
        List<String> ids = new ArrayList<>();
        String uuid = playerId.toString();
        for (Map.Entry<String, OwnerEntry> entry : owners.entrySet()) {
            if (entry.getValue().ownerUuid().equals(uuid)) {
                ids.add(entry.getKey());
            }
        }
        return ids;
    }

    public synchronized String codeFor(String mapId) {
        String normalized = MapDefinition.normalizeId(mapId);
        for (Map.Entry<String, String> entry : codes.entrySet()) {
            if (entry.getValue().equals(normalized)) {
                return entry.getKey();
            }
        }
        return "";
    }

    /** 为地图生成（或复用）一个 6 位邀请码。 */
    public synchronized String issueCode(String mapId) {
        String normalized = MapDefinition.normalizeId(mapId);
        String existing = codeFor(normalized);
        if (!existing.isEmpty()) {
            return existing;
        }
        for (int attempt = 0; attempt < 64; attempt++) {
            String code = randomCode();
            if (codes.containsKey(code)) {
                continue;
            }
            codes.put(code, normalized);
            save();
            return code;
        }
        return "";
    }

    public synchronized boolean revokeCode(String mapId) {
        String normalized = MapDefinition.normalizeId(mapId);
        boolean removed = codes.values().removeIf(normalized::equals);
        if (removed) {
            save();
        }
        return removed;
    }

    public String mapIdForCode(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        return codes.getOrDefault(code.trim().toUpperCase(Locale.ROOT), "");
    }

    /** 删除地图时清理归属和邀请码。 */
    public synchronized void removeMap(String mapId) {
        String normalized = MapDefinition.normalizeId(mapId);
        boolean changed = owners.remove(normalized) != null;
        changed |= codes.values().removeIf(normalized::equals);
        if (changed) {
            save();
        }
    }

    /** 服务器重启或外部修改后，用注册表里现存的地图 id 清理孤儿条目。 */
    public synchronized void pruneTo(List<String> existingMapIds) {
        List<String> alive = existingMapIds.stream().map(MapDefinition::normalizeId).toList();
        boolean changed = owners.keySet().removeIf(id -> !alive.contains(id));
        changed |= codes.entrySet().removeIf(entry -> !alive.contains(entry.getValue()));
        if (changed) {
            save();
        }
    }

    private static String randomCode() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return builder.toString();
    }
}
