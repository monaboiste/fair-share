package com.softwarearchetypes.product;

import org.jspecify.annotations.Nullable;

/** Restricts integer values to an inclusive range. */
record NumericRangeConstraint(int min, int max) implements FeatureValueConstraint {

    NumericRangeConstraint {
        if (min > max) {
            throw new IllegalArgumentException("Min must be less than or equal to max");
        }
    }

    static FeatureValueConstraint between(int min, int max) {
        return new NumericRangeConstraint(min, max);
    }

    @Override
    public FeatureValueType valueType() {
        return FeatureValueType.INTEGER;
    }

    @Override
    public String type() {
        return "NUMERIC_RANGE";
    }

    @Override
    public boolean isValid(@Nullable Object value) {
        if (!(value instanceof Integer intValue)) {
            return false;
        }
        return intValue >= min && intValue <= max;
    }

    @Override
    public String desc() {
        return "integer between %d and %d".formatted(min, max);
    }
}
