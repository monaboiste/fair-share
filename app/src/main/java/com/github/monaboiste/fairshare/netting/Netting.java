package com.github.monaboiste.fairshare.netting;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** Derives a valid, deterministic set of proposed repayments from a set of obligations. */
public interface Netting {

    /**
     * Nets the obligations valid at the given time into proposed repayments.
     *
     * @param obligations obligations between participants in one settlement currency
     * @param order total order over participants for deterministic output
     * @param asOf point in time; only obligations valid then contribute
     * @param <P> participant identity type
     * @return proposed repayments preserving every participant balance, with no loops, zero edges, parallel edges, or
     *     cycles, and at most {@code max(0, unbalanced - 1)} edges
     */
    <P> ProposedRepayments<P> net(
            Obligations<P> obligations, ParticipantComparator<? super P> order, LocalDateTime asOf);

    /** Nets the obligations valid now. */
    default <P> ProposedRepayments<P> net(Obligations<P> obligations, ParticipantComparator<? super P> order) {
        return net(obligations, order, LocalDateTime.now(ZoneId.systemDefault()));
    }

    static Netting greedy() {
        return new GreedyNetting();
    }
}
