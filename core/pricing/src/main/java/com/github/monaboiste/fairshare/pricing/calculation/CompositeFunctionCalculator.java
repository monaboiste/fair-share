package com.github.monaboiste.fairshare.pricing.calculation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Selects a calculator based on a numeric, time, or date range. */
record CompositeFunctionCalculator(
        CalculatorId id, String name, Ranges ranges, Map<CalculatorId, Calculator> calculators) implements Calculator {

    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\R");

    public CompositeFunctionCalculator(String name, Ranges ranges, Collection<Calculator> calculators) {
        this(CalculatorId.generate(), name, ranges, calculatorMap(calculators));
    }

    public CompositeFunctionCalculator {
        calculators = Collections.unmodifiableMap(new LinkedHashMap<>(calculators));
        validateCalculators(ranges, calculators);
    }

    private static Map<CalculatorId, Calculator> calculatorMap(Collection<Calculator> calculators) {
        Map<CalculatorId, Calculator> byId = new LinkedHashMap<>();
        calculators.forEach(calculator -> byId.put(calculator.getId(), calculator));
        return byId;
    }

    private static void validateCalculators(Ranges ranges, Map<CalculatorId, Calculator> calculators) {
        var calculatorIds = ranges.toList().stream()
                .map(CalculatorRange::calculatorId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        var missingIds = calculatorIds.stream()
                .filter(id -> !calculators.containsKey(id))
                .toList();
        if (!missingIds.isEmpty()) {
            throw new IllegalArgumentException("Calculators not found in repository: %s".formatted(missingIds));
        }
    }

    @Override
    public PricingResult calculate(Parameters parameters) {
        CalculatorRange matchingRange = ranges.findMatching(parameters)
                .orElseThrow(() -> new IllegalArgumentException("No matching range found in %s".formatted(ranges)));
        return calculators.get(matchingRange.calculatorId()).calculate(parameters);
    }

    @Override
    public String describe() {
        return "Composite function calculator: %s".formatted(ranges);
    }

    @Override
    public String formula() {
        StringBuilder sb = new StringBuilder("f(x) = piecewise function:%n".formatted());

        ranges.toList().forEach(range -> {
            Calculator calculator = calculators.get(range.calculatorId());

            sb.append("  %s → %s: %s%n"
                    .formatted(
                            range.describe(),
                            calculator.name(),
                            LINE_BREAK_PATTERN.matcher(calculator.formula()).replaceAll(" ")));
        });

        return sb.toString().trim();
    }

    @Override
    public CalculatorId getId() {
        return id;
    }
}
