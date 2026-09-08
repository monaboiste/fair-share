package com.softwarearchetypes.pricing

import static com.softwarearchetypes.pricing.ClockFixture.someFixedClock
import static java.time.Clock.fixed
import static java.time.LocalDateTime.now

import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Map
import spock.lang.Specification

class SimpleComponentVersioningSpec extends Specification {

    static final Clock clock = someFixedClock()
    def "shouldCreateComponentWithInitialVersion"() {        given:
        Calculator calculator = new SimpleFixedCalculator("fixed-100", Money.of(100, "PLN"))
        Validity validity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        and:
        Component component = SimpleComponent.withInitialVersion(
                "Base Price",
                calculator,
                validity,
                clock
        )
        and:
        assert component.name() == "Base Price"
        assert component.id() != null
    }
    def "shouldCalculateUsingVersionValidAtGivenTimestamp"() {        given:
        Calculator calculator = new SimpleFixedCalculator("fixed-100", Money.of(100, "PLN"))
        Validity validity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        Component component = SimpleComponent.withInitialVersion("Base Price", calculator, validity, clock)
        and:
        Parameters params = Parameters.of("timestamp", LocalDateTime.of(2024, 1, 15, 0, 0))
        Money result = component.calculate(params)
        and:
        assert result == Money.of(100, "PLN")
    }
    def "shouldAddNewVersionAndKeepOldOne"() {        given:
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
                now(clock)
        )
        SimpleComponent updated = component.updateWith(discountVersion)
        and:
        Parameters jan15 = Parameters.of("timestamp", LocalDateTime.of(2024, 1, 15, 0, 0))
        assert updated.calculate(jan15) == Money.of(100, "PLN")

        Parameters feb15 = Parameters.of("timestamp", LocalDateTime.of(2024, 2, 15, 0, 0))
        assert updated.calculate(feb15) == Money.of(80, "PLN")

        Parameters mar15 = Parameters.of("timestamp", LocalDateTime.of(2024, 3, 15, 0, 0))
        assert updated.calculate(mar15) == Money.of(100, "PLN")
    }
    def "shouldUseYoungestValidFromWhenVersionsOverlap"() {        given:
        Calculator baseCalculator = new SimpleFixedCalculator("base", Money.of(100, "PLN"))
        Validity baseValidity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        SimpleComponent component = SimpleComponent.withInitialVersion("Price", baseCalculator, baseValidity, clock)
        and:
        Calculator calc1 = new SimpleFixedCalculator("v1", Money.of(80, "PLN"))
        Validity validity1 = Validity.from(LocalDateTime.of(2024, 2, 1, 0, 0))
        component = component.updateWith(new SimpleComponentVersion(calc1, Map.of(), validity1, now(clock)))
        Calculator calc2 = new SimpleFixedCalculator("v2", Money.of(90, "PLN"))
        Validity validity2 = Validity.from(LocalDateTime.of(2024, 2, 10, 0, 0))
        component = component.updateWith(new SimpleComponentVersion(calc2, Map.of(), validity2, now(clock)))
        and:
        Parameters feb15 = Parameters.of("timestamp", LocalDateTime.of(2024, 2, 15, 0, 0))
        assert component.calculate(feb15) == Money.of(90, "PLN")
    }
    def "shouldThrowExceptionWhenNoVersionValidAtTimestamp"() {        given:
        Calculator calculator = new SimpleFixedCalculator("fixed", Money.of(100, "PLN"))
        Validity validity = Validity.from(LocalDateTime.of(2024, 2, 1, 0, 0))
        Component component = SimpleComponent.withInitialVersion("Price", calculator, validity, clock)
        and:
        Parameters jan15 = Parameters.of("timestamp", LocalDateTime.of(2024, 1, 15, 0, 0))
        and:
        def exception = shouldFail(IllegalStateException) { component.calculate(jan15) }
        assert exception.message.contains("No version of component")
        assert exception.message.contains("valid at 2024-01-15")
    }
    def "shouldFallbackToCurrentTimeWhenTimestampNotProvided"() {        given:
        Calculator calculator = new SimpleFixedCalculator("fixed", Money.of(100, "PLN"))
        Validity validity = Validity.from(LocalDateTime.of(2020, 1, 1, 0, 0))
        Component component = SimpleComponent.withInitialVersion("Price", calculator, validity, clock)
        and:
        Parameters params = Parameters.empty()
        Money result = component.calculate(params)
        and:
        assert result == Money.of(100, "PLN")
    }
    def "shouldRejectVersionWithIdenticalValidityByDefault"() {        given:
        Calculator calculator1 = new SimpleFixedCalculator("v1", Money.of(100, "PLN"))
        Validity validity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        SimpleComponent component = SimpleComponent.withInitialVersion("Price", calculator1, validity, clock)
        and:
        Calculator calculator2 = new SimpleFixedCalculator("v2", Money.of(200, "PLN"))
        SimpleComponentVersion duplicate = new SimpleComponentVersion(calculator2, Map.of(), validity, now(clock))
        and:
        shouldFail(IllegalArgumentException) { component.updateWith(duplicate) }.message.contains("identical validity period")
    }
    def "shouldAllowVersionWithIdenticalValidityWhenUsingALLOW ALL"() {        given:
        Calculator calculator1 = new SimpleFixedCalculator("v1", Money.of(100, "PLN"))
        Validity validity = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        SimpleComponent component = SimpleComponent.withInitialVersion("Price", calculator1, validity, clock)
        and:
        Calculator calculator2 = new SimpleFixedCalculator("v2", Money.of(200, "PLN"))
        SimpleComponentVersion duplicate = new SimpleComponentVersion(calculator2, Map.of(), validity, now(clock).plusMinutes(10))
        SimpleComponent updated = component.updateWith(duplicate, VersionUpdateStrategy.ALLOW_ALL)
        and:
        Parameters params = Parameters.of("timestamp", LocalDateTime.of(2024, 1, 15, 0, 0))
        assert updated.calculate(params) == Money.of(200, "PLN")
    }
    def "shouldRejectOverlappingVersionsWhenUsingREJECT OVERLAPPING"() {        given:
        Calculator calculator1 = new SimpleFixedCalculator("v1", Money.of(100, "PLN"))
        Validity validity1 = Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        SimpleComponent component = SimpleComponent.withInitialVersion("Price", calculator1, validity1, clock)
        and:
        Calculator calculator2 = new SimpleFixedCalculator("v2", Money.of(80, "PLN"))
        Validity validity2 = Validity.between(
                LocalDateTime.of(2024, 2, 1, 0, 0),
                LocalDateTime.of(2024, 3, 1, 0, 0)
        )
        SimpleComponentVersion overlapping = new SimpleComponentVersion(calculator2, Map.of(), validity2, now(clock))
        and:
        shouldFail(IllegalArgumentException) { component.updateWith(overlapping, VersionUpdateStrategy.REJECT_OVERLAPPING) }.message.contains("overlaps")
    }
    def "shouldWorkWithParameterMappings"() {        given:
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
        and:
        assert result == Money.of(105, "PLN")
    }

    private static Throwable shouldFail(Class<? extends Throwable> type, Closure action) {
        try {
            action.call()
        } catch (Throwable exception) {
            assert type.isInstance(exception)
            return exception
        }
        throw new AssertionError("Expected " + type.simpleName)
    }
}
