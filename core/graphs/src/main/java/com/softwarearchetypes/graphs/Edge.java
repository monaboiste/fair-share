package com.softwarearchetypes.graphs;

/** A directed edge with an associated property. */
public record Edge<T, P>(Node<T> from, Node<T> to, P property) {

    @Override
    public String toString() {
        return from + " -> " + to;
    }
}
