# 0006: Exp Debug Is Full-only And Logs Exact Text

Date: 2026-07-03

## Context

The EXP tracker debug command is used to inspect live entity labels while tuning name-tag-based parsing. For that workflow, `getString()` alone is not enough because formatting, hidden characters, and component structure can matter.

## Decision

- Keep `/ha expdebug` in the Full-only command surface.
- Do not register `expdebug` in Safe builds.
- When copying EXP tracker debug output, include the visible string, the serialized `Text` JSON, and codepoints for entity labels so the exact name tag can be reconstructed.
- Preserve the visible string and JSON in raw Unicode form, while escaping only the control characters needed to keep each debug entry on one line.

## Consequences

- Safe builds stay free of live label-dump diagnostics.
- Full users can capture the exact text they need before adding new label-based detection logic.
- Future text-based debug commands should follow the same exact-copy format instead of relying on plain strings alone.
