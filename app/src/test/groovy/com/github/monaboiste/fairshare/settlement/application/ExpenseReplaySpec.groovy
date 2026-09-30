package com.github.monaboiste.fairshare.settlement.application

import com.github.monaboiste.fairshare.common.commands.RegisteredCommandDispatcher
import com.github.monaboiste.fairshare.common.events.EventId
import com.github.monaboiste.fairshare.common.events.PendingEvent
import com.github.monaboiste.fairshare.common.events.inmemory.InMemoryEventStore
import com.github.monaboiste.fairshare.quantity.money.Money
import com.github.monaboiste.fairshare.settlement.application.command.AddParticipant
import com.github.monaboiste.fairshare.settlement.application.command.RecordExpense
import com.github.monaboiste.fairshare.settlement.application.command.RemoveParticipant
import com.github.monaboiste.fairshare.settlement.application.command.handler.RecordExpenseHandler
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlementHistory
import com.github.monaboiste.fairshare.settlement.domain.EqualShareAllocation
import com.github.monaboiste.fairshare.settlement.domain.ExpenseDescription
import com.github.monaboiste.fairshare.settlement.domain.ExpenseId
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId
import com.github.monaboiste.fairshare.settlement.domain.ParticipantName
import com.github.monaboiste.fairshare.settlement.domain.event.ExpenseRecorded
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.infrastructure.EventSourcedSettlementRepository
import com.github.monaboiste.fairshare.settlement.infrastructure.SettlementProjector
import com.github.monaboiste.fairshare.valuation.ValuationEngine
import java.time.LocalDate
import spock.lang.Specification

class ExpenseReplaySpec extends Specification {
    private static final ParticipantId ADA = new ParticipantId(new UUID(0, 11))
    private static final ParticipantId BOB = new ParticipantId(new UUID(0, 12))
    private static final ExpenseId EXPENSE = new ExpenseId(new UUID(0, 21))
    private static final LocalDate DATE = LocalDate.of(2026, 2, 3)
    def configuration = new SettlementTestConfiguration()

    def "Expense replay rejects #recipientState recipients through #route"() {
        given: "a Settlement where the Expense recipient is absent"
        def settlement = configuration.openSettlement("Holiday")
        configuration.commands.dispatch(new AddParticipant(settlement, ADA, new ParticipantName("Ada")))
        if (recipientState == "removed") {
            configuration.commands.dispatch(new AddParticipant(settlement, BOB, new ParticipantName("Bob")))
            configuration.commands.dispatch(new RemoveParticipant(settlement, BOB))
        }

        and: "a copied history with an Expense allocated to that recipient"
        def allocation = new EqualShareAllocation([BOB])
        def valuation = ValuationEngine.standard().identity(Money.of(10, "EUR"))
        def recorded = new ExpenseRecorded(EXPENSE, new ExpenseDescription("Lunch"), DATE, ADA,
                Money.of(10, "EUR"), allocation, null,
                valuation.componentVersion().id(), valuation.exchangeRate(), Money.of(10, "EUR"),
                allocation.resolve(Money.of(10, "EUR")))
        def copied = new InMemoryEventStore()
        def pending = configuration.queries.dispatch(new GetSettlementHistory(settlement)).getSuccess().collect {
            new PendingEvent<SettlementEvent>(it.eventId(), it.payload(), it.occurredAt())
        }
        pending.add(new PendingEvent<SettlementEvent>(EventId.random(), recorded, configuration.NOW))
        copied.append(settlement, 0, pending)
        def rebuilt = new SettlementProjector()
        def commands = RegisteredCommandDispatcher.builder()
                .register(RecordExpense, new RecordExpenseHandler(
                        new EventSourcedSettlementRepository(copied, configuration.CLOCK), configuration.CLOCK)).build()

        when: "the saved history is replayed"
        if (route == "projection") {
            rebuilt.rebuild(copied)
        } else {
            commands.dispatch(new RecordExpense(settlement, new ExpenseId(new UUID(0, 22)),
                    new ExpenseDescription("Dinner"), DATE, ADA, Money.of(1, "EUR"), new EqualShareAllocation([ADA])))
        }

        then: "the inconsistent history is rejected before accepting another Expense"
        thrown(IllegalStateException)
        copied.load(settlement).size() == pending.size()

        where:
        recipientState | route
        "unknown"      | "command"
        "removed"      | "command"
        "unknown"      | "projection"
        "removed"      | "projection"
    }
}
