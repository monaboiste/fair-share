package com.softwarearchetypes.product;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/** Defines supported product feature value types and their string conversions. */
public enum FeatureValueType {
    TEXT(String.class) {
        @Override
        Object castFrom(String value) {
            return value;
        }

        @Override
        String castTo(Object value) {
            return (String) value;
        }
    },

    INTEGER(Integer.class) {
        @Override
        Object castFrom(String value) {
            return Integer.valueOf(value);
        }

        @Override
        String castTo(Object value) {
            return String.valueOf(value);
        }
    },

    DECIMAL(BigDecimal.class) {
        @Override
        Object castFrom(String value) {
            return new BigDecimal(value);
        }

        @Override
        String castTo(Object value) {
            return value.toString();
        }
    },

    DATE(LocalDate.class) {
        @Override
        Object castFrom(String value) {
            return LocalDate.parse(value);
        }

        @Override
        String castTo(Object value) {
            return value.toString();
        }
    },

    BOOLEAN(Boolean.class) {
        @Override
        Object castFrom(String value) {
            return Boolean.valueOf(value);
        }

        @Override
        String castTo(Object value) {
            return String.valueOf(value);
        }
    };

    private final Class<?> type;

    FeatureValueType(Class<?> type) {
        this.type = type;
    }

    /**
     * Converts a String representation to the runtime type.
     *
     * @throws IllegalArgumentException if the value cannot be parsed
     */
    abstract Object castFrom(String value);

    /** Converts a runtime value to its string representation. */
    abstract String castTo(Object value);

    /** Returns the class that represents this value type. */
    public Class<?> type() {
        return type;
    }

    /** Returns whether the value is an instance of this type. */
    public boolean isInstance(@Nullable Object value) {
        return type.isInstance(value);
    }
}
