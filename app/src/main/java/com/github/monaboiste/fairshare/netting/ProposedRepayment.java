package com.github.monaboiste.fairshare.netting;

import com.github.monaboiste.fairshare.quantity.money.Money;

/**
 * A derived instruction: which participant repays another, and how much.
 *
 * @param debtor participant who pays
 * @param creditor participant who receives (must differ from the debtor)
 * @param amount positive amount to pay
 * @param <P> participant identity type
 */
public record ProposedRepayment<P>(P debtor, P creditor, Money amount) {

    public ProposedRepayment {
        if (debtor.equals(creditor)) {
            throw new IllegalArgumentException("Proposed repayment cannot be self-directed");
        }
        if (amount.isZero() || amount.isNegative()) {
            throw new IllegalArgumentException("Proposed repayment must be positive");
        }
    }
}
