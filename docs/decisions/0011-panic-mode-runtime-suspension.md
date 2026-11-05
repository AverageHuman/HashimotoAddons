# Panic Mode runtime suspension

- Date: 2026-09-15
- Policy: Both. Panic Mode suspends client features; it does not introduce gameplay automation. Extras cleanup remains Full-only.
- Default: disabled, key unbound.

The configured key toggles a session-only suspension. Feature configuration, bindings, registered items and waypoint/ghost-wall data are not overwritten. The same key restores the configured feature set. Both transitions are silent; activation clears visible chat using `ChatHud.clear(false)` (F3+D behavior), clears existing title notifications and stops mod-owned sounds.

Input is sampled at the start of the client tick, before normal gameplay ticks. It triggers with no screen open. Held keys are tracked while configuring or typing so closing a screen cannot activate a captured key. Feature key queues are drained while suspended and on both transitions. The ordinary `/ha` settings entry remains available; other feature commands are suppressed. Disabling Panic Mode in its settings also resumes features.

The global gate covers registered HUD/world render callbacks, tick work, packet observers, item protections, tooltip/slot/name-tag rendering and world interaction mixins. New integration points must honor this gate. Configuration persistence and disconnect cleanup continue during suspension. Suspension survives a disconnect within the current client process; it is not persisted across process restart.

Full activation restores managed ghost blocks to their original states and rebuilds world chunk meshes, including stale adjacent fence geometry. Saved ghost blocks remain available for restoration. Only managed client blocks are restored; server-owned fences are not deleted. Automation releases held input and cancels pending Alchemy/auto-throw operations rather than continuing a stale inventory transaction on resume. AFK configuration is retained and its runtime restarts normally. Tracker elapsed-time anchors are reset on resume so suspended time is not counted.

Validation: Full and Safe builds and tests. Live Minecraft verification is still required for input, chunk mesh refresh, render/mixin behavior, automation interruption, reconnect and silent restoration.
