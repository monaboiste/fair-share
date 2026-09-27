package com.github.monaboiste.fairshare.settlement.application

import com.github.monaboiste.fairshare.quantity.money.Money
import com.github.monaboiste.fairshare.settlement.application.command.AddParticipant
import com.github.monaboiste.fairshare.settlement.application.command.RecordExpense
import com.github.monaboiste.fairshare.settlement.application.command.RemoveParticipant
import com.github.monaboiste.fairshare.settlement.application.query.ExpenseView
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlement
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlementHistory
import com.github.monaboiste.fairshare.settlement.application.query.SettlementView
import com.github.monaboiste.fairshare.settlement.domain.EmptyShareAllocation
import com.github.monaboiste.fairshare.settlement.domain.EqualShareAllocation
import com.github.monaboiste.fairshare.settlement.domain.ExactShareAllocation
import com.github.monaboiste.fairshare.settlement.domain.ExactShareCurrencyMismatch
import com.github.monaboiste.fairshare.settlement.domain.ExactShareSumMismatch
import com.github.monaboiste.fairshare.settlement.domain.ExpenseDescription
import com.github.monaboiste.fairshare.settlement.domain.ExpenseId
import com.github.monaboiste.fairshare.settlement.domain.ExpenseIdentifierConflict
import com.github.monaboiste.fairshare.settlement.domain.MissingExchangeRate
import com.github.monaboiste.fairshare.settlement.domain.NonPositiveExactShare
import com.github.monaboiste.fairshare.settlement.domain.NonPositiveExpenseAmount
import com.github.monaboiste.fairshare.settlement.domain.NonPositiveShareWeight
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId
import com.github.monaboiste.fairshare.settlement.domain.ParticipantName
import com.github.monaboiste.fairshare.settlement.domain.ParticipantNotFound
import com.github.monaboiste.fairshare.settlement.domain.ParticipantReferenced
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound
import com.github.monaboiste.fairshare.settlement.domain.Share
import com.github.monaboiste.fairshare.settlement.domain.WeightedShareAllocation
import com.github.monaboiste.fairshare.settlement.domain.event.ExpenseRecorded
import com.github.monaboiste.fairshare.settlement.infrastructure.SettlementProjector
import java.time.LocalDate
import spock.lang.Specification

class ExpensesSpec extends Specification {
    private static final ParticipantId ADA = participant(11)
    private static final ParticipantId BOB = participant(12)
    private static final ParticipantId CAL = participant(13)
    private static final ParticipantId UNKNOWN = participant(14)
    private static final ParticipantId DEX = participant(15)
    private static final ExpenseId EXPENSE = new ExpenseId(UUID.fromString("00000000-0000-0000-0000-000000000021"))
    private static final LocalDate DATE = LocalDate.of(2026, 2, 3)
    def configuration = new SettlementTestConfiguration()

    def "recorded Expense freezes original amount, valuation, rate and shares"() {
        given:
        def settlement = withParticipants()
        def command = expense(settlement, ADA, Money.of(new BigDecimal("10.005"), "EUR"), [CAL, BOB, ADA])

        when:
        def commit = configuration.commands.dispatch(command).getSuccess()
        def view = configuration.queries.dispatch(new GetSettlement(settlement)).getSuccess()
        def recorded = (ExpenseRecorded) commit.events().get(0).payload()
        def rebuilt = new SettlementProjector()
        rebuilt.rebuild(configuration.store)

        then:
        recorded.type() == "ExpenseRecorded"
        recorded.schemaVersion() == 1
        recorded.expenseId() == EXPENSE
        recorded.description() == new ExpenseDescription("Lunch")
        recorded.incurredOn() == DATE
        recorded.payer() == ADA
        recorded.originalAmount().value() == new BigDecimal("10.005")
        recorded.allocation() == command.allocation()
        recorded.exchangeRate().value() == BigDecimal.ONE
        recorded.componentVersionId() != null
        recorded.valuation().compareTo(Money.of(10.01, "EUR")) == 0
        recorded.shares()*.participantId() == [CAL, BOB, ADA]
        recorded.shares()*.amount()*.value() == [3.33, 3.34, 3.34]*.toBigDecimal()
        view.expenses().get(0).status() == ExpenseView.Status.ACTIVE
        view.expenses().get(0).componentVersionId() == recorded.componentVersionId()
        view.obligations()*.from() == [CAL, BOB]
        view.obligations()*.to() == [ADA, ADA]
        view.balances().keySet().toList() == [ADA, BOB, CAL]
        view.balances()[ADA].compareTo(Money.of(6.67, "EUR")) == 0
        view.balances()[BOB].compareTo(Money.of(-3.34, "EUR")) == 0
        view.balances()[CAL].compareTo(Money.of(-3.33, "EUR")) == 0
        view.balances().values().inject(Money.zero("EUR")) { sum, balance -> sum.add(balance) }.isZero()
        rebuilt.findById(settlement).orElseThrow() == view
        configuration.queries.dispatch(new GetSettlementHistory(settlement)).getSuccess().last().payload() == recorded
    }

    def "exact and weighted Expenses persist Shares visible in the settlement view"() {
        given:
        def settlement = withParticipants()
        def allocation = new ExactShareAllocation(new LinkedHashMap<ParticipantId, Money>([
            (CAL): Money.of(5, "EUR"), (BOB): Money.of(5.005, "EUR")
        ]))

        when:
        def recorded = (ExpenseRecorded) configuration.commands.dispatch(
            expenseWithAllocation(settlement, ADA, Money.of(10.005, "EUR"), allocation))
            .getSuccess().events().first().payload()
        def view = configuration.queries.dispatch(new GetSettlement(settlement)).getSuccess()

        then:
        view.expenses().first().shares() == recorded.shares()

        when:
        def weightedSettlement = withParticipants()
        def weights = new WeightedShareAllocation(new LinkedHashMap<ParticipantId, Integer>([
            (CAL): 1, (BOB): 1, (ADA): 2
        ]))
        def weighted = (ExpenseRecorded) configuration.commands.dispatch(
            expenseWithAllocation(weightedSettlement, ADA, Money.of(0.01, "EUR"), weights))
            .getSuccess().events().first().payload()
        def weightedView = configuration.queries.dispatch(new GetSettlement(weightedSettlement)).getSuccess()

        then:
        weightedView.expenses().first().shares() == weighted.shares()
    }

    def "invalid Share Allocations reject without committing: #caseName"() {
        given:
        def settlement = withParticipants()
        if (removeFirst) configuration.commands.dispatch(new RemoveParticipant(settlement, BOB))
        def version = configuration.store.load(settlement).size()

        when:
        def result = configuration.commands.dispatch(expenseWithAllocation(settlement, ADA, amount, allocation))

        then:
        result.getFailure() == rejection.call(settlement)
        configuration.store.load(settlement).size() == version

        where:
        caseName | amount | allocation | removeFirst | rejection
        "exact currency" | Money.of(1, "EUR") | exact([(BOB): Money.of(1, "USD")]) | false |
            { id -> new ExactShareCurrencyMismatch(id, EXPENSE, BOB) }
        "exact zero" | Money.of(1, "EUR") | exact([(BOB): Money.zero("EUR")]) | false |
            { id -> new NonPositiveExactShare(id, EXPENSE, BOB) }
        "exact negative" | Money.of(1, "EUR") | exact([(BOB): Money.of(-1, "EUR")]) | false |
            { id -> new NonPositiveExactShare(id, EXPENSE, BOB) }
        "exact sum" | Money.of(1, "EUR") | exact([(BOB): Money.of(1.001, "EUR")]) | false |
            { id -> new ExactShareSumMismatch(id, EXPENSE) }
        "weight zero" | Money.of(1, "EUR") | weighted([(BOB): 0]) | false |
            { id -> new NonPositiveShareWeight(id, EXPENSE, BOB) }
        "weight negative" | Money.of(1, "EUR") | weighted([(BOB): -1]) | false |
            { id -> new NonPositiveShareWeight(id, EXPENSE, BOB) }
        "empty exact" | Money.of(1, "EUR") | exact([:]) | false |
            { id -> new EmptyShareAllocation(id, EXPENSE) }
        "empty weighted" | Money.of(1, "EUR") | weighted([:]) | false |
            { id -> new EmptyShareAllocation(id, EXPENSE) }
        "unknown exact recipient" | Money.of(1, "EUR") | exact([(UNKNOWN): Money.of(1, "EUR")]) | false |
            { id -> new ParticipantNotFound(id, UNKNOWN) }
        "removed weighted recipient" | Money.of(1, "EUR") | weighted([(BOB): 1]) | true |
            { id -> new ParticipantNotFound(id, BOB) }
    }

    def "Share Allocation rejection favors #priority"() {
        given:
        def settlement = withParticipants()
        def version = configuration.store.load(settlement).size()

        when:
        def result = configuration.commands.dispatch(expenseWithAllocation(settlement, payer, amount, allocation))

        then:
        result.getFailure() == rejection.call(settlement)
        configuration.store.load(settlement).size() == version

        where:
        priority | amount | payer | allocation | rejection
        "amount over empty exact" | Money.zero("EUR") | UNKNOWN | exact([:]) |
            { id -> new NonPositiveExpenseAmount(id, EXPENSE) }
        "amount over invalid weight" | Money.zero("EUR") | UNKNOWN | weighted([(BOB): 0]) |
            { id -> new NonPositiveExpenseAmount(id, EXPENSE) }
        "empty over invalid payer" | Money.of(1, "EUR") | UNKNOWN | weighted([:]) |
            { id -> new EmptyShareAllocation(id, EXPENSE) }
        "currency over positivity in same exact entry" | Money.of(1, "EUR") | UNKNOWN |
            exact([(BOB): Money.zero("USD")]) | { id -> new ExactShareCurrencyMismatch(id, EXPENSE, BOB) }
        "entry order over error type" | Money.of(1, "EUR") | UNKNOWN |
            exact([(CAL): Money.zero("EUR"), (BOB): Money.of(1, "USD")]) |
            { id -> new NonPositiveExactShare(id, EXPENSE, CAL) }
        "weight order over payer" | Money.of(1, "EUR") | UNKNOWN |
            weighted([(CAL): -1, (BOB): 0]) | { id -> new NonPositiveShareWeight(id, EXPENSE, CAL) }
        "sum over payer" | Money.of(1, "EUR") | UNKNOWN |
            exact([(BOB): Money.of(2, "EUR")]) | { id -> new ExactShareSumMismatch(id, EXPENSE) }
        "sum over recipient" | Money.of(1, "EUR") | ADA |
            exact([(UNKNOWN): Money.of(2, "EUR")]) | { id -> new ExactShareSumMismatch(id, EXPENSE) }
        "payer over recipient" | Money.of(1, "EUR") | UNKNOWN |
            exact([(UNKNOWN): Money.of(1, "EUR")]) | { id -> new ParticipantNotFound(id, UNKNOWN) }
        "recipient over missing exchange rate" | Money.of(1, "USD") | ADA |
            exact([(UNKNOWN): Money.of(1, "USD")]) | { id -> new ParticipantNotFound(id, UNKNOWN) }
        "sum over missing exchange rate" | Money.of(1, "USD") | ADA |
            exact([(BOB): Money.of(2, "USD")]) | { id -> new ExactShareSumMismatch(id, EXPENSE) }
        "missing exchange rate after validation" | Money.of(1, "USD") | ADA |
            weighted([(BOB): 1]) | { id -> new MissingExchangeRate(id, EXPENSE) }
    }

    def "Share Allocation retries use order type and literal values"() {
        given:
        def settlement = withParticipants()
        def original = weighted([(CAL): 2, (BOB): 2])
        configuration.commands.dispatch(expenseWithAllocation(settlement, ADA, Money.of(10, "EUR"), original))
        def version = configuration.store.load(settlement).size()

        when:
        def identical = configuration.commands.dispatch(expenseWithAllocation(settlement, ADA,
            Money.of(new BigDecimal("10.00"), "EUR"), weighted([(CAL): 2, (BOB): 2])))
        def reordered = configuration.commands.dispatch(expenseWithAllocation(settlement, ADA,
            Money.of(10, "EUR"), weighted([(BOB): 2, (CAL): 2])))
        def literalChange = configuration.commands.dispatch(expenseWithAllocation(settlement, ADA,
            Money.of(10, "EUR"), weighted([(CAL): 1, (BOB): 1])))
        def typeChange = configuration.commands.dispatch(expenseWithAllocation(settlement, ADA,
            Money.of(10, "EUR"), exact([(CAL): Money.of(5, "EUR"), (BOB): Money.of(5, "EUR")])))
        def invalidRetry = configuration.commands.dispatch(expenseWithAllocation(settlement, UNKNOWN,
            Money.zero("EUR"), weighted([:])))

        then:
        identical.getSuccess().events().empty
        [reordered, literalChange, typeChange, invalidRetry]*.getFailure() ==
            [new ExpenseIdentifierConflict(settlement, EXPENSE)] * 4
        configuration.store.load(settlement).size() == version
    }

    def "exact retry compares numeric amounts across scales"() {
        given:
        def settlement = withParticipants()
        configuration.commands.dispatch(expenseWithAllocation(settlement, ADA, Money.of(10, "EUR"),
            exact([(CAL): Money.of(4, "EUR"), (BOB): Money.of(6, "EUR")])))
        def version = configuration.store.load(settlement).size()

        when:
        def retry = configuration.commands.dispatch(expenseWithAllocation(settlement, ADA,
            Money.of(new BigDecimal("10.00"), "EUR"), exact([
                (CAL): Money.of(new BigDecimal("4.00"), "EUR"), (BOB): Money.of(new BigDecimal("6.0"), "EUR")
            ])))
        def reordered = configuration.commands.dispatch(expenseWithAllocation(settlement, ADA,
            Money.of(10, "EUR"), exact([(BOB): Money.of(6, "EUR"), (CAL): Money.of(4, "EUR")])))

        then:
        retry.getSuccess().events().empty
        reordered.getFailure() == new ExpenseIdentifierConflict(settlement, EXPENSE)
        configuration.store.load(settlement).size() == version
    }

    def "read models freeze collections without losing Balance order"() {
        given:
        def settlement = withParticipants()
        configuration.commands.dispatch(expense(settlement, ADA, Money.of(3, "EUR"), [BOB, CAL]))
        def original = configuration.queries.dispatch(new GetSettlement(settlement)).getSuccess()
        def frozen = original.expenses().get(0)
        def shares = new ArrayList<Share>(frozen.shares())
        def participants = new ArrayList(original.participants())
        def expenses = new ArrayList(original.expenses())
        def obligations = new ArrayList(original.obligations())
        def balances = new LinkedHashMap(original.balances())

        when:
        def copiedExpense = new ExpenseView(frozen.id(), frozen.description(), frozen.incurredOn(), frozen.payer(),
            frozen.originalAmount(), frozen.allocation(), frozen.componentVersionId(), frozen.exchangeRate(),
            frozen.valuation(), shares, frozen.status())
        def copiedSettlement = new SettlementView(original.id(), original.name(), original.currency(),
            original.version(), participants, expenses, obligations, balances)
        shares.clear()
        participants.clear()
        expenses.clear()
        obligations.clear()
        balances.clear()

        then:
        copiedExpense.shares() == frozen.shares()
        copiedSettlement.participants() == original.participants()
        copiedSettlement.expenses() == original.expenses()
        copiedSettlement.obligations() == original.obligations()
        copiedSettlement.balances().entrySet().toList()*.key == [ADA, BOB, CAL]
        copiedSettlement.balances() == original.balances()

        when:
        copiedSettlement.balances().clear()

        then:
        thrown(UnsupportedOperationException)
    }

    def "payer need not receive a Share and duplicate recipients collapse"() {
        given:
        def settlement = withParticipants()

        when:
        configuration.commands.dispatch(expense(settlement, ADA, Money.of(10, "EUR"), [CAL, BOB, BOB, CAL]))
        def view = configuration.queries.dispatch(new GetSettlement(settlement)).getSuccess()

        then:
        view.expenses().get(0).allocation().recipients().toList() == [CAL, BOB]
        view.expenses().get(0).shares()*.amount() == [Money.of(5, "EUR"), Money.of(5, "EUR")]
        view.balances()[ADA] == Money.of(10, "EUR")
        view.balances()[BOB] == Money.of(-5, "EUR")
        view.balances()[CAL] == Money.of(-5, "EUR")
    }

    def "invalid Expenses reject without committing"() {
        given:
        def settlement = withParticipants()
        if (removeFirst) configuration.commands.dispatch(new RemoveParticipant(settlement, BOB))
        def previous = configuration.store.load(settlement).size()

        when:
        def result = configuration.commands.dispatch(expense(settlement, payer, amount, recipients))

        then:
        result.getFailure() == rejection.call(settlement)
        configuration.store.load(settlement).size() == previous

        where:
        payer | amount                 | recipients  | removeFirst | rejection
        ADA   | Money.of(1, "EUR")     | []          | false       | { id -> new EmptyShareAllocation(id, EXPENSE) }
        ADA   | Money.zero("EUR")      | [BOB]       | false       | { id -> new NonPositiveExpenseAmount(id, EXPENSE) }
        ADA   | Money.of(-1, "EUR")    | [BOB]       | false       | { id -> new NonPositiveExpenseAmount(id, EXPENSE) }
        UNKNOWN | Money.of(1, "EUR")   | [BOB]       | false       | { id -> new ParticipantNotFound(id, UNKNOWN) }
        ADA   | Money.of(1, "EUR")     | [UNKNOWN]   | false       | { id -> new ParticipantNotFound(id, UNKNOWN) }
        BOB   | Money.of(1, "EUR")     | [ADA]       | true        | { id -> new ParticipantNotFound(id, BOB) }
        ADA   | Money.of(1, "EUR")     | [BOB]       | true        | { id -> new ParticipantNotFound(id, BOB) }
        ADA   | Money.of(1, "USD")     | [BOB]       | false       | { id -> new MissingExchangeRate(id, EXPENSE) }
    }

    def "Expense rejection favors #priority"() {
        given:
        def settlement = withParticipants()
        def version = configuration.store.load(settlement).size()

        when:
        def result = configuration.commands.dispatch(expense(settlement, payer, amount, recipients))

        then:
        result.getFailure() == rejection.call(settlement)
        configuration.store.load(settlement).size() == version

        where:
        priority                       | amount             | payer   | recipients | rejection
        "non-positive over empty"      | Money.zero("EUR")  | ADA     | []         | { id -> new NonPositiveExpenseAmount(id, EXPENSE) }
        "empty over unknown payer"     | Money.of(1, "EUR") | UNKNOWN | []         | { id -> new EmptyShareAllocation(id, EXPENSE) }
        "unknown payer over currency"  | Money.of(1, "USD") | UNKNOWN | [BOB]      | { id -> new ParticipantNotFound(id, UNKNOWN) }
        "unknown recipient over currency" | Money.of(1, "USD") | ADA | [UNKNOWN] | { id -> new ParticipantNotFound(id, UNKNOWN) }
    }

    def "dispatched retries after repository reload produce no event and conflicting reuse rejects"() {
        given:
        def settlement = withParticipants()
        def first = expense(settlement, ADA, Money.of(new BigDecimal("10.0"), "EUR"), [BOB])
        configuration.commands.dispatch(first)

        when:
        def retry = configuration.commands.dispatch(expense(settlement, ADA,
            Money.of(new BigDecimal("10.00"), "EUR"), [BOB]))
        def conflict = configuration.commands.dispatch(expense(settlement, ADA, Money.of(11, "EUR"), [BOB]))

        then:
        retry.getSuccess().events().empty
        retry.getSuccess().version() == 5
        conflict.getFailure() == new ExpenseIdentifierConflict(settlement, EXPENSE)
        configuration.store.load(settlement).size() == 5
    }

    def "equal Share Allocation exposes immutable insertion order"() {
        given:
        def allocation = new EqualShareAllocation([CAL, BOB, CAL, ADA])

        when:
        def recipients = allocation.recipients()

        then:
        recipients.toList() == [CAL, BOB, ADA]
        recipients.getFirst() == CAL
        recipients.getLast() == ADA
        allocation != new EqualShareAllocation([BOB, CAL, ADA])

        when:
        allocation.recipients().add(UNKNOWN)

        then:
        thrown(UnsupportedOperationException)
    }

    def "reordering recipients conflicts with the original Expense allocation"() {
        given:
        def settlement = withParticipants()
        configuration.commands.dispatch(expense(settlement, ADA, Money.of(10, "EUR"), [CAL, BOB]))

        when:
        def reordered = configuration.commands.dispatch(expense(settlement, ADA, Money.of(10, "EUR"), [BOB, CAL]))

        then:
        reordered.getFailure() == new ExpenseIdentifierConflict(settlement, EXPENSE)
        configuration.store.load(settlement).size() == 5
    }

    def "a payer or zero-share recipient cannot be removed"() {
        given:
        def settlement = withParticipants()
        configuration.commands.dispatch(new AddParticipant(settlement, DEX, new ParticipantName("Dex")))
        configuration.commands.dispatch(expense(settlement, ADA, Money.of(0.01, "EUR"), [BOB, CAL]))
        def beforeRemoval = configuration.queries.dispatch(new GetSettlement(settlement)).getSuccess()

        when:
        def payerRemoval = configuration.commands.dispatch(new RemoveParticipant(settlement, ADA))
        def zeroShareRemoval = configuration.commands.dispatch(new RemoveParticipant(settlement, CAL))
        def unrelatedRemoval = configuration.commands.dispatch(new RemoveParticipant(settlement, DEX))
        def afterRemoval = configuration.queries.dispatch(new GetSettlement(settlement)).getSuccess()

        then:
        beforeRemoval.balances().entrySet().toList()*.key == [ADA, BOB, CAL, DEX]
        beforeRemoval.balances().entrySet().toList()*.value ==
            [Money.of(0.01, "EUR"), Money.of(-0.01, "EUR"), Money.zero("EUR"), Money.zero("EUR")]
        payerRemoval.getFailure() == new ParticipantReferenced(settlement, ADA)
        zeroShareRemoval.getFailure() == new ParticipantReferenced(settlement, CAL)
        unrelatedRemoval.getSuccess().events().size() == 1
        afterRemoval.balances().entrySet().toList()*.key == [ADA, BOB, CAL]
        afterRemoval.balances().entrySet().toList()*.value ==
            [Money.of(0.01, "EUR"), Money.of(-0.01, "EUR"), Money.zero("EUR")]
        configuration.store.load(settlement).size() == 7
    }

    def "unknown Settlement rejects recording without a stream"() {
        when:
        def result = configuration.commands.dispatch(expense(configuration.UNKNOWN_ID, ADA, Money.of(1, "EUR"), [BOB]))

        then:
        result.getFailure() == new SettlementNotFound(configuration.UNKNOWN_ID)
        configuration.queries.dispatch(new GetSettlementHistory(configuration.UNKNOWN_ID)).getFailure() ==
            new SettlementNotFound(configuration.UNKNOWN_ID)
    }

    def "Expense descriptions reject blank input"() {
        when:
        new ExpenseDescription(value)

        then:
        thrown(IllegalArgumentException)

        where:
        value << ["", "  ", "\n"]
    }

    private def withParticipants(currency = SettlementTestConfiguration.EUR) {
        def settlement = configuration.openSettlement("Holiday", currency)
        [ADA, BOB, CAL].each { id ->
            configuration.commands.dispatch(new AddParticipant(settlement, id, new ParticipantName(id.toString())))
        }
        settlement
    }

    private static RecordExpense expense(settlement, payer, amount, recipients) {
        new RecordExpense(settlement, EXPENSE, new ExpenseDescription("Lunch"), DATE, payer, amount,
            new EqualShareAllocation(recipients))
    }

    private static ExactShareAllocation exact(Map amounts) {
        new ExactShareAllocation(new LinkedHashMap<ParticipantId, Money>(amounts))
    }

    private static WeightedShareAllocation weighted(Map weights) {
        new WeightedShareAllocation(new LinkedHashMap<ParticipantId, Integer>(weights))
    }

    private static RecordExpense expenseWithAllocation(settlement, payer, amount, allocation) {
        new RecordExpense(settlement, EXPENSE, new ExpenseDescription("Lunch"), DATE, payer, amount, allocation)
    }

    private static ParticipantId participant(int suffix) {
        new ParticipantId(new UUID(0L, suffix))
    }
}
