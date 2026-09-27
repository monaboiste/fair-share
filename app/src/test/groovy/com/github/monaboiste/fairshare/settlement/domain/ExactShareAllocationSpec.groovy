package com.github.monaboiste.fairshare.settlement.domain

import com.github.monaboiste.fairshare.quantity.money.Money
import spock.lang.Specification

class ExactShareAllocationSpec extends Specification {
    private static final ParticipantId ADA = new ParticipantId(new UUID(0L, 11L))
    private static final ParticipantId BOB = new ParticipantId(new UUID(0L, 12L))
    private static final ParticipantId CAL = new ParticipantId(new UUID(0L, 13L))

    def "sub-unit exact amounts apportion valued #currency without intermediate rounding"() {
        given:
        def allocation = new ExactShareAllocation(new LinkedHashMap<ParticipantId, Money>([
            (CAL): Money.of(first, currency), (BOB): Money.of(second, currency), (ADA): Money.of(third, currency)
        ]))

        when:
        def shares = allocation.resolve(Money.of(valuation, currency))

        then:
        shares*.participantId() == [CAL, BOB, ADA]
        shares*.amount()*.value() == expected
        shares*.amount().inject(Money.zero(currency)) { sum, share -> sum.add(share) }.compareTo(Money.of(valuation, currency)) == 0

        where:
        currency | first   | second  | third   | valuation | expected
        "JPY"    | 0.25    | 0.5     | 0.25    | 7         | [2, 3, 2]*.toBigDecimal()
        "EUR"    | 0.0025  | 0.005   | 0.0025  | 0.07      | [0.02, 0.03, 0.02]*.toBigDecimal()
        "KWD"    | 0.00025 | 0.0005  | 0.00025 | 0.007     | [0.002, 0.003, 0.002]*.toBigDecimal()
    }

    def "exact largest remainder outranks Participant identifier"() {
        given:
        def allocation = new ExactShareAllocation(new LinkedHashMap<ParticipantId, Money>([
            (CAL): Money.of(0.005, "EUR"), (BOB): Money.of(0.003, "EUR"),
            (ADA): Money.of(0.002, "EUR")
        ]))

        expect:
        allocation.resolve(Money.of(0.07, "EUR")) == [
            new Share(CAL, Money.of(0.04, "EUR")), new Share(BOB, Money.of(0.02, "EUR")),
            new Share(ADA, Money.of(0.01, "EUR"))
        ]
    }

    def "exact proportions use valuation rather than the original amount"() {
        given:
        def allocation = new ExactShareAllocation(new LinkedHashMap<ParticipantId, Money>([
            (CAL): Money.of(5, "EUR"), (BOB): Money.of(5.005, "EUR")
        ]))

        when:
        def shares = allocation.resolve(Money.of(10.01, "EUR"))

        then:
        shares == [new Share(CAL, Money.of(5, "EUR")), new Share(BOB, Money.of(5.01, "EUR"))]
    }

    def "exact allocation omits zero Shares and retains ordered recipients"() {
        given:
        def allocation = new ExactShareAllocation(new LinkedHashMap<ParticipantId, Money>([
            (CAL): Money.of(1, "EUR"), (BOB): Money.of(1, "EUR"), (ADA): Money.of(2, "EUR")
        ]))

        when:
        def shares = allocation.resolve(Money.of(0.01, "EUR"))

        then:
        allocation.recipients().toList() == [CAL, BOB, ADA]
        shares == [new Share(ADA, Money.of(0.01, "EUR"))]
    }

    def "exact identity is numeric and currency-aware but ordered"() {
        given:
        def source = new LinkedHashMap<ParticipantId, Money>([(CAL): Money.of(10, "EUR"), (BOB): Money.of(2, "EUR")])
        def allocation = new ExactShareAllocation(source)
        source[ADA] = Money.of(1, "EUR")
        def identical = new ExactShareAllocation(new LinkedHashMap<ParticipantId, Money>([
            (CAL): Money.of(new BigDecimal("10.00"), "EUR"), (BOB): Money.of(new BigDecimal("2.0"), "EUR")
        ]))

        expect:
        allocation == identical
        allocation.hashCode() == identical.hashCode()
        allocation != new ExactShareAllocation(new LinkedHashMap<ParticipantId, Money>([
            (BOB): Money.of(2, "EUR"), (CAL): Money.of(10, "EUR")
        ]))
        allocation != new ExactShareAllocation(new LinkedHashMap<ParticipantId, Money>([
            (CAL): Money.of(10, "USD"), (BOB): Money.of(2, "EUR")
        ]))
        allocation.recipients().toList() == [CAL, BOB]

        when:
        allocation.amounts().put(ADA, Money.of(1, "EUR"))

        then:
        thrown(UnsupportedOperationException)
    }
}
