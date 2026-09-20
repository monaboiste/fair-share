package com.github.monaboiste.fairshare.scoring.algebra.score.simplified;

import com.github.monaboiste.fairshare.scoring.algebra.score.Score;
import com.github.monaboiste.fairshare.scoring.ast.ComparisonOperator;
import com.github.monaboiste.fairshare.scoring.ast.Metric;
import com.github.monaboiste.fairshare.scoring.context.MetricSource;
import java.util.List;

public interface ScoringAlgebra {

    Score and(Score a, Score b);

    Score or(Score a, Score b);

    Score not(Score a);

    Score metricCmp(MetricSource source, Metric metric, ComparisonOperator op, double value);

    Score constScore(int value);

    Score sum(List<Score> children);

    Score ifThenElse(Score cond, Score thenScore, Score elseScore);
}
