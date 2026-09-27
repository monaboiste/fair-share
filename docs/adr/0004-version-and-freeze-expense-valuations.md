# Version and freeze Expense Valuations

Settlement owns versioned Exchange Rate configuration, while `ValuationEngine` selects the applicable
`SimpleComponentVersion` for the Expense date. `CompositeComponentVersion` remains reserved for genuinely composite
Valuations.

Exchange Rate versions use the existing component validity model, allow intentional overlaps, and resolve them by the
latest `validFrom`, followed by stream order. Expense `incurredOn` is a `LocalDate` and is evaluated at
`incurredOn.atStartOfDay()` against the inclusive `LocalDateTime` validity endpoints. An Exchange Rate
starting at noon does not apply to an Expense incurred on that same date; no time zone or whole-day
normalization is implied.

A `Valuation` retains the applied `SimpleComponentVersion`: configured Exchange Rates retain the selected version,
manual overrides receive a one-off version, and same-currency conversions use a stable implicit version backed by an
Exchange Rate of one.

`ExpenseRecorded` stores the applied `ComponentVersionId`, Exchange Rate, converted amount, and resolved Shares.
Consequently, later Exchange Rate configuration or Valuation logic changes cannot alter replayed history.
