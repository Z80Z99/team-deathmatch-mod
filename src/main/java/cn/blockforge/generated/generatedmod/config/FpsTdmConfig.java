package cn.blockforge.generated.generatedmod.config;

import cn.blockforge.generated.generatedmod.match.AutoBalanceMode;
import cn.blockforge.generated.generatedmod.match.SpawnSelectionStrategy;
import cn.blockforge.generated.generatedmod.match.TeamChangePolicy;
import net.minecraftforge.common.ForgeConfigSpec;

public final class FpsTdmConfig {
    public static final ForgeConfigSpec SPEC;
    public static final Common COMMON;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        COMMON = new Common(builder);
        SPEC = builder.build();
    }

    private FpsTdmConfig() {
    }

    public static final class Common {
        public final ForgeConfigSpec.IntValue matchDurationSeconds;
        public final ForgeConfigSpec.IntValue targetKills;
        public final ForgeConfigSpec.IntValue roundWinTarget;
        public final ForgeConfigSpec.IntValue warmupDurationSeconds;
        public final ForgeConfigSpec.IntValue roundEndDelaySeconds;
        public final ForgeConfigSpec.IntValue matchEndDelaySeconds;
        public final ForgeConfigSpec.IntValue respawnDelaySeconds;
        public final ForgeConfigSpec.IntValue minPlayersPerTeam;
        public final ForgeConfigSpec.IntValue maxTeamImbalance;
        public final ForgeConfigSpec.EnumValue<TeamChangePolicy> teamChangePolicy;
        public final ForgeConfigSpec.EnumValue<AutoBalanceMode> autoBalanceMode;
        public final ForgeConfigSpec.EnumValue<SpawnSelectionStrategy> spawnSelectionStrategy;
        public final ForgeConfigSpec.BooleanValue requireBothTeams;
        public final ForgeConfigSpec.BooleanValue allowJoinDuringMatch;
        public final ForgeConfigSpec.BooleanValue joinDuringMatchAsSpectator;
        public final ForgeConfigSpec.BooleanValue friendlyFire;
        public final ForgeConfigSpec.BooleanValue keepInventoryOnDeath;
        public final ForgeConfigSpec.BooleanValue suppressDeathMessages;
        public final ForgeConfigSpec.BooleanValue autoRespawnEnabled;
        public final ForgeConfigSpec.BooleanValue autoReset;
        public final ForgeConfigSpec.BooleanValue protectArena;
        public final ForgeConfigSpec.BooleanValue enforceRegion;
        public final ForgeConfigSpec.IntValue maxBlocksPerTick;
        public final ForgeConfigSpec.IntValue maxSnapshotBlocks;

        private Common(ForgeConfigSpec.Builder builder) {
            builder.push("比赛");
            matchDurationSeconds = builder
                    .comment("单回合最长时间，设为 0 表示只使用击杀目标")
                    .defineInRange("matchDurationSeconds", 600, 0, 86400);
            targetKills = builder
                    .comment("单回合击杀目标，设为 0 表示只使用时间限制")
                    .defineInRange("targetKills", 25, 0, 1000000);
            roundWinTarget = builder
                    .comment("赢得整场比赛所需的回合胜场数，设为 1 即单回合比赛")
                    .defineInRange("roundWinTarget", 1, 1, 1000);
            warmupDurationSeconds = builder
                    .comment("热身阶段时长")
                    .defineInRange("warmupDurationSeconds", 15, 0, 3600);
            roundEndDelaySeconds = builder
                    .comment("回合结束到下一回合开始的间隔")
                    .defineInRange("roundEndDelaySeconds", 5, 1, 3600);
            matchEndDelaySeconds = builder
                    .comment("比赛结束画面持续时间")
                    .defineInRange("matchEndDelaySeconds", 15, 1, 3600);
            respawnDelaySeconds = builder
                    .comment("阵亡后恢复作战前的等待时间；配置键名保留为 respawnDelaySeconds 以兼容旧配置")
                    .defineInRange("respawnDelaySeconds", 5, 0, 3600);
            minPlayersPerTeam = builder
                    .comment("开始时每队建议的最少人数，只用于提示")
                    .defineInRange("minPlayersPerTeam", 1, 1, 100);
            builder.pop();

            builder.push("队伍");
            maxTeamImbalance = builder
                    .comment("自动平衡允许的最大人数差")
                    .defineInRange("maxTeamImbalance", 1, 0, 64);
            teamChangePolicy = builder
                    .comment("队伍更换规则")
                    .defineEnum("teamChangePolicy", TeamChangePolicy.ONLY_BEFORE_MATCH);
            autoBalanceMode = builder
                    .comment("自动平衡触发时机")
                    .defineEnum("autoBalanceMode", AutoBalanceMode.ON_JOIN_AND_MATCH_START);
            requireBothTeams = builder
                    .comment("是否要求 A、B 两队都有人才能开始")
                    .define("requireBothTeams", false);
            allowJoinDuringMatch = builder
                    .comment("是否允许比赛进行时加入，加入者仍需遵守队伍锁定规则")
                    .define("allowJoinDuringMatch", true);
            joinDuringMatchAsSpectator = builder
                    .comment("比赛中途加入者是否等到下一回合再进入战斗")
                    .define("joinDuringMatchAsSpectator", true);
            friendlyFire = builder
                    .comment("是否允许队友伤害")
                    .define("friendlyFire", false);
            builder.pop();

            builder.push("出生点");
            spawnSelectionStrategy = builder
                    .comment("出生点选择策略")
                    .defineEnum("spawnSelectionStrategy", SpawnSelectionStrategy.RANDOM);
            enforceRegion = builder
                    .comment("是否将玩家限制在当前地图的 bounds 内")
                    .define("enforceRegion", true);
            maxBlocksPerTick = builder
                    .comment("地图快照捕获和恢复每个服务器 Tick 最多处理的方块数")
                    .defineInRange("maxBlocksPerTick", 4096, 1, 1000000);
            maxSnapshotBlocks = builder
                    .comment("单张地图允许保存的最大快照方块数，过大的区域会被拒绝以避免内存耗尽")
                    .defineInRange("maxSnapshotBlocks", 4000000, 1, 50000000);
            builder.pop();

            builder.push("比赛行为");
            keepInventoryOnDeath = builder
                    .comment("比赛期间是否保留死亡玩家背包")
                    .define("keepInventoryOnDeath", true);
            suppressDeathMessages = builder
                    .comment("比赛期间是否隐藏原版死亡消息，避免与外部击杀反馈重叠")
                    .define("suppressDeathMessages", true);
            autoRespawnEnabled = builder
                    .comment("是否在等待时间结束后恢复阵亡玩家；配置键名保留为 autoRespawnEnabled 以兼容旧配置")
                    .define("autoRespawnEnabled", true);
            autoReset = builder
                    .comment("比赛结束后是否自动回到等待状态")
                    .define("autoReset", true);
            protectArena = builder
                    .comment("兼容旧配置项；比赛期间不会拦截 Explosion Overhaul、TACZ 或玩家造成的地图破坏")
                    .define("protectArena", true);
            builder.pop();
        }
    }
}
