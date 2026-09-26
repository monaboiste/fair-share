package com.github.monaboiste.fairshare.netting;

import com.github.monaboiste.fairshare.pricing.calculation.Calculators;
import com.github.monaboiste.fairshare.pricing.calculation.Parameters;
import com.github.monaboiste.fairshare.pricing.calculation.PricingContext;
import com.github.monaboiste.fairshare.pricing.component.ApplicabilityConstraint;
import com.github.monaboiste.fairshare.pricing.component.Component;
import com.github.monaboiste.fairshare.pricing.component.ComponentBreakdown;
import com.github.monaboiste.fairshare.quantity.money.Money;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Per-participant net balances (incoming minus outgoing) computed through the pricing archetype.
 *
 * <p>Each participant's balance is a composite pricing {@link Component} whose children are the obligations touching
 * that participant - positive for incoming, negative for outgoing - so the same machinery yields both the balance
 * amount and an explainable {@link ComponentBreakdown}. The evaluation runs in the settlement currency at the as-of
 * time; an obligation applies only while it is valid and otherwise contributes zero, giving balance history and
 * {@link #simulate(List) simulation} across time. Parallel, opposite, loop and zero obligations net away in the signed
 * sum, so no separate normalization step is required.
 */
final class Balances<P> {

    private final Obligations<P> obligations;
    private final Map<P, Money> amounts;
    private final Map<P, ComponentBreakdown> breakdowns;

    /** Computes balances from the obligations valid at the given time. */
    private Balances(Obligations<P> obligations, LocalDateTime asOf) {
        this(
                obligations,
                Parameters.of(PricingContext.TIMESTAMP, asOf).with(PricingContext.CURRENCY, obligations.currency()),
                obligation -> ApplicabilityConstraint.validAt(obligation.validity()));
    }

    private Balances(
            Obligations<P> obligations, Parameters at, Function<Obligation<P>, ApplicabilityConstraint> applicability) {
        this.obligations = obligations;

        Map<P, Money> computedAmounts = new LinkedHashMap<>();
        Map<P, ComponentBreakdown> computedBreakdowns = new LinkedHashMap<>();

        for (P participant : obligations.participants()) {
            List<Component> contributions = new ArrayList<>();

            for (Obligation<P> obligation : obligations.obligations()) {
                if (obligation.to().equals(participant)) {
                    contributions.add(contribution(obligation, obligation.amount(), applicability.apply(obligation)));
                }
                if (obligation.from().equals(participant)) {
                    contributions.add(
                            contribution(obligation, obligation.amount().negate(), applicability.apply(obligation)));
                }
            }

            ComponentBreakdown breakdown = Component.composite(
                            "balance:" + participant, contributions.toArray(Component[]::new))
                    .calculateBreakdown(at);
            computedAmounts.put(participant, breakdown.total());
            computedBreakdowns.put(participant, breakdown);
        }

        this.amounts = Map.copyOf(computedAmounts);
        this.breakdowns = Map.copyOf(computedBreakdowns);
    }

    static <P> Balances<P> of(Obligations<P> obligations, LocalDateTime asOf) {
        return new Balances<>(obligations, asOf);
    }

    static <P> Balances<P> timeless(Obligations<P> obligations) {
        return new Balances<>(
                obligations,
                Parameters.of(PricingContext.CURRENCY, obligations.currency()),
                _ -> ApplicabilityConstraint.alwaysTrue());
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

    /** Explains a participant's balance as the tree of contributing obligations. */
    ComponentBreakdown breakdown(P participant) {
        ComponentBreakdown breakdown = breakdowns.get(participant);
        if (breakdown == null) {
            throw new IllegalArgumentException("Unknown participant: " + participant);
        }
        return breakdown;
    }

    /** Recomputes balances at each given time, for history or what-if scenarios. */
    Map<LocalDateTime, Balances<P>> simulate(List<LocalDateTime> times) {
        Map<LocalDateTime, Balances<P>> simulated = new LinkedHashMap<>();
        times.forEach(time -> simulated.put(time, new Balances<>(obligations, time)));
        return simulated;
    }

    private static <P> Component contribution(
            Obligation<P> obligation, Money signedAmount, ApplicabilityConstraint applicability) {
        String name = obligation.from() + "->" + obligation.to();
        return Component.simple(name, Calculators.fixed(name, signedAmount), applicability);
    }
}
