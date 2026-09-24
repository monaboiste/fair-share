package com.github.monaboiste.fairshare.settlement.domain.aggregate

import com.github.monaboiste.fairshare.common.events.EventId
import com.github.monaboiste.fairshare.common.events.PendingEvent
import com.github.monaboiste.fairshare.common.events.inmemory.InMemoryEventStore
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.SettlementName
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementRenamed
import com.github.monaboiste.fairshare.settlement.infrastructure.EventSourcedSettlementRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import javax.money.Monetary
import spock.lang.Specification

class SettlementSpec extends Specification {
    private static final SettlementId ID = new SettlementId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z")
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC)
    private static final EventId OPENED_ID = new EventId(UUID.fromString("00000000-0000-0000-0000-00000000000a"))
    private static final EventId RENAMED_ID = new EventId(UUID.fromString("00000000-0000-0000-0000-00000000000b"))
    private static final EUR = Monetary.getCurrency("EUR")
    private static final USD = Monetary.getCurrency("USD")

    def "opening and renaming register only changed facts"() {
        given:
        def settlement = Settlement.open(new SettlementName("Holiday"), EUR, CLOCK)

        when:
        settlement.rename(new SettlementName("Mountains"))
        settlement.rename(new SettlementName("Mountains"))

        then:
        settlement.pendingEvents()*.payload() == [new SettlementOpened("Holiday", EUR),
            new SettlementRenamed("Mountains")]
        settlement.pendingEvents().first().payload().currency() == EUR
        settlement.pendingEvents()*.occurredAt() == [NOW, NOW]
        settlement.pendingEvents().every { it.eventId() != null }
        settlement.pendingEvents().first().eventId() != settlement.pendingEvents().last().eventId()
        settlement.version() == 2
        settlement.committedVersion() == 0
    }

    def "opening Settlements generates distinct identifiers"() {
        when:
        def first = Settlement.open(new SettlementName("Holiday"), EUR, CLOCK)
        def second = Settlement.open(new SettlementName("Holiday"), EUR, CLOCK)

        then:
        first.id() != null
        second.id() != null
        first.id() != second.id()
        first.id().value() != null
    }

    def "repository recreates Settlement with committed history"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, CLOCK)
        def settlement = Settlement.open(new SettlementName("Holiday"), EUR, CLOCK)
        settlement.rename(new SettlementName("Mountains"))
        repository.save(settlement)

        when:
        def replayed = repository.findById(settlement.id()).orElseThrow()

        then:
        replayed.pendingEvents().empty
        replayed.version() == 2
        replayed.committedVersion() == 2

        when:
        replayed.rename(new SettlementName("Mountains"))

        then:
        replayed.pendingEvents().empty
    }

    def "a non-opening first event is rejected on replay"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, CLOCK)
        store.append(ID, 0, [pending(RENAMED_ID, new SettlementRenamed("Wrong"))])

        when:
        repository.findById(ID)

        then:
        thrown(IllegalStateException)
    }

    def "a second opening event is rejected on replay"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, CLOCK)
        store.append(ID, 0, [pending(OPENED_ID, new SettlementOpened("Holiday", EUR)),
            pending(RENAMED_ID, new SettlementOpened("Again", USD))])

        when:
        repository.findById(ID)

        then:
        thrown(IllegalStateException)
    }

    def "historical names are replayed without applying current input validation"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, CLOCK)
        store.append(ID, 0, [pending(OPENED_ID, new SettlementOpened("", EUR))])

        when:
        def settlement = repository.findById(ID).orElseThrow()

        then:
        settlement.version() == 1

        when:
        settlement.rename(new SettlementName("Current"))
        def committed = repository.save(settlement)

        then:
        committed.version() == 2
        repository.findById(ID).orElseThrow().pendingEvents().empty
    }

    private static PendingEvent<SettlementEvent> pending(EventId id, SettlementEvent payload) {
        new PendingEvent<SettlementEvent>(id, payload, NOW)
    }
}
