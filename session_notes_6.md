# HashimotoAddons Session 6 Handoff

## Project

- Project root: `C:\Users\sasaki\Documents\Codex\2026-05-15\minecraft-java-edition-1-16-5`
- Minecraft target: Java Edition 1.16.5, Fabric client mod.
- Current version: `1.0.4`
- Current branch: `ghost-wall-prototype`
- Latest commit at handoff: `b1d7d42 Remove exp tracker menu total`
- Communicate with the user in Japanese.

## One-Click Files

- Handoff file: [session_notes_6.md](C:/Users/sasaki/Documents/Codex/2026-05-15/minecraft-java-edition-1-16-5/session_notes_6.md)
- Previous handoff: [session_notes_5.md](C:/Users/sasaki/Documents/Codex/2026-05-15/minecraft-java-edition-1-16-5/session_notes_5.md)
- Safe build jar: [ha-fabric-safe-1.0.4.jar](C:/Users/sasaki/Documents/Codex/2026-05-15/minecraft-java-edition-1-16-5/build/libs/1.0.4/ha-fabric-safe-1.0.4.jar)
- Full build jar: [ha-fabric-1.0.4.jar](C:/Users/sasaki/Documents/Codex/2026-05-15/minecraft-java-edition-1-16-5/build/libs/1.0.4/ha-fabric-1.0.4.jar)
- Build output folder: [build/libs/1.0.4](C:/Users/sasaki/Documents/Codex/2026-05-15/minecraft-java-edition-1-16-5/build/libs/1.0.4)

Important: when reporting build results to the user, include clickable jar links.

## Build Commands

- Safe build: `.\gradle-9.4.1\bin\gradle.bat build -PhaVariant=safe --offline`
- Full build: `.\gradle-9.4.1\bin\gradle.bat build -PhaVariant=full --offline`
- Output jars are grouped by version under `build\libs\1.0.4\`.
- Both Safe and Full builds succeeded after the latest Exp Tracker menu change.

## Safe / Full Policy

- Safe build must not expose Dangerous Features at all.
- Safe build must not show Dangerous Feature buttons.
- Safe build must not register or expose Dangerous Feature keybinds.
- Full build includes Dangerous Features.
- Normal features are available in both Safe and Full.
- Do not weaken the Safe/Full separation when adding future features.

## Current Main Menu Order

1. Dangerous Features, full build only
2. Item Lock
3. HP Alert
4. Mana Alert
5. Camera
6. Soulbind Protection
7. Chest Search
8. Drop Tracker
9. Exp Tracker
10. Chat Filter
11. Extras

In the Safe build, `Dangerous Features` is omitted.

## Normal Features

- Item Lock
- HP Alert
- Mana Alert
- Camera
- Soulbind Protection
- Chest Search
- Drop Tracker
- Exp Tracker
- Chat Filter
- Extras / Ghost Blocks

## Dangerous Features

- Macro Toggle
- Default Weapon Position
- Auto Heal
- Item Macro
- Chunk Containers
- Macro Status HUD / movement controls are Full-only with the macro feature.

Keep these inaccessible from Safe builds, including buttons, config exposure, behavior, and keybinds.

## Git Status / Workflow

- Git management was initialized during this work.
- There is a baseline tag: `v1.0.4` on commit `9944034 Baseline HashimotoAddons 1.0.4`.
- Recent commits:
  - `b1d7d42 Remove exp tracker menu total`
  - `d4eeb08 Add compact exp tracker display`
  - `0595966 Polish drop tracker layout`
  - `9d00cab Add drop tracker rates`
  - `e6e9994 Preserve exp tracker timer`
  - `01f79ae Show stopped exp tracker hud`
  - `3cc2c93 Gate exp tracker on soulbind`
  - `6d504a3 Support EXP name tag format`
  - `563ea9f Add exp tracker`
  - `1bdc867 Fix block gallery command and safe keybindings`
  - `166af8b Resolve ghost block neighbor states`
  - `9bd766b Debounce ghost block use actions`
- `.git/index.lock` writes may require escalated tool permission. If `git add` fails with permission denied, rerun `git add` with escalation.
- Do not revert unrelated changes unless explicitly requested.

## Chat Filter

- Normal feature in Safe and Full.
- Main menu button is `Chat Filter`.
- Chat Filter screen has a clear enable/disable button and an `Edit Filter` flow.
- `Add New Filter` and existing filters are placed inside the edit/list UI.
- Filters are partial-match only.
- Blank or whitespace-only filters should not be retained.
- Soulbind Protection must process chat first, then Chat Filter may hide the visible message.
- Matching hidden messages only disappear from chat display; server communication and internal feature detection should not be stopped.

Important files:

- `src/main/java/com/example/ha/HaChatFilter.java`
- `src/main/java/com/example/ha/HaChatFilterScreen.java`
- `src/main/java/com/example/ha/HaChatFilterListScreen.java`
- `src/main/java/com/example/ha/HaChatFilterEditScreen.java`
- `src/main/java/com/example/ha/mixin/ClientPlayNetworkHandlerMixin.java`

## Macro Status HUD

- Full-only macro-related feature.
- Macro status display is intentionally plain and rugged, not decorative.
- Text should be close to `Macro: Enable` / `Macro: Disable`.
- Enable/disable colors should follow the existing green/red convention used elsewhere.
- HUD is movable.
- Safe build must not expose macro keybinds or macro controls.

## Chest Search

- Chest Search remains a normal Safe/Full feature.
- It has a shortcut key to jump directly into the Chest Search menu.
- Shortcut key registration follows the macro toggle style, but it is safe and available in Safe.
- `Clear Index` must keep its confirmation screen.
- Chest/barrel index persistence remains in `config/HashimotoAddons/chest_search.json`.

Important files:

- `src/main/java/com/example/ha/HaChestSearchScreen.java`
- `src/main/java/com/example/ha/HaChestSearchIndex.java`
- `src/main/java/com/example/ha/HaChestSearchOverlay.java`
- `src/main/java/com/example/ha/HaKeyBindings.java`

## Extras / Ghost Blocks

- `Ghost Wall Edit Mode` was renamed conceptually to `Extras`.
- Commands:
  - `/ha extras`: toggles whether Extras/Ghost Blocks are visible/active.
  - `/ha em`: toggles Edit Mode.
  - `/ha bg`: opens the block gallery GUI.
- Extras screen includes toggles for Extras and Edit Mode.
- Extras has a HUD showing:
  - Edit Mode on/off
  - Current world name
  - Selected block name and icon
- Extras HUD is movable through an Extras menu button.
- Ghost blocks are client-side visual/edit helpers, not server-side block placement.
- Server-side click packets should be suppressed while editing ghost blocks where appropriate.
- Right-click hold is debounced so one click places/changes one block.
- Ghost blocks can be placed in air again, including while looking at existing ghost blocks.
- Existing real blocks can be replaced visually:
  - Shift + left click on an existing block replaces it client-side with a barrier placeholder.
  - Chests, signs, and barrels are excluded from replacement.
  - Right click can change that ghost/replacement to the selected block.
- Fence/connectable blocks should resolve neighbor state so fences connect instead of rendering as isolated posts.
- There is an option to clear all ghost blocks.
- Block selection GUI is not button-list based; it renders block/item icons in a gallery.
- Gallery includes search and favorite blocks.
- Right-clicking a block in the gallery toggles Favorite status.

Important files:

- `src/main/java/com/example/ha/HaGhostWall.java`
- `src/main/java/com/example/ha/HaGhostWallEdit.java`
- `src/main/java/com/example/ha/HaExtrasScreen.java`
- `src/main/java/com/example/ha/HaGhostBlockGalleryScreen.java`
- `src/main/java/com/example/ha/HaExtrasOverlay.java`
- `src/main/java/com/example/ha/HaExtrasOverlayScreen.java`
- `src/main/java/com/example/ha/mixin/MinecraftClientMixin.java`

## Exp Tracker

- Normal feature in Safe and Full.
- Main menu placement: after Drop Tracker, before Chat Filter.
- Tracks XP only while Soulbind is active.
- Soulbind state is shared from Soulbind Protection via `HaSoulbindProtection.isSoulbound()`.
- When not soulbound, tracking stops but the HUD remains visible if Exp Tracker is enabled.
- Stopped state should be clear with `Status: Stopped`; active state uses `Status: Tracking`.
- Timer is cumulative and only advances while actively tracking.
- Timer must not reset just because Soulbind stops.
- `Reset Total` resets total XP and timer.
- Format currently supported: `+[digits] EXP!`
- Older support for XP-style formats may remain, but the user corrected the real format to `+[数字] EXP!`.
- Exp is detected from nearby entities' custom/display/name text within 20 blocks.
- Each entity should be counted once, with cache protection against duplicate ticks.
- HUD can show:
  - `Exp Tracker`
  - `Status: Tracking` or `Status: Stopped`
  - `Total XP: ...`
  - optional `Timer: ...`
  - optional `EXP/hour: ...`
- `Compact XP: ON/OFF` was added.
  - OFF: `12,345`
  - ON: `12.3k`, `4.5m`, `1.2b`
- The Exp Tracker menu GUI no longer shows a separate `Total XP:` line at the bottom.
- HUD still shows `Total XP`.

Important files:

- `src/main/java/com/example/ha/HaExpTracker.java`
- `src/main/java/com/example/ha/HaExpTrackerScreen.java`
- `src/main/java/com/example/ha/HaExpTrackerOverlay.java`
- `src/main/java/com/example/ha/HaExpTrackerOverlayScreen.java`
- `src/main/java/com/example/ha/HaTickHandler.java`
- `src/main/java/com/example/ha/HaHudRenderer.java`
- `src/main/java/com/example/ha/HaConfig.java`

## Drop Tracker

- Normal feature in Safe and Full.
- Tracks only while Soulbind is active.
- When not soulbound, tracking stops but the HUD remains visible if Drop Tracker is enabled.
- Timer is cumulative and only advances while actively tracking.
- `Reset Counts` resets counts, estimated profit, and timer-related data as implemented.
- Adds estimated profit and Profit/hour.
- Profit/hour is derived from timer and estimated profit.
- `Compact Profit: ON/OFF` supports compact notation:
  - Example: `Est.Profit: 43.5m Intercoins.`
- HUD layout was polished so item rows do not overlap estimated profit/timer/profit-hour footer.
- Footer is separated under the item list.
- Buttons in the Drop Tracker screen are evenly spaced.
- Tracking Mode description moved to hover tooltip to avoid overlapping buttons.
- Existing tracking modes:
  - `Track Everything`
  - `Built-in Currencies Only`
  - `Built-in + Registered Items`
- Registered items can be edited in the Drop Tracker UI.

Important files:

- `src/main/java/com/example/ha/HaDropTracker.java`
- `src/main/java/com/example/ha/HaDropTrackerScreen.java`
- `src/main/java/com/example/ha/HaDropTrackerOverlay.java`
- `src/main/java/com/example/ha/HaDropTrackerOverlayScreen.java`
- `src/main/java/com/example/ha/HaDropTrackerRegisteredListScreen.java`
- `src/main/java/com/example/ha/HaDropTrackerRegisteredEditScreen.java`

## Soulbind Protection

- Soulbind Protection still owns the chat-based Soulbind detection.
- Exp Tracker and Drop Tracker now depend on Soulbind active state.
- Chat Filter must never block Soulbind detection before it runs.
- Disconnect confirmation while Soulbind warning state is active remains required.

Important file:

- `src/main/java/com/example/ha/HaSoulbindProtection.java`

## Known Artifacts / Notes

- There was a stray `Desktop` directory under `build/libs/1.0.4` in one earlier session. It was not intentionally created by Codex and was not source-controlled because `build/` is ignored. Do not delete it unless the user explicitly asks.
- Gradle emits Java 8 source/target deprecation warnings. Builds still succeed.
- Runtime Minecraft verification was mostly user-driven; build verification is available from Gradle.

## Notes For Next Session

- If the user says `safe only`, build only with `-PhaVariant=safe`.
- If the user says `full`, `dangerous`, or `not safe`, build with `-PhaVariant=full`.
- If both are requested, run both build commands.
- After every jar-producing build, include clickable jar links.
- Keep Safe/Full separation as the highest priority constraint.
- Prefer small commits after each coherent feature/fix.
- Use `rg` for search and `apply_patch` for manual edits.
- Avoid deleting build artifacts or user files unless explicitly requested.
