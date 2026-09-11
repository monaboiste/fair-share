package com.softwarearchetypes.graphs.fixture.cycles

import com.softwarearchetypes.graphs.Edge
import com.softwarearchetypes.graphs.Graph
import com.softwarearchetypes.graphs.Node

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
