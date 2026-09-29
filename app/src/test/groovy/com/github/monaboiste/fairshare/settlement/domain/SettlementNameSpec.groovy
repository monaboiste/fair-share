package com.github.monaboiste.fairshare.settlement.domain

import spock.lang.Specification

class SettlementNameSpec extends Specification {
    def "blank Settlement names are rejected"() {
        when: "a Settlement name is made from spaces and a tab"
        new SettlementName(" \t")

        then: "it is rejected"
        thrown(IllegalArgumentException)
    }

    def "surrounding spaces in a Settlement name are preserved"() {
        expect:
        new SettlementName("  Holiday  ").value() == "  Holiday  "
    }
}
