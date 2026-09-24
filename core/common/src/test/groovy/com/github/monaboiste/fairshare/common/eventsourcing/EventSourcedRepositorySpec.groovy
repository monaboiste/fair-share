package com.github.monaboiste.fairshare.common.eventsourcing

import com.github.monaboiste.fairshare.common.events.Event
import com.github.monaboiste.fairshare.common.events.PostCommitPublicationException
import com.github.monaboiste.fairshare.common.events.VersionConflictException
import com.github.monaboiste.fairshare.common.events.inmemory.InMemoryEventStore
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import spock.lang.Specification

class EventSourcedRepositorySpec extends Specification {
    static class Incremented implements Event {
        String type() { "Incremented" }
        int schemaVersion() { 1 }
    }

    static class Counter extends AggregateRoot<String, Incremented> {
        private final String key
        int count
        Counter(String key) { super(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)); this.key = key }
        String id() { key }
        void increment() { register(new Incremented()) }
        protected void apply(Incremented change) { count++ }
    }

    def "repository persists and replays events without pending changes"() {
        given:
        def store = new InMemoryEventStore<String, Incremented>()
        def repository = new EventSourcedRepository<String, Incremented, Counter>(store, Counter::new)
        def counter = new Counter("one")
        counter.increment()
        counter.increment()
        def decidedIds = counter.pendingEvents()*.eventId()

        when:
        def commit = repository.save(counter)
        def loaded = repository.findById("one").orElseThrow()
        def unchanged = repository.save(loaded)

        then:
        commit.version() == 2
        commit.events()*.sequence() == [1L, 2L]
        commit.events()*.eventId() == decidedIds
        commit.events()*.occurredAt() == [Instant.EPOCH, Instant.EPOCH]
        loaded.count == 2
        loaded.pendingEvents().empty
        loaded.committedVersion() == 2
        unchanged.events().empty
        unchanged.version() == 2
        repository.findById("unknown").empty
    }

    def "publication failure retains the committed state and clears pending events"() {
        given:
        def store = new InMemoryEventStore<String, Incremented>()
        store.subscribe { throw new IllegalStateException("subscriber failed") }
        def repository = new EventSourcedRepository<String, Incremented, Counter>(store, Counter::new)
        def counter = new Counter("one")
        counter.increment()

        when:
        repository.save(counter)

        then:
        def failure = thrown(PostCommitPublicationException)
        failure.committedVersion() == 1
        counter.pendingEvents().empty
        counter.committedVersion() == 1
        store.load("one").size() == 1
    }

    def "failed save retains pending events"() {
        given:
        def store = new InMemoryEventStore<String, Incremented>()
        def repository = new EventSourcedRepository<String, Incremented, Counter>(store, Counter::new)
        def stale = new Counter("one")
        stale.increment()
        repository.save(new Counter("one").tap { increment() })

        when:
        repository.save(stale)

        then:
        thrown(VersionConflictException)
        stale.pendingEvents().size() == 1
        stale.committedVersion() == 0
    }
}
