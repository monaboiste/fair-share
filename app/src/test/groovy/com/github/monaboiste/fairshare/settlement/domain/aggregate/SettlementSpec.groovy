package com.github.monaboiste.fairshare.settlement.domain.aggregate

import com.github.monaboiste.fairshare.common.events.EventId
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
    private static final EventId OPENED_ID = new EventId(UUID.fromString("00000000-0000-0000-0000-00000000000a"))
    private static final EventId RENAMED_ID = new EventId(UUID.fromString("00000000-0000-0000-0000-00000000000b"))
    private static final EUR = Monetary.getCurrency("EUR")
    private static final USD = Monetary.getCurrency("USD")

    def "opening and renaming register only changed facts"() {
        given:
        def settlement = Settlement.open(ID, new SettlementName("Holiday"), EUR, NOW)

        when:
        settlement.rename(new SettlementName("Mountains"), NOW)
        settlement.rename(new SettlementName("Mountains"), NOW)

        then:
        settlement.pendingEvents()*.name() == ["Holiday", "Mountains"]
        settlement.pendingEvents().first().currency() == EUR
        settlement.pendingEvents()*.occurredAt() == [NOW, NOW]
        settlement.pendingEvents().every { it.eventId() != null }
        settlement.pendingEvents().first().eventId() != settlement.pendingEvents().last().eventId()
        settlement.version() == 2
        settlement.committedVersion() == 0
    }

    @Unroll
    def "repository recreates Settlement with committed history"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store)
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
        def repository = new EventSourcedSettlementRepository(store)
        store.append(ID, 0, [new SettlementRenamed(RENAMED_ID, "Wrong", NOW)])

        when:
        repository.findById(ID)

        then:
        thrown(IllegalStateException)
    }

    def "a second opening event is rejected on replay"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store)
        store.append(ID, 0, [new SettlementOpened(OPENED_ID, "Holiday", EUR, NOW),
            new SettlementOpened(RENAMED_ID, "Again", USD, NOW)])

        when:
        repository.findById(ID)

        then:
        thrown(IllegalStateException)
    }

    def "historical names are replayed without applying current input validation"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store)
        store.append(ID, 0, [new SettlementOpened(OPENED_ID, "", EUR, NOW)])

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
