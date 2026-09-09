package com.softwarearchetypes.product.scenarios

import com.softwarearchetypes.product.PackageValidationResult
import com.softwarearchetypes.product.Product
import com.softwarearchetypes.product.ProductType
import com.softwarearchetypes.product.SelectedProduct
import com.softwarearchetypes.product.fixture.LogisticsTransportFixture
import spock.lang.Specification

class LogisticsTransportScenarioSpec extends Specification {

    private LogisticsTransportFixture catalog

    def setup() {
        catalog = LogisticsTransportFixture.create()
    }

    def "#transport transport with #insurance insurance is #outcome"() {
        given:
        ProductType selectedTransport = transport == "domestic"
                ? catalog.domesticExpress()
                : catalog.internationalExpress()
        ProductType selectedInsurance = insurance == "standard"
                ? catalog.standardCargo()
                : catalog.extendedCargo()
        List<SelectedProduct> selection = selectedProducts([
                selectedTransport,
                selectedInsurance,
                catalog.trackingPackage()
        ])

        when:
        PackageValidationResult result = catalog.transportPremium().validateSelection(selection)

        then:
        result.isValid() == valid

        where:
        transport       | insurance  | outcome    | valid
        "domestic"      | "standard" | "accepted" | true
        "international" | "standard" | "rejected" | false
        "international" | "extended" | "accepted" | true
    }

    def "transport package allows at most two add-ons"() {
        given:
        List<ProductType> addOns = [catalog.coldChain(), catalog.fragileHandling(), catalog.pickupService()]
        List<SelectedProduct> selection = selectedProducts([
                catalog.domesticExpress(),
                catalog.standardCargo(),
                catalog.trackingPackage(),
                *addOns.take(addOnCount)
        ])

        when:
        PackageValidationResult result = catalog.transportPremium().validateSelection(selection)

        then:
        result.isValid() == valid

        where:
        addOnCount | valid
        2          | true
        3          | false
    }

    def "transport package requires tracking"() {
        given:
        List<SelectedProduct> selection = selectedProducts([
                catalog.domesticExpress(),
                catalog.standardCargo()
        ])

        when:
        PackageValidationResult result = catalog.transportPremium().validateSelection(selection)

        then:
        !result.isValid()
    }

    def "notification package remains optional"() {
        given:
        List<SelectedProduct> selection = selectedProducts([
                catalog.domesticExpress(),
                catalog.standardCargo(),
                catalog.trackingPackage()
        ])
        if (notificationSelected) {
            selection.add(new SelectedProduct(catalog.notificationPackage().id(), 1))
        }

        when:
        PackageValidationResult result = catalog.transportPremium().validateSelection(selection)

        then:
        result.isValid()

        where:
        notificationSelected << [true, false]
    }

    def "tracking package requires one tracking option"() {
        given:
        List<SelectedProduct> selection = trackingSelected
                ? selectedProducts([catalog.systemTracking()])
                : List.<SelectedProduct> of()

        when:
        PackageValidationResult result = catalog.trackingPackage().validateSelection(selection)

        then:
        result.isValid() == trackingSelected

        where:
        trackingSelected << [true, false]
    }

    def "notification package allows zero or one notification channel"() {
        given:
        List<SelectedProduct> selection = notificationSelected
                ? selectedProducts([catalog.emailNotification()])
                : List.<SelectedProduct> of()

        when:
        PackageValidationResult result = catalog.notificationPackage().validateSelection(selection)

        then:
        result.isValid()

        where:
        notificationSelected << [true, false]
    }

    def "#transport transport with partner insurance is accepted"() {
        given:
        ProductType selectedTransport = transport == "domestic"
                ? catalog.domesticExpress()
                : catalog.internationalExpress()
        List<SelectedProduct> selection = selectedProducts([
                selectedTransport,
                catalog.partnerInsurance(),
                catalog.trackingPackage()
        ])

        when:
        PackageValidationResult result = catalog.transportPremium().validateSelection(selection)

        then:
        result.isValid()

        where:
        transport << ["domestic", "international"]
    }

    def "notification package accepts #channelName notification"() {
        given:
        Map<String, ProductType> channels = [
                "sms"    : catalog.smsNotification(),
                "email"  : catalog.emailNotification(),
                "webhook": catalog.webhookNotification()
        ]
        List<SelectedProduct> selection = selectedProducts([channels[channelName]])

        when:
        PackageValidationResult result = catalog.notificationPackage().validateSelection(selection)

        then:
        result.isValid()

        where:
        channelName << ["sms", "email", "webhook"]
    }

    def "tracking package accepts active monitoring"() {
        given:
        List<SelectedProduct> selection = selectedProducts([catalog.activeMonitoring()])

        when:
        PackageValidationResult result = catalog.trackingPackage().validateSelection(selection)

        then:
        result.isValid()
    }

    private static List<SelectedProduct> selectedProducts(Collection<? extends Product> products) {
        return products.collect { product -> new SelectedProduct(product.id(), 1) }
    }
}
