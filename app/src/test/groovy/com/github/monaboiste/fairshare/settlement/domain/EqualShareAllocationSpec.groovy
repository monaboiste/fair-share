package com.github.monaboiste.fairshare.settlement.domain

import com.github.monaboiste.fairshare.quantity.money.Money
import spock.lang.Specification

class EqualShareAllocationSpec extends Specification {
    private static final ParticipantId ADA = new ParticipantId(new UUID(0L, 11L))
    private static final ParticipantId BOB = new ParticipantId(new UUID(0L, 12L))
    private static final ParticipantId CAL = new ParticipantId(new UUID(0L, 13L))

    def "residual units follow Participant identifiers regardless of presentation order"() {
        given:
        def allocation = new EqualShareAllocation(presentation)

        when:
        def shares = allocation.resolve(Money.of(10, "EUR"))

        then:
        shares*.participantId() == presentation
        shares*.amount()*.value() == amounts
        shares*.amount().inject(Money.zero("EUR")) { sum, share -> sum.add(share) } == Money.of(10, "EUR")

        where:
        presentation    | amounts
        [CAL, ADA, BOB] | [3.33, 3.34, 3.33]*.toBigDecimal()
        [BOB, CAL, ADA] | [3.33, 3.33, 3.34]*.toBigDecimal()
    }

    def "allocation identity includes recipient order"() {
        given:
        def allocation = new EqualShareAllocation([CAL, BOB, ADA])
        def identical = new EqualShareAllocation([CAL, BOB, ADA, BOB])
        def reordered = new EqualShareAllocation([BOB, CAL, ADA])

        when:
        def sameHash = allocation.hashCode() == identical.hashCode()
        def differentHash = allocation.hashCode() != reordered.hashCode()

        then:
        allocation == identical
        allocation != reordered
        sameHash
        differentHash
    }

    def "zero Shares are omitted while recipients remain allocated"() {
        given:
        def allocation = new EqualShareAllocation([CAL, BOB, ADA])

        when:
        def shares = allocation.resolve(Money.of(0.01, "EUR"))

        then:
        allocation.recipients().toList() == [CAL, BOB, ADA]
        shares == [new Share(ADA, Money.of(0.01, "EUR"))]
    }

    def "allocation respects the currency's smallest unit"() {
        given:
        def allocation = new EqualShareAllocation([CAL, BOB, ADA])
        def amount = Money.of(total, currency)

        when:
        def shares = allocation.resolve(amount)

        then:
        shares*.participantId() == [CAL, BOB, ADA]
        shares*.amount()*.value() == amounts
        shares*.amount().inject(Money.zero(currency)) { sum, share -> sum.add(share) } == amount

        where:
        currency | total | amounts
        "JPY"    | 10    | [3, 3, 4]*.toBigDecimal()
        "KWD"    | 0.010 | [0.003, 0.003, 0.004]*.toBigDecimal()
    }

    def "an empty allocation cannot be resolved"() {
        given:
        def allocation = new EqualShareAllocation([])

        when:
        allocation.resolve(Money.of(1, "EUR"))

        then:
        thrown(IllegalStateException)
    }
}
