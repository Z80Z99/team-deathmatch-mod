package cn.blockforge.generated.generatedmod.lobby;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

/** 持久化大厅设置，不依赖 Forge 的通用比赛配置。 */
public final class LobbyConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("generated_mod_lobby_config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private LobbyConfigValues values;

    public LobbyConfigManager(MinecraftServer server) {
        file = server.getServerDirectory().toPath().resolve("config").resolve("fpsmod").resolve("lobby.json");
        values = load();
    }

    public LobbyConfigValues values() {
        return values;
    }

    public synchronized String apply(LobbyConfigValues updated) {
        if (updated == null) {
            return "大厅配置为空。";
        }
        String error = updated.validationError();
        if (error != null) {
            return error;
        }
        LobbyConfigValues previous = values;
        values = updated;
        if (!save()) {
            values = previous;
            return "大厅配置写入失败，请检查服务器日志。";
        }
        return null;
    }

    private LobbyConfigValues load() {
        if (!Files.isRegularFile(file)) {
            return LobbyConfigValues.defaults();
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            // 旧配置里多余的匹配参数与 requireReady 字段在此被自然忽略。
            LobbyConfigValues loaded = new LobbyConfigValues(
                    integer(object, "maxRooms", 16),
                    integer(object, "defaultRoomMaxPlayers", 16),
                    integer(object, "minPlayersToStartRoom", 2),
                    booleanValue(object, "allowOwnerMapSelection", true),
                    integer(object, "roomStartCountdownSeconds", 30));
            if (loaded.validationError() != null) {
                LOGGER.warn("大厅配置无效，将使用默认值：{}", loaded.validationError());
                return LobbyConfigValues.defaults();
            }
            return loaded;
        } catch (Exception error) {
            LOGGER.warn("读取大厅配置失败，将使用默认值：{}", file, error);
            return LobbyConfigValues.defaults();
        }
    }

    private boolean save() {
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            JsonObject object = new JsonObject();
            object.addProperty("maxRooms", values.maxRooms());
            object.addProperty("defaultRoomMaxPlayers", values.defaultRoomMaxPlayers());
            object.addProperty("minPlayersToStartRoom", values.minPlayersToStartRoom());
            object.addProperty("allowOwnerMapSelection", values.allowOwnerMapSelection());
            object.addProperty("roomStartCountdownSeconds", values.roomStartCountdownSeconds());
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(object, writer);
            }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException error) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // 保留原始写入错误。
            }
            LOGGER.error("写入大厅配置失败：{}", file, error);
            return false;
        }
    }

    private static int integer(JsonObject object, String key, int fallback) {
        return object.has(key) ? object.get(key).getAsInt() : fallback;
    }

    private static boolean booleanValue(JsonObject object, String key, boolean fallback) {
        return object.has(key) ? object.get(key).getAsBoolean() : fallback;
    }
}
