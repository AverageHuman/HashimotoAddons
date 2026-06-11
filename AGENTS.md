# HashimotoAddons Development Rules

## Project

- Target Minecraft Java Edition 1.16.5 with Fabric.
- Treat Full and Safe as separate supported products.
- Communicate with the project owner in Japanese.
- Read `docs/PROJECT.md` for stable project facts and `docs/ARCHITECTURE.md` before structural changes.

## Before Editing

- Read `.codex/CURRENT_STATE.md` when it exists.
- Run `git status --short` and inspect the current branch and HEAD.
- Preserve unrelated and uncommitted user changes.
- Read the implementation surrounding the requested behavior before choosing an approach.
- Decide explicitly whether the change belongs in Safe, Full, or both.

## Implementation

- Default new features to disabled unless the owner requests otherwise.
- Put gameplay automation and server-sensitive behavior in Full unless explicitly approved for Safe.
- Do not add a new responsibility to an oversized class without considering extraction.
- Reuse established UI behavior when it remains appropriate, but do not copy known technical debt merely for consistency.
- Separate Minecraft-dependent integration from testable parsing and business logic where practical.
- Do not silently swallow persistence, process, or network failures.
- Keep changes scoped; separate feature work, infrastructure, and unrelated refactoring.

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
- Use `docs/DEVELOPMENT.md` for build and release procedures.

## Project Knowledge

- Record durable architectural decisions under `docs/decisions/`.
- Record reusable discoveries in `docs/KNOWLEDGE.md`.
- Record unresolved structural problems in `docs/TECHNICAL_DEBT.md`.
- Keep only volatile session state in `.codex/CURRENT_STATE.md`.
- Do not create a new dated handoff file for ordinary session transitions.
