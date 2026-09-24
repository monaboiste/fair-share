package com.github.monaboiste.fairshare.settlement.domain

import spock.lang.Specification

class SettlementNameSpec extends Specification {
    def "blank Settlement names are rejected"() {
        when:
        new SettlementName(" \t")

        then:
        thrown(IllegalArgumentException)
    }

    def "surrounding spaces in a Settlement name are preserved"() {
        expect:
        new SettlementName("  Holiday  ").value() == "  Holiday  "
    }
}
