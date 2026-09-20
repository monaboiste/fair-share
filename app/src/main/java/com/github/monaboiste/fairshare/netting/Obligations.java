package com.github.monaboiste.fairshare.netting;

import com.github.monaboiste.fairshare.graphs.Edge;
import com.github.monaboiste.fairshare.graphs.Graph;
import com.github.monaboiste.fairshare.graphs.Node;
import java.util.List;
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

    private Obligations(Graph<P, Obligation<P>> graph, CurrencyUnit currency) {
        this.graph = graph;
        this.currency = currency;
    }

    /**
     * Creates obligations from a participant roster, the obligations between them, and the settlement currency.
     *
     * @throws IllegalArgumentException if an obligation references a participant outside the roster or uses another
     *     currency
     */
    public static <P> Obligations<P> of(Set<P> participants, List<Obligation<P>> obligations, CurrencyUnit currency) {
        Set<P> roster = Set.copyOf(participants);
        Graph<P, Obligation<P>> graph = new Graph<>();
        roster.forEach(participant -> graph.addVertex(new Node<>(participant)));
        for (Obligation<P> obligation : obligations) {
            if (!roster.contains(obligation.from()) || !roster.contains(obligation.to())) {
                throw new IllegalArgumentException("Obligation references an unknown participant");
            }
            if (!obligation.amount().currencyUnit().equals(currency)) {
                throw new IllegalArgumentException("All obligations must use " + currency.getCurrencyCode());
            }
            graph.addEdge(new Edge<>(new Node<>(obligation.from()), new Node<>(obligation.to()), obligation));
        }
        return new Obligations<>(graph, currency);
    }

    public Set<P> participants() {
        return graph.vertices().stream().map(Node::property).collect(Collectors.toUnmodifiableSet());
    }

    public List<Obligation<P>> obligations() {
        return graph.edges().stream().map(Edge::property).toList();
    }

    public CurrencyUnit currency() {
        return currency;
    }
}
