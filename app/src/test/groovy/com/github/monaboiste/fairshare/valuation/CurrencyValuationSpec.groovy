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
        Valuation first = pricing.value(source, USD, LocalDateTime.parse("2025-01-15T00:00:00"), List.of())
        Valuation second = pricing.value(source, USD, LocalDateTime.parse("2025-02-15T00:00:00"), List.of())

        then:
        first.money() == Money.of(123.46, "USD")
        first.exchangeRate() == ExchangeRate.of(USD, USD, BigDecimal.ONE)
        first.componentVersion().id() == second.componentVersion().id()
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

    def "latest valid-from Exchange Rate version wins an overlap"() {
        given:
        ComponentVersionId latestId = componentVersionId("00000000-0000-0000-0000-000000000002")
        ExchangeRate latestRate = ExchangeRate.of(EUR, PLN, 4.5)
        ExchangeRateVersion base = exchangeRateVersion(
                "00000000-0000-0000-0000-000000000001",
                "4.0",
                Validity.from(LocalDateTime.parse("2025-01-01T00:00:00")),
                LocalDateTime.parse("2024-12-01T00:00:00"))
        ExchangeRateVersion latest = new ExchangeRateVersion(
                latestId, latestRate,
                Validity.from(LocalDateTime.parse("2025-02-01T00:00:00")),
                LocalDateTime.parse("2025-01-01T00:00:00"))

        when:
        Valuation valuation = pricing.value(
                Money.of(10, "EUR"),
                PLN,
                LocalDateTime.parse("2025-02-15T00:00:00"),
                [base, latest])

        then:
        valuation.money() == Money.of(45, "PLN")
        valuation.exchangeRate() == latestRate
        valuation.componentVersion().id() == latestId
    }

    def "Valuation selects only Exchange Rate versions matching its currency pair"() {
        given:
        ExchangeRateVersion matching = exchangeRateVersion(
                "00000000-0000-0000-0000-000000000001",
                "4.0",
                Validity.from(LocalDateTime.parse("2025-01-01T00:00:00")),
                LocalDateTime.parse("2024-12-01T00:00:00"))
        ExchangeRateVersion unrelated = new ExchangeRateVersion(
                        componentVersionId("00000000-0000-0000-0000-000000000002"),
                        ExchangeRate.of(Monetary.getCurrency("GBP"), PLN, 5.0),
                        Validity.from(LocalDateTime.parse("2025-02-01T00:00:00")),
                        LocalDateTime.parse("2025-01-01T00:00:00"))

        when:
        Valuation valuation = pricing.value(
                Money.of(10, "EUR"),
                PLN,
                LocalDateTime.parse("2025-03-01T00:00:00"),
                [matching, unrelated])

        then:
        valuation.money() == Money.of(40, "PLN")
        valuation.componentVersion().id() == matching.id()
    }

    def "Valuation falls back after a temporary Exchange Rate version expires"() {
        given:
        ExchangeRateVersion base = exchangeRateVersion(
                "00000000-0000-0000-0000-000000000001",
                "4.0",
                Validity.from(LocalDateTime.parse("2025-01-01T00:00:00")),
                LocalDateTime.parse("2024-12-01T00:00:00"))
        ExchangeRateVersion temporary = exchangeRateVersion(
                "00000000-0000-0000-0000-000000000002",
                "4.5",
                Validity.between(
                        LocalDateTime.parse("2025-02-01T00:00:00"),
                        LocalDateTime.parse("2025-02-28T23:59:00")),
                LocalDateTime.parse("2025-01-01T00:00:00"))

        when:
        Valuation valuation = pricing.value(
                Money.of(10, "EUR"),
                PLN,
                LocalDateTime.parse("2025-03-01T00:00:00"),
                [base, temporary])

        then:
        valuation.money() == Money.of(40, "PLN")
        valuation.componentVersion().id() == base.id()
    }

    def "later stream order wins when Exchange Rate versions have the same valid-from"() {
        given:
        Validity validity = Validity.from(LocalDateTime.parse("2025-01-01T00:00:00"))
        ExchangeRateVersion first = exchangeRateVersion(
                "00000000-0000-0000-0000-000000000001",
                "4.0",
                validity,
                LocalDateTime.parse("2025-02-01T00:00:00"))
        ExchangeRateVersion later = exchangeRateVersion(
                "00000000-0000-0000-0000-000000000002",
                "4.5",
                validity,
                LocalDateTime.parse("2025-01-01T00:00:00"))

        when:
        Valuation valuation = pricing.value(
                Money.of(10, "EUR"),
                PLN,
                LocalDateTime.parse("2025-03-01T00:00:00"),
                [first, later])

        then:
        valuation.money() == Money.of(45, "PLN")
        valuation.componentVersion().id() == later.id()
    }

    def "foreign-currency Valuation fails when no Exchange Rate version applies"() {
        when:
        pricing.value(
                Money.of(10, "EUR"),
                PLN,
                LocalDateTime.parse("2025-01-01T00:00:00"),
                List.of())

        then:
        thrown(IllegalStateException)
    }

    def "Exchange Rate Validity applies at inclusive midnight boundaries but not before noon on its first day"() {
        given:
        def midnight = LocalDateTime.parse("2026-09-27T00:00:00")
        def noon = LocalDateTime.parse("2026-09-27T12:00:00")
        def base = exchangeRateVersion("00000000-0000-0000-0000-000000000001", "4.0",
            Validity.until(midnight), midnight.minusDays(1))
        def newer = exchangeRateVersion("00000000-0000-0000-0000-000000000002", "4.5",
            Validity.from(noon), midnight.minusDays(2))

        when:
        def atStartOfDay = pricing.value(Money.of(10, "EUR"), PLN, midnight.toLocalDate().atStartOfDay(),
            [base, newer])
        def atNoon = pricing.value(Money.of(10, "EUR"), PLN, noon, [base, newer])

        then:
        atStartOfDay.money() == Money.of(40, "PLN")
        atStartOfDay.componentVersion().id() == base.id()
        atNoon.money() == Money.of(45, "PLN")
        atNoon.componentVersion().id() == newer.id()
    }

    def "Valuation fails in a gap between bounded and future Exchange Rate versions"() {
        given:
        def end = LocalDateTime.parse("2026-09-26T23:59:59")
        def versions = [
            exchangeRateVersion("00000000-0000-0000-0000-000000000001", "4.0",
                Validity.until(end), end.minusDays(1)),
            exchangeRateVersion("00000000-0000-0000-0000-000000000002", "4.5",
                Validity.from(end.plusDays(2)), end.minusDays(1))
        ]

        when:
        pricing.value(Money.of(10, "EUR"), PLN, end.plusSeconds(1), versions)

        then:
        thrown(IllegalStateException)
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
