# 0009: Verse Detector Uses Ground Pickup Events And Full Trash Automation

Status: Accepted

## Context

Verse items are beacons whose display names contain one of eight size variants. Large variants need a visible Title, extra-large variants need the existing item protection behavior, and small/medium variants are disposable in Full.

## Decision

- Detect only `ItemPickupAnimationS2CPacket` events collected by the local player, so container transfers and unrelated inventory changes are excluded.
- Keep Verse Detector in both Safe and Full, enabled by default as an explicitly approved exception to the normal new-feature default.
- Keep `Auto Throw Trash Verse` Full-only and enabled by default as explicitly approved destructive automation.
- Resolve the inventory slot whose count increased after pickup. If it merges with an existing trash stack, throw that entire destination stack while leaving other matching slots untouched.
- Preserve `/protectitem` precedence. Protected trash stacks are never auto-thrown, and manually removing protection remains effective until the item is picked up again.

## Consequences

The feature reuses the existing pickup packet mixin and does not poll containers for new items. Full maintains short-lived pickup and throw queues that are cleared on disable, world change, and disconnect. Safe persists only the shared detector setting and has no trash-throw UI or runtime path.
