package com.github.monaboiste.fairshare.valuation

import com.github.monaboiste.fairshare.pricing.component.ComponentVersionId
import com.github.monaboiste.fairshare.pricing.component.Validity
import com.github.monaboiste.fairshare.valuation.ExchangeRateVersions
import java.time.LocalDateTime
import javax.money.CurrencyUnit
import javax.money.Monetary
import spock.lang.Specification

class ExchangeRateVersionsSpec extends Specification {

    private static final CurrencyUnit EUR = Monetary.getCurrency("EUR")
    private static final CurrencyUnit JPY = Monetary.getCurrency("JPY")
    private static final CurrencyUnit PLN = Monetary.getCurrency("PLN")

    def applicable(ExchangeRateVersions versions, source, target, LocalDateTime at) {
        return versions.applicableAt(source, target, at)
    }

    def "selects the latest valid-from version in an overlap"() {
        given:
        ComponentVersionId latestId = componentVersionId("00000000-0000-0000-0000-000000000002")
        ExchangeRateVersion base = exchangeRateVersion(
                "00000000-0000-0000-0000-000000000001",
                "4.0",
                Validity.from(LocalDateTime.parse("2025-01-01T00:00:00")),
                LocalDateTime.parse("2024-12-01T00:00:00"))
        ExchangeRateVersion latest = new ExchangeRateVersion(
                latestId, ExchangeRate.of(EUR, PLN, 4.5),
                Validity.from(LocalDateTime.parse("2025-02-01T00:00:00")),
                LocalDateTime.parse("2025-01-01T00:00:00"))
        ExchangeRateVersions versions = ExchangeRateVersions.from([base, latest])

        when:
        def selected = applicable(versions, EUR, PLN, LocalDateTime.parse("2025-02-15T00:00:00"))

        then:
        selected.isPresent()
        selected.get().id() == latestId
    }

    def "selects only versions matching its currency pair"() {
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
        ExchangeRateVersions versions = ExchangeRateVersions.from([matching, unrelated])

        when:
        def selected = applicable(versions, EUR, PLN, LocalDateTime.parse("2025-03-01T00:00:00"))

        then:
        selected.isPresent()
        selected.get().id() == componentVersionId("00000000-0000-0000-0000-000000000001")
    }

    def "ignores versions whose target currency does not match the valuation"() {
        given:
        ExchangeRateVersion version = exchangeRateVersion(
                "00000000-0000-0000-0000-000000000001",
                "4.0",
                Validity.from(LocalDateTime.parse("2025-01-01T00:00:00")),
                LocalDateTime.parse("2024-12-01T00:00:00"))
        ExchangeRateVersions versions = ExchangeRateVersions.from([version])

        when:
        def selected = applicable(versions, EUR, JPY, LocalDateTime.parse("2025-03-01T00:00:00"))

        then:
        selected.isEmpty()
    }

    def "falls back after a temporary Exchange Rate version expires"() {
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
        ExchangeRateVersions versions = ExchangeRateVersions.from([base, temporary])

        when:
        def selected = applicable(versions, EUR, PLN, LocalDateTime.parse("2025-03-01T00:00:00"))

        then:
        selected.isPresent()
        selected.get().id() == componentVersionId("00000000-0000-0000-0000-000000000001")
    }

    def "later stream order wins when versions share a valid-from"() {
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
        ExchangeRateVersions versions = ExchangeRateVersions.from([first, later])

        when:
        def selected = applicable(versions, EUR, PLN, LocalDateTime.parse("2025-03-01T00:00:00"))

        then:
        selected.isPresent()
        selected.get().id() == componentVersionId("00000000-0000-0000-0000-000000000002")
    }

    def "reports no applicable version when nothing is configured"() {
        given:
        ExchangeRateVersions versions = ExchangeRateVersions.empty()

        when:
        def selected = applicable(versions, EUR, PLN, LocalDateTime.parse("2025-01-01T00:00:00"))

        then:
        selected.isEmpty()
    }

    def "applies Validity at inclusive midnight boundaries but not before noon on its first day"() {
        given:
        def midnight = LocalDateTime.parse("2026-09-27T00:00:00")
        def noon = LocalDateTime.parse("2026-09-27T12:00:00")
        def base = exchangeRateVersion("00000000-0000-0000-0000-000000000001", "4.0",
            Validity.until(midnight), midnight.minusDays(1))
        def newer = exchangeRateVersion("00000000-0000-0000-0000-000000000002", "4.5",
            Validity.from(noon), midnight.minusDays(2))
        ExchangeRateVersions versions = ExchangeRateVersions.from([base, newer])

        when:
        def atStartOfDay = applicable(versions, EUR, PLN, midnight.toLocalDate().atStartOfDay())
        def atNoon = applicable(versions, EUR, PLN, noon)

        then:
        atStartOfDay.isPresent()
        atNoon.isPresent()
        atStartOfDay.get().id() == componentVersionId("00000000-0000-0000-0000-000000000001")
        atNoon.get().id() == componentVersionId("00000000-0000-0000-0000-000000000002")
    }

    def "reports no applicable version in a gap between versions"() {
        given:
        def end = LocalDateTime.parse("2026-09-26T23:59:59")
        def versions = ExchangeRateVersions.from([
            exchangeRateVersion("00000000-0000-0000-0000-000000000001", "4.0",
                Validity.until(end), end.minusDays(1)),
            exchangeRateVersion("00000000-0000-0000-0000-000000000002", "4.5",
                Validity.from(end.plusDays(2)), end.minusDays(1))
        ])

        when:
        def selected = applicable(versions, EUR, PLN, end.plusSeconds(1))

        then:
        selected.isEmpty()
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
