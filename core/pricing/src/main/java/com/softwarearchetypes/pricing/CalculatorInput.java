package com.softwarearchetypes.pricing;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** A typed, calculation-only input descriptor owned by a calculator. */
public final class CalculatorInput<T> {
    private final String name;
    private final String descriptorType;
    private final String expectedType;
    private final Reader<T> reader;

    private CalculatorInput(String name, String descriptorType, String expectedType, Reader<T> reader) {
        this.name = Objects.requireNonNull(name);
        this.descriptorType = Objects.requireNonNull(descriptorType);
        this.expectedType = Objects.requireNonNull(expectedType);
        this.reader = Objects.requireNonNull(reader);
    }

    public static CalculatorInput<Money> money(String name) {
        return new CalculatorInput<>(name, "convert:money", "Money", p -> p.getMoney(name));
    }

    public static CalculatorInput<BigDecimal> bigDecimal(String name) {
        return new CalculatorInput<>(name, "convert:big-decimal", "BigDecimal", p -> p.getBigDecimal(name));
    }

    public static CalculatorInput<LocalDate> localDate(String name) {
        return new CalculatorInput<>(name, "convert:local-date", "LocalDate", p -> p.getLocalDate(name));
    }

    public static CalculatorInput<Instant> instant(String name) {
        return new CalculatorInput<>(name, "convert:instant", "Instant", p -> p.getInstant(name));
    }

    public static CalculatorInput<LocalDateTime> localDateTime(String name) {
        return new CalculatorInput<>(name, "convert:local-date-time", "LocalDateTime", p -> p.getTime(name));
    }

    public static <T> CalculatorInput<T> instanceOf(String name, Class<T> type) {
        Objects.requireNonNull(type);
        return new CalculatorInput<>(
                name, "instance:" + type.getName(), type.getSimpleName(), p -> type.cast(p.require(name)));
    }

    public T read(Parameters parameters) {
        if (!parameters.contains(name) || parameters.get(name) == null) {
            throw new IllegalArgumentException("Required parameter '%s' is absent".formatted(name));
        }
        try {
            return reader.read(parameters);
        } catch (RuntimeException cause) {
            throw new IllegalArgumentException(
                    "Parameter '%s' must be convertible to %s".formatted(name, expectedType), cause);
        }
    }

    public String name() {
        return name;
    }

    boolean isCompatibleWith(CalculatorInput<?> other) {
        return descriptorType.equals(other.descriptorType);
    }

    String expectedType() {
        return expectedType;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CalculatorInput<?> input
                && name.equals(input.name)
                && descriptorType.equals(input.descriptorType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, descriptorType);
    }

    @FunctionalInterface
    private interface Reader<T> {
        T read(Parameters parameters);
    }
}
