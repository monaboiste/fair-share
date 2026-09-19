package com.softwarearchetypes.pricing.component;

import com.softwarearchetypes.pricing.calculation.Parameters;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Determines whether a pricing component applies to parameters. */
public interface ApplicabilityConstraint {
    boolean isSatisfiedBy(Parameters parameters);

    static ApplicabilityConstraint alwaysTrue() {
        return new AlwaysTrueConstraint();
    }

    static ApplicabilityConstraint equalsTo(String parameterName, String expectedValue) {
        return new EqualsConstraint(parameterName, expectedValue);
    }

    static ApplicabilityConstraint in(String parameterName, Set<String> allowedValues) {
        return new InConstraint(parameterName, allowedValues);
    }

    static ApplicabilityConstraint in(String parameterName, String... allowedValues) {
        return new InConstraint(parameterName, Set.of(allowedValues));
    }

    static ApplicabilityConstraint greaterThan(String parameterName, int threshold) {
        return greaterThan(parameterName, BigDecimal.valueOf(threshold));
    }

    static ApplicabilityConstraint greaterThan(String parameterName, BigDecimal threshold) {
        return new GreaterThanConstraint(parameterName, threshold);
    }

    static ApplicabilityConstraint greaterThanOrEqualTo(String parameterName, int threshold) {
        return greaterThanOrEqualTo(parameterName, BigDecimal.valueOf(threshold));
    }

    static ApplicabilityConstraint greaterThanOrEqualTo(String parameterName, BigDecimal threshold) {
        return new GreaterThanOrEqualConstraint(parameterName, threshold);
    }

    static ApplicabilityConstraint lessThan(String parameterName, int threshold) {
        return lessThan(parameterName, BigDecimal.valueOf(threshold));
    }

    static ApplicabilityConstraint lessThan(String parameterName, BigDecimal threshold) {
        return new LessThanConstraint(parameterName, threshold);
    }

    static ApplicabilityConstraint lessThanOrEqualTo(String parameterName, int threshold) {
        return lessThanOrEqualTo(parameterName, BigDecimal.valueOf(threshold));
    }

    static ApplicabilityConstraint lessThanOrEqualTo(String parameterName, BigDecimal threshold) {
        return new LessThanOrEqualConstraint(parameterName, threshold);
    }

    static ApplicabilityConstraint between(String parameterName, int min, int max) {
        return between(parameterName, BigDecimal.valueOf(min), BigDecimal.valueOf(max));
    }

    static ApplicabilityConstraint between(String parameterName, BigDecimal min, BigDecimal max) {
        return new BetweenConstraint(parameterName, min, max);
    }

    static ApplicabilityConstraint and(ApplicabilityConstraint... constraints) {
        return new AndConstraint(Arrays.asList(constraints));
    }

    static ApplicabilityConstraint or(ApplicabilityConstraint... constraints) {
        return new OrConstraint(Arrays.asList(constraints));
    }

    static ApplicabilityConstraint not(ApplicabilityConstraint constraint) {
        return new NotConstraint(constraint);
    }

    static Optional<String> value(Parameters parameters, String name) {
        Object value = parameters.get(name);
        return value instanceof String s
                ? Optional.of(s)
                : value instanceof BigDecimal bd
                        ? Optional.of(bd.toPlainString())
                        : value instanceof Number n ? Optional.of(n.toString()) : Optional.empty();
    }

    static boolean numeric(Parameters p, String n, BigDecimal t, int sign) {
        return value(p, n)
                .map(v -> {
                    try {
                        return new BigDecimal(v).compareTo(t) * sign > 0;
                    } catch (NumberFormatException _) {
                        return false;
                    }
                })
                .orElse(false);
    }
}

record EqualsConstraint(String parameterName, String expectedValue) implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(Parameters p) {
        return ApplicabilityConstraint.value(p, parameterName)
                .map(expectedValue::equals)
                .orElse(false);
    }
}

record InConstraint(String parameterName, Set<String> allowedValues) implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(Parameters p) {
        return ApplicabilityConstraint.value(p, parameterName)
                .map(allowedValues::contains)
                .orElse(false);
    }
}

record GreaterThanConstraint(String parameterName, BigDecimal threshold) implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(Parameters p) {
        return ApplicabilityConstraint.numeric(p, parameterName, threshold, 1);
    }
}

record GreaterThanOrEqualConstraint(String parameterName, BigDecimal threshold) implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(Parameters p) {
        return ApplicabilityConstraint.value(p, parameterName)
                .map(v -> {
                    try {
                        return new BigDecimal(v).compareTo(threshold) >= 0;
                    } catch (NumberFormatException _) {
                        return false;
                    }
                })
                .orElse(false);
    }
}

record LessThanConstraint(String parameterName, BigDecimal threshold) implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(Parameters p) {
        return ApplicabilityConstraint.value(p, parameterName)
                .map(v -> {
                    try {
                        return new BigDecimal(v).compareTo(threshold) < 0;
                    } catch (NumberFormatException _) {
                        return false;
                    }
                })
                .orElse(false);
    }
}

record LessThanOrEqualConstraint(String parameterName, BigDecimal threshold) implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(Parameters p) {
        return ApplicabilityConstraint.value(p, parameterName)
                .map(v -> {
                    try {
                        return new BigDecimal(v).compareTo(threshold) <= 0;
                    } catch (NumberFormatException _) {
                        return false;
                    }
                })
                .orElse(false);
    }
}

record BetweenConstraint(String parameterName, BigDecimal min, BigDecimal max) implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(Parameters p) {
        return ApplicabilityConstraint.value(p, parameterName)
                .map(v -> {
                    try {
                        BigDecimal n = new BigDecimal(v);
                        return n.compareTo(min) >= 0 && n.compareTo(max) <= 0;
                    } catch (NumberFormatException _) {
                        return false;
                    }
                })
                .orElse(false);
    }
}

record AndConstraint(List<ApplicabilityConstraint> constraints) implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(Parameters p) {
        return constraints.stream().allMatch(c -> c.isSatisfiedBy(p));
    }
}

record OrConstraint(List<ApplicabilityConstraint> constraints) implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(Parameters p) {
        return constraints.stream().anyMatch(c -> c.isSatisfiedBy(p));
    }
}

record NotConstraint(ApplicabilityConstraint constraint) implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(Parameters p) {
        return !constraint.isSatisfiedBy(p);
    }
}

record AlwaysTrueConstraint() implements ApplicabilityConstraint {
    @Override
    public boolean isSatisfiedBy(Parameters p) {
        return true;
    }
}
