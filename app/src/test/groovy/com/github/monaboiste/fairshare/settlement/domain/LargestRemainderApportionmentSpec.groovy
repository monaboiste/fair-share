package com.github.monaboiste.fairshare.settlement.domain

import com.github.monaboiste.fairshare.quantity.money.Money
import spock.lang.Specification

class LargestRemainderApportionmentSpec extends Specification {
    private static final ParticipantId ADA = new ParticipantId(new UUID(0L, 11L))

    def "zero proportion cannot receive a Share"() {
        given:
        def proportions = new LinkedHashMap<ParticipantId, BigInteger>([(ADA): BigInteger.ZERO])

        when:
        LargestRemainderApportionment.apportion(proportions, Money.of(1, "EUR"))

        then:
        thrown(IllegalStateException)
    }

    def "zero Valuation resolves to no Shares"() {
        given:
        def allocation = new WeightedShareAllocation(new LinkedHashMap<ParticipantId, Integer>([(ADA): 1]))

        expect:
        allocation.resolve(Money.zero("EUR")).empty
    }

    def "negative Valuation cannot resolve Shares"() {
        given:
        def allocation = new WeightedShareAllocation(new LinkedHashMap<ParticipantId, Integer>([(ADA): 1]))

        when:
        allocation.resolve(Money.of(-0.01, "EUR"))

        then:
        thrown(IllegalStateException)
    }
}
