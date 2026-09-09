package cn.blockforge.generated.generatedmod.match;

import cn.blockforge.generated.generatedmod.MinecraftTestBootstrap;
import cn.blockforge.generated.generatedmod.mixin.CraterBoundaryMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CraterGuardTest {
    @BeforeAll static void bootstrap() { MinecraftTestBootstrap.initialize(); }
    @Test void rejectedDirectWritesAdvanceButOrdinaryLoopOwnsItsIndex() throws Exception {
        var guard = new CraterBoundaryMixin() { };
        var index = CraterBoundaryMixin.class.getDeclaredField("appliedIndex"); index.setAccessible(true);
        var direct = CraterBoundaryMixin.class.getDeclaredMethod("tdm$directProduction", ServerLevel.class, BlockPos.class);
        var ordinary = CraterBoundaryMixin.class.getDeclaredMethod("tdm$protectProduction", ServerLevel.class, BlockPos.class);
        direct.setAccessible(true); ordinary.setAccessible(true);
        var level = mock(ServerLevel.class); var match = mock(MatchManager.class);
        when(level.hasChunkAt(BlockPos.ZERO)).thenReturn(true);
        when(match.shouldProtectBlock(level, BlockPos.ZERO)).thenReturn(true);
        try (var access = mockStatic(MatchManager.class)) {
            access.when(MatchManager::get).thenReturn(match);
            assertEquals(false, ordinary.invoke(guard, level, BlockPos.ZERO));
            assertEquals(0, index.getInt(guard));
            assertEquals(false, direct.invoke(guard, level, BlockPos.ZERO));
            assertEquals(1, index.getInt(guard));
            when(match.shouldProtectBlock(level, BlockPos.ZERO)).thenReturn(false);
            assertEquals(true, direct.invoke(guard, level, BlockPos.ZERO));
            assertEquals(1, index.getInt(guard));
        }
    }
}
