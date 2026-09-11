package cn.blockforge.generated.generatedmod.spawn;

import cn.blockforge.generated.generatedmod.map.MapDefinition;
import cn.blockforge.generated.generatedmod.map.MapManager;
import cn.blockforge.generated.generatedmod.match.MapRegionActivation;
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
    private final EnumMap<Team, DynamicSpawnPool> fixedPools = new EnumMap<>(Team.class);
    private final EnumMap<Team, MapDefinition> fixedMaps = new EnumMap<>(Team.class);
    private MapDefinition fixedSource;
    private final DynamicSpawnPool randomSpawns = new DynamicSpawnPool();
    private MapDefinition verifiedRandomSource;
    private SpawnPoint verifiedRandomSpawn;
    private MapRegionActivation.Context activationContext = MapRegionActivation.INACTIVE;

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
        return maps.isValidSpectatorSpawn(point) && maps.updateSpectatorSpawns(List.of(point));
    }

    public boolean clearSpectatorSpawn() {
        return maps.updateSpectatorSpawns(List.of());
    }

    public boolean teleportToTeamSpawn(ServerPlayer player, Team team, SpawnSelectionStrategy strategy) {
        if (tryTeleportToTeamSpawn(player, team, strategy)) return true;
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "复活范围内暂时没有安全位置，转为观战。"), false);
        teleportToSpectator(player);
        return false;
    }

    public boolean tryTeleportToTeamSpawn(ServerPlayer player, Team team, SpawnSelectionStrategy strategy) {
        MapDefinition map = maps.currentMap().orElse(null);
        Optional<SpawnPoint> point = strategy == SpawnSelectionStrategy.SEQUENTIAL
                ? findFixedSpawn(team) : randomSpawns.find(map == null ? null : server.getLevel(map.world()),
                        map, pos -> enemyDistance(player, pos));
        return point.isPresent() && teleport(player, point.get());
    }

    private double enemyDistance(ServerPlayer player, BlockPos pos) {
        double distance = Double.POSITIVE_INFINITY;
        Team own = teamOf(player);
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == player || !other.isAlive() || other.isSpectator() || other.isInvulnerable()
                    || !teamOf(other).isPlayable() || teamOf(other) == own
                    || other.level() != player.level()) continue;
            distance = Math.min(distance, other.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
        }
        return distance;
    }

    private MapDefinition fixedMap(Team team, MapRegionActivation.Context context) {
        MapDefinition map = maps.currentMap().orElse(null);
        if (fixedSource != map) {
            fixedSource = map;
            fixedPools.clear();
            fixedMaps.clear();
        }
        if (map == null || !team.isPlayable()) return null;
        if (fixedMaps.containsKey(team)) return fixedMaps.get(team);
        String type = switch (team) {
            case TEAM_A -> "spawn_a"; case TEAM_B -> "spawn_b";
            case TEAM_C -> "spawn_c"; case TEAM_D -> "spawn_d";
            default -> "";
        };
        var parts = MapRegionActivation.activeRegions(map.customRegions(), context).stream()
                .filter(region -> region.type().id().equals(type))
                .flatMap(region -> region.region().boxes().stream()).toList();
        if (parts.isEmpty()) return null;
        var region = MapDefinition.Region.composite(parts);
        MapDefinition scoped = new MapDefinition(map.id(), map.displayName(), map.world(), region, region,
                List.of(), List.of(), List.of());
        fixedMaps.put(team, scoped);
        return scoped;
    }

    public Optional<SpawnPoint> findFixedSpawn(Team team) {
        return findFixedSpawn(team, activationContext);
    }

    public Optional<SpawnPoint> findFixedSpawn(Team team, MapRegionActivation.Context context) {
        MapDefinition scoped = fixedMap(team, context);
        if (scoped != null) {
            return fixedPools.computeIfAbsent(team, ignored -> new DynamicSpawnPool())
                    .find(server.getLevel(scoped.world()), scoped).filter(this::isUsableCurrentMapSpawn);
        }
        // Existing maps may still contain authored point markers instead of regions.
        List<SpawnPoint> points = getSpawns(team).stream().filter(this::isUsableCurrentMapSpawn).toList();
        return points.isEmpty() ? Optional.empty()
                : Optional.of(points.get(server.overworld().getRandom().nextInt(points.size())));
    }

    public Optional<SpawnPoint> findRandomSpawn() {
        MapDefinition map = maps.currentMap().orElse(null);
        return randomSpawns.find(map == null ? null : server.getLevel(map.world()), map);
    }

    /** Rebuild the pool for a new match; the prior match may have unloaded map chunks. */
    public Optional<SpawnPoint> prepareRandomSpawnForMatch() {
        MapDefinition map = maps.currentMap().orElse(null);
        ServerLevel level = map == null ? null : server.getLevel(map.world());
        if (map == null || level == null) {
            verifiedRandomSource = null;
            verifiedRandomSpawn = null;
            return Optional.empty();
        }
        if (verifiedRandomSource != map && (verifiedRandomSource == null
                || !verifiedRandomSource.sameConfiguration(map))) {
            verifiedRandomSource = map;
            verifiedRandomSpawn = null;
        } else if (verifiedRandomSource != map) {
            // A disk reload produced an equivalent object; retain the verified point.
            verifiedRandomSource = map;
        }
        if (verifiedRandomSpawn != null) {
            BlockPos feet = BlockPos.containing(verifiedRandomSpawn.x(), verifiedRandomSpawn.y(), verifiedRandomSpawn.z());
            level.getChunk(feet.getX() >> 4, feet.getZ() >> 4);
            if (isUsableCurrentMapSpawn(verifiedRandomSpawn)) return Optional.of(verifiedRandomSpawn);
        }
        Optional<SpawnPoint> current = randomSpawns.find(level, map);
        Optional<SpawnPoint> result = current.isPresent() ? current : randomSpawns.rebuildForMatchStart(level, map);
        result.ifPresent(point -> verifiedRandomSpawn = point);
        return result;
    }

    public void refreshRandomSpawnsAfterDeath() {
        randomSpawns.requestRefresh();
        fixedPools.values().forEach(DynamicSpawnPool::requestRefresh);
    }

    public void updateActivationContext(MapRegionActivation.Context context) {
        MapRegionActivation.Context next = context == null ? MapRegionActivation.INACTIVE : context;
        if (next.equals(activationContext)) return;
        activationContext = next;
        fixedPools.clear();
        fixedMaps.clear();
        fixedSource = null;
    }

    public void tickRandomSpawns(boolean enabled, SpawnSelectionStrategy strategy) {
        if (!enabled) {
            randomSpawns.clear();
            fixedPools.clear();
            fixedMaps.clear();
            fixedSource = null;
            return;
        }
        MapDefinition map = maps.currentMap().orElse(null);
        if (strategy == SpawnSelectionStrategy.RANDOM) {
            randomSpawns.tick(map == null ? null : server.getLevel(map.world()), map, server.getTickCount());
            return;
        }
        for (Team team : List.of(Team.TEAM_A, Team.TEAM_B, Team.TEAM_C, Team.TEAM_D)) {
            MapDefinition scoped = fixedMap(team, activationContext);
            if (scoped != null) fixedPools.computeIfAbsent(team, ignored -> new DynamicSpawnPool())
                    .tick(server.getLevel(scoped.world()), scoped, server.getTickCount(), 64);
        }
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
        return SafeSpawnFinder.safe(level, map.bounds(), feet);
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
