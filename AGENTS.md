# HashimotoAddons Development Rules

## Project

- Target Minecraft Java Edition 1.16.5 with Fabric.
- Treat Full and Safe as separate supported products.
- Communicate with the project owner in Japanese.

## Before Editing

- Read `.codex/CURRENT_STATE.md` when it exists.
- Run `git status --short` and inspect the current branch and HEAD.
- Preserve unrelated and uncommitted user changes.
- Read the implementation surrounding the requested behavior before choosing an approach.
- Decide explicitly whether the change belongs in Safe, Full, or both.

## Implementation

- Default new features to disabled unless the owner requests otherwise.
- Put gameplay automation and server-sensitive behavior in Full unless explicitly approved for Safe.
- Separate Minecraft-dependent integration from testable parsing and business logic where practical.
- Do not silently swallow persistence, process, or network failures.
- Keep changes scoped.
- Keep configuration state thin: `HaConfig` should hold feature state and normalization only; persistence, schema migration, and JSON mapping belong in dedicated classes.
- Add new configuration fields to the smallest feature-owned group that fits them, and split a group before it becomes another large mixed-responsibility block.
- When a config change needs a new DTO, mapper, or migration step, put that logic beside the owning feature or persistence layer rather than back in `HaConfig`.

## Safe And Full

- Safe must not expose Full-only UI, commands, keybindings, configuration, or runtime behavior.
- Every new feature must declare its variant policy.
- Verify both variants when variant-sensitive code changes.
- Follow `docs/SAFE_FULL_POLICY.md` as the durable policy source.

## Verification

- Match verification effort to risk and blast radius.
- Test configuration persistence when adding or changing settings.
- Test disconnect and shutdown behavior for stateful features.
- Report commands run, results, and checks that were not run.
- Follow the completion checklist in the HashimotoAddons development skill.

## Git And Release

- Do not change the version unless requested or approved.
- Do not commit or push unless requested.
- Do not treat generated jars as proof that source changes were committed.

## Project Knowledge

- Record durable architectural decisions under `docs/decisions/`.
- Record reusable discoveries in `docs/KNOWLEDGE.md`.
- Record unresolved structural problems in `docs/TECHNICAL_DEBT.md`.
- Keep only volatile session state in `.codex/CURRENT_STATE.md`.
