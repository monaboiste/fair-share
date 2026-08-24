# Version and freeze Expense Valuations

Settlement owns versioned Exchange Rate configuration and selects the applicable Pricing `SimpleComponentVersion` from
the Expense date, while `CompositeComponentVersion` remains reserved for genuinely composite Valuations. Rate versions
use Pricing validity periods, allow intentional overlaps, and resolve them by latest `validFrom` followed by stream
order. `ExpenseRecorded` stores the selected `ComponentVersionId` or manual override together with the applied rate,
converted amount, and resolved Shares, so later Pricing changes cannot alter replayed history.
