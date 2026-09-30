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
        given: "a currency conversion using an Exchange Rate of 4.5 from US dollars to zlotys"
        CurrencyConversionCalculator calculator = new CurrencyConversionCalculator(
                ExchangeRate.of(USD, PLN, 4.5))

        expect: "two US dollars convert to nine zlotys"
        calculator.calculate(Parameters.of("source", Money.of(2, "USD"))).money() == Money.of(9, "PLN")
    }

    def "currency conversion rejects a missing source before calculation"() {
        given: "a currency conversion using an Exchange Rate of 4.5 from US dollars to zlotys"
        CurrencyConversionCalculator calculator = new CurrencyConversionCalculator(
                ExchangeRate.of(USD, PLN, 4.5))

        when: "it is asked to convert without a source amount"
        calculator.calculate(Parameters.empty())

        then: "it is refused with a reason naming the missing source"
        IllegalArgumentException error = thrown()
        error.message.contains("source")
    }

    def "currency conversion exposes calculator metadata"() {
        given: "a currency conversion with a known identifier and an Exchange Rate of 4.5 from US dollars to zlotys"
        CalculatorId id = new CalculatorId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        ExchangeRate exchangeRate = ExchangeRate.of(USD, PLN, 4.5)
        CurrencyConversionCalculator calculator = new CurrencyConversionCalculator(id, exchangeRate)

        expect: "it reports its identifier, name, formula and description"
        calculator.getId() == id
        calculator.name() == "currency-conversion"
        calculator.formula() == "source × 4.5"
        calculator.describe() == "Currency conversion using Exchange Rate ${exchangeRate}"
    }

    def "currency conversion accepts a convertible Money source"() {
        given: "a currency conversion using an Exchange Rate of 4.5 from US dollars to zlotys"
        CurrencyConversionCalculator calculator = new CurrencyConversionCalculator(
                ExchangeRate.of(USD, PLN, 4.5))

        expect: "a source written as the text USD 2.00 converts to nine zlotys"
        calculator.calculate(Parameters.of("source", "USD 2.00")).money() == Money.of(9, "PLN")
    }

    def "currency conversion rejects a source with the wrong type"() {
        given: "a currency conversion using an Exchange Rate of 4.5 from US dollars to zlotys"
        CurrencyConversionCalculator calculator = new CurrencyConversionCalculator(
                ExchangeRate.of(USD, PLN, 4.5))

        when: "it is asked to convert a plain number instead of money"
        calculator.calculate(Parameters.of("source", 2))

        then: "it is refused with a reason naming the source and the expected money type"
        IllegalArgumentException error = thrown()
        error.message.contains("source")
        error.message.contains("Money")
    }

    def "same-currency Valuation uses a stable implicit Exchange Rate version of one"() {
        given: "an amount of 123.456 US dollars"
        Money source = Money.of(123.456, "USD")

        when: "it is valued twice in its own currency"
        def first = pricing.identity(source)
        def second = pricing.identity(source)

        then: "it is rounded to cents, uses an Exchange Rate of one, and both Valuations share the same version"
        first.money() == Money.of(123.46, "USD")
        first.exchangeRate() == ExchangeRate.of(USD, USD, BigDecimal.ONE)
        first.componentVersion().id() == second.componentVersion().id()
    }

    def "converting a selected Exchange Rate version applies its rate and keeps the version"() {
        given: "an Exchange Rate version of 4.5 from euros to zlotys"
        ComponentVersionId latestId = componentVersionId("00000000-0000-0000-0000-000000000002")
        ExchangeRate latestRate = ExchangeRate.of(EUR, PLN, 4.5)
        ExchangeRateVersion latest = new ExchangeRateVersion(
                latestId, latestRate,
                Validity.from(LocalDateTime.parse("2025-02-01T00:00:00")),
                LocalDateTime.parse("2025-01-01T00:00:00"))

        when: "ten euros are valued in zlotys with that version"
        Valuation valuation = pricing.value(Money.of(10, "EUR"), PLN, latest)

        then: "the Valuation is 45 zlotys and remembers the Exchange Rate and version used"
        valuation.money() == Money.of(45, "PLN")
        valuation.exchangeRate() == latestRate
        valuation.componentVersion().id() == latestId
    }

    def "converting a selected version rejects a currency that does not match the valuation"() {
        given: "an Exchange Rate version identifier"
        ComponentVersionId versionId = componentVersionId("00000000-0000-0000-0000-000000000003")

        when: "US dollars are valued in yen with a version whose currencies do not match"
        pricing.value(Money.of(100, "USD"), JPY, new ExchangeRateVersion(
                versionId, ExchangeRate.of(sourceCurrency, targetCurrency, BigDecimal.ONE),
                Validity.always(), LocalDateTime.MIN))

        then: "the Valuation is refused"
        thrown(IllegalArgumentException)

        where:
        label                  | sourceCurrency | targetCurrency
        "source only mismatch" | EUR            | JPY
        "target only mismatch" | USD            | EUR
    }

    def "rejects an Exchange Rate with #description value"() {
        when: "an Exchange Rate is defined with an invalid value"
        ExchangeRate.of(EUR, PLN, value.toBigDecimal())

        then: "it is rejected"
        thrown(IllegalArgumentException)

        where:
        description                      | value
        "zero"                           | "0"
        "negative"                       | "-1"
        "excessive fractional precision" | "1.1234567890123"
        "excessive trailing precision"   | "1.0000000000000"
    }

    def "accepts an Exchange Rate with twelve fractional digits"() {
        expect:
        ExchangeRate.of(EUR, PLN, 1.123456789012).value() == 1.123456789012
    }

    def "manual Exchange Rate creates the required one-off version"() {
        given: "an Exchange Rate Override of one from US dollars to yen"
        ComponentVersionId versionId = componentVersionId("00000000-0000-0000-0000-000000000003")
        ExchangeRate override = ExchangeRate.of(USD, JPY, BigDecimal.ONE)

        when: "100.50 US dollars are valued in yen with that override"
        Valuation valuation = pricing.value(
                Money.of(100.5, "USD"),
                JPY,
                new ExchangeRateOverride(override, versionId, LocalDateTime.parse("2025-01-15T00:00:00")))

        then: "the Valuation is 101 yen and keeps the override as a one-off version valid at all times"
        valuation.money() == Money.of(101, "JPY")
        valuation.exchangeRate() == override
        valuation.componentVersion().id() == versionId
        valuation.componentVersion().validity() == Validity.always()
        valuation.componentVersion().definedAt() == LocalDateTime.parse("2025-01-15T00:00:00")
    }

    def "Valuation rounds only after applying the Exchange Rate"() {
        given: "an Exchange Rate Override of ten billion from euros to US dollars"
        ExchangeRate override = ExchangeRate.of(EUR, USD, new BigDecimal("10000000000"))

        when: "a tiny fraction of a euro is valued in US dollars"
        Valuation valuation = pricing.value(Money.of(0.00000000006, "EUR"), USD, manual(override))

        then: "the Valuation is 60 cents, as rounding happens only after conversion"
        valuation.money() == Money.of(0.60, "USD")
    }

    def "Valuation respects a target currency with three fraction digits"() {
        given: "an Exchange Rate Override of 0.3075 from US dollars to Kuwaiti dinars"
        ExchangeRate override = ExchangeRate.of(USD, KWD, 0.3075)

        when: "one US dollar is valued in Kuwaiti dinars"
        Valuation valuation = pricing.value(Money.of(1, "USD"), KWD, manual(override))

        then: "the Valuation is rounded to three decimal places"
        valuation.money() == Money.of(0.308, "KWD")
    }

    def "manual Exchange Rate rejects a direction that does not match the Valuation"() {
        given: "an Exchange Rate Override from yen to US dollars"
        ExchangeRate wrongDirection = ExchangeRate.of(JPY, USD, BigDecimal.ONE)

        when: "US dollars are valued in yen with that override"
        pricing.value(Money.of(100, "USD"), JPY, manual(wrongDirection))

        then: "the Valuation is refused"
        thrown(IllegalArgumentException)
    }

    private static ExchangeRateOverride manual(ExchangeRate rate) {
        return new ExchangeRateOverride(rate, componentVersionId("00000000-0000-0000-0000-000000000003"),
                LocalDateTime.parse("2025-01-15T00:00:00"))
    }

    private static ComponentVersionId componentVersionId(String value) {
        return new ComponentVersionId(UUID.fromString(value))
    }
}
