package com.github.monaboiste.fairshare.pricing.scenarios

import com.github.monaboiste.fairshare.pricing.calculation.Calculator
import com.github.monaboiste.fairshare.pricing.calculation.CalculatorRange
import com.github.monaboiste.fairshare.pricing.calculation.Calculators
import com.github.monaboiste.fairshare.pricing.calculation.Parameters
import com.github.monaboiste.fairshare.pricing.calculation.PricingResult
import com.github.monaboiste.fairshare.quantity.money.Money
import spock.lang.Specification

class BankAccountFeeScenarioSpec extends Specification {

    private Calculator accountFeeCalculator

    def setup() {
        Calculator feeTier1 = Calculators.fixed("acc-fee-tier-1", Money.of(new BigDecimal("20.00"), "PLN"))
        Calculator feeTier2 = Calculators.fixed("acc-fee-tier-2", Money.of(new BigDecimal("10.00"), "PLN"))
        Calculator feeTier3 = Calculators.fixed("acc-fee-tier-3", Money.of(new BigDecimal("0.00"), "PLN"))

        List<CalculatorRange> ranges = List.of(
                CalculatorRange.numeric(BigDecimal.ZERO, new BigDecimal("1000"), feeTier1.getId()),
                CalculatorRange.numeric(new BigDecimal("1000"), new BigDecimal("4000"), feeTier2.getId()),
                CalculatorRange.numeric(new BigDecimal("4000"), new BigDecimal(Integer.MAX_VALUE), feeTier3.getId())
        )

        accountFeeCalculator = Calculators.composite("account-fee", "monthlyIncome", ranges, [feeTier1, feeTier2, feeTier3])
    }

    def "income of zero is charged 20 PLN"() {
        given:
        Parameters params = new Parameters(Map.of(
                "monthlyIncome", BigDecimal.ZERO
        ))

        and:
        PricingResult fee = accountFeeCalculator.calculate(params)

        expect:
        new BigDecimal("20.00") == fee.money().value()
    }

    def "income below 1000 PLN is charged 20 PLN"() {
        given:
        Parameters params = new Parameters(Map.of(
                "monthlyIncome", new BigDecimal("500")
        ))

        and:
        PricingResult fee = accountFeeCalculator.calculate(params)

        expect:
        new BigDecimal("20.00") == fee.money().value()
    }

    def "income just below 1000 PLN is still charged 20 PLN"() {
        given:
        Parameters params = new Parameters(Map.of(
                "monthlyIncome", new BigDecimal("999.99")
        ))

        and:
        PricingResult fee = accountFeeCalculator.calculate(params)

        expect:
        new BigDecimal("20.00") == fee.money().value()
    }

    def "income at the 1000 PLN boundary is charged 10 PLN"() {
        given:
        Parameters params = new Parameters(Map.of(
                "monthlyIncome", new BigDecimal("1000")
        ))

        and:
        PricingResult fee = accountFeeCalculator.calculate(params)

        expect:
        new BigDecimal("10.00") == fee.money().value()
    }

    def "income between 1000 and 4000 PLN is charged 10 PLN"() {
        given:
        Parameters params = new Parameters(Map.of(
                "monthlyIncome", new BigDecimal("2500")
        ))

        and:
        PricingResult fee = accountFeeCalculator.calculate(params)

        expect:
        new BigDecimal("10.00") == fee.money().value()
    }

    def "income just below 4000 PLN is charged 10 PLN"() {
        given:
        Parameters params = new Parameters(Map.of(
                "monthlyIncome", new BigDecimal("3999.99")
        ))

        and:
        PricingResult fee = accountFeeCalculator.calculate(params)

        expect:
        new BigDecimal("10.00") == fee.money().value()
    }

    def "income at the 4000 PLN boundary is charged nothing"() {
        given:
        Parameters params = new Parameters(Map.of(
                "monthlyIncome", new BigDecimal("4000")
        ))

        and:
        PricingResult fee = accountFeeCalculator.calculate(params)

        expect:
        BigDecimal.ZERO == fee.money().value()
    }

    def "income above 4000 PLN is free"() {
        given:
        Parameters params = new Parameters(Map.of(
                "monthlyIncome", new BigDecimal("5000")
        ))

        and:
        PricingResult fee = accountFeeCalculator.calculate(params)

        expect:
        BigDecimal.ZERO == fee.money().value()
    }

    def "very high income is free"() {
        given:
        Parameters params = new Parameters(Map.of(
                "monthlyIncome", new BigDecimal("50000")
        ))

        and:
        PricingResult fee = accountFeeCalculator.calculate(params)

        expect:
        BigDecimal.ZERO == fee.money().value()
    }


    def "formula shows piecewise function with all three tiers"() {
        given:
        String formula = accountFeeCalculator.formula()
        String expected = ("f(x) = piecewise function:%n" +
                "  [0, 1000) \u2192 acc-fee-tier-1: f(x) = PLN 20%n" +
                "  [1000, 4000) \u2192 acc-fee-tier-2: f(x) = PLN 10%n" +
                "  [4000, 2147483647) \u2192 acc-fee-tier-3: f(x) = PLN 0").formatted()

        expect:
        formula == expected
    }
}
