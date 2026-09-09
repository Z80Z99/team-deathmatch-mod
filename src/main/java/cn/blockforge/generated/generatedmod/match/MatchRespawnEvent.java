package cn.blockforge.generated.generatedmod.match;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Server-thread effect hook, separate from vanilla death/clone/respawn events.
 * WAITING starts the match delay; REBUILT marks the new connected player;
 * READY fires after safe placement and ability restoration.
 * Listeners may send cosmetic packets but must not replace players or trigger respawn.
 * The deadline is a server tick, or Long.MAX_VALUE for round elimination.
 */
public final class MatchRespawnEvent extends Event {
    public enum Phase { WAITING, REBUILT, READY }

    private final ServerPlayer player;
    private final Phase phase;
    private final long deadline;

    public MatchRespawnEvent(ServerPlayer player, Phase phase, long deadline) {
        this.player = player;
        this.phase = phase;
        this.deadline = deadline;
    }

    public ServerPlayer getPlayer() { return player; }
    public Phase getRespawnPhase() { return phase; }
    public long getDeadline() { return deadline; }
}
