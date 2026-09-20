package com.github.monaboiste.fairshare.netting;

import java.util.List;
import java.util.Set;
import javax.money.CurrencyUnit;

/** Participants and their Money-weighted obligations in one settlement currency. */
public record ObligationGraph<P>(Set<P> participants, List<Obligation<P>> obligations, CurrencyUnit currency) {

    public ObligationGraph {
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
