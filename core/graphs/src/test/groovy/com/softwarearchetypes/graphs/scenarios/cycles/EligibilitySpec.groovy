package com.softwarearchetypes.graphs.scenarios.cycles

import com.softwarearchetypes.graphs.fixture.cycles.Eligibility
import com.softwarearchetypes.graphs.fixture.cycles.OwnerId
import spock.lang.Specification

class EligibilitySpec extends Specification {

    def "marks a transfer as eligible"() {
        given:
        Eligibility eligibility = new Eligibility()
        OwnerId alice = OwnerId.of("Alice")
        OwnerId bob = OwnerId.of("Bob")

        when:
        eligibility.markTransferEligible(alice, bob)

        then:
        eligibility.isTransferEligible(alice, bob)
    }

    def "a transfer is ineligible by default"() {
        given:
        Eligibility eligibility = new Eligibility()
        OwnerId alice = OwnerId.of("Alice")
        OwnerId bob = OwnerId.of("Bob")

        when:
        boolean transferIsEligible = eligibility.isTransferEligible(alice, bob)

        then:
        !transferIsEligible
    }

    def "marks a transfer as ineligible"() {
        given:
        Eligibility eligibility = new Eligibility()
        OwnerId alice = OwnerId.of("Alice")
        OwnerId bob = OwnerId.of("Bob")
        eligibility.markTransferEligible(alice, bob)

        when:
        eligibility.markTransferIneligible(alice, bob)

        then:
        !eligibility.isTransferEligible(alice, bob)
    }

    def "transfer eligibility is asymmetric"() {
        given:
        Eligibility eligibility = new Eligibility()
        OwnerId alice = OwnerId.of("Alice")
        OwnerId bob = OwnerId.of("Bob")

        when:
        eligibility.markTransferEligible(alice, bob)

        then:
        eligibility.isTransferEligible(alice, bob)
        !eligibility.isTransferEligible(bob, alice)
    }

    def "marks multiple transfers as eligible"() {
        given:
        Eligibility eligibility = new Eligibility()
        OwnerId alice = OwnerId.of("Alice")
        OwnerId bob = OwnerId.of("Bob")
        OwnerId charlie = OwnerId.of("Charlie")

        when:
        eligibility.markTransferEligible(alice, bob)
        eligibility.markTransferEligible(bob, charlie)
        eligibility.markTransferEligible(charlie, alice)

        then:
        eligibility.isTransferEligible(alice, bob)
        eligibility.isTransferEligible(bob, charlie)
        eligibility.isTransferEligible(charlie, alice)
    }
}
