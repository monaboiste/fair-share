package com.github.monaboiste.fairshare.common.events

import com.github.monaboiste.fairshare.common.events.inmemory.InMemoryEventStore
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import spock.lang.Specification

class EventStoreSpec extends Specification {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z")

    @EventType(name = "Changed", version = 2)
    static class Changed implements Event {
        EventId identity
        Instant time
        Changed(Instant time) { this(EventId.random(), time) }
        Changed(EventId identity, Instant time) { this.identity = identity; this.time = time }
        EventId eventId() { identity }
        Instant occurredAt() { time }
    }

    @EventType(name = " ", version = 0)
    static class InvalidType implements Event {
        private final EventId identity = EventId.random()
        EventId eventId() { identity }
        Instant occurredAt() { NOW }
    }

    static class Unannotated implements Event {
        private final EventId identity = EventId.random()
        Instant time
        Unannotated(Instant time) { this.time = time }
        EventId eventId() { identity }
        Instant occurredAt() { time }
    }

    def "append assigns ordered stream sequences and global positions without losing metadata"() {
        given:
        def store = new InMemoryEventStore<String, Event>()
        Event first = new Changed(NOW)
        Event second = new Changed(NOW)

        when:
        def committed = store.append("one", 0, [first, second])
        def other = store.append("two", 0, [new Changed(NOW)])

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

    def "stored envelope derives identity from the committed event"() {
        given:
        def store = new InMemoryEventStore<String, Event>()
        def identity = EventId.random()
        def event = new Changed(identity, NOW)

        when:
        def committed = store.append("one", 0, [event])

        then:
        committed.events().first().eventId() == identity
        committed.events().first().payload().is(event)
        store.load("one").first().eventId() == identity
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
        store.append("missing", 0, [new Unannotated(NOW)])

        then:
        thrown(IllegalArgumentException)
        !store.exists("missing")

        when:
        store.append("missing", 0, [new Changed(NOW), new InvalidType()])

        then:
        thrown(IllegalArgumentException)
        !store.exists("missing")
        store.readAll(0).empty
    }

    def "invalid envelope metadata is rejected"() {
        when:
        new EventEnvelope<EventId, Event>(EventId.random(), 0, 1, "Changed", 1, new Changed(NOW))

        then:
        thrown(IllegalArgumentException)

        when:
        new EventEnvelope<EventId, Event>(EventId.random(), 1, 0, "Changed", 1, new Changed(NOW))

        then:
        thrown(IllegalArgumentException)

        when:
        new EventEnvelope<EventId, Event>(EventId.random(), 1, 1, " ", 1, new Changed(NOW))

        then:
        thrown(IllegalArgumentException)

        when:
        new EventEnvelope<EventId, Event>(EventId.random(), 1, 1, "Changed", 0, new Changed(NOW))

        then:
        thrown(IllegalArgumentException)
    }

    def "every append delivers committed envelopes to subscribers in subscription order"() {
        given:
        def store = new InMemoryEventStore<String, Event>()
        List<String> deliveries = []
        store.subscribe { events -> deliveries.add("first ${events.first().position()}") }
        store.subscribe { events -> deliveries.add("second ${events.first().position()}") }

        when:
        def first = store.append("one", 0, [new Changed(NOW)])
        def second = store.append("two", 0, [new Changed(NOW)])

        then:
        deliveries == ["first 1", "second 1", "first 2", "second 2"]
        store.readAll(0) == first.events() + second.events()
    }

    def "a failing subscriber stops delivery but keeps the stream committed"() {
        given:
        def store = new InMemoryEventStore<String, Event>()
        List<EventEnvelope<String, Event>> delivered = []
        def outage = new IllegalStateException("projection failed")
        store.subscribe { events -> delivered.addAll(events); throw outage }
        store.subscribe { events -> throw new AssertionError("later subscriber must not run") }

        when:
        store.append("one", 0, [new Changed(NOW)])

        then:
        def failure = thrown(PostCommitPublicationException)
        failure.streamId() == "one"
        failure.committedVersion() == 1
        failure.cause.is(outage)
        delivered == store.load("one")
        store.readAll(0) == delivered
    }

    def "a subscriber appending during delivery is rejected without committing"() {
        given:
        def store = new InMemoryEventStore<String, Event>()
        store.subscribe { events ->
            if (events.first().streamId() == "one") {
                store.append("two", 0, [new Changed(NOW)])
            }
        }

        when:
        store.append("one", 0, [new Changed(NOW)])

        then:
        def failure = thrown(PostCommitPublicationException)
        failure.cause instanceof IllegalStateException
        store.exists("one")
        !store.exists("two")
        store.readAll(0)*.streamId() == ["one"]
    }

    def "concurrent writers deliver events in commit order"() {
        given:
        def store = new InMemoryEventStore<String, Event>()
        def firstDelivered = new CountDownLatch(1)
        def releaseFirst = new CountDownLatch(1)
        List<Long> delivered = Collections.synchronizedList(new ArrayList<Long>())
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<Throwable>())
        store.subscribe { events ->
            if (events.first().streamId() == "one") {
                firstDelivered.countDown()
                if (!releaseFirst.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("first writer not released")
                }
            }
            delivered.addAll(events*.position())
        }
        Thread first = Thread.ofPlatform().unstarted({
            try {
                store.append("one", 0, [new Changed(NOW)])
            } catch (Throwable failure) {
                failures.add(failure)
            }
        } as Runnable)
        Thread second = Thread.ofPlatform().unstarted({
            try {
                store.append("two", 0, [new Changed(NOW)])
            } catch (Throwable failure) {
                failures.add(failure)
            }
        } as Runnable)

        when:
        boolean entered
        boolean blocked
        try {
            first.start()
            entered = firstDelivered.await(5, TimeUnit.SECONDS)
            second.start()
            blocked = awaitsAppendLock(second)
        } finally {
            releaseFirst.countDown()
            first.join(5000)
            second.join(5000)
        }

        then:
        entered
        blocked
        !first.alive
        !second.alive
        failures.empty
        delivered == [1L, 2L]
        store.readAll(0)*.position() == delivered
    }

    def "conflicting batch leaves existing stream unchanged"() {
        given:
        def store = new InMemoryEventStore<String, Event>()
        store.append("one", 0, [new Changed(NOW)])

        when:
        store.append("one", 0, [new Changed(NOW)])

        then:
        def conflict = thrown(VersionConflictException)
        conflict.streamId() == "one"
        conflict.expectedVersion() == 0
        conflict.actualVersion() == 1
        store.load("one").size() == 1
        store.readAll(0).size() == 1
    }

    private static boolean awaitsAppendLock(Thread writer) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (System.nanoTime() < deadline) {
            if (writer.state in [Thread.State.BLOCKED, Thread.State.WAITING]) {
                return true
            }
            Thread.onSpinWait()
        }
        false
    }
}
