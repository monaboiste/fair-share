# Fair Share

Fair Share records shared expenses and derives how participants can settle their balances with a small number of
transfers.

## Language

**Settlement**: A named, bounded expense-sharing arrangement with its own Participants, immutable Settlement Currency,
Expenses, Repayments, and lifecycle. _Avoid_: Group, trip, event

**Participant**: A person included in a Settlement who can pay an Expense, receive a Share, or make a Repayment.
Identity is independent of the Participant's non-unique display name. A Participant identifier is permanently bound to
its original add data and is never reused or reactivated. _Avoid_: User, member, friend

**Expense**: A described outlay incurred on a specified date, paid by exactly one Participant, and allocated among one
or more Participants. The payer does not need to receive a Share. _Avoid_: Payment, transaction, bill

**Share Allocation**:
The equal, exact-amount, or weighted definition used to divide an Expense into Shares.
_Avoid_: Percentage split, strategy

**Share**:
The part of an Expense assigned to one Participant.
_Avoid_: Split, contribution

**Repayment**: A transfer made on a specified date in the Settlement Currency from one Participant to another that
reduces their outstanding balances. _Avoid_: Expense, payment

**Cancellation**: An irreversible mark on an Expense or Repayment that preserves its history and Participant references
but excludes it from current Obligations and Balances. A correction is a separate entry with a new identifier.
_Avoid_: Deletion, edit, refund

**Settlement Currency**:
The currency in which a Settlement expresses balances and proposed repayments.
_Avoid_: Base currency, group currency

**Exchange Rate**: A directional conversion relationship expressed as one unit of an Expense currency multiplied into
the Settlement Currency. A configured Exchange Rate applies during a validity period, while an Expense retains the rate
used for its Valuation. _Avoid_: Rate, price, currency pair

**Valuation**: The conversion of an Expense into the Settlement Currency using the applicable versioned Exchange Rate or
an Exchange Rate Override. _Avoid_: Price, currency conversion

**Exchange Rate Override**: A manual Exchange Rate scoped to one Expense. It takes precedence over configured Exchange
Rates for that Expense and does not create or change configured Exchange Rate versions. _Avoid_: Custom rate, rate
correction

**Obligation**:
A directional monetary amount owed by one Participant to another within a Settlement.
_Avoid_: Balance, repayment

**Balance**: The net amount a Participant owes or is owed within a Settlement, derived from Obligations and Repayments.
A positive Balance is receivable by the Participant; a negative Balance is payable. _Avoid_: Debt

**Proposed Repayment**: A derived instruction identifying which Participant should repay another Participant, and how
much, to settle their balances. _Avoid_: Repayment, debt

**Netting**:
The derivation of a valid, deterministic set of Proposed Repayments by matching debtors with creditors.
_Avoid_: Settlement, optimization

**Open Settlement**:
A Settlement that accepts changes to its Participants, Exchange Rates, Expenses, and Repayments.
_Avoid_: Draft, active

**Closed Settlement**:
A Settlement whose Participants all have exactly zero Balances and whose financial and membership changes require
explicit reopening. Settlement and Participant names can still be corrected while closed.
_Avoid_: Archived, deleted
