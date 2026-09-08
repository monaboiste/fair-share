package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Map
import spock.lang.Specification

class SimpleComponentVersioningSpec extends Specification {

    static final Clock clock = ClockFixture.someFixedClock()

    def "component is created with an initial version"() {

        given:
        Calculator calculator = new SimpleFixedCalculator("fixed-100", Money.of(100, "PLN"))
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
        Calculator calculator = new SimpleFixedCalculator("fixed-100", Money.of(100, "PLN"))
        Validity validity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        Component component = SimpleComponent.withInitialVersion("Base Price", calculator, validity, clock)

        and:
        Parameters params = Parameters.of("timestamp", LocalDateTime.of(2024, 1, 15, 0, 0))
        Money result = component.calculate(params)

        expect:

        result == Money.of(100, "PLN")
    }

    def "adding a new version keeps the old one for its validity period"() {

        given:
        Calculator baseCalculator = new SimpleFixedCalculator("fixed-100", Money.of(100, "PLN"))
        Validity baseValidity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        SimpleComponent component = SimpleComponent.withInitialVersion("Base Price", baseCalculator, baseValidity, clock)

        and:
        Calculator discountCalculator = new SimpleFixedCalculator("fixed-80", Money.of(80, "PLN"))
        Validity discountValidity = Validity.between(
                LocalDateTime.of(2024, 2, 1, 0, 0),
                LocalDateTime.of(2024, 3, 1, 0, 0)
        )
        SimpleComponentVersion discountVersion = new SimpleComponentVersion(
                discountCalculator,
                Map.of(),
                discountValidity,
                java.time.LocalDateTime.now(clock)
        )
        SimpleComponent updated = component.updateWith(discountVersion)

        and:
        Parameters jan15 = Parameters.of("timestamp", LocalDateTime.of(2024, 1, 15, 0, 0))

        expect:

        updated.calculate(jan15) == Money.of(100, "PLN")

        Parameters feb15 = Parameters.of("timestamp", LocalDateTime.of(2024, 2, 15, 0, 0))
        updated.calculate(feb15) == Money.of(80, "PLN")

        Parameters mar15 = Parameters.of("timestamp", LocalDateTime.of(2024, 3, 15, 0, 0))
        updated.calculate(mar15) == Money.of(100, "PLN")
    }

    def "when versions overlap the one with the youngest validFrom wins"() {

        given:
        Calculator baseCalculator = new SimpleFixedCalculator("base", Money.of(100, "PLN"))
        Validity baseValidity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        SimpleComponent component = SimpleComponent.withInitialVersion("Price", baseCalculator, baseValidity, clock)

        and:
        Calculator calc1 = new SimpleFixedCalculator("v1", Money.of(80, "PLN"))
        Validity validity1 = Validity.from(LocalDateTime.of(2024, 2, 1, 0, 0))
        component = component.updateWith(new SimpleComponentVersion(calc1, Map.of(), validity1, java.time.LocalDateTime.now(clock)))
        Calculator calc2 = new SimpleFixedCalculator("v2", Money.of(90, "PLN"))
        Validity validity2 = Validity.from(LocalDateTime.of(2024, 2, 10, 0, 0))
        component = component.updateWith(new SimpleComponentVersion(calc2, Map.of(), validity2, java.time.LocalDateTime.now(clock)))

        and:
        Parameters feb15 = Parameters.of("timestamp", LocalDateTime.of(2024, 2, 15, 0, 0))

        expect:

        component.calculate(feb15) == Money.of(90, "PLN")
    }

    def "calculation fails when no version is valid at the given timestamp"() {

        given:
        Calculator calculator = new SimpleFixedCalculator("fixed", Money.of(100, "PLN"))
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
        Calculator calculator = new SimpleFixedCalculator("fixed", Money.of(100, "PLN"))
        Validity validity = Validity.from(LocalDateTime.of(2020, 1, 1, 0, 0))
        Component component = SimpleComponent.withInitialVersion("Price", calculator, validity, clock)

        and:
        Parameters params = Parameters.empty()
        Money result = component.calculate(params)

        expect:

        result == Money.of(100, "PLN")
    }

    def "version with identical validity is rejected by default"() {

        given:
        Calculator calculator1 = new SimpleFixedCalculator("v1", Money.of(100, "PLN"))
        Validity validity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        SimpleComponent component = SimpleComponent.withInitialVersion("Price", calculator1, validity, clock)

        and:
        Calculator calculator2 = new SimpleFixedCalculator("v2", Money.of(200, "PLN"))
        SimpleComponentVersion duplicate = new SimpleComponentVersion(calculator2, Map.of(), validity, java.time.LocalDateTime.now(clock))

        when:
        component.updateWith(duplicate)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("identical validity period")
    }

    def "version with identical validity is allowed with the ALLOW_ALL strategy"() {

        given:
        Calculator calculator1 = new SimpleFixedCalculator("v1", Money.of(100, "PLN"))
        Validity validity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        SimpleComponent component = SimpleComponent.withInitialVersion("Price", calculator1, validity, clock)

        and:
        Calculator calculator2 = new SimpleFixedCalculator("v2", Money.of(200, "PLN"))
        SimpleComponentVersion duplicate = new SimpleComponentVersion(calculator2, Map.of(), validity, java.time.LocalDateTime.now(clock).plusMinutes(10))
        SimpleComponent updated = component.updateWith(duplicate, VersionUpdateStrategy.ALLOW_ALL)

        and:
        Parameters params = Parameters.of("timestamp", LocalDateTime.of(2024, 1, 15, 0, 0))

        expect:

        updated.calculate(params) == Money.of(200, "PLN")
    }

    def "overlapping versions are rejected with the REJECT_OVERLAPPING strategy"() {

        given:
        Calculator calculator1 = new SimpleFixedCalculator("v1", Money.of(100, "PLN"))
        Validity validity1 = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        SimpleComponent component = SimpleComponent.withInitialVersion("Price", calculator1, validity1, clock)

        and:
        Calculator calculator2 = new SimpleFixedCalculator("v2", Money.of(80, "PLN"))
        Validity validity2 = Validity.between(
                LocalDateTime.of(2024, 2, 1, 0, 0),
                LocalDateTime.of(2024, 3, 1, 0, 0)
        )
        SimpleComponentVersion overlapping = new SimpleComponentVersion(calculator2, Map.of(), validity2, java.time.LocalDateTime.now(clock))

        when:
        component.updateWith(overlapping, VersionUpdateStrategy.REJECT_OVERLAPPING)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("overlaps")
    }

    def "versioned component works with parameter mappings"() {

        given:
        Calculator calculator = new StepFunctionCalculator(
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
        Money result = component.calculate(params)

        expect:

        result == Money.of(105, "PLN")
    }
}
