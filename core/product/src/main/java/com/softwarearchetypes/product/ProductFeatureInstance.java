package com.softwarearchetypes.product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/** A validated value for a product feature type. */
record ProductFeatureInstance(ProductFeatureType featureType, Object value) {

    ProductFeatureInstance {
        if (featureType == null) {
            throw new IllegalArgumentException("ProductFeatureType must be defined");
        }
        if (value == null) {
            throw new IllegalArgumentException("Feature value must be defined");
        }

        featureType.validateValue(value);
    }

    /** Creates a validated feature instance. */
    static ProductFeatureInstance of(ProductFeatureType featureType, Object value) {
        return new ProductFeatureInstance(featureType, value);
    }

    /** Parses and validates a feature instance from its string representation. */
    static ProductFeatureInstance fromString(ProductFeatureType featureType, String stringValue) {
        if (featureType == null) {
            throw new IllegalArgumentException("ProductFeatureType must be defined");
        }
        if (stringValue == null) {
            throw new IllegalArgumentException("String value must be defined");
        }

        Object parsedValue = featureType.constraint().fromString(stringValue);
        return new ProductFeatureInstance(featureType, parsedValue);
    }

    /** Returns the feature value. */
    @Override
    public Object value() {
        return value;
    }

    /** Returns the feature value's string representation. */
    String valueAsString() {
        return featureType.constraint().toString(value);
    }

    /**
     * Returns the value as a string.
     *
     * @throws IllegalStateException if the value is not a String
     */
    String asString() {
        if (!(value instanceof String string)) {
            throw new IllegalStateException("Feature '%s' value is not a string (type: %s)"
                    .formatted(featureType.name(), value.getClass().getSimpleName()));
        }
        return string;
    }

    /**
     * Returns the value as an integer.
     *
     * @throws IllegalStateException if the value is not an Integer
     */
    int asInt() {
        if (!(value instanceof Integer integer)) {
            throw new IllegalStateException("Feature '%s' value is not an integer (type: %s)"
                    .formatted(featureType.name(), value.getClass().getSimpleName()));
        }
        return integer;
    }

    /**
     * Returns the value as a decimal.
     *
     * @throws IllegalStateException if the value is not a BigDecimal
     */
    BigDecimal asDecimal() {
        if (!(value instanceof BigDecimal decimal)) {
            throw new IllegalStateException("Feature '%s' value is not a decimal (type: %s)"
                    .formatted(featureType.name(), value.getClass().getSimpleName()));
        }
        return decimal;
    }

    /**
     * Returns the value as a date.
     *
     * @throws IllegalStateException if the value is not a LocalDate
     */
    LocalDate asDate() {
        if (!(value instanceof LocalDate date)) {
            throw new IllegalStateException("Feature '%s' value is not a date (type: %s)"
                    .formatted(featureType.name(), value.getClass().getSimpleName()));
        }
        return date;
    }

    /**
     * Returns the value as a boolean.
     *
     * @throws IllegalStateException if the value is not a Boolean
     */
    boolean asBoolean() {
        if (!(value instanceof Boolean bool)) {
            throw new IllegalStateException("Feature '%s' value is not a boolean (type: %s)"
                    .formatted(featureType.name(), value.getClass().getSimpleName()));
        }
        return bool;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductFeatureInstance(ProductFeatureType thatFeatureType, Object thatValue))) {
            return false;
        }
        return Objects.equals(featureType, thatFeatureType) && Objects.equals(value, thatValue);
    }

    @Override
    @NonNull public String toString() {
        return "ProductFeatureInstance{%s=%s}".formatted(featureType.name(), value);
    }

    boolean isOfType(ProductFeatureType type) {
        return featureType.equals(type);
    }
}
