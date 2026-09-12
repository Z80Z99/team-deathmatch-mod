package cn.blockforge.generated.generatedmod.match;

import cn.blockforge.generated.generatedmod.config.FpsTdmConfig;
import cn.blockforge.generated.generatedmod.config.FpsTdmConfigController;
import cn.blockforge.generated.generatedmod.economy.EconomyManager;
import cn.blockforge.generated.generatedmod.integration.IntegrationManager;
import cn.blockforge.generated.generatedmod.map.MapManager;
import cn.blockforge.generated.generatedmod.match.MapRegionActivation;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.HudEventPacket;
import cn.blockforge.generated.generatedmod.network.packet.MatchSyncPacket;
import cn.blockforge.generated.generatedmod.spawn.SpawnManager;
import cn.blockforge.generated.generatedmod.spawn.SpawnPoint;
import cn.blockforge.generated.generatedmod.team.TeamManager;
import cn.blockforge.generated.generatedmod.weapon.WeaponRepositoryManager;
import cn.blockforge.generated.generatedmod.shop.MatchShopManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.level.ExplosionEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TDM 比赛流程管理器。地图定义、出生点和地图恢复分别由独立管理器负责。
 */
public final class MatchManager {
    private final BoundaryCountdown boundaryCountdown = new BoundaryCountdown();
    private static MatchManager instance;

    private final MinecraftServer server;
    private final MapManager maps;
    private final SpawnManager spawns;
    private final TeamManager teams;
    private final cn.blockforge.generated.generatedmod.lobby.RoomManager rooms;
    private final cn.blockforge.generated.generatedmod.map.MapEditorManager mapEditor;
    private final MatchScoreTracker scores = new MatchScoreTracker();
    private final WeaponRepositoryManager weapons;
    private final EconomyManager economy;
    private final MatchShopManager matchShop;
    private final ClassicBombManager bomb;
    private final MatchRecordManager records;
    private final Map<UUID, DownedEntry> downedPlayers = new HashMap<>();
    private final java.util.Set<UUID> readyRespawnRequests = new java.util.HashSet<>();
    private final Map<UUID, Long> respawnProtectionEnds = new HashMap<>();
    private final Map<UUID, PendingDeath> pendingDeaths = new HashMap<>();
    private final java.util.Set<UUID> respawnedPlayers = new java.util.HashSet<>();
    private final Map<UUID, Long> regionReturnTicks = new HashMap<>();
    private final Map<UUID, SpawnPoint> frozenPositions = new HashMap<>();
    private final Map<UUID, SpawnPoint> buySpawnAnchors = new HashMap<>();
    private final java.util.Set<UUID> buyAreaNoticeSent = new java.util.HashSet<>();
    private final java.util.Set<UUID> spectatorAreaPlayers = new java.util.HashSet<>();
    private final Map<UUID, UUID> spectateTargets = new HashMap<>();

    /** 整场开始时刻（tick），供 HUD「本局已进行时间」统计源使用。 */
    private long matchStartTick;
    private long roundStartTick;

    /** 最近一次击杀的公告信息，随 MatchSyncPacket 下发给客户端 HUD。 */
    private int killFeedSequence;
    private String lastKillKiller = "";
    private String lastKillVictim = "";

    private static final long REGION_RETURN_COOLDOWN_TICKS = 20L;

    private MatchState state = MatchState.WAITING;
    /** 本次比赛的房间级规则覆盖；由房主在大厅设置，开赛时应用，结束即清除。 */
    private cn.blockforge.generated.generatedmod.lobby.RoomRules roomRules;
    private boolean terrainRestoreAfterWarmup;
    private boolean terrainRestoreStarted;
    private Team winner;
    private Team roundWinner;
    private Team pendingMatchWinner;
    private long phaseEndTick;
    private int roundNumber;
    private int teamAWins;
    private int teamBWins;
    private final java.util.Map<Team, Integer> extraWins = new java.util.EnumMap<>(Team.class);
    private int teamCount = 2;
    private boolean roomTeamSelection;

    public java.util.List<Team> activeTeams() { return Team.playing(teamCount); }
    public boolean hasRoomTeamSelection() { return roomTeamSelection; }
    public void configureRoomTeams(int count) {
        teamCount = Math.max(2, Math.min(4, count));
        if (roomRules != null && roomRules.mode() == GameMode.SEARCH_DESTROY) teamCount = 2;
        roomTeamSelection = true;
    }
    private boolean rulesCaptured;
    private boolean originalKeepInventory;
    private boolean originalDeathMessages;
    private boolean resetFailureNotified;
    private boolean stopAfterMapReset;

    private MatchManager(MinecraftServer server) {
        this.server = server;
        this.maps = new MapManager(server);
        this.spawns = new SpawnManager(server, maps);
        this.teams = new TeamManager(server, this);
        this.weapons = new WeaponRepositoryManager(server);
        this.economy = new EconomyManager(server);
        this.matchShop = new MatchShopManager(server, this);
        this.bomb = new ClassicBombManager(this);
        this.records = new MatchRecordManager(server, this);
        this.rooms = new cn.blockforge.generated.generatedmod.lobby.RoomManager(server, this);
        this.mapEditor = new cn.blockforge.generated.generatedmod.map.MapEditorManager(server, this, maps);
        if (!maps.registry().definitions().isEmpty()) {
            maps.loadMap(maps.registry().definitions().get(0).id());
        }
        IntegrationManager.detect();
    }

    public static void init(MinecraftServer server) {
        instance = new MatchManager(server);
    }

    public static void shutdown() {
        if (instance != null) {
            if (instance.records != null) instance.records.shutdown();
            instance.restoreRules();
            instance.teams.resetAll();
            instance.maps.shutdown();
        }
        instance = null;
    }

    public static MatchManager get() {
        return instance;
    }

    public MinecraftServer server() {
        return server;
    }

    public SpawnManager spawns() {
        return spawns;
    }

    public MapManager maps() {
        return maps;
    }

    public TeamManager teamManager() {
        return teams;
    }

    Team attackingTeam() {
        return sidesSwappedThisRound() ? Team.TEAM_B : Team.TEAM_A;
    }

    Team defendingTeam() {
        return sidesSwappedThisRound() ? Team.TEAM_A : Team.TEAM_B;
    }

    public cn.blockforge.generated.generatedmod.lobby.RoomManager rooms() {
        return rooms;
    }

    public cn.blockforge.generated.generatedmod.map.MapEditorManager mapEditor() {
        return mapEditor;
    }

    public MatchScoreTracker scores() {
        return scores;
    }

    MatchRecordManager records() {
        return records;
    }

    void recordEvent(String message) {
        if (records != null) records.log(message);
    }

    void finishRecord(String result) {
        if (records != null) records.finish(result);
    }

    void publishHudEvent(MatchHudEventType type, String detail, int durationTicks) {
        FpsTdmNetwork.sendToAll(new HudEventPacket(type, detail, durationTicks));
    }

    void publishHudEvent(ServerPlayer player, MatchHudEventType type,
                         String detail, int durationTicks) {
        if (player != null) FpsTdmNetwork.sendToPlayer(new HudEventPacket(type, detail, durationTicks), player);
    }

    public WeaponRepositoryManager weapons() {
        return weapons;
    }

    public EconomyManager economy() {
        return economy;
    }

    public MatchShopManager matchShop() {
        return matchShop;
    }

    public ClassicBombManager bomb() {
        return bomb;
    }

    public MatchState state() {
        return state;
    }

    public Team winner() {
        return winner;
    }

    public Team roundWinner() {
        return roundWinner;
    }

    public int roundNumber() {
        return roundNumber;
    }

    public int roundWins(Team team) {
        return team == Team.TEAM_A ? teamAWins : team == Team.TEAM_B ? teamBWins : extraWins.getOrDefault(team, 0);
    }

    public boolean isMatchActive() {
        return state != MatchState.WAITING;
    }

    public boolean isMapResetting() {
        return state == MatchState.MAP_RESETTING;
    }

    public boolean isTerrainRestoring() {
        return state == MatchState.TERRAIN_RESTORING;
    }

    public boolean isRestoringTerrain() {
        return state == MatchState.TERRAIN_RESTORING || state == MatchState.MAP_RESETTING;
    }

    public boolean isCombatActive() {
        return state == MatchState.PLAYING;
    }

    /** 开赛时应用房间规则；rules 为 null 时退回服务器配置。 */
    public void applyRoomRules(cn.blockforge.generated.generatedmod.lobby.RoomRules rules) {
        roomRules = rules == null
                ? cn.blockforge.generated.generatedmod.lobby.RoomRules.serverDefaults() : rules.normalized();
        if (roomRules.mode() == GameMode.SEARCH_DESTROY) teamCount = 2;
        teams.refreshConfigRules();
    }

    /** 比赛结束或启动失败时清除覆盖，恢复服务器配置。 */
    public void clearRoomRules() {
        teamCount = 2;
        roomTeamSelection = false;
        if (roomRules != null) {
            roomRules = null;
            teams.refreshConfigRules();
        }
    }

    public cn.blockforge.generated.generatedmod.lobby.RoomRules activeRoomRules() {
        return roomRules;
    }

    /** 当前生效模式；没有房间规则时按传统团队死斗处理。 */
    public GameMode rulesMode() {
        return roomRules == null ? GameMode.TEAM_DEATHMATCH : roomRules.mode();
    }

    /** 击杀目标只对团队死斗生效；歼灭类模式恒为 0（按回合结束条件判定）。 */
    public int rulesTargetKills() {
        if (roomRules == null) {
            return FpsTdmConfig.COMMON.targetKills.get();
        }
        return roomRules.mode().respawnRules() ? roomRules.targetKills() : 0;
    }

    public int rulesMatchDurationSeconds() {
        return roomRules != null ? roomRules.matchDurationSeconds()
                : FpsTdmConfig.COMMON.matchDurationSeconds.get();
    }

    public int rulesRoundWinTarget() {
        return rulesMode() == GameMode.SEARCH_DESTROY && roomRules != null ? roomRules.roundWinTarget() : 1;
    }

    public int rulesWarmupSeconds() {
        return 30;
    }

    public int rulesRespawnDelaySeconds() {
        return roomRules != null ? roomRules.respawnDelaySeconds()
                : FpsTdmConfig.COMMON.respawnDelaySeconds.get();
    }

    public int rulesRespawnProtectionSeconds() {
        return roomRules != null ? roomRules.respawnProtectionSeconds() : 3;
    }

    public int rulesRespawnProtectionPercent() {
        return roomRules != null ? roomRules.respawnProtectionPercent() : 80;
    }

    public int rulesBuyPhaseSeconds() {
        if (roomRules != null) return roomRules.buyPhaseSeconds();
        return economy.config().buyWindowSeconds();
    }

    public int rulesBombPlantTicks() {
        return (roomRules == null ? 4 : roomRules.bombPlantSeconds()) * 20;
    }

    public int rulesBombDetonationTicks() {
        return (roomRules == null ? 40 : roomRules.bombDetonationSeconds()) * 20;
    }

    public int rulesBombDefuseTicks() {
        return (roomRules == null ? 5 : roomRules.bombDefuseSeconds()) * 20;
    }

    public boolean rulesBombDefuseResume() {
        return roomRules != null && roomRules.bombDefuseResume();
    }

    public boolean isBombDefender(ServerPlayer player) {
        return player != null && rulesMode() == GameMode.SEARCH_DESTROY
                && teams.getTeam(player) == defendingTeam();
    }

    /** 爆破与歼灭模式整回合不复活；规则快照缺失时同样关闭。 */
    public boolean rulesAutoRespawn() {
        return rulesMode().respawnRules();
    }

    /** 歼灭类模式：阵亡会被拦截成“锁定在本回合”，而不是原版死亡。 */
    public boolean rulesElimination() {
        return rulesMode().elimination();
    }

    /** 爆破模式换边间隔（回合数）；0 表示不换边。 */
    public int rulesSwitchSideEvery() {
        return roomRules != null ? roomRules.switchSideEvery() : 0;
    }

    /** 本回合双方是否处于换边状态。 */
    public boolean sidesSwappedThisRound() {
        int interval = rulesSwitchSideEvery();
        return teamCount == 2 && rulesMode().roundSwapping() && interval > 0
                && roundNumber > 0 && ((roundNumber - 1) / interval) % 2 == 1;
    }

    /** 出生点归属：换边回合里两队互换出生区。 */
    private Team spawnGroupFor(Team team) {
        if (!team.isPlayable() || !sidesSwappedThisRound()) {
            return team;
        }
        return team.opposite();
    }

    public int rulesRoundEndDelaySeconds() {
        return roomRules != null ? roomRules.roundEndDelaySeconds()
                : FpsTdmConfig.COMMON.roundEndDelaySeconds.get();
    }

    public int rulesMatchEndDelaySeconds() {
        return roomRules != null ? roomRules.matchEndDelaySeconds()
                : FpsTdmConfig.COMMON.matchEndDelaySeconds.get();
    }

    public boolean rulesKeepInventoryOnDeath() {
        return rulesMode() != GameMode.SEARCH_DESTROY;
    }

    public boolean rulesSuppressDeathMessages() {
        return true;
    }

    public boolean rulesAutoReset() {
        return true;
    }

    public boolean rulesRestoreTerrainAfterRound() {
        return roomRules == null || roomRules.restoreTerrainAfterRound();
    }

    public PlayerPerspective rulesPerspective() {
        return roomRules == null ? PlayerPerspective.FIRST_PERSON : roomRules.perspective();
    }

    public boolean rulesAllowViewSwitch() {
        return roomRules == null || roomRules.allowViewSwitch();
    }

    public boolean rulesRequireBothTeams() {
        return false;
    }

    /** 比赛中途加入开关属于服务器大厅流程配置，房间不单独覆盖。 */
    public boolean rulesAllowJoinDuringMatch() {
        return FpsTdmConfig.COMMON.allowJoinDuringMatch.get();
    }

    /** 中途加入者是否先观战、下一回合再入场；歼灭类模式强制先观战。 */
    public boolean rulesJoinDuringMatchAsSpectator() {
        return rulesElimination() || FpsTdmConfig.COMMON.joinDuringMatchAsSpectator.get();
    }

    public boolean rulesFriendlyFire() {
        return roomRules != null ? roomRules.friendlyFire() : FpsTdmConfig.COMMON.friendlyFire.get();
    }

    public TeamChangePolicy rulesTeamChangePolicy() {
        return roomRules != null ? roomRules.teamChangePolicy()
                : FpsTdmConfig.COMMON.teamChangePolicy.get();
    }

    public AutoBalanceMode rulesAutoBalanceMode() {
        return roomRules != null ? roomRules.autoBalanceMode()
                : FpsTdmConfig.COMMON.autoBalanceMode.get();
    }

    public SpawnSelectionStrategy rulesSpawnStrategy() {
        SpawnSelectionStrategy strategy = roomRules != null ? roomRules.spawnSelectionStrategy()
                : FpsTdmConfig.COMMON.spawnSelectionStrategy.get();
        return strategy == SpawnSelectionStrategy.FARTHEST_FROM_ENEMIES ? SpawnSelectionStrategy.RANDOM : strategy;
    }

    public int rulesMaxTeamImbalance() {
        return roomRules != null ? roomRules.maxTeamImbalance()
                : FpsTdmConfig.COMMON.maxTeamImbalance.get();
    }

    public int phaseRemainingTicks() {
        if (phaseEndTick <= 0L) {
            return 0;
        }
        return Math.max(0, (int) Math.min(Integer.MAX_VALUE, phaseEndTick - server.getTickCount()));
    }

    /** Total duration of the current phase for HUD progress bars. */
    public int phaseTotalTicks() {
        return switch (state) {
            case WARMUP -> hudTicks(30);
            case FROZEN -> 100;
            case BUYING -> hudTicks(rulesBuyPhaseSeconds());
            case PLAYING -> hudTicks(rulesMatchDurationSeconds());
            case ROUND_END -> hudTicks(rulesRoundEndDelaySeconds());
            case MATCH_END -> hudTicks(rulesMatchEndDelaySeconds());
            case TERRAIN_RESTORING, MAP_RESETTING, WAITING -> 0;
        };
    }

    /** Total configured match duration for single-round modes. */
    private int matchTotalTicks() {
        if (rulesMode() == GameMode.SEARCH_DESTROY) {
            return 0;
        }
        return hudTicks(rulesMatchDurationSeconds());
    }

    private static int hudTicks(int seconds) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, seconds * 20L));
    }

    public int respawnRemainingTicks(ServerPlayer player) {
        DownedEntry entry = downedPlayers.get(player.getUUID());
        if (entry == null) {
            return 0;
        }
        return Math.max(0, (int) Math.min(Integer.MAX_VALUE, entry.releaseTick() - server.getTickCount()));
    }

    public boolean isDowned(ServerPlayer player) {
        return downedPlayers.containsKey(player.getUUID());
    }

    public StartResult startMatch() {
        return startMatch(null);
    }

    /** 启动比赛；roster 不为空时仅允许名单内在线玩家参赛。 */
    public StartResult startMatch(Collection<UUID> roster) {
        if (state != MatchState.WAITING) {
            return StartResult.ALREADY_ACTIVE;
        }
        if (maps.isResetting()) {
            return StartResult.MAP_BUSY;
        }
        if (maps.currentMap().isEmpty()) {
            return StartResult.NO_MAP;
        }
        if (!maps.currentMap().orElseThrow().isComplete()) {
            return StartResult.MAP_INCOMPLETE;
        }
        if (maps.isLoading()) {
            return StartResult.MAP_LOADING;
        }
        if (!maps.isReady()) {
            return StartResult.MAP_NOT_READY;
        }
        if (rulesSpawnStrategy() == SpawnSelectionStrategy.RANDOM && spawns.prepareRandomSpawnForMatch().isEmpty()) {
            return StartResult.NO_SAFE_RANDOM_SPAWN;
        }
        MapRegionActivation.Context startContext = new MapRegionActivation.Context(
                true, rulesMode(), 1, activeTeams().size(), teams.totalParticipants());
        if (rulesMode() == GameMode.SEARCH_DESTROY && (bomb == null || !bomb.hasBombSites())) {
            return StartResult.NO_BOMB_SITES;
        }
        if (rulesSpawnStrategy() != SpawnSelectionStrategy.RANDOM
                && activeTeams().stream().anyMatch(team -> spawns.findFixedSpawn(team, startContext).isEmpty())) {
            return StartResult.NO_TEAM_SPAWNS;
        }
        if (rulesTargetKills() <= 0 && rulesMatchDurationSeconds() <= 0) {
            return StartResult.NO_END_CONDITION;
        }

        if (roster != null) {
            teams.prepareRoster(roster);
        } else if (rulesAutoBalanceMode().balancesOnMatchStart()) {
            teams.balanceTeamsAtMatchStart();
        }
        if (teams.totalParticipants() == 0) {
            return StartResult.NO_PLAYERS;
        }

        captureRules();
        economy.beginMatch(server.getPlayerList().getPlayers());
        teams.prepareForMatch();
        if (records != null) {
            records.begin(maps.currentMap().orElse(null), rulesMode(), server.getPlayerList().getPlayers());
        }
        scores.resetMatch();
        matchStartTick = server.getTickCount();
        releaseAllDowned();
        regionReturnTicks.clear();
        buySpawnAnchors.clear();
        buyAreaNoticeSent.clear();
        killFeedSequence = 0;
        lastKillKiller = "";
        lastKillVictim = "";
        winner = null;
        roundWinner = null;
        pendingMatchWinner = null;
        roundNumber = 1;
        roundStartTick = 0L;
        teamAWins = 0;
        teamBWins = 0;
        extraWins.clear();
        resetFailureNotified = false;
        beginWarmup(true);
        recordEvent("比赛已创建，正在等待玩家进入热身。");
        return StartResult.STARTED;
    }

    public boolean stopMatch() {
        if (state == MatchState.WAITING) {
            return false;
        }
        stopAfterMapReset = true;
        if (state != MatchState.MAP_RESETTING) beginForcedStopReset();
        recordEvent("比赛正在停止，地图恢复完成后退出。");
        return true;
    }

    private void beginForcedStopReset() {
        state = MatchState.MAP_RESETTING;
        if (spectatorAreaPlayers != null) spectatorAreaPlayers.clear();
        if (spectateTargets != null) spectateTargets.clear();
        spawns.updateActivationContext(MapRegionActivation.INACTIVE);
        phaseEndTick = 0L;
        frozenPositions.clear();
        terrainRestoreAfterWarmup = false;
        terrainRestoreStarted = false;
        releaseAllDowned();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.setGameMode(GameType.SPECTATOR);
            player.setInvulnerable(true);
            player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        }
        if (maps.isResetting()) {
            broadcastMatchState();
            return;
        }
        if (!maps.beginReset()) {
            state = MatchState.ROUND_END;
            phaseEndTick = server.getTickCount() + 20L;
            broadcastSystemMessage("地图恢复尚未开始，服务器将在稍后重试。", false);
        }
        broadcastMatchState();
    }

    public StartResult restartMatch() {
        if (state != MatchState.WAITING) {
            resetToWaiting();
        }
        return startMatch();
    }

    public void resetToWaiting() {
        resetToWaiting(true);
    }

    private void resetToWaiting(boolean teleportToLobby) {
        finishRecord("比赛被重置");
        economy.abortMatch();
        if (bomb != null) bomb.cleanupMatchItems();
        spawns.updateActivationContext(MapRegionActivation.INACTIVE);
        teamCount = 2;
        roomTeamSelection = false;
        extraWins.clear();
        releaseAllDowned();
        for (ServerPlayer player : new ArrayList<>(server.getPlayerList().getPlayers())) {
            player.setInvulnerable(false);
            healAndReady(player);
            if (teleportToLobby) spawns.teleportToLobby(player);
        }
        teams.resetAll();
        clearRoomRules();
        restoreRules();
        scores.resetMatch();
        matchStartTick = 0L;
        releaseAllDowned();
        regionReturnTicks.clear();
        buySpawnAnchors.clear();
        buyAreaNoticeSent.clear();
        if (spectatorAreaPlayers != null) spectatorAreaPlayers.clear();
        winner = null;
        roundWinner = null;
        pendingMatchWinner = null;
        phaseEndTick = 0L;
        roundNumber = 0;
        teamAWins = 0;
        teamBWins = 0;
        resetFailureNotified = false;
        stopAfterMapReset = false;
        state = MatchState.WAITING;
        broadcastMatchState();
    }

    public void tick() {
        spawns.updateActivationContext(new MapRegionActivation.Context(
                isMatchActive(), rulesMode(), isMatchActive() ? roundNumber : 0,
                activeTeams().size(), teams.totalParticipants()));
        processVanillaDeaths();
        if (bomb != null) bomb.tick();
        // 快照捕获和恢复必须在 Dedicated Server 主线程的 tick 中推进。
        maps.tick();
        rooms.tick();
        mapEditor.tick();

        if (state == MatchState.WARMUP) {
            int minimum = roomRules == null ? 2 : Math.max(2, roomRules.minPlayersToStart());
            long previous = phaseEndTick;
            phaseEndTick = warmupDeadline(previous, teams.totalParticipants(), minimum, server.getTickCount());
            if (phaseEndTick != previous) {
                if (phaseEndTick == 0L) {
                    recordEvent("人数不足，继续热身等待。");
                } else {
                    recordEvent("人数已满足，比赛将在 30 秒后开始。");
                    publishHudEvent(MatchHudEventType.START_COUNTDOWN,
                            "人数已满足，比赛将在 30 秒后开始", 600);
                }
                broadcastMatchState();
            }
        }
        if (state == MatchState.WARMUP && phaseEndTick > 0L && server.getTickCount() >= phaseEndTick) {
            beginTerrainRestore(true);
        } else if (state == MatchState.FROZEN && phaseEndTick > 0L
                && server.getTickCount() >= phaseEndTick) {
            beginPlaying(false);
        } else if (state == MatchState.BUYING && phaseEndTick > 0L
                && server.getTickCount() >= phaseEndTick) {
            beginPlaying(false);
        } else if (state == MatchState.PLAYING && phaseEndTick > 0L
                && server.getTickCount() >= phaseEndTick && !bombKeepsRoundAlive()) {
            finishRound(null);
        } else if (state == MatchState.ROUND_END && phaseEndTick > 0L
                && server.getTickCount() >= phaseEndTick) {
            beginMapReset();
        } else if (state == MatchState.TERRAIN_RESTORING && maps.isResetComplete()) {
            completeTerrainRestore();
        } else if (state == MatchState.TERRAIN_RESTORING && maps.isResetFailed()
                && (phaseEndTick == 0L || server.getTickCount() >= phaseEndTick)) {
            retryTerrainRestore();
        } else if (state == MatchState.TERRAIN_RESTORING && phaseEndTick > 0L
                && server.getTickCount() >= phaseEndTick) {
            retryTerrainRestore();
        } else if (state == MatchState.MAP_RESETTING && maps.isResetComplete()) {
            completeForcedMapReset();
        } else if (state == MatchState.MAP_RESETTING && maps.isResetFailed()) {
            retryForcedMapReset();
        } else if (state == MatchState.MATCH_END && phaseEndTick > 0L
                && server.getTickCount() >= phaseEndTick
                && rulesAutoReset()) {
            resetToWaiting();
        }

        spawns.tickRandomSpawns(state == MatchState.PLAYING || state == MatchState.BUYING
                || state == MatchState.WARMUP, rulesSpawnStrategy());
        processDownedPlayers();
        economy.tick(server.getPlayerList().getPlayers());
        enforceArenaRules();
        updateFrozenPlayers();
        if (server.getTickCount() % stateBroadcastInterval(isMatchActive()) == 0) {
            broadcastMatchState();
        }
    }

    static long warmupDeadline(long deadline, int players, int minimum, long now) {
        if (players < Math.max(2, minimum)) return 0L;
        return deadline == 0L ? now + 600L : deadline;
    }

    static int stateBroadcastInterval(boolean matchActive) {
        return matchActive ? 10 : 100;
    }

    private void teleportFrozen(ServerPlayer player, SpawnPoint hold) {
        ServerLevel level = server.getLevel(hold.dimension());
        if (level == null) player.teleportTo(hold.x(), hold.y(), hold.z());
        else player.teleportTo(level, hold.x(), hold.y(), hold.z(), hold.yaw(), hold.pitch());
    }

    private void updateFrozenPlayers() {
        boolean freeze = state == MatchState.TERRAIN_RESTORING || state == MatchState.FROZEN;
        if (!freeze) {
            frozenPositions.clear();
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SpawnPoint hold = frozenPositions.computeIfAbsent(player.getUUID(),
                    ignored -> new SpawnPoint(player.level().dimension(), player.getX(), player.getY(),
                            player.getZ(), player.getYRot(), player.getXRot()));
            teleportFrozen(player, hold);
            player.setGameMode(GameType.SPECTATOR);
            player.setInvulnerable(true);
            player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        }
    }

    /** Waiting players cannot take damage; active players use the real death pipeline. */
    public boolean handleFatalDamage(ServerPlayer victim, DamageSource source, float amount) {
        return isDowned(victim);
    }

    /** Damage multiplier ramps from the configured reduction back to full damage after respawn. */
    public float applyRespawnProtection(ServerPlayer player, float amount) {
        if (player == null || amount <= 0 || !isMatchActive()) return amount;
        long end = respawnProtectionEnds.getOrDefault(player.getUUID(), 0L);
        long now = server.getTickCount();
        if (end <= now) {
            respawnProtectionEnds.remove(player.getUUID());
            return amount;
        }
        int duration = Math.max(1, rulesRespawnProtectionSeconds()) * 20;
        double left = Math.min(duration, Math.max(0, end - now));
        double reduction = rulesRespawnProtectionPercent() / 100.0 * left / duration;
        return (float) (amount * (1.0 - reduction));
    }

    /** Queue real deaths without changing the player while other death listeners run. */
    public void onPlayerKilled(ServerPlayer victim, DamageSource source) {
        onPlayerKilled(victim, source, () -> false);
    }

    public void onPlayerKilled(ServerPlayer victim, DamageSource source,
                               java.util.function.BooleanSupplier cancelled) {
        if (bomb != null) bomb.onPlayerDeath(victim);
        if (!state.isActive()) {
            return;
        }
        Team victimTeam = teams.getTeam(victim);
        if (!victimTeam.isPlayable()) {
            return;
        }
        // Death events are cancellable. Confirm after the entire death call has returned.
        pendingDeaths.putIfAbsent(victim.getUUID(), new PendingDeath(victim, source, victimTeam, cancelled));
    }

    private record PendingDeath(ServerPlayer victim, DamageSource source, Team team,
                                java.util.function.BooleanSupplier cancelled) { }

    private void processVanillaDeaths() {
        for (PendingDeath death : new ArrayList<>(pendingDeaths.values())) {
            pendingDeaths.remove(death.victim().getUUID());
            if (death.cancelled().getAsBoolean() || !death.victim().isDeadOrDying()) continue;
            if (state == MatchState.PLAYING && !isDowned(death.victim())) {
                ServerPlayer killer = resolveKiller(death.source());
                String label = killer == null ? "环境伤害" : killer == death.victim() ? "自身伤害"
                        : "击杀者  " + killer.getGameProfile().getName();
                scheduleDowned(death.victim(), death.team(), label);
                creditKill(death.victim(), death.team(), death.source());
            }
            ServerPlayer current = server.getPlayerList().getPlayer(death.victim().getUUID());
            if (current != null && current.isDeadOrDying()) {
                // The vanilla command handler replaces connection.player and performs all sync.
                // Calling PlayerList.respawn alone leaves the connection pointing at the old entity.
                current.connection.handleClientCommand(new net.minecraft.network.protocol.game.ServerboundClientCommandPacket(
                        net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
            }
        }
        for (UUID id : new ArrayList<>(respawnedPlayers)) {
            respawnedPlayers.remove(id);
            ServerPlayer current = server.getPlayerList().getPlayer(id);
            if (current != null && current.isAlive()) applyRespawnState(current);
        }
    }

    /** 统一处理击杀记分、公告与回合终点判定。返回 true 表示回合已结束。 */
    private boolean creditKill(ServerPlayer victim, Team victimTeam, DamageSource source) {
        boundaryCountdown.update(victim.getUUID(), false, server.getTickCount());
        spawns.refreshRandomSpawnsAfterDeath();
        ServerPlayer killer = resolveKiller(source);
        Team killerTeam = killer == null ? Team.SPECTATOR : teams.getTeam(killer);
        if (killer != null && killer != victim && killerTeam.isPlayable() && killerTeam != victimTeam) {
            scores.addKill(killerTeam, killer, victim);
            economy.recordKill(killer, victim);
            publishKill(killer, victim);
            if (rulesElimination() && finishEliminationIfDecided(victimTeam, victim)) {
                return true;
            }
            int target = rulesTargetKills();
            if (target > 0 && scores.getTeamScore(killerTeam) >= target) {
                finishRound(killerTeam);
                return true;
            }
            return false;
        }
        scores.addDeath(victim);
        economy.recordKill(victim, victim);
        if (rulesElimination() && finishEliminationIfDecided(victimTeam, victim)) {
            return true;
        }
        return false;
    }

    private boolean finishEliminationIfDecided(Team victimTeam, ServerPlayer victim) {
        if (!isTeamWipedOut(victimTeam, victim)) return false;
        if (rulesMode() == GameMode.SEARCH_DESTROY && victimTeam == attackingTeam()
                && (bomb.state().phase() == ClassicBombState.Phase.PLANTED
                || bomb.state().phase() == ClassicBombState.Phase.DEFUSING)) {
            return false;
        }
        var survivors = activeTeams().stream().filter(team -> team != victimTeam && aliveCount(team) > 0).toList();
        if (survivors.size() > 1) return false;
        finishRound(survivors.isEmpty() ? null : survivors.get(0), false);
        return true;
    }

    /**
     * 记录一次对敌方玩家的有效伤害（护甲结算后、非致命也计）：
     * 供 HUD「对敌人伤害 / 承受伤害 / 队伍伤害」等统计源使用。
     */
    public void recordDamage(ServerPlayer victim, DamageSource source, float amount) {
        if (state != MatchState.PLAYING || amount <= 0.0F) {
            return;
        }
        Team victimTeam = teams.getTeam(victim);
        if (!victimTeam.isPlayable()) {
            return;
        }
        ServerPlayer attacker = resolveKiller(source);
        if (attacker == null || attacker == victim) {
            return;
        }
        Team attackerTeam = teams.getTeam(attacker);
        if (!attackerTeam.isPlayable() || attackerTeam == victimTeam) {
            return;
        }
        scores.addDamage(attackerTeam, attacker, victim, Math.max(1, Math.round(amount)));
        economy.recordDamage(attacker, amount);
    }

    /** 整场已进行时间（tick）；未在整场比赛中时为 0。 */
    public int matchElapsedTicks() {
        return matchStartTick == 0L ? 0 : (int) Math.max(0L, server.getTickCount() - matchStartTick);
    }

    /** 歼灭判定：除本次受害者外，该队是否已没有任何可战人员。 */
    private boolean isTeamWipedOut(Team team, ServerPlayer lastVictim) {
        if (team == null || !team.isPlayable()) {
            return false;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player == lastVictim || teams.getTeam(player) != team || teams.isPending(player)) {
                continue;
            }
            if (!isDowned(player) && player.isAlive()) {
                return false;
            }
        }
        return true;
    }

    /** Count only online combatants; queued and vanilla-dead players cannot keep a team alive. */
    private int aliveCount(Team team) {
        return (int) server.getPlayerList().getPlayers().stream()
                .filter(player -> teams.getTeam(player) == team && player.isAlive()
                        && !teams.isPending(player) && !isDowned(player)).count();
    }

    /** 击杀反馈：击杀者音效与 ActionBar、受害者提示，并更新全员击杀公告。 */
    private void publishKill(ServerPlayer killer, ServerPlayer victim) {
        killFeedSequence = killFeedSequence == Integer.MAX_VALUE ? 1 : killFeedSequence + 1;
        lastKillKiller = killer.getGameProfile().getName();
        lastKillVictim = victim.getGameProfile().getName();
        recordEvent(lastKillKiller + " 击杀 " + lastKillVictim + "（" + scoreSummary() + "）");
        killer.displayClientMessage(Component.literal("击杀 " + lastKillVictim
                + "    " + scoreSummary()), true);
        victim.displayClientMessage(Component.literal("你被 " + lastKillKiller + " 击杀"), true);
    }

    public void handlePlayerRespawn(ServerPlayer player) {
        // Forge fires this before the vanilla command handler finishes replacing its player.
        respawnedPlayers.add(player.getUUID());
    }

    private void applyRespawnState(ServerPlayer player) {
        if (player.connection.player != player) {
            respawnedPlayers.add(player.getUUID());
            return;
        }
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new MatchRespawnEvent(player,
                MatchRespawnEvent.Phase.REBUILT, server.getTickCount()));
        if (isMatchActive()) {
            teams.rememberGameMode(player);
        }
        Team team = teams.getTeam(player);
        if (state == MatchState.PLAYING && isDowned(player)) {
            DownedEntry entry = downedPlayers.get(player.getUUID());
            markDownedBody(player);
            if (entry != null) {
                player.teleportTo(entry.x(), entry.y(), entry.z());
                player.setYRot(entry.yaw());
                player.setXRot(entry.pitch());
            }
            broadcastMatchState();
            return;
        }
        downedPlayers.remove(player.getUUID());
        restoreBody(player);
        readyRespawnRequests.remove(player.getUUID());
        respawnProtectionEnds.remove(player.getUUID());
        if (state == MatchState.PLAYING && team.isPlayable() && !teams.isPending(player)
                && rulesElimination()) {
            // 歼灭类模式：原版死亡后复活也保持“本回合阵亡”，锁定到回合结束。
            scheduleDowned(player, team);
            player.setGameMode(GameType.SPECTATOR);
            player.setInvulnerable(true);
            broadcastMatchState();
            return;
        }
        if (state == MatchState.MAP_RESETTING) {
            player.setGameMode(GameType.SPECTATOR);
            player.setInvulnerable(true);
            sendToSpectatorArea(player);
            return;
        }
        if (state == MatchState.TERRAIN_RESTORING) {
            player.setGameMode(GameType.SPECTATOR);
            player.setInvulnerable(true);
            spawns.teleportToTerrainRestoreHold(player);
            return;
        }
        if (state == MatchState.ROUND_END || state == MatchState.MATCH_END) {
            player.setGameMode(GameType.SPECTATOR);
            player.setInvulnerable(true);
            sendToSpectatorArea(player);
            return;
        }
        if (isMatchActive() && team.isPlayable() && !teams.isPending(player)
                && (state == MatchState.PLAYING || state == MatchState.WARMUP)) {
            // 原版死亡兜底：复活必须回到本队出生点，而不是世界出生点。
            player.setGameMode(GameType.SURVIVAL);
            player.setInvulnerable(false);
            healAndReady(player);
            spawns.teleportToTeamSpawn(player, spawnGroupFor(team), rulesSpawnStrategy());
            return;
        }
        if (isMatchActive() && (!team.isPlayable() || teams.isPending(player))) {
            sendToSpectatorArea(player);
        }
    }

    /**
     * 中途通过大厅加入进行中的匹配比赛：分配到人少的一队并传送到本队出生点；
     * 地图恢复间隙或无法平衡分队时先观战，下一回合自动激活。
     */
    public void joinRoomMember(ServerPlayer player) {
        if (!isMatchActive()) {
            return;
        }
        teams.rememberGameMode(player);
        if (state == MatchState.MAP_RESETTING || state == MatchState.TERRAIN_RESTORING) {
            teams.setPending(player);
            if (state == MatchState.TERRAIN_RESTORING) {
                player.setGameMode(GameType.SPECTATOR);
                player.setInvulnerable(true);
                spawns.teleportToTerrainRestoreHold(player);
            } else {
                sendToSpectatorArea(player);
            }
            broadcastMatchState();
            return;
        }
        Team team = teams.forceJoinDuringMatch(player);
        broadcastMatchState();
        if (team == null || !team.isPlayable()) {
            sendToSpectatorArea(player);
            return;
        }
        if (state != MatchState.WARMUP && rulesJoinDuringMatchAsSpectator()) {
            // 歼灭类回合不允许“带着上一回合的血条插队”，下一回合再入场。
            teams.setPending(player);
            sendToSpectatorArea(player);
            return;
        }
        leaveSpectatorArea(player);
        healAndReady(player);
        spawns.teleportToTeamSpawn(player, spawnGroupFor(team), rulesSpawnStrategy());
        if (records != null) records.addParticipant(player);
        String joinMessage = player.getGameProfile().getName() + " 中途加入了比赛（"
                + team.displayName() + "）。";
        recordEvent(joinMessage);
        broadcastSystemMessage(joinMessage, false);
    }

    public void onPlayerLogin(ServerPlayer player) {
        teams.restoreGameMode(player);
        if (isMatchActive()) {
            teams.rememberGameMode(player);
        }
        if (state == MatchState.MAP_RESETTING) {
            player.setGameMode(GameType.SPECTATOR);
            player.setInvulnerable(true);
            sendToSpectatorArea(player);
        } else if (isMatchActive() && teams.getTeam(player) == Team.SPECTATOR) {
            player.setGameMode(GameType.SPECTATOR);
            player.setInvulnerable(true);
        }
        sendMatchSync(player);
        FpsTdmConfigController.sendCurrent(player, "", false);
        rooms.sendSync(player, "", false);
        rooms.sendLobbyConfig(player, "", false);
        mapEditor.sendView(player);
    }

    public void onPlayerLogout(ServerPlayer player) {
        boundaryCountdown.update(player.getUUID(), false, server.getTickCount());
        downedPlayers.remove(player.getUUID());
        readyRespawnRequests.remove(player.getUUID());
        respawnProtectionEnds.remove(player.getUUID());
        regionReturnTicks.remove(player.getUUID());
        frozenPositions.remove(player.getUUID());
        teams.clearPlayer(player);
        rooms.onLogout(player);
        mapEditor.onLogout(player);
        if (bomb != null) bomb.onPlayerLogout(player);
        broadcastMatchState();
    }

    public boolean shouldCancelAttack(LivingEntity target, DamageSource source) {
        if (state == MatchState.TERRAIN_RESTORING) {
            return true;
        }
        if (state == MatchState.MAP_RESETTING) {
            return target instanceof ServerPlayer || resolveKiller(source) != null;
        }
        ServerPlayer sourcePlayer = resolveKiller(source);
        if (sourcePlayer != null && isDowned(sourcePlayer)) {
            return true;
        }
        if (!(target instanceof ServerPlayer victim)) {
            return false;
        }
        if (isDowned(victim)) {
            return true;
        }
        Team victimTeam = teams.getTeam(victim);
        if (isMatchActive() && (!victimTeam.isPlayable() || teams.isPending(victim))) {
            return true;
        }
        if (state == MatchState.WARMUP) {
            ServerPlayer attacker = resolveKiller(source);
            if (attacker == null) return false;
            Team attackerTeam = teams.getTeam(attacker);
            return attackerTeam.isPlayable() && attackerTeam == victimTeam && !rulesFriendlyFire();
        }
        if (state != MatchState.PLAYING) {
            return isMatchActive() && victimTeam.isPlayable();
        }
        ServerPlayer attacker = resolveKiller(source);
        if (attacker == null) {
            return false;
        }
        if (isDowned(attacker)) {
            return true;
        }
        Team attackerTeam = teams.getTeam(attacker);
        return attackerTeam.isPlayable() && attackerTeam == victimTeam
                && !rulesFriendlyFire();
    }

    public boolean shouldCancelInteraction(ServerPlayer player) {
        return arePlayersFrozen() || state == MatchState.MAP_RESETTING || isDowned(player)
                || (isMatchActive() && (!teams.getTeam(player).isPlayable() || teams.isPending(player)));
    }

    /** 仅在 MAP_RESETTING 阶段保护当前地图 resetRegion 内的方块。 */
    public boolean shouldProtectBlock(LevelAccessor level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        return shouldProtectOutside(serverLevel, pos)
                || (isRestoringTerrain() && maps.isInResetRegion(serverLevel, pos));
    }

    public boolean shouldProtectOutside(ServerLevel level, BlockPos pos) {
        return isMatchActive() && maps.currentMap().filter(map -> map.hasBounds()
                && map.world().equals(level.dimension()) && !map.bounds().contains(pos)).isPresent();
    }

    public boolean shouldCancelBlockAction(ServerPlayer player) {
        return shouldCancelInteraction(player);
    }

    private boolean arePlayersFrozen() {
        return state == MatchState.TERRAIN_RESTORING || state == MatchState.FROZEN;
    }

    /** 重置阶段过滤当前地图区域内的爆炸方块，保留其他区域的服务器行为。 */
    public void removeResetRegionExplosionBlocks(ExplosionEvent.Detonate event) {
        if (!isMatchActive() || !(event.getLevel() instanceof ServerLevel level)
                || maps.currentMap().isEmpty()
                || !maps.currentMap().get().world().equals(level.dimension())) {
            return;
        }
        event.getAffectedBlocks().removeIf(pos -> shouldProtectBlock(level, pos));
    }

    public void sendMatchSync(ServerPlayer player) {
        sendMatchSync(player, teams.counts(), teamStats());
    }

    private java.util.List<TeamMatchStats> teamStats() {
        return activeTeams().stream().map(team -> new TeamMatchStats(team, scores.getTeamScore(team),
                roundWins(team), teams.teamSize(team), scores.getTeamMatchKills(team), scores.getTeamDamage(team))).toList();
    }

    private void sendMatchSync(ServerPlayer player, TeamManager.Counts counts, java.util.List<TeamMatchStats> stats) {
        MatchSyncPacket packet = new MatchSyncPacket(
                state,
                scores.getTeamScore(Team.TEAM_A),
                scores.getTeamScore(Team.TEAM_B),
                teamAWins,
                teamBWins,
                roundNumber,
                rulesTargetKills(),
                phaseRemainingTicks(),
                respawnRemainingTicks(player),
                teams.getTeam(player),
                counts.teamA(),
                counts.teamB(),
                counts.spectators(),
                teams.isPending(player),
                winner,
                killFeedSequence,
                lastKillKiller,
                lastKillVictim,
                rulesMode().ordinal(),
                rulesRoundWinTarget(),
                scores.getMatchKills(player.getUUID()),
                scores.getMatchDeaths(player.getUUID()),
                scores.getDamageDealt(player.getUUID()),
                scores.getDamageTaken(player.getUUID()),
                scores.getTeamDamage(Team.TEAM_A),
                scores.getTeamDamage(Team.TEAM_B),
                scores.getTeamMatchKills(Team.TEAM_A),
                scores.getTeamMatchKills(Team.TEAM_B),
                matchElapsedTicks()).withTeamStats(stats)
                .withTimers(boundaryCountdown.remaining(player.getUUID(), server.getTickCount()),
                        (int) secondsToTicks(rulesRespawnDelaySeconds()))
                .withBoundaryStatus(boundaryCountdown.isOutside(player.getUUID()))
                .withRespawn(state == MatchState.PLAYING && isDowned(player),
                        isDowned(player) ? downedPlayers.get(player.getUUID()).deathLabel() : "")
                .withDownedPlayers(downedPlayers.keySet())
                .withEconomy(economy.enabled(), economy.globalBalance(player.getUUID()),
                        economy.matchBalance(player.getUUID()))
                .withViewSettings(rulesPerspective(), rulesAllowViewSwitch())
                .withTimeline(phaseTotalTicks(), matchTotalTicks());
        maps.currentMap().ifPresent(map -> packet.withBoundaryBox(map.bounds()));
        FpsTdmNetwork.sendToPlayer(packet, player);
    }

    public void broadcastMatchState() {
        TeamManager.Counts counts = teams.counts();
        var stats = teamStats();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendMatchSync(player, counts, stats);
        }
    }

    public String statusLine() {
        return "状态=" + state
                + "，地图=" + maps.currentMapId()
                + "，回合=" + roundNumber
                + "，比分=" + scoreSummary()
                + "，胜场=" + activeTeams().stream().map(team -> Integer.toString(roundWins(team))).collect(java.util.stream.Collectors.joining(":"))
                + "，剩余=" + formatTicks(phaseRemainingTicks())
                + "，队伍=" + activeTeams().stream().map(team -> Integer.toString(teams.teamSize(team))).collect(java.util.stream.Collectors.joining(":"))
                + "，观战=" + teams.spectatorSize();
    }

    private void beginWarmup(boolean firstRound) {
        spawns.updateActivationContext(new MapRegionActivation.Context(
                true, rulesMode(), roundNumber, activeTeams().size(), teams.totalParticipants()));
        if (!firstRound) {
            teams.activatePendingPlayers();
            if (rulesAutoBalanceMode().balancesOnMatchStart()) {
                teams.balanceTeamsAtMatchStart();
            }
        }
        scores.resetRound();
        if (bomb != null) bomb.cleanupRound();
        releaseAllDowned();
        state = MatchState.WARMUP;
        roundWinner = null;
        phaseEndTick = 0L;
        SpawnSelectionStrategy strategy = rulesSpawnStrategy();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Team team = teams.getTeam(player);
            teams.rememberGameMode(player);
            if (team.isPlayable() && !teams.isPending(player)) {
                leaveSpectatorArea(player);
                player.setGameMode(GameType.SURVIVAL);
                player.setInvulnerable(false);
                healAndReady(player);
                spawns.teleportToTeamSpawn(player, spawnGroupFor(team), strategy);
            } else if (isMatchActive()) {
                sendToSpectatorArea(player);
            }
        }
        recordEvent("热身阶段开始，等待足够玩家加入。"
                + (sidesSwappedThisRound() ? "（本回合双方换边）" : ""));
        broadcastMatchState();
    }

    private void beginFrozen() {
        spawns.updateActivationContext(new MapRegionActivation.Context(
                true, rulesMode(), roundNumber, activeTeams().size(), teams.totalParticipants()));
        scores.resetRound();
        state = MatchState.FROZEN;
        phaseEndTick = server.getTickCount() + 100L;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Team team = teams.getTeam(player);
            if (team.isPlayable() && !teams.isPending(player)) {
                leaveSpectatorArea(player);
                player.setGameMode(GameType.SURVIVAL);
                player.setInvulnerable(true);
                player.setDeltaMovement(0.0D, 0.0D, 0.0D);
                healAndReady(player);
                spawns.teleportToTeamSpawn(player, spawnGroupFor(team), rulesSpawnStrategy());
            }
        }
        recordEvent("冻结阶段开始，5 秒后进入行动阶段。");
        publishHudEvent(MatchHudEventType.FREEZE_START, "5 秒后进入行动阶段", 100);
        broadcastMatchState();
    }

    private void beginPlaying() {
        beginPlaying(true);
    }

    private void beginPlaying(boolean teleportToSpawn) {
        spawns.updateActivationContext(new MapRegionActivation.Context(
                true, rulesMode(), roundNumber, activeTeams().size(), teams.totalParticipants()));
        if (rulesAutoBalanceMode().balancesOnMatchStart()) teams.balanceTeamsAtMatchStart();
        teams.activatePendingPlayers();
        scores.resetRound();
        state = MatchState.PLAYING;
        roundStartTick = server.getTickCount();
        phaseEndTick = rulesMatchDurationSeconds() <= 0
                ? 0L
                : server.getTickCount() + secondsToTicks(rulesMatchDurationSeconds());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Team team = teams.getTeam(player);
            if (team.isPlayable() && !teams.isPending(player)) {
                leaveSpectatorArea(player);
                player.setGameMode(GameType.SURVIVAL);
                player.setInvulnerable(false);
                healAndReady(player);
                SpawnSelectionStrategy actionSpawn = rulesMode() == GameMode.SEARCH_DESTROY
                        && spawns.findFixedSpawn(spawnGroupFor(team)).isPresent()
                        ? SpawnSelectionStrategy.SEQUENTIAL : rulesSpawnStrategy();
                if (teleportToSpawn) {
                    spawns.teleportToTeamSpawn(player, spawnGroupFor(team), actionSpawn);
                }
            }
        }
        if (rulesMode() == GameMode.SEARCH_DESTROY && bomb != null && !bomb.startRound()) {
            finishRound(defendingTeam(), false);
            return;
        }
        String actionText = switch (rulesMode()) {
            case SEARCH_DESTROY -> "行动阶段开始，回合 " + roundNumber + "！";
            case TEAM_DEATHMATCH -> "团队竞技开始！";
            case LAST_STANDING -> "歼灭竞技开始！";
        };
        recordEvent(actionText);
        publishHudEvent(MatchHudEventType.ACTION_PHASE,
                "回合 " + roundNumber + " · " + rulesMode().displayName(), 70);
        playPhaseSound();
        broadcastMatchState();
    }

    private void beginBuying() {
        if (rulesBuyPhaseSeconds() <= 0) {
            beginPlaying(true);
            return;
        }
        spawns.updateActivationContext(new MapRegionActivation.Context(
                true, rulesMode(), roundNumber, activeTeams().size(), teams.totalParticipants()));
        if (rulesAutoBalanceMode().balancesOnMatchStart()) teams.balanceTeamsAtMatchStart();
        teams.activatePendingPlayers();
        scores.resetRound();
        state = MatchState.BUYING;
        buyAreaNoticeSent.clear();
        roundStartTick = 0L;
        phaseEndTick = server.getTickCount() + secondsToTicks(rulesBuyPhaseSeconds());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Team team = teams.getTeam(player);
            teams.rememberGameMode(player);
            if (team.isPlayable() && !teams.isPending(player)) {
                leaveSpectatorArea(player);
                player.setGameMode(GameType.SURVIVAL);
                player.setInvulnerable(true);
                healAndReady(player);
                SpawnSelectionStrategy buySpawn = spawns.findFixedSpawn(spawnGroupFor(team)).isPresent()
                        ? SpawnSelectionStrategy.SEQUENTIAL : rulesSpawnStrategy();
                spawns.teleportToTeamSpawn(player, spawnGroupFor(team), buySpawn);
                buySpawnAnchors.put(player.getUUID(), new SpawnPoint(player.level().dimension(),
                        player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
            } else if (isMatchActive()) {
                sendToSpectatorArea(player);
            }
        }
        int buyTicks = Math.max(1, rulesBuyPhaseSeconds() * 20);
        recordEvent("购买阶段开始：只能在出生区域活动，准备装备。");
        publishHudEvent(MatchHudEventType.BUY_PHASE_START,
                "购买并准备装备 · " + rulesBuyPhaseSeconds() + " 秒", buyTicks);
        publishHudEvent(MatchHudEventType.BUY_AREA_ONLY,
                "购买期间只能在出生区域活动", buyTicks);
        playPhaseSound();
        broadcastMatchState();
    }

    /** CS-style buy phase: warmup is always open, combat has a configurable opening window. */
    public boolean isBuyPhaseOpen(int buyWindowSeconds) {
        if (state == MatchState.WARMUP) return true;
        if (rulesMode() == GameMode.SEARCH_DESTROY) return state == MatchState.BUYING;
        if (state != MatchState.PLAYING || roundStartTick <= 0L) return false;
        return server.getTickCount() - roundStartTick <= Math.max(0, buyWindowSeconds) * 20L;
    }

    public int buyPhaseRemainingSeconds(int buyWindowSeconds) {
        if (state == MatchState.WARMUP) return Integer.MAX_VALUE;
        if (state == MatchState.BUYING) return secondsRemaining(phaseEndTick);
        if (state != MatchState.PLAYING || roundStartTick <= 0L) return 0;
        long elapsed = Math.max(0L, server.getTickCount() - roundStartTick);
        long remaining = Math.max(0L, Math.max(0, buyWindowSeconds) * 20L - elapsed);
        return (int) ((remaining + 19L) / 20L);
    }

    private void finishRound(Team requestedWinner) {
        finishRound(requestedWinner, true);
    }

    void finishRound(Team requestedWinner, boolean resolveOnTimeout) {
        if (state != MatchState.PLAYING) {
            return;
        }
        Team resolvedWinner = requestedWinner;
        if (resolvedWinner == null && resolveOnTimeout) {
            resolvedWinner = resolveRoundWinnerOnTimeout();
        }
        roundWinner = resolvedWinner;
        Map<UUID, Team> combatants = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Team team = teams.getTeam(player);
            if (team.isPlayable()) combatants.put(player.getUUID(), team);
        }
        economy.finishRound(resolvedWinner, combatants);
        if (resolvedWinner == Team.TEAM_A) {
            teamAWins++;
        } else if (resolvedWinner == Team.TEAM_B) {
            teamBWins++;
        } else if (resolvedWinner != null && resolvedWinner.isPlayable()) {
            extraWins.merge(resolvedWinner, 1, Integer::sum);
        }
        int requiredWins = rulesRoundWinTarget();
        pendingMatchWinner = activeTeams().stream().filter(team -> roundWins(team) >= requiredWins).findFirst().orElse(null);
        state = MatchState.ROUND_END;
        if (bomb != null) bomb.cleanupRound();
        phaseEndTick = server.getTickCount() + secondsToTicks(rulesRoundEndDelaySeconds());
        resetFailureNotified = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            clearSpectatorCamera(player);
            player.setInvulnerable(true);
            if (teams.getTeam(player).isPlayable()) {
                player.setGameMode(GameType.SPECTATOR);
                sendToSpectatorArea(player);
            }
        }
        if (resolvedWinner == null) {
            String message = "第 " + roundNumber + " 回合平局，比分 " + scoreSummary() + "。";
            recordEvent(message);
            publishHudEvent(MatchHudEventType.ROUND_DRAW, "第 " + roundNumber + " 回合没有队伍获胜", 80);
        } else {
            recordEvent(resolvedWinner.displayName() + " 赢得第 " + roundNumber + " 回合。");
            markWinnerParticles(resolvedWinner);
        }
        broadcastMatchState();
    }

    /** 回合超时裁定：歼灭类模式先比存活人数、再比回合击杀；死斗直接比回合击杀。 */
    private Team resolveRoundWinnerOnTimeout() {
        if (rulesMode() == GameMode.SEARCH_DESTROY) {
            return bomb.state().phase() == ClassicBombState.Phase.PLANTED
                    || bomb.state().phase() == ClassicBombState.Phase.DEFUSING
                    ? attackingTeam() : defendingTeam();
        }
        return resolveWinner(activeTeams(), rulesElimination() ? this::aliveCount : team -> 0, scores::getTeamScore);
    }

    /** Once C4 is planted, the round clock stops deciding the result. */
    private boolean bombKeepsRoundAlive() {
        return rulesMode() == GameMode.SEARCH_DESTROY && bomb != null
                && (bomb.state().phase() == ClassicBombState.Phase.PLANTED
                || bomb.state().phase() == ClassicBombState.Phase.DEFUSING);
    }

    private String scoreSummary() {
        return activeTeams().stream().map(team -> team.displayName() + " " + scores.getTeamScore(team))
                .collect(java.util.stream.Collectors.joining(" : "));
    }

    public static Team resolveWinner(java.util.List<Team> candidates, java.util.function.ToIntFunction<Team> primary,
                                     java.util.function.ToIntFunction<Team> secondary) {
        Team best = null;
        int first = Integer.MIN_VALUE, second = Integer.MIN_VALUE;
        boolean tied = false;
        for (Team team : candidates) {
            int a = primary.applyAsInt(team), b = secondary.applyAsInt(team);
            if (a > first || a == first && b > second) {
                best = team; first = a; second = b; tied = false;
            } else if (a == first && b == second) tied = true;
        }
        return tied ? null : best;
    }

    private void beginMapReset() {
        if (state != MatchState.ROUND_END) {
            return;
        }
        if (stopAfterMapReset) {
            beginForcedStopReset();
            return;
        }
        if (!rulesRestoreTerrainAfterRound()) {
            afterRoundRestore();
            return;
        }
        beginTerrainRestore(false);
    }

    private void beginTerrainRestore(boolean afterWarmup) {
        terrainRestoreAfterWarmup = afterWarmup;
        state = MatchState.TERRAIN_RESTORING;
        phaseEndTick = 0L;
        releaseAllDowned();
        if (!terrainRestoreStarted) {
            frozenPositions.clear();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                SpawnPoint hold = spawns.teleportToTerrainRestoreHold(player);
                frozenPositions.put(player.getUUID(), hold == null
                        ? new SpawnPoint(player.level().dimension(), player.getX(), player.getY(), player.getZ(),
                        player.getYRot(), player.getXRot()) : hold);
                player.setGameMode(GameType.SPECTATOR);
                player.setInvulnerable(true);
                player.setDeltaMovement(0.0D, 0.0D, 0.0D);
            }
            terrainRestoreStarted = true;
        }
        if (!maps.beginReset()) {
            phaseEndTick = server.getTickCount() + 20L;
            if (!resetFailureNotified) {
                resetFailureNotified = true;
                String reason = maps.resetManager() == null ? "没有可用的地图快照"
                        : maps.resetManager().lastError();
                String message = "地形恢复尚未开始：" + reason + "，服务器将重试。";
                recordEvent(message);
                broadcastSystemMessage(message, false);
            }
            broadcastMatchState();
            return;
        }
        resetFailureNotified = false;
        String message = afterWarmup ? "热身结束，正在恢复比赛地形。"
                : "回合结束，正在恢复比赛地形。";
        recordEvent(message);
        broadcastSystemMessage(message, false);
        broadcastMatchState();
    }

    private void retryTerrainRestore() {
        if (state != MatchState.TERRAIN_RESTORING) {
            return;
        }
        if (phaseEndTick > 0L && server.getTickCount() < phaseEndTick) return;
        if (!maps.beginReset()) {
            phaseEndTick = server.getTickCount() + 20L;
            if (!resetFailureNotified) {
                resetFailureNotified = true;
                String reason = maps.resetManager() == null ? "没有可用的地图快照"
                        : maps.resetManager().lastError();
                String message = "地形恢复中断：" + reason + "，服务器将重试。";
                recordEvent(message);
                broadcastSystemMessage(message, false);
            }
            return;
        }
        phaseEndTick = 0L;
        resetFailureNotified = false;
        broadcastMatchState();
    }

    private void completeTerrainRestore() {
        if (state != MatchState.TERRAIN_RESTORING) {
            return;
        }
        frozenPositions.clear();
        boolean restoreAfterWarmup = terrainRestoreAfterWarmup;
        terrainRestoreAfterWarmup = false;
        terrainRestoreStarted = false;
        recordEvent("地形恢复完成。");
        broadcastSystemMessage("地形恢复完成。", false);
        if (restoreAfterWarmup) {
            if (rulesMode() == GameMode.SEARCH_DESTROY) beginBuying();
            else if (rulesMode() == GameMode.TEAM_DEATHMATCH) beginFrozen();
            else beginPlaying(true);
            return;
        }
        afterRoundRestore();
    }

    private void afterRoundRestore() {
        if (pendingMatchWinner != null || rulesMode() != GameMode.SEARCH_DESTROY) {
            enterMatchEnd(pendingMatchWinner);
            return;
        }
        roundNumber++;
        if (rulesMode() == GameMode.SEARCH_DESTROY) {
            beginBuying();
        } else {
            beginWarmup(false);
        }
    }

    private void retryForcedMapReset() {
        if (state != MatchState.MAP_RESETTING) {
            return;
        }
        state = MatchState.MAP_RESETTING;
        phaseEndTick = 0L;
        if (!maps.beginReset()) {
            phaseEndTick = server.getTickCount() + 20L;
            if (!resetFailureNotified) {
                resetFailureNotified = true;
                String reason = maps.resetManager() == null ? "没有可用的地图快照"
                        : maps.resetManager().lastError();
                String message = "地图恢复尚未开始：" + reason + "，服务器将重试。";
                recordEvent(message);
                broadcastSystemMessage(message, false);
            }
            broadcastMatchState();
            return;
        }
        resetFailureNotified = false;
        broadcastMatchState();
    }

    private void retryMapReset() {
        if (state != MatchState.MAP_RESETTING) {
            return;
        }
        state = MatchState.ROUND_END;
        phaseEndTick = server.getTickCount() + 20L;
        if (!resetFailureNotified) {
            resetFailureNotified = true;
            String reason = maps.resetManager() == null ? "地图恢复管理器不可用"
                    : maps.resetManager().lastError();
            String message = "地图恢复中断：" + reason + "，服务器将在稍后重试。";
            recordEvent(message);
            broadcastSystemMessage(message, false);
            broadcastMatchState();
        }
    }

    private void completeForcedMapReset() {
        if (state != MatchState.MAP_RESETTING) {
            return;
        }
        frozenPositions.clear();
        recordEvent("地图恢复完成。");
        broadcastSystemMessage("地图恢复完成。", false);
        if (stopAfterMapReset) {
            recordEvent("比赛已停止，玩家状态与地图均已恢复。");
            finishRecord("比赛已停止");
            resetToWaiting(false);
            return;
        }
        if (pendingMatchWinner != null || rulesMode() != GameMode.SEARCH_DESTROY) {
            enterMatchEnd(pendingMatchWinner);
            return;
        }
        roundNumber++;
        beginWarmup(false);
    }

    private void enterMatchEnd(Team finalWinner) {
        Map<UUID, Team> combatants = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Team team = teams.getTeam(player);
            if (team.isPlayable()) combatants.put(player.getUUID(), team);
        }
        economy.finishMatch(server.getPlayerList().getPlayers(), finalWinner, combatants);
        if (bomb != null) bomb.cleanupRound();
        state = MatchState.MATCH_END;
        winner = finalWinner;
        phaseEndTick = server.getTickCount() + secondsToTicks(rulesMatchEndDelaySeconds());
        releaseAllDowned();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.setInvulnerable(true);
            player.setGameMode(GameType.SPECTATOR);
            sendToSpectatorArea(player);
        }
        if (finalWinner == null) {
            recordEvent("比赛结束，双方平局。");
            finishRecord("双方平局");
        } else {
            recordEvent(finalWinner.displayName() + " 赢得整场比赛！");
            finishRecord(finalWinner.displayName() + " 获胜");
        }
        broadcastMatchState();
    }

    private void scheduleDowned(ServerPlayer player, Team team) {
        scheduleDowned(player, team, "");
    }

    private void scheduleDowned(ServerPlayer player, Team team, String deathLabel) {
        long releaseTick = downedReleaseTick();
        long cameraUnlockTick = server.getTickCount() + cameraUnlockDelay(rulesElimination());
        downedPlayers.put(player.getUUID(), new DownedEntry(player.getUUID(), team,
                player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), releaseTick,
                cameraUnlockTick, deathLabel));
        readyRespawnRequests.remove(player.getUUID());
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new MatchRespawnEvent(player,
                MatchRespawnEvent.Phase.WAITING, releaseTick));
        if (releaseTick == Long.MAX_VALUE) {
            recordEvent(player.getGameProfile().getName() + " 阵亡，本回合不再复活。");
            sendMessageTo(player, "你已阵亡，本回合不再复活，等待回合结束。", 0);
        } else {
            recordEvent(player.getGameProfile().getName() + " 阵亡，将在 "
                    + secondsRemaining(releaseTick) + " 秒后恢复。");
            sendMessageTo(player, "你已阵亡，将在 %s 秒后恢复。", secondsRemaining(releaseTick));
        }
    }

    /** 阵亡恢复时刻：可复活模式按恢复倒计时，歼灭模式锁定到回合结束（不复活）。 */
    private long downedReleaseTick() {
        if (rulesElimination() || !rulesAutoRespawn()) {
            return Long.MAX_VALUE;
        }
        return server.getTickCount() + secondsToTicks(rulesRespawnDelaySeconds());
    }

    static long cameraUnlockDelay(boolean elimination) {
        return elimination ? 20L : 200L;
    }

    private void processDownedPlayers() {
        if (downedPlayers.isEmpty()) {
            return;
        }
        long now = server.getTickCount();
        for (DownedEntry entry : new ArrayList<>(downedPlayers.values())) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.playerId());
            if (player == null || state != MatchState.PLAYING
                    || teams.getTeam(player) != entry.team() || !entry.team().isPlayable()) {
                downedPlayers.remove(entry.playerId());
                readyRespawnRequests.remove(entry.playerId());
                if (player != null) restoreBody(player);
                continue;
            }
            if (!player.isAlive() || player.connection.player != player) continue;
            markDownedBody(player);
            boolean observationFinished = now >= entry.cameraUnlockTick();
            boolean requested = readyRespawnRequests.remove(entry.playerId());
            if (rulesElimination()) {
                if (rulesMode() == GameMode.SEARCH_DESTROY && entry.team() == attackingTeam()
                        && bomb != null && (bomb.state().phase() == ClassicBombState.Phase.PLANTED
                        || bomb.state().phase() == ClassicBombState.Phase.DEFUSING)) {
                    sendToSpectatorArea(player);
                } else {
                    leaveSpectatorArea(player);
                    player.teleportTo(entry.x(), entry.y(), entry.z());
                }
                if (observationFinished) ensureSpectatorTarget(player);
                continue;
            }
            leaveSpectatorArea(player);
            player.teleportTo(entry.x(), entry.y(), entry.z());
            if (now < entry.releaseTick() || (!requested && !observationFinished)) {
                continue;
            }
            if (!spawns.tryTeleportToTeamSpawn(player, spawnGroupFor(entry.team()), rulesSpawnStrategy())) {
                readyRespawnRequests.add(entry.playerId());
                if (now % 100 == 0) player.displayClientMessage(Component.literal("等待安全复活位置…"), true);
                continue;
            }
            downedPlayers.remove(entry.playerId());
            restoreBody(player);
            player.setCamera(player);
            player.setGameMode(GameType.SURVIVAL);
            player.setInvulnerable(false);
            player.invulnerableTime = 0;
            player.fallDistance = 0;
            player.setDeltaMovement(0, 0, 0);
            healAndReady(player);
            if (rulesRespawnProtectionSeconds() > 0 && rulesRespawnProtectionPercent() > 0) {
                respawnProtectionEnds.put(player.getUUID(), now + secondsToTicks(rulesRespawnProtectionSeconds()));
            }
            player.onUpdateAbilities();
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new MatchRespawnEvent(player,
                    MatchRespawnEvent.Phase.READY, now));
            sendMatchSync(player);
            player.displayClientMessage(Component.literal("状态已恢复，继续作战。"), true);
        }
    }

    /** Client input is only a request; countdown and spawn safety stay authoritative here. */
    public void requestReadyRespawn(ServerPlayer player) {
        DownedEntry entry = player == null ? null : downedPlayers.get(player.getUUID());
        if (entry == null || state != MatchState.PLAYING) return;
        long now = server.getTickCount();
        if (now >= entry.releaseTick()) {
            readyRespawnRequests.add(player.getUUID());
        }
    }

    private void releaseAllDowned() {
        downedPlayers.clear();
        readyRespawnRequests.clear();
        respawnProtectionEnds.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            clearSpectatorCamera(player);
            restoreBody(player);
            if (teams.getTeam(player).isPlayable()) {
                player.setInvulnerable(false);
            }
        }
        if (spectateTargets != null) spectateTargets.clear();
    }

    private static void markDownedBody(ServerPlayer player) {
        player.setGameMode(GameType.SPECTATOR);
        player.setInvulnerable(true);
        player.setInvisible(true);
        player.setSilent(true);
        player.noPhysics = true;
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
    }

    private static void restoreBody(ServerPlayer player) {
        player.setInvisible(false);
        player.setSilent(false);
        player.noPhysics = false;
    }

    private void enforceArenaRules() {
        if (!isMatchActive()) {
        regionReturnTicks.clear();
        frozenPositions.clear();
        terrainRestoreAfterWarmup = false;
        terrainRestoreStarted = false;
        boundaryCountdown.clear();
            return;
        }
        long now = server.getTickCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Team team = teams.getTeam(player);
            if (spectatorAreaPlayers != null && spectatorAreaPlayers.contains(player.getUUID())
                    && state != MatchState.TERRAIN_RESTORING
                    && state != MatchState.MAP_RESETTING) {
                sendToSpectatorArea(player);
                continue;
            }

            boolean boundaryEligible = state == MatchState.PLAYING && team.isPlayable()
                    && !teams.isPending(player) && !isDowned(player) && player.isAlive()
                    && !player.isSpectator() && FpsTdmConfig.COMMON.enforceRegion.get();
            boolean outside = boundaryEligible && !spawns.isInsideMap(player);
            int previousBoundary = boundaryCountdown.remaining(player.getUUID(), now);
            int boundaryLeft = boundaryCountdown.update(player.getUUID(), outside, now);
            if (outside && boundaryLeft == 0) {
                player.hurt(player.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
                boundaryCountdown.update(player.getUUID(), false, now);
                sendMatchSync(player);
                continue;
            }
            // Keep the client countdown authoritative during the whole out-of-bounds period.
            // A one-shot packet can be missed when the player changes dimension or reconnects.
            if ((previousBoundary == 0 && boundaryLeft > 0) || (previousBoundary > 0 && !outside)
                    || (boundaryLeft > 0 && now % 20 == 0)) sendMatchSync(player);

            if (state == MatchState.MAP_RESETTING) {
                player.setGameMode(GameType.SPECTATOR);
                player.setInvulnerable(true);
                player.setDeltaMovement(0.0D, 0.0D, 0.0D);
                if (canReturnNow(player, now)) {
                    sendToSpectatorArea(player);
                }
                continue;
            }
            if (state == MatchState.TERRAIN_RESTORING) {
                SpawnPoint hold = frozenPositions.get(player.getUUID());
                if (hold != null) {
                    teleportFrozen(player, hold);
                }
                player.setGameMode(GameType.SPECTATOR);
                player.setInvulnerable(true);
                player.setDeltaMovement(0.0D, 0.0D, 0.0D);
                continue;
            }

            if (state == MatchState.BUYING && team.isPlayable() && !teams.isPending(player)) {
                player.setGameMode(GameType.SURVIVAL);
                player.setInvulnerable(true);
                Team spawnGroup = spawnGroupFor(team);
                SpawnPoint anchor = buySpawnAnchors.get(player.getUUID());
                boolean inside = spawns.hasTeamSpawnZone(spawnGroup)
                        ? spawns.isInsideTeamSpawn(player, spawnGroup)
                        : anchor == null || player.distanceToSqr(anchor.x() + 0.5D,
                        anchor.y(), anchor.z() + 0.5D) <= 64.0D;
                if (!inside && canReturnNow(player, now)) {
                    SpawnSelectionStrategy buySpawn = spawns.findFixedSpawn(spawnGroup).isPresent()
                            ? SpawnSelectionStrategy.SEQUENTIAL : rulesSpawnStrategy();
                    spawns.teleportToTeamSpawn(player, spawnGroup, buySpawn);
                    buySpawnAnchors.put(player.getUUID(), new SpawnPoint(player.level().dimension(),
                            player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
                    if (buyAreaNoticeSent.add(player.getUUID())) {
                        recordEvent(player.getGameProfile().getName()
                                + " 离开购买区，被送回出生区域。");
                    }
                    publishHudEvent(player, MatchHudEventType.BUY_AREA_ONLY,
                            "购买期间只能在出生区域活动", 60);
                }
                continue;
            }

            if (!team.isPlayable() || teams.isPending(player)) {
                sendToSpectatorArea(player);
                if (state != MatchState.MATCH_END && FpsTdmConfig.COMMON.enforceRegion.get()
                        && !spawns.isInsideMap(player) && canReturnNow(player, now)) {
                    sendToSpectatorArea(player);
                }
                continue;
            }

            if (state == MatchState.ROUND_END || state == MatchState.MATCH_END) {
                player.setInvulnerable(true);
            }
            if (state == MatchState.PLAYING && isDowned(player)) {
                player.setGameMode(GameType.SURVIVAL);
                player.setInvulnerable(true);
                player.setDeltaMovement(0.0D, 0.0D, 0.0D);
                continue;
            }
            if (state == MatchState.PLAYING && !teams.isPending(player)) {
                player.setInvulnerable(false);
            }

            if (FpsTdmConfig.COMMON.enforceRegion.get() && state != MatchState.MATCH_END
                    && state != MatchState.PLAYING
                    && !spawns.isInsideMap(player) && canReturnNow(player, now)) {
                if (state == MatchState.PLAYING || state == MatchState.WARMUP) {
                    // 越界的参赛玩家回本队出生点并保持生存模式；带冷却的强制传送
                    // 避免每 Tick 拽视角导致“视角无法移动”，也防止出生点越界时无限循环。
                    if (state == MatchState.WARMUP) {
                        player.setGameMode(GameType.SURVIVAL);
                        player.setInvulnerable(false);
                    }
                    spawns.teleportToTeamSpawn(player, spawnGroupFor(team), rulesSpawnStrategy());
                } else {
                    sendToSpectatorArea(player);
                }
            }
        }
    }

    /** 强制回位传送按玩家节流，防止 20Hz 的传送包把客户端视角钉死。 */
    private boolean canReturnNow(ServerPlayer player, long now) {
        long next = regionReturnTicks.getOrDefault(player.getUUID(), 0L);
        if (now < next) {
            return false;
        }
        regionReturnTicks.put(player.getUUID(), now + REGION_RETURN_COOLDOWN_TICKS);
        return true;
    }

    private void sendToSpectatorArea(ServerPlayer player) {
        if (player == null) return;
        player.setGameMode(GameType.SPECTATOR);
        player.setInvulnerable(true);
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (spectatorAreaPlayers == null) {
            spawns.teleportToSpectator(player);
        } else if (spectatorAreaPlayers.add(player.getUUID())) {
            spawns.teleportToSpectator(player);
        } else {
            spawns.enforceSpectatorArea(player);
        }
    }

    private void leaveSpectatorArea(ServerPlayer player) {
        if (player != null && spectatorAreaPlayers != null) spectatorAreaPlayers.remove(player.getUUID());
    }

    public void switchSpectatorTarget(ServerPlayer player, boolean next) {
        if (player == null || state != MatchState.PLAYING || !isDowned(player)
                || !rulesElimination() || spectateTargets == null) return;
        DownedEntry entry = downedPlayers.get(player.getUUID());
        if (entry == null || server.getTickCount() < entry.cameraUnlockTick()) return;
        List<ServerPlayer> candidates = teammateSpectateCandidates(player, entry.team());
        if (candidates.isEmpty()) {
            player.setCamera(player);
            spectateTargets.remove(player.getUUID());
            return;
        }
        UUID currentId = spectateTargets.get(player.getUUID());
        int current = -1;
        for (int index = 0; index < candidates.size(); index++) {
            if (candidates.get(index).getUUID().equals(currentId)) {
                current = index;
                break;
            }
        }
        int selected = current < 0 ? 0
                : Math.floorMod(current + (next ? 1 : -1), candidates.size());
        ServerPlayer target = candidates.get(selected);
        spectateTargets.put(player.getUUID(), target.getUUID());
        player.setCamera(target);
    }

    private void ensureSpectatorTarget(ServerPlayer player) {
        if (spectateTargets == null) return;
        DownedEntry entry = downedPlayers.get(player.getUUID());
        if (entry == null) return;
        UUID currentId = spectateTargets.get(player.getUUID());
        if (currentId != null) {
            ServerPlayer current = server.getPlayerList().getPlayer(currentId);
            if (current != null && current.isAlive() && !isDowned(current)
                    && teams.getTeam(current) == entry.team()) return;
        }
        List<ServerPlayer> candidates = teammateSpectateCandidates(player, entry.team());
        if (candidates.isEmpty()) {
            player.setCamera(player);
            spectateTargets.remove(player.getUUID());
            return;
        }
        ServerPlayer target = candidates.get(0);
        spectateTargets.put(player.getUUID(), target.getUUID());
        player.setCamera(target);
    }

    private List<ServerPlayer> teammateSpectateCandidates(ServerPlayer player, Team team) {
        return server.getPlayerList().getPlayers().stream()
                .filter(candidate -> candidate != player)
                .filter(ServerPlayer::isAlive)
                .filter(candidate -> teams.getTeam(candidate) == team)
                .filter(candidate -> !teams.isPending(candidate))
                .filter(candidate -> !isDowned(candidate))
                .sorted(java.util.Comparator.comparing(candidate -> candidate.getGameProfile().getName()))
                .toList();
    }

    private void clearSpectatorCamera(ServerPlayer player) {
        if (player == null) return;
        if (spectateTargets != null && spectateTargets.remove(player.getUUID()) != null) {
            player.setCamera(player);
        }
    }

    private ServerPlayer resolveKiller(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof ServerPlayer player) {
            return player;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof ServerPlayer player) {
            return player;
        }
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) {
            return player;
        }
        if (attacker instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    private void captureRules() {
        if (rulesCaptured) {
            return;
        }
        GameRules rules = server.getGameRules();
        originalKeepInventory = rules.getBoolean(GameRules.RULE_KEEPINVENTORY);
        originalDeathMessages = rules.getBoolean(GameRules.RULE_SHOWDEATHMESSAGES);
        rulesCaptured = true;
        rules.getRule(GameRules.RULE_KEEPINVENTORY).set(rulesKeepInventoryOnDeath(), server);
        if (rulesSuppressDeathMessages()) {
            rules.getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(false, server);
        }
    }

    private void restoreRules() {
        if (!rulesCaptured) {
            return;
        }
        GameRules rules = server.getGameRules();
        rules.getRule(GameRules.RULE_KEEPINVENTORY).set(originalKeepInventory, server);
        rules.getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(originalDeathMessages, server);
        rulesCaptured = false;
    }

    private void healAndReady(ServerPlayer player) {
        // A phase change must never resurrect the removed/dead entity by changing its health.
        if (!player.isAlive()) return;
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0F);
        player.clearFire();
    }

    private void markWinnerParticles(Team team) {
        for (SpawnPoint point : spawns.getSpawns(team)) {
            ServerLevel level = server.getLevel(point.dimension());
            if (level != null) {
                level.sendParticles(ParticleTypes.FIREWORK, point.x(), point.y() + 1.5D, point.z(),
                        20, 0.5D, 0.6D, 0.5D, 0.05D);
            }
        }
    }

    private void playPhaseSound() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.MASTER, 0.8F, 1.0F);
        }
    }

    private void sendMessageTo(ServerPlayer player, String format, Object... args) {
        player.sendSystemMessage(Component.literal(String.format(format, args)));
    }

    private void broadcastSystemMessage(String message, boolean overlay) {
        server.getPlayerList().broadcastSystemMessage(Component.literal(message), overlay);
    }

    private int secondsRemaining(long tick) {
        return Math.max(0, (int) Math.ceil(Math.max(0L, tick - server.getTickCount()) / 20.0D));
    }

    private static long secondsToTicks(int seconds) {
        return Math.max(0L, seconds) * 20L;
    }

    private static String formatTicks(int ticks) {
        int seconds = Math.max(0, ticks) / 20;
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    public record DownedEntry(UUID playerId, Team team, double x, double y, double z,
                               float yaw, float pitch, long releaseTick, long cameraUnlockTick, String deathLabel) {
        public DownedEntry(UUID playerId, Team team, double x, double y, double z,
                           float yaw, float pitch, long releaseTick) {
            this(playerId, team, x, y, z, yaw, pitch, releaseTick, releaseTick, "");
        }
    }

    public enum StartResult {
        STARTED,
        ALREADY_ACTIVE,
        NEED_BOTH_TEAMS,
        NO_PLAYERS,
        NO_END_CONDITION,
        NO_MAP,
        MAP_INCOMPLETE,
        MAP_LOADING,
        MAP_NOT_READY,
        NO_TEAM_SPAWNS,
        NO_SAFE_RANDOM_SPAWN,
        NO_BOMB_SITES,
        MAP_BUSY
    }
}
