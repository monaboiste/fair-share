package com.softwarearchetypes.rules.core.predicates;

public final class AlwaysPredicate<T> implements LogicalPredicate<T> {

    @Override
    public boolean test(T ignored) {
        return true;
    }
}
