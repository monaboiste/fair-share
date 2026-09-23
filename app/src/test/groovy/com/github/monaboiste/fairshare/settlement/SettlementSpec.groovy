package com.github.monaboiste.fairshare.settlement

import com.github.monaboiste.fairshare.common.events.AppendResult
import com.github.monaboiste.fairshare.common.events.EventEnvelope
import com.github.monaboiste.fairshare.common.events.EventStore
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

    def "opening a Settlement persists its currency and metadata"() {
        given:
        EventStore<SettlementId, SettlementEvent> store = new InMemoryEventStore<>()
        SettlementCommandHandler commands = commands(store)
        SettlementQueryHandler queries = new SettlementQueryHandler(store)
        def currency = Monetary.getCurrency("EUR")

        when:
        def opened = commands.handle(new OpenSettlement(ID, "  Holiday  ", currency))

        then:
        opened.success()
        opened.getSuccess().version() == 1
        opened.getSuccess().events().size() == 1
        with(opened.getSuccess().events().first()) {
            eventId() == EVENT_ID
            streamId() == ID
            sequence() == 1
            occurredAt() == NOW
            type() == "SettlementOpened"
            schemaVersion() == 1
            payload() == new SettlementOpened("  Holiday  ", currency)
        }
        queries.handle(new GetSettlement(ID)) == new SettlementView(ID, "  Holiday  ", currency, 1)
        queries.handle(new GetSettlementHistory(ID)) == opened.getSuccess().events()
    }

    def "renaming changes only the display name and reopening returns the original success"() {
        given:
        def currency = Monetary.getCurrency("USD")
        EventStore<SettlementId, SettlementEvent> store = new InMemoryEventStore<>()
        SettlementCommandHandler commands = commands(store)
        SettlementQueryHandler queries = new SettlementQueryHandler(store)
        def opened = commands.handle(new OpenSettlement(ID, "Original", currency)).getSuccess()

        when:
        def renamed = commands.handle(new RenameSettlement(ID, "  Current  "))
        def retried = commands.handle(new OpenSettlement(ID, "Original", currency))

        then:
        renamed.success()
        renamed.getSuccess().version() == 2
        renamed.getSuccess().events()*.sequence() == [2L]
        renamed.getSuccess().events().first().type() == "SettlementRenamed"
        renamed.getSuccess().events().first().payload() == new SettlementRenamed("  Current  ")
        queries.handle(new GetSettlement(ID)) == new SettlementView(ID, "  Current  ", currency, 2)
        queries.handle(new GetSettlementHistory(ID)) == opened.events() + renamed.getSuccess().events()
        retried.success()
        retried.getSuccess() == opened
        queries.handle(new GetSettlementHistory(ID)).size() == 2
    }

    def "conflicting identifier use is a typed failure even after rename"() {
        given:
        def currency = Monetary.getCurrency("EUR")
        EventStore<SettlementId, SettlementEvent> store = new InMemoryEventStore<>()
        SettlementCommandHandler commands = commands(store)
        SettlementQueryHandler queries = new SettlementQueryHandler(store)
        commands.handle(new OpenSettlement(ID, "Original", currency))
        commands.handle(new RenameSettlement(ID, "Current"))

        when:
        def differentName = commands.handle(new OpenSettlement(ID, "Current", currency))
        def differentCurrency = commands.handle(new OpenSettlement(ID, "Original", Monetary.getCurrency("USD")))

        then:
        differentName.failure()
        differentName.getFailure() == new IdentifierConflict(ID)
        differentCurrency.failure()
        differentCurrency.getFailure() == new IdentifierConflict(ID)
        queries.handle(new GetSettlementHistory(ID)).size() == 2
    }

    def "renaming to the current name does not add an event"() {
        given:
        EventStore<SettlementId, SettlementEvent> store = new InMemoryEventStore<>()
        SettlementCommandHandler commands = commands(store)
        SettlementQueryHandler queries = new SettlementQueryHandler(store)
        commands.handle(new OpenSettlement(ID, "Name", Monetary.getCurrency("EUR")))

        when:
        def unchanged = commands.handle(new RenameSettlement(ID, "Name"))

        then:
        unchanged.success()
        unchanged.getSuccess() == new AppendResult([], 1)
        queries.handle(new GetSettlementHistory(ID)).size() == 1
    }

    def "blank names are rejected without changing the stream"() {
        given:
        EventStore<SettlementId, SettlementEvent> store = new InMemoryEventStore<>()
        SettlementCommandHandler commands = commands(store)
        SettlementQueryHandler queries = new SettlementQueryHandler(store)
        commands.handle(new OpenSettlement(ID, "Name", Monetary.getCurrency("EUR")))

        when:
        new RenameSettlement(ID, "  \t")

        then:
        thrown(IllegalArgumentException)
        queries.handle(new GetSettlementHistory(ID)).size() == 1
    }

    def "replaying a Settlement with a new query handler preserves its view and history"() {
        given:
        EventStore<SettlementId, SettlementEvent> store = new InMemoryEventStore<>()
        SettlementCommandHandler writer = commands(store)
        writer.handle(new OpenSettlement(ID, "First", Monetary.getCurrency("EUR")))
        writer.handle(new RenameSettlement(ID, "Second"))

        when:
        SettlementQueryHandler reader = new SettlementQueryHandler(store)
        SettlementView view = reader.handle(new GetSettlement(ID))
        List<EventEnvelope<SettlementId, SettlementEvent>> history = reader.handle(new GetSettlementHistory(ID))

        then:
        view == new SettlementView(ID, "Second", Monetary.getCurrency("EUR"), 2)
        history*.payload() == [
            new SettlementOpened("First", Monetary.getCurrency("EUR")), new SettlementRenamed("Second")]
    }

    def "invalid opening names are rejected before creating a stream"() {
        given:
        EventStore<SettlementId, SettlementEvent> store = new InMemoryEventStore<>()
        SettlementQueryHandler queries = new SettlementQueryHandler(store)

        when:
        new OpenSettlement(ID, " \t", Monetary.getCurrency("EUR"))

        then:
        thrown(IllegalArgumentException)

        when:
        queries.handle(new GetSettlementHistory(ID))

        then:
        thrown(MissingStreamException)
    }

    def "store faults are propagated as technical exceptions"() {
        given:
        def fault = new IllegalStateException("store unavailable")
        EventStore<SettlementId, SettlementEvent> store = new EventStore<SettlementId, SettlementEvent>() {
            List<EventEnvelope<SettlementId, SettlementEvent>> load(SettlementId id) { throw fault }
            AppendResult<SettlementId, SettlementEvent> append(SettlementId id, long version,
                    List<EventEnvelope<SettlementId, SettlementEvent>> events) { throw fault }
        }
        SettlementCommandHandler commands = commands(store)

        when:
        commands.handle(new OpenSettlement(ID, "First", Monetary.getCurrency("EUR")))

        then:
        IllegalStateException error = thrown()
        error.is(fault)
    }

    def "missing Settlement streams fail as technical exceptions"() {
        given:
        SettlementQueryHandler queries = new SettlementQueryHandler(new InMemoryEventStore<>())

        when:
        queries.handle(new GetSettlement(ID))

        then:
        thrown(MissingStreamException)
    }

    private static SettlementCommandHandler commands(EventStore<SettlementId, SettlementEvent> store) {
        new SettlementCommandHandler(store, Clock.fixed(NOW, ZoneOffset.UTC), () -> EVENT_ID)
    }
}
