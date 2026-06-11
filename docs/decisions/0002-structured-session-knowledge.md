# 0002: Structured Cross-Session Knowledge

Status: Accepted

## Context

Dated handoff files duplicated rules, preserved stale branches and paths, mixed temporary state with durable knowledge, and became difficult for a new session to interpret.

## Decision

Separate cross-session information into:

- `AGENTS.md` for mandatory repository rules
- a reusable Skill for workflows
- tracked `docs/` files for stable knowledge and decisions
- ignored `.codex/CURRENT_STATE.md` for volatile work state

Use one current-state file and update it in place rather than creating routine dated handoff files.

## Consequences

- Session transitions require promotion of durable discoveries instead of copying whole histories.
- The current-state file must be checked against Git at session start.
- Legacy notes remain migration sources until the owner approves archival or removal.
