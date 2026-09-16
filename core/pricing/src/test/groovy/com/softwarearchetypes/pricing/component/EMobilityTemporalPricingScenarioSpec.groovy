package com.softwarearchetypes.pricing.component

import com.softwarearchetypes.pricing.calculation.Calculator
import com.softwarearchetypes.pricing.calculation.Calculators
import com.softwarearchetypes.pricing.calculation.Interpretation
import com.softwarearchetypes.pricing.calculation.Parameters
import com.softwarearchetypes.pricing.calculation.TotalPrice
import com.softwarearchetypes.quantity.money.Money
import java.time.Clock
import java.time.LocalDateTime
import spock.lang.Specification

class EMobilityTemporalPricingScenarioSpec extends Specification {

    private Calculator parking8
    private Calculator parking5
    private Calculator vat23
    private Calculator energy280
    private Calculator energy200
    private Component energyCharge
    private Component parkingFee
    private Component totalPrice
    private Component vat
    private Calculator energy250
    private Calculator parkingzero

    def setup() {
        registerCalculators()
        createInitialComponents()
    }

    private void registerCalculators() {
        parkingzero = Calculators.fixed("parking-zero", Money.zero("PLN"))
        energy250 = Calculators.fixed("energy-2.50", Money.of(2.50, "PLN"), Interpretation.UNIT)

        energy200 = Calculators.fixed("energy-2.00", Money.of(2.00, "PLN"), Interpretation.UNIT)

        energy280 = Calculators.fixed("energy-2.80", Money.of(2.80, "PLN"), Interpretation.UNIT)
        vat23 = Calculators.percentage("vat-23", BigDecimal.valueOf(23))
        parking5 = Calculators.fixed("parking-5", Money.of(5, "PLN"))

        parking8 = Calculators.fixed("parking-8", Money.of(8, "PLN"))
    }

    private void createInitialComponents() {
        energyCharge = Component.simple("EnergyCharge", energy250, Map.of("kwh", "quantity"), Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)))
        vat = Component.simple("VAT", vat23, Map.of("baseAmount", "baseAmount"), Validity.from(LocalDateTime.of(2024, 1, 1, 0, 0)))
        parkingFee = Component.simple("ParkingFee", parkingzero)
        parkingFee = parkingFee.updateWith(new SimpleComponentVersion(parking5, Map.of(), Validity.from(LocalDateTime.of(2024, 5, 1, 0, 0)), LocalDateTime.now()))
        totalPrice = Component.composite("TotalPrice", Map.of("VAT", Map.of("baseAmount", ParameterExpression.valueOf("EnergyCharge"))), energyCharge, vat)
    }

    def "price in January uses the base rate of 2.50 PLN per kWh"() {
        given:
        Parameters jan15 = Parameters.of(
                "timestamp", LocalDateTime.of(2024, 1, 15, 10, 30),
                "kwh", BigDecimal.valueOf(20)
        )

        and:
        ComponentBreakdown breakdown = totalPrice.calculateBreakdown( jan15)

        expect:
        breakdown.total() == Money.of(61.50, "PLN")
        breakdown.children().size() == 2
        breakdown.children().get(0).name() == "EnergyCharge"
        breakdown.children().get(0).total() == Money.of(50.00, "PLN")
    }

    def "Valentine promotion in February reduces the energy rate to 2.00 PLN per kWh"() {
        given: "a temporary discount valid only in February"
        energyCharge = energyCharge.updateWith(new SimpleComponentVersion(energy200, Map.of("kwh", "quantity"), Validity.between(LocalDateTime.of(2024, 2, 1, 0, 0), LocalDateTime.of(2024, 3, 1, 0, 0)), LocalDateTime.now()))
        totalPrice = Component.composite("TotalPrice", Map.of("VAT", Map.of("baseAmount", ParameterExpression.valueOf("EnergyCharge"))), energyCharge, vat)

        and:
        Parameters feb14 = Parameters.of(
                "timestamp", LocalDateTime.of(2024, 2, 14, 14, 0),
                "kwh", BigDecimal.valueOf(20)
        )

        ComponentBreakdown breakdown = totalPrice.calculateBreakdown( feb14)

        expect:
        breakdown.total() == Money.of(49.20, "PLN")
        breakdown.children().get(0).total() == Money.of(40.00, "PLN")
    }

    def "price in March reverts to the base rate after the promotion ends"() {
        given: "the February promotion registered alongside the base rate"
        energyCharge = energyCharge.updateWith(new SimpleComponentVersion(energy200, Map.of("kwh", "quantity"), Validity.between(LocalDateTime.of(2024, 2, 1, 0, 0), LocalDateTime.of(2024, 3, 1, 0, 0)), LocalDateTime.now()))
        totalPrice = Component.composite("TotalPrice", Map.of("VAT", Map.of("baseAmount", ParameterExpression.valueOf("EnergyCharge"))), energyCharge, vat)

        and:
        Parameters mar10 = Parameters.of(
                "timestamp", LocalDateTime.of(2024, 3, 10, 16, 45),
                "kwh", BigDecimal.valueOf(20)
        )

        ComponentBreakdown breakdown = totalPrice.calculateBreakdown( mar10)

        expect:
        breakdown.total() == Money.of(61.50, "PLN")
        breakdown.children().get(0).total() == Money.of(50.00, "PLN")
    }

    def "parking fee is added to the composite in May"() {
        given: "a new composite version that includes ParkingFee from May onwards"
        Map<String, Map<String, ParameterExpression>> dependencies = Map.<String, Map<String, ParameterExpression>> of(
                "VAT", Map.of("baseAmount", ParameterExpression.sumOf("EnergyCharge", "ParkingFee")))
        totalPrice = Component.composite("TotalPrice", dependencies, energyCharge, parkingFee, vat)

        and:
        Parameters may20 = Parameters.of(
                "timestamp", LocalDateTime.of(2024, 5, 20, 12, 0),
                "kwh", BigDecimal.valueOf(20)
        )

        ComponentBreakdown breakdown = totalPrice.calculateBreakdown( may20)

        expect:
        breakdown.total() == Money.of(67.65, "PLN")
        breakdown.children().size() == 3
        breakdown.children().get(0).name() == "EnergyCharge"
        breakdown.children().get(1).name() == "ParkingFee"
        breakdown.children().get(2).name() == "VAT"
    }

    def "summer energy rate increase is applied in July"() {
        given: "composite updated for May, and energy raised to 2.80 PLN for July-August"
        Map<String, Map<String, ParameterExpression>> dependencies = Map.<String, Map<String, ParameterExpression>> of(
                "VAT", Map.of("baseAmount", ParameterExpression.sumOf("EnergyCharge", "ParkingFee")))
        energyCharge = energyCharge.updateWith(new SimpleComponentVersion(energy280, Map.of("kwh", "quantity"), Validity.between(LocalDateTime.of(2024, 7, 1, 0, 0), LocalDateTime.of(2024, 9, 1, 0, 0)), LocalDateTime.now()))
        totalPrice = Component.composite("TotalPrice", dependencies, energyCharge, parkingFee, vat)

        and:
        Parameters jul15 = Parameters.of(
                "timestamp", LocalDateTime.of(2024, 7, 15, 18, 20),
                "kwh", BigDecimal.valueOf(20)
        )

        ComponentBreakdown breakdown = totalPrice.calculateBreakdown( jul15)

        expect:
        breakdown.total() == Money.of(75.03, "PLN")
        breakdown.children().get(0).total() == Money.of(56.00, "PLN")
    }

    def "price reverts automatically to the base rate in September"() {
        given: "composite updated for May, summer increase registered for July-August"
        Map<String, Map<String, ParameterExpression>> dependencies = Map.<String, Map<String, ParameterExpression>> of(
                "VAT", Map.of("baseAmount", ParameterExpression.sumOf("EnergyCharge", "ParkingFee")))
        energyCharge = energyCharge.updateWith(new SimpleComponentVersion(energy280, Map.of("kwh", "quantity"), Validity.between(LocalDateTime.of(2024, 7, 1, 0, 0), LocalDateTime.of(2024, 9, 1, 0, 0)), LocalDateTime.now()))
        totalPrice = Component.composite("TotalPrice", dependencies, energyCharge, parkingFee, vat)

        and:
        Parameters sep15 = Parameters.of(
                "timestamp", LocalDateTime.of(2024, 9, 15, 14, 0),
                "kwh", BigDecimal.valueOf(20)
        )

        ComponentBreakdown breakdown = totalPrice.calculateBreakdown( sep15)

        expect:
        breakdown.total() == Money.of(67.65, "PLN")
        breakdown.children().get(0).total() == Money.of(50.00, "PLN")
        breakdown.children().get(1).total() == Money.of(5.00, "PLN")
    }

    def "winter parking fee increase takes effect in November"() {
        given: "full year pricing with composite, summer rate, and winter parking fee"
        Map<String, Map<String, ParameterExpression>> dependencies = Map.<String, Map<String, ParameterExpression>> of(
                "VAT", Map.of("baseAmount", ParameterExpression.sumOf("EnergyCharge", "ParkingFee")))
        energyCharge = energyCharge.updateWith(new SimpleComponentVersion(energy280, Map.of("kwh", "quantity"), Validity.between(LocalDateTime.of(2024, 7, 1, 0, 0), LocalDateTime.of(2024, 9, 1, 0, 0)), LocalDateTime.now()))
        parkingFee = parkingFee.updateWith(new SimpleComponentVersion(parking8, Map.of(), Validity.from(LocalDateTime.of(2024, 11, 1, 0, 0)), LocalDateTime.now()))
        totalPrice = Component.composite("TotalPrice", dependencies, energyCharge, parkingFee, vat)

        and:
        Parameters dec05 = Parameters.of(
                "timestamp", LocalDateTime.of(2024, 12, 5, 8, 0),
                "kwh", BigDecimal.valueOf(20)
        )

        ComponentBreakdown breakdown = totalPrice.calculateBreakdown( dec05)

        expect:
        breakdown.total() == Money.of(71.34, "PLN")
        breakdown.children().get(1).total() == Money.of(8.00, "PLN")
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
        dates.collect { date ->
            Parameters params = Parameters.of("timestamp", date, "kwh", BigDecimal.valueOf(20))
            totalPrice.calculate( params).money()
        } == expected
    }

    private void setupFullYearPricing() {
        energyCharge = energyCharge.updateWith(new SimpleComponentVersion(energy200, Map.of("kwh", "quantity"), Validity.between(LocalDateTime.of(2024, 2, 1, 0, 0), LocalDateTime.of(2024, 3, 1, 0, 0)), LocalDateTime.now()))
        energyCharge = energyCharge.updateWith(new SimpleComponentVersion(energy280, Map.of("kwh", "quantity"), Validity.between(LocalDateTime.of(2024, 7, 1, 0, 0), LocalDateTime.of(2024, 9, 1, 0, 0)), LocalDateTime.now()))
        parkingFee = parkingFee.updateWith(new SimpleComponentVersion(parking8, Map.of(), Validity.from(LocalDateTime.of(2024, 11, 1, 0, 0)), LocalDateTime.now()))
        Map<String, Map<String, ParameterExpression>> dependencies = Map.<String, Map<String, ParameterExpression>> of(
                "VAT", Map.of("baseAmount", ParameterExpression.sumOf("EnergyCharge", "ParkingFee")))
        totalPrice = Component.composite("TotalPrice", dependencies, energyCharge, parkingFee, vat)
    }
}
