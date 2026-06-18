# Waypoint Render Knowledge Notes

Updated: 2026-06-18

## Goal

Fix the bug where waypoint text is hidden behind opaque blocks or appears to drift away from the waypoint when the camera or player moves.

This note is separate from `docs/KNOWLEDGE.md`.

## Reference Source

Odin reference folder:

- `src/main/kotlin/com/odtheking/odin/features/impl/dungeon/dungeonwaypoints/DungeonWaypoints.kt`
- `src/main/kotlin/com/odtheking/odin/features/impl/dungeon/dungeonwaypoints/DungeonWaypointEditor.kt`
- `src/main/kotlin/com/odtheking/odin/features/impl/dungeon/dungeonwaypoints/DungeonWaypointHud.kt`
- `src/main/kotlin/com/odtheking/odin/features/impl/dungeon/dungeonwaypoints/DungeonWaypointPacks.kt`
- `src/main/kotlin/com/odtheking/odin/features/impl/dungeon/dungeonwaypoints/SecretWaypoints.kt`

What the folder layout suggests:

- Waypoint data is owned separately from render code.
- Editing, HUD display, and pack selection are split into distinct responsibilities.
- The text layer is expected to behave like its own presentation path, not a side effect of box drawing.
- Per-waypoint display behavior belongs to the waypoint entry or its owning feature, not to a transient render pass.

## What We Learned From Prior Attempts

1. Rendering the label inside the same world-render pass as the box can leave the text visually behind the block.
2. Flushing a shared text buffer early can make the label appear for a moment, but that usually introduces ordering or drift bugs.
3. Moving the label to a HUD-style pass can improve visibility, but it is easy to lose the exact world anchor.
4. If the anchor is recomputed from a camera-relative value every frame, the label can appear to slide away from the waypoint.
5. If the text inherits depth state from the box or outline pass, opaque blocks can cover the label.
6. If the text and box share mutable render placement state, a later draw can corrupt the earlier one.
7. Readability problems can be caused by a missing fallback, such as shadow, outline, or a small vertical lift above the block.

## Why The Text Ends Up Behind The Block

The most likely root cause is render ordering plus state reuse:

- The box, outline, and text are not being emitted in fully isolated render paths.
- The label can inherit depth state from the geometry pass.
- The anchor can be tied to a frame-local transform instead of a stable block center.
- Camera movement changes the apparent position if the label is not anchored in a fixed world-space reference.
- A shared matrix or shared text buffer can make the text render where the box left it, not where the waypoint really is.

## 7 Fix Ideas, Ordered By Priority

### 1. Split the waypoint box pass and the text pass

Render the waypoint geometry first, then render the text in a separate pass with its own matrix and buffer usage.

Why this is high priority:

- It directly targets the ordering bug.
- It matches Odin's separation between world presentation and text presentation.

### 2. Anchor the label to the waypoint block center

Use the saved block position plus the block center, or an equivalent fixed world-space anchor, for label placement.

Why this matters:

- It prevents the label from drifting when the camera moves.
- It keeps the text attached to the same waypoint every frame.

### 3. Make the text depth policy explicit

Draw the label with a deliberate depth choice instead of inheriting whatever the box pass used.

Why this matters:

- Opaque blocks should not accidentally cover the label.
- The label should follow an intentional visibility rule, not a leftover render state.

### 4. Keep render-time placement out of saved or shared mutable state

Cache immutable text metrics if needed, but recompute world-to-screen placement from the anchor every frame.

Why this matters:

- Camera-relative placement should not be stored and reused.
- Cached position data is a common source of drifting labels.

### 5. Add a small, consistent vertical lift for the text

Place the label slightly above the block top so it has a better chance of staying readable in crowded scenes.

Why this matters:

- The label is less likely to intersect the block face visually.
- It gives the text a cleaner silhouette against walls and ceilings.

### 6. Store label visibility policy on the waypoint entry or feature state

Keep render mode, through-wall behavior, and label policy in the waypoint data model, not as ad hoc render-local flags.

Why this matters:

- It avoids accidental reuse of the wrong settings across frames.
- It matches Odin's data-driven structure.

### 7. Add debug output or a temporary visualization mode

Log the anchor position, depth choice, pass order, and any camera-relative offsets while debugging, or draw a temporary marker for the anchor.

Why this matters:

- It makes it much easier to tell whether the bug is anchor math, depth state, or ordering.
- It gives a quick way to confirm the text is still attached to the correct block.

## Practical Direction

The safest path is:

1. Fix the anchor first.
2. Split the render passes.
3. Then tune depth, offset, and debug visibility.

That sequence is the closest match to the Odin structure and is the least likely to introduce a new drift bug while fixing readability.

## Waypoint Knowledge

- A waypoint label should be anchored to a fixed world-space point, usually the center of the target block.
- Camera position should only be used to convert world coordinates into render coordinates, not to define the label anchor.
- Shape rendering and label rendering should not share mutable placement state.
- If a label moves when the player moves, the problem is usually anchor math or matrix reuse, not text content.
- If a label disappears behind opaque blocks, the problem is usually depth state or render ordering.
- Per-waypoint display settings are safer than global render-side flags because they stay attached to the data.
- Debugging should log anchor position, render pass, depth choice, and whether the label was emitted before or after the block geometry.
