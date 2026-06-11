# Development

## Before Work

1. Read `AGENTS.md` and `.codex/CURRENT_STATE.md` when present.
2. Inspect `git status --short`, branch, and HEAD.
3. Identify unrelated changes that must be preserved.
4. Classify the requested behavior as Safe, Full, or Both.

## Current Build Commands

```powershell
.\gradle-9.4.1\bin\gradle.bat build -PhaVariant=full
.\gradle-9.4.1\bin\gradle.bat build -PhaVariant=safe
```

The local Gradle distribution is currently ignored and is not a reproducible repository dependency. Introducing Gradle Wrapper remains tracked technical debt.

## Continuous Integration

GitHub Actions runs for pushes to `main` and `codex/**`, pull requests, and manual dispatches. CI performs:

- repository-owned Codex Skill validation
- a Full build using Java 17 and Gradle 9.4.1
- a Safe build using Java 17 and Gradle 9.4.1
- upload of non-dev Jar artifacts for 14 days

Workflow: `.github/workflows/ci.yml`

## Completion

- Verify behavior proportional to the change risk.
- For variant-sensitive changes, verify both variants.
- Update durable documentation only when knowledge or policy changed.
- Refresh `.codex/CURRENT_STATE.md` before a session transition.
- Commit, push, version bump, and release only when requested or approved.

## Session Transition

Do not create another dated session note by default. Update the single current-state file and promote durable information to the appropriate tracked document.

## Codex Skill

The tracked Skill source is `codex/skills/hashimotoaddons-development`. Install or refresh it by copying that directory to `%USERPROFILE%\.codex\skills\hashimotoaddons-development`. Treat the repository copy as authoritative.
