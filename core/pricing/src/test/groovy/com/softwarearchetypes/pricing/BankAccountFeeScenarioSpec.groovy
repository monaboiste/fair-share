package com.softwarearchetypes.pricing

import static java.time.Clock.fixed

import com.softwarearchetypes.pricing.Calculator
import com.softwarearchetypes.pricing.CalculatorRange
import com.softwarearchetypes.pricing.CalculatorType
import com.softwarearchetypes.pricing.CompositeFunctionCalculator
import com.softwarearchetypes.pricing.NumericRange
import com.softwarearchetypes.pricing.Parameters
import com.softwarearchetypes.pricing.PricingConfiguration
import com.softwarearchetypes.pricing.PricingFacade
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
    def "income of zero is charged 20 PLN"() {
        given:
        Parameters params = new Parameters(Map.of(
            "monthlyIncome", BigDecimal.ZERO
        ))
        and:
        Money fee = facade.calculate("account-fee", params)
        and:
        assert new BigDecimal("20.00").compareTo(fee.value()) == 0
    }
    def "income below 1000 PLN is charged 20 PLN"() {
        given:
        Parameters params = new Parameters(Map.of(
            "monthlyIncome", new BigDecimal("500")
        ))
        and:
        Money fee = facade.calculate("account-fee", params)
        and:
        assert new BigDecimal("20.00").compareTo(fee.value()) == 0
    }
    def "income just below 1000 PLN is still charged 20 PLN"() {
        given:
        Parameters params = new Parameters(Map.of(
            "monthlyIncome", new BigDecimal("999.99")
        ))
        and:
        Money fee = facade.calculate("account-fee", params)
        and:
        assert new BigDecimal("20.00").compareTo(fee.value()) == 0
    }
    def "income at the 1000 PLN boundary is charged 10 PLN"() {
        given:
        Parameters params = new Parameters(Map.of(
            "monthlyIncome", new BigDecimal("1000")
        ))
        and:
        Money fee = facade.calculate("account-fee", params)
        and:
        assert new BigDecimal("10.00").compareTo(fee.value()) == 0
    }
    def "income between 1000 and 4000 PLN is charged 10 PLN"() {
        given:
        Parameters params = new Parameters(Map.of(
            "monthlyIncome", new BigDecimal("2500")
        ))
        and:
        Money fee = facade.calculate("account-fee", params)
        and:
        assert new BigDecimal("10.00").compareTo(fee.value()) == 0
    }
    def "income just below 4000 PLN is charged 10 PLN"() {
        given:
        Parameters params = new Parameters(Map.of(
            "monthlyIncome", new BigDecimal("3999.99")
        ))
        and:
        Money fee = facade.calculate("account-fee", params)
        and:
        assert new BigDecimal("10.00").compareTo(fee.value()) == 0
    }
    def "income at the 4000 PLN boundary is charged nothing"() {
        given:
        Parameters params = new Parameters(Map.of(
            "monthlyIncome", new BigDecimal("4000")
        ))
        and:
        Money fee = facade.calculate("account-fee", params)
        and:
        assert BigDecimal.ZERO.compareTo(fee.value()) == 0
    }
    def "income above 4000 PLN is free"() {
        given:
        Parameters params = new Parameters(Map.of(
            "monthlyIncome", new BigDecimal("5000")
        ))
        and:
        Money fee = facade.calculate("account-fee", params)
        and:
        assert BigDecimal.ZERO.compareTo(fee.value()) == 0
    }
    def "very high income is free"() {
        given:
        Parameters params = new Parameters(Map.of(
            "monthlyIncome", new BigDecimal("50000")
        ))
        and:
        Money fee = facade.calculate("account-fee", params)
        and:
        assert BigDecimal.ZERO.compareTo(fee.value()) == 0
    }
    def "calculator type is composite"() {
        given:
        assert accountFeeCalculator.getType() == CalculatorType.COMPOSITE
    }
    def "formula shows piecewise function with all three tiers"() {
        given:
        String formula = accountFeeCalculator.formula()
        String expected = "f(x) = piecewise function:\n" +
                         "  [0, 1000) \u2192 acc-fee-tier-1: f(x) = PLN 20\n" +
                         "  [1000, 4000) \u2192 acc-fee-tier-2: f(x) = PLN 10\n" +
                         "  [4000, 2147483647) \u2192 acc-fee-tier-3: f(x) = PLN 0"
        assert formula == expected
    }
}
