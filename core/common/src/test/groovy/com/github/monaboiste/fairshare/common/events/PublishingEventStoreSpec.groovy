package com.github.monaboiste.fairshare.common.events

import com.github.monaboiste.fairshare.common.events.inmemory.InMemoryEventStore
import java.time.Instant
import spock.lang.Specification

class PublishingEventStoreSpec extends Specification {
    @EventType(name = "Change", version = 1)
    static class Change implements Event {
        Instant occurredAt() { Instant.EPOCH }
    }

    def "successful append publishes committed envelopes"() {
        given:
        def store = new InMemoryEventStore<String, Event>()
        List<EventEnvelope<String, Event>> received = []
        def publishing = new PublishingEventStore<String, Event>(store, store, { received.addAll(it) })

        when:
        def commit = publishing.append("one", 0, [new NewEvent<Event>(EventId.random(), new Change())])

        then:
        received == commit.events()
        publishing.readAll(0) == received
    }

    def "listener failure is fatal but does not undo commit"() {
        given:
        def store = new InMemoryEventStore<String, Event>()
        def publishing = new PublishingEventStore<String, Event>(store, store, { throw new IllegalStateException("offline") })

        when:
        publishing.append("one", 0, [new NewEvent<Event>(EventId.random(), new Change())])

        then:
        def failure = thrown(PostCommitPublicationException)
        failure.streamId() == "one"
        failure.committedVersion() == 1
        failure.cause.message == "offline"
        store.load("one").size() == 1
    }

    def "failed append never publishes"() {
        given:
        def store = new InMemoryEventStore<String, Event>()
        List<EventEnvelope<String, Event>> received = []
        def publishing = new PublishingEventStore<String, Event>(store, store, { received.addAll(it) })

        when:
        publishing.append("one", 1, [new NewEvent<Event>(EventId.random(), new Change())])

        then:
        thrown(VersionConflictException)
        received.empty
    }
}
