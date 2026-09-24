package com.github.monaboiste.fairshare.pricing.component

import static com.github.monaboiste.fairshare.pricing.calculation.PricingContext.CURRENCY
import static com.github.monaboiste.fairshare.pricing.calculation.PricingContext.TIMESTAMP

import com.github.monaboiste.fairshare.pricing.calculation.Calculators
import com.github.monaboiste.fairshare.pricing.calculation.Parameters
import com.github.monaboiste.fairshare.quantity.money.Money
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.money.CurrencyUnit
import javax.money.Monetary
import spock.lang.Specification

class ComponentCurrencyContextSpec extends Specification {

    private static final CurrencyUnit USD = Monetary.getCurrency("USD")
    private static final LocalDateTime JANUARY = LocalDateTime.of(2025, 1, 15, 0, 0)
    private static final LocalDateTime MARCH = LocalDateTime.of(2025, 3, 15, 0, 0)
    private static final Clock CLOCK = Clock.fixed(JANUARY.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    def "a #kind component refuses to evaluate without a currency, even when it does not apply"() {
        when:
        component.calculate(Parameters.of("customer", "regular"))

        then:
        IllegalArgumentException missing = thrown()
        missing.message.contains(CURRENCY.name())

        where:
        kind        | component
        "simple"    | premiumFee("fee", 10)
        "composite" | premiumBundle(fee("fee", 10))
    }

    def "a non-applicable #kind component contributes zero in the evaluation currency"() {
        expect:
        component.calculate(inDollars("customer", "regular")).money() == Money.zero("USD")

        where:
        kind        | component
        "simple"    | premiumFee("fee", 10)
        "composite" | premiumBundle(fee("fee", 10))
    }

    def "a composite sums applicable children with non-applicable ones in a non-PLN currency"() {
        given:
        Component total = Component.composite("total", fee("base", 10), premiumFee("premium", 5))

        when:
        ComponentBreakdown breakdown = total.calculateBreakdown(inDollars("customer", "regular"))

        then:
        breakdown.total() == Money.of(10, "USD")
        breakdown.children()*.total() == [Money.of(10, "USD"), Money.zero("USD")]
    }

    def "a composite whose children do not apply totals zero in the evaluation currency"() {
        given:
        Component total = Component.composite("total", premiumFee("a", 10), premiumBundle(fee("b", 5)))

        expect:
        total.calculate(inDollars("customer", "regular")).money() == Money.zero("USD")
    }

    def "a composite without children totals zero in the evaluation currency"() {
        given:
        Component empty = CompositeComponent.of("empty", List.of())

        expect:
        empty.calculate(inDollars()).money() == Money.zero("USD")
    }

    def "a component rejects a result in a currency other than the evaluation currency"() {
        given:
        Component zloty = Component.simple("fee", Calculators.fixed("fee", Money.of(10, "PLN")))

        when:
        zloty.calculate(inDollars())

        then:
        IllegalStateException mismatch = thrown()
        mismatch.message.contains("PLN")
        mismatch.message.contains("USD")
    }

    def "a gap in the #kind configuration history stays an error even when the configuration would not apply"() {
        when:
        component.calculate(inDollars(TIMESTAMP.name(), JANUARY, "customer", "regular"))

        then:
        IllegalStateException gap = thrown()
        gap.message.contains("No version")

        where:
        kind        | component
        "simple"    | SimpleComponent.withInitialVersion("fee", dollars("fee", 10), Map.of(), premiumCustomers(),
                Validity.from(MARCH), CLOCK)
        "composite" | CompositeComponent.withInitialVersion("total", List.of(fee("fee", 10)), Map.of(),
                premiumCustomers(), Validity.from(MARCH), CLOCK)
    }

    def "applicability is evaluated for the version selected by validity"() {
        given:
        SimpleComponent fee = SimpleComponent.withInitialVersion("fee", dollars("regular", 10), Map.of(),
                ApplicabilityConstraint.alwaysTrue(), Validity.until(MARCH.minusDays(1)), CLOCK)
                .updateWith(SimpleComponentVersion.of(dollars("premium", 5), Map.of(), premiumCustomers(),
                        Validity.from(MARCH), CLOCK))

        expect:
        fee.calculate(inDollars(TIMESTAMP.name(), JANUARY, "customer", "regular")).money() == Money.of(10, "USD")
        fee.calculate(inDollars(TIMESTAMP.name(), MARCH, "customer", "regular")).money() == Money.zero("USD")
        fee.calculate(inDollars(TIMESTAMP.name(), MARCH, "customer", "premium")).money() == Money.of(5, "USD")
    }

    def "a validity-bound constraint applies only at timestamps within its validity"() {
        given:
        Component fee = Component.simple("fee", dollars("fee", 10),
                ApplicabilityConstraint.validAt(Validity.from(MARCH)))

        expect:
        fee.calculate(inDollars(TIMESTAMP.name(), at)).money() == expected

        where:
        at      | expected
        JANUARY | Money.zero("USD")
        MARCH   | Money.of(10, "USD")
    }

    private static Parameters inDollars(Object... keysAndValues) {
        Parameters parameters = Parameters.of(CURRENCY, USD)
        for (int i = 0; i < keysAndValues.length; i += 2) {
            parameters = parameters.with(keysAndValues[i] as String, keysAndValues[i + 1])
        }
        parameters
    }

    private static Component fee(String name, int amount) {
        Component.simple(name, dollars(name, amount))
    }

    private static Component premiumFee(String name, int amount) {
        Component.simple(name, dollars(name, amount), premiumCustomers())
    }

    private static Component premiumBundle(Component component) {
        Component.composite(component.name() + "-bundle", Map.of(), premiumCustomers(), component)
    }

    private static ApplicabilityConstraint premiumCustomers() {
        ApplicabilityConstraint.equalsTo("customer", "premium")
    }

    private static def dollars(String name, int amount) {
        Calculators.fixed(name, Money.of(amount, "USD"))
    }
}
