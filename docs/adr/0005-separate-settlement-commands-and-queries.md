# Separate Settlement commands and queries

Application-wide typed commands enter through one dispatch interface with command-specific handlers, reconstruct a
Settlement write model from its ordered event stream, and commit newly decided events with optimistic concurrency.
Queries read an independently maintained projection; history remains an explicit event-stream query. This replaces
command decisions based on the last event and queries that replay the stream on every request, at the cost of projection
maintenance and possible lag when delivery becomes asynchronous.

The first implementation slice covers opening and renaming Settlements only; Participants, Expenses, Repayments, and
Valuations remain future work. For now, the store and projection are in-memory and updates are synchronous. Handlers
return command-specific result types, such as `Result<IdentifierConflict, SettlementId>` for opening; there is no
universal command result or shared failure envelope. Callers must not rely on read-after-write consistency as part of
the command contract. The initial synchronous adapter normally updates the projection before dispatch returns, but this
property may disappear with asynchronous delivery. The first event has sequence 1; an aggregate loaded from N events has
loaded version N. Appending K incoming events expects stored version N and commits sequences N+1 through N+K; any
aggregate current version that includes pending events is distinct from this loaded version. The event store can
enumerate all Settlement streams so a fresh projector can rebuild its entire view before command dispatch begins.
Handlers load or create an aggregate and save it through a sourced repository. The repository appends the aggregate's
`incoming` events against its loaded version, clears them after successful append/commit and before publication. A
per-Settlement in-process lock spans load, decision, and append; any remaining version conflict throws instead of
automatically retrying. Opening an existing Settlement with the same opening details is idempotent, while conflicting
details return a typed business failure. Business rejections stay typed; missing streams, version conflicts, and
post-commit projection failures throw distinct exceptions. The concrete sourced repository composes the event store and
a committed-event publisher: after append, it coordinates synchronous, in-order publication. The repository does not
know about views; a subscribed projector owns updates to the Settlement read model. The repository interface does not
expose publisher operations. The publisher stops at the first listener failure for now; multiple-listener failure
handling can be revisited later. Append is the commit point: if projection fails afterward, the command remains
committed. Runtime projection recovery is outside this initial scope; a post-commit projection failure must be logged
and surfaced as a distinct fatal exception carrying the committed version. At startup, rebuild replaces the existing
projection from the event store as the source of truth; projectors then subscribe to live events, and only afterward
accept commands. A projector tracks each Settlement's last applied stream version, ignores already-applied events, and
rejects gaps. Within the aggregate, `incoming` names uncommitted events destined for its stream; no separate outgoing
integration-event stream is needed yet.
