# Version and freeze Expense Valuations

Settlement owns versioned Exchange Rate configuration and selects the applicable Pricing `SimpleComponentVersion` from
the Expense date, while `CompositeComponentVersion` remains reserved for genuinely composite Valuations. Rate versions
use Pricing validity periods, allow intentional overlaps, and resolve them by latest `validFrom` followed by stream
order. A Valuation composes the applied `SimpleComponentVersion`: configured rates retain the selected version, manual
overrides receive a one-off version, and same-currency conversions use a stable implicit version with an Exchange Rate
of one. `ExpenseRecorded` stores the required `ComponentVersionId`, applied rate, converted amount, and resolved Shares,
so later Pricing changes cannot alter replayed history.
