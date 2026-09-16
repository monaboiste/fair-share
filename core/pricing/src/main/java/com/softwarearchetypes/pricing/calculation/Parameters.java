package com.softwarearchetypes.pricing.calculation;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
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
        this.values = Collections.unmodifiableMap(new HashMap<>(values));
    }

    public static Parameters empty() {
        return new Parameters();
    }

    public static Parameters of(String key, Object value) {
        var m = new HashMap<String, Object>();
        m.put(key, value);
        return new Parameters(m);
    }

    public static Parameters of(String key1, Object value1, String key2, Object value2) {
        return of(key1, value1).with(key2, value2);
    }

    public static Parameters of(String key1, Object value1, String key2, Object value2, String key3, Object value3) {
        return of(key1, value1, key2, value2).with(key3, value3);
    }

    public static Parameters of(
            String key1,
            Object value1,
            String key2,
            Object value2,
            String key3,
            Object value3,
            String key4,
            Object value4) {
        return of(key1, value1, key2, value2, key3, value3).with(key4, value4);
    }

    public static Parameters of(
            String key1,
            Object value1,
            String key2,
            Object value2,
            String key3,
            Object value3,
            String key4,
            Object value4,
            String key5,
            Object value5) {
        return of(key1, value1, key2, value2, key3, value3, key4, value4).with(key5, value5);
    }

    public static <T> Parameters of(ParameterKey<T> key, T value) {
        return of(key.name(), value);
    }

    public <T> Parameters with(ParameterKey<T> key, T value) {
        return with(key.name(), value);
    }

    public Parameters with(String key, Object value) {
        var m = new HashMap<>(values);
        m.put(key, value);
        return new Parameters(m);
    }

    public <T> T get(ParameterKey<T> key) {
        Object value = values.get(key.name());
        if (value == null) {
            throw new IllegalArgumentException("Required parameter '%s' is absent".formatted(key.name()));
        }
        try {
            return key.type().cast(convert(key, value));
        } catch (RuntimeException cause) {
            throw new IllegalArgumentException(
                    "Parameter '%s' must be convertible to %s"
                            .formatted(key.name(), key.type().getSimpleName()),
                    cause);
        }
    }

    public <T> T get(String name, Class<T> type) {
        return get(new ParameterKey<>(name, type));
    }

    private Object convert(ParameterKey<?> key, Object value) {
        if (key.type() == Money.class && value instanceof String text) {
            var parts = new StringTokenizer(text);
            if (parts.countTokens() != 2) {
                throw new IllegalArgumentException(
                        "Invalid Money format: " + value + ". Expected format: 'PLN 1999.00'");
            }
            String currency = parts.nextToken().toUpperCase(Locale.ROOT);
            return Money.of(new BigDecimal(parts.nextToken()), currency);
        }
        if (key.type() == BigDecimal.class && value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        if (key.type() == BigDecimal.class && value instanceof String text) {
            return new BigDecimal(text);
        }
        if (key.type() == LocalDate.class && value instanceof String text) {
            return LocalDate.parse(text);
        }
        if (key.type() == Instant.class && value instanceof String text) {
            return Instant.parse(text);
        }
        if (key.type() == LocalDateTime.class && value instanceof String text) {
            return LocalDateTime.parse(text);
        }
        if (!key.type().isInstance(value)) {
            if (key.type() == Money.class
                    || key.type() == BigDecimal.class
                    || key.type() == LocalDate.class
                    || key.type() == Instant.class
                    || key.type() == LocalDateTime.class) {
                throw new IllegalArgumentException(
                        "Cannot convert " + value + " to " + key.type().getSimpleName());
            }
            throw new ClassCastException(value.getClass().getName() + " cannot be cast to "
                    + key.type().getName());
        }
        return value;
    }

    public BigDecimal getBigDecimal(String key) {
        return get(key, BigDecimal.class);
    }

    public Money getMoney(String key) {
        return get(key, Money.class);
    }

    LocalDate getLocalDate(String key) {
        return get(key, LocalDate.class);
    }

    Instant getInstant(String key) {
        return get(key, Instant.class);
    }

    LocalDateTime getTime(String key) {
        return get(key, LocalDateTime.class);
    }

    public Optional<LocalDateTime> timestamp() {
        return contains("timestamp") ? Optional.of(getTime("timestamp")) : Optional.empty();
    }

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
        if (value == null) throw new IllegalArgumentException("Required parameter '%s' is absent".formatted(key));
        return value;
    }

    public Set<String> keys() {
        return values.keySet();
    }

    @Override
    public String toString() {
        return "Parameters" + values;
    }
}
