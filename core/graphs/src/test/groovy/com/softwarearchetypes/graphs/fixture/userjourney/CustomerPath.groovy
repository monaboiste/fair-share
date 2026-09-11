package com.softwarearchetypes.graphs.fixture.userjourney


import java.util.function.Function

record CustomerPath(List<Condition> conditions) {

    static CustomerPath of(List<Condition> conditions) {
        return new CustomerPath(List.copyOf(conditions))
    }

    int length() {
        return conditions.size()
    }

    boolean isEmpty() {
        return conditions.isEmpty()
    }

    double weight(Function<Condition, Double> weightFunction) {
        return conditions.stream().mapToDouble(weightFunction::apply).sum()
    }
}
