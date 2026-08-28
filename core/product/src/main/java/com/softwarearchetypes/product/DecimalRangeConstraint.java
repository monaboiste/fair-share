package com.softwarearchetypes.product;

import java.math.BigDecimal;

/** Restricts decimal values to an inclusive range. */
record DecimalRangeConstraint(BigDecimal min, BigDecimal max) implements FeatureValueConstraint {

    DecimalRangeConstraint {
        if (min == null) {
            throw new IllegalArgumentException("Min must be defined");
        }
        if (max == null) {
            throw new IllegalArgumentException("Max must be defined");
        }
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("Min must be less than or equal to max");
        }
    }

    static DecimalRangeConstraint of(String min, String max) {
        return new DecimalRangeConstraint(new BigDecimal(min), new BigDecimal(max));
    }

    static FeatureValueConstraint between(BigDecimal min, BigDecimal max) {
        return new DecimalRangeConstraint(min, max);
    }

    @Override
    public FeatureValueType valueType() {
        return FeatureValueType.DECIMAL;
    }

    @Override
    public String type() {
        return "DECIMAL_RANGE";
    }

    @Override
    public boolean isValid(Object value) {
        if (!(value instanceof BigDecimal decimalValue)) {
            return false;
        }
        return decimalValue.compareTo(min) >= 0 && decimalValue.compareTo(max) <= 0;
    }

    @Override
    public String desc() {
        return "decimal between %s and %s".formatted(min, max);
    }
}
