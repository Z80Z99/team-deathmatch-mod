package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.match.ClassicBombState;
import cn.blockforge.generated.generatedmod.match.GameMode;
import cn.blockforge.generated.generatedmod.match.MatchState;
import net.minecraft.client.Minecraft;

/** Stable, configurable in-match notice derived from synchronized match state. */
public final class MatchHudNotice {
    private MatchHudNotice() { }

    public static boolean urgent() {
        return ClientBombData.active
                || ClientMatchData.boundaryOutside
                || ClientMatchData.awaitingRespawn
                || ClientMatchData.state == MatchState.BUYING
                || (ClientMatchData.state == MatchState.WARMUP
                && ClientMatchData.phaseRemainingTicks > 0
                && ClientMatchData.phaseRemainingTicks <= 200);
    }

    public static String title() {
        String bomb = bombTitle();
        if (bomb != null) return bomb;
        return switch (ClientMatchData.state) {
            case WAITING -> "等待比赛";
            case WARMUP -> "热身阶段";
            case BUYING -> "购买阶段";
            case PLAYING -> ClientMatchData.mode == GameMode.SEARCH_DESTROY ? "行动阶段" : "比赛进行中";
            case ROUND_END -> "回合结束";
            case TERRAIN_RESTORING -> "地形恢复";
            case MAP_RESETTING -> "地图恢复";
            case MATCH_END -> ClientMatchData.winner == null ? "比赛平局"
                    : ClientMatchData.winner.displayName() + " 获胜";
        };
    }

    public static String detail() {
        String bomb = bombDetail();
        if (bomb != null) return bomb;
        return switch (ClientMatchData.state) {
            case WAITING -> "等待房主开始比赛";
            case WARMUP -> ClientMatchData.phaseRemainingTicks <= 0
                    ? "等待足够玩家加入 · " + ClientMatchData.teamSizesText()
                    : "比赛将在 " + seconds(ClientMatchData.phaseRemainingTicks) + " 秒后开始 · "
                    + ClientMatchData.teamSizesText();
            case BUYING -> "只能在出生区域活动 · 购买并准备装备";
            case PLAYING -> ClientMatchData.mode == GameMode.SEARCH_DESTROY
                    ? "回合 " + ClientMatchData.roundNumber + " · 目标 "
                    + HudParameters.target() + " 回合胜场"
                    : "回合 " + ClientMatchData.roundNumber + " · "
                    + ClientMatchData.targetText();
            case ROUND_END -> "本回合结算中 · " + ClientMatchData.roundText();
            case TERRAIN_RESTORING -> "正在恢复比赛地形，请稍候";
            case MAP_RESETTING -> "地图恢复完成后继续或退出比赛";
            case MATCH_END -> "比赛已经结束 · " + ClientMatchData.roundText();
        };
    }

    public static String timer() {
        if (ClientBombData.active && ClientBombData.phase == ClassicBombState.Phase.PLANTED) {
            return String.format(java.util.Locale.ROOT, "%.1f 秒", ClientBombData.detonationRemainingTicks / 20.0D);
        }
        if (ClientBombData.active && (ClientBombData.phase == ClassicBombState.Phase.PLANTING
                || ClientBombData.phase == ClassicBombState.Phase.DEFUSING)) {
            return String.format(java.util.Locale.ROOT, "%.1f 秒", ClientBombData.actionRemainingTicks / 20.0D);
        }
        if (ClientMatchData.boundaryOutside && ClientMatchData.boundaryTicks > 0) {
            return seconds(ClientMatchData.boundaryTicks) + " 秒";
        }
        if (ClientMatchData.awaitingRespawn) return ClientMatchData.respawnTimerText();
        return switch (ClientMatchData.state) {
            case WARMUP -> ClientMatchData.phaseRemainingTicks <= 0
                    ? "等待玩家" : seconds(ClientMatchData.phaseRemainingTicks) + " 秒";
            case BUYING, PLAYING -> ClientMatchData.phaseRemainingTicks <= 0
                    ? "" : ClientMatchData.phaseTimerText();
            default -> "";
        };
    }

    private static String bombTitle() {
        if (!ClientBombData.active) return null;
        return switch (ClientBombData.phase) {
            case CARRIED -> isLocalCarrier() ? "你携带 C4" : "C4 已发放";
            case DROPPED -> "C4 已掉落";
            case PLANTING -> "正在安装 C4";
            case PLANTED -> "C4 已安装";
            case DEFUSING -> "正在拆除 C4";
            case EXPLODED -> "C4 已引爆";
            case DEFUSED -> "C4 已拆除";
            default -> null;
        };
    }

    private static String bombDetail() {
        if (!ClientBombData.active) return null;
        String site = ClientBombData.bombSiteName.isBlank() ? "未标记地点" : ClientBombData.bombSiteName;
        return switch (ClientBombData.phase) {
            case CARRIED -> isLocalCarrier() ? "进入爆破区后按住右键安装"
                    : (ClientBombData.carrierName.isBlank() ? "等待携带者安装"
                    : "携带者：" + ClientBombData.carrierName);
            case DROPPED -> "前往掉落位置拾取";
            case PLANTING -> "正在安装于 " + site;
            case PLANTED -> "安装地点：" + site;
            case DEFUSING -> "正在拆除 " + site + " 的 C4";
            case EXPLODED -> "爆炸已发生";
            case DEFUSED -> "C4 已被拆除";
            default -> "";
        };
    }

    private static boolean isLocalCarrier() {
        var player = Minecraft.getInstance().player;
        return player != null && player.getGameProfile().getName().equals(ClientBombData.carrierName);
    }

    private static int seconds(int ticks) {
        return Math.max(0, (ticks + 19) / 20);
    }
}
