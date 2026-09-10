package com.softwarearchetypes.rules.core.predicates;

public interface RichLogicalPredicate<T> extends LogicalPredicate<T> {
    default LogicalPredicate<T> and(LogicalPredicate<T> other) {
        return new AndPredicate<>(this, other);
    }

    default LogicalPredicate<T> or(LogicalPredicate<T> other) {
        return new OrPredicate<>(this, other);
    }

    default LogicalPredicate<T> not() {
        return new NotPredicate<>(this);
    }
}
