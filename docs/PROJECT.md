# Project

HashimotoAddons is a client-side Fabric mod for Minecraft Java Edition 1.16.5. It provides normal quality-of-life features and a separately distributed Full variant containing features that may automate gameplay or be sensitive under server rules.

## Supported Variants

- **Safe**: Shareable build. It must not expose dangerous features through UI, commands, keybindings, configuration, or runtime behavior.
- **Full**: Personal build containing both normal and explicitly approved dangerous features.

## Runtime Baseline

- Minecraft: `1.16.5`
- Fabric Loader and Fabric API
- Java bytecode target: Java 8
- Main entrypoint: `com.example.ha.HaClientMod`

## Source Of Truth

- Product rules: `AGENTS.md`
- Variant policy: `docs/SAFE_FULL_POLICY.md`
- Architecture: `docs/ARCHITECTURE.md`
- Reusable discoveries: `docs/KNOWLEDGE.md`
- Current session state: `.codex/CURRENT_STATE.md`
