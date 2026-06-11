# Feature Workflow

1. Describe current behavior from source, not memory.
2. State the requested behavior and unresolved assumptions.
3. Classify the feature as Safe, Full, or Both using `docs/SAFE_FULL_POLICY.md`.
4. Identify config, menu/search, keybinding, tick, render, packet, mixin, persistence, and disconnect surfaces.
5. Default new behavior to disabled unless approved otherwise.
6. Keep parsing and calculation independent from Minecraft classes where practical.
7. Avoid adding unrelated responsibilities to `HaConfig`, `HaTickHandler`, or another oversized class.
8. Preserve existing user changes and JSON compatibility.
9. Verify the complete user flow, not only compilation.
10. Record reusable knowledge or architectural decisions after verification.
