# Application use cases with explicit failure semantics and conditional event commits

Use one concrete, constructor-injected Kotlin use case per operation, wired in the composition root. Writes take typed
commands; simple reads take typed parameters. Inbound adapters construct validated domain values without duplicating
domain rules. This keeps orchestration explicit without a handler framework or a growing Group service.

Preserve the domain's typed decisions from
[ADR 0001](0001-immutable-event-sourced-aggregates-with-explicit-reconstruction.md). Use cases return success directly
and signal expected failures through a small set of semantic exceptions with structured reasons and stable identifiers.
This avoids repeating outcome variants in each use case, at the cost of documenting and testing exceptions rather than
enforcing exhaustive handling in Kotlin. Technical port failures propagate unchanged; confirmed non-commit and unknown
commit outcomes are distinct, and neither triggers automatic recovery. HTTP translation stays in the adapter.

Use a strictly positive application-owned `GroupRevision`, separate from the event-store `StreamRevision` and its
`INITIAL` sentinel. An existing-Group write reconstructs state and obtains its revision from one stream snapshot before
checking the precondition, accepting replay cost so stale requests cannot mask invalid history. Apply the accepted
nonempty batch and map the candidate state before starting the write transaction. The conditional append checks the
expected revision again. Return the local result with the persistence-assigned revision only after confirmed commit. Do
not reload: a later writer could otherwise change what this operation returns. For an accepted empty decision, return
the verified loaded snapshot and its revision without starting a write transaction, as specified in
[ADR 0005](0005-group-revision-etags-and-no-op-writes.md).

## Transaction boundary

Application uses `spring-tx` directly as a deliberate exception to framework independence. The Group write path uses a
constructor-injected `TransactionTemplate`, without `@Transactional`, component annotations, scanning, or a custom
transaction abstraction. Domain remains framework-independent. Infrastructure configures the
`PlatformTransactionManager` and transaction-participating persistence adapters; bootstrap wires them.

Reject ambient transactions at command entry. Require the write callback to own a new physical transaction, not join or
suspend a caller's transaction. The conditional append is the first database operation. `AppendEvents` participates in
the active writable transaction instead of opening or committing one; `Appended(StreamRevision)` means the batch was
written in that transaction, not that it is durable. Save the Group view in the same transaction and against the same
data source, then return success only after the template completes. On revision mismatch skip the view write, roll back,
and report the existing application failure only after successful rollback. An append or view-save failure rolls back
both writes. Preserve the original failure diagnostics if rollback also fails. Infrastructure classifies uncertain
commit for the entire transaction; do not report success or automatically retry.

Decision: [Application use case organization and error model](https://github.com/monaboiste/fair-share/issues/29),
amended by [Query consistency and read model strategy](https://github.com/monaboiste/fair-share/issues/32). See
[ADR 0004](0004-synchronous-group-views-with-offline-rebuild.md) for view maintenance and queries and
[ADR 0005](0005-group-revision-etags-and-no-op-writes.md) for HTTP preconditions.
