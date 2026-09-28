# Version and freeze Expense Valuations

Settlement owns versioned Exchange Rate configuration as `ExchangeRateVersion`: version identifier, Exchange Rate,
Validity and `definedAt`. Events and views retain this data without storing Pricing calculators. `ValuationEngine`
selects the applicable version for the Expense date and converts it to `SimpleComponentVersion` at its own seam.
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
manual overrides receive a one-off version, and same-currency conversions use a stable implicit version backed by an
Exchange Rate of one. For configured Exchange Rates, the selected `ComponentVersionId` is stable across Valuations;
calculator instances and their identifiers created during valuation are transient. Full `SimpleComponentVersion`
equality across independent Valuations is not promised.

`ExpenseRecorded` stores the applied `ComponentVersionId`, Exchange Rate, converted amount, and resolved Shares.
Consequently, later Exchange Rate configuration or Valuation logic changes cannot alter replayed history.
