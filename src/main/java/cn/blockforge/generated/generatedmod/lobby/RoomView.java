package cn.blockforge.generated.generatedmod.lobby;

import java.util.List;

/**
 * 发给客户端的房间只读视图；rules 为该房间的独立比赛规则，只有房主可改，随房间解散销毁；
 * locked 表示该房设置了访问密码（密码本身永不下发）。
 */
public record RoomView(String id, String name, String owner, int memberCount, int maxPlayers,
                       String mapId, RoomState state, List<String> members,
                       RoomRules rules, boolean matchmaking, boolean locked) {
    public RoomView {
        members = List.copyOf(members == null ? List.of() : members);
        rules = rules == null ? RoomRules.fallback() : rules;
    }
}
