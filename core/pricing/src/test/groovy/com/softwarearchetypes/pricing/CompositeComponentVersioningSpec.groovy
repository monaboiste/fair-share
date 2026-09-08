package com.softwarearchetypes.pricing

import static com.softwarearchetypes.pricing.ClockFixture.someFixedClock
import static java.time.LocalDateTime.now

import com.softwarearchetypes.quantity.money.Money
import java.time.Clock
import java.time.LocalDateTime
import java.util.List
import spock.lang.Specification

class CompositeComponentVersioningSpec extends Specification {

    Clock clock = someFixedClock()
    def "composite is created with an initial version"() {
        given:
        Component basePrice = SimpleComponent.withInitialVersion(
                "Base Price",
                new SimpleFixedCalculator("base", Money.of(100, "PLN")),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                clock
        )
        Component tax = SimpleComponent.withInitialVersion(
                "Tax",
                new SimpleFixedCalculator("tax", Money.of(23, "PLN")),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                clock
        )
        and:
        Component total = CompositeComponent.withInitialVersion(
                "Total Price",
                List.of(basePrice, tax),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                clock
        )
        and:
        assert total.name() == "Total Price"
        Parameters params = Parameters.of("timestamp", LocalDateTime.of(2024, 1, 15, 0, 0))
        assert total.calculate(params) == Money.of(123, "PLN")
    }
    def "composition of children changes over time"() {
        given:
        Component basePrice = SimpleComponent.withInitialVersion(
                "Base Price",
                new SimpleFixedCalculator("base", Money.of(100, "PLN")),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                clock
        )
        Component tax = SimpleComponent.withInitialVersion(
                "Tax",
                new SimpleFixedCalculator("tax", Money.of(23, "PLN")),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                clock
        )

        CompositeComponent total = CompositeComponent.withInitialVersion(
                "Total Price",
                List.of(basePrice, tax),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                clock
        )
        and:
        Component surcharge = SimpleComponent.withInitialVersion(
                "Seasonal Surcharge",
                new SimpleFixedCalculator("surcharge", Money.of(10, "PLN")),
                Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0)),
                clock
        )

        CompositeComponentVersion newVersion = new CompositeComponentVersion(
                List.of(basePrice, tax, surcharge),
                java.util.Map.of(),
                Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0)),
                now(clock).plusMinutes(10)
        )
        total = total.updateWith(newVersion)
        and:
        Parameters april = Parameters.of("timestamp", LocalDateTime.of(2024, 4, 15, 0, 0))
        assert total.calculate(april) == Money.of(123, "PLN")

        Parameters may = Parameters.of("timestamp", LocalDateTime.of(2024, 5, 15, 0, 0))
        assert total.calculate(may) == Money.of(133, "PLN")
    }
    def "versioned children are resolved independently at each point in time"() {
        given:
        Calculator baseCalc = new SimpleFixedCalculator("base", Money.of(100, "PLN"))
        SimpleComponent basePrice = SimpleComponent.withInitialVersion(
                "Base Price",
                baseCalc,
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                clock
        )
        Calculator discountCalc = new SimpleFixedCalculator("discount", Money.of(80, "PLN"))
        SimpleComponentVersion discountVersion = new SimpleComponentVersion(
                discountCalc,
                java.util.Map.of(),
                Validity.between(
                        LocalDateTime.of(2024, 2, 1, 0, 0),
                        LocalDateTime.of(2024, 3, 1, 0, 0)
                ),
                now(clock).plusMinutes(10)
        )
        basePrice = basePrice.updateWith(discountVersion)

        Component tax = SimpleComponent.withInitialVersion(
                "Tax",
                new SimpleFixedCalculator("tax", Money.of(23, "PLN")),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                clock
        )
        and:
        Component total = CompositeComponent.withInitialVersion(
                "Total Price",
                List.of(basePrice, tax),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                clock
        )
        and:
        Parameters jan15 = Parameters.of("timestamp", LocalDateTime.of(2024, 1, 15, 0, 0))
        assert total.calculate(jan15) == Money.of(123, "PLN")

        Parameters feb15 = Parameters.of("timestamp", LocalDateTime.of(2024, 2, 15, 0, 0))
        assert total.calculate(feb15) == Money.of(103, "PLN")

        Parameters mar15 = Parameters.of("timestamp", LocalDateTime.of(2024, 3, 15, 0, 0))
        assert total.calculate(mar15) == Money.of(123, "PLN")
    }
    def "a child can be removed from the composition in a new version"() {
        given:
        Component basePrice = SimpleComponent.withInitialVersion(
                "Base Price",
                new SimpleFixedCalculator("base", Money.of(100, "PLN")),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                clock
        )
        Component tax = SimpleComponent.withInitialVersion(
                "Tax",
                new SimpleFixedCalculator("tax", Money.of(23, "PLN")),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                clock
        )
        Component surcharge = SimpleComponent.withInitialVersion(
                "Surcharge",
                new SimpleFixedCalculator("surcharge", Money.of(10, "PLN")),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                clock
        )

        CompositeComponent total = CompositeComponent.withInitialVersion(
                "Total Price",
                List.of(basePrice, tax, surcharge),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                clock
        )
        and:
        CompositeComponentVersion withoutSurcharge = new CompositeComponentVersion(
                List.of(basePrice, tax),
                java.util.Map.of(),
                Validity.from(LocalDateTime.of(2024, 3, 1, 0, 0)),
                now(clock).plusMinutes(10)
        )
        total = total.updateWith(withoutSurcharge)
        and:
        Parameters feb = Parameters.of("timestamp", LocalDateTime.of(2024, 2, 15, 0, 0))
        assert total.calculate(feb) == Money.of(133, "PLN")

        Parameters mar = Parameters.of("timestamp", LocalDateTime.of(2024, 3, 15, 0, 0))
        assert total.calculate(mar) == Money.of(123, "PLN")
    }
    def "a child can be replaced in the composition in a new version"() {
        given:
        Component basePriceV1 = SimpleComponent.withInitialVersion(
                "Base Price V1",
                new SimpleFixedCalculator("base-v1", Money.of(100, "PLN")),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                clock
        )
        Component tax = SimpleComponent.withInitialVersion(
                "Tax",
                new SimpleFixedCalculator("tax", Money.of(23, "PLN")),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                clock
        )

        CompositeComponent total = CompositeComponent.withInitialVersion(
                "Total Price",
                List.of(basePriceV1, tax),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                clock
        )
        and:
        Component basePriceV2 = SimpleComponent.withInitialVersion(
                "Base Price V2",
                new SimpleFixedCalculator("base-v2", Money.of(150, "PLN")),
                Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0)),
                clock
        )

        CompositeComponentVersion newVersion = new CompositeComponentVersion(
                List.of(basePriceV2, tax),
                java.util.Map.of(),
                Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0)),
                now(clock).plusMinutes(10)
        )
        total = total.updateWith(newVersion)
        and:
        Parameters april = Parameters.of("timestamp", LocalDateTime.of(2024, 4, 15, 0, 0))
        assert total.calculate(april) == Money.of(123, "PLN")

        Parameters may = Parameters.of("timestamp", LocalDateTime.of(2024, 5, 15, 0, 0))
        assert total.calculate(may) == Money.of(173, "PLN")
    }
    def "when multiple versions overlap the youngest version wins"() {
        given:
        Component child = SimpleComponent.withInitialVersion(
                "Child",
                new SimpleFixedCalculator("child", Money.of(100, "PLN")),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                clock
        )

        CompositeComponent composite = CompositeComponent.withInitialVersion(
                "Composite",
                List.of(child),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                clock
        )
        and:
        Component child2 = SimpleComponent.withInitialVersion(
                "Child2",
                new SimpleFixedCalculator("child2", Money.of(200, "PLN")),
                Validity.from(LocalDateTime.of(2024, 2, 1, 0, 0)),
                clock
        )
        CompositeComponentVersion version2 = new CompositeComponentVersion(
                List.of(child2),
                java.util.Map.of(),
                Validity.from(LocalDateTime.of(2024, 2, 1, 0, 0)),
                now(clock).plusMinutes(5)
        )
        composite = composite.updateWith(version2)

        Component child3 = SimpleComponent.withInitialVersion(
                "Child3",
                new SimpleFixedCalculator("child3", Money.of(300, "PLN")),
                Validity.from(LocalDateTime.of(2024, 2, 10, 0, 0)),
                clock
        )
        CompositeComponentVersion version3 = new CompositeComponentVersion(
                List.of(child3),
                java.util.Map.of(),
                Validity.from(LocalDateTime.of(2024, 2, 10, 0, 0)),
                now(clock).plusMinutes(10)
        )
        composite = composite.updateWith(version3)
        and:
        Parameters feb15 = Parameters.of("timestamp", LocalDateTime.of(2024, 2, 15, 0, 0))
        assert composite.calculate(feb15) == Money.of(300, "PLN")
    }
}
