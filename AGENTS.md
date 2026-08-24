# Repository conventions

- Write production code in Java and tests in Groovy with Spock.
- Use double-quoted strings and four-space indentation in Groovy and Gradle Groovy DSL files.
- Keep shared Gradle configuration in convention plugins under `buildSrc`.
- Declare dependency versions and aliases in `gradle/libs.versions.toml`.
- Run `./gradlew format` before finishing a change.

## Agent skills

### Issue tracker

Issues and specs live in GitHub Issues. See `docs/agents/issue-tracker.md`.

### Domain docs

This is a single-context repository. See `docs/agents/domain.md`.
