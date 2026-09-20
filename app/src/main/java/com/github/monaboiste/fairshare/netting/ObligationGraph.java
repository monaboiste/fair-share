package com.github.monaboiste.fairshare.netting;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.money.CurrencyUnit;

/** Participants and their Money-weighted Obligations in one currency. */
public record ObligationGraph<P>(Set<P> participants, List<Obligation<P>> obligations, CurrencyUnit currency) {

    public ObligationGraph {
        Objects.requireNonNull(participants, "Obligation graph participants are required");
        Objects.requireNonNull(obligations, "Obligation graph obligations are required");
        Objects.requireNonNull(currency, "Obligation graph currency is required");
        participants = Set.copyOf(participants);
        obligations = List.copyOf(obligations);
        for (Obligation<P> obligation : obligations) {
            if (!participants.contains(obligation.from()) || !participants.contains(obligation.to())) {
                throw new IllegalArgumentException("Obligation references an unknown participant");
            }
            if (!obligation.amount().currencyUnit().equals(currency)) {
                throw new IllegalArgumentException("All obligations must use " + currency.getCurrencyCode());
            }
        }
    }

    public static <P> ObligationGraph<P> of(
            Set<P> participants, List<Obligation<P>> obligations, CurrencyUnit currency) {
        return new ObligationGraph<>(participants, obligations, currency);
    }
}
