package com.softwarearchetypes.pricing.calculation;

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

public record Parameters(Map<String, Object> values) {

    private static final ParameterValueConverter VALUE_CONVERTER = new DefaultParameterValueConverter();

    public Parameters() {
        this(new HashMap<>());
    }

    public Parameters(Map<String, Object> values) {
        this.values = Map.copyOf(values);
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
        Map<String, Object> m = new HashMap<>(values);
        m.put(key, value);
        return new Parameters(m);
    }

    public <T> T get(ParameterKey<T> key) {
        Object value = values.get(key.name());

        if (value == null) {
            throw new IllegalArgumentException("Required parameter '%s' is absent".formatted(key.name()));
        }

        try {
            return VALUE_CONVERTER.convert(key, value);
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

    public <T> Optional<T> find(ParameterKey<T> key) {
        if (values.containsKey(key.name())) {
            return Optional.of(get(key));
        }
        return Optional.empty();
    }

    public <T> Optional<T> find(String name, Class<T> type) {
        return find(new ParameterKey<>(name, type));
    }

    private Object convert(ParameterKey<?> key, Object value) {
        Class<?> targetType = key.type();

        if (targetType.isInstance(value)) {
            return value;
        }

        return switch (value) {
            case String text
            when targetType == Money.class -> {
                String[] parts = text.trim().split("\\s+");
                if (parts.length != 2) {
                    throw new IllegalArgumentException(
                            "Invalid Money format: '%s'. Expected '<currency> <amount>', e.g. 'PLN 1999.00'"
                                    .formatted(text));
                }

                yield Money.of(new BigDecimal(parts[1]), parts[0].toUpperCase(Locale.ROOT));
            }
            case Number number when targetType == BigDecimal.class -> new BigDecimal(number.toString());
            case String text when targetType == BigDecimal.class -> new BigDecimal(text);
            case String text when targetType == LocalDate.class -> LocalDate.parse(text);
            case String text when targetType == Instant.class -> Instant.parse(text);
            case String text when targetType == LocalDateTime.class -> LocalDateTime.parse(text);
            default ->
                throw new IllegalArgumentException("Cannot convert %s (%s) to %s"
                        .formatted(value, value.getClass().getSimpleName(), targetType.getSimpleName()));
        };
    }

    public BigDecimal getBigDecimal(String key) {
        return get(key, BigDecimal.class);
    }

    public Money getMoney(String key) {
        return get(key, Money.class);
    }

    public LocalDate getLocalDate(String key) {
        return get(key, LocalDate.class);
    }

    public Instant getInstant(String key) {
        return get(key, Instant.class);
    }

    public LocalDateTime getLocalDateTime(String key) {
        return get(key, LocalDateTime.class);
    }

    public boolean contains(String key) {
        return values.containsKey(key);
    }

    public boolean containsAll(Set<String> keys) {
        return values.keySet().containsAll(keys);
    }

    public Object get(String key) {
        Object value = values.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required parameter '%s' is absent".formatted(key));
        }
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
