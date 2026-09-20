package com.github.monaboiste.fairshare.netting;

import java.util.Comparator;

/** Derives a valid, deterministic Proposed Repayment Graph from an Obligation Graph. */
public interface Netting {

    <P> ProposedRepaymentGraph<P> net(ObligationGraph<P> graph, Comparator<P> participantOrder);

    static Netting greedy() {
        return new GreedyNetting();
    }
}
