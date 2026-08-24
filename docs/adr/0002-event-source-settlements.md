# Event-source each Settlement

All mutable Fair Share state will be reconstructed from one ordered event stream per Settlement. Expenses and repayments
are corrected through cancellation and replacement events, while balances and netting results remain derived
projections; event envelopes are schema-versioned and appended with optimistic concurrency. The first Event Store is
in-memory without snapshots, and copied archetype modules stay ordinary building blocks until Fair Share needs to
persist their configuration.
