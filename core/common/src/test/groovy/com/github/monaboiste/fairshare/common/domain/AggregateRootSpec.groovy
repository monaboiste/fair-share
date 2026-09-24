package com.github.monaboiste.fairshare.common.domain

import com.github.monaboiste.fairshare.common.events.Event
import java.time.Instant
import spock.lang.Specification

class AggregateRootSpec extends Specification {

    def "registering an event applies it and keeps it pending"() {
        given:
        def counter = new Counter()

        when:
        counter.increment()
        counter.increment()

        then:
        counter.total == 2
        counter.pendingEvents() == [new Incremented(), new Incremented()]
        counter.version() == 2
        counter.committedVersion() == 0
    }

    def "flushing pending events hands them over once and marks them committed"() {
        given:
        def counter = new Counter()
        counter.increment()

        when:
        def flushed = counter.flushPendingEvents()

        then:
        flushed == [new Incremented()]
        counter.pendingEvents().empty
        counter.version() == 1
        counter.committedVersion() == 1
    }

    def "replaying history restores state without pending events"() {
        when:
        def counter = Counter.recreate([new Incremented(), new Incremented(), new Incremented()])

        then:
        counter.total == 3
        counter.version() == 3
        counter.committedVersion() == 3
        counter.pendingEvents().empty
    }

    private static record Incremented() implements Event {
        Instant occurredAt() { Instant.EPOCH }
        String type() { "Incremented" }
        int schemaVersion() { 1 }
    }

    private static class Counter extends AggregateRoot<String, Incremented> {
        int total

        static Counter recreate(List<Incremented> history) {
            def counter = new Counter()
            counter.replay(history)
            counter
        }

        String id() { "counter" }

        void increment() { register(new Incremented()) }

        protected void apply(Incremented event) { total++ }
    }
}
