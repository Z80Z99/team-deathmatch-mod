package cn.blockforge.generated.generatedmod.match;

import cn.blockforge.generated.generatedmod.network.packet.HudEventPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchRecordTest {
    @Test void eventPacketRoundTripsStructuredData() {
        HudEventPacket original = new HudEventPacket(MatchHudEventType.BOMB_PLANTING,
                "保持安装动作", 45, 96);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        original.encode(buffer);
        HudEventPacket decoded = new HudEventPacket(buffer);
        assertEquals(original.type(), decoded.type());
        assertEquals(original.detail(), decoded.detail());
        assertEquals(45, decoded.durationTicks());
        assertEquals(96, decoded.priority());
        buffer.release();
    }

    @Test void bookPaginationHonoursPageLimitAndPointsToFullFile() {
        List<String> lines = java.util.stream.IntStream.range(0, 120)
                .mapToObj(index -> "第 " + index + " 条比赛记录，用于验证书面书分页不会无限增长。")
                .toList();
        Path file = Path.of("match-records", "sample.log");
        List<String> pages = MatchRecordManager.paginate(lines, 80, 5, file);
        assertEquals(5, pages.size());
        assertTrue(pages.get(4).contains("match-records"));
        assertTrue(pages.subList(0, 4).stream().allMatch(page -> page.length() <= 80));
    }

    @Test void matchRecordIsWrittenToGameDirectory(@TempDir Path temporary) throws IOException {
        MinecraftServer server = mock(MinecraftServer.class);
        PlayerList playerList = mock(PlayerList.class);
        MatchManager match = mock(MatchManager.class);
        when(server.getServerDirectory()).thenReturn(temporary.toFile());
        when(server.getTickCount()).thenReturn(100);
        when(server.getPlayerList()).thenReturn(playerList);
        when(playerList.getPlayers()).thenReturn(List.of());
        when(match.activeTeams()).thenReturn(List.of());
        when(match.scores()).thenReturn(new MatchScoreTracker());

        MatchRecordManager records = new MatchRecordManager(server, match);
        records.begin(null, GameMode.TEAM_DEATHMATCH, List.of());
        when(server.getTickCount()).thenReturn(240);
        records.log("测试事件");
        records.finish("测试结束");

        Path directory = temporary.resolve("match-records");
        assertTrue(Files.isDirectory(directory));
        try (var files = Files.list(directory)) {
            Path record = files.findFirst().orElseThrow();
            String content = Files.readString(record);
            assertTrue(content.contains("测试事件"));
            assertTrue(content.contains("比赛结果：测试结束"));
            assertTrue(content.contains("=== 对局结束 ==="));
        }
    }
}
