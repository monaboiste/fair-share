package com.softwarearchetypes.scoring

import com.softwarearchetypes.scoring.algebra.AlgebraicVisitor
import com.softwarearchetypes.scoring.algebra.bool.BooleanAlgebra
import com.softwarearchetypes.scoring.algebra.score.Score
import com.softwarearchetypes.scoring.algebra.score.ScoreAlgebra
import com.softwarearchetypes.scoring.ast.ComparisonOperator
import com.softwarearchetypes.scoring.ast.Expression
import com.softwarearchetypes.scoring.ast.Metric
import com.softwarearchetypes.scoring.context.MetricSource
import spock.lang.Specification

class BooleanAlgebraSpec extends Specification {

    private static final Metric AMOUNT = Metric.of("AMOUNT")
    private static final MetricSource SOURCE = { metric -> metric == AMOUNT ? 1500d : 0d } as MetricSource

    def "compares a metric without converting it to points"() {
        expect:
        evaluate(above(1000))
        !evaluate(above(2000))
    }

    def "boolean operators obey their usual truth tables"() {
        expect:
        evaluate(new Expression.And(above(1000), above(1400)))
        !evaluate(new Expression.And(above(1000), above(2000)))
        evaluate(new Expression.Or(above(2000), above(1000)))
        !evaluate(new Expression.Or(above(2000), above(3000)))
        !evaluate(new Expression.Not(above(1000)))
    }

    def "if then else chooses the matching branch"() {
        expect:
        evaluate(new Expression.IfThenElse(above(1000), above(1400), above(9000)))
        evaluate(new Expression.IfThenElse(above(9000), above(1400), above(1000)))
    }

    def "sum acts as disjunction"() {
        expect:
        evaluate(new Expression.Sum([above(9000), above(1000)]))
        !evaluate(new Expression.Sum([above(9000), above(8000)]))
        !evaluate(new Expression.Sum([]))
    }

    def "constant is false only when zero"() {
        expect:
        evaluate(new Expression.ConstantScore(50))
        evaluate(new Expression.ConstantScore(-30))
        !evaluate(new Expression.ConstantScore(0))
    }

    def "negative points remain true in boolean filtering"() {
        given:
        Expression filter = new Expression.Not(new Expression.ConstantScore(-30))

        when:
        Score scoreInterpretation = filter.accept(new AlgebraicVisitor<>(SOURCE, new ScoreAlgebra()))

        then:
        scoreInterpretation == new Score(1)
        scoreInterpretation.value() > 0
        !evaluate(filter)
    }

    private static Expression above(double threshold) {
        new Expression.MetricComparison(AMOUNT, ComparisonOperator.GT, threshold)
    }

    private static boolean evaluate(Expression expression) {
        expression.accept(new AlgebraicVisitor<>(SOURCE, new BooleanAlgebra()))
    }
}
