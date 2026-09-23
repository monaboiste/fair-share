package com.github.monaboiste.fairshare.settlement

import com.github.monaboiste.fairshare.common.events.EventEnvelope
import com.github.monaboiste.fairshare.common.events.InMemoryEventStore
import com.github.monaboiste.fairshare.common.events.MissingStreamException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import javax.money.Monetary
import spock.lang.Specification

class SettlementSpec extends Specification {
    private static final SettlementId ID = new SettlementId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z")

    def "opening preserves the Settlement name currency and stream metadata"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def app = new SettlementApplication(store, Clock.fixed(NOW, ZoneOffset.UTC), () -> EVENT_ID)
        def currency = Monetary.getCurrency("EUR")

        when:
        def result = app.commands().dispatch(new OpenSettlement(ID, "  Holiday  ", currency))

        then:
        result.getSuccess() == ID
        app.queries().handle(new GetSettlement(ID)) == new SettlementView(ID, "  Holiday  ", currency, 1)
        app.queries().handle(new GetSettlementHistory(ID)) == [
            new EventEnvelope(EVENT_ID, ID, 1, NOW, new SettlementOpened("  Holiday  ", currency))]
    }

    def "renaming a missing Settlement fails without creating a stream"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def app = new SettlementApplication(store, Clock.fixed(NOW, ZoneOffset.UTC), () -> EVENT_ID)

        when:
        app.commands().dispatch(new RenameSettlement(ID, "Other"))

        then:
        thrown(MissingStreamException)
        store.streams().isEmpty()
    }

    def "missing Settlements and blank names fail without creating a stream"() {
        given:
        def app = new SettlementApplication(new InMemoryEventStore<>(), Clock.fixed(NOW, ZoneOffset.UTC), () -> EVENT_ID)

        when:
        new OpenSettlement(ID, " \t", Monetary.getCurrency("EUR"))

        then:
        thrown(IllegalArgumentException)

        when:
        app.queries().handle(new GetSettlement(ID))

        then:
        thrown(MissingStreamException)
    }
}
