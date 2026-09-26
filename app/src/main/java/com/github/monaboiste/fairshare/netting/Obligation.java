package com.github.monaboiste.fairshare.netting;

import com.github.monaboiste.fairshare.quantity.money.Money;

/**
 * A directional monetary amount owed by one participant to another.
 *
 * <p>An Obligation has no validity period; it represents an obligation in the current Settlement state. Its lifetime
 * follows the source Expense or Repayment through Settlement events. Exchange Rate validity is a separate Valuation
 * concern.
 *
 * @param from participant who owes
 * @param to participant who is owed
 * @param amount amount owed (non-negative; loops and zeros are netted away when balances are computed)
 * @param <P> participant identity type
 */
public record Obligation<P>(P from, P to, Money amount) {

    public Obligation {
        if (amount.isNegative()) {
            throw new IllegalArgumentException("Obligation amount must not be negative");
        }
    }
}
