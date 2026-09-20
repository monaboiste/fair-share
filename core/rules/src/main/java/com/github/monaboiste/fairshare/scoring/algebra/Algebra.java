package com.github.monaboiste.fairshare.scoring.algebra;

import com.github.monaboiste.fairshare.scoring.ast.ComparisonOperator;
import com.github.monaboiste.fairshare.scoring.ast.Metric;
import com.github.monaboiste.fairshare.scoring.context.MetricSource;
import java.util.List;

public interface Algebra<R> {

    R or(R left, R right);

    R and(R left, R right);

    R not(R inner);

    R metricCmp(MetricSource source, Metric metric, ComparisonOperator op, double value);

    R constScore(int value);

    R sum(List<R> list);

    R ifThenElse(R cond, R thenValue, R elseValue);

    default R label(String label, R inner) {
        return inner;
    }
}
