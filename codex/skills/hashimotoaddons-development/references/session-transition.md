# Session Transition Workflow

## At Session Start

1. Read `AGENTS.md` and `.codex/CURRENT_STATE.md` when present.
2. Run Git status, branch, and HEAD checks.
3. Treat Git as authoritative when current-state data is stale.
4. Identify user changes that must be preserved.

## At Session End Or Context Transition

1. Update `.codex/CURRENT_STATE.md` in place.
2. Record only volatile facts: branch, HEAD, uncommitted changes, active objective, completed work, verification, and next action.
3. Move reusable implementation discoveries to `docs/KNOWLEDGE.md`.
4. Add or revise an ADR under `docs/decisions/` for durable design decisions.
5. Add unresolved structural issues to `docs/TECHNICAL_DEBT.md`.
6. Do not duplicate permanent rules in the current-state file.
7. Do not include secrets, webhook URLs, tokens, or private server data.
8. Re-check Git after writing the state file.

Do not create a dated handoff file unless the owner explicitly requests a historical snapshot.
