package com.softwarearchetypes.scoring.algebra.score;

import com.softwarearchetypes.scoring.algebra.Algebra;
import com.softwarearchetypes.scoring.ast.ComparisonOperator;
import com.softwarearchetypes.scoring.ast.Metric;
import com.softwarearchetypes.scoring.context.MetricSource;
import java.util.List;

public class ScoreAlgebra implements Algebra<Score> {

    @Override
    public Score and(Score left, Score right) {
        return new Score(Math.min(left.value(), right.value()));
    }

    @Override
    public Score or(Score left, Score right) {
        return new Score(Math.max(left.value(), right.value()));
    }

    @Override
    public Score not(Score inner) {
        return new Score(inner.value() > 0 ? 0 : 1);
    }

    @Override
    public Score metricCmp(MetricSource source, Metric metric, ComparisonOperator op, double value) {
        double metricValue = source.metric(metric);
        boolean matches = op.compare(metricValue, value);
        return new Score(matches ? 1 : 0);
    }

    @Override
    public Score constScore(int value) {
        return new Score(value);
    }

    @Override
    public Score sum(List<Score> children) {
        int total = 0;
        for (Score s : children) total += s.value();
        return new Score(total);
    }

    @Override
    public Score ifThenElse(Score cond, Score thenValue, Score elseValue) {
        return cond.value() > 0 ? thenValue : elseValue;
    }
}
