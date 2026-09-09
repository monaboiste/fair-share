package com.softwarearchetypes.product;

import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Restricts text to a predefined set of values. */
record AllowedValuesConstraint(Set<String> allowedValues) implements FeatureValueConstraint {

    AllowedValuesConstraint(Set<String> allowedValues) {
        if (allowedValues.isEmpty()) {
            throw new IllegalArgumentException("Allowed values must not be empty");
        }
        this.allowedValues = Set.copyOf(allowedValues);
    }

    static AllowedValuesConstraint of(String... values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("Allowed values must not be empty");
        }
        return new AllowedValuesConstraint(Set.of(values));
    }

    @Override
    public FeatureValueType valueType() {
        return FeatureValueType.TEXT;
    }

    @Override
    public String type() {
        return "ALLOWED_VALUES";
    }

    @Override
    public boolean isValid(@Nullable Object value) {
        return value instanceof String text && allowedValues.contains(text);
    }

    @Override
    public String desc() {
        return "one of: " + allowedValues;
    }
}
