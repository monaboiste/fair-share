package com.softwarearchetypes.pricing;

import java.util.List;
import java.util.Optional;

/**
 * Aggregate of CalculatorRange objects with validation. Knows which parameter to check and ensures all ranges are
 * compatible (same type) and non-overlapping.
 */
record Ranges(String rangeSelector, List<CalculatorRange> ranges) {

    @SuppressWarnings("ConstantValue")
    public Ranges {
        if (rangeSelector == null || rangeSelector.isBlank()) {
            throw new IllegalArgumentException("Range selector cannot be null or blank");
        }
        ranges = List.copyOf(ranges);
        validate(ranges);
    }

    /**
     * Factory method for convenient creation of Ranges.
     *
     * @param rangeSelector the parameter name to check
     * @param ranges the calculator ranges (varargs)
     * @return new Ranges instance
     */
    public static Ranges of(String rangeSelector, CalculatorRange... ranges) {
        return new Ranges(rangeSelector, List.of(ranges));
    }

    private void validate(List<CalculatorRange> ranges) {
        if (ranges.isEmpty()) {
            throw new IllegalArgumentException("Ranges cannot be empty");
        }

        validateCompatibility(ranges);
        validateNoOverlaps(ranges);
    }

    /** Validates that all ranges are compatible (same type). */
    private void validateCompatibility(List<CalculatorRange> ranges) {
        if (ranges.size() < 2) {
            return;
        }

        CalculatorRange first = ranges.getFirst();

        for (int i = 1; i < ranges.size(); i++) {
            CalculatorRange current = ranges.get(i);
            if (!first.isCompatibleWith(current)) {
                throw new IllegalArgumentException(
                        "All ranges must be of the same type. Found incompatible types: %s and %s"
                                .formatted(
                                        first.getClass().getSimpleName(),
                                        current.getClass().getSimpleName()));
            }
        }
    }

    /** Validates that no two ranges overlap. */
    private void validateNoOverlaps(List<CalculatorRange> ranges) {
        for (int i = 0; i < ranges.size(); i++) {
            for (int j = i + 1; j < ranges.size(); j++) {
                CalculatorRange rangeI = ranges.get(i);
                CalculatorRange rangeJ = ranges.get(j);

                if (rangeI.overlaps(rangeJ)) {
                    throw new IllegalArgumentException(
                            "Ranges cannot overlap: %s overlaps with %s".formatted(rangeI, rangeJ));
                }
            }
        }
    }

    /**
     * Returns the typed input descriptor for this range selector.
     *
     * @return the selector descriptor
     */
    CalculatorInput<?> selectorInput() {
        CalculatorRange range = ranges.getFirst();
        if (range instanceof NumericRange) {
            return CalculatorInput.bigDecimal(rangeSelector);
        }
        if (range instanceof DateRange) {
            return CalculatorInput.localDate(rangeSelector);
        }
        return CalculatorInput.instanceOf(rangeSelector, java.time.LocalTime.class);
    }

    /**
     * Finds the first range that contains the converted selector value.
     *
     * @param parameters the parameters containing the selector value
     * @return the matching range, or empty if no range matches
     */
    public Optional<CalculatorRange> findMatching(Parameters parameters) {
        if (!parameters.contains(rangeSelector)) {
            throw new IllegalArgumentException(
                    "Parameter '%s' is required but not found in parameters".formatted(rangeSelector));
        }
        Object value = selectorInput().read(parameters);

        if (value == null) {
            throw new IllegalArgumentException(
                    "Parameter '%s' is required but not found in parameters".formatted(rangeSelector));
        }

        return ranges.stream().filter(range -> range.contains(value)).findFirst();
    }

    /** Returns the number of ranges. */
    public int size() {
        return ranges.size();
    }

    /** Returns all ranges as a list. */
    public List<CalculatorRange> toList() {
        return ranges;
    }

    @Override
    public String toString() {
        return "Ranges[selector='%s', ranges=%s]".formatted(rangeSelector, ranges);
    }
}
