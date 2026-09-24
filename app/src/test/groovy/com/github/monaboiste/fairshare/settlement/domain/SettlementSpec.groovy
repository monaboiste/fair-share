package com.github.monaboiste.fairshare.settlement.domain

import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementRenamed
import java.time.Instant
import javax.money.Monetary
import spock.lang.Specification

class SettlementSpec extends Specification {
    private static final SettlementId ID = new SettlementId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private static final Instant OPENED_AT = Instant.parse("2026-01-01T12:00:00Z")
    private static final Instant RENAMED_AT = Instant.parse("2026-01-02T12:00:00Z")
    private static final EUR = Monetary.getCurrency("EUR")
    private static final USD = Monetary.getCurrency("USD")

    def "opening a Settlement records when and how it was opened"() {
        when:
        def settlement = Settlement.open(ID, "Holiday", EUR, OPENED_AT)

        then:
        settlement.id() == ID
        settlement.pendingEvents() == [new SettlementOpened("Holiday", EUR, OPENED_AT)]
        settlement.version() == 1
        settlement.committedVersion() == 0
    }

    def "renaming records the new name"() {
        given:
        def settlement = Settlement.open(ID, "Holiday", EUR, OPENED_AT)

        when:
        settlement.rename("Mountains", RENAMED_AT)

        then:
        settlement.pendingEvents().last() == new SettlementRenamed("Mountains", RENAMED_AT)
        settlement.version() == 2
    }

    def "renaming to the current name records nothing"() {
        given:
        def settlement = recreated("Holiday", "Mountains")

        when:
        settlement.rename("Mountains", RENAMED_AT)

        then:
        settlement.pendingEvents().empty
        settlement.version() == 2
    }

    def "a recreated Settlement has its history committed"() {
        when:
        def settlement = recreated("Holiday", "Mountains")

        then:
        settlement.pendingEvents().empty
        settlement.version() == 2
        settlement.committedVersion() == 2
    }

    def "a Settlement is opened with its original name and currency only"() {
        given:
        def settlement = recreated("Holiday", "Mountains")

        expect:
        settlement.isOpenedWith(name, currency) == openedWith

        where:
        name        | currency || openedWith
        "Holiday"   | EUR      || true
        "Mountains" | EUR      || false
        "Holiday"   | USD      || false
    }

    private static Settlement recreated(String openingName, String currentName) {
        Settlement.recreate(ID, [
            new SettlementOpened(openingName, EUR, OPENED_AT),
            new SettlementRenamed(currentName, RENAMED_AT)])
    }
}
