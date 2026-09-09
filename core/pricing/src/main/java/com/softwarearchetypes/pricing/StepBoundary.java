package com.softwarearchetypes.pricing;

/** Defines whether a step calculation includes its upper boundary. */
public enum StepBoundary {

    /** Uses {@code floor(quantity / stepSize)} with an exclusive upper boundary. */
    EXCLUSIVE,

    /** Uses {@code floor((quantity - 1) / stepSize)} for positive quantities. */
    INCLUSIVE;

    /** Human-readable description of this boundary type. */
    public String describe() {
        return switch (this) {
            case EXCLUSIVE -> "Exclusive upper boundary: [0, N), [N, 2N), ...";
            case INCLUSIVE -> "Inclusive upper boundary: [0, N], [N+1, 2N+1], ...";
        };
    }
}
