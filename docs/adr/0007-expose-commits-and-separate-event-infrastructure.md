# Expose committed events and separate event infrastructure

A Settlement command returns a typed `Result` containing `CommitResult` (persisted envelopes and stream version), rather
than only the Settlement identifier. An unchanged rename succeeds with no envelopes and the current version. An
identical opening retry returns the original opening envelope and version 1, even after subsequent renames. Unknown
history raises `StreamNotFoundException`; unknown rename is a typed `SettlementNotFound`. Version conflicts and store
failures remain technical exceptions. On an opening version conflict, reload once and re-evaluate the original opening
details; do not retry the write. A rename may supply an expected version, checked before deciding even when the name is
unchanged.

Event sourcing lives in `common.eventsourcing`: aggregate replay and commit transitions are package-private and the
generic repository owns replay and append, not publication. The stream identifier is already the aggregate identifier;
aggregate type metadata is deferred until different aggregate types share a physical store. The store assigns stream
sequence and global position atomically to each batch. `EventStreamReader`, `EventStore`, and `AllEventsReader` separate
stream reads, appends, and ordered global reads. The in-memory store is in `common.events.inmemory`. The in-memory store
owns synchronous delivery to subscribers inside its append lock, so every append publishes and delivery follows commit
order. Listeners run in subscription order, stop at the first failure, and must not append during delivery. Append
remains the commit point: failed publication logs and raises `PostCommitPublicationException` carrying the committed
version without rolling back the stream. Projection rebuilds from global order, ignores duplicate deliveries, and
rejects gaps atomically per batch.

Domain events are plain facts with occurrence time; `@EventType` supplies validated type and schema version when the
store builds envelopes. A `SettlementName` rejects blank input but preserves all non-blank spacing. The aggregate holds
opening name, current name, and immutable Settlement Currency explicitly. Historical names are not re-validated during
replay, so later input-rule changes do not invalidate stored streams. `SettlementOpened` carries the Settlement Currency
as `CurrencyUnit`; converting it to a currency code is a future serializer concern. `Settlement` and
`SettlementRepository` live in the non-exported `settlement.domain.aggregate` package, so `Settlement.factory()` and
pending events are public only inside the module; the apply guards keep a blank aggregate unusable.
Business rejections such as `IdentifierConflict` use `Result`; malformed input throws.
`Command<F extends CommandFailure, S>` keeps each command's failure type while a registered dispatcher provides
exact-class routing, complete sealed-family registration, and ordered interceptors. Queries have a separate registered
dispatcher. Shared handler registration stays internal.

The common module exports events, in-memory events, event sourcing, commands, and queries, but not the handler registry.
The app exports Settlement commands, queries, the domain published language (identifier, name, rejections) and domain
events, alongside netting and valuation, but neither the aggregate package, the command and query handler packages, nor
infrastructure; callers use the dispatchers, and a future bootstrap module gets qualified exports. Durable-store
delivery (outbox, catch-up subscriptions by global position, async projections with checkpoints), upcasters, correlation
metadata, and bootstrap wiring are deferred.

This supersedes ADR-0005's identifier-only command results, stream enumeration, and repository-owned publication, and
ADR-0006's aggregate replay/flush visibility, envelope creation by the repository, and repository publication. The
optimistic concurrency and domain/application clock separation of those decisions remain in force.
