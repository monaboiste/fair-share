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
final class Balances<P> {

    private final Map<P, Money> amounts;

    private Balances(Obligations<P> obligations) {
        Map<P, Money> computedAmounts = new LinkedHashMap<>();

        for (P participant : obligations.participants()) {
            Money balance = moneyZero(obligations);

            for (Obligation<P> obligation : obligations.obligations()) {
                if (obligation.to().equals(participant)) {
                    balance = balance.add(obligation.amount());
                }
                if (obligation.from().equals(participant)) {
                    balance = balance.subtract(obligation.amount());
                }
            }

            computedAmounts.put(participant, balance);
        }

        this.amounts = Map.copyOf(computedAmounts);
    }

    static <P> Balances<P> of(Obligations<P> obligations) {
        return new Balances<>(obligations);
    }

    /** Signed balance of every participant, positive for a creditor and negative for a debtor. */
    Map<P, Money> amounts() {
        return amounts;
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

    private static <P> Money moneyZero(Obligations<P> obligations) {
        return Money.zero(obligations.currency());
    }
}
