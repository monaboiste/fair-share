# Run SQLite locally and migrate its schema explicitly

Deploy one or more backend processes on one host against the same local SQLite file. This keeps SQLite's simple
deployment model without weakening read-after-write across processes: a fresh read after a successful write sees that
commit or later, while an in-flight read may see the previous complete snapshot. Network filesystems and multi-host
deployments are unsupported.

Liquibase owns the schema, with one changelog in `infrastructure` shared by the Gradle migration task and persistence
integration tests. Run migrations explicitly with all backend processes stopped, then start the matching application
version. Keeping DDL out of startup avoids concurrent application instances changing their shared database and keeps
Liquibase out of the application runtime. Rolling schema upgrades are unsupported. A migration that invalidates
materialized Group views also requires the explicit rebuild in
[ADR 0004](0004-synchronous-group-views-with-offline-rebuild.md) before normal startup.

## Consequences

- Migration requires an absolute database file path and may create the file. Startup requires an explicit SQLite JDBC
  URL pointing to the same existing file and must not create it. The URL identifies the file only. Do not pass
  connection settings, especially journal mode, as URL parameters. The initial changeset preserves the existing event
  schema constraints and refuses unmanaged event tables rather than adopting them.
- Changesets update SQLite's `PRAGMA user_version` transactionally with schema changes, starting at `1`. This
  application-owned marker is independent of release versions and Liquibase bookkeeping. Startup checks for an exact
  version match without inspecting individual tables or changing the schema.
- Provision WAL unless explicitly opting into `DELETE`. Set or change journal mode explicitly while all backend
  processes are stopped, not on connection acquisition. Preserve unpooled connections with `foreign_keys=ON`,
  `busy_timeout=5000`, and `synchronous=FULL`, atomic conditional appends, success only after commit, and no automatic
  write retries. Once Group views are introduced, the physical commit includes the event append and view write.
