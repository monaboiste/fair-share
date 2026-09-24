package com.github.monaboiste.fairshare.settlement.infrastructure

import com.github.monaboiste.fairshare.common.events.EventEnvelope
import com.github.monaboiste.fairshare.common.events.EventId
import com.github.monaboiste.fairshare.common.events.EventStore
import com.github.monaboiste.fairshare.common.events.InMemoryEventStore
import com.github.monaboiste.fairshare.common.events.VersionConflictException
import com.github.monaboiste.fairshare.settlement.domain.Settlement
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementRenamed
import java.time.Instant
import javax.money.Monetary
import spock.lang.Specification

class EventSourcedSettlementRepositorySpec extends Specification {
    private static final SettlementId ID = new SettlementId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private static final EventId FIRST_EVENT = new EventId(UUID.fromString("00000000-0000-0000-0000-00000000000a"))
    private static final EventId SECOND_EVENT = new EventId(UUID.fromString("00000000-0000-0000-0000-00000000000b"))
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z")
    private static final EUR = Monetary.getCurrency("EUR")

    EventStore<SettlementId, SettlementEvent> store = new InMemoryEventStore<>()
    List<EventEnvelope> committed = []

    def "saving appends pending events as the next envelopes of the stream"() {
        given:
        def repository = repository(FIRST_EVENT, SECOND_EVENT)
        def settlement = Settlement.open(ID, "Holiday", EUR, NOW)
        settlement.rename("Mountains", NOW)

        when:
        repository.save(settlement)

        then:
        store.load(ID) == [
            new EventEnvelope(FIRST_EVENT, ID, 1, new SettlementOpened("Holiday", EUR, NOW)),
            new EventEnvelope(SECOND_EVENT, ID, 2, new SettlementRenamed("Mountains", NOW))]
        committed == store.load(ID)
        settlement.pendingEvents().empty
        settlement.committedVersion() == 2
    }

    def "a saved Settlement is found with its history committed"() {
        given:
        def repository = repository(FIRST_EVENT, SECOND_EVENT)
        repository.save(Settlement.open(ID, "Holiday", EUR, NOW))

        when:
        def found = repository.findById(ID).orElseThrow()
        found.rename("Mountains", NOW)
        repository.save(found)

        then:
        store.load(ID)*.sequence() == [1L, 2L]
    }

    def "an unknown Settlement is not found"() {
        expect:
        repository().findById(ID).empty
    }

    def "saving without pending events appends nothing"() {
        given:
        def repository = repository(FIRST_EVENT)
        repository.save(Settlement.open(ID, "Holiday", EUR, NOW))
        def unchanged = repository.findById(ID).orElseThrow()

        when:
        repository.save(unchanged)

        then:
        store.load(ID).size() == 1
        committed.size() == 1
    }

    def "a stale Settlement is rejected and keeps its pending events"() {
        given:
        def repository = new EventSourcedSettlementRepository(store, EventId::random, { committed.addAll(it) })
        repository.save(Settlement.open(ID, "Holiday", EUR, NOW))
        def stale = repository.findById(ID).orElseThrow()
        def winner = repository.findById(ID).orElseThrow()
        winner.rename("Winner", NOW)
        repository.save(winner)
        stale.rename("Loser", NOW)

        when:
        repository.save(stale)

        then:
        thrown(VersionConflictException)
        stale.pendingEvents() == [new SettlementRenamed("Loser", NOW)]
        store.load(ID)*.payload()*.name() == ["Holiday", "Winner"]
    }

    def "a publication failure after commit reports the committed version"() {
        given:
        def repository = new EventSourcedSettlementRepository(store, { FIRST_EVENT },
            { throw new IllegalStateException("projection failed") })
        def settlement = Settlement.open(ID, "Holiday", EUR, NOW)

        when:
        repository.save(settlement)

        then:
        PostCommitProjectionException failure = thrown()
        failure.committedVersion() == 1
        failure.cause.message == "projection failed"
        settlement.pendingEvents().empty
        store.load(ID).size() == 1
    }

    private EventSourcedSettlementRepository repository(EventId... ids) {
        def remaining = ids.toList()
        new EventSourcedSettlementRepository(store, { remaining.removeFirst() }, { committed.addAll(it) })
    }
}
