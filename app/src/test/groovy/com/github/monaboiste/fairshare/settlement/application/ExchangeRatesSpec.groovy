package com.github.monaboiste.fairshare.settlement.application

import com.github.monaboiste.fairshare.common.commands.RegisteredCommandDispatcher
import com.github.monaboiste.fairshare.pricing.component.ComponentVersionId
import com.github.monaboiste.fairshare.pricing.component.Validity
import com.github.monaboiste.fairshare.quantity.money.Money
import com.github.monaboiste.fairshare.settlement.application.command.ConfigureExchangeRate
import com.github.monaboiste.fairshare.settlement.application.command.RenameSettlement
import com.github.monaboiste.fairshare.settlement.application.command.handler.ConfigureExchangeRateHandler
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlement
import com.github.monaboiste.fairshare.settlement.application.query.GetSettlementHistory
import com.github.monaboiste.fairshare.settlement.domain.ExchangeRateTargetMismatch
import com.github.monaboiste.fairshare.settlement.domain.ExplicitIdentityExchangeRate
import com.github.monaboiste.fairshare.settlement.domain.SettlementName
import com.github.monaboiste.fairshare.settlement.domain.SettlementNotFound
import com.github.monaboiste.fairshare.settlement.domain.event.ExchangeRateConfigured
import com.github.monaboiste.fairshare.settlement.infrastructure.SettlementProjector
import com.github.monaboiste.fairshare.valuation.ExchangeRate
import com.github.monaboiste.fairshare.valuation.ExchangeRateVersions
import com.github.monaboiste.fairshare.valuation.ValuationEngine
import java.time.LocalDateTime
import javax.money.Monetary
import spock.lang.Specification

class ExchangeRatesSpec extends Specification {
    def configuration = new SettlementTestConfiguration()
    def from = LocalDateTime.parse("2026-02-01T00:00:00")
    def validity = Validity.from(from)

    def "configuring an Exchange Rate retains its version in the view and history"() {
        given:
        def id = configuration.openSettlement("Holiday")
        def rate = ExchangeRate.of(configuration.USD, configuration.EUR, new BigDecimal("0.90"))

        when:
        def commit = configuration.commands.dispatch(new ConfigureExchangeRate(id, rate, validity)).getSuccess()
        def view = configuration.queries.dispatch(new GetSettlement(id)).getSuccess()
        def history = configuration.queries.dispatch(new GetSettlementHistory(id)).getSuccess()
        def rebuilt = new SettlementProjector()
        rebuilt.rebuild(configuration.store)

        then:
        commit.version() == 2
        commit.events().size() == 1
        def event = commit.events().first().payload()
        event instanceof ExchangeRateConfigured
        event.type() == "ExchangeRateConfigured"
        event.schemaVersion() == 1
        event.version().exchangeRate() == rate
        event.version().id() == view.exchangeRates().first().id()
        event.version().validity() == validity
        event.version().definedAt() == LocalDateTime.ofInstant(configuration.NOW, configuration.CLOCK.zone)
        view.exchangeRates() == [event.version()]
        rebuilt.findById(id).orElseThrow() == view
        history*.sequence() == [1L, 2L]
        history.last().payload() == event
    }

    def "an identical numeric retry keeps the stream version while later changes append a version"() {
        given:
        def id = configuration.openSettlement("Holiday")
        def first = configuration.commands.dispatch(new ConfigureExchangeRate(id, rate("0.90"), validity))
            .getSuccess().events().first().payload().version()

        when:
        def retry = configuration.commands.dispatch(new ConfigureExchangeRate(id, rate("0.900"), validity))
            .getSuccess()
        def next = configuration.commands.dispatch(new ConfigureExchangeRate(id, rate("0.95"), validity))
            .getSuccess()
        def view = configuration.queries.dispatch(new GetSettlement(id)).getSuccess()
        def history = configuration.queries.dispatch(new GetSettlementHistory(id)).getSuccess()

        then:
        retry.version() == 2
        retry.events().empty
        next.version() == 3
        next.events().size() == 1
        next.events().first().payload().version().id() != first.id()
        view.exchangeRates().first() == first
        view.exchangeRates()*.exchangeRate()*.value() == ["0.90", "0.95"]*.toBigDecimal()
        view.exchangeRates().last() == next.events().first().payload().version()
        history*.sequence() == [1L, 2L, 3L]
    }

    def "a correction back to an earlier Exchange Rate appends a new version after interleaved tuples"() {
        given:
        def id = configuration.openSettlement("Holiday")
        def earlier = Validity.from(from.minusDays(1))
        def commands = [
            new ConfigureExchangeRate(id, rate("0.90"), validity),
            new ConfigureExchangeRate(id, rate("0.95"), validity),
            new ConfigureExchangeRate(id, rate("0.90"), earlier),
            new ConfigureExchangeRate(id, ExchangeRate.of(configuration.USD, configuration.EUR,
                new BigDecimal("1.10")), Validity.always()),
            new ConfigureExchangeRate(id, rate("0.90"), validity)
        ]

        when:
        def commits = commands.collect { configuration.commands.dispatch(it).getSuccess() }
        def history = configuration.queries.dispatch(new GetSettlementHistory(id)).getSuccess()
        def view = configuration.queries.dispatch(new GetSettlement(id)).getSuccess()
        def rebuilt = new SettlementProjector()
        rebuilt.rebuild(configuration.store)
        def retry = configuration.commands.dispatch(commands.last()).getSuccess()

        then:
        commits*.version() == [2L, 3L, 4L, 5L, 6L]
        history*.sequence() == [1L, 2L, 3L, 4L, 5L, 6L]
        view.exchangeRates()*.id() == commits*.events()*.first()*.payload()*.version()*.id()
        view.exchangeRates()*.validity() == [validity, validity, earlier, Validity.always(), validity]
        view.exchangeRates()*.exchangeRate()*.value() ==
            ["0.90", "0.95", "0.90", "1.10", "0.90"]*.toBigDecimal()
        rebuilt.findById(id).orElseThrow() == view
        retry.events().empty
        retry.version() == 6
    }

    def "Exchange Rate configuration rejects a wrong target or explicit identity without changing history"() {
        given:
        def id = configuration.openSettlement("Holiday")

        when:
        def wrongTarget = configuration.commands.dispatch(new ConfigureExchangeRate(id,
            ExchangeRate.of(configuration.EUR, configuration.USD, BigDecimal.ONE), validity))
        def identity = configuration.commands.dispatch(new ConfigureExchangeRate(id,
            ExchangeRate.of(configuration.EUR, configuration.EUR, BigDecimal.ONE), validity))
        def missing = configuration.commands.dispatch(new ConfigureExchangeRate(configuration.UNKNOWN_ID,
            rate("0.90"), validity))

        then:
        wrongTarget.getFailure() == new ExchangeRateTargetMismatch(id, configuration.USD)
        identity.getFailure() == new ExplicitIdentityExchangeRate(id)
        missing.getFailure() == new SettlementNotFound(configuration.UNKNOWN_ID)
        configuration.queries.dispatch(new GetSettlementHistory(id)).getSuccess().size() == 1
    }

    def "unrelated Settlement changes retain configured versions in order and view is immutable"() {
        given:
        def id = configuration.openSettlement("Holiday")
        def first = configuration.commands.dispatch(new ConfigureExchangeRate(id, rate("0.90"), validity))
            .getSuccess().events().first().payload()
        configuration.commands.dispatch(new RenameSettlement(id, new SettlementName("Journey")))

        when:
        def second = configuration.commands.dispatch(new ConfigureExchangeRate(id,
            ExchangeRate.of(Monetary.getCurrency("GBP"), configuration.EUR, BigDecimal.ONE), validity))
            .getSuccess().events().first().payload()
        def view = configuration.queries.dispatch(new GetSettlement(id)).getSuccess()

        then:
        view.name() == "Journey"
        view.exchangeRates() == [first.version(), second.version()]
        configuration.queries.dispatch(new GetSettlementHistory(id)).getSuccess()*.sequence() == [1L, 2L, 3L, 4L]

        when:
        view.exchangeRates().clear()

        then:
        thrown(UnsupportedOperationException)
    }

    def "persisted overlapping Exchange Rates select the latest valid-from Valuation"() {
        given:
        def id = configuration.openSettlement("Holiday")
        def februaryVersionId = new ComponentVersionId(new UUID(0L, 41L))
        def baselineVersionId = new ComponentVersionId(new UUID(0L, 42L))
        def ids = [februaryVersionId, baselineVersionId].iterator()
        def commands = RegisteredCommandDispatcher.builder()
            .register(ConfigureExchangeRate,
                new ConfigureExchangeRateHandler(configuration.repository, configuration.CLOCK, { ids.next() }))
            .build()
        commands.dispatch(new ConfigureExchangeRate(id, rate("0.90"), validity)).getSuccess()
        commands.dispatch(new ConfigureExchangeRate(id, rate("0.80"), Validity.always())).getSuccess()
        def rebuilt = new SettlementProjector()
        rebuilt.rebuild(configuration.store)
        def versionList = rebuilt.findById(id).orElseThrow().exchangeRates()
        ExchangeRateVersions versions = ExchangeRateVersions.from(versionList)

        when:
        def before = versions.applicableAt(configuration.USD, configuration.EUR, from.minusSeconds(1))
        def during = versions.applicableAt(configuration.USD, configuration.EUR, from.plusDays(1))

        then:
        versionList*.id() == [februaryVersionId, baselineVersionId]
        before.isPresent()
        during.isPresent()
        def beforeValuation = ValuationEngine.standard().value(Money.of(10, "USD"), configuration.EUR, before.get())
        def duringValuation = ValuationEngine.standard().value(Money.of(10, "USD"), configuration.EUR, during.get())
        beforeValuation.money() == Money.of(8, "EUR")
        beforeValuation.componentVersion().id() == baselineVersionId
        duringValuation.money() == Money.of(9, "EUR")
        duringValuation.componentVersion().id() == februaryVersionId
    }

    def "persisted equal-start Exchange Rates select the later stream version after replay"() {
        given:
        def id = configuration.openSettlement("Holiday")
        def firstId = new ComponentVersionId(new UUID(0L, 51L))
        def laterId = new ComponentVersionId(new UUID(0L, 52L))
        def ids = [firstId, laterId].iterator()
        def commands = RegisteredCommandDispatcher.builder()
            .register(ConfigureExchangeRate,
                new ConfigureExchangeRateHandler(configuration.repository, configuration.CLOCK, { ids.next() }))
            .build()
        commands.dispatch(new ConfigureExchangeRate(id, rate("0.90"), validity)).getSuccess()
        commands.dispatch(new ConfigureExchangeRate(id, rate("0.95"), validity)).getSuccess()
        def liveVersions = configuration.queries.dispatch(new GetSettlement(id)).getSuccess().exchangeRates()
        def rebuilt = new SettlementProjector()
        rebuilt.rebuild(configuration.store)
        def replayedVersions = rebuilt.findById(id).orElseThrow().exchangeRates()
        ExchangeRateVersions versions = ExchangeRateVersions.from(replayedVersions)

        when:
        def selected = versions.applicableAt(configuration.USD, configuration.EUR, from.plusDays(1))

        then:
        liveVersions == replayedVersions
        replayedVersions*.id() == [firstId, laterId]
        selected.isPresent()
        selected.get().id() == laterId
        def valuation = ValuationEngine.standard().value(Money.of(10, "USD"), configuration.EUR, selected.get())
        valuation.money() == Money.of(9.50, "EUR")
        valuation.componentVersion().id() == laterId
    }

    private ExchangeRate rate(String value) {
        ExchangeRate.of(configuration.USD, configuration.EUR, new BigDecimal(value))
    }
}
