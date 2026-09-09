package com.softwarearchetypes.quantity;

import java.math.BigDecimal;

/** Quantity represents an amount with a unit of measurement. Examples: 100 kg, 500 liters, 1000 pieces, 25.5 m² */
public record Quantity(BigDecimal amount, Unit unit) implements Comparable<Quantity> {

    public Quantity {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }

        amount = amount.stripTrailingZeros();
    }

    public static Quantity of(BigDecimal amount, Unit unit) {
        return new Quantity(amount, unit);
    }

    public static Quantity of(double amount, Unit unit) {
        return new Quantity(BigDecimal.valueOf(amount), unit);
    }

    public static Quantity of(int amount, Unit unit) {
        return new Quantity(BigDecimal.valueOf(amount), unit);
    }

    public Quantity add(Quantity other) {
        if (!unit.equals(other.unit)) {
            throw new IllegalArgumentException(
                    "Cannot add quantities with different units: %s and %s".formatted(unit, other.unit));
        }
        return new Quantity(amount.add(other.amount), unit);
    }

    public Quantity subtract(Quantity other) {
        if (!unit.equals(other.unit)) {
            throw new IllegalArgumentException(
                    "Cannot subtract quantities with different units: %s and %s".formatted(unit, other.unit));
        }
        return new Quantity(amount.subtract(other.amount), unit);
    }

    @Override
    public String toString() {
        return amount + " " + unit;
    }

    @Override
    public int compareTo(Quantity other) {
        if (!unit.equals(other.unit)) {
            throw new IllegalArgumentException(
                    "Cannot compare quantities with different units: %s and %s".formatted(unit, other.unit));
        }
        return amount.compareTo(other.amount);
    }
}
