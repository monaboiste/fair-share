package com.softwarearchetypes.scoring.algebra.fuzzy;

/**
 * A fuzzy truth value clamped to the inclusive range from {@code 0.0} to {@code 1.0}.
 *
 * @param degree degree of truth
 */
public record FuzzyValue(double degree) {

    public FuzzyValue {
        if (degree < 0.0) degree = 0.0;
        if (degree > 1.0) degree = 1.0;
    }
}
