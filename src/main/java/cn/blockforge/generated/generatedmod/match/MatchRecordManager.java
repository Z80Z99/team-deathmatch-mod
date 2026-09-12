package cn.blockforge.generated.generatedmod.match;

import cn.blockforge.generated.generatedmod.map.MapDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Writes a persistent match log and turns the finished log into books for participants. */
public final class MatchRecordManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("generated_mod_match_records");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int PAGE_CHARACTERS = 220;
    private static final int MAX_BOOK_PAGES = 50;

    private final MinecraftServer server;
    private final MatchManager match;
    private final List<String> lines = new ArrayList<>();
    private final Map<UUID, Participant> participants = new LinkedHashMap<>();
    private BufferedWriter writer;
    private Path file;
    private long startTick;
    private String mapName = "未选择地图";
    private String modeName = "团队竞技";
    private boolean active;

    MatchRecordManager(MinecraftServer server, MatchManager match) {
        this.server = server;
        this.match = match;
    }

    public synchronized void begin(MapDefinition map, GameMode mode, Collection<ServerPlayer> players) {
        finish("上一场比赛未正常结束");
        lines.clear();
        participants.clear();
        mapName = map == null ? "未选择地图" : map.displayName();
        modeName = mode == null ? "团队竞技" : mode.displayName();
        startTick = server.getTickCount();
        active = true;
        try {
            Path directory = server.getServerDirectory().toPath().resolve("match-records");
            Files.createDirectories(directory);
            String stamp = LocalDateTime.now().format(FILE_TIME);
            file = directory.resolve(stamp + "-" + safeFileName(mapName) + "-"
                    + safeFileName(modeName) + ".log");
            writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
            append("=== 对局记录 ===");
            append("地图：" + mapName);
            append("模式：" + modeName);
            append("开始时间：" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            for (ServerPlayer player : players) addParticipant(player);
        } catch (IOException error) {
            active = false;
            LOGGER.error("创建对局记录失败：{}", file, error);
        }
    }

    public synchronized void addParticipant(ServerPlayer player) {
        if (!active || player == null) return;
        Team team = match.teamManager().getTeam(player);
        if (team == null || !team.isPlayable()) return;
        Participant previous = participants.put(player.getUUID(),
                new Participant(player.getUUID(), player.getGameProfile().getName(), team));
        if (previous == null) {
            log("玩家加入对局：" + player.getGameProfile().getName() + "（" + team.displayName() + "）");
        } else if (previous.team() != team) {
            log("玩家换队：" + player.getGameProfile().getName() + "（"
                    + previous.team().displayName() + " → " + team.displayName() + "）");
        }
    }

    public synchronized void log(String message) {
        if (!active || message == null || message.isBlank()) return;
        append("[" + elapsedText() + "] " + message);
    }

    public synchronized void finish(String result) {
        if (!active) return;
        log("比赛结果：" + (result == null || result.isBlank() ? "已结束" : result));
        for (Team team : match.activeTeams()) {
            log("队伍统计：" + team.displayName() + "，胜场 " + match.roundWins(team)
                    + "，本轮得分 " + match.scores().getTeamScore(team)
                    + "，整场击杀 " + match.scores().getTeamMatchKills(team)
                    + "，伤害 " + match.scores().getTeamDamage(team));
        }
        for (Participant participant : participants.values()) {
            log("玩家统计：" + participant.name() + "，击杀 "
                    + match.scores().getMatchKills(participant.playerId())
                    + "，阵亡 " + match.scores().getMatchDeaths(participant.playerId())
                    + "，输出 " + match.scores().getDamageDealt(participant.playerId())
                    + "，承伤 " + match.scores().getDamageTaken(participant.playerId()));
        }
        append("=== 对局结束 ===");
        append("记录文件：" + (file == null ? "不可用" : file.toAbsolutePath()));
        active = false;
        closeWriter();
        deliverBooks(result);
    }

    public synchronized void shutdown() {
        finish("服务器关闭，比赛未正常结束");
        active = false;
        closeWriter();
    }

    public synchronized boolean active() {
        return active;
    }

    private void append(String text) {
        String line = text == null ? "" : text;
        lines.add(line);
        if (writer == null) return;
        try {
            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException error) {
            LOGGER.error("写入对局记录失败：{}", file, error);
        }
    }

    private void closeWriter() {
        if (writer == null) return;
        try {
            writer.close();
        } catch (IOException error) {
            LOGGER.warn("关闭对局记录失败：{}", file, error);
        } finally {
            writer = null;
        }
    }

    private void deliverBooks(String result) {
        List<String> completed = List.copyOf(lines);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Participant participant = participants.get(player.getUUID());
            if (participant == null) continue;
            ItemStack book = createBook(player, participant, completed, result);
            if (!player.getInventory().add(book)) player.drop(book, false);
            player.containerMenu.broadcastChanges();
        }
    }

    private ItemStack createBook(ServerPlayer player, Participant participant,
                                 List<String> completed, String result) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.setHoverName(Component.literal("对局记录 · " + fitTitle(mapName)));
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", fitTitle(mapName + " 对局记录"));
        tag.putString("author", "TeamDeathMatch");
        tag.putInt("generation", 0);
        List<String> source = new ArrayList<>();
        source.add("地图：" + mapName + "\n模式：" + modeName + "\n结果："
                + (result == null ? "已结束" : result) + "\n我的队伍：" + participant.team().displayName()
                + "\n我的战绩：击杀 " + match.scores().getMatchKills(participant.playerId())
                + "，阵亡 " + match.scores().getMatchDeaths(participant.playerId())
                + "，输出 " + match.scores().getDamageDealt(participant.playerId())
                + "，承伤 " + match.scores().getDamageTaken(participant.playerId()));
        source.addAll(completed);
        ListTag pages = new ListTag();
        for (String page : paginate(source, PAGE_CHARACTERS, MAX_BOOK_PAGES, file)) {
            pages.add(StringTag.valueOf(page));
        }
        tag.put("pages", pages);
        return book;
    }

    static List<String> paginate(List<String> source, int maximumCharacters,
                                 int maximumPages, Path fullRecordFile) {
        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        for (String raw : source) {
            for (String line : wrap(raw == null ? "" : raw, Math.max(20, maximumCharacters))) {
                if (page.length() > 0 && page.length() + line.length() + 1 > maximumCharacters) {
                    pages.add(page.toString());
                    page.setLength(0);
                }
                if (page.length() > 0) page.append('\n');
                page.append(line);
            }
        }
        if (page.length() > 0) pages.add(page.toString());
        if (pages.size() <= maximumPages) return pages;
        List<String> limited = new ArrayList<>(pages.subList(0, maximumPages - 1));
        limited.add("记录页数超过书面上限。\n完整内容见：\n"
                + (fullRecordFile == null ? "match-records 目录" : fullRecordFile.toAbsolutePath()));
        return limited;
    }

    private static List<String> wrap(String value, int width) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : value.split("\\R", -1)) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }
            int start = 0;
            while (start < paragraph.length()) {
                int end = Math.min(paragraph.length(), start + width);
                lines.add(paragraph.substring(start, end));
                start = end;
            }
        }
        return lines;
    }

    private String elapsedText() {
        long ticks = Math.max(0L, server.getTickCount() - startTick);
        long seconds = ticks / 20L;
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60L, seconds % 60L);
    }

    private static String safeFileName(String value) {
        String safe = value == null ? "match" : value.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        return safe.isBlank() ? "match" : safe.substring(0, Math.min(48, safe.length()));
    }

    private static String fitTitle(String value) {
        String title = value == null || value.isBlank() ? "对局记录" : value;
        return title.length() <= 32 ? title : title.substring(0, 32);
    }

    private record Participant(UUID playerId, String name, Team team) { }
}
