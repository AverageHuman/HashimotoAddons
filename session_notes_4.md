# HashimotoAddons Session 4 Handoff

## Project

- Project root: `C:\Users\sasaki\Documents\Codex\2026-05-15\minecraft-java-edition-1-16-5`
- Minecraft target: Java Edition 1.16.5, Fabric client mod.
- Current version: `1.0.3`
- Communicate with the user in Japanese.
- The user wants a Safe build for sharing with others, and a full build with dangerous features for personal use.

## One-Click Files

- Handoff file: [session_notes_4.md](C:/Users/sasaki/Documents/Codex/2026-05-15/minecraft-java-edition-1-16-5/session_notes_4.md)
- Safe build jar: [ha-fabric-safe-1.0.3.jar](C:/Users/sasaki/Documents/Codex/2026-05-15/minecraft-java-edition-1-16-5/build/libs/1.0.3/ha-fabric-safe-1.0.3.jar)
- Full build jar: [ha-fabric-1.0.3.jar](C:/Users/sasaki/Documents/Codex/2026-05-15/minecraft-java-edition-1-16-5/build/libs/1.0.3/ha-fabric-1.0.3.jar)
- Build output folder: [build/libs/1.0.3](C:/Users/sasaki/Documents/Codex/2026-05-15/minecraft-java-edition-1-16-5/build/libs/1.0.3)

Important: when reporting build results to the user, include the clickable jar links above so they can reach the generated files in one click.

## Build Commands

- Safe build: `.\gradle-9.4.1\bin\gradle.bat build -PhaVariant=safe --offline`
- Full build: `.\gradle-9.4.1\bin\gradle.bat build -PhaVariant=full --offline`
- Output jars are grouped by version under `build\libs\1.0.3\`.
- Latest verified builds:
  - Safe: success on 2026-05-17.
  - Full: success on 2026-05-17.

## Safe / Full Policy

- Safe build must not expose dangerous features at all.
- Safe build must not show dangerous feature buttons.
- Safe build must not expose dangerous feature config entries.
- Full build includes dangerous features.
- Chest Search is considered safe by the user.
- Drop Tracker is considered safe by the user.

## Current Feature Order

Main config menu order:

1. Dangerous Features, full build only
2. Item Lock
3. HP Alert
4. Mana Alert
5. Camera
6. Soulbind Protection
7. Chest Search
8. Drop Tracker

In the Safe build, Dangerous Features is removed, so the user-facing order starts at Item Lock.

## Normal / Safe Features

- Item Lock
  - Prevents moving locked items by click, shift-click, drop, pickup-all, and number-key swap.
- HP Alert
  - Configurable alert based on HP percentage.
- Mana Alert
  - Reads MANA from scoreboard/sidebar patterns.
- Camera
  - Toggles between first person and third-person back view.
- Soulbind Protection
  - Feature name decided in English as `Soulbind Protection`.
  - Tracks:
    - `[ヴェルサリオンの呪縛] 戦闘が開始し、魂が縛られました。ログアウトないしは切断すると死亡します。`
    - `[ヴェルサリオンの呪縛] 呪縛が解けました`
  - While active, pressing Disconnect from the game menu should show a confirmation screen before actually disconnecting.
- Chest Search
  - Records opened chest/barrel contents.
  - Stores index in `config/HashimotoAddons/chest_search.json`.
  - Search query highlights likely matching containers with an overlay.
  - Clear Index has a confirmation screen.
  - Confirmation buttons use `§cClear Index` and `§aCancel`.
- Drop Tracker
  - Shows picked-up items on HUD with item icon, colored display name, count, and estimated profit.
  - HUD position is draggable like Chest Count.
  - HUD remains visible in chat/inventory/chest screens unless a Hashimoto adjustment/config screen is open.
  - Counts are persisted to `config/HashimotoAddons/drop_tracker.json`.
  - Reset Counts clears the in-memory counts and the JSON file.

## Dangerous Features

- Macro Toggle
- Default Weapon Position
- Auto Heal
- Item Macro
- Chunk Containers

Keep these inaccessible from Safe builds, including UI buttons and config exposure.

## Latest Session 4 Changes

- Added Drop Tracker JSON persistence in `src/main/java/com/example/ha/HaDropTracker.java`.
- Drop Tracker now loads from `config/HashimotoAddons/drop_tracker.json` on first use.
- Drop Tracker saves after every count update.
- Drop Tracker saves an empty list when Reset Counts is used.
- Drop Tracker saves item id, display name text JSON, fallback display name, plain name, and count.
- Fixed previous mojibake in currency-name matching by using Unicode escapes:
  - `銀貨`: 1 Intercoin
  - `金貨`: 100 Intercoins
  - `銀塊`: 10000 Intercoins
  - `とこしえの金塊`: 100000 Intercoins

## Important Files

- `src/main/java/com/example/ha/HaDropTracker.java`
- `src/main/java/com/example/ha/HaDropTrackerOverlay.java`
- `src/main/java/com/example/ha/HaDropTrackerScreen.java`
- `src/main/java/com/example/ha/HaDropTrackerOverlayScreen.java`
- `src/main/java/com/example/ha/HaChestSearchIndex.java`
- `src/main/java/com/example/ha/HaConfigScreen.java`
- `src/main/java/com/example/ha/HaConfig.java`
- `src/main/java/com/example/ha/HaHudVisibility.java`
- `src/main/java/com/example/ha/mixin/ClientPlayNetworkHandlerMixin.java`
- `build.gradle`
- `gradle.properties`

## Notes For Next Session

- If the user says "Safeだけ", build only with `-PhaVariant=safe`.
- If the user says "dangerous入り", "not safe", or "full", build with `-PhaVariant=full`.
- If both are requested, run both build commands.
- After every build, point the user to the clickable jar links in this file or include fresh clickable links directly in the response.
- Be careful not to reintroduce dangerous feature access into the Safe build.
- Avoid editing unrelated files or reverting existing changes unless the user explicitly asks.
