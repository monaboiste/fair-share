package com.github.monaboiste.fairshare.settlement.application

import com.github.monaboiste.fairshare.pricing.component.Validity
import com.github.monaboiste.fairshare.quantity.money.Money
import com.github.monaboiste.fairshare.settlement.application.command.AddParticipant
import com.github.monaboiste.fairshare.settlement.application.command.ConfigureExchangeRate
import com.github.monaboiste.fairshare.settlement.application.command.RecordExpense
import com.github.monaboiste.fairshare.settlement.application.command.RecordRepayment
import com.github.monaboiste.fairshare.settlement.application.command.RemoveParticipant
import com.github.monaboiste.fairshare.settlement.application.command.RenameParticipant
import com.github.monaboiste.fairshare.settlement.application.command.RenameSettlement
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlement
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlementHistory
import com.github.monaboiste.fairshare.settlement.application.query.RepaymentView
import com.github.monaboiste.fairshare.settlement.domain.EqualShareAllocation
import com.github.monaboiste.fairshare.settlement.domain.ExpenseDescription
import com.github.monaboiste.fairshare.settlement.domain.ExpenseId
import com.github.monaboiste.fairshare.settlement.domain.NonPositiveRepaymentAmount
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId
import com.github.monaboiste.fairshare.settlement.domain.ParticipantName
import com.github.monaboiste.fairshare.settlement.domain.ParticipantNotFound
import com.github.monaboiste.fairshare.settlement.domain.ParticipantReferenced
import com.github.monaboiste.fairshare.settlement.domain.RepaymentAmountPrecisionExceeded
import com.github.monaboiste.fairshare.settlement.domain.RepaymentCurrencyMismatch
import com.github.monaboiste.fairshare.settlement.domain.RepaymentId
import com.github.monaboiste.fairshare.settlement.domain.RepaymentIdentifierConflict
import com.github.monaboiste.fairshare.settlement.domain.SelfDirectedRepayment
import com.github.monaboiste.fairshare.settlement.domain.SettlementName
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound
import com.github.monaboiste.fairshare.settlement.domain.event.RepaymentRecorded
import com.github.monaboiste.fairshare.settlement.infrastructure.SettlementProjector
import com.github.monaboiste.fairshare.valuation.ExchangeRate
import java.time.LocalDate
import spock.lang.Specification

class RepaymentsSpec extends Specification {
    private static final ParticipantId ADA = participant(11)
    private static final ParticipantId BOB = participant(12)
    private static final ParticipantId CAL = participant(13)
    private static final ParticipantId UNKNOWN = participant(14)
    private static final RepaymentId REPAYMENT = repayment(21)
    private static final LocalDate DATE = LocalDate.of(2026, 2, 3)
    def configuration = new SettlementTestConfiguration()

    def "actual Repayment records a reverse Obligation without an Expense or proposal"() {
        given: "a Settlement with Ada, Bob and Cal and no outstanding Balances"
        def settlement = withParticipants()
        def command = new RecordRepayment(settlement, REPAYMENT, DATE, BOB, ADA, Money.of(15, "EUR"))

        when: "Bob records an actual transfer to Ada and the view is rebuilt from history"
        def commit = configuration.commands.dispatch(command).getSuccess()
        def view = settlementView(settlement)
        def recorded = (RepaymentRecorded) commit.events().first().payload()
        def rebuilt = new SettlementProjector()
        rebuilt.rebuild(configuration.store)

        then: "the transfer is preserved with the opposite Obligation and zero-sum Balances"
        commit.streamId() == settlement
        commit.events().size() == 1
        recorded.type() == "RepaymentRecorded"
        recorded.schemaVersion() == 1
        recorded.repaymentId() == REPAYMENT
        recorded.paidOn() == DATE
        recorded.payer() == BOB
        recorded.recipient() == ADA
        recorded.amount().compareTo(Money.of(15, "EUR")) == 0
        view.repayments() == [new RepaymentView(REPAYMENT, DATE, BOB, ADA, recorded.amount(), RepaymentView.Status.ACTIVE)]
        view.expenses().isEmpty()
        view.obligations()*.from() == [ADA]
        view.obligations()*.to() == [BOB]
        view.obligations().first().amount().compareTo(Money.of(15, "EUR")) == 0
        view.balances().keySet().toList() == [ADA, BOB, CAL]
        view.balances()[ADA].compareTo(Money.of(-15, "EUR")) == 0
        view.balances()[BOB].compareTo(Money.of(15, "EUR")) == 0
        view.balances()[CAL].isZero()
        view.balances().values().inject(Money.zero("EUR")) { sum, balance -> sum.add(balance) }.isZero()
        view.version() == commit.version()
        rebuilt.findById(settlement).orElseThrow() == view
        history(settlement).last().payload() == recorded
    }

    def "nonpositive actual Repayment rejects without committing: #caseName"() {
        given: "a Settlement with known Participants"
        def settlement = withParticipants()
        def before = settlementView(settlement)
        def previousHistory = history(settlement)

        when: "an actual Repayment with a nonpositive amount is submitted"
        def result = configuration.commands.dispatch(
            new RecordRepayment(settlement, REPAYMENT, DATE, BOB, ADA, amount))

        then: "the rejection identifies the Repayment and leaves history and Balances unchanged"
        result.getFailure() == new NonPositiveRepaymentAmount(settlement, REPAYMENT)
        settlementView(settlement) == before
        history(settlement) == previousHistory

        where:
        caseName   | amount
        "zero"     | Money.zero("EUR")
        "negative" | Money.of(-1, "EUR")
    }

    def "actual Repayment amount must use the Settlement Currency and its minor units: #caseName"() {
        given: "a Settlement with known Participants and the specified currency"
        def settlement = withParticipants(currency)
        def before = settlementView(settlement)
        def previousHistory = history(settlement)

        when: "a Repayment with foreign currency or excess precision is submitted"
        def result = configuration.commands.dispatch(
            new RecordRepayment(settlement, REPAYMENT, DATE, BOB, ADA, amount))

        then: "a typed rejection is returned without committing"
        result.failure()
        result.getFailure().class.simpleName == rejection
        result.getFailure().settlementId() == settlement
        result.getFailure().repaymentId() == REPAYMENT
        settlementView(settlement) == before
        history(settlement) == previousHistory

        where:
        caseName           | currency | amount                    | rejection
        "foreign currency" | "EUR"    | Money.of(1, "USD")        | "RepaymentCurrencyMismatch"
        "fractional cent"  | "EUR"    | Money.of(10.005, "EUR")   | "RepaymentAmountPrecisionExceeded"
        "fractional yen"   | "JPY"    | Money.of(0.1, "JPY")      | "RepaymentAmountPrecisionExceeded"
        "fractional fils"  | "KWD"    | Money.of(0.0001, "KWD")   | "RepaymentAmountPrecisionExceeded"
    }

    def "actual Repayment requires distinct active Participants: #caseName"() {
        given: "a Settlement with Participants, optionally with one removed"
        def settlement = withParticipants()
        if (removed != null) configuration.commands.dispatch(new RemoveParticipant(settlement, removed))
        def before = settlementView(settlement)
        def previousHistory = history(settlement)

        when: "an actual Repayment with invalid Participants is submitted"
        def result = configuration.commands.dispatch(
            new RecordRepayment(settlement, REPAYMENT, DATE, payer, recipient, Money.of(1, "EUR")))

        then: "a typed rejection is returned without committing"
        result.failure()
        result.getFailure().class.simpleName == rejection
        result.getFailure().settlementId() == settlement
        if (missing != null) {
            assert result.getFailure().participantId() == missing
        } else {
            assert result.getFailure().repaymentId() == REPAYMENT
        }
        settlementView(settlement) == before
        history(settlement) == previousHistory

        where:
        caseName            | payer   | recipient | removed | missing | rejection
        "self-directed"     | ADA     | ADA       | null    | null    | "SelfDirectedRepayment"
        "unknown payer"     | UNKNOWN | ADA       | null    | UNKNOWN | "ParticipantNotFound"
        "unknown recipient" | BOB     | UNKNOWN   | null    | UNKNOWN | "ParticipantNotFound"
        "removed payer"     | BOB     | ADA       | BOB     | BOB     | "ParticipantNotFound"
        "removed recipient" | BOB     | ADA       | ADA     | ADA     | "ParticipantNotFound"
    }

    def "every reused Repayment identifier rejects: #caseName"() {
        given: "Bob has recorded a Repayment to Ada"
        def settlement = withParticipants()
        configuration.commands.dispatch(
            new RecordRepayment(settlement, REPAYMENT, DATE, BOB, ADA, Money.of(15, "EUR")))
        def before = settlementView(settlement)
        def previousHistory = history(settlement)

        when: "the same identifier is submitted again"
        def result = configuration.commands.dispatch(
            new RecordRepayment(settlement, REPAYMENT, date, payer, recipient, amount))

        then: "identifier reuse is rejected even when the submission is identical or otherwise invalid"
        result.failure()
        result.getFailure().class.simpleName == "RepaymentIdentifierConflict"
        result.getFailure().settlementId() == settlement
        result.getFailure().repaymentId() == REPAYMENT
        settlementView(settlement) == before
        history(settlement) == previousHistory

        where:
        caseName          | date             | payer   | recipient | amount
        "identical"       | DATE             | BOB     | ADA       | Money.of(15, "EUR")
        "numeric equal"   | DATE             | BOB     | ADA       | Money.of(new BigDecimal("15.00"), "EUR")
        "changed date"    | DATE.plusDays(1) | BOB     | ADA       | Money.of(15, "EUR")
        "changed payer"   | DATE             | CAL     | ADA       | Money.of(15, "EUR")
        "changed recipient" | DATE           | BOB     | CAL       | Money.of(15, "EUR")
        "changed amount"  | DATE             | BOB     | ADA       | Money.of(16, "EUR")
        "invalid fields"  | DATE             | UNKNOWN | UNKNOWN   | Money.zero("USD")
    }

    def "a Repayment's #role cannot be removed even after Balances return to zero"() {
        given: "two actual Repayments in opposite directions leave all Balances zero"
        def settlement = withParticipants()
        configuration.commands.dispatch(
            new RecordRepayment(settlement, REPAYMENT, DATE, BOB, ADA, Money.of(15, "EUR")))
        configuration.commands.dispatch(
            new RecordRepayment(settlement, repayment(22), DATE, ADA, BOB, Money.of(15, "EUR")))
        def before = settlementView(settlement)
        def previousHistory = history(settlement)

        when: "a referenced Participant is removed"
        def result = configuration.commands.dispatch(new RemoveParticipant(settlement, referenced))

        then: "the financial reference prevents removal without committing"
        before.balances().values().every { it.isZero() }
        result.getFailure() == new ParticipantReferenced(settlement, referenced)
        settlementView(settlement) == before
        history(settlement) == previousHistory

        where:
        role        | referenced
        "payer"     | BOB
        "recipient" | ADA
    }

    def "actual Repayment changes Expense-derived Balances: #caseName"() {
        given: "Ada paid a 20 euro Expense shared equally with Bob, leaving Bob owing Ada 10 euros"
        def settlement = withParticipants()
        configuration.commands.dispatch(new RecordExpense(settlement, new ExpenseId(REPAYMENT.value()),
            new ExpenseDescription("Lunch"), DATE, ADA, Money.of(20, "EUR"), new EqualShareAllocation([ADA, BOB])))
        def before = settlementView(settlement)

        when: "an actual Repayment is recorded, using an identifier independent of the Expense identifier"
        def commit = configuration.commands.dispatch(
            new RecordRepayment(settlement, REPAYMENT, DATE, payer, recipient, Money.of(amount, "EUR"))).getSuccess()
        def view = settlementView(settlement)
        def rebuilt = new SettlementProjector()
        rebuilt.rebuild(configuration.store)

        then: "the worked-example Balances and zero-sum invariant hold without altering the Expense"
        before.balances()[ADA] == Money.of(10, "EUR")
        before.balances()[BOB] == Money.of(-10, "EUR")
        view.balances()[ADA] == Money.of(adaBalance, "EUR")
        view.balances()[BOB] == Money.of(bobBalance, "EUR")
        view.balances()[CAL].isZero()
        view.balances().values().inject(Money.zero("EUR")) { sum, balance -> sum.add(balance) }.isZero()
        view.expenses() == before.expenses()
        view.obligations().size() == 2
        view.repayments().size() == 1
        view.version() == commit.version()
        rebuilt.findById(settlement).orElseThrow() == view

        where:
        caseName         | payer | recipient | amount | adaBalance | bobBalance
        "partial"        | BOB   | ADA       | 4      | 6          | -6
        "exact"          | BOB   | ADA       | 10     | 0          | 0
        "overpayment"    | BOB   | ADA       | 15     | -5         | 5
        "other direction" | ADA  | BOB       | 4      | 14         | -14
    }

    def "a valid actual Repayment accepts currency minor units and decimal representations: #caseName"() {
        given: "a Settlement in the specified currency"
        def settlement = withParticipants(currency)

        when: "the amount is recorded without rounding"
        def result = configuration.commands.dispatch(
            new RecordRepayment(settlement, REPAYMENT, date, BOB, ADA, amount))
        def view = settlementView(settlement)

        then: "the exact amount and business date are preserved"
        result.success()
        view.repayments().first().amount() == amount
        view.repayments().first().paidOn() == date
        view.balances()[BOB] == amount
        view.balances()[ADA] == amount.negate()
        history(settlement).last().occurredAt() == configuration.NOW

        where:
        caseName       | currency | amount                                    | date
        "one cent"     | "EUR"    | Money.of(0.01, "EUR")                     | DATE
        "trailing zeros" | "EUR"  | Money.of(new BigDecimal("10.0000"), "EUR") | DATE
        "whole yen"    | "JPY"    | Money.of(100, "JPY")                      | DATE
        "one fils"     | "KWD"    | Money.of(0.001, "KWD")                    | DATE
        "past date"    | "EUR"    | Money.of(1, "EUR")                        | LocalDate.of(1970, 1, 1)
    }

    def "a Repayment for an unknown Settlement returns a typed rejection"() {
        when: "an actual Repayment is recorded in a Settlement that does not exist"
        def result = configuration.commands.dispatch(new RecordRepayment(
            configuration.UNKNOWN_ID, REPAYMENT, DATE, BOB, ADA, Money.of(1, "EUR")))

        then: "the Settlement is reported as missing"
        result.getFailure() == new SettlementNotFound(configuration.UNKNOWN_ID)
    }

    def "Repayment validation favors #priority without committing"() {
        given: "a Settlement with known Participants"
        def settlement = withParticipants()
        def before = settlementView(settlement)
        def previousHistory = history(settlement)

        when: "a Repayment with several problems at once is submitted"
        def result = configuration.commands.dispatch(
            new RecordRepayment(settlement, REPAYMENT, DATE, payer, recipient, amount))

        then: "the higher-priority business rejection is returned"
        result.getFailure() == rejection.call(settlement)
        settlementView(settlement) == before
        history(settlement) == previousHistory

        where:
        priority | payer | recipient | amount | rejection
        "positivity over currency" | UNKNOWN | UNKNOWN | Money.zero("USD") |
            { id -> new NonPositiveRepaymentAmount(id, REPAYMENT) }
        "currency over precision" | UNKNOWN | UNKNOWN | Money.of(1.001, "USD") |
            { id -> new RepaymentCurrencyMismatch(id, REPAYMENT) }
        "precision over self-direction" | UNKNOWN | UNKNOWN | Money.of(1.001, "EUR") |
            { id -> new RepaymentAmountPrecisionExceeded(id, REPAYMENT) }
        "self-direction over missing payer" | UNKNOWN | UNKNOWN | Money.of(1, "EUR") |
            { id -> new SelfDirectedRepayment(id, REPAYMENT) }
        "payer over recipient" | UNKNOWN | participant(15) | Money.of(1, "EUR") |
            { id -> new ParticipantNotFound(id, UNKNOWN) }
    }

    def "recorded Repayments remain visible after #changeName"() {
        given: "a Settlement with one actual Repayment"
        def settlement = withParticipants()
        configuration.commands.dispatch(
            new RecordRepayment(settlement, REPAYMENT, DATE, BOB, ADA, Money.of(15, "EUR")))
        def before = settlementView(settlement)

        when: "a later command changes the Settlement"
        def commit = configuration.commands.dispatch(change.call(settlement)).getSuccess()
        def after = settlementView(settlement)
        def rebuilt = new SettlementProjector()
        rebuilt.rebuild(configuration.store)

        then: "the Repayment list is preserved and live and rebuilt views agree"
        after.repayments() == before.repayments()
        after.obligations().first() == before.obligations().first()
        after.balances()[ADA] == Money.of(adaBalance, "EUR")
        after.balances()[BOB] == Money.of(bobBalance, "EUR")
        after.version() == commit.version()
        rebuilt.findById(settlement).orElseThrow() == after

        where:
        changeName | change | adaBalance | bobBalance
        "Settlement rename" | { id -> new RenameSettlement(id, new SettlementName("Weekend")) } | -15 | 15
        "Participant rename" | { id -> new RenameParticipant(id, BOB, new ParticipantName("Robert")) } | -15 | 15
        "Participant addition" | { id -> new AddParticipant(id, participant(15), new ParticipantName("Dex")) } | -15 | 15
        "unreferenced Participant removal" | { id -> new RemoveParticipant(id, CAL) } | -15 | 15
        "Exchange Rate configuration" | { id -> new ConfigureExchangeRate(id,
            ExchangeRate.of(SettlementTestConfiguration.USD, SettlementTestConfiguration.EUR, new BigDecimal("0.9")),
            Validity.always()) } | -15 | 15
        "Expense recording" | { id -> new RecordExpense(id, new ExpenseId(new UUID(0, 31)),
            new ExpenseDescription("Lunch"), DATE, ADA, Money.of(10, "EUR"), new EqualShareAllocation([BOB])) } | -5 | 5
    }

    def "Repayment identifiers are scoped to each Settlement"() {
        given: "two Settlements containing the same Participants"
        def first = withParticipants()
        def second = withParticipants()

        when: "each Settlement records a transfer under the same Repayment identifier"
        def firstResult = configuration.commands.dispatch(
            new RecordRepayment(first, REPAYMENT, DATE, BOB, ADA, Money.of(1, "EUR")))
        def secondResult = configuration.commands.dispatch(
            new RecordRepayment(second, REPAYMENT, DATE, ADA, BOB, Money.of(2, "EUR")))

        then: "both transfers are recorded independently"
        firstResult.success()
        secondResult.success()
        settlementView(first).repayments()*.id() == [REPAYMENT]
        settlementView(second).repayments()*.id() == [REPAYMENT]
        settlementView(first).balances()[BOB] == Money.of(1, "EUR")
        settlementView(second).balances()[BOB] == Money.of(-2, "EUR")
    }

    def "Repayment views are immutable snapshots in recording order"() {
        given: "a snapshot with one Repayment"
        def settlement = withParticipants()
        configuration.commands.dispatch(
            new RecordRepayment(settlement, REPAYMENT, DATE, BOB, ADA, Money.of(15, "EUR")))
        def snapshot = settlementView(settlement)

        when: "a second Repayment with an earlier business date is recorded"
        configuration.commands.dispatch(
            new RecordRepayment(settlement, repayment(22), DATE.minusDays(1), ADA, BOB, Money.of(5, "EUR")))

        then: "the original snapshot is stable and the current list follows recording order"
        snapshot.repayments()*.id() == [REPAYMENT]
        settlementView(settlement).repayments()*.id() == [REPAYMENT, repayment(22)]
        snapshot.balances()[BOB] == Money.of(15, "EUR")
        settlementView(settlement).balances()[BOB] == Money.of(10, "EUR")

        when: "a caller tries to clear the current Repayment list"
        settlementView(settlement).repayments().clear()

        then: "the change is refused"
        thrown(UnsupportedOperationException)
        settlementView(settlement).repayments()*.id() == [REPAYMENT, repayment(22)]
    }

    private def withParticipants(String currency = "EUR") {
        def settlement = configuration.openSettlement("Holiday", javax.money.Monetary.getCurrency(currency))
        [ADA, BOB, CAL].each { id ->
            configuration.commands.dispatch(new AddParticipant(settlement, id, new ParticipantName("Participant ${id.value()}")))
        }
        settlement
    }

    private def settlementView(def settlement) {
        configuration.queries.dispatch(new GetSettlement(settlement)).getSuccess()
    }

    private def history(def settlement) {
        configuration.queries.dispatch(new GetSettlementHistory(settlement)).getSuccess()
    }

    private static ParticipantId participant(int suffix) {
        new ParticipantId(new UUID(0, suffix))
    }

    private static RepaymentId repayment(int suffix) {
        new RepaymentId(new UUID(0, suffix))
    }
}
