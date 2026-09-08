package com.softwarearchetypes.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents a range of values that maps to a specific calculator. Supports different value types: numeric
 * (BigDecimal), time (LocalTime), date (LocalDate).
 */
interface CalculatorRange {

    /**
     * Checks if this range supports the given value type.
     *
     * @param value the value to check
     * @return true if the value type is supported by this range
     */
    boolean supports(Object value);

    /**
     * Checks if the given value falls within this range.
     *
     * @param value the value to check
     * @return true if the value is within the range (inclusive start, exclusive end)
     */
    boolean contains(Object value);

    /** Returns the ID of the calculator to use for values in this range. */
    CalculatorId calculatorId();

    /**
     * Checks if this range is compatible with another range (same type).
     *
     * @param other the other range
     * @return true if both ranges are of the same type
     */
    boolean isCompatibleWith(CalculatorRange other);

    /**
     * Checks if this range overlaps with another range. Only compatible ranges can be checked for overlap.
     *
     * @param other the other range
     * @return true if the ranges overlap
     * @throws IllegalArgumentException if ranges are not compatible
     */
    boolean overlaps(CalculatorRange other);

    /**
     * Returns the range definition without the calculator ID.
     *
     * @return string representation of just the range interval (e.g., "[0, 1000)")
     */
    String describe();

    static NumericRange numeric(BigDecimal min, BigDecimal max, CalculatorId calculatorId) {
        return new NumericRange(min, max, calculatorId);
    }

    static TimeRange time(LocalTime from, LocalTime to, CalculatorId calculatorId) {
        return new TimeRange(from, to, calculatorId);
    }

    static DateRange date(LocalDate from, LocalDate to, CalculatorId calculatorId) {
        return new DateRange(from, to, calculatorId);
    }
}

/** Range for date values (LocalDate). Represents an interval [from, to) - inclusive from, exclusive to. */
record DateRange(LocalDate from, LocalDate to, CalculatorId calculatorId) implements CalculatorRange {

    public DateRange {
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("From must be before to: [%s, %s)".formatted(from, to));
        }
    }

    @Override
    public boolean supports(Object value) {
        return value instanceof LocalDate;
    }

    @Override
    public boolean contains(Object value) {
        if (!supports(value)) {
            return false;
        }
        LocalDate date = (LocalDate) value;
        return !date.isBefore(from) && date.isBefore(to);
    }

    @Override
    public boolean isCompatibleWith(CalculatorRange other) {
        return other instanceof DateRange;
    }

    @Override
    public boolean overlaps(CalculatorRange other) {
        if (!isCompatibleWith(other)) {
            throw new IllegalArgumentException("Cannot check overlap with incompatible range type: %s"
                    .formatted(other.getClass().getSimpleName()));
        }

        DateRange o = (DateRange) other;

        return !(to.compareTo(o.from) <= 0) && !(o.to.compareTo(from) <= 0);
    }

    @Override
    public String describe() {
        return "[%s, %s)".formatted(from, to);
    }

    @Override
    public String toString() {
        return "[%s, %s) → %s".formatted(from, to, calculatorId);
    }
}

/** Range for numeric values (BigDecimal). Represents an interval [min, max) - inclusive min, exclusive max. */
record NumericRange(BigDecimal min, BigDecimal max, CalculatorId calculatorId) implements CalculatorRange {

    public NumericRange {
        if (min.compareTo(max) >= 0) {
            throw new IllegalArgumentException("Min must be less than max: [%s, %s)".formatted(min, max));
        }
    }

    @Override
    public boolean supports(Object value) {
        return value instanceof BigDecimal;
    }

    @Override
    public boolean contains(Object value) {
        if (!supports(value)) {
            return false;
        }
        BigDecimal bd = (BigDecimal) value;
        return bd.compareTo(min) >= 0 && bd.compareTo(max) < 0;
    }

    @Override
    public boolean isCompatibleWith(CalculatorRange other) {
        return other instanceof NumericRange;
    }

    @Override
    public boolean overlaps(CalculatorRange other) {
        if (!isCompatibleWith(other)) {
            throw new IllegalArgumentException("Cannot check overlap with incompatible range type: %s"
                    .formatted(other.getClass().getSimpleName()));
        }

        NumericRange o = (NumericRange) other;

        return !(max.compareTo(o.min) <= 0) && !(o.max.compareTo(min) <= 0);
    }

    @Override
    public String describe() {
        return "[%s, %s)".formatted(min, max);
    }

    @Override
    public String toString() {
        return "[%s, %s) → %s".formatted(min, max, calculatorId);
    }
}

/**
 * Range for time values (LocalTime). Represents an interval [from, to) - inclusive from, exclusive to. Supports ranges
 * that cross midnight (e.g., 22:00-06:00).
 */
record TimeRange(LocalTime from, LocalTime to, CalculatorId calculatorId) implements CalculatorRange {

    @Override
    public boolean supports(Object value) {
        return value instanceof LocalTime;
    }

    @Override
    public boolean contains(Object value) {
        if (!supports(value)) {
            return false;
        }
        LocalTime time = (LocalTime) value;

        if (from.isBefore(to)) {
            return !time.isBefore(from) && time.isBefore(to);
        } else {
            return !time.isBefore(from) || time.isBefore(to);
        }
    }

    @Override
    public boolean isCompatibleWith(CalculatorRange other) {
        return other instanceof TimeRange;
    }

    @Override
    public boolean overlaps(CalculatorRange other) {
        if (!isCompatibleWith(other)) {
            throw new IllegalArgumentException("Cannot check overlap with incompatible range type: %s"
                    .formatted(other.getClass().getSimpleName()));
        }

        TimeRange o = (TimeRange) other;

        boolean thisNormal = from.isBefore(to);
        boolean otherNormal = o.from.isBefore(o.to);

        if (thisNormal && otherNormal) {
            return !(to.compareTo(o.from) <= 0) && !(o.to.compareTo(from) <= 0);
        }

        if (!thisNormal && otherNormal) {
            boolean otherInGap = o.from.compareTo(to) >= 0 && o.to.compareTo(from) <= 0;
            return !otherInGap;
        }

        if (thisNormal && !otherNormal) {
            boolean thisInGap = from.compareTo(o.to) >= 0 && to.compareTo(o.from) <= 0;
            return !thisInGap;
        }

        return true;
    }

    @Override
    public String describe() {
        if (from.isBefore(to)) {
            return "[%s, %s)".formatted(from, to);
        } else {
            return "[%s, %s) (crosses midnight)".formatted(from, to);
        }
    }

    @Override
    public String toString() {
        if (from.isBefore(to)) {
            return "[%s, %s) → %s".formatted(from, to, calculatorId);
        } else {
            return "[%s, %s) (crosses midnight) → %s".formatted(from, to, calculatorId);
        }
    }
}
