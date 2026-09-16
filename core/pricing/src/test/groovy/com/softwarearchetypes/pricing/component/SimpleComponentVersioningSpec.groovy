package com.softwarearchetypes.pricing.component

import com.softwarearchetypes.pricing.calculation.Calculator
import com.softwarearchetypes.pricing.calculation.CalculatorId
import com.softwarearchetypes.pricing.calculation.CalculatorRange
import com.softwarearchetypes.pricing.calculation.Calculators
import com.softwarearchetypes.pricing.calculation.Interpretation
import com.softwarearchetypes.pricing.calculation.MarginalPrice
import com.softwarearchetypes.pricing.calculation.ParameterKey
import com.softwarearchetypes.pricing.calculation.Parameters
import com.softwarearchetypes.pricing.calculation.PricingResult
import com.softwarearchetypes.pricing.calculation.StepBoundary
import com.softwarearchetypes.pricing.calculation.TotalPrice
import com.softwarearchetypes.pricing.calculation.UnitPrice

import com.softwarearchetypes.quantity.money.Money
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import spock.lang.Specification

class SimpleComponentVersioningSpec extends Specification {

    private final Clock clock = Clock.fixed(Instant.parse("2025-01-15T12:50:00Z"), ZoneOffset.UTC)

    def "component is created with an initial version"() {
        given:
        Calculator calculator = Calculators.fixed("fixed-100", Money.of(100, "PLN"))
        Validity validity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))

        and:
        Component component = SimpleComponent.withInitialVersion(
                "Base Price",
                calculator,
                validity,
                clock
        )

        expect:
        component.name() == "Base Price"
        component.id() != null
    }

    def "calculation uses the version valid at the given timestamp"() {
        given:
        Calculator calculator = Calculators.fixed("fixed-100", Money.of(100, "PLN"))
        Validity validity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        Component component = SimpleComponent.withInitialVersion("Base Price", calculator, validity, clock)

        and:
        Parameters params = Parameters.of("timestamp", LocalDateTime.of(2024, 1, 15, 0, 0))
        PricingResult result = component.calculate(params)

        expect:
        result.money() == Money.of(100, "PLN")
    }

    def "adding a new version keeps the old one for its validity period"() {
        given:
        Calculator baseCalculator = Calculators.fixed("fixed-100", Money.of(100, "PLN"))
        Validity baseValidity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        SimpleComponent component = SimpleComponent.withInitialVersion("Base Price", baseCalculator, baseValidity, clock)

        and:
        Calculator discountCalculator = Calculators.fixed("fixed-80", Money.of(80, "PLN"))
        Validity discountValidity = Validity.between(
                LocalDateTime.of(2024, 2, 1, 0, 0),
                LocalDateTime.of(2024, 3, 1, 0, 0)
        )
        SimpleComponentVersion discountVersion = new SimpleComponentVersion(
                discountCalculator,
                Map.of(),
                discountValidity,
                LocalDateTime.now(clock)
        )
        SimpleComponent updated = component.updateWith(discountVersion)

        and:
        Parameters jan15 = Parameters.of("timestamp", LocalDateTime.of(2024, 1, 15, 0, 0))

        expect:
        updated.calculate(jan15).money() == Money.of(100, "PLN")

        Parameters feb15 = Parameters.of("timestamp", LocalDateTime.of(2024, 2, 15, 0, 0))
        updated.calculate(feb15).money() == Money.of(80, "PLN")

        Parameters mar15 = Parameters.of("timestamp", LocalDateTime.of(2024, 3, 15, 0, 0))
        updated.calculate(mar15).money() == Money.of(100, "PLN")
    }

    def "when versions overlap the one with the youngest valid.from wins"() {
        given:
        Calculator baseCalculator = Calculators.fixed("base", Money.of(100, "PLN"))
        Validity baseValidity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        SimpleComponent component = SimpleComponent.withInitialVersion("Price", baseCalculator, baseValidity, clock)

        and:
        Calculator calc1 = Calculators.fixed("v1", Money.of(80, "PLN"))
        Validity validity1 = Validity.from(LocalDateTime.of(2024, 2, 1, 0, 0))
        component = component.updateWith(new SimpleComponentVersion(calc1, Map.of(), validity1, LocalDateTime.now(clock)))
        Calculator calc2 = Calculators.fixed("v2", Money.of(90, "PLN"))
        Validity validity2 = Validity.from(LocalDateTime.of(2024, 2, 10, 0, 0))
        component = component.updateWith(new SimpleComponentVersion(calc2, Map.of(), validity2, LocalDateTime.now(clock)))

        and:
        Parameters feb15 = Parameters.of("timestamp", LocalDateTime.of(2024, 2, 15, 0, 0))

        expect:
        component.calculate(feb15).money() == Money.of(90, "PLN")
    }

    def "calculation fails when no version is valid at the given timestamp"() {
        given:
        Calculator calculator = Calculators.fixed("fixed", Money.of(100, "PLN"))
        Validity validity = Validity.from(LocalDateTime.of(2024, 2, 1, 0, 0))
        Component component = SimpleComponent.withInitialVersion("Price", calculator, validity, clock)

        and:
        Parameters jan15 = Parameters.of("timestamp", LocalDateTime.of(2024, 1, 15, 0, 0))

        when:
        component.calculate(jan15)

        then:
        def ex = thrown(IllegalStateException)
        ex.message.contains("No version of component")
        ex.message.contains("valid at 2024-01-15")
    }

    def "calculation falls back to current time when no timestamp is provided"() {
        given:
        Calculator calculator = Calculators.fixed("fixed", Money.of(100, "PLN"))
        Validity validity = Validity.from(LocalDateTime.of(2020, 1, 1, 0, 0))
        Component component = SimpleComponent.withInitialVersion("Price", calculator, validity, clock)

        and:
        Parameters params = Parameters.empty()
        PricingResult result = component.calculate(params)

        expect:
        result.money() == Money.of(100, "PLN")
    }

    def "version with identical validity is rejected by default"() {
        given:
        Calculator calculator1 = Calculators.fixed("v1", Money.of(100, "PLN"))
        Validity validity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        SimpleComponent component = SimpleComponent.withInitialVersion("Price", calculator1, validity, clock)

        and:
        Calculator calculator2 = Calculators.fixed("v2", Money.of(200, "PLN"))
        SimpleComponentVersion duplicate = new SimpleComponentVersion(calculator2, Map.of(), validity, LocalDateTime.now(clock))

        when:
        component.updateWith(duplicate)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("identical validity period")
    }

    def "version with identical validity is allowed with the ALLOW_ALL strategy"() {
        given:
        Calculator calculator1 = Calculators.fixed("v1", Money.of(100, "PLN"))
        Validity validity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        SimpleComponent component = SimpleComponent.withInitialVersion("Price", calculator1, validity, clock)

        and:
        Calculator calculator2 = Calculators.fixed("v2", Money.of(200, "PLN"))
        SimpleComponentVersion duplicate = new SimpleComponentVersion(calculator2, Map.of(), validity, LocalDateTime.now(clock).plusMinutes(10))
        SimpleComponent updated = component.updateWith(duplicate, VersionUpdateStrategy.ALLOW_ALL)

        and:
        Parameters params = Parameters.of("timestamp", LocalDateTime.of(2024, 1, 15, 0, 0))

        expect:
        updated.calculate(params).money() == Money.of(200, "PLN")
    }

    def "overlapping versions are rejected with the REJECT_OVERLAPPING strategy"() {
        given:
        Calculator calculator1 = Calculators.fixed("v1", Money.of(100, "PLN"))
        Validity validity1 = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        SimpleComponent component = SimpleComponent.withInitialVersion("Price", calculator1, validity1, clock)

        and:
        Calculator calculator2 = Calculators.fixed("v2", Money.of(80, "PLN"))
        Validity validity2 = Validity.between(
                LocalDateTime.of(2024, 2, 1, 0, 0),
                LocalDateTime.of(2024, 3, 1, 0, 0)
        )
        SimpleComponentVersion overlapping = new SimpleComponentVersion(calculator2, Map.of(), validity2, LocalDateTime.now(clock))

        when:
        component.updateWith(overlapping, VersionUpdateStrategy.REJECT_OVERLAPPING)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("overlaps")
    }

    def "only the selected version's mapped calculator contract is validated"() {
        given:
        Calculator legacyCalculator = calculatorWithInput(
                new ParameterKey<>("amount", String), Money.of(10, "PLN"))
        Calculator currentCalculator = calculatorWithInput(
                new ParameterKey<>("quantity", BigDecimal.class), Money.of(20, "PLN"))
        SimpleComponent component = SimpleComponent.withInitialVersion(
                "Versioned price",
                legacyCalculator,
                Map.of("legacyAmount", "amount"),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                clock)
        component = component.updateWith(new SimpleComponentVersion(
                currentCalculator,
                Map.of("currentQuantity", "quantity"),
                Validity.from(LocalDateTime.of(2024, 2, 1, 0, 0)),
                LocalDateTime.of(2024, 1, 15, 0, 0)))

        expect:
        component.calculate(Parameters.of(
                "timestamp", LocalDateTime.of(2024, 1, 15, 0, 0),
                "legacyAmount", "legacy")).money() == Money.of(10, "PLN")
        component.calculate(Parameters.of(
                "timestamp", LocalDateTime.of(2024, 2, 15, 0, 0),
                "currentQuantity", BigDecimal.ONE)).money() == Money.of(20, "PLN")
    }

    def "applicability is checked before selected calculator validation"() {
        given:
        Calculator calculator = calculatorWithInput(new ParameterKey<>("quantity", BigDecimal.class), Money.of(10, "PLN"))
        SimpleComponent component = SimpleComponent.withInitialVersion(
                "conditional price",
                calculator,
                Map.of(),
                ApplicabilityConstraint.equalsTo("customer", "excluded"),
                Validity.always(),
                clock)

        when:
        PricingResult result = component.calculate(Parameters.of("customer", "included"))

        then:
        result.money() == Money.zero("PLN")
    }

    private static Calculator calculatorWithInput(ParameterKey input, Money result) {
        [
                calculateWithValidInputs: { Parameters parameters -> parameters.get(input); new TotalPrice(result) }
        ] as Calculator
    }

    def "versioned component works with parameter mappings"() {
        given:
        Calculator calculator = Calculators.stepFunction(
                "step",
                Money.of(100, "PLN"),
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(5)
        )
        Map<String, String> mappings = Map.of("kwh", "quantity")
        Validity validity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))

        Component component = SimpleComponent.withInitialVersion(
                "Energy Charge",
                calculator,
                mappings,
                validity,
                clock
        )

        and:
        Parameters params = Parameters.of(
                "timestamp", LocalDateTime.of(2024, 1, 15, 0, 0),
                "kwh", BigDecimal.valueOf(15)
        )
        PricingResult result = component.calculate(params)

        expect:
        result.money() == Money.of(105, "PLN")
    }
}
