package com.github.monaboiste.fairshare.settlement.application

import com.github.monaboiste.fairshare.common.commands.CommandDispatcher
import com.github.monaboiste.fairshare.common.commands.RegisteredCommandDispatcher
import com.github.monaboiste.fairshare.common.events.EventId
import com.github.monaboiste.fairshare.common.events.InMemoryEventStore
import com.github.monaboiste.fairshare.common.events.VersionConflictException
import com.github.monaboiste.fairshare.settlement.application.command.IdentifierConflict
import com.github.monaboiste.fairshare.settlement.application.command.OpenSettlement
import com.github.monaboiste.fairshare.settlement.application.command.OpenSettlementHandler
import com.github.monaboiste.fairshare.settlement.application.command.RenameSettlement
import com.github.monaboiste.fairshare.settlement.application.command.RenameSettlementHandler
import com.github.monaboiste.fairshare.settlement.application.command.SettlementNotFound
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlement
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlementHistory
import com.github.monaboiste.fairshare.settlement.application.query.SettlementQueryHandler
import com.github.monaboiste.fairshare.settlement.application.query.SettlementView
import com.github.monaboiste.fairshare.settlement.domain.Settlement
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
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
    def repository = new EventSourcedSettlementRepository(store, { EVENT_ID }, projector::accept)
    CommandDispatcher commands = new RegisteredCommandDispatcher([
        new OpenSettlementHandler(repository, CLOCK), new RenameSettlementHandler(repository, CLOCK)])
    def queries = new SettlementQueryHandler(projector, store)

    def "opening a Settlement makes its view and history available"() {
        when:
        def opened = commands.dispatch(new OpenSettlement(ID, "  Holiday  ", EUR))

        then:
        opened.getSuccess() == ID
        queries.handle(new GetSettlement(ID)) == Optional.of(new SettlementView(ID, "  Holiday  ", EUR, 1))
        queries.handle(new GetSettlementHistory(ID))*.payload() == [new SettlementOpened("  Holiday  ", EUR, NOW)]
        queries.handle(new GetSettlementHistory(ID))*.eventId() == [EVENT_ID]
    }

    def "renaming updates the view with the next version"() {
        given:
        commands.dispatch(new OpenSettlement(ID, "Holiday", EUR))

        when:
        def renamed = commands.dispatch(new RenameSettlement(ID, "Mountains"))
        commands.dispatch(new RenameSettlement(ID, "Mountains"))

        then:
        renamed.getSuccess() == ID
        queries.handle(new GetSettlement(ID)) == Optional.of(new SettlementView(ID, "Mountains", EUR, 2))
        queries.handle(new GetSettlementHistory(ID))*.sequence() == [1L, 2L]
    }

    def "reopening with #reopening is #outcome"() {
        given:
        commands.dispatch(new OpenSettlement(ID, "Holiday", EUR))
        commands.dispatch(new RenameSettlement(ID, "Mountains"))

        when:
        def result = commands.dispatch(new OpenSettlement(ID, name, currency))

        then:
        result.success() == accepted
        result.success() || result.getFailure() == new IdentifierConflict(ID)
        queries.handle(new GetSettlementHistory(ID)).size() == 2

        where:
        reopening                  | name        | currency || accepted
        "the original details"     | "Holiday"   | EUR      || true
        "the current name"         | "Mountains" | EUR      || false
        "a different currency"     | "Holiday"   | USD      || false

        outcome = accepted ? "idempotent" : "an identifier conflict"
    }

    def "renaming an unknown Settlement is not found and creates no stream"() {
        when:
        def result = commands.dispatch(new RenameSettlement(ID, "Mountains"))

        then:
        result.getFailure() == new SettlementNotFound(ID)
        store.streams().isEmpty()
        queries.handle(new GetSettlement(ID)).empty
        queries.handle(new GetSettlementHistory(ID)).empty
    }

    def "a blank name is rejected before dispatch"() {
        when:
        new OpenSettlement(ID, " \t", EUR)

        then:
        thrown(IllegalArgumentException)
    }

    def "a rename decided on an outdated Settlement fails with a version conflict"() {
        given:
        commands.dispatch(new OpenSettlement(ID, "Holiday", EUR))
        def racing = new RenameSettlementHandler(outdatedRepository(), CLOCK)
        commands.dispatch(new RenameSettlement(ID, "Winner"))

        when:
        racing.handle(new RenameSettlement(ID, "Loser"))

        then:
        thrown(VersionConflictException)
        queries.handle(new GetSettlement(ID)).orElseThrow().name() == "Winner"
    }

    private SettlementRepository outdatedRepository() {
        Settlement outdated = repository.findById(ID).orElseThrow()
        new SettlementRepository() {
            Optional<Settlement> findById(SettlementId id) { Optional.of(outdated) }
            void save(Settlement settlement) { repository.save(settlement) }
        }
    }
}
