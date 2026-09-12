package cn.blockforge.generated.generatedmod.command;

import cn.blockforge.generated.generatedmod.integration.IntegrationManager;
import cn.blockforge.generated.generatedmod.map.MapDefinition;
import cn.blockforge.generated.generatedmod.map.MapManager;
import cn.blockforge.generated.generatedmod.match.MatchManager;
import cn.blockforge.generated.generatedmod.match.Team;
import cn.blockforge.generated.generatedmod.spawn.SpawnManager;
import cn.blockforge.generated.generatedmod.spawn.SpawnPoint;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 比赛管理员和玩家使用的 /fps 命令。 */
public final class FpsCommand {
    private static final int ADMIN_PERMISSION = 2;

    private FpsCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("fps")
                .then(Commands.literal("start")
                        .requires(source -> source.hasPermission(ADMIN_PERMISSION))
                        .executes(FpsCommand::start))
                .then(Commands.literal("stop")
                        .requires(source -> source.hasPermission(ADMIN_PERMISSION))
                        .executes(FpsCommand::stop))
                .then(Commands.literal("restart")
                        .requires(source -> source.hasPermission(ADMIN_PERMISSION))
                        .executes(FpsCommand::restart))
                .then(Commands.literal("status")
                        .executes(FpsCommand::status))
                .then(Commands.literal("hudstat")
                        .requires(source -> source.hasPermission(ADMIN_PERMISSION))
                        .then(Commands.literal("list")
                                .executes(FpsCommand::hudStatList))
                        .then(Commands.literal("set")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("value", IntegerArgumentType.integer())
                                                .then(Commands.argument("maximum", IntegerArgumentType.integer(0))
                                                        .executes(FpsCommand::hudStatSet)))))
                        .then(Commands.literal("name")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("label", StringArgumentType.greedyString())
                                                .executes(FpsCommand::hudStatName))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(FpsCommand::hudStatRemove)))
                        .then(Commands.literal("clear")
                                .executes(FpsCommand::hudStatClear)))
                .then(Commands.literal("integration")
                        .requires(source -> source.hasPermission(ADMIN_PERMISSION))
                        .executes(FpsCommand::integration))
                .then(Commands.literal("money")
                        .then(Commands.literal("balance")
                                .executes(FpsCommand::moneyBalanceSelf)
                                .then(Commands.argument("player", EntityArgument.player())
                                        .requires(source -> source.hasPermission(ADMIN_PERMISSION))
                                        .executes(FpsCommand::moneyBalance)))
                        .then(Commands.literal("add")
                                .requires(source -> source.hasPermission(ADMIN_PERMISSION))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                .executes(FpsCommand::moneyAdd))))
                        .then(Commands.literal("set")
                                .requires(source -> source.hasPermission(ADMIN_PERMISSION))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(FpsCommand::moneySet)))))
                .then(Commands.literal("economy")
                        .then(Commands.literal("reload")
                                .requires(source -> source.hasPermission(ADMIN_PERMISSION))
                                .executes(FpsCommand::economyReload)))
                .then(Commands.literal("shop")
                        .then(Commands.literal("list")
                                .executes(FpsCommand::shopList))
                        .then(Commands.literal("addheld")
                                .requires(source -> source.hasPermission(ADMIN_PERMISSION))
                                .executes(FpsCommand::shopAddHeld))
                        .then(Commands.literal("price")
                                .requires(source -> source.hasPermission(ADMIN_PERMISSION))
                                .then(Commands.argument("entry", StringArgumentType.word())
                                        .then(Commands.argument("price", IntegerArgumentType.integer(0))
                                                .executes(FpsCommand::shopPrice)))))
                .then(Commands.literal("map")
                        .then(Commands.literal("list")
                                .executes(FpsCommand::mapList))
                        .then(Commands.literal("current")
                                .executes(FpsCommand::mapCurrent))
                        .then(Commands.literal("load")
                                .requires(source -> source.hasPermission(ADMIN_PERMISSION))
                                .then(Commands.argument("map", StringArgumentType.word())
                                        .executes(FpsCommand::mapLoad)))
                        .then(Commands.literal("reload")
                                .requires(source -> source.hasPermission(ADMIN_PERMISSION))
                                .executes(FpsCommand::mapReload)))
                .then(Commands.literal("setspawn")
                        .requires(source -> source.hasPermission(ADMIN_PERMISSION))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .executes(FpsCommand::setSpawn)))
                .then(Commands.literal("join")
                        .executes(FpsCommand::join)
                        .then(Commands.argument("team", StringArgumentType.word())
                                .executes(FpsCommand::joinSpecific)))
                .then(Commands.literal("leave")
                        .executes(FpsCommand::leave))
                .then(Commands.literal("team")
                        .then(Commands.argument("team", StringArgumentType.word())
                                .executes(FpsCommand::changeOwnTeam))
                        .then(Commands.literal("player")
                                .requires(source -> source.hasPermission(ADMIN_PERMISSION))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("team", StringArgumentType.word())
                                                .executes(FpsCommand::changeOtherTeam)))))
                .then(Commands.literal("spawn")
                        .requires(source -> source.hasPermission(ADMIN_PERMISSION))
                        .then(Commands.literal("add")
                                .then(Commands.argument("team", StringArgumentType.word())
                                        .executes(FpsCommand::addSpawn)))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("team", StringArgumentType.word())
                                        .executes(FpsCommand::clearSpawns)))
                        .then(Commands.literal("list")
                                .executes(FpsCommand::listSpawns)))
                .then(Commands.literal("lobby")
                        .requires(source -> source.hasPermission(ADMIN_PERMISSION))
                        .then(Commands.literal("set")
                                .executes(FpsCommand::setLobby))
                        .then(Commands.literal("clear")
                                .executes(FpsCommand::clearLobby))
                        .then(Commands.literal("info")
                                .executes(FpsCommand::lobbyInfo)))
                .then(Commands.literal("region")
                        .then(Commands.literal("info")
                                .executes(FpsCommand::regionInfo))
                        .then(Commands.literal("pos1")
                                .requires(source -> source.hasPermission(ADMIN_PERMISSION))
                                .executes(FpsCommand::legacyRegionEdit))
                        .then(Commands.literal("pos2")
                                .requires(source -> source.hasPermission(ADMIN_PERMISSION))
                                .executes(FpsCommand::legacyRegionEdit))
                        .then(Commands.literal("clear")
                                .requires(source -> source.hasPermission(ADMIN_PERMISSION))
                                .executes(FpsCommand::legacyRegionEdit))));
    }

    private static int start(CommandContext<CommandSourceStack> context) {
        MatchManager manager = manager(context);
        if (manager == null) {
            return 0;
        }
        MatchManager.StartResult result = manager.startMatch();
        if (result == MatchManager.StartResult.STARTED) {
            success(context, "比赛已开始。");
            return 1;
        }
        failure(context, startResultText(result));
        return 0;
    }

    private static int stop(CommandContext<CommandSourceStack> context) {
        MatchManager manager = manager(context);
        if (manager == null) {
            return 0;
        }
        if (!manager.stopMatch()) {
            failure(context, "当前没有正在进行的比赛。");
            return 0;
        }
        success(context, "比赛已停止。");
        return 1;
    }

    private static int restart(CommandContext<CommandSourceStack> context) {
        MatchManager manager = manager(context);
        if (manager == null) {
            return 0;
        }
        MatchManager.StartResult result = manager.restartMatch();
        if (result == MatchManager.StartResult.STARTED) {
            success(context, "比赛已重新开始。");
            return 1;
        }
        failure(context, "比赛无法重新开始：" + startResultText(result));
        return 0;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        MatchManager manager = manager(context);
        if (manager == null) {
            return 0;
        }
        success(context, manager.statusLine() + "；" + manager.maps().statusLine());
        return 1;
    }

    private static int integration(CommandContext<CommandSourceStack> context) {
        success(context, IntegrationManager.statusText());
        return 1;
    }

    private static int mapList(CommandContext<CommandSourceStack> context) {
        MatchManager manager = manager(context);
        if (manager == null) {
            return 0;
        }
        if (manager.maps().registry().definitions().isEmpty()) {
            failure(context, "没有地图配置，请在 config/fpsmod/maps/ 添加 JSON 文件。");
            return 0;
        }
        StringBuilder message = new StringBuilder("已注册地图：");
        for (MapDefinition definition : manager.maps().registry().definitions()) {
            message.append("\n").append(definition.id()).append("（").append(definition.displayName()).append("）");
            if (definition.id().equals(manager.maps().currentMapId())) {
                message.append(" [当前]");
            }
        }
        success(context, message.toString());
        return 1;
    }

    private static int mapCurrent(CommandContext<CommandSourceStack> context) {
        MatchManager manager = manager(context);
        if (manager == null) {
            return 0;
        }
        success(context, manager.maps().statusLine());
        return 1;
    }

    private static int mapLoad(CommandContext<CommandSourceStack> context) {
        MatchManager manager = manager(context);
        if (manager == null) {
            return 0;
        }
        if (manager.state() != cn.blockforge.generated.generatedmod.match.MatchState.WAITING) {
            failure(context, "比赛进行中不能切换地图，请先停止比赛。");
            return 0;
        }
        String id = StringArgumentType.getString(context, "map");
        MapManager.LoadResult result = manager.maps().loadMap(id);
        switch (result) {
            case STARTED -> success(context, "已选择地图 " + id + "，正在分 Tick 捕获初始快照。");
            case ALREADY_CURRENT -> success(context, "地图已经是当前地图，初始快照已就绪。");
            case LOADING -> success(context, "地图初始快照仍在捕获中：" + manager.maps().resetProgress());
            case NOT_FOUND -> failure(context, "未找到地图配置：" + id);
            case INCOMPLETE -> failure(context, "地图尚未完成：请先创建地图边界和重置区域。");
            case BUSY -> failure(context, "地图正在恢复，暂时不能切换。");
            case FAILED -> failure(context, "地图无法加载：" + manager.maps().resetManager().lastError());
        }
        return result == MapManager.LoadResult.STARTED || result == MapManager.LoadResult.ALREADY_CURRENT
                || result == MapManager.LoadResult.LOADING ? 1 : 0;
    }

    private static int mapReload(CommandContext<CommandSourceStack> context) {
        MatchManager manager = manager(context);
        if (manager == null) {
            return 0;
        }
        if (manager.state() != cn.blockforge.generated.generatedmod.match.MatchState.WAITING) {
            failure(context, "比赛进行中不能重载地图配置，请先停止比赛。");
            return 0;
        }
        int count = manager.maps().reloadMaps();
        success(context, "已重载 " + count + " 张地图配置。当前地图：" + manager.maps().currentMapId());
        return 1;
    }

    private static int setSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MatchManager manager = manager(context);
        if (manager == null) {
            return 0;
        }
        if (!ensureMapEditable(context, manager)) {
            return 0;
        }
        ServerPlayer player = context.getSource().getPlayerOrException();
        String type = StringArgumentType.getString(context, "type");
        Team team = Team.parse(type);
        SpawnPoint point = new SpawnPoint(player.serverLevel().dimension(), player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot());
        boolean updated;
        if (team == Team.SPECTATOR) {
            updated = manager.spawns().setSpectatorSpawn(point);
        } else if (team != null && team.isPlayable()) {
            updated = manager.spawns().addTeamSpawn(team, point);
        } else {
            failure(context, "出生点类型必须是 team_a、team_b 或 spectator。");
            return 0;
        }
        if (!updated) {
            failure(context, "出生点写入失败；请确认玩家位于当前地图维度。");
            return 0;
        }
        success(context, "已设置 " + (team == Team.SPECTATOR ? "观战" : team.displayName())
                + " 出生点：" + point.description());
        return 1;
    }

    private static boolean ensureMapEditable(CommandContext<CommandSourceStack> context, MatchManager manager) {
        if (manager.state() != cn.blockforge.generated.generatedmod.match.MatchState.WAITING) {
            failure(context, "比赛进行中不能修改出生点。");
            return false;
        }
        if (manager.maps().currentMap().isEmpty()) {
            failure(context, "尚未选择当前地图，请先执行 /fps map load <map>。");
            return false;
        }
        return true;
    }

    private static int join(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MatchManager manager = manager(context);
        if (manager == null) {
            return 0;
        }
        ServerPlayer player = context.getSource().getPlayerOrException();
        return reportJoin(context, manager, player, manager.teamManager().getPreference(player));
    }

    private static int joinSpecific(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MatchManager manager = manager(context);
        if (manager == null) {
            return 0;
        }
        ServerPlayer player = context.getSource().getPlayerOrException();
        Team requested = parseTeam(context, "team");
        if (requested == null || !requested.isPlayable()) {
            failure(context, "队伍必须是 a、b、c 或 d，且必须在当前比赛中启用。");
            return 0;
        }
        manager.teamManager().setPreference(player, requested);
        return reportJoin(context, manager, player, requested);
    }

    private static int reportJoin(CommandContext<CommandSourceStack> context, MatchManager manager,
                                  ServerPlayer player, Team requested) {
        Team current = manager.teamManager().getTeam(player);
        if (current == requested && !manager.teamManager().isPending(player)) {
            success(context, "你已经在 " + requested.displayName() + "。");
            return 1;
        }
        TeamManagerJoinResult result = TeamManagerJoinResult.from(manager.teamManager().joinPlayer(player));
        switch (result) {
            case JOINED -> success(context, "已加入 " + manager.teamManager().getTeam(player).displayName() + "。");
            case QUEUED -> success(context, "已加入候场队列，将在下一回合进入 " + requested.displayName() + "。");
            case FULL -> failure(context, "当前队伍不符合自动平衡规则，请选择另一队。");
            case LOCKED -> failure(context, "比赛进行中不允许加入。");
        }
        return result.success() ? 1 : 0;
    }

    private static int leave(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MatchManager manager = manager(context);
        if (manager == null) {
            return 0;
        }
        ServerPlayer player = context.getSource().getPlayerOrException();
        manager.teamManager().leavePlayer(player);
        success(context, "你已离开比赛并进入观战状态。");
        manager.broadcastMatchState();
        return 1;
    }

    private static int changeOwnTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MatchManager manager = manager(context);
        if (manager == null) {
            return 0;
        }
        ServerPlayer player = context.getSource().getPlayerOrException();
        Team requested = parseTeam(context, "team");
        return reportTeamChange(context, manager, player, requested);
    }

    private static int changeOtherTeam(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MatchManager manager = manager(context);
        if (manager == null) {
            return 0;
        }
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        Team requested = parseTeam(context, "team");
        return reportTeamChange(context, manager, player, requested);
    }

    private static int reportTeamChange(CommandContext<CommandSourceStack> context, MatchManager manager,
                                        ServerPlayer player, Team requested) {
        if (requested == null) {
            failure(context, "队伍必须是 a、b 或 spectator。");
            return 0;
        }
        switch (manager.teamManager().changeTeam(player, requested)) {
            case CHANGED -> {
                success(context, player.getGameProfile().getName() + " 已加入 " + requested.displayName() + "。");
                manager.broadcastMatchState();
                return 1;
            }
            case INVALID -> failure(context, "无效的队伍。");
            case LOCKED -> failure(context, "当前比赛阶段不允许更换队伍。");
            case ALIVE -> failure(context, "玩家存活时不能更换队伍。");
            case BALANCED -> failure(context, "更换后队伍人数差会超过配置限制。");
        }
        return 0;
    }

    private static int addSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MatchManager manager = manager(context);
        if (manager == null || !ensureMapEditable(context, manager)) {
            return 0;
        }
        ServerPlayer player = context.getSource().getPlayerOrException();
        Team team = parseTeam(context, "team");
        if (team == null || !team.isPlayable()) {
            failure(context, "出生点队伍必须是 a、b、c 或 d。");
            return 0;
        }
        SpawnPoint point = new SpawnPoint(player.serverLevel().dimension(), player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot());
        if (!manager.spawns().addTeamSpawn(team, point)) {
            failure(context, "出生点写入失败；请确认玩家位于当前地图维度。");
            return 0;
        }
        success(context, "已为 " + team.displayName() + " 添加出生点：" + point.description());
        return 1;
    }

    private static int clearSpawns(CommandContext<CommandSourceStack> context) {
        MatchManager manager = manager(context);
        if (manager == null || !ensureMapEditable(context, manager)) {
            return 0;
        }
        Team team = parseTeam(context, "team");
        if (team == null || !team.isPlayable()) {
            failure(context, "出生点队伍必须是 a、b、c 或 d。");
            return 0;
        }
        if (!manager.spawns().clearTeamSpawns(team)) {
            failure(context, "清除出生点失败。");
            return 0;
        }
        success(context, "已清除 " + team.displayName() + " 的全部出生点。");
        return 1;
    }

    private static int listSpawns(CommandContext<CommandSourceStack> context) {
        MatchManager manager = manager(context);
        if (manager == null) {
            return 0;
        }
        StringBuilder message = new StringBuilder("出生点：");
        for (Team team : Team.values()) {
            message.append("\n").append(team.displayName()).append("（")
                    .append(manager.spawns().spawnCount(team)).append("）：");
            int index = 1;
            for (SpawnPoint point : manager.spawns().getSpawns(team)) {
                message.append("\n  ").append(index++).append(". ").append(point.description());
            }
        }
        success(context, message.toString());
        return 1;
    }

    private static int setLobby(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return setSpawnForType(context, "spectator");
    }

    private static int clearLobby(CommandContext<CommandSourceStack> context) {
        MatchManager manager = manager(context);
        if (manager == null || !ensureMapEditable(context, manager)) {
            return 0;
        }
        if (!manager.spawns().clearSpectatorSpawn()) {
            failure(context, "清除观战出生点失败。");
            return 0;
        }
        success(context, "已清除观战出生点，将使用地图世界出生点。");
        return 1;
    }

    private static int lobbyInfo(CommandContext<CommandSourceStack> context) {
        MatchManager manager = manager(context);
        if (manager == null) {
            return 0;
        }
        success(context, "观战出生点：" + manager.spawns().spectatorDescription());
        return 1;
    }

    private static int setSpawnForType(CommandContext<CommandSourceStack> context, String type)
            throws CommandSyntaxException {
        MatchManager manager = manager(context);
        if (manager == null || !ensureMapEditable(context, manager)) {
            return 0;
        }
        ServerPlayer player = context.getSource().getPlayerOrException();
        SpawnPoint point = new SpawnPoint(player.serverLevel().dimension(), player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot());
        if (!manager.spawns().setSpectatorSpawn(point)) {
            failure(context, "观战出生点写入失败；请确认玩家位于当前地图维度。");
            return 0;
        }
        success(context, "已设置观战出生点：" + point.description());
        return 1;
    }

    private static int regionInfo(CommandContext<CommandSourceStack> context) {
        MatchManager manager = manager(context);
        if (manager == null) {
            return 0;
        }
        success(context, "当前地图边界：" + manager.spawns().boundsDescription()
                + "（由地图 JSON 的 bounds 配置）");
        return 1;
    }

    private static int legacyRegionEdit(CommandContext<CommandSourceStack> context) {
        failure(context, "区域边界已改为地图 JSON 配置，请编辑当前地图的 bounds 后执行 /fps map reload。");
        return 0;
    }

    private static MatchManager manager(CommandContext<CommandSourceStack> context) {
        MatchManager manager = MatchManager.get();
        if (manager == null) {
            failure(context, "比赛系统尚未初始化。");
        }
        return manager;
    }

    private static Team parseTeam(CommandContext<CommandSourceStack> context, String argument) {
        return Team.parse(StringArgumentType.getString(context, argument));
    }

    private static String startResultText(MatchManager.StartResult result) {
        return switch (result) {
            case STARTED -> "比赛已开始。";
            case ALREADY_ACTIVE -> "比赛已经在进行中。";
            case NEED_BOTH_TEAMS -> "A队 和 B队 都需要有玩家。";
            case NO_PLAYERS -> "没有可参加比赛的玩家。";
            case NO_END_CONDITION -> "击杀目标和时间限制不能同时为 0。";
            case NO_MAP -> "尚未选择地图，请先执行 /fps map load <map>。";
            case MAP_INCOMPLETE -> "地图尚未完成，请先创建地图边界和重置区域。";
            case MAP_LOADING -> "地图初始快照仍在捕获，请稍后再试。";
            case MAP_NOT_READY -> "当前地图快照不可用，请检查地图配置和服务器日志。";
            case NO_TEAM_SPAWNS -> "每支启用队伍至少需要一个出生点。";
            case NO_SAFE_RANDOM_SPAWN -> "地图内未找到安全随机出生位置，请检查地面支撑、头顶空间和通路。";
            case NO_BOMB_SITES -> "爆破模式至少需要一个激活的爆破区（BOMB 区域）。";
            case MAP_BUSY -> "地图正在恢复，请等待恢复完成。";
        };
    }

    private static void success(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendSuccess(() -> Component.literal(message), false);
    }

    // ------------------------------------------------------ /fps money 与 /fps shop

    private static int moneyBalanceSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        return moneyMessage(context, player, "你的");
    }

    private static int moneyBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return moneyMessage(context, EntityArgument.getPlayer(context, "player"), "玩家");
    }

    private static int moneyMessage(CommandContext<CommandSourceStack> context,
                                    ServerPlayer player, String prefix) {
        MatchManager manager = MatchManager.get();
        if (manager == null) return unavailable(context);
        success(context, prefix + " " + player.getGameProfile().getName()
                + "：大厅账户 $" + manager.economy().globalBalance(player.getUUID())
                + "，比赛资金 $" + manager.economy().matchBalance(player.getUUID()));
        return 1;
    }

    private static int moneyAdd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MatchManager manager = MatchManager.get();
        if (manager == null) return unavailable(context);
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        int value = manager.economy().addBalance(player.getUUID(), amount, false);
        success(context, "已把 " + player.getGameProfile().getName()
                + " 的大厅账户调整为 $" + value + "。");
        return 1;
    }

    private static int moneySet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MatchManager manager = MatchManager.get();
        if (manager == null) return unavailable(context);
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        int value = manager.economy().setBalance(player.getUUID(),
                IntegerArgumentType.getInteger(context, "amount"), false);
        success(context, "已设置 " + player.getGameProfile().getName()
                + " 的大厅账户为 $" + value + "。");
        return 1;
    }

    private static int economyReload(CommandContext<CommandSourceStack> context) {
        MatchManager manager = MatchManager.get();
        if (manager == null) return unavailable(context);
        manager.economy().reload();
        success(context, "经济配置已重载：" + manager.economy().config().toJson().toString());
        return 1;
    }

    private static int shopList(CommandContext<CommandSourceStack> context) {
        MatchManager manager = MatchManager.get();
        if (manager == null) return unavailable(context);
        var items = manager.weapons().repository();
        if (items.isEmpty()) {
            success(context, "武器仓库暂无可购买条目。先在“武器仓库”界面把目录物品加入仓库。");
            return 0;
        }
        StringBuilder builder = new StringBuilder("商店条目：");
        for (var item : items) {
            int price = manager.economy().price(item.id(), item.categoryId(),
                    Math.max(1, item.snapshot().stack().getCount()));
            builder.append("\n").append(item.id()).append("  $").append(price).append("  ")
                    .append(item.title());
        }
        success(context, builder.toString());
        return items.size();
    }

    private static int shopPrice(CommandContext<CommandSourceStack> context) {
        MatchManager manager = MatchManager.get();
        if (manager == null) return unavailable(context);
        String entry = StringArgumentType.getString(context, "entry");
        int price = IntegerArgumentType.getInteger(context, "price");
        manager.economy().setItemPrice(entry, price);
        success(context, "已把仓库条目 " + entry + " 的价格固定为 $" + price
                + "（0 表示免费）。");
        return 1;
    }

    private static int shopAddHeld(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        MatchManager manager = MatchManager.get();
        if (manager == null) return unavailable(context);
        ServerPlayer player = context.getSource().getPlayerOrException();
        manager.weapons().handleAction(player,
                cn.blockforge.generated.generatedmod.weapon.WeaponRepositoryAction.ADD_HELD,
                "", "", 0);
        success(context, "已请求把手持物品加入商店仓库。 ");
        return 1;
    }

    private static int unavailable(CommandContext<CommandSourceStack> context) {
        failure(context, "比赛管理器尚未初始化。");
        return 0;
    }

    private static void failure(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendFailure(Component.literal(message));
    }

    // ------------------------------------------------------ /fps hudstat 自定义统计通道

    private static int hudStatSet(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        int value = IntegerArgumentType.getInteger(context, "value");
        int maximum = IntegerArgumentType.getInteger(context, "maximum");
        try {
            cn.blockforge.generated.generatedmod.match.HudStatStore.get()
                    .set(id, null, value, maximum);
        } catch (IllegalArgumentException error) {
            failure(context, error.getMessage());
            return 0;
        }
        cn.blockforge.generated.generatedmod.match.HudStatStore.get()
                .broadcast(context.getSource().getServer());
        success(context, "自定义统计 " + id + " = " + value
                + (maximum > 0 ? "/" + maximum : "") + "，已推送全体客户端（HUD 数据源·自定义）。");
        return 1;
    }

    private static int hudStatName(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        String label = StringArgumentType.getString(context, "label");
        if (!cn.blockforge.generated.generatedmod.match.HudStatStore.get().rename(id, label)) {
            failure(context, "尚不存在标识 " + id + "，请先 /fps hudstat set " + id + " <数值> <上限>。");
            return 0;
        }
        cn.blockforge.generated.generatedmod.match.HudStatStore.get()
                .broadcast(context.getSource().getServer());
        success(context, "已把统计 " + id + " 的显示名改为「" + label + "」。");
        return 1;
    }

    private static int hudStatRemove(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        if (!cn.blockforge.generated.generatedmod.match.HudStatStore.get().remove(id)) {
            failure(context, "找不到标识 " + id + " 的自定义统计。");
            return 0;
        }
        cn.blockforge.generated.generatedmod.match.HudStatStore.get()
                .broadcast(context.getSource().getServer());
        success(context, "已移除统计 " + id + "。");
        return 1;
    }

    private static int hudStatClear(CommandContext<CommandSourceStack> context) {
        cn.blockforge.generated.generatedmod.match.HudStatStore.get().clear();
        cn.blockforge.generated.generatedmod.match.HudStatStore.get()
                .broadcast(context.getSource().getServer());
        success(context, "已清空全部自定义统计。");
        return 1;
    }

    private static int hudStatList(CommandContext<CommandSourceStack> context) {
        var entries = cn.blockforge.generated.generatedmod.match.HudStatStore.get().snapshot();
        if (entries.isEmpty()) {
            success(context, "当前没有自定义统计。用法：/fps hudstat set <标识> <数值> <上限>，"
                    + "再用 /fps hudstat name <标识> <显示名> 起中文名。");
            return 0;
        }
        StringBuilder builder = new StringBuilder("自定义统计：");
        for (var entry : entries) {
            builder.append("\n").append(entry.id()).append("  「").append(entry.label())
                    .append("」  ").append(entry.value())
                    .append(entry.maximum() > 0 ? " / " + entry.maximum() : "");
        }
        success(context, builder.toString());
        return entries.size();
    }

    private enum TeamManagerJoinResult {
        JOINED(true),
        QUEUED(true),
        FULL(false),
        LOCKED(false);

        private final boolean success;

        TeamManagerJoinResult(boolean success) {
            this.success = success;
        }

        public boolean success() {
            return success;
        }

        public static TeamManagerJoinResult from(cn.blockforge.generated.generatedmod.team.TeamManager.JoinResult result) {
            return switch (result) {
                case JOINED -> JOINED;
                case QUEUED -> QUEUED;
                case FULL -> FULL;
                case LOCKED -> LOCKED;
            };
        }
    }
}
