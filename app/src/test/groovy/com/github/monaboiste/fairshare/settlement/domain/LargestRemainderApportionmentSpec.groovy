package com.github.monaboiste.fairshare.settlement.domain

import com.github.monaboiste.fairshare.quantity.money.Money
import spock.lang.Specification

class LargestRemainderApportionmentSpec extends Specification {
    private static final ParticipantId ADA = new ParticipantId(new UUID(0L, 11L))

    def "zero proportion cannot receive a Share"() {
        given: "a single Participant with a proportion of zero"
        def proportions = new LinkedHashMap<ParticipantId, BigInteger>([(ADA): BigInteger.ZERO])

        when: "one euro is apportioned"
        LargestRemainderApportionment.apportion(proportions, Money.of(1, "EUR"))

        then: "the apportionment is refused"
        thrown(IllegalStateException)
    }

    def "zero Valuation resolves to no Shares"() {
        given: "a weighted Share Allocation for a single Participant"
        def allocation = new WeightedShareAllocation(new LinkedHashMap<ParticipantId, Integer>([(ADA): 1]))

        expect:
        allocation.resolve(Money.zero("EUR")).empty
    }

    def "negative Valuation cannot resolve Shares"() {
        given: "a weighted Share Allocation for a single Participant"
        def allocation = new WeightedShareAllocation(new LinkedHashMap<ParticipantId, Integer>([(ADA): 1]))

        when: "a negative Valuation is divided into Shares"
        allocation.resolve(Money.of(-0.01, "EUR"))

        then: "the division is refused"
        thrown(IllegalStateException)
    }
}
