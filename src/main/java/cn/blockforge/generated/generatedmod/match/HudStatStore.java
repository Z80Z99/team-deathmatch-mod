package cn.blockforge.generated.generatedmod.match;

import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.HudStatSyncPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义统计通道（服务器权威）：管理员用 {@code /fps hudstat set <标识> <显示名> <数值> <上限>}
 * 推送任意进度数值（如“C4 安装进度 3/10”），全体客户端即可在 HUD 配置窗的
 * 数据源列表里把这些值绑定给进度条 / 计数文字模块。
 *
 * <p>上限填 0 表示这不是进度而是一个普通数值；重复 set 同一标识会覆盖。
 * 这一通道也是开放给其他玩法系统（C4、占点、护送等）对接本 Mod HUD 的接口。
 */
public final class HudStatStore {
    private static final HudStatStore INSTANCE = new HudStatStore();

    /** 一项自定义统计：标识、显示名、当前值、上限（0=普通数值）。 */
    public record Entry(String id, String label, int value, int maximum) {
    }

    private final Map<String, Entry> entries = new LinkedHashMap<>();

    private HudStatStore() {
    }

    public static HudStatStore get() {
        return INSTANCE;
    }

    public synchronized void set(String id, String label, int value, int maximum) {
        String safeId = safeId(id);
        Entry existing = entries.get(safeId);
        if (existing == null && entries.size() >= 1024) {
            throw new IllegalArgumentException("HUD stat count limit reached");
        }
        String safeLabel = cleanLabel(label);
        String resolvedLabel = safeLabel.isBlank()
                ? (existing != null ? existing.label() : safeId) : safeLabel;
        entries.put(safeId, new Entry(safeId, resolvedLabel, value, Math.max(0, maximum)));
    }

    /** 仅改显示名；标识不存在时返回 false。 */
    public synchronized boolean rename(String id, String label) {
        String safeId = safeId(id);
        Entry existing = entries.get(safeId);
        String safeLabel = cleanLabel(label);
        if (existing == null || safeLabel.isBlank()) {
            return false;
        }
        entries.put(safeId, new Entry(safeId, safeLabel, existing.value(), existing.maximum()));
        return true;
    }

    public synchronized boolean remove(String id) {
        return entries.remove(safeId(id)) != null;
    }

    public synchronized List<Entry> snapshot() {
        return new ArrayList<>(entries.values());
    }

    public synchronized void clear() {
        entries.clear();
    }

    /** 把当前全部自定义统计广播给在线玩家（含稍后入场的，见 sendTo）。 */
    public synchronized void broadcast(MinecraftServer server) {
        HudStatSyncPacket packet = toPacket();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            FpsTdmNetwork.sendToPlayer(packet, player);
        }
    }

    /** 单个玩家（重新）入场时补发一次。 */
    public synchronized void sendTo(ServerPlayer player) {
        FpsTdmNetwork.sendToPlayer(toPacket(), player);
    }

    private HudStatSyncPacket toPacket() {
        List<String> lines = new ArrayList<>();
        for (Entry entry : entries.values()) {
            lines.add(entry.id() + "\u0001" + entry.label() + "\u0001" + entry.value()
                    + "\u0001" + entry.maximum());
        }
        return new HudStatSyncPacket(lines);
    }

    private static String safeId(String id) {
        String value = id == null ? "" : id.trim();
        if (value.isBlank()) throw new IllegalArgumentException("HUD stat id is required");
        value = value.replace('\u0001', '_');
        return value.length() > 96 ? value.substring(0, 96) : value;
    }

    private static String cleanLabel(String label) {
        String value = label == null ? "" : label.trim();
        value = value.replace('\u0001', ' ');
        return value.length() > 96 ? value.substring(0, 96) : value;
    }
}
