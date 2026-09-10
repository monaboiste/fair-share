package com.softwarearchetypes.graphs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** A directed graph supporting cycle detection and edge-set intersection. */
public class Graph<T, P> {

    private final Map<Node<T>, List<Edge<T, P>>> adjacency = new HashMap<>();

    public Graph<T, P> addEdge(Edge<T, P> edge) {
        adjacency.computeIfAbsent(edge.from(), _ -> new ArrayList<>()).add(edge);
        adjacency.putIfAbsent(edge.to(), new ArrayList<>());
        return this;
    }

    public Optional<Path<T, P>> findFirstCycle() {
        Set<Node<T>> visited = new HashSet<>();
        Set<Node<T>> inStack = new HashSet<>();

        for (Node<T> node : adjacency.keySet()) {
            if (!visited.contains(node)) {
                Optional<Path<T, P>> cycle = findCycleDFS(node, visited, inStack, new ArrayList<>());
                if (cycle.isPresent()) {
                    return cycle;
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Path<T, P>> findCycleDFS(
            Node<T> current, Set<Node<T>> visited, Set<Node<T>> inStack, List<Edge<T, P>> path) {

        visited.add(current);
        inStack.add(current);

        for (Edge<T, P> edge : adjacency.getOrDefault(current, List.of())) {
            Node<T> neighbor = edge.to();

            if (inStack.contains(neighbor)) {
                return Optional.of(cycleFrom(neighbor, path, edge));
            }

            if (visited.contains(neighbor)) {
                continue;
            }

            path.add(edge);

            Optional<Path<T, P>> cycle = findCycleDFS(neighbor, visited, inStack, path);
            if (cycle.isPresent()) {
                return cycle;
            }

            path.removeLast();
        }

        inStack.remove(current);
        return Optional.empty();
    }

    private Path<T, P> cycleFrom(Node<T> start, List<Edge<T, P>> path, Edge<T, P> closingEdge) {

        List<Edge<T, P>> cycle = path.stream()
                .dropWhile(edge -> !edge.from().equals(start))
                .collect(Collectors.toCollection(ArrayList::new));

        cycle.add(closingEdge);
        return new Path<>(cycle);
    }

    public boolean hasEdge(Node<T> from, Node<T> to) {
        List<Edge<T, P>> edges = adjacency.get(from);
        if (edges == null) {
            return false;
        }
        return edges.stream().anyMatch(edge -> edge.to().equals(to));
    }

    public <P2> Graph<T, P> intersection(Graph<T, P2> other) {
        Graph<T, P> result = new Graph<>();

        for (Map.Entry<Node<T>, List<Edge<T, P>>> entry : adjacency.entrySet()) {
            for (Edge<T, P> edge : entry.getValue()) {
                if (other.hasEdge(edge.from(), edge.to())) {
                    result.addEdge(edge);
                }
            }
        }

        return result;
    }

    public void removeEdge(Edge<T, P> edge) {
        List<Edge<T, P>> edges = adjacency.get(edge.from());
        if (edges != null) {
            edges.removeIf(current -> current.to().equals(edge.to()));
        }
    }
}
