package com.github.monaboiste.fairshare.settlement.application

import com.github.monaboiste.fairshare.common.events.PendingEvent
import com.github.monaboiste.fairshare.pricing.component.Validity
import com.github.monaboiste.fairshare.quantity.money.Money
import com.github.monaboiste.fairshare.settlement.application.command.AddParticipant
import com.github.monaboiste.fairshare.settlement.application.command.CancelExpense
import com.github.monaboiste.fairshare.settlement.application.command.CancelRepayment
import com.github.monaboiste.fairshare.settlement.application.command.CloseSettlement
import com.github.monaboiste.fairshare.settlement.application.command.ConfigureExchangeRate
import com.github.monaboiste.fairshare.settlement.application.command.RecordExpense
import com.github.monaboiste.fairshare.settlement.application.command.RecordRepayment
import com.github.monaboiste.fairshare.settlement.application.command.RemoveParticipant
import com.github.monaboiste.fairshare.settlement.application.command.RenameParticipant
import com.github.monaboiste.fairshare.settlement.application.command.RenameSettlement
import com.github.monaboiste.fairshare.settlement.application.command.ReopenSettlement
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlement
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlementHistory
import com.github.monaboiste.fairshare.settlement.domain.EqualShareAllocation
import com.github.monaboiste.fairshare.settlement.domain.ExpenseDescription
import com.github.monaboiste.fairshare.settlement.domain.ExpenseId
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId
import com.github.monaboiste.fairshare.settlement.domain.ParticipantName
import com.github.monaboiste.fairshare.settlement.domain.RepaymentId
import com.github.monaboiste.fairshare.settlement.domain.SettlementAlreadyOpen
import com.github.monaboiste.fairshare.settlement.domain.SettlementIsClosed
import com.github.monaboiste.fairshare.settlement.domain.SettlementName
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotSettled
import com.github.monaboiste.fairshare.settlement.domain.SettlementStatus
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementClosed
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementOpened
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementReopened
import com.github.monaboiste.fairshare.valuation.ExchangeRate
import java.time.LocalDate
import java.time.LocalDateTime
import javax.money.Monetary
import spock.lang.Specification

class SettlementLifecycleSpec extends Specification {
    private static final ParticipantId ADA = new ParticipantId(new UUID(0, 11))
    private static final ParticipantId BOB = new ParticipantId(new UUID(0, 12))
    private static final ExpenseId EXPENSE = new ExpenseId(new UUID(0, 21))
    private static final RepaymentId REPAYMENT = new RepaymentId(new UUID(0, 31))
    private static final LocalDate DATE = LocalDate.of(2026, 2, 3)
    def configuration = new SettlementTestConfiguration()

    def "an empty Settlement can close without altering its financial state"() {
        given: "an Open Settlement with no Participants or financial entries"
        def settlement = configuration.openSettlement("Holiday")
        def opened = view(settlement)

        when: "the organizer closes the empty Settlement"
        def result = configuration.commands.dispatch(new CloseSettlement(settlement))

        then: "closing records its own event and preserves the empty Balances"
        opened.status() == SettlementStatus.OPEN
        result.success()
        result.getSuccess().version() == 2
        result.getSuccess().events()*.payload() == [new SettlementClosed()]
        result.getSuccess().events()*.occurredAt() == [configuration.NOW]
        view(settlement).status() == SettlementStatus.CLOSED
        view(settlement).balances() == opened.balances()
        history(settlement)*.payload() == [new SettlementOpened("Holiday", configuration.EUR), new SettlementClosed()]
        new SettlementClosed().type() == "SettlementClosed"
        new SettlementClosed().schemaVersion() == 1
    }

    def "one unsettled minor unit prevents closing in #currency"() {
        given: "an Expense leaving each Participant one minor unit from zero"
        def settlement = withParticipants(currency)
        configuration.commands.dispatch(expense(settlement, amount, currency))
        def before = view(settlement)
        def recorded = history(settlement)

        when: "the organizer attempts to close the unsettled Settlement"
        def result = configuration.commands.dispatch(new CloseSettlement(settlement))

        then: "the nonzero Balances prevent closing without changing state or history"
        result.getFailure() == new SettlementNotSettled(settlement)
        before.balances()[ADA] == Money.of(amount, currency)
        before.balances()[BOB] == Money.of(-amount, currency)
        view(settlement) == before
        history(settlement) == recorded

        where:
        currency | amount
        "EUR"    | 0.01
        "JPY"    | 1
        "KWD"    | 0.001
    }

    def "a Settlement closes using only active financial facts: #settledBy"() {
        given: "an Expense settled by a Repayment or excluded through Cancellation"
        def settlement = withParticipants()
        configuration.commands.dispatch(expense(settlement, 10))
        if (settledBy == "Repayment") {
            configuration.commands.dispatch(repayment(settlement, 10))
        } else if (settledBy == "cancellations") {
            configuration.commands.dispatch(repayment(settlement, 3))
            configuration.commands.dispatch(new CancelExpense(settlement, EXPENSE))
            configuration.commands.dispatch(new CancelRepayment(settlement, REPAYMENT))
        } else {
            configuration.commands.dispatch(new CancelExpense(settlement, EXPENSE))
        }
        def before = view(settlement)

        when: "the organizer closes the fully settled Settlement"
        def result = configuration.commands.dispatch(new CloseSettlement(settlement))

        then: "closing records its own event without rewriting financial facts"
        result.success()
        result.getSuccess().events()*.payload() == [new SettlementClosed()]
        view(settlement).status() == SettlementStatus.CLOSED
        view(settlement).balances() == [(ADA): Money.zero("EUR"), (BOB): Money.zero("EUR")]
        view(settlement).expenses() == before.expenses()
        view(settlement).repayments() == before.repayments()
        view(settlement).obligations() == before.obligations()

        where:
        settledBy << ["Repayment", "Expense cancellation", "cancellations"]
    }

    def "a Closed Settlement rejects #change before duplicate, no-op or validation decisions"() {
        given: "a Closed Settlement with settled financial entries and a configured Exchange Rate"
        def settlement = withParticipants()
        configuration.commands.dispatch(expense(settlement, 10))
        configuration.commands.dispatch(repayment(settlement, 10))
        configuration.commands.dispatch(rate(settlement))
        configuration.commands.dispatch(new CloseSettlement(settlement))
        def before = view(settlement)
        def recorded = history(settlement)

        when: "a forbidden change is requested while closed"
        def result = configuration.commands.dispatch(mutation(settlement))

        then: "Closed status takes priority and leaves state and history unchanged"
        result.getFailure() == new SettlementIsClosed(settlement)
        view(settlement) == before
        history(settlement) == recorded

        when: "the same command is dispatched after fresh replay"
        configuration = replayed(settlement)
        def replayedResult = configuration.commands.dispatch(mutation(settlement))

        then: "replayed Closed status enforces the same restriction without new events"
        replayedResult.getFailure() == new SettlementIsClosed(settlement)
        view(settlement) == before
        history(settlement) == recorded

        where:
        change                         | mutation
        "new Participant"              | { new AddParticipant(it, new ParticipantId(new UUID(0, 13)), new ParticipantName("Cal")) }
        "identical Participant retry"  | { new AddParticipant(it, ADA, new ParticipantName("Ada")) }
        "conflicting Participant"      | { new AddParticipant(it, ADA, new ParticipantName("Alex")) }
        "referenced Participant removal" | { new RemoveParticipant(it, ADA) }
        "unknown Participant removal" | { new RemoveParticipant(it, new ParticipantId(new UUID(0, 99))) }
        "identical Exchange Rate"      | { rate(it) }
        "invalid Exchange Rate"        | { new ConfigureExchangeRate(it, new ExchangeRate(Monetary.getCurrency("EUR"), Monetary.getCurrency("EUR"), 1G), validity()) }
        "new Expense"                  | { expense(it, 1, "EUR", new ExpenseId(new UUID(0, 22))) }
        "duplicate Expense"            | { expense(it, 10) }
        "invalid Expense"              | { expense(it, 0, "EUR", new ExpenseId(new UUID(0, 22))) }
        "Expense cancellation"         | { new CancelExpense(it, EXPENSE) }
        "unknown Expense cancellation" | { new CancelExpense(it, new ExpenseId(new UUID(0, 99))) }
        "new Repayment"                | { repayment(it, 1, new RepaymentId(new UUID(0, 32))) }
        "duplicate Repayment"          | { repayment(it, 10) }
        "invalid Repayment"            | { repayment(it, 0, new RepaymentId(new UUID(0, 32))) }
        "Repayment cancellation"       | { new CancelRepayment(it, REPAYMENT) }
        "unknown Repayment cancellation" | { new CancelRepayment(it, new RepaymentId(new UUID(0, 99))) }
        "repeated close"               | { new CloseSettlement(it) }
    }

    def "explicit reopening preserves settled financial facts and records its own event"() {
        given: "a Closed Settlement whose Expense has been fully repaid"
        def settlement = withParticipants()
        configuration.commands.dispatch(expense(settlement, 10))
        configuration.commands.dispatch(repayment(settlement, 10))
        configuration.commands.dispatch(new CloseSettlement(settlement))
        def before = view(settlement)
        def recorded = history(settlement)

        when: "the organizer explicitly reopens the Settlement"
        def result = configuration.commands.dispatch(new ReopenSettlement(settlement))

        then: "reopening records its own event and preserves settled financial facts"
        result.success()
        result.getSuccess().version() == before.version() + 1
        result.getSuccess().events()*.payload() == [new SettlementReopened()]
        result.getSuccess().events()*.occurredAt() == [configuration.NOW]
        view(settlement).status() == SettlementStatus.OPEN
        view(settlement).expenses() == before.expenses()
        view(settlement).repayments() == before.repayments()
        view(settlement).obligations() == before.obligations()
        view(settlement).balances() == before.balances()
        history(settlement).take(recorded.size()) == recorded
        new SettlementReopened().type() == "SettlementReopened"
        new SettlementReopened().schemaVersion() == 1

        when: "the organizer repeats the reopening command"
        def retry = configuration.commands.dispatch(new ReopenSettlement(settlement))

        then: "the already Open Settlement rejects the retry without another event"
        retry.getFailure() == new SettlementAlreadyOpen(settlement)
        history(settlement).size() == recorded.size() + 1
        view(settlement).version() == before.version() + 1
    }

    def "an Open Settlement rejects reopening without a new event"() {
        given: "an Open Settlement and its unchanged history"
        def settlement = withParticipants()
        def before = view(settlement)
        def recorded = history(settlement)

        when: "the organizer requests reopening without first closing"
        def result = configuration.commands.dispatch(new ReopenSettlement(settlement))

        then: "the already Open Settlement rejects reopening without changing state or history"
        result.getFailure() == new SettlementAlreadyOpen(settlement)
        view(settlement) == before
        history(settlement) == recorded
    }

    def "lifecycle transitions reject an unknown Settlement: #transition"() {
        when: "a lifecycle transition is requested for an unknown Settlement"
        def result = configuration.commands.dispatch(command(configuration.UNKNOWN_ID))

        then: "the transition reports the missing Settlement without creating a history"
        result.getFailure() == new SettlementNotFound(configuration.UNKNOWN_ID)
        configuration.queries.dispatch(new GetSettlementHistory(configuration.UNKNOWN_ID)).getFailure() ==
                new SettlementNotFound(configuration.UNKNOWN_ID)

        where:
        transition | command
        "close"    | { new CloseSettlement(it) }
        "reopen"   | { new ReopenSettlement(it) }
    }

    def "Closed Settlement and Participant names can be corrected without changing financial facts"() {
        given: "a Closed Settlement with harmless name corrections to make"
        def settlement = withParticipants()
        configuration.commands.dispatch(expense(settlement, 10))
        configuration.commands.dispatch(repayment(settlement, 10))
        configuration.commands.dispatch(rate(settlement))
        configuration.commands.dispatch(new CloseSettlement(settlement))
        def before = view(settlement)
        def renameSettlement = new RenameSettlement(settlement, new SettlementName("Summer Holiday"))
        def renameParticipant = new RenameParticipant(settlement, ADA, new ParticipantName("Adeline"))

        when: "the organizer corrects the Settlement and Participant names"
        def settlementRename = configuration.commands.dispatch(renameSettlement)
        def participantRename = configuration.commands.dispatch(renameParticipant)

        then: "both corrections are recorded while Closed status and financial facts remain unchanged"
        settlementRename.getSuccess().events().size() == 1
        participantRename.getSuccess().events().size() == 1
        view(settlement).name() == "Summer Holiday"
        view(settlement).participants()*.name() == ["Adeline", "Bob"]
        view(settlement).status() == SettlementStatus.CLOSED
        view(settlement).balances() == before.balances()
        view(settlement).obligations() == before.obligations()
        view(settlement).expenses() == before.expenses()
        view(settlement).repayments() == before.repayments()
        view(settlement).exchangeRates() == before.exchangeRates()

        when: "corrected names and Closed status are replayed"
        def corrected = view(settlement)
        def recorded = history(settlement)
        configuration = replayed(settlement)
        def unchangedSettlement = configuration.commands.dispatch(renameSettlement)
        def unchangedParticipant = configuration.commands.dispatch(renameParticipant)

        then: "replay preserves the corrections and identical renames produce no new events"
        view(settlement) == corrected
        unchangedSettlement.getSuccess().events().empty
        unchangedParticipant.getSuccess().events().empty
        unchangedSettlement.getSuccess().version() == corrected.version()
        unchangedParticipant.getSuccess().version() == corrected.version()
        history(settlement) == recorded

        when: "further harmless corrections are made after replay"
        configuration.commands.dispatch(new RenameSettlement(settlement, new SettlementName("Summer")))
        configuration.commands.dispatch(new RenameParticipant(settlement, ADA, new ParticipantName("Ada")))

        then: "further name corrections remain allowed without reopening or changing Balances"
        view(settlement).name() == "Summer"
        view(settlement).participants()*.name() == ["Ada", "Bob"]
        view(settlement).status() == SettlementStatus.CLOSED
        view(settlement).balances() == before.balances()
    }

    def "reopening restores #change even after fresh replay"() {
        given: "a Closed Settlement explicitly reopened and reconstructed from history"
        def settlement = withParticipants()
        def cal = new ParticipantId(new UUID(0, 13))
        configuration.commands.dispatch(new AddParticipant(settlement, cal, new ParticipantName("Cal")))
        configuration.commands.dispatch(expense(settlement, 10))
        configuration.commands.dispatch(repayment(settlement, 10))
        configuration.commands.dispatch(rate(settlement))
        configuration.commands.dispatch(new CloseSettlement(settlement))
        configuration = replayed(settlement)
        configuration.commands.dispatch(new ReopenSettlement(settlement))
        def reopened = view(settlement)
        def recorded = history(settlement)
        configuration = replayed(settlement)

        when: "a previously forbidden change is requested after reopening"
        def result = configuration.commands.dispatch(mutation(settlement))

        then: "the change succeeds with a new event while retaining the earlier history"
        reopened.status() == SettlementStatus.OPEN
        result.success()
        result.getSuccess().events().size() == 1
        result.getSuccess().version() == reopened.version() + 1
        history(settlement).take(recorded.size()) == recorded
        view(settlement).status() == SettlementStatus.OPEN
        changed(view(settlement))

        where:
        change                 | mutation | changed
        "Participant addition" | { new AddParticipant(it, new ParticipantId(new UUID(0, 14)), new ParticipantName("Dan")) } | { it.participants()*.name() == ["Ada", "Bob", "Cal", "Dan"] }
        "Participant removal"  | { new RemoveParticipant(it, new ParticipantId(new UUID(0, 13))) } | { it.participants()*.name() == ["Ada", "Bob"] }
        "Exchange Rate change" | { new ConfigureExchangeRate(it, new ExchangeRate(Monetary.getCurrency("USD"), Monetary.getCurrency("EUR"), 0.8), validity()) } | { it.exchangeRates()*.exchangeRate()*.value() == [0.9, 0.8] }
        "Expense recording"    | { expense(it, 2, "EUR", new ExpenseId(new UUID(0, 22))) } | { it.expenses().size() == 2 && it.balances()[ADA] == Money.of(2, "EUR") }
        "Expense cancellation" | { new CancelExpense(it, EXPENSE) } | { it.expenses().first().status().name() == "CANCELLED" && it.balances()[ADA] == Money.of(-10, "EUR") }
        "Repayment recording"  | { repayment(it, 2, new RepaymentId(new UUID(0, 32))) } | { it.repayments().size() == 2 && it.balances()[ADA] == Money.of(-2, "EUR") }
        "Repayment cancellation" | { new CancelRepayment(it, REPAYMENT) } | { it.repayments().first().status().name() == "CANCELLED" && it.balances()[ADA] == Money.of(10, "EUR") }
    }

    def "closed cancellations take priority over already cancelled entries"() {
        given: "a Closed Settlement containing an already cancelled Expense and Repayment"
        def settlement = withParticipants()
        configuration.commands.dispatch(expense(settlement, 10))
        configuration.commands.dispatch(repayment(settlement, 3))
        configuration.commands.dispatch(new CancelExpense(settlement, EXPENSE))
        configuration.commands.dispatch(new CancelRepayment(settlement, REPAYMENT))
        configuration.commands.dispatch(new CloseSettlement(settlement))
        def before = view(settlement)
        def recorded = history(settlement)

        when: "the organizer repeats both cancellations while closed"
        def expenseResult = configuration.commands.dispatch(new CancelExpense(settlement, EXPENSE))
        def repaymentResult = configuration.commands.dispatch(new CancelRepayment(settlement, REPAYMENT))

        then: "Closed status takes priority over repeated Cancellation without changing state or history"
        expenseResult.getFailure() == new SettlementIsClosed(settlement)
        repaymentResult.getFailure() == new SettlementIsClosed(settlement)
        view(settlement) == before
        history(settlement) == recorded
    }

    private def replayed(def settlement) {
        def copied = new SettlementTestConfiguration()
        copied.store.append(settlement, 0, history(settlement).collect {
            new PendingEvent<SettlementEvent>(it.eventId(), it.payload(), it.occurredAt())
        })
        copied.projector.rebuild(copied.store)
        copied
    }

    private static def validity() {
        new Validity(LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2027, 1, 1, 0, 0))
    }

    private static def rate(def settlement) {
        new ConfigureExchangeRate(settlement, new ExchangeRate(Monetary.getCurrency("USD"), Monetary.getCurrency("EUR"), 0.9), validity())
    }

    private def withParticipants(String currency = "EUR") {
        def settlement = configuration.openSettlement("Holiday", Monetary.getCurrency(currency))
        configuration.commands.dispatch(new AddParticipant(settlement, ADA, new ParticipantName("Ada")))
        configuration.commands.dispatch(new AddParticipant(settlement, BOB, new ParticipantName("Bob")))
        settlement
    }

    private static def expense(def settlement, def amount, String currency = "EUR", ExpenseId expenseId = EXPENSE) {
        new RecordExpense(settlement, expenseId, new ExpenseDescription("Dinner"), DATE, ADA,
                Money.of(amount, currency), new EqualShareAllocation([BOB]))
    }

    private static def repayment(def settlement, def amount, RepaymentId repaymentId = REPAYMENT) {
        new RecordRepayment(settlement, repaymentId, DATE, BOB, ADA, Money.of(amount, "EUR"))
    }

    private def view(def settlement) {
        configuration.queries.dispatch(new GetSettlement(settlement)).getSuccess()
    }

    private def history(def settlement) {
        configuration.queries.dispatch(new GetSettlementHistory(settlement)).getSuccess()
    }
}
