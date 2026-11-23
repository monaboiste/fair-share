# Agent instructions

## Java environment

- Use the JDK specified in `.sdkmanrc`.

- SDKMAN initialization is not required. Before running Gradle, set `JAVA_HOME` to the matching SDKMAN installation:

  ```bash
  java_version=$(sed -n 's/^java=//p' .sdkmanrc)
  export JAVA_HOME="${SDKMAN_DIR:-$HOME/.sdkman}/candidates/java/$java_version"
  ```

- Do not change the global Java default.

## Build and verification

- Run Gradle through the wrapper: `./gradlew`.
- Run verification only when modifying source code, tests, build logic, or build configuration, unless explicitly
  requested otherwise.
- Do not run verification for read-only tasks (reviews, investigations, grilling, planning) or documentation-only
  changes.
- During implementation, run the narrowest relevant tests for fast feedback.
- Run `./gradlew verify` frequently after meaningful code changes.
- Before considering a substantial code change complete, run `./gradlew qualityCheck`.

A substantial code change includes new behavior, a bug fix, a non-trivial refactor, dependency or build-logic changes,
or changes spanning multiple code files. For small, localized code changes, `verify` plus the relevant focused tests is
sufficient unless another applicable instruction requires more.

## Quality rules

- Fix the code or tests when a check fails. Keep compiler, formatting, static-analysis, coverage, mutation, and
  dependency-analysis rules unchanged; do not weaken them to make a change pass.
- Library modules use Kotlin explicit API mode. Declare the visibility and return types the compiler requires.

## Dependencies

- Declare every dependency the code uses directly; do not rely on transitively available dependencies.
- Before adding or promoting a dependency, check the existing conventions in `build-logic/` and
  `gradle/libs.versions.toml`.

## Issue tracker

Issues and specs live in GitHub Issues. See `docs/agents/issue-tracker.md`.

## Domain docs

This is a single-context repository. See `docs/agents/domain.md`.
