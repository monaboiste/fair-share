package com.softwarearchetypes.pricing

import java.time.LocalDateTime
import spock.lang.Specification



class ValiditySpec extends Specification {
    def "shouldCreateValidityFromDate"() {
        given:
        LocalDateTime from = LocalDateTime.of(2024, 1, 1, 0, 0)
        and:
        Validity validity = Validity.from(from)
        and:
        assert validity.validFrom() == from
        assert validity.validTo() == LocalDateTime.MAX
    }
    def "shouldCreateValidityBetweenDates"() {
        given:
        LocalDateTime from = LocalDateTime.of(2024, 1, 1, 0, 0)
        LocalDateTime to = LocalDateTime.of(2024, 2, 1, 0, 0)
        and:
        Validity validity = Validity.between(from, to)
        and:
        assert validity.validFrom() == from
        assert validity.validTo() == to
    }
    def "shouldRejectInvalidRange"() {
        given:
        LocalDateTime from = LocalDateTime.of(2024, 2, 1, 0, 0)
        LocalDateTime to = LocalDateTime.of(2024, 1, 1, 0, 0)
        and:
        shouldFail(IllegalArgumentException) { Validity.between(from, to) }.message.contains("validFrom must be before validTo")
    }
    def "shouldCheckIfValidAt"() {
        given:
        Validity validity = Validity.between(
                LocalDateTime.of(2024, 2, 1, 0, 0),
                LocalDateTime.of(2024, 3, 1, 0, 0)
        )
        and:
        assert !(validity.isValidAt(LocalDateTime.of(2024, 1, 31, 23, 59)))
        assert validity.isValidAt(LocalDateTime.of(2024, 2, 1, 0, 0))
        assert validity.isValidAt(LocalDateTime.of(2024, 2, 15, 0, 0))
        assert validity.isValidAt(LocalDateTime.of(2024, 2, 28, 23, 59))
        assert !(validity.isValidAt(LocalDateTime.of(2024, 3, 1, 0, 0)))
        assert !(validity.isValidAt(LocalDateTime.of(2024, 3, 2, 0, 0)))
    }
    def "shouldDetectOverlappingPeriods"() {
        given:
        Validity v1 = Validity.between(
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 3, 1, 0, 0)
        )
        Validity v2 = Validity.between(
                LocalDateTime.of(2024, 2, 1, 0, 0),
                LocalDateTime.of(2024, 4, 1, 0, 0)
        )
        and:
        assert v1.overlaps(v2)
        assert v2.overlaps(v1)
    }
    def "shouldDetectNonOverlappingPeriods"() {
        given:
        Validity v1 = Validity.between(
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 2, 1, 0, 0)
        )
        Validity v2 = Validity.between(
                LocalDateTime.of(2024, 2, 1, 0, 0),
                LocalDateTime.of(2024, 3, 1, 0, 0)
        )
        and:
        assert !(v1.overlaps(v2))
        assert !(v2.overlaps(v1))
    }
    def "shouldHandleOpenEndedValidity"() {
        given:
        Validity openEnded = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        Validity limited = Validity.between(
                LocalDateTime.of(2024, 2, 1, 0, 0),
                LocalDateTime.of(2024, 3, 1, 0, 0)
        )
        and:
        assert openEnded.overlaps(limited)
        assert limited.overlaps(openEnded)
        assert openEnded.isValidAt(LocalDateTime.of(2100, 1, 1, 0, 0))
    }
    def "shouldCheckIfExpired"() {
        given:
        Validity validity = Validity.between(
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 2, 1, 0, 0)
        )
        and:
        assert !(validity.hasExpired(LocalDateTime.of(2024, 1, 15, 0, 0)))
        assert validity.hasExpired(LocalDateTime.of(2024, 2, 1, 0, 0))
        assert validity.hasExpired(LocalDateTime.of(2024, 3, 1, 0, 0))
    }
    def "shouldCheckIfNotStartedYet"() {
        given:
        Validity validity = Validity.from(LocalDateTime.of(2024, 2, 1, 0, 0))
        and:
        assert validity.hasNotStartedYet(LocalDateTime.of(2024, 1, 15, 0, 0))
        assert !(validity.hasNotStartedYet(LocalDateTime.of(2024, 2, 1, 0, 0)))
        assert !(validity.hasNotStartedYet(LocalDateTime.of(2024, 3, 1, 0, 0)))
    }
    def "shouldCreateAlwaysValidity"() {
        given:
        Validity always = Validity.always()
        and:
        assert always.isValidAt(LocalDateTime.MIN)
        assert always.isValidAt(LocalDateTime.of(2024, 1, 1, 0, 0))
        assert always.isValidAt(LocalDateTime.MAX.minusYears(1))
    }

    private static Throwable shouldFail(Class<? extends Throwable> type, Closure action) {
        try {
            action.call()
        } catch (Throwable exception) {
            assert type.isInstance(exception)
            return exception
        }
        throw new AssertionError("Expected " + type.simpleName)
    }
}
