package com.github.monaboiste.fairshare.pricing.component;

import com.github.monaboiste.fairshare.pricing.calculation.Calculator;
import com.github.monaboiste.fairshare.pricing.calculation.Interpretation;
import com.github.monaboiste.fairshare.pricing.calculation.Parameters;
import java.util.Locale;

/** Utility for wrapping calculators with interpretation adapters. */
class InterpretationAdapters {
    private InterpretationAdapters() {}

    static Calculator adapt(Calculator calc, Interpretation target, Parameters parameters, String suffix) {
        Interpretation source = calc.calculate(parameters).interpretation();
        if (source == target) return calc;
        return switch (target) {
            case TOTAL ->
                source == Interpretation.UNIT
                        ? UnitToTotalAdapter.wrap(calc.name() + suffix, calc)
                        : MarginalToTotalAdapter.wrap(calc.name() + suffix, calc);
            case UNIT ->
                source == Interpretation.TOTAL
                        ? TotalToUnitAdapter.wrap(calc.name() + suffix, calc)
                        : MarginalToUnitAdapter.wrap(calc.name() + suffix, calc);
            case MARGINAL ->
                source == Interpretation.UNIT
                        ? UnitToMarginalAdapter.wrap(calc.name() + suffix, calc)
                        : TotalToMarginalAdapter.wrap(calc.name() + suffix, calc);
        };
    }

    static Calculator adapt(Calculator calc, Interpretation target, Parameters parameters) {
        return adapt(calc, target, parameters, "-to-" + target.name().toLowerCase(Locale.ROOT));
    }
}
