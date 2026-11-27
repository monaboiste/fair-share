# Materialize Group views in the event commit transaction

Keep the Group event stream authoritative. `GetGroup` reads a materialized `GroupView` through the application-owned
`LoadGroupView` port, without reconstructing the aggregate or loading events. Full-view replacement on the command path
makes reads independent of history length without introducing asynchronous lag. The trade-off is an extra write in every
Group event commit and a recovery procedure for derived data.

## Commands and queries

- Keep `LoadEvents` and `GroupReconstructor` command-side. Reconstruct and validate history, check the precondition,
  decide, evolve the candidate state, and map the complete view before starting the write transaction. Replay validates
  recorded facts and historical consistency, not current command acceptance rules. Never run Pricing during replay.
- A concrete internal `GroupWriter` shared by `CreateGroup` and `ChangeSettlementCurrency` uses Spring's
  `TransactionTemplate` directly as specified in
  [ADR 0002](0002-application-use-cases-with-explicit-failure-semantics-and-conditional-event-commits.md). It calls the
  participating `AppendEvents` and `SaveGroupView` outbound ports in the same writable transaction against the same data
  source. The conditional append is the first database operation inside the transaction. No production Group append-only
  entry point, additional commit port, or projection framework.
- `AppendEvents` assigns the revision. `SaveGroupView` replaces the full view with that revision before commit.
  `CreateGroup`, `ChangeSettlementCurrency`, and `GetGroup` return the shared application-owned `GroupResult`, pairing a
  `GroupView` with a positive `GroupRevision`. Command results use local state and the assigned revision, without a
  reload. REST maps the result to the body and ETag. Empty-batch and no-op semantics are decided separately in
  [HTTP adapter conventions and Group revisions](https://github.com/monaboiste/fair-share/issues/33), with behavior
  owned by domain and application.
- SQLite reads the view and checks stream-header existence and revision in the same SQL statement. Neither present means
  not found; one present or different revisions means internal consistency failure. Reject ambient transactions for
  normal query loads so an earlier snapshot cannot be reused. A read racing a commit sees one complete old or new state;
  a read starting after success sees that revision or later even on another instance using the same file.
- Matching revisions show revision consistency, not historical validity or full view correctness. `GetGroup` does not
  scan event payloads and cannot detect corruption that leaves the revision unchanged. Commands and explicit replay,
  such as rebuild, reject invalid history rather than producing partial results. Surface detected consistency failures
  through the existing generic internal error, with diagnostic detail only on the server.

## Rebuild and schema evolution

- Provide `rebuild-group-views` as an explicit non-web application invocation using the normal SQLite configuration and
  schema compatibility check. Stop all application instances using the database before invoking it. Use narrow
  administrative ports to enumerate Group IDs from authoritative stream headers and clear views. In one transaction
  clear and repopulate the views, reusing `LoadEvents`, reconstruction, mapping, and `SaveGroupView` and retaining each
  stream's revision. Write no events and never modify or delete history. This repairs missing or stale views, removes
  orphans, and is repeatable. Invalid history or a confirmed failure leaves the prior views unchanged; an uncertain
  commit is not proof of rollback. No failure can publish a partial replacement. Report failure with a nonzero exit.
- Liquibase changes that invalidate existing views mark them as requiring a rebuild in the migration transaction.
  Compatible changes need no marker. Normal startup refuses to serve while the marker is present; the admin invocation
  may proceed with the marker but may not bypass schema compatibility. Clear the marker in the same transaction that
  publishes the rebuilt views. Never rebuild automatically on startup or query miss. Document the actual invocation and
  recovery procedure with the first slice.
- Offline rebuild and its downtime are temporary MVP limitations. Stopping writers lets one transaction replace all
  views without managing simultaneous commits or catch-up. Operators must schedule downtime for recovery or an
  incompatible view migration and keep the service stopped until recovery succeeds. The intended direction is rebuilding
  while the service remains operational. Concurrent writes, catch-up, and atomic publication of a replacement generation
  belong to
  [Design and implement online Group read model rebuilding](https://github.com/monaboiste/fair-share/issues/45), not a
  blocker for this slice.

Decision: [Query consistency and read model strategy](https://github.com/monaboiste/fair-share/issues/32).
