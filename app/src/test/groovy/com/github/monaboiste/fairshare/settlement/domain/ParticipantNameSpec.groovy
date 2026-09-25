package com.github.monaboiste.fairshare.settlement.domain

import spock.lang.Specification

class ParticipantNameSpec extends Specification {
    def "blank Participant names reject"() {
        when:
        new ParticipantName(value)

        then:
        thrown(IllegalArgumentException)

        where:
        value << ["", "  ", "\t"]
    }

    def "non-blank Participant names preserve spacing"() {
        expect:
        new ParticipantName(" Ada ").value() == " Ada "
    }
}
