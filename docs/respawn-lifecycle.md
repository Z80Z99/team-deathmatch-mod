# Match respawn lifecycle

Lethal damage is allowed through to Minecraft. Forge death, drops, statistics,
clone and respawn hooks run through the vanilla pipeline. Match scoring is
deferred until the death call returns, and checks the final cancellation state.

At the server tick, queued deaths request PERFORM_RESPAWN through the vanilla
connection handler. Do not call PlayerList.respawn in isolation: the handler
must replace connection.player with the returned player. Match state is applied
after the handler returns, looking up the current player by UUID.

The reconstructed player waits as an invulnerable spectator. Reconstruction is
not permission to fight: the original deadline survives manual respawn. In
elimination modes the wait lasts until the round ends. When a safe spawn is
unavailable, retry without making the player vulnerable. Only successful safe
placement restores survival, camera, abilities, health and movement.

## Cosmetic extension points

MatchRespawnEvent is a non-cancellable Forge server event:

- WAITING: confirmed match death, with the original respawn deadline. The player
  may still be the dead entity; use UUID for retained state.
- REBUILT: vanilla has finished replacing the connected player. This does not
  mean they can participate yet.
- READY: safe placement and combat-state restoration are complete.

Use getRespawnPhase(), getPlayer() and getDeadline(). Deadlines use server ticks;
Long.MAX_VALUE means elimination until the round ends. Send dedicated cosmetic
packets from listeners for future fades, sounds or particles. Never retain a
ServerPlayer across reconstruction, mutate death flags, or trigger another
respawn from an effect listener. Client effects must clean up on disconnect,
match end and scene changes. Do not repost vanilla death events.

## Multiplayer acceptance checks

Automated tests cover cancellation, duplicate notification, deferred state
application and unsafe-spawn waiting. The following require two real clients:

- Repeated gun kills: both clients see and can damage the respawned player.
- GD656: one kill/death feedback per actual death, including final-round kills.
- Manual respawn and immediate-respawn gamerule do not bypass the match delay.
- Team competition preserves inventory; bomb mode uses actual vanilla drops.
- Elimination death stays spectator until the next round.
- Destroyed spawn terrain keeps the player waiting until a safe point exists.
- Boundary expiry, round transition, disconnect and reconnect do not leave an
  old entity attached to the connection or restore health to a dead entity.

This has not yet been verified in a two-client game with GD656 installed.

## Client presentation (r94)

Protocol 17 adds an explicit awaiting-respawn flag and victim-specific death label
to match snapshots. A zero countdown never authorizes a cosmetic return while
the server still reports waiting for safe terrain. The global kill feed is not
used as the victim's death summary.

RespawnOverlay suppresses the vanilla death screen for active combatants only.
The real death/respawn path is untouched. Pause menus and disconnect remain
accessible. The overlay uses an 180 ms entrance, restrained red edges, deployment
progress or elimination status, and a 700 ms return fade after the server clears
waiting and the local player is alive in a non-spectator mode. It has no camera
shake, shader changes or recorded killcam. Disconnect and leaving combat clear
the effect. Both server and clients must install r94 because the protocol changed.
