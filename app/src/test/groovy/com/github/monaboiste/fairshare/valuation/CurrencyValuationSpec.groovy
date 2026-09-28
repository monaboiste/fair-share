package com.github.monaboiste.fairshare.valuation

import com.github.monaboiste.fairshare.pricing.calculation.CalculatorId
import com.github.monaboiste.fairshare.pricing.calculation.Parameters
import com.github.monaboiste.fairshare.pricing.component.ComponentVersionId
import com.github.monaboiste.fairshare.pricing.component.Validity
import com.github.monaboiste.fairshare.quantity.money.Money
import java.time.LocalDateTime
import javax.money.CurrencyUnit
import javax.money.Monetary
import spock.lang.Specification

class CurrencyValuationSpec extends Specification {

    private static final CurrencyUnit EUR = Monetary.getCurrency("EUR")
    private static final CurrencyUnit JPY = Monetary.getCurrency("JPY")
    private static final CurrencyUnit KWD = Monetary.getCurrency("KWD")
    private static final CurrencyUnit PLN = Monetary.getCurrency("PLN")
    private static final CurrencyUnit USD = Monetary.getCurrency("USD")

    private final ValuationEngine pricing = ValuationEngine.standard()

    def "currency conversion calculates a source amount"() {
        given:
        CurrencyConversionCalculator calculator = new CurrencyConversionCalculator(
                ExchangeRate.of(USD, PLN, 4.5))

        expect:
        calculator.calculate(Parameters.of("source", Money.of(2, "USD"))).money() == Money.of(9, "PLN")
    }

    def "currency conversion rejects a missing source before calculation"() {
        given:
        CurrencyConversionCalculator calculator = new CurrencyConversionCalculator(
                ExchangeRate.of(USD, PLN, 4.5))

        when:
        calculator.calculate(Parameters.empty())

        then:
        IllegalArgumentException error = thrown()
        error.message.contains("source")
    }

    def "currency conversion exposes calculator metadata"() {
        given:
        CalculatorId id = new CalculatorId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        ExchangeRate exchangeRate = ExchangeRate.of(USD, PLN, 4.5)
        CurrencyConversionCalculator calculator = new CurrencyConversionCalculator(id, exchangeRate)

        expect:
        calculator.getId() == id
        calculator.name() == "currency-conversion"
        calculator.formula() == "source × 4.5"
        calculator.describe() == "Currency conversion using Exchange Rate ${exchangeRate}"
    }

    def "currency conversion accepts a convertible Money source"() {
        given:
        CurrencyConversionCalculator calculator = new CurrencyConversionCalculator(
                ExchangeRate.of(USD, PLN, 4.5))

        expect:
        calculator.calculate(Parameters.of("source", "USD 2.00")).money() == Money.of(9, "PLN")
    }

    def "currency conversion rejects a source with the wrong type"() {
        given:
        CurrencyConversionCalculator calculator = new CurrencyConversionCalculator(
                ExchangeRate.of(USD, PLN, 4.5))

        when:
        calculator.calculate(Parameters.of("source", 2))

        then:
        IllegalArgumentException error = thrown()
        error.message.contains("source")
        error.message.contains("Money")
    }

    def "same-currency Valuation uses a stable implicit Exchange Rate version of one"() {
        given:
        Money source = Money.of(123.456, "USD")

        when:
        def first = pricing.identity(source)
        def second = pricing.identity(source)

        then:
        first.money() == Money.of(123.46, "USD")
        first.exchangeRate() == ExchangeRate.of(USD, USD, BigDecimal.ONE)
        first.componentVersion().id() == second.componentVersion().id()
    }

    def "converting a selected Exchange Rate version applies its rate and keeps the version"() {
        given:
        ComponentVersionId latestId = componentVersionId("00000000-0000-0000-0000-000000000002")
        ExchangeRate latestRate = ExchangeRate.of(EUR, PLN, 4.5)
        ExchangeRateVersion latest = new ExchangeRateVersion(
                latestId, latestRate,
                Validity.from(LocalDateTime.parse("2025-02-01T00:00:00")),
                LocalDateTime.parse("2025-01-01T00:00:00"))

        when:
        Valuation valuation = pricing.value(Money.of(10, "EUR"), PLN, latest)

        then:
        valuation.money() == Money.of(45, "PLN")
        valuation.exchangeRate() == latestRate
        valuation.componentVersion().id() == latestId
    }

    def "converting a selected version rejects a currency pair that does not match the valuation"() {
        given:
        ComponentVersionId versionId = componentVersionId("00000000-0000-0000-0000-000000000003")
        ExchangeRateVersion wrongDirection = new ExchangeRateVersion(
                versionId, ExchangeRate.of(JPY, USD, BigDecimal.ONE), Validity.always(), LocalDateTime.MIN)

        when:
        pricing.value(Money.of(100, "USD"), JPY, wrongDirection)

        then:
        thrown(IllegalArgumentException)
    }

    def "rejects an Exchange Rate with #description value"() {
        when:
        ExchangeRate.of(EUR, PLN, value.toBigDecimal())

        then:
        thrown(IllegalArgumentException)

        where:
        description                       | value
        "zero"                            | "0"
        "negative"                        | "-1"
        "excessive fractional precision" | "1.1234567890123"
        "excessive trailing precision"   | "1.0000000000000"
    }

    def "accepts an Exchange Rate with twelve fractional digits"() {
        expect:
        ExchangeRate.of(EUR, PLN, 1.123456789012).value() == 1.123456789012
    }

    def "manual Exchange Rate creates the required one-off version"() {
        given:
        ComponentVersionId versionId = componentVersionId("00000000-0000-0000-0000-000000000003")
        ExchangeRate override = ExchangeRate.of(USD, JPY, BigDecimal.ONE)

        when:
        Valuation valuation = pricing.value(
                Money.of(100.5, "USD"),
                JPY,
                LocalDateTime.parse("2025-01-15T00:00:00"),
                override,
                versionId)

        then:
        valuation.money() == Money.of(101, "JPY")
        valuation.exchangeRate() == override
        valuation.componentVersion().id() == versionId
    }

    def "Valuation rounds only after applying the Exchange Rate"() {
        given:
        ExchangeRate override = ExchangeRate.of(EUR, USD, new BigDecimal("10000000000"))

        when:
        Valuation valuation = pricing.value(
                Money.of(0.00000000006, "EUR"),
                USD,
                LocalDateTime.parse("2025-01-15T00:00:00"),
                override,
                componentVersionId("00000000-0000-0000-0000-000000000003"))

        then:
        valuation.money() == Money.of(0.60, "USD")
    }

    def "Valuation respects a target currency with three fraction digits"() {
        given:
        ExchangeRate override = ExchangeRate.of(USD, KWD, 0.3075)

        when:
        Valuation valuation = pricing.value(
                Money.of(1, "USD"),
                KWD,
                LocalDateTime.parse("2025-01-15T00:00:00"),
                override,
                componentVersionId("00000000-0000-0000-0000-000000000003"))

        then:
        valuation.money() == Money.of(0.308, "KWD")
    }

    def "manual Exchange Rate rejects a direction that does not match the Valuation"() {
        given:
        ExchangeRate wrongDirection = ExchangeRate.of(JPY, USD, BigDecimal.ONE)

        when:
        pricing.value(
                Money.of(100, "USD"),
                JPY,
                LocalDateTime.parse("2025-01-15T00:00:00"),
                wrongDirection,
                componentVersionId("00000000-0000-0000-0000-000000000003"))

        then:
        thrown(IllegalArgumentException)
    }

    private static ExchangeRateVersion exchangeRateVersion(
            String versionId, String rateValue, Validity validity, LocalDateTime definedAt) {
        return new ExchangeRateVersion(componentVersionId(versionId),
                ExchangeRate.of(EUR, PLN, rateValue.toBigDecimal()), validity, definedAt)
    }

    private static ComponentVersionId componentVersionId(String value) {
        return new ComponentVersionId(UUID.fromString(value))
    }
}
