package com.softwarearchetypes.pricing;

@SuppressWarnings("ImmutableEnumChecker")
public enum CalculatorType {
    SIMPLE_FIXED("simple-fixed", "Fixed amount calculator - returns %s regardless"),
    SIMPLE_INTEREST(
            "simple-interest",
            "Annual interest calculator - calculates %s%% annual interest based on base and time unit"),
    STEP_FUNCTION("step-function", "Step function calculator - base price %s PLN + increments every %s units"),
    DISCRETE_POINTS("discrete-points", "Discrete points calculator - price lookup from predefined points"),
    DAILY_INCREMENT("daily-increment", "Daily increment calculator - starts at %s, grows by %s per day (discrete)"),
    CONTINUOUS_LINEAR_TIME(
            "continuous-linear-time", "Continuous linear time calculator - interpolates between %s and %s"),
    COMPOSITE(
            "composite",
            "Composite function calculator - delegates to different calculators based on parameter ranges"),
    PERCENTAGE("percentage", "Percentage calculator - calculates %s%% of base amount"),
    CUSTOM("custom", "Application-defined calculator"),
    UNIT_TO_TOTAL_ADAPTER("unit-to-total-adapter", "Converts unit price to total: total = quantity × unitPrice"),
    UNIT_TO_MARGINAL_ADAPTER(
            "unit-to-marginal-adapter",
            "Converts unit price to marginal: for constant unit price, marginal = unitPrice"),
    TOTAL_TO_UNIT_ADAPTER("total-to-unit-adapter", "Converts total price to unit price: unitPrice = total / quantity"),
    TOTAL_TO_MARGINAL_ADAPTER(
            "total-to-marginal-adapter", "Converts total to marginal: marginal(n) = total(n) - total(n-1)"),
    MARGINAL_TO_TOTAL_ADAPTER(
            "marginal-to-total-adapter", "Converts marginal to total: total = Σ marginal(i) for i=1..quantity"),
    MARGINAL_TO_UNIT_ADAPTER(
            "marginal-to-unit-adapter", "Converts marginal to unit price: unitPrice = Σ marginal(i) / quantity");

    private final String typeName;
    private final String descriptionTemplate;

    CalculatorType(String typeName, String descriptionTemplate) {
        this.typeName = typeName;
        this.descriptionTemplate = descriptionTemplate;
    }

    public String getTypeName() {
        return typeName;
    }

    public String formatDescription(Object value) {
        return String.format(descriptionTemplate, value);
    }
}
