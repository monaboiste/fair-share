package com.github.monaboiste.fairshare.pricing.calculation;

import com.github.monaboiste.fairshare.quantity.money.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Converts raw parameter values to the type declared by a {@link ParameterKey}. */
interface ParameterValueConverter {

    <T> T convert(ParameterKey<T> key, Object value);
}

final class DefaultParameterValueConverter implements ParameterValueConverter {

    private final List<ValueConverter<?, ?>> converters;

    DefaultParameterValueConverter() {
        this.converters = List.of(
                new StringToMoneyConverter(),
                new StringToBigDecimalConverter(),
                new NumberToBigDecimalConverter(),
                new StringToLocalDateConverter(),
                new StringToInstantConverter(),
                new StringToLocalDateTimeConverter());
    }

    @Override
    public <T> T convert(ParameterKey<T> key, Object value) {
        Class<T> targetType = key.type();

        if (targetType.isInstance(value)) {
            return targetType.cast(value);
        }

        List<ValueConverter<?, ?>> matchingConverters = converters.stream()
                .filter(converter -> converter.canConvert(targetType, value))
                .toList();

        if (matchingConverters.isEmpty()) {
            throw new IllegalArgumentException("Cannot convert %s (%s) to %s"
                    .formatted(value, value.getClass().getSimpleName(), targetType.getSimpleName()));
        }

        Object converted = matchingConverters.getFirst().convertValue(value);
        return targetType.cast(converted);
    }
}

/**
 * Converts parameter values from a supported source type to a target type.
 *
 * @param <S> source type
 * @param <T> target type
 * @implSpec Implementations should be stateless and deterministic. When multiple converters support the same
 *     conversion, the first registered converter is used.
 */
interface ValueConverter<S, T> {

    Class<S> sourceType();

    Class<T> targetType();

    T convert(S value);

    default boolean canConvert(Class<?> targetType, Object value) {
        return this.targetType() == targetType && sourceType().isInstance(value);
    }

    default T convertValue(Object value) {
        return convert(sourceType().cast(value));
    }
}

final class StringToMoneyConverter implements ValueConverter<String, Money> {

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    @Override
    public Class<String> sourceType() {
        return String.class;
    }

    @Override
    public Class<Money> targetType() {
        return Money.class;
    }

    @Override
    public Money convert(String value) {
        String[] parts = WHITESPACE_PATTERN.split(value.trim(), -1);

        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "Invalid Money format: '%s'. Expected '<currency> <amount>', e.g. 'PLN 1999.00'".formatted(value));
        }

        return Money.of(new BigDecimal(parts[1]), parts[0].toUpperCase(Locale.ROOT));
    }
}

final class StringToBigDecimalConverter implements ValueConverter<String, BigDecimal> {

    @Override
    public Class<String> sourceType() {
        return String.class;
    }

    @Override
    public Class<BigDecimal> targetType() {
        return BigDecimal.class;
    }

    @Override
    public BigDecimal convert(String value) {
        return new BigDecimal(value);
    }
}

final class NumberToBigDecimalConverter implements ValueConverter<Number, BigDecimal> {

    @Override
    public Class<Number> sourceType() {
        return Number.class;
    }

    @Override
    public Class<BigDecimal> targetType() {
        return BigDecimal.class;
    }

    @Override
    public BigDecimal convert(Number value) {
        return new BigDecimal(value.toString());
    }
}

final class StringToLocalDateConverter implements ValueConverter<String, LocalDate> {

    @Override
    public Class<String> sourceType() {
        return String.class;
    }

    @Override
    public Class<LocalDate> targetType() {
        return LocalDate.class;
    }

    @Override
    public LocalDate convert(String value) {
        return LocalDate.parse(value);
    }
}

final class StringToInstantConverter implements ValueConverter<String, Instant> {

    @Override
    public Class<String> sourceType() {
        return String.class;
    }

    @Override
    public Class<Instant> targetType() {
        return Instant.class;
    }

    @Override
    public Instant convert(String value) {
        return Instant.parse(value);
    }
}

final class StringToLocalDateTimeConverter implements ValueConverter<String, LocalDateTime> {

    @Override
    public Class<String> sourceType() {
        return String.class;
    }

    @Override
    public Class<LocalDateTime> targetType() {
        return LocalDateTime.class;
    }

    @Override
    public LocalDateTime convert(String value) {
        return LocalDateTime.parse(value);
    }
}
