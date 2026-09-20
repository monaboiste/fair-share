package com.github.monaboiste.fairshare.graphs.fixture.cycles

import com.github.monaboiste.fairshare.graphs.Edge
import com.github.monaboiste.fairshare.graphs.Graph
import com.github.monaboiste.fairshare.graphs.Node

class Eligibility {

    private final Graph<OwnerId, EligibilityEdge> graph = new Graph<>()

    void markTransferEligible(OwnerId from, OwnerId to) {
        graph.addEdge(new Edge<>(new Node<>(from), new Node<>(to), EligibilityEdge.INSTANCE))
    }

    void markTransferIneligible(OwnerId from, OwnerId to) {
        graph.removeEdge(new Edge<>(new Node<>(from), new Node<>(to), EligibilityEdge.INSTANCE))
    }

    boolean isTransferEligible(OwnerId from, OwnerId to) {
        return graph.hasEdge(new Node<>(from), new Node<>(to))
    }

    Graph<OwnerId, EligibilityEdge> asGraph() {
        return graph
    }

    enum EligibilityEdge {
        INSTANCE
    }
}
