# HashimotoAddons Session 5 Handoff

## Project

- Project root: `C:\Users\sasaki\Documents\Codex\2026-05-15\minecraft-java-edition-1-16-5`
- Minecraft target: Java Edition 1.16.5, Fabric client mod.
- Current version: `1.0.3`
- Communicate with the user in Japanese.
- There are two variants:
  - `safe`: shareable build with dangerous features fully hidden.
  - `full`: personal build with dangerous features enabled.

## One-Click Files

- Handoff file: [session_notes_5.md](C:/Users/sasaki/Documents/Codex/2026-05-15/minecraft-java-edition-1-16-5/session_notes_5.md)
- Previous handoff: [session_notes_4.md](C:/Users/sasaki/Documents/Codex/2026-05-15/minecraft-java-edition-1-16-5/session_notes_4.md)
- Safe build jar: [ha-fabric-safe-1.0.3.jar](C:/Users/sasaki/Documents/Codex/2026-05-15/minecraft-java-edition-1-16-5/build/libs/1.0.3/ha-fabric-safe-1.0.3.jar)
- Full build jar: [ha-fabric-1.0.3.jar](C:/Users/sasaki/Documents/Codex/2026-05-15/minecraft-java-edition-1-16-5/build/libs/1.0.3/ha-fabric-1.0.3.jar)
- Build output folder: [build/libs/1.0.3](C:/Users/sasaki/Documents/Codex/2026-05-15/minecraft-java-edition-1-16-5/build/libs/1.0.3)

Important: when reporting build results to the user, include clickable jar links.

## Build Commands

- Safe build: `.\gradle-9.4.1\bin\gradle.bat build -PhaVariant=safe --offline`
- Full build: `.\gradle-9.4.1\bin\gradle.bat build -PhaVariant=full --offline`
- Output jars are grouped by version under `build\libs\1.0.3\`.

## Safe / Full Policy

- Safe build must not expose dangerous features at all.
- Safe build must not show dangerous feature buttons.
- Safe build must not expose dangerous feature config entries.
- Full build includes dangerous features.
- `Chest Search` is safe.
- `Drop Tracker` is safe.

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

In the Safe build, `Dangerous Features` is omitted.

## Safe Features

- Item Lock
  - Prevents moving locked items by click, shift-click, drop, pickup-all, and number-key swap.
- HP Alert
  - Configurable low-HP alert based on percentage.
- Mana Alert
  - Reads MANA from scoreboard/sidebar patterns.
- Camera
  - Toggles between first person and third-person back view.
- Soulbind Protection
  - Shows a confirmation before disconnecting while soulbind warning state is active.
- Chest Search
  - Records opened chest/barrel contents to `config/HashimotoAddons/chest_search.json`.
  - Highlights matching containers in the world.
  - Highlights matching item slots inside opened chest/barrel screens.
  - `Clear Index` has a confirmation screen.
- Drop Tracker
  - Tracks picked-up items and shows HUD with icon, colored name, count, and estimated profit.
  - HUD is draggable.
  - Counts are persisted to `config/HashimotoAddons/drop_tracker.json`.
  - `Reset Counts` clears both in-memory data and JSON data.

## Dangerous Features

- Macro Toggle
- Default Weapon Position
- Auto Heal
- Item Macro
- Chunk Containers

Keep these inaccessible from Safe builds, including buttons and config exposure.

## Session 5 Changes

### Drop Tracker

- Added multiple tracking modes in `src/main/java/com/example/ha/HaDropTracker.java`:
  - `Track Everything`
  - `Built-in Currencies Only`
  - `Built-in + Registered Items`
- Added manual item registration command:
  - `/ha tracker add`
  - `/ha tracker add <price>`
- The held item can be registered with an optional unit price.
- Added registered-item editing UI:
  - `src/main/java/com/example/ha/HaDropTrackerRegisteredListScreen.java`
  - `src/main/java/com/example/ha/HaDropTrackerRegisteredEditScreen.java`
- Added an `Edit Registered Items` button to the Drop Tracker screen.
- `registered_only` behavior was adjusted so built-in currencies are still tracked.
- If a scoreboard line containing `現在地` starts with dark green `§2` or `Formatting.DARK_GREEN`, Drop Tracker ignores item pickups in that location.

### Chest Search Rendering

- Reworked world overlay rendering in `src/main/java/com/example/ha/HaChestSearchOverlay.java`.
- The current rendering approach is:
  - world hook: `WorldRenderEvents.BEFORE_DEBUG_RENDER`
  - semi-transparent yellow-green fill via direct `Tessellator` / `BufferBuilder`
  - yellow-green outline via `RenderLayer.getLines()`
- This was iterated specifically to satisfy all of the following:
  - renders without Iris
  - renders with Iris
  - stays attached to the target block
  - does not break the vanilla black block-outline selection box
- The user confirmed that the box now renders with and without Iris.

### Chest Search Slot Highlight

- Added chest GUI item highlighting for matches in:
  - `src/main/java/com/example/ha/HaChestSearchSlotHighlight.java`
  - `src/main/java/com/example/ha/mixin/HandledScreenRenderMixin.java`
- Matching logic checks both display name and item id.

### Chest Search Wrong-Chest Overwrite Mitigation

- The index previously could sometimes write chest A contents into chest B after the player moved their view.
- Current mitigation in `src/main/java/com/example/ha/HaChestSearchIndex.java`:
  - stores `pendingTargetPos`
  - latches container position per `syncId`
  - avoids switching target while the same container screen remains open
- Additional capture was added in `src/main/java/com/example/ha/mixin/ClientPlayNetworkHandlerMixin.java`:
  - on `onOpenScreen`, call `HaChestSearchIndex.get().onContainerScreenOpen(MinecraftClient.getInstance())`
- This is meant to reduce timing races by capturing the looked-at container when the screen-open packet arrives.

## Important Current Status

- The world overlay rendering issue appears resolved.
- The Iris-related non-rendering issue appears resolved.
- The vanilla black outline displacement issue appears resolved.
- The wrong-chest overwrite issue has been mitigated, but the latest `onOpenScreen`-based fix still needs real user verification.

Important: if the user reports that chest overwrite still happens sometimes, the next likely fix is to capture the interacted chest position at right-click time instead of relying on `crosshairTarget` during `tick` or `onOpenScreen`.

Likely next technical direction:

1. Add a client interaction hook around chest/barrel right-click.
2. Store the exact interacted `BlockPos` immediately.
3. Consume that stored position when the container screen opens.
4. Stop relying on a potentially changed `crosshairTarget` for chest ownership.

## Important Files

- `src/main/java/com/example/ha/HaChestSearchIndex.java`
- `src/main/java/com/example/ha/HaChestSearchOverlay.java`
- `src/main/java/com/example/ha/HaChestSearchSlotHighlight.java`
- `src/main/java/com/example/ha/HaDropTracker.java`
- `src/main/java/com/example/ha/HaDropTrackerScreen.java`
- `src/main/java/com/example/ha/HaDropTrackerRegisteredListScreen.java`
- `src/main/java/com/example/ha/HaDropTrackerRegisteredEditScreen.java`
- `src/main/java/com/example/ha/HaConfig.java`
- `src/main/java/com/example/ha/HaConfigScreen.java`
- `src/main/java/com/example/ha/mixin/ClientPlayNetworkHandlerMixin.java`
- `src/main/java/com/example/ha/mixin/HandledScreenRenderMixin.java`
- `build.gradle`
- `gradle.properties`

## Notes For Next Session

- If the user says `safe only`, build only with `-PhaVariant=safe`.
- If the user says `full`, `dangerous`, or `not safe`, build with `-PhaVariant=full`.
- If both are requested, run both build commands.
- After every build, include clickable jar links in the response.
- Do not reintroduce dangerous features into the Safe build.
- Avoid reverting unrelated changes unless the user explicitly asks.
- If the user wants another version bump, update `gradle.properties` and rebuild so the output folder and jar names match the new version.
