package com.github.monaboiste.fairshare.settlement.domain

import com.github.monaboiste.fairshare.quantity.money.Money
import spock.lang.Specification

class ExactShareAllocationSpec extends Specification {
    private static final ParticipantId ADA = new ParticipantId(new UUID(0L, 11L))
    private static final ParticipantId BOB = new ParticipantId(new UUID(0L, 12L))
    private static final ParticipantId CAL = new ParticipantId(new UUID(0L, 13L))

    def "exact Share Allocation refuses a #missingValue"() {
        given: "an allocation missing a recipient or its amount"
        def amounts = new LinkedHashMap<ParticipantId, Money>(incomplete)

        when: "the Share Allocation is created"
        new ExactShareAllocation(amounts)

        then: "the incomplete allocation is refused"
        thrown(NullPointerException)

        where:
        missingValue        | incomplete
        "missing recipient" | [(null): Money.of(1, "EUR")]
        "missing amount"    | [(ADA): null]
    }

    def "sub-unit exact amounts apportion valued #currency without intermediate rounding"() {
        given: "an exact Share Allocation with amounts smaller than the currency's smallest unit"
        def allocation = new ExactShareAllocation(new LinkedHashMap<ParticipantId, Money>([
                (CAL): Money.of(calShare, currency), (BOB): Money.of(bobShare, currency),
                (ADA): Money.of(adaShare, currency)
        ]))

        when: "a Valuation in that currency is divided into Shares"
        def shares = allocation.resolve(Money.of(valuation, currency))

        then: "Shares follow the exact proportions without early rounding and add up to the Valuation"
        shares*.participantId() == [CAL, BOB, ADA]
        shares*.amount()*.value() == expected
        shares*.amount().inject(Money.zero(currency)) { sum, share -> sum.add(share) } == Money.of(valuation, currency)

        where:
        currency | calShare | bobShare | adaShare | valuation | expected
        "JPY"    | 0.25     | 0.5      | 0.25     | 7         | [2, 3, 2]*.toBigDecimal()
        "EUR"    | 0.0025   | 0.005    | 0.0025   | 0.07      | [0.02, 0.03, 0.02]*.toBigDecimal()
        "KWD"    | 0.00025  | 0.0005   | 0.00025  | 0.007     | [0.002, 0.003, 0.002]*.toBigDecimal()
    }

    def "exact largest remainder outranks Participant identifier"() {
        given: "an exact Share Allocation whose amounts leave different remainders"
        def allocation = new ExactShareAllocation(new LinkedHashMap<ParticipantId, Money>([
                (CAL): Money.of(0.005, "EUR"), (BOB): Money.of(0.003, "EUR"),
                (ADA): Money.of(0.002, "EUR")
        ]))

        expect: "the leftover cent goes to the largest remainder rather than by Participant identifier"
        allocation.resolve(Money.of(0.07, "EUR")) == [
                new Share(CAL, Money.of(0.04, "EUR")), new Share(BOB, Money.of(0.02, "EUR")),
                new Share(ADA, Money.of(0.01, "EUR"))
        ]
    }

    def "exact proportions use valuation rather than the original amount"() {
        given: "an exact Share Allocation of five euros and of five euros and half a cent"
        def allocation = new ExactShareAllocation(new LinkedHashMap<ParticipantId, Money>([
                (CAL): Money.of(5, "EUR"), (BOB): Money.of(5.005, "EUR")
        ]))

        when: "a Valuation of ten euros and one cent is divided into Shares"
        def shares = allocation.resolve(Money.of(10.01, "EUR"))

        then: "Shares follow the Valuation rather than the original exact amounts"
        shares == [new Share(CAL, Money.of(5, "EUR")), new Share(BOB, Money.of(5.01, "EUR"))]
    }

    def "exact amounts in one currency apportion a Valuation in another"() {
        given: "an exact Share Allocation defined in US dollars"
        def allocation = new ExactShareAllocation(new LinkedHashMap<ParticipantId, Money>([
                (CAL): Money.of(3, "USD"), (BOB): Money.of(7, "USD")
        ]))

        when: "a Valuation in euros is divided into Shares"
        def shares = allocation.resolve(Money.of(10, "EUR"))

        then: "Shares keep the dollar proportions in euros and add up to the Valuation"
        shares == [new Share(CAL, Money.of(3, "EUR")), new Share(BOB, Money.of(7, "EUR"))]
        shares*.amount().inject(Money.zero("EUR")) { sum, share -> sum.add(share) } == Money.of(10, "EUR")
    }

    def "mixed-currency exact amounts cannot be resolved"() {
        given: "an exact Share Allocation mixing US dollars and euros"
        def allocation = new ExactShareAllocation(new LinkedHashMap<ParticipantId, Money>([
                (CAL): Money.of(3, "USD"), (BOB): Money.of(7, "EUR")
        ]))

        when: "a Valuation is divided into Shares"
        allocation.resolve(Money.of(10, "EUR"))

        then: "the division is refused"
        thrown(IllegalStateException)
    }

    def "exact allocation omits zero Shares and retains ordered recipients"() {
        given: "an exact Share Allocation among three Participants"
        def allocation = new ExactShareAllocation(new LinkedHashMap<ParticipantId, Money>([
                (CAL): Money.of(1, "EUR"), (BOB): Money.of(1, "EUR"), (ADA): Money.of(2, "EUR")
        ]))

        when: "a single cent is divided into Shares"
        def shares = allocation.resolve(Money.of(0.01, "EUR"))

        then: "only one Participant receives a Share, while all three remain recipients"
        allocation.recipients().toList() == [CAL, BOB, ADA]
        shares == [new Share(ADA, Money.of(0.01, "EUR"))]
    }

    def "exact identity is numeric and currency-aware but ordered"() {
        given: "an exact Share Allocation whose source later changes, and one with numerically equal amounts"
        def source = new LinkedHashMap<ParticipantId, Money>([(CAL): Money.of(10, "EUR"), (BOB): Money.of(2, "EUR")])
        def allocation = new ExactShareAllocation(source)
        source[ADA] = Money.of(1, "EUR")
        def identical = new ExactShareAllocation(new LinkedHashMap<ParticipantId, Money>([
                (CAL): Money.of(new BigDecimal("10.00"), "EUR"), (BOB): Money.of(new BigDecimal("2.0"), "EUR")
        ]))

        when: "their identities are compared"
        def sameHash = allocation.hashCode() == identical.hashCode()

        then: "they match, while a different order, currency, recipient list or amount makes a different allocation"
        allocation == identical
        sameHash
        allocation != new ExactShareAllocation(new LinkedHashMap<ParticipantId, Money>([
                (BOB): Money.of(2, "EUR"), (CAL): Money.of(10, "EUR")
        ]))
        allocation != new ExactShareAllocation(new LinkedHashMap<ParticipantId, Money>([
                (CAL): Money.of(10, "USD"), (BOB): Money.of(2, "EUR")
        ]))
        allocation.recipients().toList() == [CAL, BOB]
        allocation != new ExactShareAllocation(new LinkedHashMap<ParticipantId, Money>([
                (CAL): Money.of(10, "EUR")
        ]))
        allocation.hashCode() != new ExactShareAllocation(new LinkedHashMap<ParticipantId, Money>([
                (BOB): Money.of(2, "EUR"), (CAL): Money.of(10, "EUR")
        ])).hashCode()
        allocation.hashCode() != new ExactShareAllocation(new LinkedHashMap<ParticipantId, Money>([
                (CAL): Money.of(11, "EUR"), (BOB): Money.of(2, "EUR")
        ])).hashCode()

        when: "its amounts are changed afterwards"
        allocation.amounts().put(ADA, Money.of(1, "EUR"))

        then: "the change is refused"
        thrown(UnsupportedOperationException)
    }
}
