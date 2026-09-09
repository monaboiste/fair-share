package com.softwarearchetypes.product;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Defines a named product feature and its value constraint. */
public class ProductFeatureType {

    private final String name;
    private final FeatureValueConstraint constraint;

    ProductFeatureType(String name, FeatureValueConstraint constraint) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Feature type name must be defined");
        }

        this.name = name;
        this.constraint = constraint;
    }

    /** Creates a text feature restricted to the given values. */
    public static ProductFeatureType withAllowedValues(String name, String... allowedValues) {
        return new ProductFeatureType(name, AllowedValuesConstraint.of(allowedValues));
    }

    /** Creates an integer feature restricted to an inclusive range. */
    public static ProductFeatureType withNumericRange(String name, int min, int max) {
        return new ProductFeatureType(name, new NumericRangeConstraint(min, max));
    }

    /** Creates a decimal feature restricted to an inclusive range. */
    public static ProductFeatureType withDecimalRange(String name, String min, String max) {
        return new ProductFeatureType(name, DecimalRangeConstraint.of(min, max));
    }

    /** Creates a text feature restricted by a regular expression. */
    public static ProductFeatureType withRegex(String name, String pattern) {
        return new ProductFeatureType(name, RegexConstraint.of(pattern));
    }

    /** Creates a date feature restricted to an inclusive range. */
    public static ProductFeatureType withDateRange(String name, String from, String to) {
        return new ProductFeatureType(name, DateRangeConstraint.between(from, to));
    }

    /** Creates a feature accepting every value of the given type. */
    public static ProductFeatureType unconstrained(String name, FeatureValueType valueType) {
        return new ProductFeatureType(name, new Unconstrained(valueType));
    }

    /** Creates a feature with a custom constraint. */
    static ProductFeatureType of(String name, FeatureValueConstraint constraint) {
        return new ProductFeatureType(name, constraint);
    }

    public String name() {
        return name;
    }

    FeatureValueConstraint constraint() {
        return constraint;
    }

    /**
     * Validates whether the given value is valid for this feature type.
     *
     * @param value the value to validate
     * @return {@code true} if the value is valid
     */
    public boolean isValidValue(@Nullable Object value) {
        return constraint.isValid(value);
    }

    /**
     * Validates a feature value.
     *
     * @param value the value to validate
     * @throws IllegalArgumentException if the value is invalid
     */
    public void validateValue(Object value) {
        if (!constraint.valueType().isInstance(value)) {
            throw new IllegalArgumentException(String.format(
                    "Feature '%s' expects type %s but got %s",
                    name,
                    constraint.valueType().type().getSimpleName(),
                    value.getClass().getSimpleName()));
        }
        if (!isValidValue(value)) {
            throw new IllegalArgumentException(
                    String.format("Invalid value '%s' for feature '%s'. Expected: %s", value, name, constraint.desc()));
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductFeatureType that)) {
            return false;
        }
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "ProductFeatureType{name='%s', constraint=%s}".formatted(name, constraint.desc());
    }
}
