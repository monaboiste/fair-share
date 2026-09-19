package com.softwarearchetypes.pricing.calculation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface Calculator {

    PricingResult calculate(Parameters parameters);

    String describe();

    String formula();

    /**
     * Simulates calculation for multiple points in parameter space.
     *
     * @param points list of parameter sets to evaluate
     * @return map from each parameter set to its calculated price
     */
    default Map<Parameters, PricingResult> simulate(List<Parameters> points) {
        Map<Parameters, PricingResult> results = new LinkedHashMap<>();
        for (Parameters params : points) {
            results.put(params, calculate(params));
        }
        return results;
    }

    CalculatorId getId();

    String name();
}
