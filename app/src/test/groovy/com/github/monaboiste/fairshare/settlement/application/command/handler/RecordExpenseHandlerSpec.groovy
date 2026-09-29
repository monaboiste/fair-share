package com.github.monaboiste.fairshare.settlement.application.command.handler

import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.CLOCK
import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.EUR
import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.NOW
import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.UNKNOWN_ID
import static com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration.USD

import com.github.monaboiste.fairshare.common.events.CommitResult
import com.github.monaboiste.fairshare.common.events.VersionConflictException
import com.github.monaboiste.fairshare.pricing.component.ComponentVersionId
import com.github.monaboiste.fairshare.pricing.component.Validity
import com.github.monaboiste.fairshare.quantity.money.Money
import com.github.monaboiste.fairshare.settlement.application.SettlementTestConfiguration
import com.github.monaboiste.fairshare.settlement.application.command.AddParticipant
import com.github.monaboiste.fairshare.settlement.application.command.ConfigureExchangeRate
import com.github.monaboiste.fairshare.settlement.application.command.RecordExpense
import com.github.monaboiste.fairshare.settlement.domain.EqualShareAllocation
import com.github.monaboiste.fairshare.settlement.domain.ExpenseDescription
import com.github.monaboiste.fairshare.settlement.domain.ExpenseId
import com.github.monaboiste.fairshare.settlement.domain.ExpenseIdentifierConflict
import com.github.monaboiste.fairshare.settlement.domain.ParticipantId
import com.github.monaboiste.fairshare.settlement.domain.ParticipantName
import com.github.monaboiste.fairshare.settlement.domain.SettlementId
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound
import com.github.monaboiste.fairshare.settlement.domain.Share
import com.github.monaboiste.fairshare.settlement.domain.event.ExpenseRecorded
import com.github.monaboiste.fairshare.settlement.domain.event.SettlementEvent
import com.github.monaboiste.fairshare.settlement.domain.model.Settlement
import com.github.monaboiste.fairshare.settlement.domain.model.SettlementRepository
import com.github.monaboiste.fairshare.valuation.DoublingValuationEngine
import com.github.monaboiste.fairshare.valuation.ExchangeRate
import com.github.monaboiste.fairshare.valuation.ExchangeRateOverride
import com.github.monaboiste.fairshare.valuation.ValuationEngine
import java.time.LocalDate
import java.time.LocalDateTime
import spock.lang.Specification

class RecordExpenseHandlerSpec extends Specification {
    private static final ParticipantId ADA = new ParticipantId(new UUID(0L, 11L))
    private static final ExpenseId EXPENSE = new ExpenseId(new UUID(0L, 21L))
    private static final LocalDate DATE = LocalDate.of(2026, 2, 3)
    private static final ComponentVersionId OVERRIDE_VERSION = new ComponentVersionId(new UUID(0L, 31L))
    private static final ValuationEngine CHANGED_PRICING = new DoublingValuationEngine()
    def configuration = new SettlementTestConfiguration()

    def "recording an Expense commits its valuation and ordered envelope"() {
        given: "an open euro Settlement with Ada, and a 10 euro lunch paid by Ada and shared only with her"
        SettlementId id = withParticipant()
        def command = expense(id, Money.of(10, "EUR"))

        when: "the Expense is recorded"
        def commit = configuration.recordExpenseHandler.handle(command).getSuccess()

        then: "the commit carries the Expense with its Valuation and Ada's full Share at the next version"
        commit.streamId() == id
        commit.version() == 3
        commit.events().size() == 1
        commit.events().first().streamId() == id
        commit.events().first().sequence() == 3
        commit.events().first().occurredAt() == NOW
        def payload = commit.events().first().payload()
        payload instanceof ExpenseRecorded
        payload.expenseId() == EXPENSE
        payload.originalAmount() == command.amount()
        payload.allocation() == command.allocation()
        payload.valuation().compareTo(Money.of(10, "EUR")) == 0
        payload.shares() == [new Share(ADA, Money.of(10, "EUR"))]
    }

    def "identical Expense retry succeeds without committing"() {
        given: "a 10 euro Expense has already been recorded"
        SettlementId id = withParticipant()
        def command = expense(id, Money.of(10, "EUR"))
        configuration.recordExpenseHandler.handle(command)

        when: "the same recording is sent again"
        def retry = configuration.recordExpenseHandler.handle(command).getSuccess()

        then: "it succeeds at the same version and nothing new is saved"
        retry.streamId() == id
        retry.version() == 3
        retry.events().empty
        configuration.store.load(id).size() == 3
    }

    def "conflicting Expense identifier rejects without committing"() {
        given: "a 10 euro Expense has already been recorded"
        SettlementId id = withParticipant()
        configuration.recordExpenseHandler.handle(expense(id, Money.of(10, "EUR")))

        when: "a different 11 euro Expense is recorded under the same identifier"
        def result = configuration.recordExpenseHandler.handle(expense(id, Money.of(11, "EUR")))

        then: "the recording is rejected as an identifier conflict and nothing new is saved"
        result.getFailure() == new ExpenseIdentifierConflict(id, EXPENSE)
        configuration.store.load(id).size() == 3
    }

    def "recording in an unknown Settlement rejects without creating a stream"() {
        when: "an Expense is recorded in a Settlement that does not exist"
        def result = configuration.recordExpenseHandler.handle(expense(UNKNOWN_ID, Money.of(10, "EUR")))

        then: "it is rejected as not found and no stream is created"
        result.getFailure() == new SettlementNotFound(UNKNOWN_ID)
        !configuration.store.exists(UNKNOWN_ID)
    }

    def "stale Expense recording fails optimistic concurrency and preserves the winner"() {
        given: "a handler holds an outdated copy of a Settlement after a 10 euro Expense was recorded"
        SettlementId id = withParticipant()
        def stale = configuration.repository.findById(id).orElseThrow()
        configuration.recordExpenseHandler.handle(expense(id, Money.of(10, "EUR")))
        SettlementRepository outdated = new SettlementRepository() {
            Optional<Settlement> findById(SettlementId lookupId) { Optional.of(stale) }

            CommitResult<SettlementId, SettlementEvent> save(Settlement settlement) {
                configuration.repository.save(settlement)
            }
        }

        when: "the outdated handler records an 11 euro Expense under the same identifier"
        new RecordExpenseHandler(outdated, CLOCK).handle(expense(id, Money.of(11, "EUR")))

        then: "the save fails on a version conflict and the 10 euro Expense is kept"
        thrown(VersionConflictException)
        configuration.projector.findById(id).orElseThrow().expenses().first().originalAmount() == Money.of(10, "EUR")
    }

    def "changed Pricing values a new same-currency Expense with the implicit Exchange Rate"() {
        given: "a 10 euro Expense and a changed Valuation that doubles every amount"
        SettlementId id = withParticipant()
        def command = expense(id, Money.of(10, "EUR"))
        def implicit = ValuationEngine.standard().identity(command.amount())
        def engine = Mock(ValuationEngine)
        def handler = new RecordExpenseHandler(configuration.repository, CLOCK, engine, { OVERRIDE_VERSION })

        when: "the Expense is recorded"
        def payload = (ExpenseRecorded) handler.handle(command).getSuccess().events().first().payload()

        then: "it keeps the implicit Exchange Rate while its Valuation and Share follow the doubled amount"
        1 * engine.identity(command.amount()) >> CHANGED_PRICING.identity(command.amount())
        0 * engine._
        payload.componentVersionId() == implicit.componentVersion().id()
        payload.exchangeRate() == implicit.exchangeRate()
        payload.valuation() == Money.of(20, "EUR")
        payload.shares() == [new Share(ADA, Money.of(20, "EUR"))]
    }

    def "changed Pricing values a new foreign Expense with the selected Exchange Rate"() {
        given: "a configured dollar to euro Exchange Rate, a 10 dollar Expense and a changed Valuation that doubles amounts"
        SettlementId id = withParticipant()
        def selected = configuration.configureExchangeRateHandler.handle(new ConfigureExchangeRate(id,
                ExchangeRate.of(USD, EUR, new BigDecimal("0.90")), Validity.always()))
                .getSuccess().events().first().payload().version()
        def command = expense(id, Money.of(10, "USD"))
        def engine = Mock(ValuationEngine)
        def handler = new RecordExpenseHandler(configuration.repository, CLOCK, engine, { OVERRIDE_VERSION })

        when: "the Expense is recorded"
        def payload = (ExpenseRecorded) handler.handle(command).getSuccess().events().first().payload()

        then: "it is valued with the configured Exchange Rate and its Valuation and Share follow the doubled amount"
        1 * engine.value(command.amount(), EUR, selected) >> CHANGED_PRICING.value(command.amount(), EUR, selected)
        0 * engine._
        payload.exchangeRateOverride() == null
        payload.componentVersionId() == selected.id()
        payload.exchangeRate() == selected.exchangeRate()
        payload.valuation() == Money.of(18, "EUR")
        payload.shares() == [new Share(ADA, Money.of(18, "EUR"))]
    }

    def "changed Pricing values a new foreign Expense with the prepared Exchange Rate Override"() {
        given: "a 10 dollar Expense with an Exchange Rate Override and a changed Valuation that doubles amounts"
        SettlementId id = withParticipant()
        def manual = ExchangeRate.of(USD, EUR, new BigDecimal("0.97"))
        def command = new RecordExpense(id, EXPENSE, new ExpenseDescription("Lunch"), DATE, ADA, Money.of(10, "USD"),
                new EqualShareAllocation([ADA]), manual)
        def prepared = new ExchangeRateOverride(manual, OVERRIDE_VERSION, LocalDateTime.now(CLOCK))
        def engine = Mock(ValuationEngine)
        def handler = new RecordExpenseHandler(configuration.repository, CLOCK, engine, { OVERRIDE_VERSION })

        when: "the Expense is recorded"
        def payload = (ExpenseRecorded) handler.handle(command).getSuccess().events().first().payload()

        then: "it is valued with the Exchange Rate Override and its Valuation and Share follow the doubled amount"
        1 * engine.value(command.amount(), EUR, prepared) >> CHANGED_PRICING.value(command.amount(), EUR, prepared)
        0 * engine._
        payload.exchangeRateOverride() == manual
        payload.componentVersionId() == OVERRIDE_VERSION
        payload.exchangeRate() == manual
        payload.valuation() == Money.of(19.40, "EUR")
        payload.shares() == [new Share(ADA, Money.of(19.40, "EUR"))]
    }

    private SettlementId withParticipant() {
        SettlementId id = configuration.openSettlement("Holiday")
        configuration.addHandler.handle(new AddParticipant(id, ADA, new ParticipantName("Ada")))
        id
    }

    private static RecordExpense expense(SettlementId id, Money amount) {
        new RecordExpense(id, EXPENSE, new ExpenseDescription("Lunch"), DATE, ADA, amount,
                new EqualShareAllocation([ADA]))
    }
}
