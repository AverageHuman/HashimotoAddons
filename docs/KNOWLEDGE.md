# Project Knowledge

This file contains reusable implementation knowledge, not session history. Update entries when later evidence changes them.

## Item Lock

- Locked slot identity uses the player-inventory-relative slot index rather than a container-local slot ID.
- Handled-screen input interception through mixins is more reliable than reflection or tick polling for preventing item interaction.

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
- This area has historically contained mojibake-sensitive Japanese literals; inspect encoding carefully before broad edits.

## Build And Reporting

- Safe and Full artifacts are grouped by version under `build/libs/<version>/`.
- A successful local build does not prove that its source state was committed.
