package com.softwarearchetypes.scoring

import com.softwarearchetypes.scoring.algebra.bool.BooleanAlgebra
import com.softwarearchetypes.scoring.algebra.explained.Contribution
import com.softwarearchetypes.scoring.algebra.explained.ExplainableAlgebra
import com.softwarearchetypes.scoring.algebra.explained.ExplainedScore
import com.softwarearchetypes.scoring.algebra.explained.ExplainedScoreMonoid
import com.softwarearchetypes.scoring.algebra.score.Score
import com.softwarearchetypes.scoring.algebra.score.ScoreAlgebra
import com.softwarearchetypes.scoring.algebra.score.ScoreMonoid
import com.softwarearchetypes.scoring.ast.ComparisonOperator
import com.softwarearchetypes.scoring.ast.EventRule
import com.softwarearchetypes.scoring.ast.Expression
import com.softwarearchetypes.scoring.ast.Metric
import com.softwarearchetypes.scoring.customer.CustomerEvent
import com.softwarearchetypes.scoring.customer.CustomerEventMetrics
import com.softwarearchetypes.scoring.customer.CustomerMetrics
import com.softwarearchetypes.scoring.customer.CustomerWindow
import spock.lang.Specification

class EventRuleEngineSpec extends Specification {

    private final EventRuleEngine<Score> engine =
            new EventRuleEngine<>(new BooleanAlgebra(), new ScoreAlgebra(), new ScoreMonoid())

    def "scores every event matching its own amount"() {
        expect:
        engine.evaluateRules([pointsPerLargePurchase()], window(1500, 500, 2000)) == new Score(6)
    }

    def "scores nothing when the filter does not match"() {
        given:
        def neverApplies = new EventRule(
                new Expression.MetricComparison(
                        CustomerEventMetrics.AMOUNT,
                        ComparisonOperator.GT,
                        100_000d
                ),
                new Expression.ConstantScore(3)
        )

        expect:
        engine.evaluateRules([neverApplies], window(1500, 2000)) == Score.ZERO
    }

    def "only matching rules contribute"() {
        given:
        def smallPurchases = new EventRule(
                new Expression.MetricComparison(CustomerEventMetrics.AMOUNT, ComparisonOperator.LT, 100d),
                new Expression.ConstantScore(50)
        )

        expect:
        engine.evaluateRules([pointsPerLargePurchase(), smallPurchases], window(1500)) == new Score(3)
    }

    def "combines event and window metrics in one filter"() {
        given:
        def loyalCustomerBonus = new EventRule(
                new Expression.And(
                        new Expression.MetricComparison(
                                CustomerEventMetrics.AMOUNT,
                                ComparisonOperator.GT,
                                1000d
                        ),
                        new Expression.MetricComparison(
                                CustomerMetrics.YEARLY_PURCHASE_AMOUNT,
                                ComparisonOperator.GT,
                                10_000d
                        )
                ),
                new Expression.ConstantScore(3)
        )

        expect:
        engine.evaluateRules([loyalCustomerBonus], window(1500)) == new Score(3)
        engine.evaluateRules([loyalCustomerBonus], poorWindow(1500)) == Score.ZERO
    }

    def "filters events by type"() {
        given:
        def largePurchases = new EventRule(
                new Expression.And(
                        new Expression.MetricComparison(
                                CustomerEventMetrics.typeIs("PURCHASE"),
                                ComparisonOperator.EQ,
                                1d
                        ),
                        new Expression.MetricComparison(
                                CustomerEventMetrics.AMOUNT,
                                ComparisonOperator.GT,
                                1000d
                        )
                ),
                new Expression.ConstantScore(3)
        )
        def customerWindow = mixedWindow(
                new CustomerEvent("PURCHASE", Instant.EPOCH, 1500),
                new CustomerEvent("REFUND", Instant.EPOCH, 2000),
                new CustomerEvent("PURCHASE", Instant.EPOCH, 500)
        )

        expect:
        engine.evaluateRules([largePurchases], customerWindow) == new Score(3)
    }

    def "supports explained scores without changing the rules"() {
        given:
        def explainingEngine = new EventRuleEngine<ExplainedScore>(
                new BooleanAlgebra(),
                new ExplainableAlgebra(),
                new ExplainedScoreMonoid()
        )
        def labelled = new EventRule(
                new Expression.MetricComparison(CustomerEventMetrics.AMOUNT, ComparisonOperator.GT, 1000d),
                new Expression.Labeled("large purchase", new Expression.ConstantScore(3))
        )

        when:
        def explained = explainingEngine.evaluateRules([labelled], window(1500, 500, 2000))

        then:
        explained.total() == 6
        explained.contributions() == [
                new Contribution("large purchase", 3),
                new Contribution("large purchase", 3)
        ]
    }

    private static EventRule pointsPerLargePurchase() {
        new EventRule(
                new Expression.MetricComparison(CustomerEventMetrics.AMOUNT, ComparisonOperator.GT, 1000d),
                new Expression.ConstantScore(3)
        )
    }

    private static CustomerWindow window(double ... amounts) {
        customerWindow([(CustomerMetrics.YEARLY_PURCHASE_AMOUNT): 20_000d], amounts)
    }

    private static CustomerWindow poorWindow(double ... amounts) {
        customerWindow([(CustomerMetrics.YEARLY_PURCHASE_AMOUNT): 500d], amounts)
    }

    private static CustomerWindow customerWindow(Map<Metric, Double> metrics, double ... amounts) {
        def events = amounts.collect { amount -> new CustomerEvent("PURCHASE", Instant.EPOCH, amount) }
        new CustomerWindow("c-1", Instant.EPOCH, Instant.EPOCH, events, metrics)
    }

    private static CustomerWindow mixedWindow(CustomerEvent... events) {
        new CustomerWindow(
                "c-1",
                Instant.EPOCH,
                Instant.EPOCH,
                events.toList(),
                [(CustomerMetrics.YEARLY_PURCHASE_AMOUNT): 20_000d]
        )
    }
}
