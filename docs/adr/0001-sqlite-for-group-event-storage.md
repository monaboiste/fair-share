# Use SQLite for initial Group event storage

Use SQLite for the single-host MVP. It gives us durable, transactional storage without running a separate database
service. All backend processes must use the same database file on a durable local filesystem with SQLite-compatible
locking. Separate files, network filesystems, and deployments on multiple hosts are not supported.

The Group is the consistency boundary. The application port loads an ordered Group event stream and conditionally
appends a batch under one Group revision. All events in the batch become visible together or none do. The port does
not expose SQLite concepts. Reconsider PostgreSQL when instances must run on separate hosts, measured write contention
exceeds our latency or throughput targets, or we need database failover independent of the application host.

For #14, initialize the current schema idempotently before serving requests. Development schemas have no backward
compatibility guarantee. Fail startup on incompatible schemas without migrating, deleting or replacing existing files.
Liquibase is preferred for future schema management; its integration is deferred beyond #14.

Persist stable event type identifiers and explicit encoding versions independent of Kotlin class names. Reject
unsupported event types or versions rather than skipping them. Event encoding is independent of database schema
management. Do not introduce an upcasting framework until a real evolution requires one.

Each event has its own durable ID. Sequence orders events within a Group; timestamps do not determine replay order.
The Group view adapter reads and validates the stored facts and derives the representation and revision in one committed
snapshot without reconstructing the command aggregate.
