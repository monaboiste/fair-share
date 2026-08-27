package com.softwarearchetypes.quantity;

import static com.softwarearchetypes.common.Preconditions.checkArgument;
import static com.softwarearchetypes.common.Preconditions.checkNotNull;

import java.math.BigDecimal;
import org.jspecify.annotations.NonNull;

/** Quantity represents an amount with a unit of measurement. Examples: 100 kg, 500 liters, 1000 pieces, 25.5 m² */
public record Quantity(BigDecimal amount, Unit unit) implements Comparable<Quantity> {

    public Quantity {
        checkNotNull(amount, "Amount cannot be null");
        checkNotNull(unit, "Unit cannot be null");
        checkArgument(amount.compareTo(BigDecimal.ZERO) >= 0, "Amount cannot be negative");

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
        checkArgument(
                unit.equals(other.unit),
                String.format("Cannot add quantities with different units: %s and %s", unit, other.unit));
        return new Quantity(amount.add(other.amount), unit);
    }

    public Quantity subtract(Quantity other) {
        checkArgument(
                unit.equals(other.unit),
                String.format("Cannot subtract quantities with different units: %s and %s", unit, other.unit));
        return new Quantity(amount.subtract(other.amount), unit);
    }

    @Override
    @NonNull public String toString() {
        return amount + " " + unit;
    }

    @Override
    public int compareTo(@NonNull Quantity other) {
        checkNotNull(other, "Cannot compare to null Quantity");
        checkArgument(
                unit.equals(other.unit),
                String.format("Cannot compare quantities with different units: %s and %s", unit, other.unit));
        return amount.compareTo(other.amount);
    }
}
