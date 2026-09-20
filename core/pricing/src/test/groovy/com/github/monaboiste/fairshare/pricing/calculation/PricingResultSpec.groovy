package com.github.monaboiste.fairshare.pricing.calculation


import com.github.monaboiste.fairshare.quantity.money.Money
import spock.lang.Specification

class PricingResultSpec extends Specification {

    def "describes standard pricing results with their interpretation and money"() {
        given:
        PricingResult result = pricingResult

        expect:
        result.describe() == expectedDescription

        where:
        pricingResult                         | expectedDescription
        new TotalPrice(Money.of(10, "PLN"))   | "Total price for entire quantity/period: PLN 10"
        new UnitPrice(Money.of(2, "PLN"))     | "Average price per single unit: PLN 2"
        new MarginalPrice(Money.of(3, "PLN")) | "Price of n-th specific unit: PLN 3"
    }

    def "describes custom pricing results through the open result interface"() {
        given:
        PricingResult result = new PricingResult() {
            @Override
            Money money() {
                Money.of(12, "PLN")
            }

            @Override
            Interpretation interpretation() {
                Interpretation.TOTAL
            }
        }

        expect:
        result.describe() == "Total price for entire quantity/period: PLN 12"
    }
}
