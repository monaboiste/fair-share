package com.softwarearchetypes.product;

/** Validates and converts product feature values. */
sealed interface FeatureValueConstraint
        permits AllowedValuesConstraint,
                NumericRangeConstraint,
                DecimalRangeConstraint,
                RegexConstraint,
                DateRangeConstraint,
                Unconstrained {

    /**
     * Returns the type of value accepted by this constraint.
     *
     * @return the expected value type
     */
    FeatureValueType valueType();

    /**
     * Returns the constraint identifier used for persistence and deserialization.
     *
     * <p>Example identifiers include {@code ALLOWED_VALUES}, {@code NUMERIC_RANGE}, {@code REGEX}, {@code DATE_RANGE},
     * and {@code UNCONSTRAINED}.
     *
     * @return the constraint identifier
     */
    String type();

    /**
     * Determines whether the given value satisfies this constraint.
     *
     * <p>The value must have the type indicated by {@link #valueType()}.
     *
     * @param value the value to validate
     * @return {@code true} if the value satisfies this constraint; otherwise {@code false}
     */
    boolean isValid(Object value);

    /**
     * Returns a human-readable description of this constraint.
     *
     * <p>Examples include {@code one of: [red, blue, green]} and {@code integer between 1 and 100}.
     *
     * @return the constraint description
     */
    String desc();

    /**
     * Converts a value to its string representation using the conversion rules of {@link #valueType()}.
     *
     * <p>This method performs type conversion but does not verify that the value satisfies this constraint.
     *
     * @param value the value to convert
     * @return the string representation of the value
     */
    default String toString(Object value) {
        return valueType().castTo(value);
    }

    /**
     * Converts a string representation to a value and verifies that it satisfies this constraint.
     *
     * @param value the string representation to convert
     * @return the converted and validated value
     * @throws IllegalArgumentException if the value cannot be converted or does not satisfy this constraint
     */
    default Object fromString(String value) {
        Object casted = valueType().castFrom(value);
        if (!isValid(casted)) {
            throw new IllegalArgumentException("Invalid value: '%s'. Expected: %s".formatted(value, desc()));
        }
        return casted;
    }
}
