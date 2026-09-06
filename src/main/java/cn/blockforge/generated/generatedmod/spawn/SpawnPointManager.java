package cn.blockforge.generated.generatedmod.spawn;

import cn.blockforge.generated.generatedmod.match.SpawnSelectionStrategy;
import cn.blockforge.generated.generatedmod.match.Team;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

public final class SpawnPointManager extends SavedData {
    private static final String DATA_NAME = "generated_mod_fps_arena";
    private final EnumMap<Team, List<SpawnPoint>> teamSpawns = new EnumMap<>(Team.class);
    private SpawnPoint lobbySpawn;
    private ResourceKey<Level> regionDimension;
    private BlockPos regionMin;
    private BlockPos regionMax;
    private BlockPos regionPos1;
    private BlockPos regionPos2;

    public SpawnPointManager() {
        teamSpawns.put(Team.TEAM_A, new ArrayList<>());
        teamSpawns.put(Team.TEAM_B, new ArrayList<>());
    }

    public static SpawnPointManager load(CompoundTag tag) {
        SpawnPointManager data = new SpawnPointManager();
        data.readSpawns(tag, Team.TEAM_A, "TeamASpawns");
        data.readSpawns(tag, Team.TEAM_B, "TeamBSpawns");
        if (tag.contains("Lobby")) {
            data.lobbySpawn = readPoint(tag.getCompound("Lobby"));
        }
        if (tag.contains("RegionDimension")) {
            data.regionDimension = readDimension(tag.getString("RegionDimension"));
            data.regionMin = readBlockPos(tag, "RegionMin");
            data.regionMax = readBlockPos(tag, "RegionMax");
        }
        data.regionPos1 = readOptionalBlockPos(tag, "RegionPos1");
        data.regionPos2 = readOptionalBlockPos(tag, "RegionPos2");
        return data;
    }

    public static SpawnPointManager get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(SpawnPointManager::load, SpawnPointManager::new, DATA_NAME);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        writeSpawns(tag, Team.TEAM_A, "TeamASpawns");
        writeSpawns(tag, Team.TEAM_B, "TeamBSpawns");
        if (lobbySpawn != null) {
            tag.put("Lobby", writePoint(lobbySpawn));
        }
        if (regionDimension != null && regionMin != null && regionMax != null) {
            tag.putString("RegionDimension", regionDimension.location().toString());
            writeBlockPos(tag, "RegionMin", regionMin);
            writeBlockPos(tag, "RegionMax", regionMax);
        }
        if (regionPos1 != null) {
            writeBlockPos(tag, "RegionPos1", regionPos1);
        }
        if (regionPos2 != null) {
            writeBlockPos(tag, "RegionPos2", regionPos2);
        }
        return tag;
    }

    public void addSpawn(Team team, SpawnPoint point) {
        if (!team.isPlayable()) {
            return;
        }
        teamSpawns.computeIfAbsent(team, ignored -> new ArrayList<>()).add(point);
        setDirty();
    }

    public void clearSpawns(Team team) {
        if (!team.isPlayable()) {
            return;
        }
        teamSpawns.computeIfAbsent(team, ignored -> new ArrayList<>()).clear();
        setDirty();
    }

    public List<SpawnPoint> getSpawns(Team team) {
        return List.copyOf(teamSpawns.getOrDefault(team, List.of()));
    }

    public int spawnCount(Team team) {
        return teamSpawns.getOrDefault(team, List.of()).size();
    }

    public void setLobbySpawn(SpawnPoint point) {
        lobbySpawn = point;
        setDirty();
    }

    public Optional<SpawnPoint> getLobbySpawn() {
        return Optional.ofNullable(lobbySpawn);
    }

    public void clearLobbySpawn() {
        lobbySpawn = null;
        setDirty();
    }

    public String lobbyDescription() {
        return lobbySpawn == null ? "未设置（使用世界出生点）" : pointDescription(lobbySpawn);
    }

    private static String pointDescription(SpawnPoint point) {
        return point.dimension().location() + " ["
                + point.pos().getX() + ", " + point.pos().getY() + ", " + point.pos().getZ()
                + "]，朝向 " + point.yaw();
    }

    public boolean setRegionCorner(int corner, ResourceKey<Level> dimension, BlockPos pos) {
        if ((corner != 1 && corner != 2) || dimension == null || pos == null) {
            return false;
        }
        if (regionDimension != null && !regionDimension.equals(dimension)
                && (regionPos1 != null || regionPos2 != null)) {
            return false;
        }
        regionDimension = dimension;
        if (corner == 1) {
            regionPos1 = pos.immutable();
        } else {
            regionPos2 = pos.immutable();
        }
        if (regionPos1 != null && regionPos2 != null) {
            regionMin = new BlockPos(
                    Math.min(regionPos1.getX(), regionPos2.getX()),
                    Math.min(regionPos1.getY(), regionPos2.getY()),
                    Math.min(regionPos1.getZ(), regionPos2.getZ()));
            regionMax = new BlockPos(
                    Math.max(regionPos1.getX(), regionPos2.getX()),
                    Math.max(regionPos1.getY(), regionPos2.getY()),
                    Math.max(regionPos1.getZ(), regionPos2.getZ()));
        }
        setDirty();
        return true;
    }

    public void setRegionDimension(ResourceKey<Level> dimension) {
        regionDimension = dimension;
        setDirty();
    }

    public void clearRegion() {
        regionDimension = null;
        regionMin = null;
        regionMax = null;
        regionPos1 = null;
        regionPos2 = null;
        setDirty();
    }

    public boolean hasRegion() {
        return regionDimension != null && regionMin != null && regionMax != null;
    }

    public boolean isInsideRegion(ServerPlayer player) {
        if (!hasRegion() || !regionDimension.equals(player.level().dimension())) {
            return !hasRegion();
        }
        return player.getX() >= regionMin.getX()
                && player.getX() <= regionMax.getX() + 1.0D
                && player.getY() >= regionMin.getY()
                && player.getY() <= regionMax.getY() + 1.0D
                && player.getZ() >= regionMin.getZ()
                && player.getZ() <= regionMax.getZ() + 1.0D;
    }

    public String regionDescription() {
        if (!hasRegion()) {
            return "未设置";
        }
        return regionDimension.location() + " ["
                + regionMin.getX() + ", " + regionMin.getY() + ", " + regionMin.getZ()
                + "] 到 ["
                + regionMax.getX() + ", " + regionMax.getY() + ", " + regionMax.getZ() + "]";
    }

    public boolean teleportToTeamSpawn(MinecraftServer server, ServerPlayer player, Team team,
                                       SpawnSelectionStrategy strategy) {
        ServerLevel currentLevel = player.serverLevel();
        List<SpawnPoint> points = getSpawns(team);
        if (points.isEmpty()) {
            return teleportToSharedSpawn(server, player);
        }
        SpawnPoint selected = selectSpawn(server, player, team, points, strategy);
        ServerLevel level = server.getLevel(selected.dimension());
        if (level == null) {
            return teleportToSharedSpawn(server, player);
        }
        player.teleportTo(level, selected.pos().getX() + 0.5D, selected.pos().getY() + 1.0D,
                selected.pos().getZ() + 0.5D, selected.yaw(), 0.0F);
        return true;
    }

    public boolean teleportToLobby(MinecraftServer server, ServerPlayer player) {
        if (lobbySpawn != null) {
            ServerLevel level = server.getLevel(lobbySpawn.dimension());
            if (level != null) {
                player.teleportTo(level, lobbySpawn.pos().getX() + 0.5D, lobbySpawn.pos().getY() + 1.0D,
                        lobbySpawn.pos().getZ() + 0.5D, lobbySpawn.yaw(), 0.0F);
                return true;
            }
        }
        return teleportToSharedSpawn(server, player);
    }

    private boolean teleportToSharedSpawn(MinecraftServer server, ServerPlayer player) {
        ServerLevel level = server.overworld();
        BlockPos pos = level.getSharedSpawnPos();
        player.teleportTo(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                level.getSharedSpawnAngle(), 0.0F);
        return true;
    }

    private SpawnPoint selectSpawn(MinecraftServer server, ServerPlayer player, Team team,
                                   List<SpawnPoint> points, SpawnSelectionStrategy strategy) {
        if (strategy == SpawnSelectionStrategy.RANDOM) {
            return points.get(server.overworld().getRandom().nextInt(points.size()));
        }
        if (strategy == SpawnSelectionStrategy.FARTHEST_FROM_ENEMIES) {
            SpawnPoint farthest = points.get(0);
            double farthestDistance = Double.NEGATIVE_INFINITY;
            for (SpawnPoint point : points) {
                ServerLevel level = server.getLevel(point.dimension());
                if (level == null) {
                    continue;
                }
                double nearestEnemy = Double.POSITIVE_INFINITY;
                for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                    if (other == player || !other.isAlive() || teamFromPlayer(server, other) != team.opposite()) {
                        continue;
                    }
                    if (!other.level().dimension().equals(point.dimension())) {
                        continue;
                    }
                    nearestEnemy = Math.min(nearestEnemy, other.distanceToSqr(point.pos().getX() + 0.5D,
                            point.pos().getY() + 1.0D, point.pos().getZ() + 0.5D));
                }
                if (nearestEnemy > farthestDistance) {
                    farthestDistance = nearestEnemy;
                    farthest = point;
                }
            }
            return farthest;
        }
        return points.get(Math.floorMod(player.getRandom().nextInt(), points.size()));
    }

    private Team teamFromPlayer(MinecraftServer server, ServerPlayer player) {
        net.minecraft.world.scores.PlayerTeam scoreboardTeam =
                server.getScoreboard().getPlayersTeam(player.getScoreboardName());
        if (scoreboardTeam == null) {
            return Team.SPECTATOR;
        }
        if ("generated_mod_team_a".equals(scoreboardTeam.getName())) {
            return Team.TEAM_A;
        }
        if ("generated_mod_team_b".equals(scoreboardTeam.getName())) {
            return Team.TEAM_B;
        }
        return Team.SPECTATOR;
    }

    private void writeSpawns(CompoundTag tag, Team team, String key) {
        ListTag list = new ListTag();
        for (SpawnPoint point : teamSpawns.getOrDefault(team, List.of())) {
            list.add(writePoint(point));
        }
        tag.put(key, list);
    }

    private void readSpawns(CompoundTag tag, Team team, String key) {
        List<SpawnPoint> points = teamSpawns.computeIfAbsent(team, ignored -> new ArrayList<>());
        points.clear();
        ListTag list = tag.getList(key, 10);
        for (int i = 0; i < list.size(); i++) {
            points.add(readPoint(list.getCompound(i)));
        }
    }

    private static CompoundTag writePoint(SpawnPoint point) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", point.dimension().location().toString());
        tag.putInt("X", point.pos().getX());
        tag.putInt("Y", point.pos().getY());
        tag.putInt("Z", point.pos().getZ());
        tag.putFloat("Yaw", point.yaw());
        return tag;
    }

    private static SpawnPoint readPoint(CompoundTag tag) {
        return new SpawnPoint(readDimension(tag.getString("Dimension")),
                new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z")), tag.getFloat("Yaw"));
    }

    private static ResourceKey<Level> readDimension(String value) {
        ResourceLocation location = ResourceLocation.tryParse(value);
        if (location == null) {
            return Level.OVERWORLD;
        }
        return ResourceKey.create(Registries.DIMENSION, location);
    }

    private static void writeBlockPos(CompoundTag tag, String key, BlockPos pos) {
        CompoundTag position = new CompoundTag();
        position.putInt("X", pos.getX());
        position.putInt("Y", pos.getY());
        position.putInt("Z", pos.getZ());
        tag.put(key, position);
    }

    private static BlockPos readBlockPos(CompoundTag tag, String key) {
        CompoundTag position = tag.getCompound(key);
        return new BlockPos(position.getInt("X"), position.getInt("Y"), position.getInt("Z"));
    }

    private static BlockPos readOptionalBlockPos(CompoundTag tag, String key) {
        return tag.contains(key) ? readBlockPos(tag, key) : null;
    }

    public record SpawnPoint(ResourceKey<Level> dimension, BlockPos pos, float yaw) {
    }
}
