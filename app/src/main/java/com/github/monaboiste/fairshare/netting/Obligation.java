package com.github.monaboiste.fairshare.netting;

import com.github.monaboiste.fairshare.pricing.component.Validity;
import com.github.monaboiste.fairshare.quantity.money.Money;

/**
 * A directional monetary amount owed by one participant to another, valid over a period.
 *
 * <p>The {@link Validity} lets balances be computed as of a point in time: an obligation contributes only while it is
 * valid. Use {@link #of(Object, Object, Money)} for an obligation that is always valid.
 *
 * @param from participant who owes
 * @param to participant who is owed
 * @param amount amount owed (non-negative; loops and zeros are netted away when balances are computed)
 * @param validity period during which the obligation applies
 * @param <P> participant identity type
 */
public record Obligation<P>(P from, P to, Money amount, Validity validity) {

    public Obligation {
        if (amount.isNegative()) {
            throw new IllegalArgumentException("Obligation amount must not be negative");
        }
    }

    /** Creates an always-valid obligation. */
    public static <P> Obligation<P> of(P from, P to, Money amount) {
        return new Obligation<>(from, to, amount, Validity.always());
    }
}
