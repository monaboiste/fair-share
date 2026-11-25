# Fair Share

Fair Share helps groups of people settle shared expenses in a simple and transparent way.

Imagine a group of friends going on a weekend trip to the mountains: one person pays for the accommodation, another for
groceries, someone else for fuel, and some expenses are shared only between part of the group. Fair Share keeps track of
who paid for what, applies the appropriate split rules, and calculates who owes whom at the end of the trip.

Expenses can also be made in different currencies, while preserving the exchange rate used at the time of each expense
so that historical settlements remain consistent.

## Backend

The backend is organized into five Gradle modules:

| Module             | Responsibility                                            |
| ------------------ | --------------------------------------------------------- |
| `domain`           | Domain models, business rules and domain events           |
| `application`      | Commands and queries, use cases and their required ports  |
| `infrastructure`   | REST and persistence adapters                             |
| `bootstrap`        | Spring Boot startup, wiring and external configuration    |
| `acceptance-tests` | Black-box HTTP tests of an externally running application |

Keep the domain and application independent of Spring and persistence. Commands and queries have separate packages.
Event payloads and their domain metadata live under `fairshare.domain.events`. Load and append ports are on the command
side because they reconstruct and commit state for commands. Query ports represent individual capabilities, rather than
a generic repository interface.

The SQLite event store uses named JDBC parameters. It encodes each nonempty batch before opening a dedicated connection,
then claims the revision and inserts all events in one explicit transaction. Revisions count batches, with zero
reserved for an expected missing stream; sequences count individual events within a stream. The store rolls back on a
revision mismatch or pre-commit failure. If commit throws a `SQLException`, the store raises
`UnknownEventCommitException` even if cleanup succeeds. It never retries an append automatically. Loads read metadata
and ordered events in one database snapshot, return `null` only for missing streams, and reject invalid history.

The supported deployment uses local, file-backed SQLite with WAL enabled and `synchronous=FULL` by default. The
writable database directory must accommodate the adjacent `-wal` and `-shm` files. The store requires a data source
that supplies a dedicated physical connection per append, not a connection bound to a caller's transaction.

The example endpoint is a temporary REST-only example: `GET /example` returns `{"message":"test"}`. Bootstrap configures
the SQLite data source but does not wire the event store, initialize its schema, or provide a production codec before
real event types exist. The idempotent schema initializer is test-only; Liquibase will be introduced separately.
The first feature with real events must supply a codec with explicit type and version mappings, independent of the
REST mapper. A codec must reject unsupported formats and malformed payloads with `IllegalArgumentException` so replay
identifies them as invalid history.

### Run locally

Use the JDK specified in `.sdkmanrc` and the Gradle wrapper.

```bash
./gradlew :bootstrap:bootRun
curl http://localhost:8080/example
```

### Run in Docker Compose

Build the ordinary Spring Boot executable JAR, then start the application:

```bash
./gradlew :bootstrap:bootJar
docker compose up --build -d
curl http://localhost:8080/example
```

The application listens on port 8080. SQLite is embedded in the application; its data directory is mounted as a named
Docker volume. No separate database container is needed.

### Run acceptance tests on demand

Start the application first (locally or with Docker Compose), then run:

```bash
./gradlew :acceptance-tests:test -Pacceptance
```

Set `FAIR_SHARE_BASE_URL` to test a different running instance. The tests do not start or stop the application,
build an image, or access its database. They are disabled in ordinary Gradle test and quality runs unless
`-Pacceptance` is explicitly supplied.

Stop the Compose environment when finished:

```bash
docker compose down
```

The named data volume is retained.
