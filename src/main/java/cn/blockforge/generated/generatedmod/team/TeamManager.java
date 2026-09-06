package cn.blockforge.generated.generatedmod.team;

import cn.blockforge.generated.generatedmod.match.MatchManager;
import cn.blockforge.generated.generatedmod.match.MatchState;
import cn.blockforge.generated.generatedmod.match.Team;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
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
    private final Map<UUID, GameType> originalGameModes = new HashMap<>();
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
        if (team.isPlayable()) {
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
        return teamSize(Team.TEAM_A) + teamSize(Team.TEAM_B);
    }

    public int spectatorSize() {
        return Math.max(0, server.getPlayerList().getPlayers().size() - totalParticipants());
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
            setTeamInternal(player, preferred.isPlayable() ? preferred : suggestTeam());
        }
        balanceTeamsAtMatchStart();
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
        List<ServerPlayer> participants = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (getTeam(player).isPlayable()) {
                participants.add(player);
            }
        }
        participants.sort(Comparator.comparing(player -> player.getUUID().toString()));
        if (participants.isEmpty()) {
            return;
        }
        int a = teamSize(Team.TEAM_A);
        int b = teamSize(Team.TEAM_B);
        if (a == 0 || b == 0) {
            for (int i = 0; i < participants.size(); i++) {
                setTeamInternal(participants.get(i), i % 2 == 0 ? Team.TEAM_A : Team.TEAM_B);
            }
            return;
        }
        int maximumDifference = matchManager.rulesMaxTeamImbalance();
        while (Math.abs(a - b) > maximumDifference) {
            Team from = a > b ? Team.TEAM_A : Team.TEAM_B;
            Team to = from == Team.TEAM_A ? Team.TEAM_B : Team.TEAM_A;
            ServerPlayer candidate = participants.stream()
                    .filter(player -> getTeam(player) == from)
                    .findFirst().orElse(null);
            if (candidate == null) {
                break;
            }
            setTeamInternal(candidate, to);
            if (from == Team.TEAM_A) {
                a--;
                b++;
            } else {
                b--;
                a++;
            }
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
        pendingPlayers.add(player.getUUID());
    }

    public boolean canChangeTeam(ServerPlayer player) {
        MatchState state = matchManager.state();
        return !state.isActive() || matchManager.rulesTeamChangePolicy().allowsChangeDuringMatch();
    }

    public Team suggestTeam() {
        if (teamSize(Team.TEAM_A) <= teamSize(Team.TEAM_B)) {
            return Team.TEAM_A;
        }
        return Team.TEAM_B;
    }

    public void resetAll() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            removeFromScoreboard(player);
            restoreGameMode(player);
        }
        playerTeams.clear();
        pendingPlayers.clear();
        preferences.clear();
    }

    public void clearPlayer(ServerPlayer player) {
        playerTeams.remove(player.getUUID());
        pendingPlayers.remove(player.getUUID());
        removeFromScoreboard(player);
        originalGameModes.remove(player.getUUID());
    }

    private Team selectJoinTeam(Team desired) {
        if (!desired.isPlayable()) {
            desired = suggestTeam();
        }
        if (canJoinTeam(desired, Team.SPECTATOR)) {
            return desired;
        }
        Team alternate = desired == Team.TEAM_A ? Team.TEAM_B : Team.TEAM_A;
        return canJoinTeam(alternate, Team.SPECTATOR) ? alternate : null;
    }

    private boolean canJoinTeam(Team target, Team current) {
        if (!target.isPlayable()) {
            return true;
        }
        int a = teamSize(Team.TEAM_A);
        int b = teamSize(Team.TEAM_B);
        if (current == Team.TEAM_A) {
            a--;
        } else if (current == Team.TEAM_B) {
            b--;
        }
        if (target == Team.TEAM_A) {
            a++;
        } else {
            b++;
        }
        return Math.abs(a - b) <= matchManager.rulesMaxTeamImbalance();
    }

    private void setTeamInternal(ServerPlayer player, Team team) {
        rememberGameMode(player);
        playerTeams.put(player.getUUID(), team);
        syncScoreboardTeam(player, team);
    }

    private void rememberGameMode(ServerPlayer player) {
        originalGameModes.putIfAbsent(player.getUUID(), player.gameMode.getGameModeForPlayer());
    }

    public void restoreGameMode(ServerPlayer player) {
        GameType original = originalGameModes.remove(player.getUUID());
        if (original != null) {
            player.setGameMode(original);
        }
    }

    /** 配置界面保存后立即刷新记分板中缓存的友军伤害规则。 */
    public void refreshConfigRules() {
        ensureScoreboardTeams();
    }

    private void ensureScoreboardTeams() {
        Scoreboard scoreboard = server.getScoreboard();
        createScoreboardTeam(scoreboard, Team.TEAM_A, ChatFormatting.RED);
        createScoreboardTeam(scoreboard, Team.TEAM_B, ChatFormatting.BLUE);
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
