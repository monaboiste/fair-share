package com.github.monaboiste.fairshare.common.eventsourcing

import com.github.monaboiste.fairshare.common.events.Event
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import spock.lang.Specification

class AggregateRootSpec extends Specification {
    static class Changed implements Event {
        String type() { "Changed" }
        int schemaVersion() { 1 }
    }

    static class Counter extends AggregateRoot<String, Changed> {
        Counter(Clock clock) { super(clock) }
        String id() { "counter" }
        void change() { register(new Changed()) }
        protected void apply(Changed change) {}
    }

    def "registered events advance current version without advancing committed version"() {
        given:
        def counter = new Counter(Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC))

        when:
        counter.change()
        def pending = counter.pendingEvents()

        then:
        counter.version() == 1
        counter.committedVersion() == 0
        pending.size() == 1
        pending.first().payload() instanceof Changed
        pending.first().eventId() != null
        pending.first().occurredAt() == Instant.parse("2026-01-01T12:00:00Z")

        when:
        counter.markCommitted(1)

        then:
        counter.pendingEvents().empty
        pending.size() == 1
        counter.committedVersion() == 1
    }

    def "invalid commit does not clear pending events"() {
        given:
        def counter = new Counter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
        counter.change()

        when:
        counter.markCommitted(2)

        then:
        thrown(IllegalArgumentException)
        counter.pendingEvents().size() == 1
    }

    def "replay only accepts fresh aggregates"() {
        given:
        def counter = new Counter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
        counter.replay([new Changed()])

        when:
        counter.replay([new Changed()])

        then:
        thrown(IllegalStateException)
        counter.version() == 1

        when:
        def pending = new Counter(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
        pending.change()
        pending.replay([new Changed()])

        then:
        thrown(IllegalStateException)
    }
}
