package com.softwarearchetypes.scoring

import com.softwarearchetypes.scoring.algebra.AlgebraicVisitor
import com.softwarearchetypes.scoring.algebra.score.Score
import com.softwarearchetypes.scoring.algebra.score.ScoreAlgebra
import com.softwarearchetypes.scoring.ast.ComparisonOperator
import com.softwarearchetypes.scoring.ast.Expression
import com.softwarearchetypes.scoring.ast.Metric
import com.softwarearchetypes.scoring.context.MetricSource
import spock.lang.Specification

class MetricGeneralizationSpec extends Specification {

    private static final Metric ON_TIME_DELIVERY_RATE = Metric.of("on-time-delivery-rate")
    private static final Metric OPEN_DISPUTES = Metric.of("open-disputes")

    def "scores a domain unknown to the core"() {
        given:
        def supplier = metrics([
                (ON_TIME_DELIVERY_RATE): 0.98d,
                (OPEN_DISPUTES)        : 4d
        ])

        expect:
        supplierRule().accept(new AlgebraicVisitor<>(supplier, new ScoreAlgebra())) == new Score(20)
    }

    def "absent metrics score as zero"() {
        given:
        def nothingKnown = metrics([:])

        expect:
        supplierRule().accept(new AlgebraicVisitor<>(nothingKnown, new ScoreAlgebra())) == Score.ZERO
    }

    def "equal keys identify the same metric"() {
        expect:
        Metric.of("on-time-delivery-rate") == ON_TIME_DELIVERY_RATE
    }

    private static MetricSource metrics(Map<Metric, Double> values) {
        { metric -> values.getOrDefault(metric, 0d) } as MetricSource
    }

    private static Expression supplierRule() {
        new Expression.Sum([
                new Expression.IfThenElse(
                        new Expression.MetricComparison(
                                ON_TIME_DELIVERY_RATE,
                                ComparisonOperator.GT,
                                0.95d
                        ),
                        new Expression.ConstantScore(50),
                        new Expression.ConstantScore(0)
                ),
                new Expression.IfThenElse(
                        new Expression.MetricComparison(OPEN_DISPUTES, ComparisonOperator.GT, 3d),
                        new Expression.ConstantScore(-30),
                        new Expression.ConstantScore(0)
                )
        ])
    }
}
