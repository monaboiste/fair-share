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

Keep domain framework-independent and keep persistence and HTTP details out of the core. Application commands and
queries have separate packages. The first Group slice deliberately allows `spring-tx` in application for explicit
`TransactionTemplate` use. Commands reconstruct Group state from events and maintain a derived view in the same
transaction as the event append. `GetGroup` reads through its own query port without replay. See
[the Group views ADR](docs/adr/0004-synchronous-group-views-with-offline-rebuild.md) for the planned write, query and
rebuild contracts. The current pre-Group event store is described below.

The SQLite event store uses Spring's named JDBC and transaction templates. It encodes each nonempty batch before
starting a transaction, rejects ambient transactions, and reports success only after physical commit. Revisions count
batches, with zero reserved for an expected missing stream; sequences count individual events within a stream. A
revision mismatch or pre-commit failure rolls back the batch. A failed commit raises `UnknownCommitOutcomeException`
even if rollback succeeds. The store never retries an append automatically. Loads read metadata and ordered events
in one database snapshot, return `null` only for missing streams, and reject invalid history.

The SQLite transaction manager requires unpooled physical connections. It closes them without restoring auto-commit:
restoring it after a failed rollback could commit partial writes. Rollback failures are suppressed on the original
write or commit failure.

The supported deployment uses local, file-backed SQLite with `synchronous=FULL` by default.
Provision `WAL` before starting the application. In WAL mode, the writable database directory must accommodate the
adjacent `-wal` and `-shm` files. Application connections do not set journal mode.

The example endpoint is a temporary REST-only example: `GET /example` returns `{"message":"test"}`. Bootstrap configures
the SQLite data source and persistence templates but does not wire the event store, initialize its schema, or provide
a production codec before real event types exist.
The first feature with real events must supply a codec with explicit type and version mappings, independent of the
REST mapper. A codec must reject unsupported formats and malformed payloads with `IllegalArgumentException` so replay
identifies them as invalid history.

### Triage mutation failures

After a PIT run, list surviving and uncovered mutations from the existing reports:

```bash
python3 .github/scripts/quality_pr_summary.py mutation-triage --module :infrastructure --limit 20
```

Omit `--module` to include all modules. Triage does not rerun Gradle.

### Provision the SQLite event database

Stop all backend processes using the database. Using the JDK specified in `.sdkmanrc`, run:

```bash
database="$(pwd)/.data/fair-share.db"
mkdir -p "$(dirname "$database")"
./gradlew :infrastructure:migrateDatabase -PdatabasePath="$database"
sqlite3 "$database" 'PRAGMA journal_mode=WAL;'
```

`databasePath` must be an absolute path to a local SQLite file. Check that `sqlite3` prints `wal` before starting the
matching application version. Startup requires an explicit SQLite JDBC URL and fails if the file is missing or its
schema version is incompatible. It does not create or migrate the database. To opt into rollback journaling, use
`PRAGMA journal_mode=DELETE;` instead while all processes using the database are stopped.

Each Git worktree has its own `.data/` directory. Run the migration above for a fresh database. To reuse event history
from a schema-compatible worktree, use SQLite's `.backup` rather than sharing a live database or copying its `.db`
file in WAL mode. From the destination worktree root, before starting the backend:

```bash
mkdir -p .data
if [ ! -e .data/fair-share.db ]; then
    sqlite3 -readonly /absolute/path/to/source/.data/fair-share.db '.backup .data/fair-share.db'
fi
```

### Run locally

Use the same file as migration and the JDK specified in `.sdkmanrc`. The SQLite JDBC URL should identify the file only.
Do not pass connection settings, especially journal mode, as URL parameters.

```bash
FAIRSHARE_PERSISTENCE_SQLITE_JDBCURL="jdbc:sqlite:$database" ./gradlew :bootstrap:bootRun
curl http://localhost:8080/example
```

### Run in Docker Compose

Stop Compose before migrating or provisioning WAL for an existing deployment. Prepare the host file
`$(pwd)/.data/fair-share.db` as shown above, then build the matching Spring Boot JAR and start the application:

```bash
./gradlew :bootstrap:bootJar
docker compose up --build -d
curl http://localhost:8080/example
```

The application listens on port 8080. Compose bind-mounts the worktree's `.data/` directory at `/data`.
No separate database container is needed.

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

The worktree's `.data/` directory is retained.
