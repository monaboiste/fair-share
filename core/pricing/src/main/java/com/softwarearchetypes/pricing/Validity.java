package com.softwarearchetypes.pricing;

import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/** A pricing validity period with optional inclusive start and end timestamps. */
public record Validity(
        @Nullable LocalDateTime from, @Nullable LocalDateTime to) {

    public static final Validity ALWAYS_VALID = new Validity(null, null);

    public Validity {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }
    }

    /** Creates a period beginning on the given inclusive timestamp. */
    public static Validity from(LocalDateTime from) {
        return new Validity(from, null);
    }

    /** Creates a period ending on the given inclusive timestamp. */
    public static Validity until(LocalDateTime to) {
        return new Validity(null, to);
    }

    /** Creates a period with inclusive boundaries. */
    public static Validity between(LocalDateTime from, LocalDateTime to) {
        return new Validity(from, to);
    }

    public static Validity always() {
        return ALWAYS_VALID;
    }

    /** Returns whether the given timestamp falls within this period. */
    public boolean isValidAt(@Nullable LocalDateTime pointInTime) {
        if (pointInTime == null) {
            return false;
        }
        if (from != null && pointInTime.isBefore(from)) {
            return false;
        }
        return to == null || !pointInTime.isAfter(to);
    }

    public boolean hasExpired(@Nullable LocalDateTime pointInTime) {
        return pointInTime != null && to != null && pointInTime.isAfter(to);
    }

    public boolean hasNotStartedYet(@Nullable LocalDateTime pointInTime) {
        return pointInTime != null && from != null && pointInTime.isBefore(from);
    }

    public boolean overlaps(Validity other) {
        return (from == null || other.to == null || !from.isAfter(other.to))
                && (other.from == null || to == null || !other.from.isAfter(to));
    }
}
