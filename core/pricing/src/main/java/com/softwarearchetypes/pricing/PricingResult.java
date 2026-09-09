package com.softwarearchetypes.pricing;

import com.softwarearchetypes.quantity.Unit;
import com.softwarearchetypes.quantity.money.Money;

/**
 * Represents a pricing result together with its semantic interpretation.
 *
 * <ul>
 *   <li>total price for the whole quantity or period;
 *   <li>average unit price; or
 *   <li>marginal price for a specific unit.
 * </ul>
 */
public sealed interface PricingResult permits TotalPrice, UnitPrice, MarginalPrice {

    /** The monetary value of this pricing result. */
    Money money();

    /** Human-readable description of what this price represents. */
    String describe();
}

/** Represents the total price for the entire quantity or period. */
record TotalPrice(Money amount) implements PricingResult {

    @Override
    public Money money() {
        return amount;
    }

    @Override
    public String describe() {
        return "Total: " + amount;
    }
}

/** Represents the average price per unit. */
record UnitPrice(Money amountPerUnit, Unit unit) implements PricingResult {

    @Override
    public Money money() {
        return amountPerUnit;
    }

    @Override
    public String describe() {
        return "Unit price: " + amountPerUnit + "/" + unit;
    }
}

/** Represents the price of a specific unit at the margin. */
record MarginalPrice(Money amount, int unitIndex, Unit unit) implements PricingResult {

    @Override
    public Money money() {
        return amount;
    }

    @Override
    public String describe() {
        return unitIndex + "-th " + unit.name() + ": " + amount;
    }
}
