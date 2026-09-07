package cn.blockforge.generated.generatedmod.map;

public enum MapEditorAction {
    REQUEST,
    /** 创建一张归属于当前玩家的地图（所有玩家可用）。 */
    CREATE_MAP,
    /** 选择一张自己可编辑的地图作为编辑目标。 */
    EDIT_MAP,
    /** 删除自己的地图。 */
    DELETE_MAP,
    /** 为自己的地图生成（或复用）邀请码。 */
    GENERATE_SHARE,
    /** 撤销自己地图的邀请码。 */
    REVOKE_SHARE,
    /** 用邀请码导入一份属于自己的独立副本。 */
    IMPORT_MAP,
    SET_BOUNDS_MIN,
    SET_BOUNDS_MAX,
    SET_RESET_MIN,
    SET_RESET_MAX,
    APPLY_REGIONS,
    ADD_TEAM_A,
    ADD_TEAM_B,
    SET_SPECTATOR,
    CLEAR_TEAM_A,
    CLEAR_TEAM_B,
    CLEAR_SPECTATOR,
    /** 客户端后台轮询，只读取状态，不确认或覆盖草稿失效告警。 */
    POLL,
    ADD_TEAM_C, ADD_TEAM_D, CLEAR_TEAM_C, CLEAR_TEAM_D
}
