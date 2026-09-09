package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.time.Clock
import java.time.LocalTime
import spock.lang.Specification

class CompositeFunctionCalculatorScenarioSpec extends Specification {

    private final PricingFacade facade = PricingTestConfiguration.inMemory(Clock.systemUTC())

    def "parking price depends on time of day"() {
        given: "day rate of 5 PLN (06:00-22:00) and night rate of 2 PLN (22:00-06:00)"
        CalculatorId dayRateId = addFixedCalculator("parking-day-rate", Money.of(5.00, "PLN"))
        CalculatorId nightRateId = addFixedCalculator("parking-night-rate", Money.of(2.00, "PLN"))
        addCompositeCalculator(
                "parking-hourly-rate",
                "parkingTime",
                CalculatorRange.time(LocalTime.of(6, 0), LocalTime.of(22, 0), dayRateId),
                CalculatorRange.time(LocalTime.of(22, 0), LocalTime.of(6, 0), nightRateId))

        when: "calculating at 15:00 (day) and 23:00 (night)"
        Money dayPrice = facade.calculate(
                "parking-hourly-rate", Parameters.of("parkingTime", LocalTime.of(15, 0)))
        Money nightPrice = facade.calculate(
                "parking-hourly-rate", Parameters.of("parkingTime", LocalTime.of(23, 0)))

        then:
        new BigDecimal("5.00") == dayPrice.value()
        new BigDecimal("2.00") == nightPrice.value()
    }

    def "volume discounts are applied by quantity"() {
        given: "10 PLN for 1-9 units, 8 PLN for 10-49, 6 PLN for 50-999"
        CalculatorId smallOrderId = addFixedCalculator("price-small", Money.of(10.00, "PLN"))
        CalculatorId mediumOrderId = addFixedCalculator("price-medium", Money.of(8.00, "PLN"))
        CalculatorId largeOrderId = addFixedCalculator("price-large", Money.of(6.00, "PLN"))
        addCompositeCalculator(
                "volume-discount",
                "quantity",
                CalculatorRange.numeric(new BigDecimal("1"), new BigDecimal("10"), smallOrderId),
                CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), mediumOrderId),
                CalculatorRange.numeric(new BigDecimal("50"), new BigDecimal("1000"), largeOrderId))

        when: "calculating for quantities 5, 25, and 100"
        Money smallPrice = facade.calculate("volume-discount", Parameters.of("quantity", new BigDecimal("5")))
        Money mediumPrice = facade.calculate("volume-discount", Parameters.of("quantity", new BigDecimal("25")))
        Money largePrice = facade.calculate("volume-discount", Parameters.of("quantity", new BigDecimal("100")))

        then:
        new BigDecimal("10.00") == smallPrice.value()
        new BigDecimal("8.00") == mediumPrice.value()
        new BigDecimal("6.00") == largePrice.value()
    }

    def "shipping cost is determined by package weight"() {
        given: "four weight tiers: tiny (<1 kg), small (1-5 kg), medium (5-10 kg), large (10-20 kg)"
        CalculatorId tinyPackageId = addFixedCalculator("shipping-tiny", Money.of(12.00, "PLN"))
        CalculatorId smallPackageId = addFixedCalculator("shipping-small", Money.of(18.00, "PLN"))
        CalculatorId mediumPackageId = addFixedCalculator("shipping-medium", Money.of(28.00, "PLN"))
        CalculatorId largePackageId = addFixedCalculator("shipping-large", Money.of(45.00, "PLN"))
        addCompositeCalculator(
                "shipping-by-weight",
                "weight",
                CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("1"), tinyPackageId),
                CalculatorRange.numeric(new BigDecimal("1"), new BigDecimal("5"), smallPackageId),
                CalculatorRange.numeric(new BigDecimal("5"), new BigDecimal("10"), mediumPackageId),
                CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("20"), largePackageId))

        when: "calculating for 0.5 kg, 3.5 kg, and 15 kg"
        Money tinyPrice = facade.calculate("shipping-by-weight", Parameters.of("weight", new BigDecimal("0.5")))
        Money smallPrice = facade.calculate("shipping-by-weight", Parameters.of("weight", new BigDecimal("3.5")))
        Money largePrice = facade.calculate("shipping-by-weight", Parameters.of("weight", new BigDecimal("15")))

        then:
        new BigDecimal("12.00") == tinyPrice.value()
        new BigDecimal("18.00") == smallPrice.value()
        new BigDecimal("45.00") == largePrice.value()
    }

    def "bar pricing applies a happy-hour discount in the early evening"() {
        given: "regular price of 25 PLN, happy-hour price of 15 PLN between 17:00 and 19:00"
        CalculatorId regularPriceId = addFixedCalculator("drink-regular", Money.of(25.00, "PLN"))
        CalculatorId happyHourPriceId = addFixedCalculator("drink-happy-hour", Money.of(15.00, "PLN"))
        addCompositeCalculator(
                "bar-pricing",
                "orderTime",
                CalculatorRange.time(LocalTime.of(0, 0), LocalTime.of(17, 0), regularPriceId),
                CalculatorRange.time(LocalTime.of(17, 0), LocalTime.of(19, 0), happyHourPriceId),
                CalculatorRange.time(LocalTime.of(19, 0), LocalTime.of(23, 59), regularPriceId))

        when: "calculating at 14:00, 18:00, and 21:00"
        Money afternoonPrice = facade.calculate("bar-pricing", Parameters.of("orderTime", LocalTime.of(14, 0)))
        Money happyPrice = facade.calculate("bar-pricing", Parameters.of("orderTime", LocalTime.of(18, 0)))
        Money eveningPrice = facade.calculate("bar-pricing", Parameters.of("orderTime", LocalTime.of(21, 0)))

        then:
        new BigDecimal("25.00") == afternoonPrice.value()
        new BigDecimal("15.00") == happyPrice.value()
        new BigDecimal("25.00") == eveningPrice.value()
    }

    def "transfer fees are applied based on transfer amount"() {
        given: "free for amounts below 100 PLN, 2 PLN for 100-999 PLN, 5 PLN for 1000-9999 PLN"
        CalculatorId freeTransferId = addFixedCalculator("transfer-free", Money.zero("PLN"))
        CalculatorId smallFeeId = addFixedCalculator("transfer-small-fee", Money.of(2.00, "PLN"))
        CalculatorId mediumFeeId = addFixedCalculator("transfer-medium-fee", Money.of(5.00, "PLN"))
        addCompositeCalculator(
                "transfer-fees",
                "amount",
                CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("100"), freeTransferId),
                CalculatorRange.numeric(new BigDecimal("100"), new BigDecimal("1000"), smallFeeId),
                CalculatorRange.numeric(new BigDecimal("1000"), new BigDecimal("10000"), mediumFeeId))

        when: "calculating for 50, 500, and 5000 PLN transfers"
        Money smallFee = facade.calculate("transfer-fees", Parameters.of("amount", new BigDecimal("50")))
        Money mediumFee = facade.calculate("transfer-fees", Parameters.of("amount", new BigDecimal("500")))
        Money largeFee = facade.calculate("transfer-fees", Parameters.of("amount", new BigDecimal("5000")))

        then:
        BigDecimal.ZERO == smallFee.value()
        new BigDecimal("2.00") == mediumFee.value()
        new BigDecimal("5.00") == largeFee.value()
    }

    private CalculatorId addFixedCalculator(String name, Money amount) {
        facade.addCalculator(name, CalculatorType.SIMPLE_FIXED, Parameters.of("amount", amount)).getId()
    }

    private void addCompositeCalculator(String name, String rangeSelector, CalculatorRange... ranges) {
        facade.addCalculator(
                name,
                CalculatorType.COMPOSITE,
                Parameters.of("rangeSelector", rangeSelector, "ranges", ranges.toList()))
    }
}
