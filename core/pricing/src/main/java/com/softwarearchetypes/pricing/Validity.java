package com.softwarearchetypes.pricing;

import java.time.LocalDateTime;

/**
 * Represents a pricing validity period with an inclusive start and exclusive end.
 *
 * <p>When periods overlap, the version with the latest {@code validFrom} takes precedence.
 */
public record Validity(LocalDateTime validFrom, LocalDateTime validTo) {

    public static final Validity ALWAYS_VALID = new Validity(LocalDateTime.MIN, LocalDateTime.MAX);

    public static Validity until(LocalDateTime validTo) {
        return new Validity(LocalDateTime.MIN, validTo);
    }

    public static Validity from(LocalDateTime validFrom) {
        return new Validity(validFrom, LocalDateTime.MAX);
    }

    public static Validity between(LocalDateTime validFrom, LocalDateTime validTo) {
        if (validFrom == null && validTo == null) {
            return ALWAYS_VALID;
        }
        if (validFrom == null) {
            return until(validTo);
        }
        if (validTo == null) {
            return from(validFrom);
        }
        if (!validFrom.isBefore(validTo)) {
            throw new IllegalArgumentException(
                    "validFrom must be before validTo: [%s, %s)".formatted(validFrom, validTo));
        }
        return new Validity(validFrom, validTo);
    }

    public static Validity always() {
        return ALWAYS_VALID;
    }

    public boolean isValidAt(LocalDateTime pointInTime) {
        return !pointInTime.isBefore(validFrom) && pointInTime.isBefore(validTo);
    }

    public boolean hasExpired(LocalDateTime pointInTime) {
        return !pointInTime.isBefore(validTo);
    }

    public boolean hasNotStartedYet(LocalDateTime pointInTime) {
        return pointInTime.isBefore(validFrom);
    }

    public boolean overlaps(Validity other) {
        return this.validFrom.isBefore(other.validTo) && other.validFrom.isBefore(this.validTo);
    }
}
