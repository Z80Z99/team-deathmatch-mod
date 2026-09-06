package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.network.packet.HudStatSyncPacket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义统计通道的客户端缓存：管理员通过 {@code /fps hudstat} 或其他玩法系统
 * 推送的数值（如 C4 安装进度）会出现在这里，并由 {@link HudStats}
 * 作为数据源暴露给 HUD 的进度条 / 计数文字模块。
 */
public final class ClientHudDynamicStats {
    /** 一项自定义统计：标识、显示名、数值与上限（0=普通数值）。 */
    public record Entry(String id, String label, int value, int maximum) {
    }

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();
    private static int revision;

    private ClientHudDynamicStats() {
    }

    public static void apply(HudStatSyncPacket packet) {
        ENTRIES.clear();
        for (String line : packet.entries()) {
            String[] parts = line.split("\u0001", -1);
            if (parts.length < 4 || parts[0].isBlank()) {
                continue;
            }
            try {
                ENTRIES.put(parts[0], new Entry(parts[0],
                        parts[1].isBlank() ? parts[0] : parts[1],
                        Integer.parseInt(parts[2]), Math.max(0, Integer.parseInt(parts[3]))));
            } catch (NumberFormatException ignored) {
                // 数值损坏的条目直接跳过，不影响其余统计。
            }
        }
        revision++;
    }

    public static List<Entry> entries() {
        return new ArrayList<>(ENTRIES.values());
    }

    public static Entry byId(String id) {
        return ENTRIES.get(id);
    }

    public static int revision() {
        return revision;
    }

    public static void clear() {
        ENTRIES.clear();
        revision++;
    }
}
