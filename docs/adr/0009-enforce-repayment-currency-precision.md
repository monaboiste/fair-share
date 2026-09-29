# Enforce Settlement Currency precision for Repayments

A Repayment records an actual transfer, so its amount must be exactly representable in the Settlement Currency's
minor units. Reject amounts whose decimal scale, after removing trailing zeros, exceeds the currency's default
fraction digits with `RepaymentAmountPrecisionExceeded`; never round the submitted amount silently, because that
would record a different transfer and alter Balances.

For example, EUR `10.005` is rejected while EUR `10.0000` is accepted. JPY permits whole units and KWD permits three
fraction digits. This invariant applies to Repayments; it does not change Expense Valuation or Share Allocation rules.

Command handling, aggregate replay, and projection replay enforce the same amount invariants: positive amount,
Settlement Currency, and permitted precision. Invalid persisted amounts fail replay explicitly rather than producing
Balances from an impossible Repayment. Changing this rule would affect which historical events are valid.
