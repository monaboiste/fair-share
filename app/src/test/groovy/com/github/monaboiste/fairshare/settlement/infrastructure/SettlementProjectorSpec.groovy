package com.github.monaboiste.fairshare.settlement.infrastructure

import com.github.monaboiste.fairshare.common.events.EventEnvelope
import com.github.monaboiste.fairshare.common.events.EventId
import com.github.monaboiste.fairshare.common.events.PendingEvent
import com.github.monaboiste.fairshare.common.events.inmemory.InMemoryEventStore
import com.github.monaboiste.fairshare.pricing.component.ComponentVersionId
import com.github.monaboiste.fairshare.quantity.money.Money
import com.github.monaboiste.fairshare.settlement.application.query.SettlementView
import com.github.monaboiste.fairshare.settlement.domain.EqualShareAllocation
import com.github.monaboiste.fairshare.settlement.domain.ExpenseDescription
import com.github.monaboiste.fairshare.settlement.domain.ExpenseId
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId
import com.github.monaboiste.fairshare.settlement.domain.RepaymentId
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.Share
import com.github.monaboiste.fairshare.settlement.domain.event.ExpenseCancelled
import com.github.monaboiste.fairshare.settlement.domain.event.ExpenseRecorded
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantAdded
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRemoved
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRenamed
import com.github.monaboiste.fairshare.settlement.domain.event.RepaymentCancelled
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementRenamed
import com.github.monaboiste.fairshare.valuation.ExchangeRate
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
    private static final ComponentVersionId VERSION_ID = new ComponentVersionId(new UUID(0L, 61L))
    private static final ExchangeRate IDENTITY_RATE = ExchangeRate.of(EUR, EUR, BigDecimal.ONE)
    def projector = new SettlementProjector()

    def "duplicate deliveries are ignored and renames preserve the opening currency"() {
        when: "the opening is delivered twice, followed by a rename to Mountains"
        projector.accept([opened(ID), opened(ID), renamed(ID, 2, "Mountains")])

        then: "the view shows the renamed Settlement at version 2, still in euros"
        projector.findById(ID) == Optional.of(emptySettlementView(ID, "Mountains", 2))
    }

    def "a batch containing a gap does not partially update any view"() {
        when: "a batch opens one Settlement and renames another that was never opened"
        projector.accept([opened(ID), renamed(OTHER, 2, "Skipped")])

        then: "the batch is rejected and neither Settlement is projected"
        thrown(IllegalStateException)
        projector.findById(ID).empty
    }

    def "a gap in an opened Settlement is rejected"() {
        given: "a Settlement has been opened"
        projector.accept([opened(ID)])

        when: "an event arrives that skips a version"
        projector.accept([renamed(ID, 3, "Skipped")])

        then: "it is rejected and the view stays as opened"
        thrown(IllegalStateException)
        projector.findById(ID) == Optional.of(emptySettlementView(ID, "Holiday", 1))
    }

    def "a missing Participant cannot be #change"() {
        given: "a Settlement has been opened without any Participants"
        projector.accept([opened(ID)])

        when: "a change arrives for a Participant who was never added"
        projector.accept([event(ID, 2, payload)])

        then: "it is rejected and the view stays as opened"
        thrown(IllegalStateException)
        projector.findById(ID) == Optional.of(emptySettlementView(ID, "Holiday", 1))

        where:
        change    | payload
        "renamed" | new ParticipantRenamed(PARTICIPANT, "Sam")
        "removed" | new ParticipantRemoved(PARTICIPANT)
    }

    def "batch stages touched streams while preserving untouched views"() {
        given: "two Settlements have been opened"
        def untouched = new SettlementId(UUID.randomUUID())
        projector.accept([opened(untouched), opened(ID)])

        when: "a batch renames one of them and opens and renames a third"
        projector.accept([renamed(ID, 2, "Mountains"), opened(OTHER), renamed(OTHER, 2, "Forest")])

        then: "the untouched Settlement is unchanged while the other two show their latest names"
        projector.findById(untouched) == Optional.of(emptySettlementView(untouched, "Holiday", 1))
        projector.findById(ID) == Optional.of(emptySettlementView(ID, "Mountains", 2))
        projector.findById(OTHER) == Optional.of(emptySettlementView(OTHER, "Forest", 2))
    }

    def "rebuild replaces existing views with globally ordered history"() {
        given: "a saved history of two Settlements, one renamed, and a stale view of a Settlement not in the store"
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        store.append(ID, 0, [pending(new SettlementOpened("Holiday", EUR))])
        store.append(OTHER, 0, [pending(new SettlementOpened("Other", EUR))])
        store.append(ID, 1, [pending(new SettlementRenamed("Mountains"))])
        def stale = new SettlementId(UUID.randomUUID())
        projector.accept([opened(stale)])

        when: "the projection is rebuilt from the store"
        projector.rebuild(store)

        then: "only the saved Settlements are shown, with their latest names and versions"
        projector.findById(ID) == Optional.of(emptySettlementView(ID, "Mountains", 2))
        projector.findById(OTHER) == Optional.of(emptySettlementView(OTHER, "Other", 1))
        projector.findById(stale).empty
    }

    def "a removed Participant cannot be re-added by a live delivery"() {
        given: "a Participant has been added to a projected Settlement and then removed"
        projector.accept([
            opened(ID),
            event(ID, 2, new ParticipantAdded(PARTICIPANT, "Alex")),
            event(ID, 3, new ParticipantRemoved(PARTICIPANT))
        ])

        when: "a live delivery adds the same Participant again"
        projector.accept([event(ID, 4, new ParticipantAdded(PARTICIPANT, "Alex"))])

        then: "it is rejected and the view stays as it was after the removal"
        thrown(IllegalStateException)
        projector.findById(ID) == Optional.of(emptySettlementView(ID, "Holiday", 3))
    }

    def "a removed Participant cannot be re-added during rebuild"() {
        given: "a saved history in which a removed Participant is added again"
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        store.append(ID, 0, [
            pending(new SettlementOpened("Holiday", EUR)),
            pending(new ParticipantAdded(PARTICIPANT, "Alex")),
            pending(new ParticipantRemoved(PARTICIPANT)),
            pending(new ParticipantAdded(PARTICIPANT, "Alex"))
        ])

        when: "the projection is rebuilt from the store"
        projector.rebuild(store)

        then: "the rebuild is rejected"
        thrown(IllegalStateException)
    }

    def "recorded Expense is projected from frozen facts and rebuild matches live view"() {
        given: "a live view of a 3 euro Expense paid by one Participant and fully shared to another"
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def payer = new ParticipantId(new UUID(0L, 11L))
        def expense = new ExpenseRecorded(new ExpenseId(UUID.randomUUID()), new ExpenseDescription("Lunch"),
            LocalDate.of(2026, 1, 2), payer, Money.of(3, "EUR"), new EqualShareAllocation([PARTICIPANT]), null,
            VERSION_ID, IDENTITY_RATE, Money.of(3, "EUR"),
            [new Share(PARTICIPANT, Money.of(3, "EUR"))])
        def history = [pending(new SettlementOpened("Holiday", EUR)), pending(new ParticipantAdded(payer, "Payer")),
            pending(new ParticipantAdded(PARTICIPANT, "Recipient")), pending(expense)]
        store.append(ID, 0, history)
        projector.accept(store.load(ID))
        def live = projector.findById(ID).orElseThrow()

        when: "the projection is rebuilt from the same history"
        def rebuilt = new SettlementProjector()
        rebuilt.rebuild(store)

        then: "the recipient owes the payer 3 euros and the rebuilt view matches the live one"
        live.expenses().get(0).shares() == expense.shares()
        live.obligations()*.from() == [PARTICIPANT]
        live.obligations()*.to() == [payer]
        live.balances()[payer] == Money.of(3, "EUR")
        live.balances()[PARTICIPANT] == Money.of(-3, "EUR")
        rebuilt.findById(ID).orElseThrow() == live

        when: "a second Expense with the same facts is delivered"
        def second = new ExpenseRecorded(new ExpenseId(UUID.randomUUID()), expense.description(), expense.incurredOn(),
            payer, expense.originalAmount(), expense.allocation(), null, expense.componentVersionId(),
            expense.exchangeRate(), expense.valuation(), expense.shares())
        projector.accept([event(ID, 5, second)])
        def withTwoExpenses = projector.findById(ID).orElseThrow()

        then: "both Expenses are listed and the payer's Balance doubles to 6 euros"
        withTwoExpenses.expenses()*.id() == [expense.expenseId(), second.expenseId()]
        withTwoExpenses.balances()[payer] == Money.of(6, "EUR")

        when: "the recipient, who still holds a Share, is removed"
        projector.accept([event(ID, 6, new ParticipantRemoved(PARTICIPANT))])

        then: "the removal is rejected and the view with both Expenses is kept"
        thrown(IllegalStateException)
        projector.findById(ID).orElseThrow() == withTwoExpenses
    }

    def "Expense projection rejects missing payer, recipient or repeated identifier"() {
        given: "a projected Settlement with a payer and a recipient, and a way to build a 1 euro Expense"
        def payer = new ParticipantId(new UUID(0L, 11L))
        def missing = new ParticipantId(new UUID(0L, 12L))
        projector.accept([opened(ID), event(ID, 2, new ParticipantAdded(payer, "Payer")),
            event(ID, 3, new ParticipantAdded(PARTICIPANT, "Recipient"))])
        def recorded = { whoPaid, recipient -> new ExpenseRecorded(new ExpenseId(new UUID(0L, 21L)),
            new ExpenseDescription("Dinner"), LocalDate.of(2026, 1, 2), whoPaid, Money.of(1, "EUR"),
            new EqualShareAllocation([recipient]), null, VERSION_ID, IDENTITY_RATE,
            Money.of(1, "EUR"), [new Share(recipient, Money.of(1, "EUR"))]) }

        when: "an Expense paid by an unknown Participant arrives"
        projector.accept([event(ID, 4, recorded(missing, PARTICIPANT))])

        then: "it is rejected and the view stays unchanged"
        thrown(IllegalStateException)
        projector.findById(ID).orElseThrow().version() == 3

        when: "an Expense shared with an unknown Participant arrives"
        projector.accept([event(ID, 4, recorded(payer, missing))])

        then: "it is rejected and the view stays unchanged"
        thrown(IllegalStateException)
        projector.findById(ID).orElseThrow().version() == 3

        when: "a valid Expense arrives, followed by another under the same identifier"
        projector.accept([event(ID, 4, recorded(payer, PARTICIPANT))])
        projector.accept([event(ID, 5, recorded(payer, PARTICIPANT))])

        then: "the repeat is rejected and only the first Expense is kept"
        thrown(IllegalStateException)
        projector.findById(ID).orElseThrow().version() == 4
    }

    def "an unknown #kind cancellation rejects live batches and rebuilds atomically"() {
        given: "an opened Settlement with an untouched prior view"
        projector.accept([opened(ID)])
        def before = projector.findById(ID).orElseThrow()
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        store.append(ID, 0, [pending(new SettlementOpened("Holiday", EUR)),
            pending(new SettlementRenamed("Updated")), pending(invalid.call())])

        when: "a batch includes a valid rename before the invalid cancellation"
        projector.accept(store.load(ID).drop(1))

        then: "the batch fails without committing the rename"
        thrown(IllegalStateException)
        projector.findById(ID).orElseThrow() == before

        when: "a rebuild uses the invalid history"
        projector.rebuild(store)

        then: "the rebuild fails without replacing the existing view"
        thrown(IllegalStateException)
        projector.findById(ID).orElseThrow() == before

        where:
        kind | invalid
        "Expense" | { -> new ExpenseCancelled(new ExpenseId(new UUID(0, 21))) }
        "Repayment" | { -> new RepaymentCancelled(new RepaymentId(new UUID(0, 31))) }
    }

    def "repeated Expense cancellation rejects a projected batch without changing the view"() {
        given: "an Expense has been recorded and cancelled"
        def expenseId = new ExpenseId(new UUID(0, 21))
        def recorded = new ExpenseRecorded(expenseId, new ExpenseDescription("Lunch"), LocalDate.of(2026, 1, 2),
            PARTICIPANT, Money.of(1, "EUR"), new EqualShareAllocation([PARTICIPANT]), null,
            VERSION_ID, IDENTITY_RATE, Money.of(1, "EUR"), [new Share(PARTICIPANT, Money.of(1, "EUR"))])
        projector.accept([opened(ID), event(ID, 2, new ParticipantAdded(PARTICIPANT, "Alex")),
            event(ID, 3, recorded), event(ID, 4, new ExpenseCancelled(expenseId))])
        def before = projector.findById(ID).orElseThrow()

        when: "a later batch renames the Settlement before repeating the cancellation"
        projector.accept([renamed(ID, 5, "Weekend"), event(ID, 6, new ExpenseCancelled(expenseId))])

        then: "the batch is rejected atomically"
        thrown(IllegalStateException)
        projector.findById(ID).orElseThrow() == before
    }

    private static SettlementView emptySettlementView(SettlementId id, String name, long version) {
        new SettlementView(id, name, EUR, version, [], [], [], [], [:], [])
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
