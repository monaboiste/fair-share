package com.github.monaboiste.fairshare.settlement.domain

import com.github.monaboiste.fairshare.quantity.money.Money
import spock.lang.Specification

class EqualShareAllocationSpec extends Specification {
    private static final ParticipantId ADA = new ParticipantId(new UUID(0L, 11L))
    private static final ParticipantId BOB = new ParticipantId(new UUID(0L, 12L))
    private static final ParticipantId CAL = new ParticipantId(new UUID(0L, 13L))

    def "residual units follow Participant identifiers regardless of presentation order"() {
        given: "an equal Share Allocation among three Participants listed in varying order"
        def allocation = new EqualShareAllocation(presentation)

        when: "ten euros are divided into Shares"
        def shares = allocation.resolve(Money.of(10, "EUR"))

        then: "Shares keep the listed order, the leftover cent goes by Participant identifier, and they add up to ten euros"
        shares*.participantId() == presentation
        shares*.amount()*.value() == amounts
        shares*.amount().inject(Money.zero("EUR")) { sum, share -> sum.add(share) } == Money.of(10, "EUR")

        where:
        presentation    | amounts
        [CAL, ADA, BOB] | [3.33, 3.34, 3.33]*.toBigDecimal()
        [BOB, CAL, ADA] | [3.33, 3.33, 3.34]*.toBigDecimal()
    }

    def "allocation identity includes recipient order"() {
        given: "an equal Share Allocation, a copy that repeats a Participant, and one listing Participants in another order"
        def allocation = new EqualShareAllocation([CAL, BOB, ADA])
        def identical = new EqualShareAllocation([CAL, BOB, ADA, BOB])
        def reordered = new EqualShareAllocation([BOB, CAL, ADA])

        when: "their identities are compared"
        def sameHash = allocation.hashCode() == identical.hashCode()
        def differentHash = allocation.hashCode() != reordered.hashCode()

        then: "repeating a Participant makes no difference, but a different order makes a different allocation"
        allocation == identical
        allocation != reordered
        sameHash
        differentHash
    }

    def "zero Shares are omitted while recipients remain allocated"() {
        given: "an equal Share Allocation among three Participants"
        def allocation = new EqualShareAllocation([CAL, BOB, ADA])

        when: "a single cent is divided into Shares"
        def shares = allocation.resolve(Money.of(0.01, "EUR"))

        then: "only one Participant receives a Share, while all three remain recipients"
        allocation.recipients().toList() == [CAL, BOB, ADA]
        shares == [new Share(ADA, Money.of(0.01, "EUR"))]
    }

    def "allocation respects the currency's smallest unit"() {
        given: "an equal Share Allocation among three Participants and an amount in a currency with its own smallest unit"
        def allocation = new EqualShareAllocation([CAL, BOB, ADA])
        def amount = Money.of(total, currency)

        when: "the amount is divided into Shares"
        def shares = allocation.resolve(amount)

        then: "each Share is a whole number of the currency's smallest unit and the Shares add up to the amount"
        shares*.participantId() == [CAL, BOB, ADA]
        shares*.amount()*.value() == amounts
        shares*.amount().inject(Money.zero(currency)) { sum, share -> sum.add(share) } == amount

        where:
        currency | total | amounts
        "JPY"    | 10    | [3, 3, 4]*.toBigDecimal()
        "KWD"    | 0.010 | [0.003, 0.003, 0.004]*.toBigDecimal()
    }

    def "an empty allocation cannot be resolved"() {
        given: "an equal Share Allocation with no Participants"
        def allocation = new EqualShareAllocation([])

        when: "one euro is divided into Shares"
        allocation.resolve(Money.of(1, "EUR"))

        then: "the division is refused"
        thrown(IllegalStateException)
    }
}
