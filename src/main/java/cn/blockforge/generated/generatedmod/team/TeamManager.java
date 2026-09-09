package cn.blockforge.generated.generatedmod.team;

import cn.blockforge.generated.generatedmod.match.MatchManager;
import cn.blockforge.generated.generatedmod.match.MatchState;
import cn.blockforge.generated.generatedmod.match.Team;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TeamManager {
    private final MinecraftServer server;
    private final MatchManager matchManager;
    private final Map<UUID, Team> playerTeams = new HashMap<>();
    private final Map<UUID, Team> preferences = new HashMap<>();
    private static final String ORIGINAL_STATE_KEY = "generated_mod_original_state";
    private final Map<UUID, OriginalState> originalStates = new HashMap<>();
    private final Set<UUID> pendingPlayers = new HashSet<>();

    public TeamManager(MinecraftServer server, MatchManager matchManager) {
        this.server = server;
        this.matchManager = matchManager;
        ensureScoreboardTeams();
    }

    public Team getTeam(ServerPlayer player) {
        return getTeam(player.getUUID());
    }

    public Team getTeam(UUID playerId) {
        return playerTeams.getOrDefault(playerId, Team.SPECTATOR);
    }

    public Team getPreference(ServerPlayer player) {
        return preferences.getOrDefault(player.getUUID(), suggestTeam());
    }

    public void setPreference(ServerPlayer player, Team team) {
        if (team != null && team.isPlayable()) {
            preferences.put(player.getUUID(), team);
        }
    }

    public int teamSize(Team team) {
        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (getTeam(player) == team) {
                count++;
            }
        }
        return count;
    }

    public int totalParticipants() {
        return Team.playing(4).stream().mapToInt(this::teamSize).sum();
    }

    public int spectatorSize() {
        return Math.max(0, server.getPlayerList().getPlayers().size() - totalParticipants());
    }

    public record Counts(int teamA, int teamB, int spectators) { }

    public Counts counts() {
        int a = 0;
        int b = 0;
        int spectators = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            switch (getTeam(player)) {
                case TEAM_A -> a++;
                case TEAM_B -> b++;
                case SPECTATOR -> spectators++;
            }
        }
        return new Counts(a, b, spectators);
    }

    public JoinResult joinPlayer(ServerPlayer player) {
        MatchState state = matchManager.state();
        if (state.isActive() && !matchManager.rulesAllowJoinDuringMatch()) {
            return JoinResult.LOCKED;
        }
        Team desired = getPreference(player);
        if (state.isActive() && matchManager.rulesJoinDuringMatchAsSpectator()) {
            pendingPlayers.add(player.getUUID());
            preferences.put(player.getUUID(), desired);
            setTeamInternal(player, Team.SPECTATOR);
            player.setGameMode(GameType.SPECTATOR);
            return JoinResult.QUEUED;
        }
        Team selected = selectJoinTeam(desired);
        if (selected == null) {
            return JoinResult.FULL;
        }
        pendingPlayers.remove(player.getUUID());
        setTeamInternal(player, selected);
        player.setGameMode(GameType.SURVIVAL);
        return JoinResult.JOINED;
    }

    /**
     * 房间流程内的强制入队：玩家通过大厅加入进行中的匹配比赛时视为明确参赛意愿，
     * 不受“比赛进行中禁止加入”类配置限制；无法平衡分队时挂到下一回合激活。
     */
    public Team forceJoinDuringMatch(ServerPlayer player) {
        Team desired = getPreference(player);
        Team selected = selectJoinTeam(desired);
        if (selected == null) {
            pendingPlayers.add(player.getUUID());
            setTeamInternal(player, Team.SPECTATOR);
            player.setGameMode(GameType.SPECTATOR);
            return null;
        }
        pendingPlayers.remove(player.getUUID());
        setTeamInternal(player, selected);
        player.setGameMode(GameType.SURVIVAL);
        return selected;
    }

    public ChangeResult changeTeam(ServerPlayer player, Team requested) {
        if (requested == null) {
            return ChangeResult.INVALID;
        }
        if (requested == Team.SPECTATOR) {
            pendingPlayers.remove(player.getUUID());
            setTeamInternal(player, Team.SPECTATOR);
            player.setGameMode(GameType.SPECTATOR);
            return ChangeResult.CHANGED;
        }
        MatchState state = matchManager.state();
        if (state.isActive() && !matchManager.rulesTeamChangePolicy().allowsChangeDuringMatch()) {
            return ChangeResult.LOCKED;
        }
        if (state.isActive() && getTeam(player).isPlayable() && player.isAlive()) {
            return ChangeResult.ALIVE;
        }
        if (!canJoinTeam(requested, getTeam(player))) {
            return ChangeResult.BALANCED;
        }
        preferences.put(player.getUUID(), requested);
        pendingPlayers.remove(player.getUUID());
        setTeamInternal(player, requested);
        player.setGameMode(GameType.SURVIVAL);
        return ChangeResult.CHANGED;
    }

    public void leavePlayer(ServerPlayer player) {
        pendingPlayers.remove(player.getUUID());
        setTeamInternal(player, Team.SPECTATOR);
        if (matchManager.state().isActive()) {
            player.setGameMode(GameType.SPECTATOR);
        } else {
            restoreGameMode(player);
        }
    }

    /** 将指定房间成员设置为本场参赛者，其余在线玩家保持观战。 */
    public void prepareRoster(Collection<UUID> roster) {
        Set<UUID> allowed = new HashSet<>(roster == null ? Set.of() : roster);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!allowed.contains(player.getUUID())) {
                pendingPlayers.remove(player.getUUID());
                setTeamInternal(player, Team.SPECTATOR);
                player.setGameMode(GameType.SPECTATOR);
            }
        }
        for (UUID playerId : allowed) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                continue;
            }
            // 名单内玩家的“等待下一回合”标记必须清除，否则比赛中会被当成中途加入者强制观战。
            pendingPlayers.remove(playerId);
            Team preferred = preferences.getOrDefault(playerId, suggestTeam());
            setTeamInternal(player, matchManager.activeTeams().contains(preferred) ? preferred : suggestTeam());
        }
        if (matchManager.rulesAutoBalanceMode().balancesOnMatchStart()) balanceTeamsAtMatchStart();
    }

    public void prepareForMatch() {
        if (matchManager.rulesAutoBalanceMode().balancesOnMatchStart()) {
            balanceTeamsAtMatchStart();
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (getTeam(player).isPlayable()) {
                rememberGameMode(player);
                player.setGameMode(GameType.SURVIVAL);
            }
        }
    }

    public void balanceTeamsAtMatchStart() {
        List<Team> enabled = matchManager.activeTeams();
        int maximumDifference = Math.max(1, matchManager.rulesMaxTeamImbalance());
        while (true) {
            Team smallest = enabled.stream().min(Comparator.comparingInt(this::teamSize)).orElseThrow();
            Team largest = enabled.stream().max(Comparator.comparingInt(this::teamSize)).orElseThrow();
            if (teamSize(largest) - teamSize(smallest) <= maximumDifference
                    && (teamSize(smallest) > 0 || teamSize(largest) <= 1)) return;
            ServerPlayer candidate = server.getPlayerList().getPlayers().stream()
                    .filter(player -> getTeam(player) == largest && !isPending(player))
                    .min(Comparator.comparing(player -> player.getUUID().toString())).orElse(null);
            if (candidate == null) return;
            setTeamInternal(candidate, smallest);
        }
    }

    public void activatePendingPlayers() {
        for (UUID playerId : new HashSet<>(pendingPlayers)) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                pendingPlayers.remove(playerId);
                continue;
            }
            if (getTeam(player).isPlayable()) {
                // 已经有归属队（如歼灭模式中途加入）：到点直接入场，不再重分队。
                pendingPlayers.remove(playerId);
                player.setGameMode(GameType.SURVIVAL);
                continue;
            }
            Team desired = preferences.getOrDefault(playerId, suggestTeam());
            Team selected = selectJoinTeam(desired);
            if (selected != null) {
                pendingPlayers.remove(playerId);
                setTeamInternal(player, selected);
                player.setGameMode(GameType.SURVIVAL);
            }
        }
    }

    public boolean isPending(ServerPlayer player) {
        return pendingPlayers.contains(player.getUUID());
    }

    /** 标记“下一回合再入场”：歼灭类模式的中途加入者本回合先旁观。 */
    public void setPending(ServerPlayer player) {
        rememberGameMode(player);
        pendingPlayers.add(player.getUUID());
    }

    public boolean canChangeTeam(ServerPlayer player) {
        MatchState state = matchManager.state();
        return !state.isActive() || matchManager.rulesTeamChangePolicy().allowsChangeDuringMatch();
    }

    public Team suggestTeam() {
        return matchManager.activeTeams().stream().min(Comparator.comparingInt(this::teamSize)).orElse(Team.TEAM_A);
    }

    public void resetAll() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            removeFromScoreboard(player);
            restoreGameMode(player);
        }
        playerTeams.clear();
        pendingPlayers.clear();
        preferences.clear();
        originalStates.clear();
    }

    public void clearPlayer(ServerPlayer player) {
        playerTeams.remove(player.getUUID());
        pendingPlayers.remove(player.getUUID());
        removeFromScoreboard(player);
        restoreGameMode(player);
        preferences.remove(player.getUUID());
    }

    private Team selectJoinTeam(Team desired) {
        if (matchManager.rulesAutoBalanceMode().balancesOnJoin()) return suggestTeam();
        if (matchManager.activeTeams().contains(desired)) return desired;
        if (!matchManager.activeTeams().contains(desired)) {
            desired = suggestTeam();
        }
        if (canJoinTeam(desired, Team.SPECTATOR)) {
            return desired;
        }
        return matchManager.activeTeams().stream().filter(team -> canJoinTeam(team, Team.SPECTATOR)).findFirst().orElse(null);
    }

    private boolean canJoinTeam(Team target, Team current) {
        if (!target.isPlayable()) {
            return true;
        }
        if (!matchManager.activeTeams().contains(target)) return false;
        int min = Integer.MAX_VALUE, max = 0, total = 0;
        for (Team team : matchManager.activeTeams()) {
            int size = teamSize(team) - (current == team ? 1 : 0) + (target == team ? 1 : 0);
            min = Math.min(min, size); max = Math.max(max, size); total += size;
        }
        int unavoidable = total % matchManager.activeTeams().size() == 0 ? 0 : 1;
        return max - min <= Math.max(unavoidable, matchManager.rulesMaxTeamImbalance());
    }

    static int effectiveImbalance(int totalPlayers, int configured) {
        // 奇数人数无法严格均分，至少需要允许一人的差额。
        return Math.max(Math.max(0, configured), totalPlayers & 1);
    }

    private void setTeamInternal(ServerPlayer player, Team team) {
        rememberGameMode(player);
        playerTeams.put(player.getUUID(), team);
        syncScoreboardTeam(player, team);
    }

    public void rememberGameMode(ServerPlayer player) {
        if (originalStates.containsKey(player.getUUID())) {
            return;
        }
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        OriginalState original;
        if (persisted.contains(ORIGINAL_STATE_KEY)) {
            CompoundTag saved = persisted.getCompound(ORIGINAL_STATE_KEY);
            original = new OriginalState(GameType.byId(saved.getInt("gameMode")), saved.getBoolean("invulnerable"));
        } else {
            original = new OriginalState(player.gameMode.getGameModeForPlayer(), player.isInvulnerable());
            CompoundTag saved = new CompoundTag();
            saved.putInt("gameMode", original.gameMode().getId());
            saved.putBoolean("invulnerable", original.invulnerable());
            persisted.put(ORIGINAL_STATE_KEY, saved);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        }
        originalStates.put(player.getUUID(), original);
    }

    public void restoreGameMode(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!originalStates.containsKey(player.getUUID()) && persisted.contains(ORIGINAL_STATE_KEY)) {
            rememberGameMode(player);
        }
        OriginalState original = originalStates.remove(player.getUUID());
        if (original != null) {
            player.setGameMode(original.gameMode());
            player.setInvulnerable(original.invulnerable());
            persisted.remove(ORIGINAL_STATE_KEY);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        }
    }

    private record OriginalState(GameType gameMode, boolean invulnerable) { }

    /** 配置界面保存后立即刷新记分板中缓存的友军伤害规则。 */
    public void refreshConfigRules() {
        ensureScoreboardTeams();
    }

    private void ensureScoreboardTeams() {
        Scoreboard scoreboard = server.getScoreboard();
        createScoreboardTeam(scoreboard, Team.TEAM_A, ChatFormatting.RED);
        createScoreboardTeam(scoreboard, Team.TEAM_B, ChatFormatting.BLUE);
        createScoreboardTeam(scoreboard, Team.TEAM_C, ChatFormatting.GREEN);
        createScoreboardTeam(scoreboard, Team.TEAM_D, ChatFormatting.YELLOW);
        createScoreboardTeam(scoreboard, Team.SPECTATOR, ChatFormatting.GRAY);
    }

    private void createScoreboardTeam(Scoreboard scoreboard, Team team, ChatFormatting color) {
        String name = scoreboardTeamName(team);
        PlayerTeam scoreboardTeam = scoreboard.getPlayerTeam(name);
        if (scoreboardTeam == null) {
            scoreboardTeam = scoreboard.addPlayerTeam(name);
        }
        scoreboardTeam.setDisplayName(Component.literal(team.displayName()));
        scoreboardTeam.setColor(color);
        scoreboardTeam.setAllowFriendlyFire(matchManager.rulesFriendlyFire());
        scoreboardTeam.setSeeFriendlyInvisibles(team == Team.SPECTATOR);
    }

    private void syncScoreboardTeam(ServerPlayer player, Team team) {
        Scoreboard scoreboard = server.getScoreboard();
        ensureScoreboardTeams();
        PlayerTeam old = scoreboard.getPlayersTeam(player.getScoreboardName());
        if (old != null) {
            scoreboard.removePlayerFromTeam(player.getScoreboardName(), old);
        }
        PlayerTeam target = scoreboard.getPlayerTeam(scoreboardTeamName(team));
        if (target != null) {
            scoreboard.addPlayerToTeam(player.getScoreboardName(), target);
        }
    }

    private void removeFromScoreboard(ServerPlayer player) {
        PlayerTeam old = server.getScoreboard().getPlayersTeam(player.getScoreboardName());
        if (old != null && old.getName().startsWith("generated_mod_team_")) {
            server.getScoreboard().removePlayerFromTeam(player.getScoreboardName(), old);
        }
    }

    private static String scoreboardTeamName(Team team) {
        return switch (team) {
            case TEAM_A -> "generated_mod_team_a";
            case TEAM_B -> "generated_mod_team_b";
            case TEAM_C -> "generated_mod_team_c";
            case TEAM_D -> "generated_mod_team_d";
            case SPECTATOR -> "generated_mod_team_spectator";
        };
    }

    public enum JoinResult {
        JOINED,
        QUEUED,
        FULL,
        LOCKED
    }

    public enum ChangeResult {
        CHANGED,
        INVALID,
        LOCKED,
        ALIVE,
        BALANCED
    }
}
