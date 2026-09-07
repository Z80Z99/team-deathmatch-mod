package cn.blockforge.generated.generatedmod.match;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 比赛记分与统计：本轮分数（resetRound 清空）之外，另记
 * 整场累计击杀、阵亡与伤害（resetMatch 才清空），供 HUD 统计接口取用。
 */
public final class MatchScoreTracker {
    private final Map<Team, Integer> teamKills = new java.util.EnumMap<>(Team.class);
    private final Map<UUID, Integer> playerKills = new HashMap<>();
    private final Map<UUID, Integer> playerDeaths = new HashMap<>();

    /** 整场累计：击杀 / 阵亡 / 造成伤害 / 承受伤害 / 队伍击杀与伤害合计。 */
    private final Map<UUID, Integer> matchKills = new HashMap<>();
    private final Map<UUID, Integer> matchDeaths = new HashMap<>();
    private final Map<UUID, Integer> damageDealt = new HashMap<>();
    private final Map<UUID, Integer> damageTaken = new HashMap<>();
    private final Map<Team, Integer> teamDamage = new HashMap<>();
    private final Map<Team, Integer> teamMatchKills = new java.util.EnumMap<>(Team.class);

    public int getTeamScore(Team team) {
        return teamKills.getOrDefault(team, 0);
    }

    public int getPlayerKills(UUID playerId) {
        return playerKills.getOrDefault(playerId, 0);
    }

    public int getPlayerDeaths(UUID playerId) {
        return playerDeaths.getOrDefault(playerId, 0);
    }

    public int getMatchKills(UUID playerId) {
        return matchKills.getOrDefault(playerId, 0);
    }

    public int getMatchDeaths(UUID playerId) {
        return matchDeaths.getOrDefault(playerId, 0);
    }

    public int getDamageDealt(UUID playerId) {
        return damageDealt.getOrDefault(playerId, 0);
    }

    public int getDamageTaken(UUID playerId) {
        return damageTaken.getOrDefault(playerId, 0);
    }

    /** 整场累计的队伍击杀（跨回合不重置）。 */
    public int getTeamMatchKills(Team team) {
        return teamMatchKills.getOrDefault(team, 0);
    }

    public int getTeamDamage(Team team) {
        return teamDamage.getOrDefault(team, 0);
    }

    public void addKill(Team team, ServerPlayer killer, ServerPlayer victim) {
        if (team != null && team.isPlayable()) {
            teamKills.merge(team, 1, Integer::sum);
            teamMatchKills.merge(team, 1, Integer::sum);
        }
        playerKills.merge(killer.getUUID(), 1, Integer::sum);
        playerDeaths.merge(victim.getUUID(), 1, Integer::sum);
        matchKills.merge(killer.getUUID(), 1, Integer::sum);
        matchDeaths.merge(victim.getUUID(), 1, Integer::sum);
    }

    public void addDeath(ServerPlayer victim) {
        playerDeaths.merge(victim.getUUID(), 1, Integer::sum);
        matchDeaths.merge(victim.getUUID(), 1, Integer::sum);
    }

    /** 记录一次有效伤害：按实际伤害值累计到攻击者、受害者与所属队伍。 */
    public void addDamage(Team attackerTeam, ServerPlayer attacker, ServerPlayer victim, int amount) {
        int dealt = Math.max(1, amount);
        if (attacker != null) {
            damageDealt.merge(attacker.getUUID(), dealt, Integer::sum);
        }
        if (victim != null) {
            damageTaken.merge(victim.getUUID(), dealt, Integer::sum);
        }
        if (attackerTeam != null && attackerTeam.isPlayable()) {
            teamDamage.merge(attackerTeam, dealt, Integer::sum);
        }
    }

    public void resetRound() {
        teamKills.clear();
        playerKills.clear();
        playerDeaths.clear();
    }

    /** 整场开始时清空全部累计（含伤害与队伍击杀合计）。 */
    public void resetMatch() {
        resetRound();
        matchKills.clear();
        matchDeaths.clear();
        damageDealt.clear();
        damageTaken.clear();
        teamDamage.clear();
        teamMatchKills.clear();
    }
}
