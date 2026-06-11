# Completion Checklist

Use the items relevant to the change and report skipped checks.

## Behavior

- Requested behavior is implemented end to end.
- Defaults and labels match the approved requirements.
- Stateful behavior stops correctly on disable, disconnect, world change, and shutdown where applicable.

## Variants

- Variant classification is explicit.
- Safe does not expose Full-only UI, search results, commands, keybindings, config, or callbacks.
- Both variants are built when the boundary may be affected.

## Configuration And Data

- New settings load, normalize, save, and preserve compatibility.
- Failure handling is visible and does not silently destroy user data.
- Frequent runtime updates do not cause unnecessary synchronous disk writes.

## Code Quality

- Unrelated changes remain untouched.
- New logic has a clear owner and does not deepen a large class without reason.
- Repeated or fragile behavior uses an existing shared helper or justifies a new one.
- Tests cover pure logic when a test surface exists.

## Delivery

- Commands and results are reported.
- Version changes, commits, pushes, and artifacts occur only when approved.
- Durable knowledge and current state are updated when needed.
