package com.softwarearchetypes.scoring.algebra.bool;

import com.softwarearchetypes.scoring.algebra.Algebra;
import com.softwarearchetypes.scoring.ast.ComparisonOperator;
import com.softwarearchetypes.scoring.ast.Metric;
import com.softwarearchetypes.scoring.context.MetricSource;
import java.util.List;

public class BooleanAlgebra implements Algebra<Boolean> {

    @Override
    public Boolean and(Boolean left, Boolean right) {
        return left && right;
    }

    @Override
    public Boolean or(Boolean left, Boolean right) {
        return left || right;
    }

    @Override
    public Boolean not(Boolean inner) {
        return !inner;
    }

    @Override
    public Boolean metricCmp(MetricSource source, Metric metric, ComparisonOperator op, double value) {
        return op.compare(source.metric(metric), value);
    }

    @Override
    public Boolean constScore(int value) {
        return value != 0;
    }

    @Override
    public Boolean sum(List<Boolean> list) {
        return list.stream().anyMatch(Boolean::booleanValue);
    }

    @Override
    public Boolean ifThenElse(Boolean cond, Boolean thenValue, Boolean elseValue) {
        return cond ? thenValue : elseValue;
    }
}
