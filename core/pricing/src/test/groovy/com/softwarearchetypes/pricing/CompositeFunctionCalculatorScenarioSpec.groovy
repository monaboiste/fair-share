package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.time.LocalTime
import spock.lang.Specification

class CompositeFunctionCalculatorScenarioSpec extends Specification {

    private InMemoryCalculatorRepository repository

    def setup() {
        repository = new InMemoryCalculatorRepository()
    }

    def "parking price depends on time of day"() {
        given: "day rate of 5 PLN (06:00-22:00) and night rate of 2 PLN (22:00-06:00)"
        SimpleFixedCalculator dayRate = new SimpleFixedCalculator(
                "parking-day-rate",
                Money.of(5.00, "PLN")
        )
        SimpleFixedCalculator nightRate = new SimpleFixedCalculator(
                "parking-night-rate",
                Money.of(2.00, "PLN")
        )

        repository.save(dayRate)
        repository.save(nightRate)

        Ranges timeRanges = new Ranges(
                "parkingTime",
                List.of(
                        CalculatorRange.time(LocalTime.of(6, 0), LocalTime.of(22, 0), dayRate.id()),
                        CalculatorRange.time(LocalTime.of(22, 0), LocalTime.of(6, 0), nightRate.id())
                )
        )

        CompositeFunctionCalculator parkingPricing = new CompositeFunctionCalculator(
                "parking-hourly-rate",
                timeRanges,
                repository
        )

        when: "calculating at 15:00 (day) and 23:00 (night)"
        Parameters dayParams = new Parameters(Map.of("parkingTime", LocalTime.of(15, 0)))
        Money dayPrice = parkingPricing.calculate(dayParams)
        Parameters nightParams = new Parameters(Map.of("parkingTime", LocalTime.of(23, 0)))
        Money nightPrice = parkingPricing.calculate(nightParams)

        then:
        new BigDecimal("5.00") == dayPrice.value()
        new BigDecimal("2.00") == nightPrice.value()
    }

    def "volume discounts are applied by quantity"() {
        given: "10 PLN for 1-9 units, 8 PLN for 10-49, 6 PLN for 50-999"
        SimpleFixedCalculator smallOrder = new SimpleFixedCalculator("price-small", Money.of(10.00, "PLN"))
        SimpleFixedCalculator mediumOrder = new SimpleFixedCalculator("price-medium", Money.of(8.00, "PLN"))
        SimpleFixedCalculator largeOrder = new SimpleFixedCalculator("price-large", Money.of(6.00, "PLN"))

        repository.save(smallOrder)
        repository.save(mediumOrder)
        repository.save(largeOrder)

        Ranges quantityRanges = new Ranges(
                "quantity",
                List.of(
                        CalculatorRange.numeric(new BigDecimal("1"), new BigDecimal("10"), smallOrder.id()),
                        CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), mediumOrder.id()),
                        CalculatorRange.numeric(new BigDecimal("50"), new BigDecimal("1000"), largeOrder.id())
                )
        )

        CompositeFunctionCalculator volumePricing = new CompositeFunctionCalculator(
                "volume-discount",
                quantityRanges,
                repository
        )

        when: "calculating for quantities 5, 25, and 100"
        Money smallPrice = volumePricing.calculate(new Parameters(Map.of("quantity", new BigDecimal("5"))))
        Money mediumPrice = volumePricing.calculate(new Parameters(Map.of("quantity", new BigDecimal("25"))))
        Money largePrice = volumePricing.calculate(new Parameters(Map.of("quantity", new BigDecimal("100"))))

        then:
        new BigDecimal("10.00") == smallPrice.value()
        new BigDecimal("8.00") == mediumPrice.value()
        new BigDecimal("6.00") == largePrice.value()
    }

    def "shipping cost is determined by package weight"() {
        given: "four weight tiers: tiny (<1 kg), small (1-5 kg), medium (5-10 kg), large (10-20 kg)"
        SimpleFixedCalculator tinyPackage = new SimpleFixedCalculator("shipping-tiny", Money.of(12.00, "PLN"))
        SimpleFixedCalculator smallPackage = new SimpleFixedCalculator("shipping-small", Money.of(18.00, "PLN"))
        SimpleFixedCalculator mediumPackage = new SimpleFixedCalculator("shipping-medium", Money.of(28.00, "PLN"))
        SimpleFixedCalculator largePackage = new SimpleFixedCalculator("shipping-large", Money.of(45.00, "PLN"))

        repository.save(tinyPackage)
        repository.save(smallPackage)
        repository.save(mediumPackage)
        repository.save(largePackage)

        Ranges weightRanges = new Ranges(
                "weight",
                List.of(
                        CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("1"), tinyPackage.id()),
                        CalculatorRange.numeric(new BigDecimal("1"), new BigDecimal("5"), smallPackage.id()),
                        CalculatorRange.numeric(new BigDecimal("5"), new BigDecimal("10"), mediumPackage.id()),
                        CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("20"), largePackage.id())
                )
        )

        CompositeFunctionCalculator shippingPricing = new CompositeFunctionCalculator(
                "shipping-by-weight",
                weightRanges,
                repository
        )

        when: "calculating for 0.5 kg, 3.5 kg, and 15 kg"
        Money tinyPrice = shippingPricing.calculate(new Parameters(Map.of("weight", new BigDecimal("0.5"))))
        Money smallPrice = shippingPricing.calculate(new Parameters(Map.of("weight", new BigDecimal("3.5"))))
        Money largePrice = shippingPricing.calculate(new Parameters(Map.of("weight", new BigDecimal("15"))))

        then:
        new BigDecimal("12.00") == tinyPrice.value()
        new BigDecimal("18.00") == smallPrice.value()
        new BigDecimal("45.00") == largePrice.value()
    }

    def "bar pricing applies a happy-hour discount in the early evening"() {
        given: "regular price of 25 PLN, happy-hour price of 15 PLN between 17:00 and 19:00"
        SimpleFixedCalculator regularPrice = new SimpleFixedCalculator("drink-regular", Money.of(25.00, "PLN"))
        SimpleFixedCalculator happyHourPrice = new SimpleFixedCalculator("drink-happy-hour", Money.of(15.00, "PLN"))

        repository.save(regularPrice)
        repository.save(happyHourPrice)

        Ranges happyHourRanges = new Ranges(
                "orderTime",
                List.of(
                        CalculatorRange.time(LocalTime.of(0, 0), LocalTime.of(17, 0), regularPrice.id()),
                        CalculatorRange.time(LocalTime.of(17, 0), LocalTime.of(19, 0), happyHourPrice.id()),
                        CalculatorRange.time(LocalTime.of(19, 0), LocalTime.of(23, 59), regularPrice.id())
                )
        )

        CompositeFunctionCalculator barPricing = new CompositeFunctionCalculator(
                "bar-pricing",
                happyHourRanges,
                repository
        )

        when: "calculating at 14:00, 18:00, and 21:00"
        Money afternoonPrice = barPricing.calculate(new Parameters(Map.of("orderTime", LocalTime.of(14, 0))))
        Money happyPrice = barPricing.calculate(new Parameters(Map.of("orderTime", LocalTime.of(18, 0))))
        Money eveningPrice = barPricing.calculate(new Parameters(Map.of("orderTime", LocalTime.of(21, 0))))

        then:
        new BigDecimal("25.00") == afternoonPrice.value()
        new BigDecimal("15.00") == happyPrice.value()
        new BigDecimal("25.00") == eveningPrice.value()
    }

    def "transfer fees are applied based on transfer amount"() {
        given: "free for amounts below 100 PLN, 2 PLN for 100-999 PLN, 5 PLN for 1000-9999 PLN"
        SimpleFixedCalculator freeTransfer = new SimpleFixedCalculator("transfer-free", Money.zero("PLN"))
        SimpleFixedCalculator smallFee = new SimpleFixedCalculator("transfer-small-fee", Money.of(2.00, "PLN"))
        SimpleFixedCalculator mediumFee = new SimpleFixedCalculator("transfer-medium-fee", Money.of(5.00, "PLN"))

        repository.save(freeTransfer)
        repository.save(smallFee)
        repository.save(mediumFee)

        Ranges amountRanges = new Ranges(
                "amount",
                List.of(
                        CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("100"), freeTransfer.id()),
                        CalculatorRange.numeric(new BigDecimal("100"), new BigDecimal("1000"), smallFee.id()),
                        CalculatorRange.numeric(new BigDecimal("1000"), new BigDecimal("10000"), mediumFee.id())
                )
        )

        CompositeFunctionCalculator transferFees = new CompositeFunctionCalculator(
                "transfer-fees",
                amountRanges,
                repository
        )

        when: "calculating for 50, 500, and 5000 PLN transfers"
        Money smallFeeAmount = transferFees.calculate(new Parameters(Map.of("amount", new BigDecimal("50"))))
        Money mediumFeeAmount = transferFees.calculate(new Parameters(Map.of("amount", new BigDecimal("500"))))
        Money largeFeeAmount = transferFees.calculate(new Parameters(Map.of("amount", new BigDecimal("5000"))))

        then:
        BigDecimal.ZERO == smallFeeAmount.value()
        new BigDecimal("2.00") == mediumFeeAmount.value()
        new BigDecimal("5.00") == largeFeeAmount.value()
    }
}
