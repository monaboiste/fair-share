package com.github.monaboiste.fairshare.settlement

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
        SettlementCommands commands = new SettlementCommands(new InMemoryEventStore(),
                Clock.fixed(NOW, ZoneOffset.UTC), () -> EVENT_ID)
        def currency = Monetary.getCurrency("EUR")

        when:
        def opened = commands.open(ID, "  Holiday  ", currency)

        then:
        opened.success()
        opened.getSuccess().version() == 1
        opened.getSuccess().events().size() == 1
        with(opened.getSuccess().events().first()) {
            eventId() == EVENT_ID
            settlementId() == ID
            sequence() == 1
            occurredAt() == NOW
            type() == "SettlementOpened"
            schemaVersion() == 1
            payload() == new SettlementOpened("  Holiday  ", currency)
        }
        commands.view(ID) == new SettlementView(ID, "  Holiday  ", currency, 1)
        commands.history(ID) == opened.getSuccess().events()
    }

    def "renaming changes only the display name and reopening returns the original success"() {
        given:
        def currency = Monetary.getCurrency("USD")
        SettlementCommands commands = new SettlementCommands(new InMemoryEventStore(),
                Clock.fixed(NOW, ZoneOffset.UTC), () -> EVENT_ID)
        def opened = commands.open(ID, "Original", currency).getSuccess()

        when:
        def renamed = commands.rename(ID, "  Current  ")
        def retried = commands.open(ID, "Original", currency)

        then:
        renamed.version() == 2
        renamed.events()*.sequence() == [2L]
        renamed.events().first().payload() == new SettlementRenamed("  Current  ")
        commands.view(ID) == new SettlementView(ID, "  Current  ", currency, 2)
        commands.history(ID) == opened.events() + renamed.events()
        retried.success()
        retried.getSuccess() == opened
        commands.history(ID).size() == 2
    }

    def "conflicting identifier use is a typed failure even after rename"() {
        given:
        def currency = Monetary.getCurrency("EUR")
        SettlementCommands commands = new SettlementCommands(new InMemoryEventStore(),
                Clock.fixed(NOW, ZoneOffset.UTC), () -> EVENT_ID)
        commands.open(ID, "Original", currency)
        commands.rename(ID, "Current")

        when:
        def differentName = commands.open(ID, "Current", currency)
        def differentCurrency = commands.open(ID, "Original", Monetary.getCurrency("USD"))

        then:
        differentName.failure()
        differentName.getFailure() == new IdentifierConflict(ID)
        differentCurrency.failure()
        differentCurrency.getFailure() == new IdentifierConflict(ID)
        commands.history(ID).size() == 2
    }

    def "renaming to the current name does not add an event"() {
        given:
        SettlementCommands commands = new SettlementCommands(new InMemoryEventStore(),
                Clock.fixed(NOW, ZoneOffset.UTC), () -> EVENT_ID)
        commands.open(ID, "Name", Monetary.getCurrency("EUR"))

        when:
        def unchanged = commands.rename(ID, "Name")

        then:
        unchanged == new AppendResult([], 1)
        commands.history(ID).size() == 1
    }

    def "blank names are rejected without changing the stream"() {
        given:
        SettlementCommands commands = new SettlementCommands(new InMemoryEventStore(),
                Clock.fixed(NOW, ZoneOffset.UTC), () -> EVENT_ID)
        commands.open(ID, "Name", Monetary.getCurrency("EUR"))

        when:
        commands.rename(ID, "  \t")

        then:
        thrown(IllegalArgumentException)
        commands.history(ID).size() == 1
    }

    def "replaying a Settlement with a new command entry point preserves its view and history"() {
        given:
        EventStore store = new InMemoryEventStore()
        SettlementCommands writer = new SettlementCommands(store, Clock.fixed(NOW, ZoneOffset.UTC), () -> EVENT_ID)
        writer.open(ID, "First", Monetary.getCurrency("EUR"))
        writer.rename(ID, "Second")

        when:
        SettlementCommands reader = new SettlementCommands(store, Clock.systemUTC(), UUID::randomUUID)
        SettlementView view = reader.view(ID)
        List<EventEnvelope> history = reader.history(ID)

        then:
        view == new SettlementView(ID, "Second", Monetary.getCurrency("EUR"), 2)
        history*.payload() == [
            new SettlementOpened("First", Monetary.getCurrency("EUR")), new SettlementRenamed("Second")]
    }

    def "invalid opening names are rejected before creating a stream"() {
        given:
        SettlementCommands commands = new SettlementCommands(new InMemoryEventStore(),
                Clock.fixed(NOW, ZoneOffset.UTC), () -> EVENT_ID)

        when:
        commands.open(ID, " \t", Monetary.getCurrency("EUR"))

        then:
        thrown(IllegalArgumentException)

        when:
        commands.history(ID)

        then:
        thrown(MissingStreamException)
    }

    def "store faults are propagated as technical exceptions"() {
        given:
        def fault = new IllegalStateException("store unavailable")
        EventStore store = new EventStore() {
            List<EventEnvelope> load(SettlementId id) { throw fault }
            AppendResult append(SettlementId id, long version, List<EventEnvelope> events) { throw fault }
        }
        SettlementCommands commands = new SettlementCommands(store, Clock.fixed(NOW, ZoneOffset.UTC), () -> EVENT_ID)

        when:
        commands.open(ID, "First", Monetary.getCurrency("EUR"))

        then:
        IllegalStateException error = thrown()
        error.is(fault)
    }

    def "missing Settlement streams fail as technical exceptions"() {
        given:
        SettlementCommands commands = new SettlementCommands(new InMemoryEventStore(),
                Clock.fixed(NOW, ZoneOffset.UTC), () -> EVENT_ID)

        when:
        commands.view(ID)

        then:
        thrown(MissingStreamException)
    }
}
