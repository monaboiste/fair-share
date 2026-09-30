package com.github.monaboiste.fairshare.settlement.application

import com.github.monaboiste.fairshare.pricing.component.ComponentVersionId
import com.github.monaboiste.fairshare.pricing.component.Validity
import com.github.monaboiste.fairshare.quantity.money.Money
import com.github.monaboiste.fairshare.settlement.application.command.AddParticipant
import com.github.monaboiste.fairshare.settlement.application.command.ConfigureExchangeRate
import com.github.monaboiste.fairshare.settlement.application.command.RecordExpense
import com.github.monaboiste.fairshare.settlement.application.command.handler.RecordExpenseHandler
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlement
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlementHistory
import com.github.monaboiste.fairshare.settlement.domain.EqualShareAllocation
import com.github.monaboiste.fairshare.settlement.domain.ExactShareAllocation
import com.github.monaboiste.fairshare.settlement.domain.ExchangeRateOverrideMismatch
import com.github.monaboiste.fairshare.settlement.domain.ExpenseDescription
import com.github.monaboiste.fairshare.settlement.domain.ExpenseId
import com.github.monaboiste.fairshare.settlement.domain.ExpenseIdentifierConflict
import com.github.monaboiste.fairshare.settlement.domain.MissingExchangeRate
import com.github.monaboiste.fairshare.settlement.domain.NonPositiveExpenseAmount
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId
import com.github.monaboiste.fairshare.settlement.domain.ParticipantName
import com.github.monaboiste.fairshare.settlement.domain.ParticipantNotFound
import com.github.monaboiste.fairshare.settlement.domain.Share
import com.github.monaboiste.fairshare.settlement.domain.WeightedShareAllocation
import com.github.monaboiste.fairshare.settlement.domain.event.ExchangeRateConfigured
import com.github.monaboiste.fairshare.settlement.domain.event.ExpenseRecorded
import com.github.monaboiste.fairshare.settlement.infrastructure.EventSourcedSettlementRepository
import com.github.monaboiste.fairshare.settlement.infrastructure.SettlementProjector
import com.github.monaboiste.fairshare.valuation.DoublingValuationEngine
import com.github.monaboiste.fairshare.valuation.ExchangeRate
import com.github.monaboiste.fairshare.valuation.ValuationEngine
import java.time.LocalDate
import javax.money.CurrencyUnit
import javax.money.Monetary
import spock.lang.Specification

class ForeignCurrencyExpensesSpec extends Specification {
    private static final ParticipantId ADA = new ParticipantId(new UUID(0L, 11L))
    private static final ParticipantId BOB = new ParticipantId(new UUID(0L, 12L))
    private static final ParticipantId CAL = new ParticipantId(new UUID(0L, 13L))
    private static final ParticipantId UNKNOWN = new ParticipantId(new UUID(0L, 99L))
    private static final ExpenseId EXPENSE = new ExpenseId(new UUID(0L, 21L))
    private static final ExpenseId OTHER_EXPENSE = new ExpenseId(new UUID(0L, 22L))
    private static final ExpenseId FRESH_EXPENSE = new ExpenseId(new UUID(0L, 23L))
    private static final CurrencyUnit GBP = Monetary.getCurrency("GBP")
    private static final LocalDate DATE = LocalDate.of(2026, 2, 3)
    def configuration = new SettlementTestConfiguration()

    def "Expense selects the latest applicable Exchange Rate at incurred-date midnight"() {
        given: "a Settlement with a baseline Exchange Rate, one valid from the Expense date, and one from the next day"
        def settlement = withParticipants()
        def february = configure(settlement, "0.90", Validity.from(DATE.atStartOfDay()))
        configure(settlement, "0.80", Validity.always())
        def later = configure(settlement, "0.95", Validity.from(DATE.atStartOfDay().plusDays(1)))

        when: "a 10 US dollar Expense is recorded on that date"
        def recorded = record(settlement, DATE, Money.of(10, "USD"), new EqualShareAllocation([BOB]))

        then: "the Exchange Rate valid from that date's midnight gives the Valuation"
        recorded.componentVersionId() == february.id()
        recorded.exchangeRate() == february.exchangeRate()
        recorded.valuation() == Money.of(9, "EUR")
        recorded.componentVersionId() != later.id()
    }

    def "later stream order wins when applicable Exchange Rates share a valid-from date"() {
        given: "a Settlement with two Exchange Rates valid from the Expense date, the second being a correction"
        def settlement = withParticipants()
        configure(settlement, "0.90", Validity.from(DATE.atStartOfDay()))
        def corrected = configure(settlement, "0.95", Validity.from(DATE.atStartOfDay()))

        when: "a 10 US dollar Expense is recorded on that date"
        def recorded = record(settlement, DATE, Money.of(10, "USD"), new EqualShareAllocation([BOB]))

        then: "the later configured Exchange Rate gives the Valuation"
        recorded.componentVersionId() == corrected.id()
        recorded.exchangeRate() == corrected.exchangeRate()
        recorded.valuation() == Money.of(9.50, "EUR")
    }

    def "foreign Expense without an applicable directional Exchange Rate rejects without an event: #scenario"() {
        given: "a Settlement without an Exchange Rate from US dollars to euros applicable on the Expense date"
        def settlement = withParticipants()
        if (configuredRate != null) {
            configuration.commands.dispatch(new ConfigureExchangeRate(settlement, configuredRate, validity))
        }
        def before = configuration.queries.dispatch(new GetSettlementHistory(settlement)).getSuccess()

        when: "a US dollar Expense is recorded"
        def result = configuration.commands.dispatch(expense(settlement, DATE,
                Money.of(1, "USD"), new EqualShareAllocation([BOB])))

        then: "the Expense is rejected as missing an Exchange Rate and nothing is recorded"
        result.getFailure() == new MissingExchangeRate(settlement, EXPENSE)
        configuration.queries.dispatch(new GetSettlementHistory(settlement)).getSuccess() == before
        configuration.queries.dispatch(new GetSettlement(settlement)).getSuccess().expenses().empty

        where:
        scenario                     | configuredRate                           | validity
        "unconfigured"               | null                                     | null
        "other source currency"      | ExchangeRate.of(Monetary.getCurrency("GBP"),
                SettlementTestConfiguration.EUR, BigDecimal.ONE)                | Validity.always()
        "before valid-from midnight" | ExchangeRate.of(SettlementTestConfiguration.USD,
                SettlementTestConfiguration.EUR, BigDecimal.ONE)                | Validity.from(DATE.atStartOfDay().plusSeconds(1))
        "after valid-until"          | ExchangeRate.of(SettlementTestConfiguration.USD,
                SettlementTestConfiguration.EUR, BigDecimal.ONE)                | Validity.until(DATE.minusDays(1).atStartOfDay())
    }

    def "foreign #allocationName allocation resolves Shares in Settlement Currency"() {
        given: "a Settlement with an always-valid Exchange Rate from US dollars to euros"
        def settlement = withParticipants()
        configure(settlement, rate, Validity.always())

        when: "a foreign Expense is recorded with the given Share Allocation"
        def recorded = record(settlement, DATE, original, allocation)
        def view = configuration.queries.dispatch(new GetSettlement(settlement)).getSuccess()

        then: "the Shares are expressed in the Settlement Currency and add up to the Valuation"
        recorded.allocation() == allocation
        recorded.valuation() == valuation
        recorded.shares() == shares
        recorded.shares()*.amount()*.currencyUnit() == [SettlementTestConfiguration.EUR] * shares.size()
        recorded.shares()*.amount().inject(Money.zero("EUR")) { sum, share -> sum.add(share) } == valuation
        view.expenses().first().shares() == shares

        where:
        allocationName | rate   | original            | allocation                                | valuation                       | shares
        "equal"        | "0.95" | Money.of(10, "USD") | new EqualShareAllocation([CAL, BOB, ADA]) |
                Money.of(9.50, "EUR")                                                                                               | [new Share(CAL, Money.of(3.16, "EUR")),
                                                                                                                                       new Share(BOB, Money.of(3.17, "EUR")), new Share(ADA, Money.of(3.17, "EUR"))]
        "weighted"     | "0.95" | Money.of(10, "USD") |
                new WeightedShareAllocation(new LinkedHashMap([(CAL): 1, (BOB): 1, (ADA): 2]))    |
                Money.of(9.50, "EUR")                                                                                               | [new Share(CAL, Money.of(2.37, "EUR")),
                                                                                                                                       new Share(BOB, Money.of(2.38, "EUR")), new Share(ADA, Money.of(4.75, "EUR"))]
        "exact in USD" | "0.01" | Money.of(1, "USD")  |
                new ExactShareAllocation(new LinkedHashMap([(CAL): Money.of(0.50, "USD"),
                                                            (BOB): Money.of(0.50, "USD")]))       |
                Money.of(0.01, "EUR")                                                                                               | [new Share(BOB, Money.of(0.01, "EUR"))]
    }

    def "recorded foreign Expense freezes original Money, selected Valuation and balances across replay"() {
        given: "a Settlement with an Exchange Rate of 0.95 and a 10 US dollar Expense shared by Bob and Cal"
        def settlement = withParticipants()
        def version = configure(settlement, "0.95", Validity.from(DATE.atStartOfDay()))
        def allocation = new EqualShareAllocation([BOB, CAL])
        def command = expense(settlement, DATE, Money.of(10, "USD"), allocation)

        when: "the Expense is recorded and the view is rebuilt from history"
        def commit = configuration.commands.dispatch(command).getSuccess()
        def recorded = (ExpenseRecorded) commit.events().first().payload()
        def view = configuration.queries.dispatch(new GetSettlement(settlement)).getSuccess()
        def history = configuration.queries.dispatch(new GetSettlementHistory(settlement)).getSuccess()
        def replay = new SettlementProjector()
        replay.rebuild(configuration.store)

        then: "the original amount, Exchange Rate, Valuation and Shares are frozen, and Balances net to zero"
        recorded.type() == "ExpenseRecorded"
        recorded.schemaVersion() == 1
        recorded.originalAmount() == Money.of(10, "USD")
        recorded.componentVersionId() == version.id()
        recorded.exchangeRate() == version.exchangeRate()
        recorded.valuation() == Money.of(9.50, "EUR")
        recorded.allocation() == allocation
        recorded.shares() == [new Share(BOB, Money.of(4.75, "EUR")),
                              new Share(CAL, Money.of(4.75, "EUR"))]
        view.expenses().first().originalAmount() == recorded.originalAmount()
        view.expenses().first().componentVersionId() == recorded.componentVersionId()
        view.expenses().first().exchangeRate() == recorded.exchangeRate()
        view.expenses().first().valuation() == recorded.valuation()
        view.expenses().first().allocation() == recorded.allocation()
        view.expenses().first().shares() == recorded.shares()
        history.last().payload() == recorded
        replay.findById(settlement).orElseThrow() == view
        view.balances()[ADA] == Money.of(9.50, "EUR")
        view.balances()[BOB] == Money.of(-4.75, "EUR")
        view.balances()[CAL] == Money.of(-4.75, "EUR")
        view.balances().values().inject(Money.zero("EUR")) { sum, balance -> sum.add(balance) }.isZero()
    }

    def "used Expense identifier conflicts after a newer rate without revaluing"() {
        given: "a Settlement with a recorded US dollar Expense, after which a newer Exchange Rate was configured"
        def settlement = withParticipants()
        configure(settlement, "0.90", Validity.always())
        def original = expense(settlement, DATE, Money.of(10, "USD"), new EqualShareAllocation([BOB]))
        configuration.commands.dispatch(original).getSuccess()
        def frozen = configuration.queries.dispatch(new GetSettlement(settlement)).getSuccess().expenses().first()
        configure(settlement, "0.95", Validity.from(DATE.atStartOfDay()))
        def before = configuration.queries.dispatch(new GetSettlementHistory(settlement)).getSuccess()

        when: "the Expense is retried identically, with a changed amount, and in another currency"
        def retry = configuration.commands.dispatch(expense(settlement, DATE,
                Money.of(new BigDecimal("10.00"), "USD"), new EqualShareAllocation([BOB])))
        def changedAmount = configuration.commands.dispatch(expense(settlement, DATE,
                Money.of(11, "USD"), new EqualShareAllocation([BOB])))
        def changedCurrency = configuration.commands.dispatch(expense(settlement, DATE,
                Money.of(10, "EUR"), new EqualShareAllocation([BOB])))
        def view = configuration.queries.dispatch(new GetSettlement(settlement)).getSuccess()

        then: "all reused identifiers conflict, keeping the frozen Valuation"
        retry.getFailure() == new ExpenseIdentifierConflict(settlement, EXPENSE)
        changedAmount.getFailure() == new ExpenseIdentifierConflict(settlement, EXPENSE)
        changedCurrency.getFailure() == new ExpenseIdentifierConflict(settlement, EXPENSE)
        configuration.queries.dispatch(new GetSettlementHistory(settlement)).getSuccess() == before
        view.expenses().first() == frozen
    }

    def "Exchange Rate Override values one foreign Expense without changing configured rates: #scenario"() {
        given: "a Settlement with or without a configured Exchange Rate"
        def settlement = withParticipants()
        if (configuredRate != null) {
            configure(settlement, configuredRate, Validity.always())
        }
        def configuredBefore = configuredVersions(settlement)

        when: "a 10 US dollar Expense is recorded with an Exchange Rate Override of 0.97"
        def recorded = record(settlement, DATE, Money.of(10, "USD"), new EqualShareAllocation([BOB, CAL]),
                manual("0.97"))
        def view = configuration.queries.dispatch(new GetSettlement(settlement)).getSuccess()

        then: "the override gives the Valuation and Shares, and configured Exchange Rates stay unchanged"
        recorded.exchangeRateOverride() == manual("0.97")
        recorded.exchangeRate() == manual("0.97")
        recorded.valuation() == Money.of(9.70, "EUR")
        recorded.shares() == [new Share(BOB, Money.of(4.85, "EUR")), new Share(CAL, Money.of(4.85, "EUR"))]
        configuredVersions(settlement) == configuredBefore
        !configuredBefore*.id().contains(recorded.componentVersionId())
        view.expenses().first().exchangeRate() == manual("0.97")
        view.expenses().first().valuation() == Money.of(9.70, "EUR")
        view.expenses().first().shares() == recorded.shares()

        where:
        scenario                     | configuredRate
        "applicable configured rate" | "0.90"
        "no configured rate"         | null
    }

    def "Exchange Rate Override affects only its Expense"() {
        given: "a Settlement with a configured Exchange Rate and an Expense recorded with an Exchange Rate Override"
        def settlement = withParticipants()
        def configured = configure(settlement, "0.90", Validity.always())
        record(settlement, DATE, Money.of(10, "USD"), new EqualShareAllocation([BOB]), manual("0.97"))

        when: "another Expense is recorded without an override"
        def sibling = record(settlement, DATE, Money.of(10, "USD"), new EqualShareAllocation([BOB]), null,
                OTHER_EXPENSE)

        then: "the other Expense is valued with the configured Exchange Rate"
        sibling.exchangeRateOverride() == null
        sibling.componentVersionId() == configured.id()
        sibling.exchangeRate() == configured.exchangeRate()
        sibling.valuation() == Money.of(9, "EUR")
    }

    def "Exchange Rate Override for another direction rejects without an event: #scenario"() {
        given: "a Settlement with a configured Exchange Rate from US dollars to euros"
        def settlement = withParticipants()
        configure(settlement, "0.90", Validity.always())
        def before = configuration.queries.dispatch(new GetSettlementHistory(settlement)).getSuccess()

        when: "an Expense is recorded with an Exchange Rate Override for another currency direction"
        def result = configuration.commands.dispatch(expense(settlement, DATE, original,
                new EqualShareAllocation([BOB]), override))

        then: "the Expense is rejected as an override mismatch and nothing is recorded"
        result.getFailure() == new ExchangeRateOverrideMismatch(settlement, EXPENSE)
        configuration.queries.dispatch(new GetSettlementHistory(settlement)).getSuccess() == before
        configuration.queries.dispatch(new GetSettlement(settlement)).getSuccess().expenses().empty

        where:
        scenario                | original            | override
        "other source currency" | Money.of(10, "USD") | ExchangeRate.of(GBP, SettlementTestConfiguration.EUR, BigDecimal.ONE)
        "other target currency" | Money.of(10, "USD") | ExchangeRate.of(SettlementTestConfiguration.USD, GBP, BigDecimal.ONE)
        "same-currency Expense" | Money.of(10, "EUR") |
                ExchangeRate.of(SettlementTestConfiguration.EUR, SettlementTestConfiguration.EUR, BigDecimal.ONE)
    }

    def "Exchange Rate Override mismatch yields to #priority"() {
        given: "a Settlement with Participants"
        def settlement = withParticipants()

        when: "an Expense with a mismatched Exchange Rate Override and another problem is recorded"
        def result = configuration.commands.dispatch(expense(settlement, DATE, original,
                new EqualShareAllocation([recipient]), ExchangeRate.of(GBP, SettlementTestConfiguration.EUR, BigDecimal.ONE)))

        then: "the Expense is rejected with the higher-priority reason"
        result.getFailure() == rejection.call(settlement)

        where:
        priority              | original            | recipient | rejection
        "non-positive amount" | Money.zero("USD")   | BOB       | { id -> new NonPositiveExpenseAmount(id, EXPENSE) }
        "unknown recipient"   | Money.of(10, "USD") | UNKNOWN   | { id -> new ParticipantNotFound(id, UNKNOWN) }
    }

    def "used Expense identifier conflicts with equivalent Exchange Rate Override"() {
        given: "a Settlement with an Expense recorded with an Exchange Rate Override of 0.95"
        def settlement = withParticipants()
        configuration.commands.dispatch(expense(settlement, DATE, Money.of(10, "USD"),
                new EqualShareAllocation([BOB]), manual("0.95"))).getSuccess()
        def before = configuration.queries.dispatch(new GetSettlementHistory(settlement)).getSuccess()

        when: "the Expense is retried with the same override written as 0.950"
        def retry = configuration.commands.dispatch(expense(settlement, DATE, Money.of(10, "USD"),
                new EqualShareAllocation([BOB]), manual("0.950")))

        then: "the reuse conflicts without appending"
        retry.getFailure() == new ExpenseIdentifierConflict(settlement, EXPENSE)
        configuration.queries.dispatch(new GetSettlementHistory(settlement)).getSuccess() == before
    }

    def "retry with another Exchange Rate Override conflicts: #scenario"() {
        given: "a Settlement with an Expense recorded with or without an Exchange Rate Override"
        def settlement = withParticipants()
        configure(settlement, "0.95", Validity.always())
        configuration.commands.dispatch(expense(settlement, DATE, Money.of(10, "USD"),
                new EqualShareAllocation([BOB]), originalOverride)).getSuccess()
        def before = configuration.queries.dispatch(new GetSettlementHistory(settlement)).getSuccess()

        when: "the Expense is retried with a different override, or none"
        def retry = configuration.commands.dispatch(expense(settlement, DATE, Money.of(10, "USD"),
                new EqualShareAllocation([BOB]), retryOverride))

        then: "the retry conflicts and nothing is recorded"
        retry.getFailure() == new ExpenseIdentifierConflict(settlement, EXPENSE)
        configuration.queries.dispatch(new GetSettlementHistory(settlement)).getSuccess() == before

        where:
        scenario                                    | originalOverride | retryOverride
        "manual retried without override"           | manual("0.95")   | null
        "configured retried with equal manual rate" | null             | manual("0.95")
        "other manual rate"                         | manual("0.95")   | manual("0.96")
        "manual rate from another source"           | manual("0.95")   |
                ExchangeRate.of(GBP, SettlementTestConfiguration.EUR, new BigDecimal("0.95"))
        "manual rate into another target"           | manual("0.95")   |
                ExchangeRate.of(SettlementTestConfiguration.USD, GBP, new BigDecimal("0.95"))
    }

    def "recorded Valuations and Shares survive changed Exchange Rates and Pricing on replay"() {
        given: "a Settlement with one Expense valued by an Exchange Rate Override and another by a configured one"
        def settlement = withParticipants()
        configure(settlement, "0.90", Validity.from(DATE.atStartOfDay()))
        def overridden = record(settlement, DATE, Money.of(10, "USD"), new EqualShareAllocation([BOB, CAL]),
                manual("0.97"))
        def configured = record(settlement, DATE, Money.of(10, "USD"),
                new WeightedShareAllocation(new LinkedHashMap([(BOB): 1, (CAL): 2])), null, OTHER_EXPENSE)
        def frozen = configuration.queries.dispatch(new GetSettlement(settlement)).getSuccess()

        and: "a lower Exchange Rate is configured and Valuation now doubles every amount"
        configure(settlement, "0.50", Validity.from(DATE.atStartOfDay()))
        def doubledPricing = new DoublingValuationEngine()
        def changedValuationEngine = Mock(ValuationEngine) {
            identity(_) >> { Money source -> doubledPricing.identity(source) }
            value(*_) >> { arguments -> doubledPricing.value(*arguments) }
        }
        def reloadingHandler = new RecordExpenseHandler(
                new EventSourcedSettlementRepository(configuration.store, SettlementTestConfiguration.CLOCK),
                SettlementTestConfiguration.CLOCK, changedValuationEngine, ComponentVersionId::generate)

        when: "both Expenses are retried under the changed Valuation, the view is rebuilt, and a new Expense is recorded"
        def overriddenRetry = reloadingHandler.handle(expense(settlement, DATE, Money.of(10, "USD"),
                new EqualShareAllocation([BOB, CAL]), manual("0.97")))
        def configuredRetry = reloadingHandler.handle(expense(settlement, DATE, Money.of(10, "USD"),
                new WeightedShareAllocation(new LinkedHashMap([(BOB): 1, (CAL): 2])), null, OTHER_EXPENSE))
        def rebuilt = new SettlementProjector()
        rebuilt.rebuild(configuration.store)
        def replayed = rebuilt.findById(settlement).orElseThrow()
        def fresh = record(settlement, DATE, Money.of(10, "USD"), new EqualShareAllocation([BOB]), null, FRESH_EXPENSE)

        then: "reused identifiers conflict without revaluation, replay retains facts, and a new Expense uses the lower rate"
        0 * changedValuationEngine._
        overriddenRetry.getFailure() == new ExpenseIdentifierConflict(settlement, EXPENSE)
        configuredRetry.getFailure() == new ExpenseIdentifierConflict(settlement, OTHER_EXPENSE)
        overridden.valuation() == Money.of(9.70, "EUR")
        overridden.shares() == [new Share(BOB, Money.of(4.85, "EUR")), new Share(CAL, Money.of(4.85, "EUR"))]
        configured.valuation() == Money.of(9, "EUR")
        configured.shares() == [new Share(BOB, Money.of(3, "EUR")), new Share(CAL, Money.of(6, "EUR"))]
        replayed.expenses()*.valuation() == [overridden.valuation(), configured.valuation()]
        replayed.expenses()*.exchangeRate() == [overridden.exchangeRate(), configured.exchangeRate()]
        replayed.expenses()*.shares() == [overridden.shares(), configured.shares()]
        replayed.expenses() == frozen.expenses()
        replayed.balances() == frozen.balances()
        fresh.valuation() == Money.of(5, "EUR")
    }

    private def withParticipants() {
        def settlement = configuration.openSettlement("Holiday")
        [ADA, BOB, CAL].each { id ->
            configuration.commands.dispatch(new AddParticipant(settlement, id, new ParticipantName(id.toString())))
        }
        settlement
    }

    private def configure(settlement, String rate, Validity validity) {
        configuration.commands.dispatch(new ConfigureExchangeRate(settlement,
                ExchangeRate.of(configuration.USD, configuration.EUR, new BigDecimal(rate)), validity))
                .getSuccess().events().first().payload().version()
    }

    private def record(settlement, LocalDate date, Money original, allocation, ExchangeRate override = null,
                       ExpenseId expenseId = EXPENSE) {
        (ExpenseRecorded) configuration.commands.dispatch(expense(settlement, date, original, allocation, override,
                expenseId)).getSuccess().events().first().payload()
    }

    private def configuredVersions(settlement) {
        configuration.queries.dispatch(new GetSettlementHistory(settlement)).getSuccess()*.payload()
                .findAll { it instanceof ExchangeRateConfigured }*.version()
    }

    private static ExchangeRate manual(String rate) {
        ExchangeRate.of(SettlementTestConfiguration.USD, SettlementTestConfiguration.EUR, new BigDecimal(rate))
    }

    private static RecordExpense expense(settlement, LocalDate date, Money original, allocation,
                                         ExchangeRate override = null, ExpenseId expenseId = EXPENSE) {
        new RecordExpense(settlement, expenseId, new ExpenseDescription("Lunch"), date, ADA, original, allocation,
                override)
    }
}
