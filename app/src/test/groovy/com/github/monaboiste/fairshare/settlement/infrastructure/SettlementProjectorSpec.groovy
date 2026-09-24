package com.github.monaboiste.fairshare.settlement.infrastructure

import com.github.monaboiste.fairshare.common.events.EventEnvelope
import com.github.monaboiste.fairshare.common.events.EventId
import com.github.monaboiste.fairshare.common.events.InMemoryEventStore
import com.github.monaboiste.fairshare.settlement.application.query.SettlementView
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementRenamed
import java.time.Instant
import javax.money.Monetary
import spock.lang.Specification

class SettlementProjectorSpec extends Specification {
    private static final SettlementId ID = new SettlementId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private static final SettlementId OTHER = new SettlementId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
    private static final SettlementId STALE = new SettlementId(UUID.fromString("00000000-0000-0000-0000-000000000003"))
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z")
    private static final EUR = Monetary.getCurrency("EUR")

    def projector = new SettlementProjector()

    def "projecting opening and renaming yields the current view"() {
        when:
        projector.accept([opened(ID, "Holiday"), renamed(ID, 2, "Mountains")])

        then:
        projector.findById(ID) == Optional.of(new SettlementView(ID, "Mountains", EUR, 2))
    }

    def "a duplicate delivery is ignored"() {
        given:
        projector.accept(opened(ID, "Holiday"))

        when:
        projector.accept(opened(ID, "Holiday"))

        then:
        projector.findById(ID).orElseThrow().version() == 1
    }

    def "a gap in the stream is rejected"() {
        given:
        projector.accept(opened(ID, "Holiday"))

        when:
        projector.accept(renamed(ID, 3, "Skipped"))

        then:
        thrown(IllegalStateException)
        projector.findById(ID).orElseThrow().name() == "Holiday"
    }

    def "an unprojected Settlement has no view"() {
        expect:
        projector.findById(ID).empty
    }

    def "rebuilding replaces every view with the committed streams"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        store.append(ID, 0, [opened(ID, "Before"), renamed(ID, 2, "After")])
        store.append(OTHER, 0, [opened(OTHER, "Other")])
        projector.accept(opened(STALE, "Stale"))

        when:
        projector.rebuild(store)

        then:
        projector.findById(ID) == Optional.of(new SettlementView(ID, "After", EUR, 2))
        projector.findById(OTHER) == Optional.of(new SettlementView(OTHER, "Other", EUR, 1))
        projector.findById(STALE).empty
    }

    private static EventEnvelope<SettlementId, SettlementEvent> opened(SettlementId id, String name) {
        new EventEnvelope(EventId.random(), id, 1, new SettlementOpened(name, EUR, NOW))
    }

    private static EventEnvelope<SettlementId, SettlementEvent> renamed(SettlementId id, long sequence, String name) {
        new EventEnvelope(EventId.random(), id, sequence, new SettlementRenamed(name, NOW))
    }
}
