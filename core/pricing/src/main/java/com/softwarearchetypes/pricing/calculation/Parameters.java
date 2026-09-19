package com.softwarearchetypes.pricing.calculation;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

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
        return initialized(key, value, null, null, null, null, null, null, null, null);
    }

    public static Parameters of(String key1, Object value1, String key2, Object value2) {
        return initialized(key1, value1, key2, value2, null, null, null, null, null, null);
    }

    public static Parameters of(String key1, Object value1, String key2, Object value2, String key3, Object value3) {
        return initialized(key1, value1, key2, value2, key3, value3, null, null, null, null);
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
        return initialized(key1, value1, key2, value2, key3, value3, key4, value4, null, null);
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
        return initialized(key1, value1, key2, value2, key3, value3, key4, value4, key5, value5);
    }

    public static <T> Parameters of(ParameterKey<T> key, T value) {
        return of(key.name(), value);
    }

    private static Parameters initialized(
            @Nullable String key1,
            @Nullable Object value1,
            @Nullable String key2,
            @Nullable Object value2,
            @Nullable String key3,
            @Nullable Object value3,
            @Nullable String key4,
            @Nullable Object value4,
            @Nullable String key5,
            @Nullable Object value5) {
        Map<String, Object> m = new HashMap<>();
        if (key1 != null && value1 != null) {
            m.put(key1, value1);
        }
        if (key2 != null && value2 != null) {
            m.put(key2, value2);
        }
        if (key3 != null && value3 != null) {
            m.put(key3, value3);
        }
        if (key4 != null && value4 != null) {
            m.put(key4, value4);
        }
        if (key5 != null && value5 != null) {
            m.put(key5, value5);
        }
        return new Parameters(m);
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
