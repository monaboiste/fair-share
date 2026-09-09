package com.softwarearchetypes.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import org.jspecify.annotations.Nullable;

/** Maps values in an interval to a calculator. Numeric, time, and date ranges are supported. */
public interface CalculatorRange {

    /**
     * Returns whether this range supports the given value type.
     *
     * @param value the value to check
     * @return {@code true} when this range supports the value type
     */
    boolean supports(Object value);

    /**
     * Returns whether the given value falls within this range.
     *
     * @param value the value to check
     * @return {@code true} when the value is within the range (inclusive start, exclusive end)
     */
    boolean contains(Object value);

    /** Returns the ID of the calculator to use for values in this range. */
    CalculatorId calculatorId();

    /**
     * Returns whether this range is compatible with another range (same type).
     *
     * @param other the other range
     * @return {@code true} when both ranges are of the same type
     */
    boolean isCompatibleWith(CalculatorRange other);

    /**
     * Returns whether this range overlaps with another range. Only compatible ranges can be checked for overlap.
     *
     * @param other the other range
     * @return {@code true} when the ranges overlap
     * @throws IllegalArgumentException when ranges are not compatible
     */
    boolean overlaps(CalculatorRange other);

    /**
     * Returns the range definition without the calculator ID.
     *
     * @return string representation of just the range interval (e.g., "[0, 1000)")
     */
    String describe();

    static CalculatorRange numeric(BigDecimal min, BigDecimal max, CalculatorId calculatorId) {
        return new NumericRange(min, max, calculatorId);
    }

    static CalculatorRange time(LocalTime from, LocalTime to, CalculatorId calculatorId) {
        return new TimeRange(from, to, calculatorId);
    }

    static CalculatorRange date(LocalDate from, LocalDate to, CalculatorId calculatorId) {
        return new DateRange(from, to, calculatorId);
    }
}

/** A date range represented by the half-open interval {@code [from, to)}. */
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

        return to.isAfter(o.from) && o.to.isAfter(from);
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

/** A numeric range represented by the half-open interval {@code [min, max)}. */
record NumericRange(BigDecimal min, BigDecimal max, CalculatorId calculatorId) implements CalculatorRange {

    public NumericRange {
        if (min.compareTo(max) >= 0) {
            throw new IllegalArgumentException("Min must be less than max: [%s, %s)".formatted(min, max));
        }
    }

    @Override
    public boolean supports(@Nullable Object value) {
        return value instanceof BigDecimal;
    }

    @Override
    public boolean contains(Object value) {
        if (!supports(value)) {
            return false;
        }
        BigDecimal bigDecimal = (BigDecimal) value;
        return bigDecimal.compareTo(min) >= 0 && bigDecimal.compareTo(max) < 0;
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

        return max.compareTo(o.min) > 0 && o.max.compareTo(min) > 0;
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

/** A time range represented by {@code [from, to)}; it may cross midnight. */
record TimeRange(LocalTime from, LocalTime to, CalculatorId calculatorId) implements CalculatorRange {

    @Override
    public boolean supports(@Nullable Object value) {
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
            return to.isAfter(o.from) && o.to.isAfter(from);
        }

        if (!thisNormal && otherNormal) {
            boolean otherInGap = !o.from.isBefore(to) && !o.to.isAfter(from);
            return !otherInGap;
        }

        if (thisNormal) {
            boolean thisInGap = !from.isBefore(o.to) && !to.isAfter(o.from);
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
