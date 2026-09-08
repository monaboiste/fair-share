package com.softwarearchetypes.pricing

import static java.time.Clock.fixed

import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.List
import java.util.Map
import spock.lang.Specification


class BankAccountFeeScenarioSpec extends Specification {

    static final Instant NOW = LocalDateTime.of(2025, 1, 15, 12, 50).atZone(ZoneId.systemDefault()).toInstant()
    static final Clock clock = fixed(NOW, ZoneId.systemDefault())
    private final PricingFacade facade = PricingConfiguration.inMemory(clock).pricingFacade()
    private CompositeFunctionCalculator accountFeeCalculator
    def setup() {
        Calculator feeTier1 = facade.addCalculator(
            "acc-fee-tier-1",
            CalculatorType.SIMPLE_FIXED,
            new Parameters(Map.of(
                "amount", Money.of(new BigDecimal("20.00"), "PLN")
            ))
        )

        Calculator feeTier2 = facade.addCalculator(
            "acc-fee-tier-2",
            CalculatorType.SIMPLE_FIXED,
            new Parameters(Map.of(
                "amount", Money.of(new BigDecimal("10.00"), "PLN")
            ))
        )

        Calculator feeTier3 = facade.addCalculator(
            "acc-fee-tier-3",
            CalculatorType.SIMPLE_FIXED,
            new Parameters(Map.of(
                "amount", Money.of(new BigDecimal("0.00"), "PLN")
            ))
        )
        List<CalculatorRange> ranges = List.of(
            new NumericRange(BigDecimal.ZERO, new BigDecimal("1000"), feeTier1.getId()),
            new NumericRange(new BigDecimal("1000"), new BigDecimal("4000"), feeTier2.getId()),
            new NumericRange(new BigDecimal("4000"), new BigDecimal(Integer.MAX_VALUE), feeTier3.getId())
        )
        accountFeeCalculator = (CompositeFunctionCalculator) facade.addCalculator(
            "account-fee",
            CalculatorType.COMPOSITE,
            new Parameters(Map.of(
                "rangeSelector", "monthlyIncome",
                "ranges", ranges
            ))
        )
    }
    def "shouldCharge20PlnForVeryLowIncome"() {
        Parameters params = new Parameters(Map.of(
            "monthlyIncome", BigDecimal.ZERO
        ))
        given:
        Money fee = facade.calculate("account-fee", params)
        assert new BigDecimal("20.00").compareTo(fee.value()) == 0
    }
    def "shouldCharge20PlnForLowIncome"() {
        Parameters params = new Parameters(Map.of(
            "monthlyIncome", new BigDecimal("500")
        ))
        given:
        Money fee = facade.calculate("account-fee", params)
        assert new BigDecimal("20.00").compareTo(fee.value()) == 0
    }
    def "shouldCharge20PlnForIncomeJustBelowThreshold"() {
        Parameters params = new Parameters(Map.of(
            "monthlyIncome", new BigDecimal("999.99")
        ))
        given:
        Money fee = facade.calculate("account-fee", params)
        assert new BigDecimal("20.00").compareTo(fee.value()) == 0
    }
    def "shouldCharge10PlnForIncomeAtLowerBoundary"() {
        Parameters params = new Parameters(Map.of(
            "monthlyIncome", new BigDecimal("1000")
        ))
        given:
        Money fee = facade.calculate("account-fee", params)
        assert new BigDecimal("10.00").compareTo(fee.value()) == 0
    }
    def "shouldCharge10PlnForMediumIncome"() {
        Parameters params = new Parameters(Map.of(
            "monthlyIncome", new BigDecimal("2500")
        ))
        given:
        Money fee = facade.calculate("account-fee", params)
        assert new BigDecimal("10.00").compareTo(fee.value()) == 0
    }
    def "shouldCharge10PlnForIncomeJustBelowHighTier"() {
        Parameters params = new Parameters(Map.of(
            "monthlyIncome", new BigDecimal("3999.99")
        ))
        given:
        Money fee = facade.calculate("account-fee", params)
        assert new BigDecimal("10.00").compareTo(fee.value()) == 0
    }
    def "shouldChargeNothingForIncomeAtHighTierBoundary"() {
        Parameters params = new Parameters(Map.of(
            "monthlyIncome", new BigDecimal("4000")
        ))
        given:
        Money fee = facade.calculate("account-fee", params)
        assert BigDecimal.ZERO.compareTo(fee.value()) == 0
    }
    def "shouldChargeNothingForHighIncome"() {
        Parameters params = new Parameters(Map.of(
            "monthlyIncome", new BigDecimal("5000")
        ))
        given:
        Money fee = facade.calculate("account-fee", params)
        assert BigDecimal.ZERO.compareTo(fee.value()) == 0
    }
    def "shouldChargeNothingForVeryHighIncome"() {
        Parameters params = new Parameters(Map.of(
            "monthlyIncome", new BigDecimal("50000")
        ))
        given:
        Money fee = facade.calculate("account-fee", params)
        assert BigDecimal.ZERO.compareTo(fee.value()) == 0
    }
    def "shouldVerifyCalculatorType"() {
        given:
        assert accountFeeCalculator.getType() == CalculatorType.COMPOSITE
    }
    def "shouldProvideCompositeFunctionFormula"() {
        given:
        String formula = accountFeeCalculator.formula()
        String expected = "f(x) = piecewise function:\n" +
                         "  [0, 1000) → acc-fee-tier-1: f(x) = PLN 20\n" +
                         "  [1000, 4000) → acc-fee-tier-2: f(x) = PLN 10\n" +
                         "  [4000, 2147483647) → acc-fee-tier-3: f(x) = PLN 0"
        assert formula == expected
    }
}
