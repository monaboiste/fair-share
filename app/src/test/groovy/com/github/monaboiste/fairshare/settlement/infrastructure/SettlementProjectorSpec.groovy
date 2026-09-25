package com.github.monaboiste.fairshare.settlement.infrastructure

import com.github.monaboiste.fairshare.common.events.EventEnvelope
import com.github.monaboiste.fairshare.common.events.EventId
import com.github.monaboiste.fairshare.common.events.PendingEvent
import com.github.monaboiste.fairshare.common.events.inmemory.InMemoryEventStore
import com.github.monaboiste.fairshare.settlement.application.query.SettlementView
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantAdded
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRemoved
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementRenamed
import java.time.Instant
import javax.money.Monetary
import spock.lang.Specification

class SettlementProjectorSpec extends Specification {
    private static final SettlementId ID = new SettlementId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private static final SettlementId OTHER = new SettlementId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
    private static final ParticipantId PARTICIPANT =
        new ParticipantId(UUID.fromString("00000000-0000-0000-0000-000000000010"))
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z")
    private static final EUR = Monetary.getCurrency("EUR")
    def projector = new SettlementProjector()

    def "duplicate deliveries are ignored and renames preserve the opening currency"() {
        when:
        projector.accept([opened(ID), opened(ID), renamed(ID, 2, "Mountains")])

        then:
        projector.findById(ID) == Optional.of(new SettlementView(ID, "Mountains", EUR, 2, []))
    }

    def "a batch containing a gap does not partially update any view"() {
        when:
        projector.accept([opened(ID), renamed(OTHER, 2, "Skipped")])

        then:
        thrown(IllegalStateException)
        projector.findById(ID).empty
    }

    def "batch stages touched streams while preserving untouched views"() {
        given:
        def untouched = new SettlementId(UUID.randomUUID())
        projector.accept([opened(untouched), opened(ID)])

        when:
        projector.accept([renamed(ID, 2, "Mountains"), opened(OTHER), renamed(OTHER, 2, "Forest")])

        then:
        projector.findById(untouched) == Optional.of(new SettlementView(untouched, "Holiday", EUR, 1, []))
        projector.findById(ID) == Optional.of(new SettlementView(ID, "Mountains", EUR, 2, []))
        projector.findById(OTHER) == Optional.of(new SettlementView(OTHER, "Forest", EUR, 2, []))
    }

    def "rebuild replaces existing views with globally ordered history"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        store.append(ID, 0, [pending(new SettlementOpened("Holiday", EUR))])
        store.append(OTHER, 0, [pending(new SettlementOpened("Other", EUR))])
        store.append(ID, 1, [pending(new SettlementRenamed("Mountains"))])
        def stale = new SettlementId(UUID.randomUUID())
        projector.accept([opened(stale)])

        when:
        projector.rebuild(store)

        then:
        projector.findById(ID) == Optional.of(new SettlementView(ID, "Mountains", EUR, 2, []))
        projector.findById(OTHER) == Optional.of(new SettlementView(OTHER, "Other", EUR, 1, []))
        projector.findById(stale).empty
    }

    def "a removed Participant cannot be re-added by a live delivery"() {
        given:
        projector.accept([
            opened(ID),
            event(ID, 2, new ParticipantAdded(PARTICIPANT, "Alex")),
            event(ID, 3, new ParticipantRemoved(PARTICIPANT))
        ])

        when:
        projector.accept([event(ID, 4, new ParticipantAdded(PARTICIPANT, "Alex"))])

        then:
        thrown(IllegalStateException)
        projector.findById(ID) == Optional.of(new SettlementView(ID, "Holiday", EUR, 3, []))
    }

    def "a removed Participant cannot be re-added during rebuild"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        store.append(ID, 0, [
            pending(new SettlementOpened("Holiday", EUR)),
            pending(new ParticipantAdded(PARTICIPANT, "Alex")),
            pending(new ParticipantRemoved(PARTICIPANT)),
            pending(new ParticipantAdded(PARTICIPANT, "Alex"))
        ])

        when:
        projector.rebuild(store)

        then:
        thrown(IllegalStateException)
    }

    private static PendingEvent<SettlementEvent> pending(SettlementEvent payload) {
        new PendingEvent<SettlementEvent>(EventId.random(), payload, NOW)
    }

    private static EventEnvelope<SettlementId, SettlementEvent> opened(SettlementId id) {
        new EventEnvelope<SettlementId, SettlementEvent>(id, 1, 1, EventId.random(), NOW,
            new SettlementOpened("Holiday", EUR))
    }

    private static EventEnvelope<SettlementId, SettlementEvent> event(
        SettlementId id, long sequence, SettlementEvent payload) {
        new EventEnvelope<SettlementId, SettlementEvent>(id, sequence, sequence, EventId.random(), NOW, payload)
    }

    private static EventEnvelope<SettlementId, SettlementEvent> renamed(SettlementId id, long sequence, String name) {
        new EventEnvelope<SettlementId, SettlementEvent>(id, sequence, sequence, EventId.random(), NOW,
            new SettlementRenamed(name))
    }
}
