package com.softwarearchetypes.pricing.scenarios

import static java.time.Clock.fixed

import com.softwarearchetypes.pricing.CalculatorType
import com.softwarearchetypes.pricing.ComponentBreakdown
import com.softwarearchetypes.pricing.ParameterValue
import com.softwarearchetypes.pricing.Parameters
import com.softwarearchetypes.pricing.PricingConfiguration
import com.softwarearchetypes.pricing.PricingFacade
import com.softwarearchetypes.pricing.SumOf
import com.softwarearchetypes.pricing.ValueOf
import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Map
import spock.lang.Specification

class BankingComponentScenarioSpec extends Specification {

    static final Instant NOW = LocalDateTime.of(2025, 1, 15, 12, 50).atZone(ZoneId.systemDefault()).toInstant()
    static final Clock clock = fixed(NOW, ZoneId.systemDefault())
    private final PricingFacade facade = PricingConfiguration.inMemory(clock).pricingFacade()

    def setup() {
        facade.addCalculator("loan-interest", CalculatorType.SIMPLE_INTEREST,
                Parameters.of("annualRate", BigDecimal.valueOf(5.5)))

        facade.addCalculator("insurance-rate", CalculatorType.PERCENTAGE,
                Parameters.of("percentageRate", BigDecimal.valueOf(2)))

        facade.addCalculator("processing-fee", CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(BigDecimal.valueOf(500), "PLN")))
        facade.addCalculator("monthly-account-fee", CalculatorType.SIMPLE_FIXED,
                Parameters.of("amount", Money.of(BigDecimal.valueOf(15), "PLN")))

        facade.addCalculator("transaction-fee", CalculatorType.STEP_FUNCTION,
                Parameters.of(
                        "basePrice", Money.of(BigDecimal.ZERO, "PLN"),
                        "stepSize", BigDecimal.ONE,
                        "stepIncrement", BigDecimal.valueOf(0.50)
                ))
        facade.addCalculator("management-fee", CalculatorType.PERCENTAGE,
                Parameters.of("percentageRate", BigDecimal.valueOf(1.5)))

        facade.addCalculator("performance-fee", CalculatorType.PERCENTAGE,
                Parameters.of("percentageRate", BigDecimal.valueOf(20)))
    }

    def "loan cost with insurance is calculated as a dependency on the base"() {

        given: "a loan base of 100,000 PLN at 5.5% annual interest plus a 500 PLN processing fee, with 2% insurance on the base"
        facade.createSimpleComponent("principal-interest", "loan-interest")
        facade.createSimpleComponent("loan-insurance", "insurance-rate")
        facade.createSimpleComponent("processing", "processing-fee")
        facade.createCompositeComponent(
                "loan-base",
                Map.of(),
                "principal-interest", "processing"
        )

        Map<String, Map<String, ParameterValue>> loanDependencies = Map.of(
                "loan-insurance", Map.of("baseAmount", new ValueOf("loan-base")))
        facade.createCompositeComponent(
                "total-loan-cost", loanDependencies,
                "loan-base", "loan-insurance"
        )

        and:
        Parameters loanParams = Parameters.of(
                "base", Money.of(BigDecimal.valueOf(100000), "PLN"),
                "unit", ChronoUnit.YEARS
        )

        Money result = facade.calculateComponent("total-loan-cost", loanParams)

        and:
        Money expectedBase = Money.of(BigDecimal.valueOf(6000), "PLN")
        Money expectedInsurance = Money.of(BigDecimal.valueOf(120), "PLN")
        Money expectedTotal = Money.of(BigDecimal.valueOf(6120), "PLN")

        expect:

        result == expectedTotal

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("total-loan-cost", loanParams)
        breakdown.name() == "total-loan-cost"
        breakdown.total() == expectedTotal
        breakdown.children().size() == 2

        breakdown.children().find { it.name() == "loan-base" }.total() == expectedBase
        breakdown.children().find { it.name() == "loan-base" }.children().size() == 2

        breakdown.children().find { it.name() == "loan-insurance" }.total() == expectedInsurance
        breakdown.children().find { it.name() == "loan-insurance" }.children().isEmpty()
    }

    def "account fees include both monthly fee and per-transaction charges"() {

        given: "monthly account fee of 15 PLN plus 0.50 PLN per transaction for 50 transactions"
        facade.createSimpleComponent("monthly-fee", "monthly-account-fee")
        facade.createSimpleComponent("transaction-fees", "transaction-fee")

        facade.createCompositeComponent(
                "total-account-fees",
                Map.of(),
                "monthly-fee", "transaction-fees"
        )

        and:
        Parameters accountParams = Parameters.of("quantity", BigDecimal.valueOf(50))
        Money result = facade.calculateComponent("total-account-fees", accountParams)

        and:
        Money expected = Money.of(BigDecimal.valueOf(40), "PLN")

        expect:

        result == expected

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("total-account-fees", accountParams)
        breakdown.name() == "total-account-fees"
        breakdown.total() == expected
        breakdown.children().size() == 2
    }

    def "portfolio fees include a performance bonus calculated on the management fee"() {

        given: "1,000,000 PLN portfolio with 1.5% management fee and 20% performance bonus on the management fee"
        facade.createSimpleComponent("base-management", "management-fee")
        facade.createSimpleComponent("performance-bonus", "performance-fee")
        Map<String, Map<String, ParameterValue>> managementDependencies = Map.of(
                "performance-bonus", Map.of("baseAmount", new ValueOf("base-management")))
        facade.createCompositeComponent(
                "total-management-fees", managementDependencies,
                "base-management", "performance-bonus"
        )

        and:
        Parameters portfolioParams = Parameters.of(
                "baseAmount", Money.of(BigDecimal.valueOf(1000000), "PLN")
        )

        Money result = facade.calculateComponent("total-management-fees", portfolioParams)

        and:
        Money expectedBase = Money.of(BigDecimal.valueOf(15000), "PLN")
        Money expectedPerformance = Money.of(BigDecimal.valueOf(3000), "PLN")
        Money expectedTotal = Money.of(BigDecimal.valueOf(18000), "PLN")

        expect:

        result == expectedTotal

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("total-management-fees", portfolioParams)
        breakdown.name() == "total-management-fees"
        breakdown.total() == expectedTotal
        breakdown.children().size() == 2
        breakdown.children().find { it.name() == "base-management" }.total() == expectedBase

        breakdown.children().find { it.name() == "performance-bonus" }.total() == expectedPerformance
    }

    def "complex loan cost combines interest, processing, and insurance with multiple dependencies"() {

        given: "200,000 PLN loan with interest, processing fee, and insurance on the sum of both"
        facade.createSimpleComponent("interest", "loan-interest")
        facade.createSimpleComponent("processing", "processing-fee")
        facade.createSimpleComponent("insurance", "insurance-rate")
        Map<String, Map<String, ParameterValue>> financingDependencies = Map.of(
                "insurance", Map.of("baseAmount", new SumOf("interest", "processing")))
        facade.createCompositeComponent(
                "financing-costs", financingDependencies,
                "interest", "processing", "insurance"
        )

        and:
        Parameters loanParams = Parameters.of(
                "base", Money.of(BigDecimal.valueOf(200000), "PLN"),
                "unit", ChronoUnit.YEARS
        )

        Money result = facade.calculateComponent("financing-costs", loanParams)

        and:
        Money expectedInterest = Money.of(BigDecimal.valueOf(11000), "PLN")
        Money expectedProcessing = Money.of(BigDecimal.valueOf(500), "PLN")
        Money expectedInsurance = Money.of(BigDecimal.valueOf(230), "PLN")
        Money expectedTotal = Money.of(BigDecimal.valueOf(11730), "PLN")

        expect:

        result == expectedTotal

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("financing-costs", loanParams)
        breakdown.name() == "financing-costs"
        breakdown.total() == expectedTotal
        breakdown.children().size() == 3

        breakdown.children().find { it.name() == "interest" }.total() == expectedInterest

        breakdown.children().find { it.name() == "processing" }.total() == expectedProcessing

        breakdown.children().find { it.name() == "insurance" }.total() == expectedInsurance
    }
}
