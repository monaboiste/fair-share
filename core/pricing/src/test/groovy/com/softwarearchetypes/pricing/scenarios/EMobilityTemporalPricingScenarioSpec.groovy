package com.softwarearchetypes.pricing.scenarios

import com.softwarearchetypes.pricing.CalculatorType
import com.softwarearchetypes.pricing.ComponentBreakdown
import com.softwarearchetypes.pricing.Interpretation
import com.softwarearchetypes.pricing.Parameters
import com.softwarearchetypes.pricing.PricingConfiguration
import com.softwarearchetypes.pricing.PricingFacade
import com.softwarearchetypes.pricing.SumOf
import com.softwarearchetypes.pricing.Validity
import com.softwarearchetypes.pricing.ValueOf
import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Map
import spock.lang.Specification


class EMobilityTemporalPricingScenarioSpec extends Specification {

    private Clock fixedClock
    private PricingFacade facade
    def setup() {
        fixedClock = Clock.fixed(
                Instant.parse("2024-01-01T00:00:00Z"),
                ZoneId.systemDefault()
        )

        facade = PricingConfiguration.inMemory(fixedClock).pricingFacade()
        registerCalculators()
        createInitialComponents()
    }

    private void registerCalculators() {
        facade.addCalculator("energy-2.50", CalculatorType.SIMPLE_FIXED, Parameters.of(
                "amount", Money.of(2.50, "PLN"),
                "interpretation", Interpretation.UNIT
        ))

        facade.addCalculator("energy-2.00", CalculatorType.SIMPLE_FIXED, Parameters.of(
                "amount", Money.of(2.00, "PLN"),
                "interpretation", Interpretation.UNIT
        ))

        facade.addCalculator("energy-2.80", CalculatorType.SIMPLE_FIXED, Parameters.of(
                "amount", Money.of(2.80, "PLN"),
                "interpretation", Interpretation.UNIT
        ))
        facade.addCalculator("vat-23", CalculatorType.PERCENTAGE, Parameters.of(
                "percentageRate", BigDecimal.valueOf(23)
        ))
        facade.addCalculator("parking-5", CalculatorType.SIMPLE_FIXED, Parameters.of(
                "amount", Money.of(5, "PLN")
        ))

        facade.addCalculator("parking-8", CalculatorType.SIMPLE_FIXED, Parameters.of(
                "amount", Money.of(8, "PLN")
        ))
    }

    private void createInitialComponents() {
        facade.createSimpleComponent(
                "EnergyCharge",
                "energy-2.50",
                Map.of("kwh", "quantity"),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        )
        facade.createSimpleComponent(
                "VAT",
                "vat-23",
                Map.of("baseAmount", "baseAmount"),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0))
        )
        facade.createSimpleComponent(
                "ParkingFee",
                "parking-5",
                Map.of(),
                Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0))
        )
        facade.createCompositeComponent(
                "TotalPrice",
                Map.of("VAT", Map.of("baseAmount", new ValueOf("EnergyCharge"))),
                Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)),
                "EnergyCharge", "VAT"
        )
    }
    def "price in January uses the base rate of 2.50 PLN per kWh"() {
        given:
        Parameters jan15 = Parameters.of(
                "timestamp", LocalDateTime.of(2024, 1, 15, 10, 30),
                "kwh", BigDecimal.valueOf(20)
        )
        and:
        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("TotalPrice", jan15)
        and:
        assert breakdown.total() == Money.of(61.50, "PLN")
        assert breakdown.children().size() == 2
        assert breakdown.children().get(0).name() == "EnergyCharge"
        assert breakdown.children().get(0).total() == Money.of(50.00, "PLN")
    }
    def "Valentine promotion in February reduces the energy rate to 2.00 PLN per kWh"() {
        given: "a temporary discount valid only in February"
        facade.createSimpleComponent(
                "EnergyCharge",
                "energy-2.00",
                Map.of("kwh", "quantity"),
                Validity.between(
                        LocalDateTime.of(2024, 2, 1, 0, 0),
                        LocalDateTime.of(2024, 3, 1, 0, 0)
                )
        )
        and:
        Parameters feb14 = Parameters.of(
                "timestamp", LocalDateTime.of(2024, 2, 14, 14, 0),
                "kwh", BigDecimal.valueOf(20)
        )

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("TotalPrice", feb14)
        and:
        assert breakdown.total() == Money.of(49.20, "PLN")
        assert breakdown.children().get(0).total() == Money.of(40.00, "PLN")
    }
    def "price in March reverts to the base rate after the promotion ends"() {
        given: "the February promotion registered alongside the base rate"
        facade.createSimpleComponent(
                "EnergyCharge",
                "energy-2.00",
                Map.of("kwh", "quantity"),
                Validity.between(
                        LocalDateTime.of(2024, 2, 1, 0, 0),
                        LocalDateTime.of(2024, 3, 1, 0, 0)
                )
        )
        and:
        Parameters mar10 = Parameters.of(
                "timestamp", LocalDateTime.of(2024, 3, 10, 16, 45),
                "kwh", BigDecimal.valueOf(20)
        )

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("TotalPrice", mar10)
        and:
        assert breakdown.total() == Money.of(61.50, "PLN")
        assert breakdown.children().get(0).total() == Money.of(50.00, "PLN")
    }
    def "parking fee is added to the composite in May"() {
        given: "a new composite version that includes ParkingFee from May onwards"
        facade.createCompositeComponent(
                "TotalPrice",
                Map.of("VAT", Map.of("baseAmount", new SumOf("EnergyCharge", "ParkingFee"))),
                Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0)),
                "EnergyCharge", "ParkingFee", "VAT"
        )
        and:
        Parameters may20 = Parameters.of(
                "timestamp", LocalDateTime.of(2024, 5, 20, 12, 0),
                "kwh", BigDecimal.valueOf(20)
        )

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("TotalPrice", may20)
        and:
        assert breakdown.total() == Money.of(67.65, "PLN")
        assert breakdown.children().size() == 3
        assert breakdown.children().get(0).name() == "EnergyCharge"
        assert breakdown.children().get(1).name() == "ParkingFee"
        assert breakdown.children().get(2).name() == "VAT"
    }
    def "summer energy rate increase is applied in July"() {
        given: "composite updated for May, and energy raised to 2.80 PLN for July-August"
        facade.createCompositeComponent(
                "TotalPrice",
                Map.of("VAT", Map.of("baseAmount", new SumOf("EnergyCharge", "ParkingFee"))),
                Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0)),
                "EnergyCharge", "ParkingFee", "VAT"
        )
        facade.createSimpleComponent(
                "EnergyCharge",
                "energy-2.80",
                Map.of("kwh", "quantity"),
                Validity.between(
                        LocalDateTime.of(2024, 7, 1, 0, 0),
                        LocalDateTime.of(2024, 9, 1, 0, 0)
                )
        )
        and:
        Parameters jul15 = Parameters.of(
                "timestamp", LocalDateTime.of(2024, 7, 15, 18, 20),
                "kwh", BigDecimal.valueOf(20)
        )

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("TotalPrice", jul15)
        and:
        assert breakdown.total() == Money.of(75.03, "PLN")
        assert breakdown.children().get(0).total() == Money.of(56.00, "PLN")
    }
    def "price reverts automatically to the base rate in September"() {
        given: "composite updated for May, summer increase registered for July-August"
        facade.createCompositeComponent(
                "TotalPrice",
                Map.of("VAT", Map.of("baseAmount", new SumOf("EnergyCharge", "ParkingFee"))),
                Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0)),
                "EnergyCharge", "ParkingFee", "VAT"
        )
        facade.createSimpleComponent(
                "EnergyCharge",
                "energy-2.80",
                Map.of("kwh", "quantity"),
                Validity.between(
                        LocalDateTime.of(2024, 7, 1, 0, 0),
                        LocalDateTime.of(2024, 9, 1, 0, 0)
                )
        )
        and:
        Parameters sep15 = Parameters.of(
                "timestamp", LocalDateTime.of(2024, 9, 15, 14, 0),
                "kwh", BigDecimal.valueOf(20)
        )

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("TotalPrice", sep15)
        and:
        assert breakdown.total() == Money.of(67.65, "PLN")
        assert breakdown.children().get(0).total() == Money.of(50.00, "PLN")
        assert breakdown.children().get(1).total() == Money.of(5.00, "PLN")
    }
    def "winter parking fee increase takes effect in November"() {
        given: "full year pricing with composite, summer rate, and winter parking fee"
        facade.createCompositeComponent(
                "TotalPrice",
                Map.of("VAT", Map.of("baseAmount", new SumOf("EnergyCharge", "ParkingFee"))),
                Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0)),
                "EnergyCharge", "ParkingFee", "VAT"
        )
        facade.createSimpleComponent(
                "EnergyCharge",
                "energy-2.80",
                Map.of("kwh", "quantity"),
                Validity.between(
                        LocalDateTime.of(2024, 7, 1, 0, 0),
                        LocalDateTime.of(2024, 9, 1, 0, 0)
                )
        )
        facade.createSimpleComponent(
                "ParkingFee",
                "parking-8",
                Map.of(),
                Validity.from(LocalDateTime.of(2024, 11, 1, 0, 0))
        )
        and:
        Parameters dec05 = Parameters.of(
                "timestamp", LocalDateTime.of(2024, 12, 5, 8, 0),
                "kwh", BigDecimal.valueOf(20)
        )

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("TotalPrice", dec05)
        and:
        assert breakdown.total() == Money.of(71.34, "PLN")
        assert breakdown.children().get(1).total() == Money.of(8.00, "PLN")
    }
    def "all twelve months of pricing are consistent with the configured timeline"() {
        given: "full year pricing timeline for 2024"
        setupFullYearPricing()
        and: "sample dates covering each month of 2024"
        List<LocalDateTime> dates = [
                LocalDateTime.of(2024, 1, 15, 12, 0),
                LocalDateTime.of(2024, 2, 14, 12, 0),
                LocalDateTime.of(2024, 3, 15, 12, 0),
                LocalDateTime.of(2024, 4, 15, 12, 0),
                LocalDateTime.of(2024, 5, 15, 12, 0),
                LocalDateTime.of(2024, 6, 15, 12, 0),
                LocalDateTime.of(2024, 7, 15, 12, 0),
                LocalDateTime.of(2024, 8, 15, 12, 0),
                LocalDateTime.of(2024, 9, 15, 12, 0),
                LocalDateTime.of(2024, 10, 15, 12, 0),
                LocalDateTime.of(2024, 11, 15, 12, 0),
                LocalDateTime.of(2024, 12, 15, 12, 0)
        ]
        and: "expected totals per month for 20 kWh"
        List<Money> expected = [
                Money.of(61.50, "PLN"),
                Money.of(49.20, "PLN"),
                Money.of(61.50, "PLN"),
                Money.of(61.50, "PLN"),
                Money.of(67.65, "PLN"),
                Money.of(67.65, "PLN"),
                Money.of(75.03, "PLN"),
                Money.of(75.03, "PLN"),
                Money.of(67.65, "PLN"),
                Money.of(67.65, "PLN"),
                Money.of(71.34, "PLN"),
                Money.of(71.34, "PLN")
        ]
        expect:
        dates.eachWithIndex { date, i ->
            Parameters params = Parameters.of("timestamp", date, "kwh", BigDecimal.valueOf(20))
            assert facade.calculateComponent("TotalPrice", params) == expected[i]
        }
    }

    private void setupFullYearPricing() {
        facade.createSimpleComponent(
                "EnergyCharge",
                "energy-2.00",
                Map.of("kwh", "quantity"),
                Validity.between(LocalDateTime.of(2024, 2, 1, 0, 0), LocalDateTime.of(2024, 3, 1, 0, 0))
        )
        facade.createSimpleComponent(
                "EnergyCharge",
                "energy-2.80",
                Map.of("kwh", "quantity"),
                Validity.between(LocalDateTime.of(2024, 7, 1, 0, 0), LocalDateTime.of(2024, 9, 1, 0, 0))
        )
        facade.createSimpleComponent(
                "ParkingFee",
                "parking-8",
                Map.of(),
                Validity.from(LocalDateTime.of(2024, 11, 1, 0, 0))
        )
        facade.createCompositeComponent(
                "TotalPrice",
                Map.of("VAT", Map.of("baseAmount", new SumOf("EnergyCharge", "ParkingFee"))),
                Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0)),
                "EnergyCharge", "ParkingFee", "VAT"
        )
    }
}
