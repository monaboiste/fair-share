package com.github.monaboiste.fairshare.netting;

import com.github.monaboiste.fairshare.quantity.money.Money;
import java.util.Objects;

/** A derived instruction identifying which Participant repays another Participant, and how much. */
public record ProposedRepayment<P>(P debtor, P creditor, Money amount) {

    public ProposedRepayment {
        Objects.requireNonNull(debtor, "Proposed repayment debtor is required");
        Objects.requireNonNull(creditor, "Proposed repayment creditor is required");
        Objects.requireNonNull(amount, "Proposed repayment amount is required");
        if (debtor.equals(creditor)) {
            throw new IllegalArgumentException("Proposed repayment cannot be self-directed");
        }
        if (amount.isZero() || amount.isNegative()) {
            throw new IllegalArgumentException("Proposed repayment must be positive");
        }
    }
}
