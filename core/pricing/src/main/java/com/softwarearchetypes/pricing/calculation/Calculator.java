package com.softwarearchetypes.pricing.calculation;

import com.softwarearchetypes.quantity.money.Money;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface Calculator {

    static PricingResult result(Interpretation interpretation, Money money) {
        return switch (interpretation) {
            case TOTAL -> new TotalPrice(money);
            case UNIT -> new UnitPrice(money);
            case MARGINAL -> new MarginalPrice(money);
        };
    }

    default PricingResult calculate(Parameters parameters) {
        return calculateWithValidInputs(parameters);
    }

    PricingResult calculateWithValidInputs(Parameters parameters);

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
