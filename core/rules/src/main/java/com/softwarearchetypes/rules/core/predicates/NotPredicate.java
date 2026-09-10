package com.softwarearchetypes.rules.core.predicates;

public record NotPredicate<T>(LogicalPredicate<T> child) implements LogicalPredicate<T> {

    @Override
    public boolean test(T t) {
        return !child.test(t);
    }
}
