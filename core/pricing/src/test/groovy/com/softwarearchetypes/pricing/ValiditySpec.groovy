package com.softwarearchetypes.pricing

import java.time.LocalDateTime
import spock.lang.Specification

class ValiditySpec extends Specification {

    def "validity from a date has no end boundary"() {
        given:
        LocalDateTime from = LocalDateTime.of(2024, 1, 1, 0, 0)

        and:
        Validity validity = Validity.from(from)

        expect:
        validity.from() == from
        validity.to() == null
    }

    def "validity until a date has no start boundary"() {
        given:
        LocalDateTime to = LocalDateTime.of(2024, 2, 1, 0, 0)

        and:
        Validity validity = Validity.until(to)

        expect:
        validity.from() == null
        validity.to() == to
    }

    def "validity between two dates has both boundaries set"() {
        given:
        LocalDateTime from = LocalDateTime.of(2024, 1, 1, 0, 0)
        LocalDateTime to = LocalDateTime.of(2024, 2, 1, 0, 0)

        and:
        Validity validity = Validity.between(from, to)

        expect:
        validity.from() == from
        validity.to() == to
    }

    def "invalid date range with from after to is rejected"() {
        given:
        LocalDateTime from = LocalDateTime.of(2024, 2, 1, 0, 0)
        LocalDateTime to = LocalDateTime.of(2024, 1, 1, 0, 0)

        when:
        Validity.between(from, to)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("Start date must not be after end date")
    }

    def "isValidAt returns true only within the inclusive validity window"() {
        given:
        Validity validity = Validity.between(
                LocalDateTime.of(2024, 2, 1, 0, 0),
                LocalDateTime.of(2024, 3, 1, 0, 0)
        )

        expect:
        !(validity.isValidAt(LocalDateTime.of(2024, 1, 31, 23, 59)))
        validity.isValidAt(LocalDateTime.of(2024, 2, 1, 0, 0))
        validity.isValidAt(LocalDateTime.of(2024, 2, 15, 0, 0))
        validity.isValidAt(LocalDateTime.of(2024, 2, 28, 23, 59))
        validity.isValidAt(LocalDateTime.of(2024, 3, 1, 0, 0))
        !(validity.isValidAt(LocalDateTime.of(2024, 3, 2, 0, 0)))
    }

    def "overlapping validity periods are detected"() {
        given:
        Validity v1 = Validity.between(
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 3, 1, 0, 0)
        )
        Validity v2 = Validity.between(
                LocalDateTime.of(2024, 2, 1, 0, 0),
                LocalDateTime.of(2024, 4, 1, 0, 0)
        )

        expect:
        v1.overlaps(v2)
        v2.overlaps(v1)
    }

    def "validity periods sharing a boundary are overlapping"() {
        given:
        Validity v1 = Validity.between(
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 2, 1, 0, 0)
        )
        Validity v2 = Validity.between(
                LocalDateTime.of(2024, 2, 1, 0, 0),
                LocalDateTime.of(2024, 3, 1, 0, 0)
        )

        expect:
        v1.overlaps(v2)
        v2.overlaps(v1)
    }

    def "open-ended validity overlaps with any later period"() {
        given:
        Validity openEnded = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        Validity limited = Validity.between(
                LocalDateTime.of(2024, 2, 1, 0, 0),
                LocalDateTime.of(2024, 3, 1, 0, 0)
        )

        expect:
        openEnded.overlaps(limited)
        limited.overlaps(openEnded)
        openEnded.isValidAt(LocalDateTime.of(2100, 1, 1, 0, 0))
    }

    def "hasExpired returns true only after the inclusive end date"() {
        given:
        Validity validity = Validity.between(
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 2, 1, 0, 0)
        )

        expect:
        !(validity.hasExpired(LocalDateTime.of(2024, 1, 15, 0, 0)))
        !(validity.hasExpired(LocalDateTime.of(2024, 2, 1, 0, 0)))
        validity.hasExpired(LocalDateTime.of(2024, 3, 1, 0, 0))
    }

    def "hasNotStartedYet returns true only before the start date"() {
        given:
        Validity validity = Validity.from(LocalDateTime.of(2024, 2, 1, 0, 0))

        expect:
        validity.hasNotStartedYet(LocalDateTime.of(2024, 1, 15, 0, 0))
        !(validity.hasNotStartedYet(LocalDateTime.of(2024, 2, 1, 0, 0)))
        !(validity.hasNotStartedYet(LocalDateTime.of(2024, 3, 1, 0, 0)))
    }

    def "always validity is valid at any point in time"() {
        given:
        Validity always = Validity.always()

        expect:
        always.isValidAt(LocalDateTime.MIN)
        always.isValidAt(LocalDateTime.of(2024, 1, 1, 0, 0))
        always.isValidAt(LocalDateTime.MAX)
        always.from() == null
        always.to() == null
    }
}
