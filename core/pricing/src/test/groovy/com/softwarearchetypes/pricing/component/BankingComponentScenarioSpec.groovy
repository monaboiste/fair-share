package com.softwarearchetypes.pricing.component

import com.softwarearchetypes.pricing.calculation.Calculator
import com.softwarearchetypes.pricing.calculation.Calculators
import com.softwarearchetypes.pricing.calculation.Parameters
import com.softwarearchetypes.quantity.money.Money
import java.time.temporal.ChronoUnit
import spock.lang.Specification

class BankingComponentScenarioSpec extends Specification {


    private Calculator performancefee
    private Calculator managementfee
    private Calculator transactionfee
    private Calculator monthlyaccountfee
    private Calculator processingfee
    private Calculator insurancerate
    private Calculator loaninterest

    def setup() {
        loaninterest = Calculators.simpleInterest("loan-interest", BigDecimal.valueOf(5.5))

        insurancerate = Calculators.percentage("insurance-rate", BigDecimal.valueOf(2))

        processingfee = Calculators.fixed("processing-fee", Money.of(BigDecimal.valueOf(500), "PLN"))
        monthlyaccountfee = Calculators.fixed("monthly-account-fee", Money.of(BigDecimal.valueOf(15), "PLN"))

        transactionfee = Calculators.stepFunction("transaction-fee", Money.of(BigDecimal.ZERO, "PLN"), BigDecimal.ONE, BigDecimal.valueOf(0.50))
        managementfee = Calculators.percentage("management-fee", BigDecimal.valueOf(1.5))

        performancefee = Calculators.percentage("performance-fee", BigDecimal.valueOf(20))
    }

    def "loan cost with insurance is calculated as a dependency on the base"() {
        given: "a loan base of 100,000 PLN at 5.5% annual interest plus a 500 PLN processing fee, with 2% insurance on the base"
        def principal_interest = Component.simple("principal-interest", loaninterest)
        def loan_insurance = Component.simple("loan-insurance", insurancerate)
        def processing = Component.simple("processing", processingfee)
        def loan_base = Component.composite("loan-base", Map.of(), principal_interest, processing)

        Map<String, Map<String, ParameterExpression>> loanDependencies = Map.<String, Map<String, ParameterExpression>> of(
                "loan-insurance", Map.of("baseAmount", ParameterExpression.valueOf("loan-base")))
        def total_loan_cost = Component.composite("total-loan-cost", loanDependencies, loan_base, loan_insurance)

        and:
        Parameters loanParams = Parameters.of(
                "base", Money.of(BigDecimal.valueOf(100000), "PLN"),
                "unit", ChronoUnit.YEARS
        )

        Money result = total_loan_cost.calculate( loanParams).money()

        and:
        Money expectedBase = Money.of(BigDecimal.valueOf(6000), "PLN")
        Money expectedInsurance = Money.of(BigDecimal.valueOf(120), "PLN")
        Money expectedTotal = Money.of(BigDecimal.valueOf(6120), "PLN")

        expect:
        result == expectedTotal

        ComponentBreakdown breakdown = total_loan_cost.calculateBreakdown( loanParams)
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
        def monthly_fee = Component.simple("monthly-fee", monthlyaccountfee)
        def transaction_fees = Component.simple("transaction-fees", transactionfee)

        def total_account_fees = Component.composite("total-account-fees", Map.of(), monthly_fee, transaction_fees)

        and:
        Parameters accountParams = Parameters.of("quantity", BigDecimal.valueOf(50))
        Money result = total_account_fees.calculate( accountParams).money()

        and:
        Money expected = Money.of(BigDecimal.valueOf(40), "PLN")

        expect:
        result == expected

        ComponentBreakdown breakdown = total_account_fees.calculateBreakdown( accountParams)
        breakdown.name() == "total-account-fees"
        breakdown.total() == expected
        breakdown.children().size() == 2
    }

    def "portfolio fees include a performance bonus calculated on the management fee"() {
        given: "1,000,000 PLN portfolio with 1.5% management fee and 20% performance bonus on the management fee"
        def base_management = Component.simple("base-management", managementfee)
        def performance_bonus = Component.simple("performance-bonus", performancefee)
        Map<String, Map<String, ParameterExpression>> managementDependencies = Map.<String, Map<String, ParameterExpression>> of(
                "performance-bonus", Map.of("baseAmount", ParameterExpression.valueOf("base-management")))
        def total_management_fees = Component.composite("total-management-fees", managementDependencies, base_management, performance_bonus)

        and:
        Parameters portfolioParams = Parameters.of(
                "baseAmount", Money.of(BigDecimal.valueOf(1000000), "PLN")
        )

        Money result = total_management_fees.calculate( portfolioParams).money()

        and:
        Money expectedBase = Money.of(BigDecimal.valueOf(15000), "PLN")
        Money expectedPerformance = Money.of(BigDecimal.valueOf(3000), "PLN")
        Money expectedTotal = Money.of(BigDecimal.valueOf(18000), "PLN")

        expect:
        result == expectedTotal

        ComponentBreakdown breakdown = total_management_fees.calculateBreakdown( portfolioParams)
        breakdown.name() == "total-management-fees"
        breakdown.total() == expectedTotal
        breakdown.children().size() == 2
        breakdown.children().find { it.name() == "base-management" }.total() == expectedBase

        breakdown.children().find { it.name() == "performance-bonus" }.total() == expectedPerformance
    }

    def "complex loan cost combines interest, processing, and insurance with multiple dependencies"() {
        given: "200,000 PLN loan with interest, processing fee, and insurance on the sum of both"
        def interest = Component.simple("interest", loaninterest)
        def processing = Component.simple("processing", processingfee)
        def insurance = Component.simple("insurance", insurancerate)
        Map<String, Map<String, ParameterExpression>> financingDependencies = Map.<String, Map<String, ParameterExpression>> of(
                "insurance", Map.of("baseAmount", ParameterExpression.sumOf("interest", "processing")))
        def financing_costs = Component.composite("financing-costs", financingDependencies, interest, processing, insurance)

        and:
        Parameters loanParams = Parameters.of(
                "base", Money.of(BigDecimal.valueOf(200000), "PLN"),
                "unit", ChronoUnit.YEARS
        )

        Money result = financing_costs.calculate( loanParams).money()

        and:
        Money expectedInterest = Money.of(BigDecimal.valueOf(11000), "PLN")
        Money expectedProcessing = Money.of(BigDecimal.valueOf(500), "PLN")
        Money expectedInsurance = Money.of(BigDecimal.valueOf(230), "PLN")
        Money expectedTotal = Money.of(BigDecimal.valueOf(11730), "PLN")

        expect:
        result == expectedTotal

        ComponentBreakdown breakdown = financing_costs.calculateBreakdown( loanParams)
        breakdown.name() == "financing-costs"
        breakdown.total() == expectedTotal
        breakdown.children().size() == 3

        breakdown.children().find { it.name() == "interest" }.total() == expectedInterest

        breakdown.children().find { it.name() == "processing" }.total() == expectedProcessing

        breakdown.children().find { it.name() == "insurance" }.total() == expectedInsurance
    }
}
