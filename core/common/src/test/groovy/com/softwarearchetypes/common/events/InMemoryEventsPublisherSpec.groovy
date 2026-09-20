package com.softwarearchetypes.common.events

import java.time.Instant
import java.util.function.Predicate
import spock.lang.Specification

class InMemoryEventsPublisherSpec extends Specification {

    def "publishes an event only to handlers that support it"() {
        given:
        def publisher = new InMemoryEventsPublisher()
        def accepted = new TrackingHandler({ it.type() == "accepted" })
        def rejected = new TrackingHandler({ false })
        def event = event("accepted")
        publisher.register(accepted)
        publisher.register(rejected)

        when:
        publisher.publish(event)

        then:
        accepted.handled == [event]
        rejected.handled.isEmpty()
    }

    def "publishes every event in list order"() {
        given:
        def publisher = new InMemoryEventsPublisher()
        def handler = new TrackingHandler({ true })
        def first = event("first")
        def second = event("second")
        publisher.register(handler)

        when:
        publisher.publish([first, second])

        then:
        handler.handled == [first, second]
    }

    def "registering the same handler twice still handles an event once"() {
        given:
        def publisher = new InMemoryEventsPublisher()
        def handler = new TrackingHandler({ true })
        def event = event("event")
        publisher.register(handler)
        publisher.register(handler)

        when:
        publisher.publish(event)

        then:
        handler.handled == [event]
    }

    private static PublishedEvent event(String type) {
        new TestEvent(UUID.randomUUID(), type, Instant.parse("2026-09-19T12:00:00Z"))
    }

    private static final class TestEvent implements PublishedEvent {

        private final UUID id
        private final String type
        private final Instant occurredAt

        private TestEvent(UUID id, String type, Instant occurredAt) {
            this.id = id
            this.type = type
            this.occurredAt = occurredAt
        }

        @Override
        UUID id() {
            id
        }

        @Override
        String type() {
            type
        }

        @Override
        Instant occurredAt() {
            occurredAt
        }
    }

    private static final class TrackingHandler implements EventHandler {

        private final Predicate<PublishedEvent> supported
        private final List<PublishedEvent> handled = []

        private TrackingHandler(Predicate<PublishedEvent> supported) {
            this.supported = supported
        }

        @Override
        boolean supports(PublishedEvent event) {
            supported.test(event)
        }

        @Override
        void handle(PublishedEvent event) {
            handled.add(event)
        }
    }
}
