package com.github.monaboiste.fairshare.settlement.domain

import spock.lang.Specification

class ParticipantNameSpec extends Specification {
    def "blank Participant names reject"() {
        when: "a Participant name is made from blank text"
        new ParticipantName(value)

        then: "it is rejected"
        thrown(IllegalArgumentException)

        where:
        value << ["", "  ", "\t"]
    }

    def "non-blank Participant names preserve spacing"() {
        expect:
        new ParticipantName(" Ada ").value() == " Ada "
    }
}
