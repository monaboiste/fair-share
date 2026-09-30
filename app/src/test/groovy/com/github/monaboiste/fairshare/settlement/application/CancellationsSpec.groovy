package com.github.monaboiste.fairshare.settlement.application

import com.github.monaboiste.fairshare.netting.Obligation
import com.github.monaboiste.fairshare.quantity.money.Money
import com.github.monaboiste.fairshare.settlement.application.command.AddParticipant
import com.github.monaboiste.fairshare.settlement.application.command.CancelExpense
import com.github.monaboiste.fairshare.settlement.application.command.CancelRepayment
import com.github.monaboiste.fairshare.settlement.application.command.RecordExpense
import com.github.monaboiste.fairshare.settlement.application.command.RecordRepayment
import com.github.monaboiste.fairshare.settlement.application.command.RemoveParticipant
import com.github.monaboiste.fairshare.settlement.application.query.ExpenseView
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlement
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlementHistory
import com.github.monaboiste.fairshare.settlement.application.query.RepaymentView
import com.github.monaboiste.fairshare.settlement.domain.EqualShareAllocation
import com.github.monaboiste.fairshare.settlement.domain.ExpenseAlreadyCancelled
import com.github.monaboiste.fairshare.settlement.domain.ExpenseDescription
import com.github.monaboiste.fairshare.settlement.domain.ExpenseId
import com.github.monaboiste.fairshare.settlement.domain.ExpenseIdentifierConflict
import com.github.monaboiste.fairshare.settlement.domain.ExpenseNotFound
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId
import com.github.monaboiste.fairshare.settlement.domain.ParticipantName
import com.github.monaboiste.fairshare.settlement.domain.ParticipantReferenced
import com.github.monaboiste.fairshare.settlement.domain.RepaymentAlreadyCancelled
import com.github.monaboiste.fairshare.settlement.domain.RepaymentId
import com.github.monaboiste.fairshare.settlement.domain.RepaymentIdentifierConflict
import com.github.monaboiste.fairshare.settlement.domain.RepaymentNotFound
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound
import com.github.monaboiste.fairshare.settlement.domain.event.ExpenseCancelled
import com.github.monaboiste.fairshare.settlement.domain.event.RepaymentCancelled
import com.github.monaboiste.fairshare.settlement.infrastructure.SettlementProjector
import java.time.LocalDate
import spock.lang.Specification

class CancellationsSpec extends Specification {
    private static final ParticipantId ADA = new ParticipantId(new UUID(0, 11))
    private static final ParticipantId BOB = new ParticipantId(new UUID(0, 12))
    private static final ExpenseId ORIGINAL = new ExpenseId(new UUID(0, 21))
    private static final RepaymentId REPAYMENT = new RepaymentId(new UUID(0, 31))
    def configuration = new SettlementTestConfiguration()

    def "cancelling an Expense retains its record and clears its Obligation"() {
        given: "an Expense owed by Bob to Ada"
        def settlement = settlementWithParticipants()
        configuration.commands.dispatch(expense(settlement, ORIGINAL, 10))

        when: "the Expense is cancelled"
        def commit = configuration.commands.dispatch(new CancelExpense(settlement, ORIGINAL)).getSuccess()
        def view = view(settlement)

        then: "a specific event preserves the record but no longer contributes to Balances"
        commit.events()*.payload() == [new ExpenseCancelled(ORIGINAL)]
        view.expenses()*.status() == [ExpenseView.Status.CANCELLED]
        view.obligations().empty
        view.balances()[ADA] == Money.zero("EUR")
        view.balances()[BOB] == Money.zero("EUR")
        history(settlement)*.payload()*.class*.simpleName ==
                ["SettlementOpened", "ParticipantAdded", "ParticipantAdded", "ExpenseRecorded", "ExpenseCancelled"]
    }

    def "cancelling a Repayment retains its transfer and clears its reverse Obligation"() {
        given: "Bob has transferred money to Ada"
        def settlement = settlementWithParticipants()
        configuration.commands.dispatch(repayment(settlement, REPAYMENT, 5))

        when: "the transfer is cancelled"
        def commit = configuration.commands.dispatch(new CancelRepayment(settlement, REPAYMENT)).getSuccess()
        def view = view(settlement)

        then: "the history retains both events and the Balances return to zero"
        commit.events()*.payload() == [new RepaymentCancelled(REPAYMENT)]
        view.repayments()*.status() == [RepaymentView.Status.CANCELLED]
        view.obligations().empty
        view.balances()[ADA] == Money.zero("EUR")
        view.balances()[BOB] == Money.zero("EUR")
        history(settlement)*.payload()*.class*.simpleName.last() == "RepaymentCancelled"
    }

    private static RecordRepayment repayment(def settlement, RepaymentId id, int amount) {
        new RecordRepayment(settlement, id, LocalDate.of(2026, 2, 3), BOB, ADA, Money.of(amount, "EUR"))
    }

    def "unknown or cancelled #kind cannot be cancelled again"() {
        given: "a Settlement with an entry already cancelled"
        def settlement = settlementWithParticipants()
        configuration.commands.dispatch(record.call(settlement))
        configuration.commands.dispatch(cancel.call(settlement, known))
        def before = view(settlement)
        def previousHistory = history(settlement)

        when: "cancellation is attempted for a missing and a cancelled identifier"
        def missing = configuration.commands.dispatch(cancel.call(settlement, unknown))
        def repeated = configuration.commands.dispatch(cancel.call(settlement, known))
        def absentSettlement = configuration.commands.dispatch(cancel.call(configuration.UNKNOWN_ID, known))

        then: "each returns a typed error without changing the view or history"
        missing.getFailure() == notFound.call(settlement, unknown)
        repeated.getFailure() == already.call(settlement, known)
        absentSettlement.getFailure() == new SettlementNotFound(configuration.UNKNOWN_ID)
        view(settlement) == before
        history(settlement) == previousHistory

        where:
        kind        | known     | unknown                          | record | cancel                                          | notFound | already
        "Expense"   | ORIGINAL  | new ExpenseId(new UUID(0, 99))   |
                { id -> expense(id, ORIGINAL, 10) }                         | { id, entry -> new CancelExpense(id, entry) }   |
                { id, entry -> new ExpenseNotFound(id, entry) }                                                                          | { id, entry -> new ExpenseAlreadyCancelled(id, entry) }
        "Repayment" | REPAYMENT | new RepaymentId(new UUID(0, 99)) |
                { id -> repayment(id, REPAYMENT, 5) }                       | { id, entry -> new CancelRepayment(id, entry) } |
                { id, entry -> new RepaymentNotFound(id, entry) }                                                                        | { id, entry -> new RepaymentAlreadyCancelled(id, entry) }
    }

    def "cancelled Expense and Repayment require new identifiers for corrections"() {
        given: "Ada and Bob have an Expense and a Repayment"
        def settlement = settlementWithParticipants()
        configuration.commands.dispatch(expense(settlement, ORIGINAL, 10))
        configuration.commands.dispatch(repayment(settlement, REPAYMENT, 4))
        def old = view(settlement)

        when: "both are cancelled and corrections are recorded with fresh identifiers"
        configuration.commands.dispatch(new CancelExpense(settlement, ORIGINAL))
        configuration.commands.dispatch(new CancelRepayment(settlement, REPAYMENT))
        def cleared = view(settlement)
        def expenseConflict = configuration.commands.dispatch(expense(settlement, ORIGINAL, 10))
        def repaymentConflict = configuration.commands.dispatch(repayment(settlement, REPAYMENT, 4))
        def replacementExpense = new ExpenseId(new UUID(0, 22))
        def replacementRepayment = new RepaymentId(new UUID(0, 32))
        configuration.commands.dispatch(expense(settlement, replacementExpense, 15))
        configuration.commands.dispatch(repayment(settlement, replacementRepayment, 6))
        def corrected = view(settlement)
        def rebuilt = new SettlementProjector()
        rebuilt.rebuild(configuration.store)

        then: "the original facts survive, only corrections contribute, and replay reproduces the complete history"
        old.expenses()*.status() == [ExpenseView.Status.ACTIVE]
        old.repayments()*.status() == [RepaymentView.Status.ACTIVE]
        cleared.obligations().empty
        cleared.balances().values().every { it.isZero() }
        expenseConflict.getFailure() == new ExpenseIdentifierConflict(settlement, ORIGINAL)
        repaymentConflict.getFailure() == new RepaymentIdentifierConflict(settlement, REPAYMENT)
        corrected.expenses()*.id() == [ORIGINAL, replacementExpense]
        corrected.expenses()*.status() == [ExpenseView.Status.CANCELLED, ExpenseView.Status.ACTIVE]
        corrected.repayments()*.id() == [REPAYMENT, replacementRepayment]
        corrected.repayments()*.status() == [RepaymentView.Status.CANCELLED, RepaymentView.Status.ACTIVE]
        corrected.obligations()*.amount() == [Money.of(15, "EUR"), Money.of(6, "EUR")]
        corrected.balances()[ADA] == Money.of(9, "EUR")
        corrected.balances()[BOB] == Money.of(-9, "EUR")
        rebuilt.findById(settlement).orElseThrow() == corrected
        history(settlement)*.payload()*.class*.simpleName == ["SettlementOpened", "ParticipantAdded",
                                                              "ParticipantAdded", "ExpenseRecorded", "RepaymentRecorded", "ExpenseCancelled", "RepaymentCancelled",
                                                              "ExpenseRecorded", "RepaymentRecorded"]
        configuration.commands.dispatch(new RemoveParticipant(settlement, BOB)).getFailure() ==
                new ParticipantReferenced(settlement, BOB)
    }

    def "cancelled entries still keep their Participants in the Settlement"() {
        given: "a recorded Expense and Repayment referencing Ada and Bob"
        def settlement = settlementWithParticipants()
        configuration.commands.dispatch(expense(settlement, ORIGINAL, 10))
        configuration.commands.dispatch(repayment(settlement, REPAYMENT, 4))
        configuration.commands.dispatch(new CancelExpense(settlement, ORIGINAL))
        configuration.commands.dispatch(new CancelRepayment(settlement, REPAYMENT))
        def before = history(settlement)

        when: "both Participants are removed despite zero Balances"
        def ada = configuration.commands.dispatch(new RemoveParticipant(settlement, ADA))
        def bob = configuration.commands.dispatch(new RemoveParticipant(settlement, BOB))

        then: "historical references prohibit both removals without appending"
        ada.getFailure() == new ParticipantReferenced(settlement, ADA)
        bob.getFailure() == new ParticipantReferenced(settlement, BOB)
        history(settlement) == before
        view(settlement).balances().values().every { it.isZero() }
    }

    def "cancelling parallel entries leaves independent obligations intact"() {
        given: "two identical Expenses and two equal Repayments with independent identifiers"
        def settlement = settlementWithParticipants()
        def otherExpense = new ExpenseId(new UUID(0, 22))
        def otherRepayment = new RepaymentId(new UUID(0, 32))
        configuration.commands.dispatch(expense(settlement, ORIGINAL, 10))
        configuration.commands.dispatch(expense(settlement, otherExpense, 10))
        configuration.commands.dispatch(repayment(settlement, REPAYMENT, 4))
        configuration.commands.dispatch(repayment(settlement, otherRepayment, 4))

        when: "one of each is cancelled"
        configuration.commands.dispatch(new CancelExpense(settlement, ORIGINAL))
        configuration.commands.dispatch(new CancelRepayment(settlement, REPAYMENT))
        def result = view(settlement)

        then: "the remaining entries alone determine Balances"
        result.obligations()*.amount() == [Money.of(10, "EUR"), Money.of(4, "EUR")]
        result.balances()[ADA] == Money.of(6, "EUR")
        result.balances()[BOB] == Money.of(-6, "EUR")
        result.expenses()*.status() == [ExpenseView.Status.CANCELLED, ExpenseView.Status.ACTIVE]
        result.repayments()*.status() == [RepaymentView.Status.CANCELLED, RepaymentView.Status.ACTIVE]
    }

    def "cancelling an Expense with a payer Share leaves an equal Repayment contribution"() {
        given: "Ada shares an Expense with Bob and transfers him the amount of his Share"
        def settlement = settlementWithParticipants()
        configuration.commands.dispatch(new RecordExpense(settlement, ORIGINAL, new ExpenseDescription("Lunch"),
                LocalDate.of(2026, 2, 3), ADA, Money.of(20, "EUR"), new EqualShareAllocation([ADA, BOB])))
        configuration.commands.dispatch(new RecordRepayment(settlement, REPAYMENT, LocalDate.of(2026, 2, 3),
                ADA, BOB, Money.of(10, "EUR")))
        def equalContribution = new Obligation(BOB, ADA, Money.of(10, "EUR"))
        def before = view(settlement)

        when: "the Expense is cancelled while the Repayment remains active"
        configuration.commands.dispatch(new CancelExpense(settlement, ORIGINAL)).getSuccess()
        def current = view(settlement)
        def rebuilt = new SettlementProjector()
        rebuilt.rebuild(configuration.store)

        then: "only the Repayment remains, with its original direction and ordered history"
        before.obligations() == [equalContribution, equalContribution]
        before.balances()[ADA] == Money.of(20, "EUR")
        before.balances()[BOB] == Money.of(-20, "EUR")
        current.obligations() == [equalContribution]
        current.expenses()*.status() == [ExpenseView.Status.CANCELLED]
        current.repayments()*.status() == [RepaymentView.Status.ACTIVE]
        current.balances()[ADA] == Money.of(10, "EUR")
        current.balances()[BOB] == Money.of(-10, "EUR")
        history(settlement)*.payload()*.class*.simpleName == ["SettlementOpened", "ParticipantAdded",
                                                              "ParticipantAdded", "ExpenseRecorded", "RepaymentRecorded", "ExpenseCancelled"]
        rebuilt.findById(settlement).orElseThrow() == current
    }

    def "cancelling a Repayment leaves an equal Expense contribution with a payer Share"() {
        given: "Ada transfers Bob before sharing an Expense equally with him"
        def settlement = settlementWithParticipants()
        configuration.commands.dispatch(new RecordRepayment(settlement, REPAYMENT, LocalDate.of(2026, 2, 3),
                ADA, BOB, Money.of(10, "EUR")))
        configuration.commands.dispatch(new RecordExpense(settlement, ORIGINAL, new ExpenseDescription("Lunch"),
                LocalDate.of(2026, 2, 3), ADA, Money.of(20, "EUR"), new EqualShareAllocation([ADA, BOB])))
        def equalContribution = new Obligation(BOB, ADA, Money.of(10, "EUR"))
        def before = view(settlement)

        when: "the Repayment is cancelled while the Expense remains active"
        configuration.commands.dispatch(new CancelRepayment(settlement, REPAYMENT)).getSuccess()
        def current = view(settlement)
        def rebuilt = new SettlementProjector()
        rebuilt.rebuild(configuration.store)

        then: "only Bob's Expense Share remains, with both entries and history intact"
        before.obligations() == [equalContribution, equalContribution]
        before.balances()[ADA] == Money.of(20, "EUR")
        before.balances()[BOB] == Money.of(-20, "EUR")
        current.obligations() == [equalContribution]
        current.expenses()*.status() == [ExpenseView.Status.ACTIVE]
        current.repayments()*.status() == [RepaymentView.Status.CANCELLED]
        current.balances()[ADA] == Money.of(10, "EUR")
        current.balances()[BOB] == Money.of(-10, "EUR")
        history(settlement)*.payload()*.class*.simpleName == ["SettlementOpened", "ParticipantAdded",
                                                              "ParticipantAdded", "RepaymentRecorded", "ExpenseRecorded", "RepaymentCancelled"]
        rebuilt.findById(settlement).orElseThrow() == current
    }

    def "cancelling a later Expense removes only its distinct contribution"() {
        given: "two Expenses owed by Bob to Ada with different amounts"
        def settlement = settlementWithParticipants()
        def later = new ExpenseId(new UUID(0, 22))
        configuration.commands.dispatch(expense(settlement, ORIGINAL, 10))
        configuration.commands.dispatch(expense(settlement, later, 7))

        when: "the later Expense is cancelled"
        def commit = configuration.commands.dispatch(new CancelExpense(settlement, later)).getSuccess()
        def current = view(settlement)
        def rebuilt = new SettlementProjector()
        rebuilt.rebuild(configuration.store)

        then: "only the first Expense contributes, with both records and ordered events preserved"
        commit.events()*.payload() == [new ExpenseCancelled(later)]
        current.expenses()*.id() == [ORIGINAL, later]
        current.expenses()*.status() == [ExpenseView.Status.ACTIVE, ExpenseView.Status.CANCELLED]
        current.obligations()*.amount() == [Money.of(10, "EUR")]
        current.balances()[ADA] == Money.of(10, "EUR")
        current.balances()[BOB] == Money.of(-10, "EUR")
        history(settlement)*.payload()*.class*.simpleName == ["SettlementOpened", "ParticipantAdded",
                                                              "ParticipantAdded", "ExpenseRecorded", "ExpenseRecorded", "ExpenseCancelled"]
        history(settlement).last().payload() == new ExpenseCancelled(later)
        rebuilt.findById(settlement).orElseThrow() == current
    }

    def "cancelling a later Repayment removes only its distinct contribution"() {
        given: "two Repayments from Bob to Ada with different amounts"
        def settlement = settlementWithParticipants()
        def later = new RepaymentId(new UUID(0, 32))
        configuration.commands.dispatch(repayment(settlement, REPAYMENT, 4))
        configuration.commands.dispatch(repayment(settlement, later, 3))

        when: "the later Repayment is cancelled"
        def commit = configuration.commands.dispatch(new CancelRepayment(settlement, later)).getSuccess()
        def current = view(settlement)
        def rebuilt = new SettlementProjector()
        rebuilt.rebuild(configuration.store)

        then: "only the first Repayment contributes, with both transfers and ordered events preserved"
        commit.events()*.payload() == [new RepaymentCancelled(later)]
        current.repayments()*.id() == [REPAYMENT, later]
        current.repayments()*.status() == [RepaymentView.Status.ACTIVE, RepaymentView.Status.CANCELLED]
        current.obligations()*.amount() == [Money.of(4, "EUR")]
        current.balances()[ADA] == Money.of(-4, "EUR")
        current.balances()[BOB] == Money.of(4, "EUR")
        history(settlement)*.payload()*.class*.simpleName == ["SettlementOpened", "ParticipantAdded",
                                                              "ParticipantAdded", "RepaymentRecorded", "RepaymentRecorded", "RepaymentCancelled"]
        history(settlement).last().payload() == new RepaymentCancelled(later)
        rebuilt.findById(settlement).orElseThrow() == current
    }

    private def settlementWithParticipants() {
        def id = configuration.openSettlement("Holiday")
        [ADA, BOB].each { participant ->
            configuration.commands.dispatch(new AddParticipant(id, participant, new ParticipantName(participant.toString())))
        }
        id
    }

    private static RecordExpense expense(def settlement, ExpenseId id, int amount) {
        new RecordExpense(settlement, id, new ExpenseDescription("Lunch"), LocalDate.of(2026, 2, 3), ADA,
                Money.of(amount, "EUR"), new EqualShareAllocation([BOB]))
    }

    private def view(def settlement) {
        configuration.queries.dispatch(new GetSettlement(settlement)).getSuccess()
    }

    private def history(def settlement) {
        configuration.queries.dispatch(new GetSettlementHistory(settlement)).getSuccess()
    }
}
