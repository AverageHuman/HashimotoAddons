# 0010: Command Key is Full-only

Date: 2026-09-10

## Decision

`Command Key` is available only in the Full variant and is disabled by default.

## Rationale

The feature sends a user-configured chat command when a dedicated key is pressed. This alters normal interaction and can have server-sensitive effects, so it meets the Full-only classification in `SAFE_FULL_POLICY.md`.

## Consequences

Safe does not register the keybinding, expose the Dangerous Features entry or screens, load or save Command Key state, or execute a configured command. The user must type the leading slash in the command field; the feature never adds one.
