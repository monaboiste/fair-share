package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Map
import spock.lang.Specification



class EMobilityTemporalPricingSpec extends Specification {

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
    def "shouldCalculatePriceInJanuary BasePrice"() {
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

        System.out.println("=== STYCZEŃ 2024 - Cennik bazowy ===")
        System.out.println("Energia: 20 kWh × 2.50 PLN = " + breakdown.children().get(0).total())
        System.out.println("VAT 23%: " + breakdown.children().get(1).total())
        System.out.println("RAZEM: " + breakdown.total())
        System.out.println()
    }
    def "shouldApplyValentinePromotion February"() {
        given:
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

        System.out.println("=== LUTY 2024 - Walentynkowa promocja ===")
        System.out.println("Energia: 20 kWh × 2.00 PLN = " + breakdown.children().get(0).total() + " ← PROMOCJA!")
        System.out.println("VAT 23%: " + breakdown.children().get(1).total())
        System.out.println("RAZEM: " + breakdown.total() + " (-12.30 PLN taniej!)")
        System.out.println()
    }
    def "shouldRevertToBasePriceAfterPromotion March"() {
        given:
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

        System.out.println("=== MARZEC 2024 - Powrót do cennika bazowego ===")
        System.out.println("Energia: 20 kWh × 2.50 PLN = " + breakdown.children().get(0).total() + " ← automatyczny powrót")
        System.out.println("VAT 23%: " + breakdown.children().get(1).total())
        System.out.println("RAZEM: " + breakdown.total())
        System.out.println()
    }
    def "shouldAddParkingFeeInMay CompositeVersionUpdate"() {
        given:
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

        System.out.println("=== MAJ 2024 - Dodanie parkingu ===")
        System.out.println("Energia: " + breakdown.children().get(0).total())
        System.out.println("Parking: " + breakdown.children().get(1).total() + " ← NOWY SKŁADNIK")
        System.out.println("VAT: " + breakdown.children().get(2).total())
        System.out.println("RAZEM: " + breakdown.total())
        System.out.println()
    }
    def "shouldIncreasePriceInSummer July"() {
        given:
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

        System.out.println("=== LIPIEC 2024 - Letnia podwyżka ===")
        System.out.println("Energia: " + breakdown.children().get(0).total() + " ← podwyżka")
        System.out.println("Parking: " + breakdown.children().get(1).total())
        System.out.println("VAT: " + breakdown.children().get(2).total())
        System.out.println("RAZEM: " + breakdown.total() + " ← NAJDROŻEJ!")
        System.out.println()
    }
    def "shouldRevertToBasePriceAfterSummer September"() {
        given:
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

        System.out.println("=== WRZESIEŃ 2024 - Automatyczny powrót po lecie ===")
        System.out.println("Energia: " + breakdown.children().get(0).total() + " ← automatyczny powrót do 2.50 PLN/kWh")
        System.out.println("Parking: " + breakdown.children().get(1).total())
        System.out.println("VAT: " + breakdown.children().get(2).total())
        System.out.println("RAZEM: " + breakdown.total())
        System.out.println("⚡ NIC NIE MUSIELIŚMY ROBIĆ - system sam wrócił do ceny bazowej!")
        System.out.println()
    }
    def "shouldIncreaseWinterParkingFee November"() {
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
        given:
        Parameters dec05 = Parameters.of(
                "timestamp", LocalDateTime.of(2024, 12, 5, 8, 0),
                "kwh", BigDecimal.valueOf(20)
        )

        ComponentBreakdown breakdown = facade.calculateComponentBreakdown("TotalPrice", dec05)
        and:
        assert breakdown.total() == Money.of(71.34, "PLN")
        assert breakdown.children().get(1).total() == Money.of(8.00, "PLN")

        System.out.println("=== GRUDZIEŃ 2024 - Zimowy parking ===")
        System.out.println("Energia: " + breakdown.children().get(0).total())
        System.out.println("Parking: " + breakdown.children().get(1).total() + " ← zimowa podwyżka")
        System.out.println("VAT: " + breakdown.children().get(2).total())
        System.out.println("RAZEM: " + breakdown.total())
        System.out.println()
    }
    def "shouldVisualizeFullYearTimeline"() {
        given:
        setupFullYearPricing()

        System.out.println("╔════════════════════════════════════════════════════════════════╗")
        System.out.println("║    EMOBILITY - TIMELINE CENNIKA 2024 (20 kWh)                 ║")
        System.out.println("╠════════════════════════════════════════════════════════════════╣")

        LocalDateTime[] dates = [
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

        String[] months = ["STY", "LUT", "MAR", "KWI", "MAJ", "CZE",
                "LIP", "SIE", "WRZ", "PAŹ", "LIS", "GRU"]

        for (int i in 0..<dates.length) {
            Parameters params = Parameters.of("timestamp", dates[i], "kwh", BigDecimal.valueOf(20))
            Money price = facade.calculateComponent("TotalPrice", params)

            System.out.printf("║ %s  %s  %7s  %s%n",
                    months[i],
                    getIndicator(price),
                    price,
                    getComment(dates[i].getMonthValue()))
        }

        System.out.println("╚════════════════════════════════════════════════════════════════╝")
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

    private String getIndicator(Money price) {
        double amount = price.value().doubleValue()
        if (amount < 50) {
            return "▁▁▁"
        }
        if (amount < 55) {
            return "▂▂▂"
        }
        if (amount < 60) {
            return "▃▃▃"
        }
        if (amount < 65) {
            return "▄▄▄"
        }
        if (amount < 70) {
            return "▅▅▅"
        }
        if (amount < 75) {
            return "▆▆▆"
        }
        return "▇▇▇"
    }

    private String getComment(int month) {
        return switch (month) {
            case 1 -> "║ ← Cennik bazowy"
            case 2 -> "║ ← PROMOCJA -20%!"
            case 3 -> "║ ← Powrót do bazowej"
            case 5 -> "║ ← +Parking 5 PLN"
            case 7 -> "║ ← Letnia podwyżka"
            case 9 -> "║ ← Powrót ceny"
            case 11 -> "║ ← Zimowy parking 8 PLN"
            default -> "║"
        }
    }
}
