package com.github.monaboiste.fairshare.netting;

import com.github.monaboiste.fairshare.quantity.money.Money;

/**
 * An Obligation has no validity period; it represents an obligation in the current Settlement state. Its lifetime
 * follows the source Expense or Repayment through Settlement events. Exchange Rate validity is a separate Valuation
 * concern.
 *
 * <p>The negative-amount check rejects invalid input; loops and zero amounts net away when balances are computed.
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
