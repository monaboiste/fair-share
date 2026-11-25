# Run SQLite locally and migrate its schema explicitly

Deploy one or more backend processes on one host against the same local SQLite file. This keeps SQLite's simple
deployment model without weakening read-after-write across processes: a fresh read after a successful write sees that
commit or later, while an in-flight read may see the previous complete snapshot. Network filesystems and multi-host
deployments are unsupported.

Liquibase owns the schema, with one changelog in `infrastructure` shared by the Gradle migration task and persistence
integration tests. Run migrations explicitly with all backend processes stopped, then start the matching application
version. Keeping DDL out of startup avoids concurrent application instances changing their shared database and keeps
Liquibase out of the application runtime. Rolling schema upgrades are unsupported.

## Consequences

- Both migration and application configuration require an explicit absolute database file path. Migration may create the
  file; startup must not. The initial changeset preserves the existing event schema constraints and refuses unmanaged
  event tables rather than adopting them.
- Changesets update SQLite's `PRAGMA user_version` transactionally with schema changes, starting at `1`. This
  application-owned marker is independent of release versions and Liquibase bookkeeping. Startup must verify an exact
  version match and the presence of required event tables without changing the schema.
- WAL is the default; `DELETE` is opt-in. Set or change journal mode explicitly while all backend processes are stopped,
  not on connection acquisition. Preserve unpooled connections with `foreign_keys=ON`, `busy_timeout=5000`, and
  `synchronous=FULL`, atomic conditional appends, success only after commit, and no automatic write retries.
