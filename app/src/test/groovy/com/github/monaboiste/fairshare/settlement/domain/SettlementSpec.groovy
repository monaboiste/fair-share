package com.github.monaboiste.fairshare.settlement.domain

import com.github.monaboiste.fairshare.common.events.EventId
import com.github.monaboiste.fairshare.common.events.inmemory.InMemoryEventStore
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementRenamed
import com.github.monaboiste.fairshare.settlement.infrastructure.EventSourcedSettlementRepository
import java.time.Instant
import javax.money.Monetary
import spock.lang.Specification

class SettlementSpec extends Specification {
    private static final SettlementId ID = new SettlementId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z")
    private static final EUR = Monetary.getCurrency("EUR")
    private static final USD = Monetary.getCurrency("USD")

    def "opening and renaming register only changed facts"() {
        given:
        def settlement = Settlement.open(ID, new SettlementName("Holiday"), EUR, NOW)

        when:
        settlement.rename(new SettlementName("Mountains"), NOW)
        settlement.rename(new SettlementName("Mountains"), NOW)

        then:
        settlement.pendingEvents() == [new SettlementOpened("Holiday", "EUR", NOW),
            new SettlementRenamed("Mountains", NOW)]
        settlement.version() == 2
        settlement.committedVersion() == 0
    }

    def "opening retry accepts only the original name and immutable currency after renaming"() {
        given:
        def settlement = Settlement.open(ID, new SettlementName("Holiday"), EUR, NOW)
        settlement.rename(new SettlementName("Mountains"), NOW)

        when:
        def retry = settlement.acceptOpeningRetry(new SettlementName(name), currency)

        then:
        retry.success() == accepted
        retry.success() ? retry.getSuccess().is(settlement) : retry.getFailure() == new IdentifierConflict(ID)

        where:
        name        | currency || accepted
        "Holiday"   | EUR      || true
        "Mountains" | EUR      || false
        "Holiday"   | USD      || false
    }

    def "repository recreates Settlement with committed history"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, EventId::random)
        def settlement = Settlement.open(ID, new SettlementName("Holiday"), EUR, NOW)
        settlement.rename(new SettlementName("Mountains"), NOW)
        repository.save(settlement)

        when:
        def replayed = repository.findById(ID).orElseThrow()

        then:
        replayed.pendingEvents().empty
        replayed.version() == 2
        replayed.committedVersion() == 2
        replayed.acceptOpeningRetry(new SettlementName("Holiday"), EUR).success()

        when:
        replayed.rename(new SettlementName("Mountains"), NOW)

        then:
        replayed.pendingEvents().empty
    }

    def "a non-opening first event and a second opening are rejected"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, EventId::random)
        store.append(ID, 0, [new com.github.monaboiste.fairshare.common.events.NewEvent<SettlementEvent>(
            EventId.random(), new SettlementRenamed("Wrong", NOW))])

        when:
        repository.findById(ID)

        then:
        thrown(IllegalStateException)

        when:
        def otherId = new SettlementId(UUID.randomUUID())
        store.append(otherId, 0, [new com.github.monaboiste.fairshare.common.events.NewEvent<SettlementEvent>(
            EventId.random(), new SettlementOpened("Holiday", "EUR", NOW)),
            new com.github.monaboiste.fairshare.common.events.NewEvent<SettlementEvent>(
                EventId.random(), new SettlementOpened("Again", "USD", NOW))])
        repository.findById(otherId)

        then:
        thrown(IllegalStateException)
    }
}
