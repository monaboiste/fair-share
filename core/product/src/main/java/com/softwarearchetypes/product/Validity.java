package com.softwarearchetypes.product;

import java.time.LocalDate;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** A period with optional inclusive start and end dates. */
public record Validity(@Nullable LocalDate from, @Nullable LocalDate to) {

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
        return new Validity(null, null);
    }

    /** Returns whether the given date falls within this period. */
    public boolean isValidAt(@Nullable LocalDate date) {
        if (date == null) {
            return false;
        }
        if (from != null && date.isBefore(from)) {
            return false;
        }
        if (to != null && date.isAfter(to)) {
            return false;
        }
        return true;
    }

    @Override
    @NonNull public String toString() {
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
