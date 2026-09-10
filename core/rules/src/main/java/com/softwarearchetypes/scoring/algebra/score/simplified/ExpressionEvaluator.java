package com.softwarearchetypes.scoring.algebra.score.simplified;

import com.softwarearchetypes.scoring.algebra.score.Score;
import com.softwarearchetypes.scoring.ast.ComparisonOperator;
import com.softwarearchetypes.scoring.ast.Expression;
import com.softwarearchetypes.scoring.ast.Metric;
import com.softwarearchetypes.scoring.context.MetricSource;
import java.util.ArrayList;
import java.util.List;

public final class ExpressionEvaluator {

    private ExpressionEvaluator() {}

    public static Score eval(Expression expr, MetricSource metricSource, ScoringAlgebra algebra) {
        return switch (expr) {
            case Expression.And(Expression lhs, Expression rhs) ->
                algebra.and(eval(lhs, metricSource, algebra), eval(rhs, metricSource, algebra));

            case Expression.Or(Expression lhs, Expression rhs) ->
                algebra.or(eval(lhs, metricSource, algebra), eval(rhs, metricSource, algebra));

            case Expression.Not(Expression innerExpr) -> algebra.not(eval(innerExpr, metricSource, algebra));

            case Expression.MetricComparison(Metric metric, ComparisonOperator op, double value) ->
                algebra.metricCmp(metricSource, metric, op, value);

            case Expression.ConstantScore(int value) -> algebra.constScore(value);

            case Expression.Sum(List<Expression> children) -> {
                List<Score> scores = new ArrayList<>();
                for (Expression child : children) {
                    scores.add(eval(child, metricSource, algebra));
                }
                yield algebra.sum(scores);
            }

            case Expression.IfThenElse(Expression condition, Expression thenBranch, Expression elseBranch) ->
                algebra.ifThenElse(
                        eval(condition, metricSource, algebra),
                        eval(thenBranch, metricSource, algebra),
                        eval(elseBranch, metricSource, algebra));

            default -> throw new IllegalArgumentException("Unknown Expr type: " + expr.getClass());
        };
    }
}
