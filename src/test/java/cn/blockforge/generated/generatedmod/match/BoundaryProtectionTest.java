package cn.blockforge.generated.generatedmod.match;

import cn.blockforge.generated.generatedmod.MinecraftTestBootstrap;
import cn.blockforge.generated.generatedmod.map.MapDefinition;
import cn.blockforge.generated.generatedmod.map.MapManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BoundaryProtectionTest {
    @BeforeAll static void bootstrap() { MinecraftTestBootstrap.initialize(); }
    @Test void onlyActiveArenaDimensionProtectsOutsideAndRemovedCells() throws Exception {
        var match = mock(MatchManager.class, CALLS_REAL_METHODS);
        var maps = mock(MapManager.class);
        var field = MatchManager.class.getDeclaredField("maps"); field.setAccessible(true); field.set(match, maps);
        var level = mock(ServerLevel.class); when(level.dimension()).thenReturn(Level.OVERWORLD);
        var bounds = new MapDefinition.Region(BlockPos.ZERO, new BlockPos(9, 9, 9)).withBlock(new BlockPos(5, 5, 5), false);
        when(maps.currentMap()).thenReturn(Optional.of(new MapDefinition("map", "map", Level.OVERWORLD,
                bounds, bounds, List.of(), List.of(), List.of())));
        doReturn(true).when(match).isMatchActive();
        assertTrue(match.shouldProtectOutside(level, new BlockPos(10, 0, 0)));
        assertTrue(match.shouldProtectOutside(level, new BlockPos(5, 5, 5)));
        assertFalse(match.shouldProtectOutside(level, BlockPos.ZERO));
        when(level.dimension()).thenReturn(Level.NETHER);
        assertFalse(match.shouldProtectOutside(level, new BlockPos(10, 0, 0)));
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        doReturn(false).when(match).isMatchActive();
        assertFalse(match.shouldProtectOutside(level, new BlockPos(10, 0, 0)));
    }
}
