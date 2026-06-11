# Build And Release Workflow

1. Confirm whether the owner requested a build, version change, commit, push, or release.
2. Inspect Git status and preserve unrelated changes.
3. Validate the requested version against `gradle.properties`.
4. Build Full and Safe when releasing or changing shared/variant-sensitive code.
5. Confirm artifact names and embedded mod metadata.
6. Report direct local links to requested artifacts.
7. Keep infrastructure, feature, and release metadata changes logically separate where practical.
8. Do not claim an artifact corresponds to a commit without checking HEAD and worktree state.

Current local commands are documented in `docs/DEVELOPMENT.md`. Prefer Gradle Wrapper after TD-004 is resolved.
