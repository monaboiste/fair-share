package com.github.monaboiste.fairshare.common.events

import java.time.Instant
import spock.lang.Specification

class EventStoreSpec extends Specification {

    private static final String STREAM = "holiday"
    private static final Instant TIME = Instant.parse("2026-01-01T00:00:00Z")

    def "appending multiple events persists their order and metadata atomically"() {
        given:
        EventStore<String, NamedEvent> store = new InMemoryEventStore<>()
        def opened = envelope(1, STREAM, new NamedEvent("first"))
        def renamed = envelope(2, STREAM, new NamedEvent("second"))

        when:
        def result = store.append(STREAM, 0, [opened, renamed])

        then:
        result == new AppendResult([opened, renamed], 2)
        store.load(STREAM) == [opened, renamed]
        store.load(STREAM)*.type() == ["NamedEvent", "NamedEvent"]
        store.load(STREAM)*.schemaVersion() == [1, 1]

        when:
        store.load(STREAM).clear()

        then:
        thrown(UnsupportedOperationException)
        store.load(STREAM) == [opened, renamed]
    }

    def "stream enumeration provides an immutable snapshot for rebuilding projections"() {
        given:
        EventStore<String, NamedEvent> store = new InMemoryEventStore<>()
        def opened = envelope(1, STREAM, new NamedEvent("first"))
        store.append(STREAM, 0, [opened])

        when:
        def snapshot = store.streams()
        store.append(STREAM, 1, [envelope(2, STREAM, new NamedEvent("second"))])

        then:
        snapshot == [(STREAM): [opened]]
        store.streams().get(STREAM).size() == 2

        when:
        snapshot.clear()

        then:
        thrown(UnsupportedOperationException)
    }

    def "an invalid batch writes no events"() {
        given:
        EventStore<String, NamedEvent> store = new InMemoryEventStore<>()
        def opened = envelope(1, STREAM, new NamedEvent("first"))

        when:
        store.append(STREAM, 0, [opened, envelope(3, STREAM, new NamedEvent("wrong sequence"))])

        then:
        thrown(IllegalArgumentException)

        when:
        store.load(STREAM)

        then:
        thrown(MissingStreamException)
    }

    def "an invalid batch does not partially append to an existing stream"() {
        given:
        EventStore<String, NamedEvent> store = new InMemoryEventStore<>()
        def opened = envelope(1, STREAM, new NamedEvent("first"))
        store.append(STREAM, 0, [opened])

        when:
        store.append(STREAM, 1, [envelope(2, STREAM, new NamedEvent("second")),
            envelope(4, STREAM, new NamedEvent("wrong sequence"))])

        then:
        thrown(IllegalArgumentException)
        store.load(STREAM) == [opened]
    }

    def "an incorrect stream identifier writes no events"() {
        given:
        EventStore<String, NamedEvent> store = new InMemoryEventStore<>()

        when:
        store.append(STREAM, 0, [envelope(1, "different", new NamedEvent("first"))])

        then:
        thrown(IllegalArgumentException)

        when:
        store.load(STREAM)

        then:
        thrown(MissingStreamException)
    }

    def "expected version conflicts do not change the stream"() {
        given:
        EventStore<String, NamedEvent> store = new InMemoryEventStore<>()
        def opened = envelope(1, STREAM, new NamedEvent("first"))
        store.append(STREAM, 0, [opened])

        when:
        store.append(STREAM, 0, [envelope(1, STREAM, new NamedEvent("stale"))])

        then:
        thrown(VersionConflictException)
        store.load(STREAM) == [opened]
    }

    private static EventEnvelope<String, NamedEvent> envelope(long sequence, String stream, NamedEvent payload) {
        new EventEnvelope(UUID.fromString("00000000-0000-0000-0000-00000000000${sequence}"),
                stream, sequence, TIME, payload)
    }

    private static class NamedEvent implements Event {
        final String name

        NamedEvent(String name) { this.name = name }
        String type() { "NamedEvent" }
        int schemaVersion() { 1 }
    }
}
