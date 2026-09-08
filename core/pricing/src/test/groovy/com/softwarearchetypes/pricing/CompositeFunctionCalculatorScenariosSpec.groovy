package com.softwarearchetypes.pricing

import com.softwarearchetypes.quantity.money.Money
import java.math.BigDecimal
import java.time.LocalTime
import java.util.List
import java.util.Map
import spock.lang.Specification



class CompositeFunctionCalculatorScenariosSpec extends Specification {

    private CalculatorRepository repository
    def setup() {
        repository = new InMemoryCalculatorsRepository()
    }
    def "use case parking pricing by time of day"() {
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
        Parameters dayParams = new Parameters(Map.of("parkingTime", LocalTime.of(15, 0)))
        Money dayPrice = parkingPricing.calculate(dayParams)
        given:
        assert new BigDecimal("5.00").compareTo(dayPrice.value()) == 0
        Parameters nightParams = new Parameters(Map.of("parkingTime", LocalTime.of(23, 0)))
        Money nightPrice = parkingPricing.calculate(nightParams)
        and:
        assert new BigDecimal("2.00").compareTo(nightPrice.value()) == 0
    }
    def "use case volume discount by quantity"() {
        SimpleFixedCalculator smallOrder = new SimpleFixedCalculator(
            "price-small",
            Money.of(10.00, "PLN")
        )
        SimpleFixedCalculator mediumOrder = new SimpleFixedCalculator(
            "price-medium",
            Money.of(8.00, "PLN")
        )
        SimpleFixedCalculator largeOrder = new SimpleFixedCalculator(
            "price-large",
            Money.of(6.00, "PLN")
        )

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
        Parameters smallParams = new Parameters(Map.of("quantity", new BigDecimal("5")))
        Money smallPrice = volumePricing.calculate(smallParams)
        given:
        assert new BigDecimal("10.00").compareTo(smallPrice.value()) == 0
        Parameters mediumParams = new Parameters(Map.of("quantity", new BigDecimal("25")))
        Money mediumPrice = volumePricing.calculate(mediumParams)
        and:
        assert new BigDecimal("8.00").compareTo(mediumPrice.value()) == 0
        Parameters largeParams = new Parameters(Map.of("quantity", new BigDecimal("100")))
        Money largePrice = volumePricing.calculate(largeParams)
        and:
        assert new BigDecimal("6.00").compareTo(largePrice.value()) == 0
    }
    def "use case shipping cost by weight"() {
        SimpleFixedCalculator tinyPackage = new SimpleFixedCalculator(
            "shipping-tiny",
            Money.of(12.00, "PLN")
        )
        SimpleFixedCalculator smallPackage = new SimpleFixedCalculator(
            "shipping-small",
            Money.of(18.00, "PLN")
        )
        SimpleFixedCalculator mediumPackage = new SimpleFixedCalculator(
            "shipping-medium",
            Money.of(28.00, "PLN")
        )
        SimpleFixedCalculator largePackage = new SimpleFixedCalculator(
            "shipping-large",
            Money.of(45.00, "PLN")
        )

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
        Parameters tinyParams = new Parameters(Map.of("weight", new BigDecimal("0.5")))
        Money tinyPrice = shippingPricing.calculate(tinyParams)
        given:
        assert new BigDecimal("12.00").compareTo(tinyPrice.value()) == 0
        Parameters smallParams = new Parameters(Map.of("weight", new BigDecimal("3.5")))
        Money smallPrice = shippingPricing.calculate(smallParams)
        and:
        assert new BigDecimal("18.00").compareTo(smallPrice.value()) == 0
        Parameters largeParams = new Parameters(Map.of("weight", new BigDecimal("15")))
        Money largePrice = shippingPricing.calculate(largeParams)
        and:
        assert new BigDecimal("45.00").compareTo(largePrice.value()) == 0
    }
    def "use case happy hour bar pricing"() {
        SimpleFixedCalculator regularPrice = new SimpleFixedCalculator(
            "drink-regular",
            Money.of(25.00, "PLN")
        )
        SimpleFixedCalculator happyHourPrice = new SimpleFixedCalculator(
            "drink-happy-hour",
            Money.of(15.00, "PLN")
        )

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
        Parameters afternoonParams = new Parameters(Map.of("orderTime", LocalTime.of(14, 0)))
        Money afternoonPrice = barPricing.calculate(afternoonParams)
        given:
        assert new BigDecimal("25.00").compareTo(afternoonPrice.value()) == 0
        Parameters happyParams = new Parameters(Map.of("orderTime", LocalTime.of(18, 0)))
        Money happyPrice = barPricing.calculate(happyParams)
        and:
        assert new BigDecimal("15.00").compareTo(happyPrice.value()) == 0
        Parameters eveningParams = new Parameters(Map.of("orderTime", LocalTime.of(21, 0)))
        Money eveningPrice = barPricing.calculate(eveningParams)
        and:
        assert new BigDecimal("25.00").compareTo(eveningPrice.value()) == 0
    }
    def "use case transfer fee by amount"() {
        SimpleFixedCalculator freeTransfer = new SimpleFixedCalculator(
            "transfer-free",
            Money.zero("PLN")
        )
        SimpleFixedCalculator smallFee = new SimpleFixedCalculator(
            "transfer-small-fee",
            Money.of(2.00, "PLN")
        )
        SimpleFixedCalculator mediumFee = new SimpleFixedCalculator(
            "transfer-medium-fee",
            Money.of(5.00, "PLN")
        )

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
        Parameters smallTransfer = new Parameters(Map.of("amount", new BigDecimal("50")))
        Money smallFeeAmount = transferFees.calculate(smallTransfer)
        given:
        assert BigDecimal.ZERO.compareTo(smallFeeAmount.value()) == 0
        Parameters mediumTransfer = new Parameters(Map.of("amount", new BigDecimal("500")))
        Money mediumFeeAmount = transferFees.calculate(mediumTransfer)
        and:
        assert new BigDecimal("2.00").compareTo(mediumFeeAmount.value()) == 0
        Parameters largeTransfer = new Parameters(Map.of("amount", new BigDecimal("5000")))
        Money largeFeeAmount = transferFees.calculate(largeTransfer)
        and:
        assert new BigDecimal("5.00").compareTo(largeFeeAmount.value()) == 0
    }
}
