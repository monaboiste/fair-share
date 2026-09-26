package com.github.monaboiste.fairshare.netting;

import com.github.monaboiste.fairshare.graphs.Edge;
import com.github.monaboiste.fairshare.graphs.Graph;
import com.github.monaboiste.fairshare.graphs.Node;
import com.github.monaboiste.fairshare.quantity.money.Money;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.money.CurrencyUnit;

/**
 * Participants and the obligations owed between them, in one settlement currency.
 *
 * <p>Participants are vertices and obligations are directed, {@code Money}-weighted edges of a graph that is the single
 * source of truth. The graph is never exposed - only participants, obligations, and the currency cross the boundary.
 */
public final class Obligations<P> {

    private final Graph<P, Obligation<P>> graph;
    private final CurrencyUnit currency;

    /**
     * Creates obligations from a participant roster, the obligations between them, and the settlement currency.
     *
     * @throws IllegalArgumentException if an obligation references a participant outside the roster or uses another
     *     currency
     */
    private Obligations(Set<P> participants, List<Obligation<P>> obligations, CurrencyUnit currency) {
        Set<P> roster = Set.copyOf(participants);
        Graph<P, Obligation<P>> createdGraph = new Graph<>();

        roster.forEach(participant -> createdGraph.addVertex(new Node<>(participant)));

        for (Obligation<P> obligation : obligations) {
            if (!roster.contains(obligation.from()) || !roster.contains(obligation.to())) {
                throw new IllegalArgumentException("Obligation references an unknown participant");
            }
            if (!obligation.amount().currencyUnit().equals(currency)) {
                throw new IllegalArgumentException("All obligations must use " + currency.getCurrencyCode());
            }

            createdGraph.addEdge(new Edge<>(new Node<>(obligation.from()), new Node<>(obligation.to()), obligation));
        }

        this.graph = createdGraph;
        this.currency = currency;
    }

    public static <P> Obligations<P> of(Set<P> participants, List<Obligation<P>> obligations, CurrencyUnit currency) {
        return new Obligations<>(participants, obligations, currency);
    }

    public Set<P> participants() {
        return graph.vertices().stream().map(Node::property).collect(Collectors.toUnmodifiableSet());
    }

    public List<Obligation<P>> obligations() {
        return graph.edges().stream().map(Edge::property).toList();
    }

    /** Signed balances of every participant, including zero-balance participants, in the settlement currency. */
    public Map<P, Money> signedBalances() {
        return Balances.of(this).amounts();
    }

    public CurrencyUnit currency() {
        return currency;
    }
}
