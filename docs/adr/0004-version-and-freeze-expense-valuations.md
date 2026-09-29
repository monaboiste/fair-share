# Version and freeze Expense Valuations

Settlement owns versioned Exchange Rate configuration as `ExchangeRateVersion`: version identifier, Exchange Rate,
Validity and `definedAt`. Events and views retain this data without storing Pricing calculators. All configured
selection rules (pair match, inclusive validity, latest `validFrom` and stream-order ties) are owned by a small
`ExchangeRateVersions` object in the valuation package; it exposes `Optional<ExchangeRateVersion> applicableAt`
and is the single owner of selection. `Settlement` selects a version for the Expense incurred-date midnight via
that object and maps an absent selection to `MissingExchangeRate`; it never duplicates selection rules. The
`ValuationEngine` no longer selects or combines selection with conversion: it receives an already-selected
`ExchangeRateVersion` and converts only that version to `SimpleComponentVersion`. The target currency remains in
the configured overload strictly as a defensive direction invariant check, never as selection. Same-currency
Expenses take an explicit `identity` path that consults no configured rates.
`CompositeComponentVersion` remains reserved for genuinely composite Valuations.

`definedAt` is retained Pricing version metadata supplied by the application Clock, not the event envelope's
`occurredAt`. Likewise, the version identifier identifies the configured version, not its event. These are domain
payload data; ADR 0007 keeps event identifiers and event occurrence time in the envelope. Neither `definedAt` nor
version identifiers determine selection order.

Exchange Rate versions use the existing component validity model, allow intentional overlaps, and resolve them by the
latest `validFrom`, followed by stream order. Expense `incurredOn` is a `LocalDate` and is evaluated at
`incurredOn.atStartOfDay()` against the inclusive `LocalDateTime` validity endpoints. An Exchange Rate
starting at noon does not apply to an Expense incurred on that same date; no time zone or whole-day
normalization is implied.

A `Valuation` retains the applied `SimpleComponentVersion`: configured Exchange Rates retain the selected version,
manual overrides receive a one-off version, same-currency conversions use identity, and stable implicit versions back
an Exchange Rate of one. For configured Exchange Rates, the selected `ComponentVersionId` is stable across Valuations;
calculator instances and their identifiers created during valuation are transient. Full `SimpleComponentVersion`
equality across independent Valuations is not promised.

`ExpenseRecorded` stores the applied `ComponentVersionId`, Exchange Rate, converted amount, and resolved Shares.
Consequently, later Exchange Rate configuration or Valuation logic changes cannot alter replayed history.
