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
- Shared checkout: name one Gradle owner in the delegation. Other agents hand off their check commands to that owner.
  Use separate worktrees when agents need independent build loops.
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

## Delegation

- Own the task end to end. Inspect known paths and symbols, make localized changes, and fix clear test failures inline.
- Delegate only a bounded investigation whose result you will use, a self-contained implementation slice, or an
  independent review when requested or warranted by material risk. Keep parallel scopes distinct. Verify returned
  findings and diffs yourself.
- State the question or expected behavior, the scope, the allowed operations, and the deliverable. For changes, include
  acceptance criteria and name the Gradle owner. For review, include the exact diff and any checks already run.
- Use direct subagent calls for routine delegation. Reserve orchestrated workflows for explicit requests.
- For read-only research without a model pinned to the role, explicitly choose a lower-cost model and match reasoning
  effort to the question. Set a turn limit when supported and ask for concise evidence.
  For example, in Pi call `Agent` with `model: openai-codex/gpt-6-luna`, `thinking: medium`, and `max_turns: 8`.

## Review handoff

- Before reporting completion after a delegated review, account for every finding: fix it, refute it with evidence, or
  report it as deferred with the remaining risk and reason.

## Quality rules

- Fix the code or tests when a check fails. Keep compiler, formatting, static-analysis, coverage, mutation, and
  dependency-analysis rules unchanged; do not weaken them to make a change pass.
- Library modules use Kotlin explicit API mode. Declare the visibility and return types the compiler requires.
- Document public API contracts with concise KDoc. Use an **Implementation requirements** section for nontrivial
  obligations that every implementation must satisfy. Keep technology-specific implementation notes in concrete
  implementations.

## Dependencies

- Declare every dependency the code uses directly; do not rely on transitively available dependencies.
- Before adding or promoting a dependency, check the existing conventions in `build-logic/` and
  `gradle/libs.versions.toml`.
- Keep production Spring Boot starters in `bootstrap`. Library modules should depend on the specific libraries
  and Spring modules they use. Test starters may be declared in the modules that need them.
- Prefer `runtimeOnly` for dependencies required at runtime but not used directly by source code.
- Use dependency analysis bundles for cohesive sets of artifacts, such as Spring Boot starters and framework
  internals. Keep exclusions narrow and justified.

## Issue tracker

Issues and specs live in GitHub Issues. See `docs/agents/issue-tracker.md`.

## Domain docs

This is a single-context repository. See `docs/agents/domain.md`.
