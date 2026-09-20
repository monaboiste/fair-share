package com.github.monaboiste.fairshare.netting;

import com.github.monaboiste.fairshare.graphs.Edge;
import com.github.monaboiste.fairshare.graphs.Graph;
import com.github.monaboiste.fairshare.graphs.Node;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.money.CurrencyUnit;

/**
 * Participants and the positive debtor-to-creditor proposed repayments between them, in one settlement currency.
 *
 * <p>Backed by a graph that enforces the output invariants independently of the algorithm that produced it: no parallel
 * edges (checked with {@link Graph#hasEdge}) and no cycles of any length (checked with {@link Graph#findFirstCycle}).
 * The set of proposed repayments is unordered.
 */
public final class ProposedRepayments<P> {

    private final Graph<P, ProposedRepayment<P>> graph;
    private final CurrencyUnit currency;

    /**
     * Creates proposed repayments from a participant roster, the repayments between them, and the settlement currency.
     *
     * @throws IllegalArgumentException if a repayment references a participant outside the roster, uses another
     *     currency, duplicates a debtor-to-creditor pair, or forms a cycle
     */
    private ProposedRepayments(
            Set<P> participants, List<ProposedRepayment<P>> proposedRepayments, CurrencyUnit currency) {
        Set<P> roster = Set.copyOf(participants);
        Graph<P, ProposedRepayment<P>> createdGraph = new Graph<>();

        roster.forEach(participant -> createdGraph.addVertex(new Node<>(participant)));

        for (ProposedRepayment<P> repayment : proposedRepayments) {
            if (!roster.contains(repayment.debtor()) || !roster.contains(repayment.creditor())) {
                throw new IllegalArgumentException("Proposed repayment references an unknown participant");
            }
            if (!repayment.amount().currencyUnit().equals(currency)) {
                throw new IllegalArgumentException("All proposedRepayments must use " + currency.getCurrencyCode());
            }

            Node<P> debtor = new Node<>(repayment.debtor());
            Node<P> creditor = new Node<>(repayment.creditor());

            if (createdGraph.hasEdge(debtor, creditor)) {
                throw new IllegalArgumentException("Proposed proposedRepayments must not contain parallel edges");
            }

            createdGraph.addEdge(new Edge<>(debtor, creditor, repayment));
        }

        if (createdGraph.findFirstCycle().isPresent()) {
            throw new IllegalArgumentException("Proposed proposedRepayments must not contain cycles");
        }

        this.graph = createdGraph;
        this.currency = currency;
    }

    /**
     * Creates proposed repayments from a participant roster, the repayments between them, and the settlement currency.
     *
     * @throws IllegalArgumentException if a repayment references a participant outside the roster, uses another
     *     currency, duplicates a debtor-to-creditor pair, or forms a cycle
     */
    public static <P> ProposedRepayments<P> of(
            Set<P> participants, List<ProposedRepayment<P>> repayments, CurrencyUnit currency) {
        return new ProposedRepayments<>(participants, repayments, currency);
    }

    public Set<P> participants() {
        return graph.vertices().stream().map(Node::property).collect(Collectors.toUnmodifiableSet());
    }

    public List<ProposedRepayment<P>> proposedRepayments() {
        return graph.edges().stream().map(Edge::property).toList();
    }

    public CurrencyUnit currency() {
        return currency;
    }
}
