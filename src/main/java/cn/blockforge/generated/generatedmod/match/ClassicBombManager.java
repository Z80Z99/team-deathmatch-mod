package cn.blockforge.generated.generatedmod.match;

import cn.blockforge.generated.generatedmod.item.ModItems;
import cn.blockforge.generated.generatedmod.map.MapDefinition;
import cn.blockforge.generated.generatedmod.map.MapRegion;
import cn.blockforge.generated.generatedmod.network.packet.BombSyncPacket;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** Server-authoritative manager for the two-team classic bomb objective. */
public final class ClassicBombManager {
    private static final String MATCH_ITEM_TAG = "generated_mod_classic_bomb";
    private static final String ROUND_TAG = "generated_mod_bomb_round";
    private static final String ROLE_TAG = "generated_mod_bomb_role";
    private static final int DEFUSE_DISTANCE_SQUARED = 9;

    private final MatchManager match;
    private final MinecraftServer server;
    private final ClassicBombState state = new ClassicBombState();
    private final Random random = new Random();
    private List<MapRegion> activeSites = List.of();
    private ItemEntity droppedC4;
    private ItemEntity plantedC4;

    ClassicBombManager(MatchManager match) {
        this.match = match;
        this.server = match.server();
    }

    public ClassicBombState state() {
        return state;
    }

    public List<MapRegion> activeSites() {
        return activeSites;
    }

    public boolean hasBombSites() {
        return !activeBombSites().isEmpty();
    }

    public boolean startRound() {
        if (match.rulesMode() != GameMode.SEARCH_DESTROY) {
            return false;
        }
        cleanupRoundItems();
        activeSites = activeBombSites();
        Team attacking = match.attackingTeam();
        Team defending = match.defendingTeam();
        if (activeSites.isEmpty() || attacking == null || defending == null) {
            return false;
        }
        List<ServerPlayer> attackers = teamPlayers(attacking);
        if (attackers.isEmpty()) {
            return false;
        }
        ServerPlayer carrier = attackers.get(random.nextInt(attackers.size()));
        giveItem(carrier, createRoundItem("c4"));
        for (ServerPlayer defender : teamPlayers(defending)) {
            giveItem(defender, createRoundItem("defuse_kit"));
        }
        state.startRound(carrier.getUUID(), server.getTickCount());
        sync();
        match.recordEvent(carrier.getGameProfile().getName()
                + " 携带 C4：进入爆破区后按住右键安装。");
        for (ServerPlayer defender : teamPlayers(defending)) {
            match.recordEvent(defender.getGameProfile().getName()
                    + " 获得拆弹器：C4 安装后靠近它按住右键拆除。");
        }
        match.recordEvent("本回合 " + attacking.displayName() + " 进攻，"
                + defending.displayName() + " 防守。");
        return true;
    }

    public void tick() {
        if (state.phase() == ClassicBombState.Phase.INACTIVE || state.isTerminal()) {
            return;
        }
        long now = server.getTickCount();
        validateCarrier(now);
        validateAction(now);
        if (state.isActionActive()) {
            int duration = state.phase() == ClassicBombState.Phase.PLANTING
                    ? match.rulesBombPlantTicks() : match.rulesBombDefuseTicks();
            if (state.advanceAction(now, duration)) {
                finishAction(now);
            }
        }
        if ((state.phase() == ClassicBombState.Phase.PLANTED || state.phase() == ClassicBombState.Phase.DEFUSING)
                && state.detonationRemainingTicks(now) <= 0) {
            explode(now);
        }
        trackDroppedC4(now);
        if (now % 10L == 0L) sync();
    }

    public boolean requestPlant(ServerPlayer player) {
        if (match.rulesMode() != GameMode.SEARCH_DESTROY || match.state() != MatchState.PLAYING
                || player == null) {
            return false;
        }
        if (state.phase() == ClassicBombState.Phase.PLANTING
                && player.getUUID().equals(state.operatorId())) {
            return true;
        }
        if (state.phase() == ClassicBombState.Phase.DROPPED && holdsRoundItem(player, "c4")) {
            state.pickup(player.getUUID());
        }
        if (state.phase() != ClassicBombState.Phase.CARRIED
                || !player.getUUID().equals(state.carrierId())
                || match.teamManager().getTeam(player) != match.attackingTeam()) {
            return false;
        }
        MapRegion site = activeSiteAt(player);
        if (site == null) {
            match.recordEvent(player.getGameProfile().getName()
                    + " 尝试安装 C4，但不在激活的爆破区内。");
            match.publishHudEvent(player, MatchHudEventType.BOMB_SITE_REQUIRED,
                    "进入激活的爆破区域后再安装 C4", 70);
            return false;
        }
        state.startPlanting(player.getUUID(), server.getTickCount(),
                player.getX(), player.getY(), player.getZ());
        match.recordEvent(player.getGameProfile().getName() + " 正在安装 C4。");
        match.publishHudEvent(player, MatchHudEventType.BOMB_PLANTING,
                "保持安装动作直到进度完成", match.rulesBombPlantTicks());
        player.playNotifySound(SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 0.7F, 1.0F);
        return true;
    }

    public boolean requestDefuse(ServerPlayer player) {
        if (match.rulesMode() != GameMode.SEARCH_DESTROY || match.state() != MatchState.PLAYING
                || player == null) {
            return false;
        }
        if (state.phase() == ClassicBombState.Phase.DEFUSING
                && player.getUUID().equals(state.operatorId())) {
            return true;
        }
        if (state.phase() != ClassicBombState.Phase.PLANTED
                || match.teamManager().getTeam(player) != match.defendingTeam()
                || !holdsRoundItem(player, "defuse_kit")
                || distanceSquared(player, state.x(), state.y(), state.z()) > DEFUSE_DISTANCE_SQUARED) {
            return false;
        }
        state.startDefusing(player.getUUID(), server.getTickCount(),
                match.rulesBombDefuseResume() && state.actionProgress() > 0);
        match.recordEvent(player.getGameProfile().getName() + " 正在拆除 C4。");
        match.publishHudEvent(player, MatchHudEventType.BOMB_DEFUSING,
                "保持拆除动作直到进度完成", match.rulesBombDefuseTicks());
        player.playNotifySound(SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 0.5F, 1.5F);
        return true;
    }

    public void cancelAction(ServerPlayer player) {
        if (player == null || !player.getUUID().equals(state.operatorId())) {
            return;
        }
        state.cancelAction(state.phase() == ClassicBombState.Phase.DEFUSING
                && match.rulesBombDefuseResume());
        match.recordEvent(player.getGameProfile().getName() + " 的 C4 动作已中断。");
        match.publishHudEvent(player, MatchHudEventType.BOMB_ACTION_INTERRUPTED,
                "安装或拆除没有完成", 60);
    }

    public void interruptIfOperator(ServerPlayer player) {
        if (player != null && player.getUUID().equals(state.operatorId())) {
            state.cancelAction(state.phase() == ClassicBombState.Phase.DEFUSING
                    && match.rulesBombDefuseResume());
            match.recordEvent(player.getGameProfile().getName() + " 的 C4 动作因状态变化中断。");
            match.publishHudEvent(player, MatchHudEventType.BOMB_ACTION_INTERRUPTED,
                    "移动或状态变化导致动作中断", 60);
        }
    }

    public void onPlayerDeath(ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (player.getUUID().equals(state.carrierId())) {
            state.dropAt(player.getUUID(), player.getX(), player.getY(), player.getZ(), server.getTickCount());
        } else if (player.getUUID().equals(state.operatorId())) {
            state.cancelAction(state.phase() == ClassicBombState.Phase.DEFUSING
                    && match.rulesBombDefuseResume());
        }
    }

    public void onPlayerLogout(ServerPlayer player) {
        if (player == null) {
            return;
        }
        interruptIfOperator(player);
        if (player.getUUID().equals(state.carrierId())) {
            ItemStack stack = takeRoundItem(player, "c4");
            if (!stack.isEmpty()) {
                droppedC4 = player.drop(stack, false);
            }
            state.dropAt(player.getUUID(), player.getX(), player.getY(), player.getZ(), server.getTickCount());
        }
    }

    public void onItemPickup(ServerPlayer player, ItemStack stack) {
        if (player != null && isRoundItem(stack, "c4") && state.phase() == ClassicBombState.Phase.DROPPED) {
            state.pickup(player.getUUID());
            droppedC4 = null;
        }
    }

    public void onItemToss(ServerPlayer player, ItemEntity entity) {
        if (player == null || entity == null || !isRoundItem(entity.getItem(), "c4")) {
            return;
        }
        droppedC4 = entity;
        state.dropAt(player.getUUID(), entity.getX(), entity.getY(), entity.getZ(), server.getTickCount());
    }

    public void cleanupRound() {
        cleanupRoundItems();
        activeSites = List.of();
        state.reset();
        sync();
    }

    private void sync() {
        BombSyncPacket packet = syncPacket();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            FpsTdmNetwork.sendToPlayer(packet, player);
        }
    }

    public BombSyncPacket syncPacket() {
        boolean active = state.phase() != ClassicBombState.Phase.INACTIVE;
        return new BombSyncPacket(
                active,
                state.phase(),
                match.attackingTeam(),
                match.defendingTeam(),
                state.carrierId(),
                playerName(state.carrierId()),
                state.operatorId(),
                playerName(state.operatorId()),
                state.bombSiteId(),
                state.bombSiteName(),
                state.actionProgress(),
                state.actionRemainingTicks(match.rulesBombPlantTicks(), match.rulesBombDefuseTicks()),
                state.detonationRemainingTicks(server.getTickCount()),
                state.x(),
                state.y(),
                state.z());
    }

    private void finishAction(long now) {
        ServerPlayer operator = player(state.operatorId());
        if (operator == null) {
            state.cancelAction(state.phase() == ClassicBombState.Phase.DEFUSING
                    && match.rulesBombDefuseResume());
            return;
        }
        if (state.phase() == ClassicBombState.Phase.PLANTING) {
            finishPlanting(operator, now);
        } else if (state.phase() == ClassicBombState.Phase.DEFUSING) {
            finishDefusing(operator);
        }
    }

    private void finishPlanting(ServerPlayer player, long now) {
        MapRegion site = activeSiteAt(player);
        if (site == null || !removeRoundItem(player, "c4")) {
            state.cancelAction();
            match.recordEvent(player.getGameProfile().getName()
                    + " 安装 C4 失败：位置或道具状态已变化。");
            return;
        }
        state.finishPlanting(now, site.id(), site.displayName(), match.rulesBombDetonationTicks());
        plantedC4 = createDisplayC4(player.serverLevel(), player.blockPosition());
        player.playNotifySound(SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
        match.recordEvent("C4 已安装在 " + site.displayName() + "，"
                + (match.rulesBombDetonationTicks() / 20) + " 秒后引爆！");
        sync();
    }

    private void finishDefusing(ServerPlayer player) {
        state.finishDefusing(match.rulesBombDefuseTicks());
        discardPlantedC4();
        player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0F, 1.2F);
        match.recordEvent(player.getGameProfile().getName() + " 成功拆除了 C4！");
        sync();
        match.finishRound(match.defendingTeam(), false);
    }

    private void explode(long now) {
        ServerLevel level = server.getPlayerList().getPlayer(state.planterId()) != null
                ? server.getPlayerList().getPlayer(state.planterId()).serverLevel()
                : server.overworld();
        state.explode(now);
        discardPlantedC4();
        match.finishRound(match.attackingTeam(), false);
        level.explode(null, state.x(), state.y(), state.z(), 6.0F,
                Level.ExplosionInteraction.NONE);
        match.recordEvent("C4 已引爆！");
        sync();
    }

    private void validateCarrier(long now) {
        if (state.phase() != ClassicBombState.Phase.CARRIED) {
            return;
        }
        ServerPlayer carrier = player(state.carrierId());
        if (carrier == null || !carrier.isAlive() || match.teamManager().isPending(carrier)) {
            state.dropAt(state.carrierId(), state.x(), state.y(), state.z(), now);
        }
    }

    private void validateAction(long now) {
        if (!state.isActionActive()) {
            return;
        }
        ServerPlayer operator = player(state.operatorId());
        boolean planting = state.phase() == ClassicBombState.Phase.PLANTING;
        boolean valid = operator != null && operator.isAlive()
                && !match.teamManager().isPending(operator)
                && holdsRoundItem(operator, planting ? "c4" : "defuse_kit")
                && match.teamManager().getTeam(operator) == (planting ? match.attackingTeam() : match.defendingTeam());
        if (valid && planting) {
            valid = activeSiteAt(operator) != null;
        }
        if (valid && !planting) {
            valid = distanceSquared(operator, state.x(), state.y(), state.z()) <= DEFUSE_DISTANCE_SQUARED;
        }
        if (!valid) {
            state.cancelAction(!planting && match.rulesBombDefuseResume());
        }
    }

    private void trackDroppedC4(long now) {
        if (state.phase() != ClassicBombState.Phase.DROPPED) {
            return;
        }
        if (droppedC4 == null || !droppedC4.isAlive()) {
            ServerLevel level = currentLevel();
            if (level == null) {
                return;
            }
            if (droppedC4 == null && now - state.droppedTick() <= 20L) {
                droppedC4 = findDroppedC4(level);
                if (droppedC4 != null) {
                    return;
                }
            }
            if (now - state.droppedTick() > 20L) {
                droppedC4 = createDroppedC4(level);
            }
        }
    }

    private ItemEntity findDroppedC4(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ItemEntity item && entity.isAlive()
                    && isRoundItem(item.getItem(), "c4")
                    && distanceSquared(item, state.x(), state.y(), state.z()) <= 16.0D) {
                return item;
            }
        }
        return null;
    }

    private ItemEntity createDroppedC4(ServerLevel level) {
        ItemEntity entity = new ItemEntity(level, state.x(), state.y(), state.z(), createRoundItem("c4"));
        level.addFreshEntity(entity);
        return entity;
    }

    private ItemEntity createDisplayC4(ServerLevel level, BlockPos position) {
        ItemEntity entity = new ItemEntity(level, position.getX() + 0.5D,
                position.getY() + 0.25D, position.getZ() + 0.5D, createRoundItem("c4"));
        entity.setNoGravity(true);
        entity.setInvulnerable(true);
        entity.setPickUpDelay(Integer.MAX_VALUE);
        entity.setUnlimitedLifetime();
        level.addFreshEntity(entity);
        return entity;
    }

    private void discardPlantedC4() {
        if (plantedC4 != null) {
            plantedC4.discard();
            plantedC4 = null;
        }
    }

    private void cleanupRoundItems() {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            removeRoundItems(player);
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ItemEntity item && isRoundItem(item.getItem())) {
                    item.discard();
                }
            }
        }
        droppedC4 = null;
        discardPlantedC4();
    }

    private void removeRoundItems(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isRoundItem(stack)) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    private boolean removeRoundItem(ServerPlayer player, String role) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isRoundItem(stack, role)) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }

    private ItemStack takeRoundItem(ServerPlayer player, String role) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isRoundItem(stack, role)) {
                ItemStack copy = stack.copy();
                player.getInventory().setItem(slot, ItemStack.EMPTY);
                return copy;
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean holdsRoundItem(ServerPlayer player, String role) {
        return isRoundItem(player.getMainHandItem(), role) || isRoundItem(player.getOffhandItem(), role);
    }

    private boolean isRoundItem(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(MATCH_ITEM_TAG)
                && tag.getInt(ROUND_TAG) == match.roundNumber();
    }

    private boolean isRoundItem(ItemStack stack, String role) {
        CompoundTag tag = stack.getTag();
        return isRoundItem(stack) && role.equals(tag.getString(ROLE_TAG));
    }

    private ItemStack createRoundItem(String role) {
        ItemStack stack = new ItemStack(role.equals("c4") ? ModItems.C4.get() : ModItems.JAMMER_TABLET.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(MATCH_ITEM_TAG, true);
        tag.putInt(ROUND_TAG, match.roundNumber());
        tag.putString(ROLE_TAG, role);
        return stack;
    }

    private void giveItem(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private List<ServerPlayer> teamPlayers(Team team) {
        List<ServerPlayer> result = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (match.teamManager().getTeam(player) == team
                    && !match.teamManager().isPending(player) && player.isAlive()) {
                result.add(player);
            }
        }
        return result;
    }

    private List<MapRegion> activeBombSites() {
        return match.maps().currentMap().stream()
                .flatMap(map -> MapRegionActivation.activeRegions(map.regions(), activationContext()).stream())
                .filter(region -> region.type() == MapRegion.Type.BOMB)
                .sorted(Comparator.comparingInt(MapRegion::priority).reversed()
                        .thenComparingLong(ClassicBombManager::regionVolume))
                .toList();
    }

    private MapRegion activeSiteAt(ServerPlayer player) {
        return activeSites.stream()
                .filter(region -> region.region().contains(player.getX(), player.getY(), player.getZ()))
                .findFirst()
                .orElse(null);
    }

    private MapRegionActivation.Context activationContext() {
        return new MapRegionActivation.Context(match.isMatchActive(), match.rulesMode(),
                match.roundNumber(), match.activeTeams().size(), match.teamManager().totalParticipants());
    }

    private ServerLevel currentLevel() {
        return match.maps().currentMap()
                .map(MapDefinition::world)
                .map(server::getLevel)
                .orElseGet(server::overworld);
    }

    private ServerPlayer player(UUID playerId) {
        return playerId == null ? null : server.getPlayerList().getPlayer(playerId);
    }

    private String playerName(UUID playerId) {
        ServerPlayer player = player(playerId);
        return player == null ? "" : player.getGameProfile().getName();
    }

    private static long regionVolume(MapRegion region) {
        long volume = 1L;
        for (MapDefinition.Region part : region.region().parts()) {
            volume *= Math.max(1L, (long) part.max().getX() - part.min().getX() + 1L)
                    * Math.max(1L, (long) part.max().getY() - part.min().getY() + 1L)
                    * Math.max(1L, (long) part.max().getZ() - part.min().getZ() + 1L);
        }
        return volume;
    }

    private static double distanceSquared(Entity entity, double x, double y, double z) {
        double deltaX = entity.getX() - x;
        double deltaY = entity.getY() - y;
        double deltaZ = entity.getZ() - z;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }
}
