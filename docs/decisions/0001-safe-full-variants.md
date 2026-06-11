# 0001: Safe And Full Are Separate Supported Variants

Status: Accepted

## Context

HashimotoAddons contains ordinary quality-of-life features and features that automate gameplay or may be sensitive under server rules. A shareable build must provide a stronger boundary than an in-game toggle.

## Decision

Maintain Safe and Full as separate supported build variants. Safe must not expose Full-only UI, commands, keybindings, configuration, or runtime behavior. Feature work must state its variant classification before implementation.

## Consequences

- Variant-sensitive changes require verification in both builds.
- Registration and persistence paths are part of the boundary, not only menus.
- Ambiguous feature classification requires owner confirmation.

## Rejected Alternative

A single build with dangerous features disabled by default was rejected because disabled features would still be distributed and could be exposed accidentally.
