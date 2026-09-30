package com.github.monaboiste.fairshare.settlement.domain.model

import com.github.monaboiste.fairshare.common.events.EventId
import com.github.monaboiste.fairshare.common.events.PendingEvent
import com.github.monaboiste.fairshare.common.events.inmemory.InMemoryEventStore
import com.github.monaboiste.fairshare.quantity.money.Money
import com.github.monaboiste.fairshare.settlement.domain.EqualShareAllocation
import com.github.monaboiste.fairshare.settlement.domain.ExpenseDescription
import com.github.monaboiste.fairshare.settlement.domain.ExpenseDetails
import com.github.monaboiste.fairshare.settlement.domain.ExpenseId
import com.github.monaboiste.fairshare.settlement.domain.ExpenseIdentifierConflict
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId
import com.github.monaboiste.fairshare.settlement.domain.ParticipantIdentifierConflict
import com.github.monaboiste.fairshare.settlement.domain.ParticipantName
import com.github.monaboiste.fairshare.settlement.domain.ParticipantNotFound
import com.github.monaboiste.fairshare.settlement.domain.ParticipantReferenced
import com.github.monaboiste.fairshare.settlement.domain.RepaymentDetails
import com.github.monaboiste.fairshare.settlement.domain.RepaymentId
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.SettlementName
import com.github.monaboiste.fairshare.settlement.domain.event.ExpenseCancelled
import com.github.monaboiste.fairshare.settlement.domain.event.ExpenseRecorded
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantAdded
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRemoved
import com.github.monaboiste.fairshare.settlement.domain.event.ParticipantRenamed
import com.github.monaboiste.fairshare.settlement.domain.event.RepaymentCancelled
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementRenamed
import com.github.monaboiste.fairshare.settlement.infrastructure.EventSourcedSettlementRepository
import com.github.monaboiste.fairshare.valuation.ValuationEngine
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.money.Monetary
import spock.lang.Specification

class SettlementSpec extends Specification {
    private static final SettlementId ID = new SettlementId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
    private static final ParticipantId PARTICIPANT = new ParticipantId(UUID.fromString("00000000-0000-0000-0000-000000000011"))
    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z")
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC)
    private static final EventId OPENED_ID = new EventId(UUID.fromString("00000000-0000-0000-0000-00000000000a"))
    private static final EventId RENAMED_ID = new EventId(UUID.fromString("00000000-0000-0000-0000-00000000000b"))
    private static final EUR = Monetary.getCurrency("EUR")
    private static final USD = Monetary.getCurrency("USD")

    def "opening and renaming register only changed facts"() {
        given: "a Settlement called Holiday is opened in euros"
        def settlement = Settlement.open(new SettlementName("Holiday"), EUR, CLOCK)

        when: "it is renamed to Mountains twice"
        settlement.rename(new SettlementName("Mountains"))
        settlement.rename(new SettlementName("Mountains"))

        then: "only the opening and a single rename are remembered, both stamped with the current time"
        settlement.pendingEvents()*.payload() == [new SettlementOpened("Holiday", EUR),
            new SettlementRenamed("Mountains")]
        settlement.pendingEvents().first().payload().currency() == EUR
        settlement.pendingEvents()*.occurredAt() == [NOW, NOW]
        settlement.pendingEvents().every { it.eventId() != null }
        settlement.pendingEvents().first().eventId() != settlement.pendingEvents().last().eventId()
        settlement.version() == 2
        settlement.committedVersion() == 0
    }

    def "opening Settlements generates distinct identifiers"() {
        when: "two Settlements with the same name are opened"
        def first = Settlement.open(new SettlementName("Holiday"), EUR, CLOCK)
        def second = Settlement.open(new SettlementName("Holiday"), EUR, CLOCK)

        then: "each one gets its own distinct identity"
        first.id() != null
        second.id() != null
        first.id() != second.id()
        first.id().value() != null
    }

    def "repository recreates Settlement with committed history"() {
        given: "a Settlement that was opened, renamed and saved"
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, CLOCK)
        def settlement = Settlement.open(new SettlementName("Holiday"), EUR, CLOCK)
        settlement.rename(new SettlementName("Mountains"))
        repository.save(settlement)

        when: "it is loaded again from its saved history"
        def replayed = repository.findById(settlement.id()).orElseThrow()

        then: "it knows its history and has nothing new waiting to be saved"
        replayed.pendingEvents().empty
        replayed.version() == 2
        replayed.committedVersion() == 2

        when: "it is renamed to the name it already has"
        replayed.rename(new SettlementName("Mountains"))

        then: "nothing new is recorded"
        replayed.pendingEvents().empty
    }

    def "a non-opening first event is rejected on replay"() {
        given: "a saved history that starts with a rename instead of an opening"
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, CLOCK)
        store.append(ID, 0, [pending(RENAMED_ID, new SettlementRenamed("Wrong"))])

        when: "the Settlement is loaded"
        repository.findById(ID)

        then: "loading fails because the history is broken"
        thrown(IllegalStateException)
    }

    def "a second opening event is rejected on replay"() {
        given: "a saved history in which the Settlement is opened twice"
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, CLOCK)
        store.append(ID, 0, [pending(OPENED_ID, new SettlementOpened("Holiday", EUR)),
            pending(RENAMED_ID, new SettlementOpened("Again", USD))])

        when: "the Settlement is loaded"
        repository.findById(ID)

        then: "loading fails because the history is broken"
        thrown(IllegalStateException)
    }

    def "historical names are replayed without applying current input validation"() {
        given: "a saved history with a blank name that today's rules would not allow"
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, CLOCK)
        store.append(ID, 0, [pending(OPENED_ID, new SettlementOpened("", EUR))])

        when: "the Settlement is loaded"
        def settlement = repository.findById(ID).orElseThrow()

        then: "the old name is accepted as it was"
        settlement.version() == 1

        when: "it is renamed with a valid name and saved"
        settlement.rename(new SettlementName("Current"))
        def committed = repository.save(settlement)

        then: "the rename is saved as the next step in its history"
        committed.version() == 2
        repository.findById(ID).orElseThrow().pendingEvents().empty
    }

    def "replay retains original add data after rename and removal"() {
        given: "a Participant who was added as Alex, renamed to Ada and then removed"
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, CLOCK)
        def settlement = Settlement.open(new SettlementName("Holiday"), EUR, CLOCK)
        settlement.addParticipant(PARTICIPANT, new ParticipantName("Alex"))
        settlement.renameParticipant(PARTICIPANT, new ParticipantName("Ada"))
        settlement.removeParticipant(PARTICIPANT)
        repository.save(settlement)

        when: "the Settlement is loaded and the Participant is added again, once as Alex and once as Ada"
        def replayed = repository.findById(settlement.id()).orElseThrow()
        def retry = replayed.addParticipant(PARTICIPANT, new ParticipantName("Alex"))
        def conflict = replayed.addParticipant(PARTICIPANT, new ParticipantName("Ada"))

        then: "repeating the original addition is accepted, changing it is refused, and renaming is refused"
        retry.success()
        conflict.getFailure() == new ParticipantIdentifierConflict(settlement.id(), PARTICIPANT)
        replayed.renameParticipant(PARTICIPANT, new ParticipantName("Again")).getFailure() ==
            new ParticipantNotFound(settlement.id(), PARTICIPANT)
        replayed.pendingEvents().empty
        replayed.version() == 4
    }

    def "historical Participant names replay without current input validation"() {
        given: "a saved history with a blank Participant name that today's rules would not allow"
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, CLOCK)
        store.append(ID, 0, [pending(OPENED_ID, new SettlementOpened("Holiday", EUR)),
            pending(RENAMED_ID, new ParticipantAdded(PARTICIPANT, ""))])

        when: "the Settlement is loaded"
        def replayed = repository.findById(ID).orElseThrow()

        then: "the old name is accepted and the Participant can still be renamed"
        replayed.version() == 2
        replayed.renameParticipant(PARTICIPANT, new ParticipantName("Ada")).success()
    }

    def "invalid Participant event sequences are rejected on replay"() {
        given: "a saved history that changes a Participant who was never added"
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, CLOCK)
        store.append(ID, 0, [pending(OPENED_ID, new SettlementOpened("Holiday", EUR)), pending(RENAMED_ID, invalid)])

        when: "the Settlement is loaded"
        repository.findById(ID)

        then: "loading fails because the history is broken"
        thrown(IllegalStateException)

        where:
        invalid << [new ParticipantRenamed(PARTICIPANT, "Ada"), new ParticipantRemoved(PARTICIPANT)]
    }

    def "a removed Participant cannot be changed or re-added by a later event"() {
        given: "a saved history that changes or re-adds a Participant after they were removed"
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, CLOCK)
        store.append(ID, 0, [pending(OPENED_ID, new SettlementOpened("Holiday", EUR)),
            pending(EventId.random(), new ParticipantAdded(PARTICIPANT, "Alex")),
            pending(EventId.random(), new ParticipantRemoved(PARTICIPANT)),
            pending(RENAMED_ID, laterEvent)])

        when: "the Settlement is loaded"
        repository.findById(ID)

        then: "loading fails because the history is broken"
        thrown(IllegalStateException)

        where:
        laterEvent << [
            new ParticipantRenamed(PARTICIPANT, "Ada"),
            new ParticipantRemoved(PARTICIPANT),
            new ParticipantAdded(PARTICIPANT, "Alex")
        ]
    }

    def "replay reserves Expense identifiers and protects referenced Participants"() {
        given: "a Settlement with a Dinner Expense paid by and shared with one Participant"
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, CLOCK)
        def settlement = Settlement.open(new SettlementName("Holiday"), EUR, CLOCK)
        def expenseId = new ExpenseId(UUID.randomUUID())
        def date = LocalDate.of(2026, 1, 2)
        def allocation = new EqualShareAllocation([PARTICIPANT])
        def expense = { String description, Money amount ->
            new ExpenseDetails(expenseId, new ExpenseDescription(description), date, PARTICIPANT, amount, allocation)
        }
        settlement.addParticipant(PARTICIPANT, new ParticipantName("Ada"))
        def recorded = settlement.recordExpense(expense("Dinner", Money.of(10.0, "EUR")), null,
                ValuationEngine.standard())
        repository.save(settlement)

        when: "the Settlement is loaded and the same Expense is recorded again, once unchanged and once as Lunch"
        def replayed = repository.findById(settlement.id()).orElseThrow()
        def retry = replayed.recordExpense(expense("Dinner", Money.of(10.00, "EUR")), null, ValuationEngine.standard())
        def conflict = replayed.recordExpense(expense("Lunch", Money.of(10, "EUR")), null, ValuationEngine.standard())

        then: "both repeats are refused and the Participant cannot be removed while in use"
        recorded.getSuccess() == expenseId
        retry.getFailure() == new ExpenseIdentifierConflict(settlement.id(), expenseId)
        conflict.getFailure() == new ExpenseIdentifierConflict(settlement.id(), expenseId)
        replayed.removeParticipant(PARTICIPANT).getFailure() == new ParticipantReferenced(settlement.id(), PARTICIPANT)
        replayed.pendingEvents().empty
        replayed.version() == 3
        store.load(settlement.id()).last().payload() instanceof ExpenseRecorded
    }

    def "an invalid #kind cancellation in saved history rejects aggregate replay"() {
        given: "a Settlement with the Participant and a valid opening"
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, CLOCK)
        def settlement = Settlement.open(new SettlementName("Holiday"), EUR, CLOCK)
        settlement.addParticipant(PARTICIPANT, new ParticipantName("Alex"))
        repository.save(settlement)
        def id = settlement.id()
        def entry = cancellation.call()
        store.append(id, 2, [pending(EventId.random(), entry)])

        when: "an entry that was never recorded is cancelled during replay"
        repository.findById(id)

        then: "the broken stream is rejected"
        thrown(IllegalStateException)

        where:
        kind | cancellation
        "Expense" | { -> new ExpenseCancelled(new ExpenseId(new UUID(0, 21))) }
        "Repayment" | { -> new RepaymentCancelled(new RepaymentId(new UUID(0, 31))) }
    }

    def "a repeated #kind cancellation in saved history rejects aggregate replay"() {
        given: "a Settlement with an entry already cancelled"
        def store = new InMemoryEventStore<SettlementId, SettlementEvent>()
        def repository = new EventSourcedSettlementRepository(store, CLOCK)
        def settlement = Settlement.open(new SettlementName("Holiday"), EUR, CLOCK)
        settlement.addParticipant(PARTICIPANT, new ParticipantName("Alex"))
        record.call(settlement)
        cancel.call(settlement)
        repository.save(settlement)
        def id = settlement.id()
        def version = settlement.version()
        store.append(id, version, [pending(EventId.random(), event.call())])

        when: "the duplicate cancellation is replayed"
        repository.findById(id)

        then: "the broken stream is rejected"
        thrown(IllegalStateException)

        where:
        kind | record | cancel | event
        "Expense" | { aggregate -> aggregate.recordExpense(
            new ExpenseDetails(new ExpenseId(new UUID(0, 21)), new ExpenseDescription("Lunch"),
                LocalDate.of(2026, 1, 2), PARTICIPANT, Money.of(1, "EUR"),
                new EqualShareAllocation([PARTICIPANT])), null, ValuationEngine.standard()) } |
            { aggregate -> aggregate.cancelExpense(new ExpenseId(new UUID(0, 21))) } |
            { -> new ExpenseCancelled(new ExpenseId(new UUID(0, 21))) }
        "Repayment" | { aggregate ->
            def other = new ParticipantId(new UUID(0, 12))
            aggregate.addParticipant(other, new ParticipantName("Bob"))
            aggregate.recordRepayment(new RepaymentDetails(new RepaymentId(new UUID(0, 31)),
                LocalDate.of(2026, 1, 2), other, PARTICIPANT, Money.of(1, "EUR"))) } |
            { aggregate -> aggregate.cancelRepayment(new RepaymentId(new UUID(0, 31))) } |
            { -> new RepaymentCancelled(new RepaymentId(new UUID(0, 31))) }
    }

    private static PendingEvent<SettlementEvent> pending(EventId id, SettlementEvent payload) {
        new PendingEvent<SettlementEvent>(id, payload, NOW)
    }
}
