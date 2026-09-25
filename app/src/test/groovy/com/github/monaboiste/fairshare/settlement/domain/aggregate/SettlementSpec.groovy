package com.github.monaboiste.fairshare.settlement.domain.aggregate

import com.github.monaboiste.fairshare.common.events.EventId
import com.github.monaboiste.fairshare.common.events.PendingEvent
import com.github.monaboiste.fairshare.common.events.inmemory.InMemoryEventStore
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId
import com.github.monaboiste.fairshare.settlement.domain.ParticipantIdentifierConflict
import com.github.monaboiste.fairshare.settlement.domain.ParticipantName
import com.github.monaboiste.fairshare.settlement.domain.ParticipantNotFound
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.SettlementName
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantAdded
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRemoved
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRenamed
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
    private static final ParticipantId PARTICIPANT = new ParticipantId(UUID.fromString("00000000-0000-0000-0000-000000000011"))
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

    def "replay retains original add data after rename and removal"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, CLOCK)
        def settlement = Settlement.open(new SettlementName("Holiday"), EUR, CLOCK)
        settlement.addParticipant(PARTICIPANT, new ParticipantName("Alex"))
        settlement.renameParticipant(PARTICIPANT, new ParticipantName("Ada"))
        settlement.removeParticipant(PARTICIPANT)
        repository.save(settlement)

        when:
        def replayed = repository.findById(settlement.id()).orElseThrow()
        def retry = replayed.addParticipant(PARTICIPANT, new ParticipantName("Alex"))
        def conflict = replayed.addParticipant(PARTICIPANT, new ParticipantName("Ada"))

        then:
        retry.success()
        conflict.getFailure() == new ParticipantIdentifierConflict(settlement.id(), PARTICIPANT)
        replayed.renameParticipant(PARTICIPANT, new ParticipantName("Again")).getFailure() ==
            new ParticipantNotFound(settlement.id(), PARTICIPANT)
        replayed.pendingEvents().empty
        replayed.version() == 4
    }

    def "historical Participant names replay without current input validation"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, CLOCK)
        store.append(ID, 0, [pending(OPENED_ID, new SettlementOpened("Holiday", EUR)),
            pending(RENAMED_ID, new ParticipantAdded(PARTICIPANT, ""))])

        when:
        def replayed = repository.findById(ID).orElseThrow()

        then:
        replayed.version() == 2
        replayed.renameParticipant(PARTICIPANT, new ParticipantName("Ada")).success()
    }

    def "invalid Participant event sequences are rejected on replay"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, CLOCK)
        store.append(ID, 0, [pending(OPENED_ID, new SettlementOpened("Holiday", EUR)), pending(RENAMED_ID, invalid)])

        when:
        repository.findById(ID)

        then:
        thrown(IllegalStateException)

        where:
        invalid << [new ParticipantRenamed(PARTICIPANT, "Ada"), new ParticipantRemoved(PARTICIPANT)]
    }

    def "a removed Participant cannot be changed by a later event"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, CLOCK)
        store.append(ID, 0, [pending(OPENED_ID, new SettlementOpened("Holiday", EUR)),
            pending(EventId.random(), new ParticipantAdded(PARTICIPANT, "Alex")),
            pending(EventId.random(), new ParticipantRemoved(PARTICIPANT)),
            pending(RENAMED_ID, laterEvent)])

        when:
        repository.findById(ID)

        then:
        thrown(IllegalStateException)

        where:
        laterEvent << [new ParticipantRenamed(PARTICIPANT, "Ada"), new ParticipantRemoved(PARTICIPANT)]
    }

    private static PendingEvent<SettlementEvent> pending(EventId id, SettlementEvent payload) {
        new PendingEvent<SettlementEvent>(id, payload, NOW)
    }
}
