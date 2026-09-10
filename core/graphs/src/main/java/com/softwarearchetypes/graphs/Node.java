package com.softwarearchetypes.graphs;

/** A graph node identified by its property. */
public record Node<T>(T property) {
    @Override
    public String toString() {
        return String.valueOf(property);
    }
}
