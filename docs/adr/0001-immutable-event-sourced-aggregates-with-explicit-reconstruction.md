# Immutable event-sourced aggregates with explicit reconstruction

Use `AggregateRoot<Id, E : DomainEvent, State>` with only `val id: Id` and `fun evolve(event: E): State`. Leave `State`
unconstrained and keep Decide methods on concrete aggregates. Unlike the
[previous mutable root](https://github.com/monaboiste/fair-share-mvp/blob/master/core/common/src/main/java/com/github/monaboiste/fairshare/common/eventsourcing/AggregateRoot.java),
deciding emits facts without changing state, and evolving applies a fact deterministically without recording it. This
separation makes replay independent of command execution and storage. No base class, generic repository, or shared
replay lifecycle. Clocks, ID generation, pending events, stream revisions, persistence, and commit tracking stay outside
aggregates.

`Group` is a regular Kotlin class with a private constructor, no public setters, and no unrestricted `copy`. A domain
factory accepts `GroupId` and specifically `GroupCreated`, not a command or arbitrary event. Existing state changes only
by evolving domain events. State is deeply immutable: evolving must not alter earlier states, including nested
collections. Choose defensive copies or persistent immutable collections when adding Participants.

Application owns Group-specific reconstruction on the command side, also reused by the offline rebuild.
`GetGroup` reads the derived view without reconstruction, as specified in
[ADR 0004](0004-synchronous-group-views-with-offline-rebuild.md). For an existing-Group command, a missing stream means
Group not found. For a loaded stream, verify its
ID against the requested `GroupId` mapping and use that verified identity rather than store it again in event payloads.
Rely on `EventStream` for structural validation. Require `GroupCreated` first, then pass only Group event payloads to
evolution in sequence order. Reject foreign events, invalid creation, and impossible transitions at their recorded
positions without returning partial state, invoking current Decide methods, or running Pricing or recalculating
financial facts. Report consistency failures with stream identity, offending sequence, event ID, and original cause
where applicable; keep diagnostics out of public responses. Application supplies event IDs and recording timestamps for
`PendingEvent`; persistence assigns sequence and committed revision. Replay creates no metadata.

Use a domain `Decision<E, Rejection>` rather than a general-purpose `Result`. `Rejected` carries a typed expected
rejection; `Accepted` carries only an ordered, immutable event batch. Allow zero, one, or many events in the contract.
Snapshot mutable inputs and require immutable payloads. An empty acceptance makes no claim about appends or revisions.
Concrete outcomes, multi-event use cases, failure types, and commit orchestration belong to
[Application use case organization and error model](https://github.com/monaboiste/fair-share/issues/29). Same-currency
event emission and its commit semantics remain open in
[HTTP adapter conventions and Group revisions](https://github.com/monaboiste/fair-share/issues/33); domain owns
emission, application owns commits and revisions, and HTTP owns their wire representation.

Decision: [Event-sourced aggregate design](https://github.com/monaboiste/fair-share/issues/28).
