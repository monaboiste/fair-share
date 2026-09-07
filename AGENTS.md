# Repository conventions

- Use the Java version from [.sdkmanrc](.sdkmanrc). Configure `JAVA_HOME` from `~/.sdkman/candidates/java/<version>`
  before running Gradle.
- Write tests in Groovy with Spock.
- Name tests `*Spec`; extend `Specification` directly; use Spock conditions and `thrown(...)`, not assertion wrappers or
  JUnit assertions.
- Ask before using AssertJ.
- Use `given` for setup, `when` for behavior, and `then` for assertions; reserve `expect` for single-phase expressions.
- Parameterize repeated input cases with Spock `where` blocks; name results by meaning.
- Keep tests comment-free; put necessary context in Spock block labels.
- Use double-quoted strings and four-space indentation in Groovy and Gradle Groovy DSL files.
- Keep shared Gradle configuration in convention plugins under `buildSrc`.
- Declare dependency versions and aliases in `gradle/libs.versions.toml`.
- Run `./gradlew format compileJava` before finishing a change.
- Use conventional commits (`feat`, `fix`, `docs`, `chore`) with scopes. Prefer lowercase imperative subjects ≤50 chars
  and 72-char bodies.

## Agent skills

### Issue tracker

Issues and specs live in GitHub Issues. See `docs/agents/issue-tracker.md`.

### Domain docs

This is a single-context repository. See `docs/agents/domain.md`.
