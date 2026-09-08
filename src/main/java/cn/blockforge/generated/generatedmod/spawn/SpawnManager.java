package cn.blockforge.generated.generatedmod.spawn;

import cn.blockforge.generated.generatedmod.map.MapDefinition;
import cn.blockforge.generated.generatedmod.map.MapManager;
import cn.blockforge.generated.generatedmod.match.SpawnSelectionStrategy;
import cn.blockforge.generated.generatedmod.match.Team;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

/** 读取当前地图出生点并负责玩家传送，不保存独立的硬编码坐标。 */
public final class SpawnManager {
    private final MinecraftServer server;
    private final MapManager maps;
    private final EnumMap<Team, Integer> sequentialIndexes = new EnumMap<>(Team.class);
    private final DynamicSpawnPool randomSpawns = new DynamicSpawnPool();

    public SpawnManager(MinecraftServer server, MapManager maps) {
        this.server = server;
        this.maps = maps;
    }

    public List<SpawnPoint> getSpawns(Team team) {
        MapDefinition map = maps.currentMap().orElse(null);
        return map == null ? List.of() : map.spawns(team);
    }

    public Optional<SpawnPoint> getSpectatorSpawn() {
        return getSpawns(Team.SPECTATOR).stream().findFirst();
    }

    public int spawnCount(Team team) {
        return getSpawns(team).size();
    }

    public boolean hasTeamSpawns() {
        return !getSpawns(Team.TEAM_A).isEmpty() && !getSpawns(Team.TEAM_B).isEmpty();
    }

    public boolean addTeamSpawn(Team team, SpawnPoint point) {
        if (!team.isPlayable() || !maps.isValidTeamSpawn(point)) {
            return false;
        }
        List<SpawnPoint> updated = new ArrayList<>(getSpawns(team));
        updated.add(point);
        return maps.updateTeamSpawns(team, updated);
    }

    public boolean clearTeamSpawns(Team team) {
        return team.isPlayable() && maps.updateTeamSpawns(team, List.of());
    }

    public boolean setSpectatorSpawn(SpawnPoint point) {
        return maps.isValidTeamSpawn(point) && maps.updateSpectatorSpawns(List.of(point));
    }

    public boolean clearSpectatorSpawn() {
        return maps.updateSpectatorSpawns(List.of());
    }

    public boolean teleportToTeamSpawn(ServerPlayer player, Team team, SpawnSelectionStrategy strategy) {
        if (strategy == SpawnSelectionStrategy.RANDOM) {
            Optional<SpawnPoint> random = findRandomSpawn();
            if (random.isPresent()) return teleport(player, random.get());
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "地图内未找到安全的随机出生位置，暂时转为观战。"), false);
            teleportToSpectator(player);
            return false;
        }
        List<SpawnPoint> points = getSpawns(team).stream()
                .filter(this::isUsableCurrentMapSpawn)
                .toList();
        if (points.isEmpty()) {
            return teleportToSpectator(player);
        }
        SpawnPoint selected = selectSpawn(player, team, points, strategy);
        return teleport(player, selected);
    }

    public Optional<SpawnPoint> findRandomSpawn() {
        MapDefinition map = maps.currentMap().orElse(null);
        return randomSpawns.find(map == null ? null : server.getLevel(map.world()), map);
    }

    public void refreshRandomSpawnsAfterDeath() {
        randomSpawns.requestRefresh();
    }

    public void tickRandomSpawns(boolean enabled) {
        if (!enabled) {
            randomSpawns.clear();
            return;
        }
        MapDefinition map = maps.currentMap().orElse(null);
        randomSpawns.tick(map == null ? null : server.getLevel(map.world()), map, server.getTickCount());
    }

    public boolean teleportToSpectator(ServerPlayer player) {
        Optional<SpawnPoint> configured = getSpectatorSpawn();
        if (configured.isPresent() && teleport(player, configured.get())) {
            player.setGameMode(GameType.SPECTATOR);
            return true;
        }
        MapDefinition map = maps.currentMap().orElse(null);
        if (map != null) {
            ServerLevel level = server.getLevel(map.world());
            if (level != null) {
                BlockPos pos = level.getSharedSpawnPos();
                player.teleportTo(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                        level.getSharedSpawnAngle(), 0.0F);
                player.setGameMode(GameType.SPECTATOR);
                return true;
            }
        }
        boolean teleported = teleportToSharedSpawn(player);
        player.setGameMode(GameType.SPECTATOR);
        return teleported;
    }

    public boolean teleportToLobby(ServerPlayer player) {
        return teleportToSpectator(player);
    }

    public boolean isInsideMap(ServerPlayer player) {
        return maps.isInsideMap(player);
    }

    public String boundsDescription() {
        return maps.currentMap().map(map -> map.bounds().toString()).orElse("未选择地图");
    }

    public String spectatorDescription() {
        return getSpectatorSpawn().map(SpawnPoint::description).orElse("未设置（使用地图世界出生点）");
    }

    private boolean isUsableCurrentMapSpawn(SpawnPoint point) {
        MapDefinition map = maps.currentMap().orElse(null);
        if (map == null || !isCurrentMapDimension(point) || !map.bounds().contains(point.x(), point.y(), point.z())) {
            return false;
        }
        ServerLevel level = server.getLevel(point.dimension());
        if (level == null) {
            return false;
        }
        BlockPos feet = BlockPos.containing(point.x(), point.y(), point.z());
        return level.getWorldBorder().isWithinBounds(feet)
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                && !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty();
    }

    private SpawnPoint selectSpawn(ServerPlayer player, Team team, List<SpawnPoint> points,
                                   SpawnSelectionStrategy strategy) {
        if (strategy == SpawnSelectionStrategy.SEQUENTIAL) {
            int index = sequentialIndexes.merge(team, 1, Integer::sum) - 1;
            return points.get(Math.floorMod(index, points.size()));
        }
        if (strategy == SpawnSelectionStrategy.FARTHEST_FROM_ENEMIES) {
            SpawnPoint best = points.get(0);
            double bestDistance = Double.NEGATIVE_INFINITY;
            for (SpawnPoint point : points) {
                double nearestEnemy = Double.POSITIVE_INFINITY;
                for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                    if (other == player || !other.isAlive() || !teamOf(other).isPlayable() || teamOf(other) == team
                            || !other.level().dimension().equals(point.dimension())) {
                        continue;
                    }
                    nearestEnemy = Math.min(nearestEnemy, other.distanceToSqr(point.x(), point.y(), point.z()));
                }
                if (nearestEnemy > bestDistance) {
                    bestDistance = nearestEnemy;
                    best = point;
                }
            }
            return best;
        }
        return points.get(server.overworld().getRandom().nextInt(points.size()));
    }

    private boolean teleport(ServerPlayer player, SpawnPoint point) {
        ServerLevel level = server.getLevel(point.dimension());
        if (level == null) {
            return false;
        }
        player.teleportTo(level, point.x(), point.y(), point.z(), point.yaw(), point.pitch());
        player.fallDistance = 0;
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        return true;
    }

    private boolean teleportToSharedSpawn(ServerPlayer player) {
        ServerLevel level = server.overworld();
        BlockPos pos = level.getSharedSpawnPos();
        player.teleportTo(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                level.getSharedSpawnAngle(), 0.0F);
        return true;
    }

    private boolean isCurrentMapDimension(SpawnPoint point) {
        return maps.currentMap().map(map -> map.world().equals(point.dimension())).orElse(false);
    }

    private Team teamOf(ServerPlayer player) {
        PlayerTeam scoreboardTeam = server.getScoreboard().getPlayersTeam(player.getScoreboardName());
        if (scoreboardTeam == null) {
            return Team.SPECTATOR;
        }
        return switch (scoreboardTeam.getName()) {
            case "generated_mod_team_a" -> Team.TEAM_A;
            case "generated_mod_team_b" -> Team.TEAM_B;
            case "generated_mod_team_c" -> Team.TEAM_C;
            case "generated_mod_team_d" -> Team.TEAM_D;
            default -> Team.SPECTATOR;
        };
    }
}
