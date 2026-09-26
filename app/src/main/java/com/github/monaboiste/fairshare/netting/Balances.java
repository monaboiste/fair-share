package com.github.monaboiste.fairshare.netting;

import com.github.monaboiste.fairshare.quantity.money.Money;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-participant net balances (incoming minus outgoing) over the current Settlement state.
 *
 * <p>Each participant's balance is a signed {@link Money} sum: positive for incoming obligations, negative for outgoing
 * ones. Parallel, opposite, loop and zero contributions net away in the signed sum, so no separate normalization step
 * is required. Every roster participant appears in the result, including uninvolved participants with a zero balance in
 * the settlement currency.
 */
record Balances<P>(Map<P, Money> amounts) {

    Balances {
        amounts = Map.copyOf(amounts);
    }

    static <P> Balances<P> of(Obligations<P> obligations) {
        return new Balances<>(computeAmounts(obligations));
    }

    /** Participants who owe money, mapped to the positive amount they owe. */
    Map<P, Money> debtors() {
        Map<P, Money> debtors = new LinkedHashMap<>();
        amounts.forEach((participant, balance) -> {
            if (balance.isNegative()) {
                debtors.put(participant, balance.abs());
            }
        });
        return Map.copyOf(debtors);
    }

    /** Participants who are owed money, mapped to the positive amount owed to them. */
    Map<P, Money> creditors() {
        Map<P, Money> creditors = new LinkedHashMap<>();
        amounts.forEach((participant, balance) -> {
            if (!balance.isZero() && !balance.isNegative()) {
                creditors.put(participant, balance);
            }
        });
        return Map.copyOf(creditors);
    }

    private static <P> Map<P, Money> computeAmounts(Obligations<P> obligations) {
        Map<P, Money> amounts = new LinkedHashMap<>();

        for (P participant : obligations.participants()) {
            amounts.put(participant, Money.zero(obligations.currency()));
        }

        for (Obligation<P> obligation : obligations.obligations()) {
            amounts.merge(obligation.to(), obligation.amount(), Money::add);
            amounts.merge(obligation.from(), obligation.amount().negate(), Money::add);
        }

        return amounts;
    }
}
