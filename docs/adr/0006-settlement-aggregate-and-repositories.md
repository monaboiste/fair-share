# Model Settlement as an event-sourced aggregate root

Settlement extends a shared `AggregateRoot` that applies registered events, keeps them pending, and tracks its version;
`committedVersion` is the stored stream version the next append expects. Domain events carry their own `occurredAt`
instant, so the domain receives plain `Instant` values and never sees a `Clock`, event identifiers, or envelopes.
Handlers read the clock at the application boundary. The event-sourced repository implements the domain's read and
write ports: `findById` recreates a Settlement from its stream or returns empty, and `save` wraps pending events in
envelopes with an `EventId`, appends them against the committed version, flushes them only after the append succeeds,
and then hands the committed envelopes to a single post-commit consumer. Opening checks `findById` instead of
loading-or-creating, and renaming an unknown Settlement returns a typed `SettlementNotFound`. Concurrency relies only on
optimistic version checks; a stale save throws `VersionConflictException` without retrying. Packages follow
`settlement.{domain, domain.event, application.command, application.query, infrastructure}`, and there is no bootstrap
class: wiring stays in tests until an IoC container arrives.

This supersedes the parts of ADR-0005 that describe the per-Settlement in-process lock, load-or-create, the `incoming`
event naming, and the subscribable committed-event publisher. Outgoing integration events and batched publication are
deferred until another context consumes Settlement events.
