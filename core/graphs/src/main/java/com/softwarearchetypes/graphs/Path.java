package com.softwarearchetypes.graphs;

import java.util.List;
import java.util.stream.Collectors;

/** An ordered sequence of directed edges. */
public record Path<T, P>(List<Edge<T, P>> edges) {
    @Override
    public String toString() {
        if (edges.isEmpty()) {
            return "-";
        }
        return edges.stream()
                .map(edge -> edge.to().toString())
                .collect(Collectors.joining(" -> ", edges.getFirst().from() + " -> ", ""));
    }
}
