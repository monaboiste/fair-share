package com.github.monaboiste.fairshare.common.events

import com.github.monaboiste.fairshare.common.events.inmemory.InMemoryEventStore
import java.time.Instant
import spock.lang.Specification

class EventStoreSpec extends Specification {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z")

    @EventType(name = "Changed", version = 2)
    static class Changed implements Event {
        Instant time
        Changed(Instant time) { this.time = time }
        Instant occurredAt() { time }
    }

    @EventType(name = " ", version = 0)
    static class InvalidType implements Event {
        Instant occurredAt() { NOW }
    }

    static class Unannotated implements Event {
        Instant time
        Unannotated(Instant time) { this.time = time }
        Instant occurredAt() { time }
    }

    def "append assigns ordered stream sequences and global positions without losing metadata"() {
        given:
        def store = new InMemoryEventStore<String, Event>()
        def first = new NewEvent<Event>(EventId.random(), new Changed(NOW))
        def second = new NewEvent<Event>(EventId.random(), new Changed(NOW))

        when:
        def committed = store.append("one", 0, [first, second])
        def other = store.append("two", 0, [new NewEvent<Event>(EventId.random(), new Changed(NOW))])

        then:
        committed.version() == 2
        committed.events()*.sequence() == [1L, 2L]
        committed.events()*.position() == [1L, 2L]
        committed.events()*.eventId() == [first.eventId(), second.eventId()]
        committed.events()*.type() == ["Changed", "Changed"]
        committed.events()*.schemaVersion() == [2, 2]
        committed.events()*.occurredAt() == [NOW, NOW]
        store.load("one") == committed.events()
        store.readAll(1)*.position() == [2L, 3L]
        other.events().first().position() == 3
        store.exists("one")
        !store.exists("missing")
    }

    def "unknown streams and invalid batches do not create streams"() {
        given:
        def store = new InMemoryEventStore<String, Event>()

        when:
        store.load("missing")

        then:
        def missing = thrown(StreamNotFoundException)
        missing.streamId() == "missing"

        when:
        store.append("missing", 0, [])

        then:
        thrown(IllegalArgumentException)
        !store.exists("missing")

        when:
        store.append("missing", 0, [new NewEvent<Event>(EventId.random(), new Unannotated(NOW))])

        then:
        thrown(IllegalArgumentException)
        !store.exists("missing")

        when:
        store.append("missing", 0, [new NewEvent<Event>(EventId.random(), new Changed(NOW)),
            new NewEvent<Event>(EventId.random(), new InvalidType())])

        then:
        thrown(IllegalArgumentException)
        !store.exists("missing")
        store.readAll(0).empty
    }

    def "invalid envelope metadata is rejected"() {
        when:
        new EventEnvelope<EventId, Event>(EventId.random(), EventId.random(), 0, 1, "Changed", 1, new Changed(NOW))

        then:
        thrown(IllegalArgumentException)

        when:
        new EventEnvelope<EventId, Event>(EventId.random(), EventId.random(), 1, 0, "Changed", 1, new Changed(NOW))

        then:
        thrown(IllegalArgumentException)

        when:
        new EventEnvelope<EventId, Event>(EventId.random(), EventId.random(), 1, 1, " ", 1, new Changed(NOW))

        then:
        thrown(IllegalArgumentException)

        when:
        new EventEnvelope<EventId, Event>(EventId.random(), EventId.random(), 1, 1, "Changed", 0, new Changed(NOW))

        then:
        thrown(IllegalArgumentException)
    }

    def "conflicting batch leaves existing stream unchanged"() {
        given:
        def store = new InMemoryEventStore<String, Event>()
        store.append("one", 0, [new NewEvent<Event>(EventId.random(), new Changed(NOW))])

        when:
        store.append("one", 0, [new NewEvent<Event>(EventId.random(), new Changed(NOW))])

        then:
        def conflict = thrown(VersionConflictException)
        conflict.streamId() == "one"
        conflict.expectedVersion() == 0
        conflict.actualVersion() == 1
        store.load("one").size() == 1
        store.readAll(0).size() == 1
    }
}
