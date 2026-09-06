package cn.blockforge.generated.generatedmod.lobby;

/** 快速匹配队列状态快照；readySeconds > 0 表示已匹配并处于开赛准备倒计时。 */
public record MatchmakingStatus(boolean queued, int position, int queueSize, int waitedTicks,
                                int readySeconds, String message, boolean error) {
    public MatchmakingStatus {
        message = message == null ? "" : message;
    }
}
