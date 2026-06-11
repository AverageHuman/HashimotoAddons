# Technical Debt

## TD-001: Configuration responsibility concentration

- Priority: High
- Area: Configuration
- Problem: `HaConfig` owns most feature state, normalization, migration, loading, and saving.
- Impact: Every feature change touches a shared high-risk file and requires manual synchronization of multiple representations.
- Desired state: Feature-owned configuration models with a compatible aggregate persistence layer.
- Migration: Extract one feature group at a time while preserving the current JSON schema.

## TD-002: Oversized feature classes

- Priority: High
- Area: Architecture
- Problem: Several classes combine parsing, runtime state, UI integration, and persistence.
- Impact: Review scope and regression risk grow with each feature addition.
- Desired state: Separate integration, pure logic, state, and persistence where this reduces complexity.

## TD-003: Missing automated verification

- Priority: High
- Area: Tooling
- Problem: The repository has no unit-test suite, CI workflow, or static analysis gate.
- Desired state: Start with pure Java logic tests, then add Safe/Full builds and static checks to CI.

## TD-004: Non-reproducible build entrypoint

- Priority: Medium
- Area: Build
- Problem: Build scripts depend on ignored local Gradle distributions.
- Desired state: Add Gradle Wrapper and document the supported JDK.

## TD-005: Repeated infrastructure implementations

- Priority: Medium
- Area: Shared services
- Problem: Persistence, timers, HUD behavior, lifecycle cleanup, and process execution are implemented separately by features.
- Desired state: Introduce shared services only where they remove proven duplication and centralize safety guarantees.

## TD-006: Legacy handoff archive

- Priority: Low
- Area: Documentation
- Problem: Dated session notes mix stale state, durable rules, and historical implementation details.
- Desired state: Finish knowledge migration, then archive or remove legacy local notes with owner approval.
