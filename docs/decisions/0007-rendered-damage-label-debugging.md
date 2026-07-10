# 0007: Rendered Numeric Damage Labels Are Tracked In Full

Date: 2026-07-03

## Context

Damage numbers can appear as entity name tags during rendering, which makes them available at the same hook point used for name-tag truncation. The owner wants those labels captured for critical-hit analysis, while keeping the copied debug log readable with raw Unicode text instead of `\\uXXXX` escapes.

## Decision

- Capture rendered numeric name tags from `EntityRenderer.renderLabelIfPresent` in the Full-only debug path.
- Treat the label capture as diagnostic-only and keep it out of Safe builds by gating the recorder behind the build flag.
- Keep copied debug payloads in raw Unicode form, while escaping only the control characters needed to preserve one-line log entries.

## Consequences

- Critical-hit and damage-label investigation can reuse the same debug clipboard flow as EXP tracker diagnostics.
- Safe builds remain free of the extra label-tracking recorder.
- Future label-based diagnostics should use the same raw-Unicode logging helper instead of appending `\\uXXXX` escapes directly.
