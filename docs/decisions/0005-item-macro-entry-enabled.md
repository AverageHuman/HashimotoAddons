# 0005. Item Macro Uses Per-Entry Enabled State

Date: 2026-06-29

## Context

The previous Full-only Item Macro sync toggle was a global setting in Dangerous Features. The requested UI now lives inside the Item Macro editor, and the user wants to enable or disable each macro entry there instead of exposing a separate sync switch. The owner clarified that the relevant companion feature is `Auto Heal`.

## Decision

- Item Macro now uses a per-entry `enabled` state stored on each `SwapEntry`.
- The Item Macro list/editor screens are the only place where this state is edited.
- Item Macro auto-swap runs only when both the global Macro Toggle and Auto Heal are enabled.
- AFK Farming and TriggerBot continue to use the shared macro list independently of the Item Macro enabled state.
- This remains a Full-only behavior surface; Safe must not expose the editor controls or runtime path.

## Consequences

- Older configs continue to load because missing per-entry `enabled` fields default to `true`.
- Dangerous Features no longer needs a separate Item Macro sync control.
- Users can disable Item Macro use for a specific macro without affecting AFK Farming or TriggerBot selection.
