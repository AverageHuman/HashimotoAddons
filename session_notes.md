# Session Notes

## Project
- Minecraft Java Edition `1.16.5`
- Fabric client mod
- Mod display name: `HashimotoAddons`

## Build Output
- Main jar output path:
  - `C:\Users\sasaki\Documents\Codex\2026-05-15\minecraft-java-edition-1-16-5\build\libs\ha-fabric-1.0.0.jar`
- Sources jar output path:
  - `C:\Users\sasaki\Documents\Codex\2026-05-15\minecraft-java-edition-1-16-5\build\libs\ha-fabric-1.0.0-sources.jar`

## Main Features Implemented
- `/ha` opens the main GUI
- Main menu GUI with paging
- `Dangerous Features` submenu
- Macro toggle key setting
- Auto Heal
  - configurable enable/disable
  - configurable hotbar key
  - configurable cooldown in seconds
  - configurable HP percentage threshold
- Item Macro
  - multiple macros
  - editable interval seconds
  - editable hold ticks
  - default weapon position for return slot
- Item Lock
  - enable/disable from main menu
  - press `L` while hovering a player-inventory slot in inventory screens to toggle lock
  - locked slots block dropping
  - armor slot locks are temporarily suspended while chest/container screens are open
  - lock markers render on inventory screens
- HP Alert
  - multiple alert entries
  - configurable HP percentage
  - configurable title color
  - plays title + sound when threshold is crossed

## Important Implementation Notes
- Item lock was moved from fragile reflection/tick polling to mixin-based handled-screen input handling.
- Locked slot identity now uses player-inventory-relative slot index, not container-local slot id.
- Swap hold uses absolute world time for more stable hold duration.
- Macro return slot now uses `defaultWeaponHotbarSlot`.

## Important Files
- `src/main/java/com/example/ha/HaConfig.java`
- `src/main/java/com/example/ha/HaClientMod.java`
- `src/main/java/com/example/ha/HaTickHandler.java`
- `src/main/java/com/example/ha/HaConfigScreen.java`
- `src/main/java/com/example/ha/HaDangerousFeaturesScreen.java`
- `src/main/java/com/example/ha/HaAutoHealScreen.java`
- `src/main/java/com/example/ha/HaMacroListScreen.java`
- `src/main/java/com/example/ha/HaMacroEditScreen.java`
- `src/main/java/com/example/ha/HaHpAlertListScreen.java`
- `src/main/java/com/example/ha/HaHpAlertEditScreen.java`
- `src/main/java/com/example/ha/HaItemLockOverlay.java`
- `src/main/java/com/example/ha/HaItemLockHelper.java`
- `src/main/java/com/example/ha/mixin/HandledScreenAccessor.java`
- `src/main/java/com/example/ha/mixin/HandledScreenMixin.java`
- `src/main/java/com/example/ha/mixin/HandledScreenRenderMixin.java`
- `src/main/java/com/example/ha/mixin/ClientPlayerEntityMixin.java`
- `src/main/java/com/example/ha/mixin/SlotAccessor.java`
- `src/main/resources/fabric.mod.json`
- `src/main/resources/ha.client.mixins.json`

## Config File
- Runtime config path:
  - `config/HashimotoAddons/config.json`

## Current Known Context
- User wants clickable jar path in final answers when a build is updated.
- User may continue expanding GUI-driven gameplay helper features.
