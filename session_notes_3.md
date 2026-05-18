# HashimotoAddons Session 3 Handoff

## Project

- Project root: `C:\Users\sasaki\Documents\Codex\2026-05-15\minecraft-java-edition-1-16-5`
- Build command: `.\gradle-9.4.1\bin\gradle.bat build`
- Built jar: `build\libs\ha-fabric-1.0.0.jar`
- Minecraft target: Java Edition 1.16.5, Fabric client mod.

## User Preference / Development Policy

- Communicate in Japanese.
- The user wants practical QOL features for an RPG server.
- Anything that can automate gameplay or inspect server/game state in a way that may be sensitive should be treated carefully and discussed as `Dangerous Features`.
- `Dangerous Features` are controlled by the macro toggle. Normal features should stay available even when macro is disabled.
- Avoid implementing risky server-communication behavior unless the user explicitly accepts it.

## Current Feature Layout

### Normal Features

- `Item Lock`
  - Prevents unintended item movement in handled screens.
- `HP Alert`
  - Configurable alerts based on remaining HP percentage.
  - Alert title text and color are configurable.
- `Mana Alert`
  - Reads MANA from the scoreboard sidebar.
  - Expected scoreboard pattern:
    - A line containing `MANA`
    - The next line containing a fraction such as `103.14/103.14`
  - Current value is the number left of `/`.
  - Percentage is `current / max * 100`.
  - Alert percentage, title text, and color are configurable like HP Alert.
  - If enabled Mana Alert cannot detect mana, it sends a chat message asking the user to report it to the developer.
- `Camera`
  - Uses a configurable key.
  - Toggles only between first person and third-person back view.
  - It does not cycle into third-person front view.
  - The config button has a Japanese hover tooltip explaining the behavior.

### Dangerous Features

- `Macro Toggle`
  - Default key: `H`.
  - Dangerous features are enabled/disabled by this toggle.
  - UI label is `§cDangerous Features`.
- `Default Weapon Position`
  - Button is placed directly under `Change Macro Toggle Key`.
  - When macro is enabled, the mod switches to this hotbar slot.
- `Auto Heal`
  - Uses configured hotbar slot and threshold.
- `Item Macro`
  - Simulates the actual bound hotbar key instead of only sending slot packets.
  - This was done so the client visibly holds the selected hotbar item for the configured hold ticks.
  - It no longer auto-returns to the original/default slot after firing.
  - While the simulated key is held, another macro swap is blocked.
  - Important files:
    - `src/main/java/com/example/ha/HaTickHandler.java`
    - `src/main/java/com/example/ha/mixin/KeyBindingAccessor.java`
    - `src/main/resources/ha.client.mixins.json`
- `Chunk Containers`
  - Counts chests, trapped chests, and barrels in the player's current chunk.
  - Overlay position is configurable.
  - This was moved to `Dangerous Features`.
  - Important files:
    - `src/main/java/com/example/ha/HaChunkChestCounter.java`
    - `src/main/java/com/example/ha/HaChunkChestOverlay.java`
    - `src/main/java/com/example/ha/HaChunkChestScreen.java`
    - `src/main/java/com/example/ha/HaChunkChestOverlayScreen.java`

## Removed Feature

- `Monitor Server` was removed in this session.
- Reason: Ping/TPS display was not useful enough, and TPS/ping approaches had tradeoffs.
- Removed items:
  - Config fields for ping/TPS monitor.
  - Main config menu button.
  - HUD render callback.
  - Tick update call.
  - Client play network handler mixin used for server timing packets.
  - All `HaServerMonitor*` files.
- Verification search used:
  - No remaining references to `ServerMonitor`, `pingMonitor`, `tpsMonitor`, `Monitor Server`, `ClientPlayNetworkHandlerMixin`, `TPS`, or `Ping` under `src`.

## Important Files

- Main initializer: `src/main/java/com/example/ha/HaClientMod.java`
- Tick logic and scoreboard parsing: `src/main/java/com/example/ha/HaTickHandler.java`
- Config model and JSON persistence: `src/main/java/com/example/ha/HaConfig.java`
- Main config screen: `src/main/java/com/example/ha/HaConfigScreen.java`
- Dangerous features screen: `src/main/java/com/example/ha/HaDangerousFeaturesScreen.java`
- Mixin list: `src/main/resources/ha.client.mixins.json`

## Notes For Next Session

- If adding a new feature, first decide whether it is normal or dangerous.
- Normal features should not depend on `macroEnabled`.
- Dangerous features should respect `macroEnabled`.
- For UI changes, keep button order clear:
  - Main config currently includes Dangerous Features, Item Lock, HP Alert, Camera, Mana Alert.
  - Dangerous Features currently includes Change Macro Toggle Key, Default Weapon Position, Auto Heal, Item Macro, Chunk Containers.
- If changing Item Macro again, be careful not to reintroduce instant 0-tick swaps or automatic return-to-original-slot behavior.
- If changing Mana Alert, test against scoreboard lines where color codes or formatting may be present.

