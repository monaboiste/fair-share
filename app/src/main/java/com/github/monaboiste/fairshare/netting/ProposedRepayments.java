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

    private ProposedRepayments(Graph<P, ProposedRepayment<P>> graph, CurrencyUnit currency) {
        this.graph = graph;
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
        Set<P> roster = Set.copyOf(participants);
        Graph<P, ProposedRepayment<P>> graph = new Graph<>();
        roster.forEach(participant -> graph.addVertex(new Node<>(participant)));
        for (ProposedRepayment<P> repayment : repayments) {
            if (!roster.contains(repayment.debtor()) || !roster.contains(repayment.creditor())) {
                throw new IllegalArgumentException("Proposed repayment references an unknown participant");
            }
            if (!repayment.amount().currencyUnit().equals(currency)) {
                throw new IllegalArgumentException("All repayments must use " + currency.getCurrencyCode());
            }
            Node<P> debtor = new Node<>(repayment.debtor());
            Node<P> creditor = new Node<>(repayment.creditor());
            if (graph.hasEdge(debtor, creditor)) {
                throw new IllegalArgumentException("Proposed repayments must not contain parallel edges");
            }
            graph.addEdge(new Edge<>(debtor, creditor, repayment));
        }
        if (graph.findFirstCycle().isPresent()) {
            throw new IllegalArgumentException("Proposed repayments must not contain cycles");
        }
        return new ProposedRepayments<>(graph, currency);
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
