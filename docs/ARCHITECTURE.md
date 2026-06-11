# Architecture

## Current Shape

- `HaClientMod` registers commands, keybindings, tick callbacks, HUD callbacks, and world-render callbacks.
- `HaTickHandler` coordinates much of the per-tick feature behavior.
- `HaConfig` contains configuration state, normalization, migration, loading, and saving for most features.
- Feature classes commonly combine Minecraft integration, state, parsing, and persistence.
- Mixin classes bridge Minecraft events that Fabric callbacks do not expose directly.

## Direction

New work should move toward feature-owned modules with explicit boundaries:

```text
feature/
  FeatureConfig
  FeatureService
  FeatureScreen
  FeatureOverlay
  FeatureParser
```

This is a direction, not a requirement for a disruptive rewrite. Extract responsibilities when touching an area and when the extraction lowers risk or duplication.

## Desired Shared Services

- Feature lifecycle: initialize, tick, disconnect, shutdown
- Variant-aware feature registration
- Configuration persistence and migration
- Debounced and atomic data saving
- HUD layout and visibility
- External command execution with timeout and diagnostics

## Boundary Rule

Keep Minecraft objects and callbacks near integration classes. Prefer plain Java inputs and outputs for parsing, matching, calculation, migration, and formatting so those behaviors can be tested without launching Minecraft.
