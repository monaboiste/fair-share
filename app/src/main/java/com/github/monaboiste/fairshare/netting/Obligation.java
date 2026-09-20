package com.github.monaboiste.fairshare.netting;

import com.github.monaboiste.fairshare.quantity.money.Money;
import java.util.Objects;

/** A directional monetary amount owed by one Participant to another. */
public record Obligation<P>(P from, P to, Money amount) {

    public Obligation {
        Objects.requireNonNull(from, "Obligation sender is required");
        Objects.requireNonNull(to, "Obligation recipient is required");
        Objects.requireNonNull(amount, "Obligation amount is required");
    }
}
