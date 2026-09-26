package com.github.monaboiste.fairshare.settlement.infrastructure

import com.github.monaboiste.fairshare.common.events.EventEnvelope
import com.github.monaboiste.fairshare.common.events.EventId
import com.github.monaboiste.fairshare.common.events.PendingEvent
import com.github.monaboiste.fairshare.common.events.inmemory.InMemoryEventStore
import com.github.monaboiste.fairshare.quantity.money.Money
import com.github.monaboiste.fairshare.settlement.application.query.SettlementView
import com.github.monaboiste.fairshare.settlement.domain.EqualShareAllocation
import com.github.monaboiste.fairshare.settlement.domain.ExpenseDescription
import com.github.monaboiste.fairshare.settlement.domain.ExpenseId
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.Share
import com.github.monaboiste.fairshare.settlement.domain.event.ExpenseRecorded
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantAdded
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRemoved
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRenamed
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementRenamed
import com.github.monaboiste.fairshare.valuation.ValuationEngine
import java.time.Instant
import java.time.LocalDate
import javax.money.Monetary
import spock.lang.Specification

class SettlementProjectorSpec extends Specification {
    private static final SettlementId ID = new SettlementId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private static final SettlementId OTHER = new SettlementId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
    private static final ParticipantId PARTICIPANT =
        new ParticipantId(UUID.fromString("00000000-0000-0000-0000-000000000010"))
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z")
    private static final EUR = Monetary.getCurrency("EUR")
    def projector = new SettlementProjector()

    def "duplicate deliveries are ignored and renames preserve the opening currency"() {
        when:
        projector.accept([opened(ID), opened(ID), renamed(ID, 2, "Mountains")])

        then:
        projector.findById(ID) == Optional.of(new SettlementView(ID, "Mountains", EUR, 2, []))
    }

    def "a batch containing a gap does not partially update any view"() {
        when:
        projector.accept([opened(ID), renamed(OTHER, 2, "Skipped")])

        then:
        thrown(IllegalStateException)
        projector.findById(ID).empty
    }

    def "a gap in an opened Settlement is rejected"() {
        given:
        projector.accept([opened(ID)])

        when:
        projector.accept([renamed(ID, 3, "Skipped")])

        then:
        thrown(IllegalStateException)
        projector.findById(ID) == Optional.of(new SettlementView(ID, "Holiday", EUR, 1, []))
    }

    def "a missing Participant cannot be #change"() {
        given:
        projector.accept([opened(ID)])

        when:
        projector.accept([event(ID, 2, payload)])

        then:
        thrown(IllegalStateException)
        projector.findById(ID) == Optional.of(new SettlementView(ID, "Holiday", EUR, 1, []))

        where:
        change    | payload
        "renamed" | new ParticipantRenamed(PARTICIPANT, "Sam")
        "removed" | new ParticipantRemoved(PARTICIPANT)
    }

    def "batch stages touched streams while preserving untouched views"() {
        given:
        def untouched = new SettlementId(UUID.randomUUID())
        projector.accept([opened(untouched), opened(ID)])

        when:
        projector.accept([renamed(ID, 2, "Mountains"), opened(OTHER), renamed(OTHER, 2, "Forest")])

        then:
        projector.findById(untouched) == Optional.of(new SettlementView(untouched, "Holiday", EUR, 1, []))
        projector.findById(ID) == Optional.of(new SettlementView(ID, "Mountains", EUR, 2, []))
        projector.findById(OTHER) == Optional.of(new SettlementView(OTHER, "Forest", EUR, 2, []))
    }

    def "rebuild replaces existing views with globally ordered history"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        store.append(ID, 0, [pending(new SettlementOpened("Holiday", EUR))])
        store.append(OTHER, 0, [pending(new SettlementOpened("Other", EUR))])
        store.append(ID, 1, [pending(new SettlementRenamed("Mountains"))])
        def stale = new SettlementId(UUID.randomUUID())
        projector.accept([opened(stale)])

        when:
        projector.rebuild(store)

        then:
        projector.findById(ID) == Optional.of(new SettlementView(ID, "Mountains", EUR, 2, []))
        projector.findById(OTHER) == Optional.of(new SettlementView(OTHER, "Other", EUR, 1, []))
        projector.findById(stale).empty
    }

    def "a removed Participant cannot be re-added by a live delivery"() {
        given:
        projector.accept([
            opened(ID),
            event(ID, 2, new ParticipantAdded(PARTICIPANT, "Alex")),
            event(ID, 3, new ParticipantRemoved(PARTICIPANT))
        ])

        when:
        projector.accept([event(ID, 4, new ParticipantAdded(PARTICIPANT, "Alex"))])

        then:
        thrown(IllegalStateException)
        projector.findById(ID) == Optional.of(new SettlementView(ID, "Holiday", EUR, 3, []))
    }

    def "a removed Participant cannot be re-added during rebuild"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        store.append(ID, 0, [
            pending(new SettlementOpened("Holiday", EUR)),
            pending(new ParticipantAdded(PARTICIPANT, "Alex")),
            pending(new ParticipantRemoved(PARTICIPANT)),
            pending(new ParticipantAdded(PARTICIPANT, "Alex"))
        ])

        when:
        projector.rebuild(store)

        then:
        thrown(IllegalStateException)
    }

    def "recorded Expense is projected from frozen facts and rebuild matches live view"() {
        given:
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def payer = new ParticipantId(new UUID(0L, 11L))
        def valuation = ValuationEngine.standard().value(Money.of(3, "EUR"), EUR,
            LocalDate.of(2026, 1, 2).atStartOfDay(), [])
        def expense = new ExpenseRecorded(new ExpenseId(UUID.randomUUID()), new ExpenseDescription("Lunch"),
            LocalDate.of(2026, 1, 2), payer, Money.of(3, "EUR"), new EqualShareAllocation([PARTICIPANT]),
            valuation.componentVersion().id(), valuation.exchangeRate(), Money.of(3, "EUR"),
            [new Share(PARTICIPANT, Money.of(3, "EUR"))])
        def history = [pending(new SettlementOpened("Holiday", EUR)), pending(new ParticipantAdded(payer, "Payer")),
            pending(new ParticipantAdded(PARTICIPANT, "Recipient")), pending(expense)]
        store.append(ID, 0, history)
        projector.accept(store.load(ID))
        def live = projector.findById(ID).orElseThrow()

        when:
        def rebuilt = new SettlementProjector()
        rebuilt.rebuild(store)

        then:
        live.expenses().get(0).shares() == expense.shares()
        live.obligations()*.from() == [PARTICIPANT]
        live.obligations()*.to() == [payer]
        live.balances()[payer] == Money.of(3, "EUR")
        live.balances()[PARTICIPANT] == Money.of(-3, "EUR")
        rebuilt.findById(ID).orElseThrow() == live

        when:
        def second = new ExpenseRecorded(new ExpenseId(UUID.randomUUID()), expense.description(), expense.incurredOn(),
            payer, expense.originalAmount(), expense.allocation(), expense.componentVersionId(),
            expense.exchangeRate(), expense.valuation(), expense.shares())
        projector.accept([event(ID, 5, second)])
        def withTwoExpenses = projector.findById(ID).orElseThrow()

        then:
        withTwoExpenses.expenses()*.id() == [expense.expenseId(), second.expenseId()]
        withTwoExpenses.balances()[payer] == Money.of(6, "EUR")

        when:
        projector.accept([event(ID, 6, new ParticipantRemoved(PARTICIPANT))])

        then:
        thrown(IllegalStateException)
        projector.findById(ID).orElseThrow() == withTwoExpenses
    }

    def "Expense projection rejects missing payer, recipient or repeated identifier"() {
        given:
        def payer = new ParticipantId(new UUID(0L, 11L))
        def missing = new ParticipantId(new UUID(0L, 12L))
        def valued = ValuationEngine.standard().value(Money.of(1, "EUR"), EUR,
            LocalDate.of(2026, 1, 2).atStartOfDay(), [])
        projector.accept([opened(ID), event(ID, 2, new ParticipantAdded(payer, "Payer")),
            event(ID, 3, new ParticipantAdded(PARTICIPANT, "Recipient"))])
        def recorded = { whoPaid, recipient -> new ExpenseRecorded(new ExpenseId(new UUID(0L, 21L)),
            new ExpenseDescription("Dinner"), LocalDate.of(2026, 1, 2), whoPaid, Money.of(1, "EUR"),
            new EqualShareAllocation([recipient]), valued.componentVersion().id(), valued.exchangeRate(),
            Money.of(1, "EUR"), [new Share(recipient, Money.of(1, "EUR"))]) }

        when:
        projector.accept([event(ID, 4, recorded(missing, PARTICIPANT))])

        then:
        thrown(IllegalStateException)
        projector.findById(ID).orElseThrow().version() == 3

        when:
        projector.accept([event(ID, 4, recorded(payer, missing))])

        then:
        thrown(IllegalStateException)
        projector.findById(ID).orElseThrow().version() == 3

        when:
        projector.accept([event(ID, 4, recorded(payer, PARTICIPANT))])
        projector.accept([event(ID, 5, recorded(payer, PARTICIPANT))])

        then:
        thrown(IllegalStateException)
        projector.findById(ID).orElseThrow().version() == 4
    }

    private static PendingEvent<SettlementEvent> pending(SettlementEvent payload) {
        new PendingEvent<SettlementEvent>(EventId.random(), payload, NOW)
    }

    private static EventEnvelope<SettlementId, SettlementEvent> opened(SettlementId id) {
        new EventEnvelope<SettlementId, SettlementEvent>(id, 1, 1, EventId.random(), NOW,
            new SettlementOpened("Holiday", EUR))
    }

    private static EventEnvelope<SettlementId, SettlementEvent> event(
        SettlementId id, long sequence, SettlementEvent payload) {
        new EventEnvelope<SettlementId, SettlementEvent>(id, sequence, sequence, EventId.random(), NOW, payload)
    }

    private static EventEnvelope<SettlementId, SettlementEvent> renamed(SettlementId id, long sequence, String name) {
        new EventEnvelope<SettlementId, SettlementEvent>(id, sequence, sequence, EventId.random(), NOW,
            new SettlementRenamed(name))
    }
}
