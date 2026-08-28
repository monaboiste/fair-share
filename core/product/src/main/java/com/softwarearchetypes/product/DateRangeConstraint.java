package com.softwarearchetypes.product;

import java.time.LocalDate;

/** Restricts dates to an inclusive range. */
record DateRangeConstraint(LocalDate from, LocalDate to) implements FeatureValueConstraint {

    DateRangeConstraint {
        if (from == null) {
            throw new IllegalArgumentException("From date must be defined");
        }
        if (to == null) {
            throw new IllegalArgumentException("To date must be defined");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }
    }

    static DateRangeConstraint between(String from, String to) {
        return new DateRangeConstraint(LocalDate.parse(from), LocalDate.parse(to));
    }

    @Override
    public FeatureValueType valueType() {
        return FeatureValueType.DATE;
    }

    @Override
    public String type() {
        return "DATE_RANGE";
    }

    @Override
    public boolean isValid(Object value) {
        if (!(value instanceof LocalDate dateValue)) {
            return false;
        }
        return !dateValue.isBefore(from) && !dateValue.isAfter(to);
    }

    @Override
    public String desc() {
        return "date between %s and %s".formatted(from, to);
    }
}
