# 0004: TriggerBot Is Full-only And Shares Mob HP Targeting

Status: Accepted

## Context

HashimotoAddons already exposes a Mob HP display that resolves a nearby LivingEntity from the crosshair or a fallback ray. The new TriggerBot feature reacts to that same live entity state and can execute registered macros repeatedly while the target stays above a fixed HP threshold.

## Decision

- Classify TriggerBot as Full-only because it inspects live combat state and automates repeated macro execution.
- Reuse the Mob HP Display target-resolution path for TriggerBot so both features choose the same target.
- Keep Item Macro's new Macro Toggle sync option defaulted on so existing behavior remains unchanged until the owner opts out.

## Consequences

- Safe builds must not expose TriggerBot UI, persistence, or runtime behavior.
- Future target-driven Full features should reuse the shared targeting helper instead of duplicating raycast logic.
- Item Macro can now opt out of the global Macro Toggle without affecting AFK Farming or TriggerBot.
