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
nonempty batch before a single conditional append. On confirmed commit, return the resulting local state with the
revision reported by the port. Do not reload: a later writer could otherwise change what this operation returns.

Decision and conventions: [#29](https://github.com/monaboiste/fair-share/issues/29). Query strategy remains in
[#32](https://github.com/monaboiste/fair-share/issues/32); HTTP preconditions and empty-batch behavior remain in
[#33](https://github.com/monaboiste/fair-share/issues/33).
