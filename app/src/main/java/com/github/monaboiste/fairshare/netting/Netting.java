package com.github.monaboiste.fairshare.netting;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 *
 *
 * <pre>
 * Obligations:
 *
 *             Obligation(PLN 30)
 *       Ada ─────────────────────▶ Bob
 *        ▲                          │
 *        │                          │
 * Obligation(PLN 5)         Obligation(PLN 10)
 *        │                          │
 *        │                          ▼
 *       Dan ◀───────────────────── Cid
 *             Obligation(PLN 5)
 *
 * Balances:
 *
 *   Ada PLN -25    Bob PLN 20    Cid PLN 5    Dan PLN 0
 *
 * Proposed repayments:
 *
 *          ProposedRepayment(PLN 20)
 *       Ada ─────────────────────────▶ Bob
 *        │
 *        │ ProposedRepayment(PLN 5)
 *        └───────────────────────────▶ Cid
 * </pre>
 *
 * <p>The original four-obligation cycle is reduced to two acyclic proposed repayments while preserving every
 * participant's balance.
 */
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
