# Project Knowledge

This file contains reusable implementation knowledge, not session history. Update entries when later evidence changes them.

## Item Lock

- Locked slot identity uses the player-inventory-relative slot index rather than a container-local slot ID.
- Handled-screen input interception through mixins is more reliable than reflection or tick polling for preventing item interaction.

## Item Protection

- Item protection now accepts both legacy hidden per-stack IDs and content-based keys derived from item ID, damage, and visible NBT.
- Hidden IDs remain the most precise identity mechanism when an item stack keeps them, but content keys let tag-light items such as armor or snowballs participate in protection.
- Protection and slot-lock semantics should stay separate so item-instance protection can coexist with slot-based locking.

## Safe And Full

- Safe must hide dangerous feature UI, config, commands, behavior, and keybindings rather than merely showing them disabled.
- Chest Search and Drop Tracker have previously been classified as Safe features by the owner.

## HUD

- Existing slim/full HUD and positioning patterns can be found in tracker and Spotify overlay/screen classes.
- Long Spotify text uses bounded-width scrolling with pauses at both ends.

## Evolution Forge

- Item keys should omit enhancement suffixes from `(+1)` through `(+12)`.
- Subweapons and soul protectors use a different enhancement reverse-calculation rule from ordinary equipment.
- HP booster contribution must be removed before learning or displaying base maximum-HP ranges.
- Evolution Forge stat caches should be keyed by item name, item rank, and sub-accessory flag; legacy caches without rank metadata are treated as contaminated and dropped.
- Sub-accessory enhancement reverse-calculation uses a flat `1.5%` per-level multiplier for most stats and a `15%` per-level additive rule for `対MOBダメージ`.
- This area has historically contained mojibake-sensitive Japanese literals; inspect encoding carefully before broad edits.

- Avoid Evolution Forge schema migrations unless they are explicitly necessary; if one is proposed, the user-facing summary must warn that saved data may be deleted or rewritten incorrectly.

## Build And Reporting

- Safe and Full artifacts are grouped by version under `build/libs/<version>/`.
- A successful local build does not prove that its source state was committed.
