# Expose committed events and separate event infrastructure

A Settlement command returns a typed `Result` containing `CommitResult` (persisted envelopes and stream version), rather
than only the Settlement identifier. An unchanged rename succeeds with no envelopes and the current version. Opening an
identifier that already has a Settlement is an `IdentifierConflict` regardless of its details: idempotent open retries
(issue #15) are deferred together with request idempotency, and caller permissions to use an identifier are out of
scope. An unknown Settlement returns typed `SettlementNotFound` from rename, view, and history queries; the view query
reads the projection, so with asynchronous delivery not found may also mean not projected yet. An unknown stream still
raises `StreamNotFoundException` at the store level. Version conflicts and store failures remain technical exceptions. A
concurrent open that commits first makes the later one throw `VersionConflictException`; nothing is retried. Optimistic
locking is the repository's append against the loaded stream version; request ETags belong to a future REST layer.

Event sourcing lives in `common.eventsourcing`: aggregate replay and commit transitions are package-private and the
generic repository owns replay and append, not publication. The stream identifier is already the aggregate identifier;
aggregate type metadata is deferred until different aggregate types share a physical store. The store assigns stream
sequence and global position atomically to each batch. A pending event carries the fact, generated `EventId`, and
registration time; an envelope adds stream sequence and global position at commit. `EventStreamReader`, `EventStore`,
and `AllEventsReader` separate stream reads, appends, and ordered global reads. The in-memory store is in
`common.events.inmemory`. The in-memory store owns synchronous delivery to subscribers inside its append lock, so every
append publishes and delivery follows commit order. Listeners run in subscription order, stop at the first failure, and
must not append during delivery. Append remains the commit point: failed publication logs and raises
`PostCommitPublicationException` carrying the committed version without rolling back the stream. Projection rebuilds
from global order, ignores duplicate deliveries, and rejects gaps atomically per batch.

Domain event payloads are plain facts: they declare `type()` and `schemaVersion()` but contain neither identifiers nor
occurrence time. The aggregate holds an injected `Clock` and stamps each registered fact with system time and a
generated `EventId`. `occurredAt` is the moment of registration, not a business date; business dates remain payload
fields. Tests are deterministic except for generated event identifiers, superseding issue #15's supplied-identifier
criterion. This supersedes ADR-0006's decision that the domain never sees a `Clock` or event identifiers. Replay passes
payloads alone; switch to envelopes if the aggregate ever needs past occurrence times. A `SettlementName` rejects blank
input but preserves all non-blank spacing. The aggregate holds the current name and the immutable Settlement Currency
explicitly. Historical names are not re-validated during replay, so later input-rule changes do not invalidate stored
streams. `SettlementOpened` carries the Settlement Currency as `CurrencyUnit`; converting it to a currency code is a
future serializer concern. `Settlement` and `SettlementRepository` live in the non-exported
`settlement.domain.aggregate` package, so `Settlement.factory(Clock)` and pending events are public only inside the
module; the apply guards keep a blank aggregate unusable. Business rejections use `Result` with the sealed
`SettlementRejection` (`IdentifierConflict`, `SettlementNotFound`) shared by every Settlement command and query, so
adding a rejection keeps signatures stable and exhaustive switches flag unhandled cases; malformed input throws.
Commands and queries both return `Result<F, S>`. `Command<F, S>` and `Query<F, S>` have unbounded failure types; the
`CommandFailure` marker is removed. Registered dispatchers provide exact-class routing and complete sealed-family
registration; command interceptors are ordered. Shared handler registration stays internal.

The common module exports events, in-memory events, event sourcing, commands, and queries, but not the handler registry.
The app exports Settlement commands, queries, the domain published language (identifier, name, rejections) and domain
events, alongside netting and valuation, but neither the aggregate package, the command and query handler packages, nor
infrastructure; callers use the dispatchers, and a future bootstrap module gets qualified exports. Durable-store
delivery (outbox, catch-up subscriptions by global position, async projections with checkpoints), upcasters, correlation
metadata, and bootstrap wiring are deferred.

This supersedes ADR-0005's identifier-only command results, stream enumeration, and repository-owned publication, and
ADR-0006's aggregate replay/flush visibility, repository-generated event identifiers and envelopes, repository
publication, and domain clock exclusion. Optimistic concurrency remains in force.
