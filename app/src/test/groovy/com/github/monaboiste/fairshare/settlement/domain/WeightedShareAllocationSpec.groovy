package com.github.monaboiste.fairshare.settlement.domain

import com.github.monaboiste.fairshare.quantity.money.Money
import spock.lang.Specification

class WeightedShareAllocationSpec extends Specification {
    private static final ParticipantId ADA = new ParticipantId(new UUID(0L, 11L))
    private static final ParticipantId BOB = new ParticipantId(new UUID(0L, 12L))
    private static final ParticipantId CAL = new ParticipantId(new UUID(0L, 13L))

    def "weighted Shares rank exact remainders and identifier ties in #currency"() {
        given: "a weighted Share Allocation of one, two and one"
        def allocation = new WeightedShareAllocation(new LinkedHashMap<ParticipantId, Integer>([
            (CAL): 1, (BOB): 2, (ADA): 1
        ]))

        when: "a total in the currency is divided into Shares"
        def shares = allocation.resolve(Money.of(total, currency))

        then: "leftover units go by remainder and then by Participant identifier, and the Shares add up to the total"
        shares*.participantId() == [CAL, BOB, ADA]
        shares*.amount()*.value() == expected
        shares*.amount().inject(Money.zero(currency)) { sum, share -> sum.add(share) } == Money.of(total, currency)

        where:
        currency | total | expected
        "JPY"    | 7     | [2, 3, 2]*.toBigDecimal()
        "EUR"    | 0.07  | [0.02, 0.03, 0.02]*.toBigDecimal()
        "KWD"    | 0.007 | [0.002, 0.003, 0.002]*.toBigDecimal()
    }

    def "largest remainder beats identifier order"() {
        given: "a weighted Share Allocation of five, three and two"
        def allocation = new WeightedShareAllocation(new LinkedHashMap<ParticipantId, Integer>([
            (CAL): 5, (BOB): 3, (ADA): 2
        ]))

        expect: "the leftover cent goes to the largest remainder rather than by Participant identifier"
        allocation.resolve(Money.of(0.07, "EUR")) == [
            new Share(CAL, Money.of(0.04, "EUR")), new Share(BOB, Money.of(0.02, "EUR")),
            new Share(ADA, Money.of(0.01, "EUR"))
        ]
    }

    def "large weight totals do not overflow"() {
        given: "a weighted Share Allocation whose weights add up to more than the largest supported whole number"
        def allocation = new WeightedShareAllocation(new LinkedHashMap<ParticipantId, Integer>([
            (BOB): Integer.MAX_VALUE, (ADA): 1, (CAL): Integer.MAX_VALUE
        ]))

        when: "three cents are divided into Shares"
        def shares = allocation.resolve(Money.of(0.03, "EUR"))

        then: "the Shares are divided correctly and the smallest weight receives nothing"
        shares == [new Share(BOB, Money.of(0.02, "EUR")), new Share(CAL, Money.of(0.01, "EUR"))]
    }

    def "zero Shares are omitted without removing recipients"() {
        given: "a weighted Share Allocation among three Participants"
        def allocation = new WeightedShareAllocation(new LinkedHashMap<ParticipantId, Integer>([
            (CAL): 1, (BOB): 1, (ADA): 2
        ]))

        when: "a single cent is divided into Shares"
        def shares = allocation.resolve(Money.of(0.01, "EUR"))

        then: "only one Participant receives a Share, while all three remain recipients"
        allocation.recipients().toList() == [CAL, BOB, ADA]
        shares == [new Share(ADA, Money.of(0.01, "EUR"))]
    }

    def "weighted identity retains literal values and presentation order"() {
        given: "a weighted Share Allocation whose source weights later change, and one with the same weights"
        def source = new LinkedHashMap<ParticipantId, Integer>([(CAL): 2, (BOB): 2])
        def allocation = new WeightedShareAllocation(source)
        source[ADA] = 1
        def identical = new WeightedShareAllocation(new LinkedHashMap<ParticipantId, Integer>([(CAL): 2, (BOB): 2]))

        when: "their identities are compared"
        def sameHash = allocation.hashCode() == identical.hashCode()

        then: "they match, while different weights or a different order make a different allocation"
        allocation == identical
        sameHash
        allocation != new WeightedShareAllocation(new LinkedHashMap<ParticipantId, Integer>([(CAL): 1, (BOB): 1]))
        allocation != new WeightedShareAllocation(new LinkedHashMap<ParticipantId, Integer>([(BOB): 2, (CAL): 2]))
        allocation.recipients().toList() == [CAL, BOB]
        allocation.hashCode() != new WeightedShareAllocation(new LinkedHashMap<ParticipantId, Integer>([
            (CAL): 1, (BOB): 2
        ])).hashCode()
        allocation.hashCode() != new WeightedShareAllocation(new LinkedHashMap<ParticipantId, Integer>([
            (BOB): 2, (CAL): 2
        ])).hashCode()

        when: "its weights are changed afterwards"
        allocation.weights().put(ADA, 1)

        then: "the change is refused"
        thrown(UnsupportedOperationException)
    }
}
