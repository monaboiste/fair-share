package com.softwarearchetypes.pricing;

/**
 * Semantic interpretation of a calculator result.
 *
 * <p>The same mathematical function can represent a total, unit, or marginal price.
 */
public enum Interpretation {

    /** Complete cost for the entire quantity or time period. */
    TOTAL,

    /** Average price for a single unit. */
    UNIT,

    /** Price of a specific unit at the margin. */
    MARGINAL;

    /** Human-readable description of this interpretation. */
    public String describe() {
        return switch (this) {
            case TOTAL -> "Total price for entire quantity/period";
            case UNIT -> "Average price per single unit";
            case MARGINAL -> "Price of n-th specific unit";
        };
    }
}
