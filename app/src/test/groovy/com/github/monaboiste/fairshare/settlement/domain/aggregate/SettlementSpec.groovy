package com.github.monaboiste.fairshare.settlement.domain.aggregate

import com.github.monaboiste.fairshare.common.events.EventId
import com.github.monaboiste.fairshare.common.events.NewEvent
import com.github.monaboiste.fairshare.common.events.inmemory.InMemoryEventStore
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.SettlementName
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementRenamed
import com.github.monaboiste.fairshare.settlement.infrastructure.EventSourcedSettlementRepository
import java.time.Instant
import javax.money.Monetary
import spock.lang.Specification
import spock.lang.Unroll

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
        settlement.pendingEvents() == [new SettlementOpened("Holiday", EUR, NOW),
            new SettlementRenamed("Mountains", NOW)]
        settlement.version() == 2
        settlement.committedVersion() == 0
    }

    @Unroll
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

        when:
        replayed.rename(new SettlementName("Mountains"), NOW)

        then:
        replayed.pendingEvents().empty
    }

    def "a non-opening first event is rejected on replay"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, EventId::random)
        store.append(ID, 0, [new NewEvent<SettlementEvent>(EventId.random(), new SettlementRenamed("Wrong", NOW))])

        when:
        repository.findById(ID)

        then:
        thrown(IllegalStateException)
    }

    def "a second opening event is rejected on replay"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, EventId::random)
        store.append(ID, 0, [new NewEvent<SettlementEvent>(EventId.random(),
            new SettlementOpened("Holiday", EUR, NOW)),
            new NewEvent<SettlementEvent>(EventId.random(), new SettlementOpened("Again", USD, NOW))])

        when:
        repository.findById(ID)

        then:
        thrown(IllegalStateException)
    }

    def "historical names are replayed without applying current input validation"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, EventId::random)
        store.append(ID, 0, [new NewEvent<SettlementEvent>(EventId.random(), new SettlementOpened("", EUR, NOW))])

        when:
        def settlement = repository.findById(ID).orElseThrow()

        then:
        settlement.version() == 1

        when:
        settlement.rename(new SettlementName("Current"), NOW)
        def committed = repository.save(settlement)

        then:
        committed.version() == 2
        repository.findById(ID).orElseThrow().pendingEvents().empty
    }
}
