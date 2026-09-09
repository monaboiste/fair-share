package com.softwarearchetypes.pricing;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import org.jspecify.annotations.Nullable;

public record Parameters(Map<String, Object> values) {
    public Parameters() {
        this(new HashMap<>());
    }

    public Parameters(Map<String, Object> values) {
        this.values = new HashMap<>(values);
    }

    public static Parameters empty() {
        return new Parameters();
    }

    public static Parameters of(String key, Object value) {
        return new Parameters(Map.of(key, value));
    }

    public static Parameters of(String k1, Object v1, String k2, Object v2) {
        return new Parameters(Map.of(k1, v1, k2, v2));
    }

    public static Parameters of(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
        return new Parameters(Map.of(k1, v1, k2, v2, k3, v3));
    }

    public static Parameters of(
            String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) {
        return new Parameters(Map.of(k1, v1, k2, v2, k3, v3, k4, v4));
    }

    public static Parameters of(
            String k1,
            Object v1,
            String k2,
            Object v2,
            String k3,
            Object v3,
            String k4,
            Object v4,
            String k5,
            Object v5) {
        return new Parameters(Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5));
    }

    public BigDecimal getBigDecimal(String key) {
        Object value = values.get(key);
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        if (value instanceof String text) {
            return new BigDecimal(text);
        }
        throw new IllegalArgumentException("Cannot convert " + value + " to BigDecimal");
    }

    public Money getMoney(String key) {
        Object value = values.get(key);
        if (value instanceof Money money) {
            return money;
        }
        if (value instanceof String text) {
            StringTokenizer parts = new StringTokenizer(text);
            if (parts.countTokens() != 2) {
                throw new IllegalArgumentException(
                        "Invalid Money format: " + value + ". Expected format: 'PLN 1999.00'");
            }
            String currency = parts.nextToken().toUpperCase(Locale.ROOT);
            BigDecimal amount = new BigDecimal(parts.nextToken());
            return Money.of(amount, currency);
        }
        throw new IllegalArgumentException("Cannot convert " + value + " to Money");
    }

    LocalDate getLocalDate(String key) {
        Object value = values.get(key);
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof String text) {
            return LocalDate.parse(text);
        }
        throw new IllegalArgumentException("Cannot convert " + value + " to LocalDate");
    }

    Instant getInstant(String key) {
        Object value = values.get(key);
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof String text) {
            return Instant.parse(text);
        }
        throw new IllegalArgumentException("Cannot convert " + value + " to Instant");
    }

    LocalDateTime getTime(String key) {
        Object value = values.get(key);
        if (value instanceof LocalDateTime date) {
            return date;
        }
        if (value instanceof String text) {
            return LocalDateTime.parse(text);
        }
        throw new IllegalArgumentException("Cannot convert " + value + " to LocalDateTime");
    }

    /** Returns the calculation timestamp, if present. */
    public Optional<LocalDateTime> timestamp() {
        if (contains("timestamp")) {
            return Optional.of(getTime("timestamp"));
        }
        return Optional.empty();
    }

    /**
     * Returns the required calculation timestamp.
     *
     * @throws IllegalArgumentException when the timestamp is absent.
     */
    public LocalDateTime requireTimestamp() {
        return timestamp()
                .orElseThrow(() ->
                        new IllegalArgumentException("Parameters must contain 'timestamp' for versioned calculations"));
    }

    public boolean contains(String key) {
        return values.containsKey(key);
    }

    public boolean containsAll(Set<String> keys) {
        return values.keySet().containsAll(keys);
    }

    public @Nullable Object get(String key) {
        return values.get(key);
    }

    Object require(String key) {
        Object value = values.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required parameter '%s' is absent".formatted(key));
        }
        return value;
    }

    @Override
    public String toString() {
        return "Parameters" + values;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Parameters(var thatValues))) {
            return false;
        }
        return values.equals(thatValues);
    }

    public Set<String> keys() {
        return values.keySet();
    }

    public void setValues(Map<String, Object> values) {
        this.values.clear();
        this.values.putAll(values);
    }

    /** Returns a copy containing the given parameter. */
    public Parameters with(String key, Object value) {
        Map<String, Object> newValues = new HashMap<>(this.values);
        newValues.put(key, value);
        return new Parameters(newValues);
    }
}
