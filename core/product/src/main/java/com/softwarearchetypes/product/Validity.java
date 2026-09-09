package com.softwarearchetypes.product;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/** A period with optional inclusive start and end dates. */
public record Validity(@Nullable LocalDate from, @Nullable LocalDate to) {

    private static final Validity ALWAYS_VALID = new Validity(null, null);

    public Validity {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }
    }

    /** Creates a period beginning on the given inclusive date. */
    public static Validity from(LocalDate from) {
        return new Validity(from, null);
    }

    /** Creates a period ending on the given inclusive date. */
    public static Validity until(LocalDate to) {
        return new Validity(null, to);
    }

    /** Creates a period with inclusive boundaries. */
    public static Validity between(LocalDate from, LocalDate to) {
        return new Validity(from, to);
    }

    /** Creates an unbounded period. */
    public static Validity always() {
        return ALWAYS_VALID;
    }

    /** Returns whether the given date falls within this period. */
    public boolean isValidAt(@Nullable LocalDate date) {
        if (date == null) {
            return false;
        }
        if (from != null && date.isBefore(from)) {
            return false;
        }
        return to == null || !date.isAfter(to);
    }

    @Override
    public String toString() {
        if (from == null && to == null) {
            return "always";
        }
        if (from == null) {
            return "until " + to;
        }
        if (to == null) {
            return "from " + from;
        }
        return from + " to " + to;
    }
}
