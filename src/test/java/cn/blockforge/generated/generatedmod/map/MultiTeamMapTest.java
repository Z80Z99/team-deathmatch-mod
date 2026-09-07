package cn.blockforge.generated.generatedmod.map;

import cn.blockforge.generated.generatedmod.match.Team;
import cn.blockforge.generated.generatedmod.spawn.SpawnPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MultiTeamMapTest {
    @org.junit.jupiter.api.BeforeAll static void bootstrap() {
        cn.blockforge.generated.generatedmod.MinecraftTestBootstrap.initialize();
    }
    @Test void extraSpawnsPersistAcrossJsonRegionsAndOtherSpawnEdits() {
        var region = new MapDefinition.Region(new BlockPos(0, 0, 0), new BlockPos(32, 32, 32));
        var base = new MapDefinition("map", "map", Level.OVERWORLD, region, region, List.of(), List.of(), List.of());
        var point = new SpawnPoint(Level.OVERWORLD, 10, 10, 10, 0, 0);
        var changed = base.withTeamSpawns(Team.TEAM_C, List.of(point)).withTeamSpawns(Team.TEAM_D, List.of(point));
        assertTrue(base.spawns(Team.TEAM_C).isEmpty());
        assertTrue(changed.sameConfiguration(MapDefinition.fromJson(changed.toJson(), "map")));
        assertEquals(List.of(point), changed.withRegions(region, region).spawns(Team.TEAM_D));
        assertEquals(List.of(point), changed.withTeamSpawns(Team.TEAM_A, List.of(point)).spawns(Team.TEAM_C));
        assertTrue(MapDefinition.fromJson(base.toJson(), "map").spawns(Team.TEAM_D).isEmpty());
    }
}
