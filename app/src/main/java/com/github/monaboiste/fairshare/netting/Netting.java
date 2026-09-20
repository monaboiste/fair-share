package com.github.monaboiste.fairshare.netting;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** Derives a valid, deterministic Proposed Repayment Graph from an Obligation Graph. */
public interface Netting {

    /**
     * Nets the obligations valid at the given time into proposed repayments.
     *
     * @param graph obligations between participants in one settlement currency
     * @param order total order over participants for deterministic output
     * @param asOf point in time; only obligations valid then contribute
     * @param <P> participant identity type
     * @return a proposed repayment graph preserving every participant balance, with no loops, zero edges, parallel
     *     edges, or cycles, and at most {@code max(0, unbalanced - 1)} edges
     */
    <P> ProposedRepaymentGraph<P> net(
            ObligationGraph<P> graph, ParticipantComparator<? super P> order, LocalDateTime asOf);

    /** Nets the obligations valid now. */
    default <P> ProposedRepaymentGraph<P> net(ObligationGraph<P> graph, ParticipantComparator<? super P> order) {
        return net(graph, order, LocalDateTime.now(ZoneId.systemDefault()));
    }

    static Netting greedy() {
        return new GreedyNetting();
    }
}
