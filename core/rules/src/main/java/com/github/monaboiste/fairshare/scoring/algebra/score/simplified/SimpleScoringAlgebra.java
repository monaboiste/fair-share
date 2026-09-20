package com.github.monaboiste.fairshare.scoring.algebra.score.simplified;

import com.github.monaboiste.fairshare.scoring.algebra.score.Score;
import com.github.monaboiste.fairshare.scoring.ast.ComparisonOperator;
import com.github.monaboiste.fairshare.scoring.ast.Metric;
import com.github.monaboiste.fairshare.scoring.context.MetricSource;
import java.util.List;

public class SimpleScoringAlgebra implements ScoringAlgebra {

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
        int value = inner.value();
        return value <= 0 ? new Score(1) : new Score(0);
    }

    @Override
    public Score metricCmp(MetricSource source, Metric metric, ComparisonOperator op, double value) {
        double metricValue = source.metric(metric);
        boolean result = op.compare(metricValue, value);
        return new Score(result ? 1 : 0);
    }

    @Override
    public Score constScore(int value) {
        return new Score(value);
    }

    @Override
    public Score sum(List<Score> children) {
        int total = 0;
        for (Score s : children) {
            total += s.value();
        }
        return new Score(total);
    }

    @Override
    public Score ifThenElse(Score cond, Score thenScore, Score elseScore) {
        return cond.value() > 0 ? thenScore : elseScore;
    }
}
