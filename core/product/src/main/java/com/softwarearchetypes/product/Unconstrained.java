package com.softwarearchetypes.product;

import java.util.Locale;
import org.jspecify.annotations.Nullable;

/** Accepts every value of the specified type. */
record Unconstrained(FeatureValueType valueType) implements FeatureValueConstraint {

    @Override
    public String type() {
        return "UNCONSTRAINED";
    }

    @Override
    public boolean isValid(@Nullable Object value) {
        return valueType.isInstance(value);
    }

    @Override
    public String desc() {
        return "any " + valueType.name().toLowerCase(Locale.ROOT);
    }
}
