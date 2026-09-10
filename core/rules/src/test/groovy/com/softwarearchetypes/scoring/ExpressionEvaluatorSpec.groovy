package com.softwarearchetypes.scoring

import com.softwarearchetypes.scoring.algebra.AlgebraicVisitor
import com.softwarearchetypes.scoring.algebra.score.Score
import com.softwarearchetypes.scoring.algebra.score.ScoreAlgebra
import com.softwarearchetypes.scoring.algebra.score.simplified.ExpressionEvaluator
import com.softwarearchetypes.scoring.algebra.score.simplified.SimpleScoringAlgebra
import com.softwarearchetypes.scoring.ast.ComparisonOperator
import com.softwarearchetypes.scoring.ast.Expression
import com.softwarearchetypes.scoring.ast.Metric
import com.softwarearchetypes.scoring.customer.CustomerMetrics
import com.softwarearchetypes.scoring.customer.CustomerWindow
import spock.lang.Specification

class ExpressionEvaluatorSpec extends Specification {

    def "simplified algebra evaluates a customer window"() {
        given:
        def customerWindow = window([
                (CustomerMetrics.YEARLY_PURCHASE_AMOUNT)   : 20_000d,
                (CustomerMetrics.QUARTERLY_COMPLAINT_COUNT): 5d
        ])

        expect:
        ExpressionEvaluator.eval(
                yearlyAndQuarterlyRule(),
                customerWindow,
                new SimpleScoringAlgebra()
        ) == new Score(20)
    }

    def "visitor algebra evaluates a customer window"() {
        given:
        def customerWindow = window([
                (CustomerMetrics.YEARLY_PURCHASE_AMOUNT)   : 20_000d,
                (CustomerMetrics.QUARTERLY_COMPLAINT_COUNT): 5d
        ])

        expect:
        yearlyAndQuarterlyRule().accept(
                new AlgebraicVisitor<>(customerWindow, new ScoreAlgebra())
        ) == new Score(20)
    }

    private static CustomerWindow window(Map<Metric, Double> metrics) {
        new CustomerWindow("c-1", Instant.EPOCH, Instant.EPOCH, [], metrics)
    }

    private static Expression yearlyAndQuarterlyRule() {
        def highTurnover = new Expression.IfThenElse(
                new Expression.MetricComparison(
                        CustomerMetrics.YEARLY_PURCHASE_AMOUNT,
                        ComparisonOperator.GT,
                        10_000d
                ),
                new Expression.ConstantScore(50),
                new Expression.ConstantScore(0)
        )
        def tooManyComplaints = new Expression.IfThenElse(
                new Expression.MetricComparison(
                        CustomerMetrics.QUARTERLY_COMPLAINT_COUNT,
                        ComparisonOperator.GT,
                        3d
                ),
                new Expression.ConstantScore(-30),
                new Expression.ConstantScore(0)
        )
        new Expression.Sum([highTurnover, tooManyComplaints])
    }
}
