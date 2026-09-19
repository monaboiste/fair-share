# Repository conventions

- Use the Java version from [.sdkmanrc](.sdkmanrc). Configure `JAVA_HOME` from
  `~/.local/bin/sdkman/candidates/java/<version>` before running Gradle.
- Use double-quoted strings and four-space indentation in Groovy and Gradle Groovy DSL files.
- Keep shared Gradle configuration in convention plugins under `buildSrc`.
- Declare dependency versions and aliases in `gradle/libs.versions.toml`.
- Use conventional commits (`feat`, `fix`, `docs`, `chore`) with scopes. Prefer lowercase imperative subjects ≤50 chars
  and 72-char bodies.

## Testing conventions

- Write Groovy Spock tests named `*Spec` and extend `Specification` directly.
- Make specs read as natural language, following behavioral feature names, `given`/`when`/`then`
  flow, domain-named helpers, and semantic `where` labels and values. Reserve `expect` for single-phase expressions.
- Use Spock conditions and `thrown(...)`; keep context in names or block labels instead of comments.
- Ask before AssertJ; avoid JUnit assertions.

## Quality checks

Run `./gradlew format check` during development.

After meaningful production code or test changes, run `./gradlew coverageTest`.

Before completing non-trivial production code or test changes, run:
`./gradlew coverageTest mutationTest scan`.

Run these checks earlier when their results are useful for guiding implementation or tests. Do not run expensive quality
checks after every intermediate edit.

## Agent skills

### Issue tracker

Issues and specs live in GitHub Issues. See `docs/agents/issue-tracker.md`.

### Domain docs

This is a single-context repository. See `docs/agents/domain.md`.
