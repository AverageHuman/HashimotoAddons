# 0008: Critical Sound Stays Full-only And Triggers From Rendered Name Tags

Date: 2026-07-04

## Context

The owner wants a toggleable critical-hit sound in Full, using the same render-time name-tag inspection path that was already used for EXP/debug label work. The sound must ship inside the jar so the feature works after download without extra setup.

## Decision

- Keep `Critical Sound` in the Full-only UI and runtime surface.
- Trigger playback from `EntityRenderer.renderLabelIfPresent` after inspecting the rendered text.
- Match the first visible text segment against the dagger glyph with `styleColor = #AA0000` and `bold = true`, and require the full label to contain the wave glyph.
- Register a dedicated `critical_sound` sound event only in Full builds.
- Bundle the source `crit.mp3` alongside the converted OGG playback asset so the jar carries both the original audio source and the Minecraft-playable file.

## Consequences

- Safe builds stay free of the feature surface.
- Full builds can test playback from the config screen and can ship a self-contained sound asset.
- If the source audio changes later, the OGG asset must be regenerated before release.
