package com.softwarearchetypes.product;

import java.util.Locale;

/** Accepts every value of the specified type. */
record Unconstrained(FeatureValueType valueType) implements FeatureValueConstraint {

    Unconstrained {
        if (valueType == null) {
            throw new IllegalArgumentException("Value type must be defined");
        }
    }

    @Override
    public String type() {
        return "UNCONSTRAINED";
    }

    @Override
    public boolean isValid(Object value) {
        return valueType.isInstance(value);
    }

    @Override
    public String desc() {
        return "any " + valueType.name().toLowerCase(Locale.ROOT);
    }
}
