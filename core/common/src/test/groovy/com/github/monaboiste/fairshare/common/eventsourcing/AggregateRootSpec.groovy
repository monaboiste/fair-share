package com.github.monaboiste.fairshare.common.eventsourcing

import com.github.monaboiste.fairshare.common.events.Event
import com.github.monaboiste.fairshare.common.events.EventId
import java.time.Instant
import spock.lang.Specification

class AggregateRootSpec extends Specification {
    static class Changed implements Event {
        private final EventId identity = EventId.random()
        EventId eventId() { identity }
        String type() { "Changed" }
        int schemaVersion() { 1 }
        Instant occurredAt() { Instant.EPOCH }
    }

    static class Counter extends AggregateRoot<String, Changed> {
        String id() { "counter" }
        void change() { register(new Changed()) }
        protected void apply(Changed change) {}
    }

    def "registered events advance current version without advancing committed version"() {
        given:
        def counter = new Counter()

        when:
        counter.change()
        def pending = counter.pendingEvents()

        then:
        counter.version() == 1
        counter.committedVersion() == 0
        pending.size() == 1

        when:
        counter.markCommitted(1)

        then:
        counter.pendingEvents().empty
        pending.size() == 1
        counter.committedVersion() == 1
    }

    def "invalid commit does not clear pending events"() {
        given:
        def counter = new Counter()
        counter.change()

        when:
        counter.markCommitted(2)

        then:
        thrown(IllegalArgumentException)
        counter.pendingEvents().size() == 1
    }

    def "replay only accepts fresh aggregates"() {
        given:
        def counter = new Counter()
        counter.replay([new Changed()])

        when:
        counter.replay([new Changed()])

        then:
        thrown(IllegalStateException)
        counter.version() == 1

        when:
        def pending = new Counter()
        pending.change()
        pending.replay([new Changed()])

        then:
        thrown(IllegalStateException)
    }
}
