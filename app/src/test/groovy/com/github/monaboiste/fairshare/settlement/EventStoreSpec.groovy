package com.github.monaboiste.fairshare.settlement

import java.time.Instant
import javax.money.Monetary
import spock.lang.Specification

class EventStoreSpec extends Specification {

    private static final SettlementId ID = new SettlementId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private static final Instant TIME = Instant.parse("2026-01-01T00:00:00Z")

    def "appending multiple events persists their order and original metadata atomically"() {
        given:
        EventStore store = new InMemoryEventStore()
        def opened = envelope(1, new SettlementOpened("First", Monetary.getCurrency("EUR")))
        def renamed = envelope(2, new SettlementRenamed("Second"))

        when:
        def result = store.append(ID, 0, [opened, renamed])

        then:
        result == new AppendResult([opened, renamed], 2)
        store.load(ID) == [opened, renamed]

        when:
        store.load(ID).clear()

        then:
        thrown(UnsupportedOperationException)
        store.load(ID) == [opened, renamed]
    }

    def "an invalid batch writes no events"() {
        given:
        EventStore store = new InMemoryEventStore()
        def opened = envelope(1, new SettlementOpened("First", Monetary.getCurrency("EUR")))

        when:
        store.append(ID, 0, [opened, envelope(3, new SettlementRenamed("Wrong sequence"))])

        then:
        thrown(IllegalArgumentException)

        when:
        store.load(ID)

        then:
        thrown(MissingStreamException)
    }

    def "expected version conflicts do not change the stream"() {
        given:
        EventStore store = new InMemoryEventStore()
        def opened = envelope(1, new SettlementOpened("First", Monetary.getCurrency("EUR")))
        store.append(ID, 0, [opened])

        when:
        store.append(ID, 0, [envelope(1, new SettlementRenamed("Stale"))])

        then:
        thrown(VersionConflictException)
        store.load(ID) == [opened]
    }

    private static EventEnvelope envelope(long sequence, SettlementEvent payload) {
        new EventEnvelope(UUID.fromString("00000000-0000-0000-0000-00000000000${sequence}"),
                ID, sequence, TIME, payload.class.simpleName, 1, payload)
    }
}
