# Prioritize Settlement lifecycle restrictions

When a Settlement is CLOSED, lifecycle restrictions take precedence over idempotency, conflict detection, and
command-specific validation, so command outcomes consistently reflect its protected settled state. Only
`RenameSettlement`, `RenameParticipant`, and explicit `ReopenSettlement` are allowed while CLOSED; other mutation
commands return `SettlementIsClosed` without emitting events. Consequently, `AddParticipant` and
`ConfigureExchangeRate` are rejected even when they would otherwise be idempotent no-ops.

The idempotency guarantees from [#16](https://github.com/monaboiste/fair-share/issues/16) and
[#19](https://github.com/monaboiste/fair-share/issues/19) apply only when the corresponding mutation is allowed by the
Settlement lifecycle state; this qualifies the add-retry guarantee in
[ADR-0008](0008-use-caller-supplied-participant-identifiers.md). Reopening is the explicit mechanism that restores
financial and membership mutations, rather than allowing retries to bypass CLOSED restrictions.
