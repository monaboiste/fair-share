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
        given: "a base Exchange Rate version and a later one whose validity periods overlap"
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

        when: "the applicable version is looked up within the overlap"
        def selected = applicable(versions, EUR, PLN, LocalDateTime.parse("2025-02-15T00:00:00"))

        then: "the version valid from the later date is chosen"
        selected.isPresent()
        selected.get().id() == latestId
    }

    def "selects only versions matching its currency pair"() {
        given: "a euro-to-zloty Exchange Rate version and a newer pound-to-zloty one"
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

        when: "the applicable euro-to-zloty version is looked up"
        def selected = applicable(versions, EUR, PLN, LocalDateTime.parse("2025-03-01T00:00:00"))

        then: "the euro-to-zloty version is chosen"
        selected.isPresent()
        selected.get().id() == componentVersionId("00000000-0000-0000-0000-000000000001")
    }

    def "ignores versions whose target currency does not match the valuation"() {
        given: "a single euro-to-zloty Exchange Rate version"
        ExchangeRateVersion version = exchangeRateVersion(
                "00000000-0000-0000-0000-000000000001",
                "4.0",
                Validity.from(LocalDateTime.parse("2025-01-01T00:00:00")),
                LocalDateTime.parse("2024-12-01T00:00:00"))
        ExchangeRateVersions versions = ExchangeRateVersions.from([version])

        when: "an applicable euro-to-yen version is looked up"
        def selected = applicable(versions, EUR, JPY, LocalDateTime.parse("2025-03-01T00:00:00"))

        then: "none is found"
        selected.isEmpty()
    }

    def "falls back after a temporary Exchange Rate version expires"() {
        given: "a base Exchange Rate version and a temporary one valid only in February"
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

        when: "the applicable version is looked up in March"
        def selected = applicable(versions, EUR, PLN, LocalDateTime.parse("2025-03-01T00:00:00"))

        then: "the base version applies again"
        selected.isPresent()
        selected.get().id() == componentVersionId("00000000-0000-0000-0000-000000000001")
    }

    def "later stream order wins when versions share a valid-from"() {
        given: "two Exchange Rate versions valid from the same date, listed one after the other"
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

        when: "the applicable version is looked up"
        def selected = applicable(versions, EUR, PLN, LocalDateTime.parse("2025-03-01T00:00:00"))

        then: "the one listed later is chosen"
        selected.isPresent()
        selected.get().id() == componentVersionId("00000000-0000-0000-0000-000000000002")
    }

    def "reports no applicable version when nothing is configured"() {
        given: "no configured Exchange Rate versions"
        ExchangeRateVersions versions = ExchangeRateVersions.empty()

        when: "the applicable version is looked up"
        def selected = applicable(versions, EUR, PLN, LocalDateTime.parse("2025-01-01T00:00:00"))

        then: "none is found"
        selected.isEmpty()
    }

    def "applies Validity at inclusive midnight boundaries but not before noon on its first day"() {
        given: "a version valid until midnight and a newer one valid from noon the same day"
        def midnight = LocalDateTime.parse("2026-09-27T00:00:00")
        def noon = LocalDateTime.parse("2026-09-27T12:00:00")
        def base = exchangeRateVersion("00000000-0000-0000-0000-000000000001", "4.0",
            Validity.until(midnight), midnight.minusDays(1))
        def newer = exchangeRateVersion("00000000-0000-0000-0000-000000000002", "4.5",
            Validity.from(noon), midnight.minusDays(2))
        ExchangeRateVersions versions = ExchangeRateVersions.from([base, newer])

        when: "the applicable version is looked up at midnight and at noon"
        def atStartOfDay = applicable(versions, EUR, PLN, midnight.toLocalDate().atStartOfDay())
        def atNoon = applicable(versions, EUR, PLN, noon)

        then: "the older version applies at midnight and the newer one from noon"
        atStartOfDay.isPresent()
        atNoon.isPresent()
        atStartOfDay.get().id() == componentVersionId("00000000-0000-0000-0000-000000000001")
        atNoon.get().id() == componentVersionId("00000000-0000-0000-0000-000000000002")
    }

    def "reports no applicable version in a gap between versions"() {
        given: "a version that ends and another that starts two days later"
        def end = LocalDateTime.parse("2026-09-26T23:59:59")
        def versions = ExchangeRateVersions.from([
            exchangeRateVersion("00000000-0000-0000-0000-000000000001", "4.0",
                Validity.until(end), end.minusDays(1)),
            exchangeRateVersion("00000000-0000-0000-0000-000000000002", "4.5",
                Validity.from(end.plusDays(2)), end.minusDays(1))
        ])

        when: "the applicable version is looked up one second after the first ends"
        def selected = applicable(versions, EUR, PLN, end.plusSeconds(1))

        then: "none is found"
        selected.isEmpty()
    }

    def "latestFor returns the most recent matching tuple regardless of its numeric value"() {
        given: "two euro-to-zloty Exchange Rate versions with the same validity and different values"
        Validity validity = Validity.from(LocalDateTime.parse("2025-01-01T00:00:00"))
        ExchangeRateVersions versions = ExchangeRateVersions.from([
            exchangeRateVersion("00000000-0000-0000-0000-000000000001", "4.0", validity,
                LocalDateTime.parse("2024-12-01T00:00:00")),
            exchangeRateVersion("00000000-0000-0000-0000-000000000002", "4.5", validity,
                LocalDateTime.parse("2024-12-01T00:00:00"))
        ])

        when: "the latest version with that validity is looked up"
        def latest = versions.latestFor(EUR, PLN, validity)

        then: "the one listed last is returned, whatever its value"
        latest.isPresent()
        latest.get().id() == componentVersionId("00000000-0000-0000-0000-000000000002")
    }

    def "latestFor ignores an interleaved non-matching tuple and returns the last matching one"() {
        given: "a euro-to-zloty Exchange Rate version followed by an unrelated pound-to-zloty one"
        Validity validity = Validity.from(LocalDateTime.parse("2025-01-01T00:00:00"))
        ExchangeRateVersions versions = ExchangeRateVersions.from([
            exchangeRateVersion("00000000-0000-0000-0000-000000000001", "4.0", validity,
                LocalDateTime.parse("2024-12-01T00:00:00")),
            new ExchangeRateVersion(componentVersionId("00000000-0000-0000-0000-000000000002"),
                ExchangeRate.of(Monetary.getCurrency("GBP"), PLN, 9.9),
                Validity.between(LocalDateTime.parse("2025-03-01T00:00:00"),
                    LocalDateTime.parse("2025-03-15T00:00:00")),
                LocalDateTime.parse("2024-12-01T00:00:00"))
        ])

        when: "the latest euro-to-zloty version with that validity is looked up"
        def latest = versions.latestFor(EUR, PLN, validity)

        then: "the euro-to-zloty version is returned"
        latest.isPresent()
        latest.get().id() == componentVersionId("00000000-0000-0000-0000-000000000001")
    }

    def "reports no matching tuple for a different currency pair or validity"() {
        given: "a single Exchange Rate version valid at all times"
        Validity validity = Validity.from(LocalDateTime.parse("2025-01-01T00:00:00"))
        ExchangeRateVersions versions = ExchangeRateVersions.from([
            exchangeRateVersion("00000000-0000-0000-0000-000000000001", "4.0",
                Validity.always(), LocalDateTime.parse("2024-12-01T00:00:00"))
        ])

        when: "the latest version with a different validity is looked up"
        def latest = versions.latestFor(EUR, PLN, validity)

        then: "none is found"
        latest.isEmpty()
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
