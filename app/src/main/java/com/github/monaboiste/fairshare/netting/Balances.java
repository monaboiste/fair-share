package com.github.monaboiste.fairshare.netting;

import com.github.monaboiste.fairshare.pricing.calculation.Calculators;
import com.github.monaboiste.fairshare.pricing.calculation.ParameterKey;
import com.github.monaboiste.fairshare.pricing.calculation.Parameters;
import com.github.monaboiste.fairshare.pricing.calculation.TotalPrice;
import com.github.monaboiste.fairshare.pricing.component.Component;
import com.github.monaboiste.fairshare.pricing.component.ComponentBreakdown;
import com.github.monaboiste.fairshare.quantity.money.Money;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-participant net balances (incoming minus outgoing) computed through the pricing archetype.
 *
 * <p>Each participant's balance is a composite pricing {@link Component} whose children are the obligations touching
 * that participant - positive for incoming, negative for outgoing - so the same machinery yields both the balance
 * amount and an explainable {@link ComponentBreakdown}. Only obligations valid at the as-of time contribute, giving
 * balance history and {@link #simulate(List) simulation} across time. Parallel, opposite, loop and zero obligations net
 * away in the signed sum, so no separate normalization step is required.
 *
 * <p>Validity is filtered here rather than delegated to the component's version selection: the archetype returns a
 * PLN-denominated zero for a non-applicable version, which would corrupt a non-PLN settlement sum.
 */
final class Balances<P> {

    private static final ParameterKey<LocalDateTime> TIMESTAMP = new ParameterKey<>("timestamp", LocalDateTime.class);

    private final ObligationGraph<P> graph;
    private final Map<P, Money> amounts;
    private final Map<P, ComponentBreakdown> breakdowns;

    private Balances(ObligationGraph<P> graph, Map<P, Money> amounts, Map<P, ComponentBreakdown> breakdowns) {
        this.graph = graph;
        this.amounts = amounts;
        this.breakdowns = breakdowns;
    }

    /** Computes balances from the obligations valid at the given time. */
    static <P> Balances<P> of(ObligationGraph<P> graph, LocalDateTime asOf) {
        String code = graph.currency().getCurrencyCode();
        List<Obligation<P>> valid = graph.obligations().stream()
                .filter(obligation -> obligation.validity().isValidAt(asOf))
                .toList();
        Parameters at = Parameters.of(TIMESTAMP, asOf);

        Map<P, Money> amounts = new LinkedHashMap<>();
        Map<P, ComponentBreakdown> breakdowns = new LinkedHashMap<>();
        for (P participant : graph.participants()) {
            List<Component> contributions = new ArrayList<>();
            for (Obligation<P> obligation : valid) {
                if (obligation.to().equals(participant)) {
                    contributions.add(contribution(obligation, obligation.amount()));
                }
                if (obligation.from().equals(participant)) {
                    contributions.add(
                            contribution(obligation, obligation.amount().negate()));
                }
            }
            String name = "balance:" + participant;
            if (contributions.isEmpty()) {
                amounts.put(participant, Money.zero(code));
                breakdowns.put(participant, new ComponentBreakdown(name, new TotalPrice(Money.zero(code))));
            } else {
                ComponentBreakdown breakdown = Component.composite(name, contributions.toArray(new Component[0]))
                        .calculateBreakdown(at);
                amounts.put(participant, breakdown.total());
                breakdowns.put(participant, breakdown);
            }
        }
        return new Balances<>(graph, amounts, breakdowns);
    }

    private static <P> Component contribution(Obligation<P> obligation, Money signedAmount) {
        String name = obligation.from() + "->" + obligation.to();
        return Component.simple(name, Calculators.fixed(name, signedAmount));
    }

    /** Signed balance of every participant, positive for a creditor and negative for a debtor. */
    Map<P, Money> amounts() {
        return Map.copyOf(amounts);
    }

    /** Participants who owe money, mapped to the positive amount they owe. */
    Map<P, Money> debtors() {
        Map<P, Money> debtors = new LinkedHashMap<>();
        amounts.forEach((participant, balance) -> {
            if (balance.isNegative()) {
                debtors.put(participant, balance.abs());
            }
        });
        return debtors;
    }

    /** Participants who are owed money, mapped to the positive amount owed to them. */
    Map<P, Money> creditors() {
        Map<P, Money> creditors = new LinkedHashMap<>();
        amounts.forEach((participant, balance) -> {
            if (!balance.isZero() && !balance.isNegative()) {
                creditors.put(participant, balance);
            }
        });
        return creditors;
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
        for (LocalDateTime time : times) {
            simulated.put(time, Balances.of(graph, time));
        }
        return simulated;
    }
}
