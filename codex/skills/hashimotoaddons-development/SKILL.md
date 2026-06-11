---
name: hashimotoaddons-development
description: Develop, review, refactor, test, document, hand off, build, or release the HashimotoAddons Minecraft 1.16.5 Fabric mod. Use for feature work, bug fixes, Safe/Full decisions, configuration changes, UI and HUD changes, architecture improvements, repository maintenance, and session transitions.
---

# HashimotoAddons Development

1. Read `AGENTS.md`.
2. Read `.codex/CURRENT_STATE.md` when it exists, then verify it against Git.
3. Inspect relevant source and documentation before proposing implementation details.
4. Classify the task as feature, bug fix, refactoring, infrastructure, release, or handoff.
5. Declare whether behavior affects Safe, Full, or both.
6. Clarify requirements when product behavior, variant policy, defaults, UI, server-rule risk, versioning, or release intent is ambiguous.
7. Implement conservatively using current repository patterns while avoiding known technical debt where practical.
8. Verify according to `references/completion-checklist.md`.
9. Promote durable discoveries according to `references/session-transition.md`.
10. Do not build, change the version, commit, push, or release unless requested or included in the approved task.

Read only the reference relevant to the current work:

- Feature and behavior changes: `references/feature-workflow.md`
- Completion and review: `references/completion-checklist.md`
- Build and release: `references/release-workflow.md`
- Session transition and knowledge capture: `references/session-transition.md`
