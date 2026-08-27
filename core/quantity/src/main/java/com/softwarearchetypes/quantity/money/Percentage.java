package com.softwarearchetypes.quantity.money;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;

import java.math.BigDecimal;
import org.jspecify.annotations.NonNull;

public record Percentage(BigDecimal value) {

    public Percentage {
        if (value.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("Percentage can't be negative");
        }
        value = value.setScale(5, HALF_UP);
    }

    public static Percentage of(BigDecimal percentage) {
        return new Percentage(percentage);
    }

    public static Percentage of(int percentage) {
        return of(BigDecimal.valueOf(percentage));
    }

    public static Percentage ofFraction(double v) {
        return of(BigDecimal.valueOf(v * 100));
    }

    public static Percentage zero() {
        return of(0);
    }

    public Percentage add(Percentage other) {
        return of(value.add(other.value));
    }

    public Percentage subtract(Percentage other) {
        return of(value.subtract(other.value));
    }

    public Percentage multiply(Percentage other) {
        return of(value.multiply(other.value()).divide(new BigDecimal(100), HALF_UP));
    }

    @Override
    @NonNull public String toString() {
        return value.setScale(2, HALF_UP).stripTrailingZeros().toPlainString() + "%";
    }
}
