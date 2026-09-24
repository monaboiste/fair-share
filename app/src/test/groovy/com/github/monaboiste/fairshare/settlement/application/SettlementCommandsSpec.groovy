package com.github.monaboiste.fairshare.settlement.application

import com.github.monaboiste.fairshare.common.commands.RegisteredCommandDispatcher
import com.github.monaboiste.fairshare.common.events.CommitResult
import com.github.monaboiste.fairshare.common.events.EventEnvelope
import com.github.monaboiste.fairshare.common.events.EventId
import com.github.monaboiste.fairshare.common.events.EventStore
import com.github.monaboiste.fairshare.common.events.NewEvent
import com.github.monaboiste.fairshare.common.events.PostCommitPublicationException
import com.github.monaboiste.fairshare.common.events.PublishingEventStore
import com.github.monaboiste.fairshare.common.events.StreamNotFoundException
import com.github.monaboiste.fairshare.common.events.VersionConflictException
import com.github.monaboiste.fairshare.common.events.inmemory.InMemoryEventStore
import com.github.monaboiste.fairshare.common.queries.RegisteredQueryDispatcher
import com.github.monaboiste.fairshare.settlement.application.command.OpenSettlement
import com.github.monaboiste.fairshare.settlement.application.command.OpenSettlementHandler
import com.github.monaboiste.fairshare.settlement.application.command.RenameSettlement
import com.github.monaboiste.fairshare.settlement.application.command.RenameSettlementHandler
import com.github.monaboiste.fairshare.settlement.application.command.SettlementCommand
import com.github.monaboiste.fairshare.settlement.application.command.SettlementNotFound
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlement
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlementHandler
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlementHistory
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlementHistoryHandler
import com.github.monaboiste.fairshare.settlement.application.query.SettlementQuery
import com.github.monaboiste.fairshare.settlement.application.query.SettlementView
import com.github.monaboiste.fairshare.settlement.domain.IdentifierConflict
import com.github.monaboiste.fairshare.settlement.domain.Settlement
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.SettlementName
import com.github.monaboiste.fairshare.settlement.domain.SettlementRepository
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened
import com.github.monaboiste.fairshare.settlement.infrastructure.EventSourcedSettlementRepository
import com.github.monaboiste.fairshare.settlement.infrastructure.SettlementProjector
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import javax.money.Monetary
import spock.lang.Specification

class SettlementCommandsSpec extends Specification {
    private static final SettlementId ID = new SettlementId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private static final EventId EVENT_ID = new EventId(UUID.fromString("00000000-0000-0000-0000-00000000000a"))
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z")
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC)
    private static final EUR = Monetary.getCurrency("EUR")
    private static final USD = Monetary.getCurrency("USD")

    def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
    def projector = new SettlementProjector()
    EventStore<SettlementId, SettlementEvent> publishing = new PublishingEventStore<>(store, store, projector)
    SettlementRepository repository = new EventSourcedSettlementRepository(publishing, { EVENT_ID })
    def commands = RegisteredCommandDispatcher.builder()
        .register(OpenSettlement, new OpenSettlementHandler(repository, store, CLOCK))
        .register(RenameSettlement, new RenameSettlementHandler(repository, CLOCK))
        .requireHandlersFor(SettlementCommand).build()
    def queries = RegisteredQueryDispatcher.builder()
        .register(GetSettlement, new GetSettlementHandler(projector))
        .register(GetSettlementHistory, new GetSettlementHistoryHandler(store))
        .requireHandlersFor(SettlementQuery).build()

    def "open and rename return deterministic committed envelopes and an inspectable view"() {
        when:
        def opened = commands.dispatch(new OpenSettlement(ID, new SettlementName("  Holiday  "), EUR))
        def renamed = commands.dispatch(new RenameSettlement(ID, new SettlementName("Mountains")))
        def noChange = commands.dispatch(new RenameSettlement(ID, new SettlementName("Mountains")))

        then:
        opened.getSuccess().version() == 1
        opened.getSuccess().events()*.payload() == [new SettlementOpened("  Holiday  ", "EUR", NOW)]
        opened.getSuccess().events()*.eventId() == [EVENT_ID]
        opened.getSuccess().events()*.position() == [1L]
        renamed.getSuccess().version() == 2
        renamed.getSuccess().events()*.sequence() == [2L]
        renamed.getSuccess().events()*.occurredAt() == [NOW]
        noChange.getSuccess().events().empty
        noChange.getSuccess().version() == 2
        queries.dispatch(new GetSettlement(ID)) == Optional.of(new SettlementView(ID, "Mountains", EUR, 2))
        queries.dispatch(new GetSettlementHistory(ID))*.sequence() == [1L, 2L]
    }

    def "reopening with #reopening is #outcome"() {
        given:
        def opened = commands.dispatch(new OpenSettlement(ID, new SettlementName("Holiday"), EUR))
        commands.dispatch(new RenameSettlement(ID, new SettlementName("Mountains")))

        when:
        def result = commands.dispatch(new OpenSettlement(ID, new SettlementName(name), currency))

        then:
        result.success() == accepted
        result.success() ? result.getSuccess() == opened.getSuccess() : result.getFailure() == new IdentifierConflict(ID)
        queries.dispatch(new GetSettlementHistory(ID)).size() == 2

        where:
        reopening              | name        | currency || accepted
        "the opening details"  | "Holiday"   | EUR      || true
        "the current name"     | "Mountains" | EUR      || false
        "another currency"     | "Holiday"   | USD      || false

        outcome = accepted ? "idempotent" : "an identifier conflict"
    }

    def "unknown rename returns a rejection while unknown history throws"() {
        when:
        def result = commands.dispatch(new RenameSettlement(ID, new SettlementName("Mountains")))

        then:
        result.getFailure() == new SettlementNotFound(ID)
        queries.dispatch(new GetSettlement(ID)).empty
        !store.exists(ID)

        when:
        queries.dispatch(new GetSettlementHistory(ID))

        then:
        def failure = thrown(StreamNotFoundException)
        failure.streamId() == ID
    }

    def "stale expected version is rejected even on a no-op rename"() {
        given:
        commands.dispatch(new OpenSettlement(ID, new SettlementName("Holiday"), EUR))

        when:
        commands.dispatch(new RenameSettlement(ID, new SettlementName("Holiday"), 0L))

        then:
        def failure = thrown(VersionConflictException)
        failure.expectedVersion() == 0
        failure.actualVersion() == 1
    }

    def "stale aggregate fails optimistic concurrency"() {
        given:
        commands.dispatch(new OpenSettlement(ID, new SettlementName("Holiday"), EUR))
        def stale = repository.findById(ID).orElseThrow()
        commands.dispatch(new RenameSettlement(ID, new SettlementName("Winner")))
        SettlementRepository outdated = new SettlementRepository() {
            Optional<Settlement> findById(SettlementId id) { Optional.of(stale) }
            CommitResult<SettlementId, SettlementEvent> save(Settlement settlement) { repository.save(settlement) }
        }

        when:
        new RenameSettlementHandler(outdated, CLOCK).handle(new RenameSettlement(ID, new SettlementName("Loser")))

        then:
        thrown(VersionConflictException)
        queries.dispatch(new GetSettlement(ID)).orElseThrow().name() == "Winner"
    }

    def "an identical concurrent open reloads and returns the original envelope"() {
        given:
        def opened = commands.dispatch(new OpenSettlement(ID, new SettlementName("Holiday"), EUR))
        SettlementRepository racing = new SettlementRepository() {
            boolean first = true
            Optional<Settlement> findById(SettlementId id) {
                if (first) { first = false; return Optional.empty() }
                repository.findById(id)
            }
            CommitResult<SettlementId, SettlementEvent> save(Settlement settlement) { repository.save(settlement) }
        }

        when:
        def retry = new OpenSettlementHandler(racing, store, CLOCK)
            .handle(new OpenSettlement(ID, new SettlementName("Holiday"), EUR))

        then:
        retry.getSuccess() == opened.getSuccess()
        store.load(ID).size() == 1
    }

    def "a conflicting concurrent open reloads and returns identifier conflict"() {
        given:
        commands.dispatch(new OpenSettlement(ID, new SettlementName("Holiday"), EUR))
        SettlementRepository racing = new SettlementRepository() {
            boolean first = true
            Optional<Settlement> findById(SettlementId id) {
                if (first) { first = false; return Optional.empty() }
                repository.findById(id)
            }
            CommitResult<SettlementId, SettlementEvent> save(Settlement settlement) { repository.save(settlement) }
        }

        when:
        def retry = new OpenSettlementHandler(racing, store, CLOCK)
            .handle(new OpenSettlement(ID, new SettlementName("Different"), EUR))

        then:
        retry.getFailure() == new IdentifierConflict(ID)
        store.load(ID).size() == 1
    }

    def "publication failure leaves the command committed with a fatal versioned exception"() {
        given:
        def failingStore = new PublishingEventStore<SettlementId, SettlementEvent>(store, store,
            { throw new IllegalStateException("projection failed") })
        def failingRepository = new EventSourcedSettlementRepository(failingStore, { EVENT_ID })
        def handler = new OpenSettlementHandler(failingRepository, store, CLOCK)

        when:
        handler.handle(new OpenSettlement(ID, new SettlementName("Holiday"), EUR))

        then:
        def failure = thrown(PostCommitPublicationException)
        failure.streamId() == ID
        failure.committedVersion() == 1
        store.load(ID).size() == 1
        projector.findById(ID).empty
    }

    def "store failures propagate unchanged and leave no projection"() {
        given:
        def outage = new IllegalStateException("unavailable")
        EventStore<SettlementId, SettlementEvent> broken = new EventStore<SettlementId, SettlementEvent>() {
            boolean exists(SettlementId id) { false }
            List<EventEnvelope<SettlementId, SettlementEvent>> load(SettlementId id) { throw new AssertionError("load") }
            CommitResult<SettlementId, SettlementEvent> append(SettlementId id, long version,
                    List<NewEvent<SettlementEvent>> events) { throw outage }
        }
        def handler = new OpenSettlementHandler(new EventSourcedSettlementRepository(broken, { EVENT_ID }), broken, CLOCK)

        when:
        handler.handle(new OpenSettlement(ID, new SettlementName("Holiday"), EUR))

        then:
        def failure = thrown(IllegalStateException)
        failure.is(outage)
        projector.findById(ID).empty
    }

    def "rebuilding from store after live commands reproduces the same view"() {
        given:
        commands.dispatch(new OpenSettlement(ID, new SettlementName("Holiday"), EUR))
        commands.dispatch(new RenameSettlement(ID, new SettlementName("Mountains")))
        def rebuilt = new SettlementProjector()

        when:
        rebuilt.rebuild(store)

        then:
        rebuilt.findById(ID) == queries.dispatch(new GetSettlement(ID))
    }
}
