package com.softwarearchetypes.product;

import java.time.LocalDate;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** A period with optional inclusive start and end dates. */
public class Validity {

    private final LocalDate from;
    private final LocalDate to;

    private Validity(@Nullable LocalDate from, @Nullable LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }
        this.from = from;
        this.to = to;
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

    public @Nullable LocalDate from() {
        return from;
    }

    public @Nullable LocalDate to() {
        return to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Validity validity)) {
            return false;
        }
        return Objects.equals(from, validity.from) && Objects.equals(to, validity.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
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
