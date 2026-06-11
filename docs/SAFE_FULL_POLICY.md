# Safe And Full Policy

## Safe

Safe is intended for distribution to other players. It must not expose dangerous features through:

- menus or search results
- commands
- keybindings
- configuration fields or screens
- tick, input, packet, or render behavior
- persisted active state

Normal informational and protective quality-of-life features may be included after explicit classification.

## Full

Full may include explicitly approved automation and server-sensitive features. New dangerous behavior must default to disabled unless the owner requests otherwise.

## Classification

Classify a feature as Full-only when it performs gameplay input, automates repeated actions, alters normal interaction, or inspects server/game state in a potentially sensitive way. Ask the owner when classification is ambiguous.

## Change Checklist

- Declare `Safe`, `Full`, or `Both` before implementation.
- Check menus, search, commands, keybindings, config, callbacks, mixins, and persisted state.
- Build and verify both variants for boundary-sensitive changes.
- Record a policy decision under `docs/decisions/` when classification establishes a reusable precedent.
