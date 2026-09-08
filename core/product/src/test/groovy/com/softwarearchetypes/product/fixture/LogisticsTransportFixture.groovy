package com.softwarearchetypes.product.fixture

import static com.softwarearchetypes.product.SelectionRule.ifThen
import static com.softwarearchetypes.product.SelectionRule.not
import static com.softwarearchetypes.product.SelectionRule.single

import com.softwarearchetypes.product.PackageType
import com.softwarearchetypes.product.Product
import com.softwarearchetypes.product.ProductDescription
import com.softwarearchetypes.product.ProductIdentifier
import com.softwarearchetypes.product.ProductName
import com.softwarearchetypes.product.ProductSet
import com.softwarearchetypes.product.ProductType
import com.softwarearchetypes.quantity.Unit

record LogisticsTransportFixture(
        ProductType systemTracking,
        ProductType activeMonitoring,
        ProductType smsNotification,
        ProductType emailNotification,
        ProductType webhookNotification,
        ProductType domesticExpress,
        ProductType internationalExpress,
        ProductType standardCargo,
        ProductType extendedCargo,
        ProductType partnerInsurance,
        ProductType coldChain,
        ProductType fragileHandling,
        ProductType pickupService,
        PackageType trackingPackage,
        PackageType notificationPackage,
        PackageType transportPremium) {

    static LogisticsTransportFixture create() {
        ProductType systemTracking = product("System Tracking", "Status-level tracking from dispatch through delivery")
        ProductType activeMonitoring = product("Active Monitoring", "GPS monitoring with temperature sensors")

        ProductType smsNotification = product("SMS Notification", "SMS alerts for shipment updates")
        ProductType emailNotification = product("Email Notification", "Email alerts for shipment updates")
        ProductType webhookNotification = product("Webhook Notification", "Webhook integration for ERP systems")

        ProductType domesticExpress = product("Domestic Express", "Fast delivery within the country")
        ProductType internationalExpress = product("International Express", "International delivery with customs")

        ProductType standardCargo = product("Standard Cargo Insurance", "Basic coverage for domestic transport")
        ProductType extendedCargo = product("Extended Cargo Insurance", "Extended coverage for valuable items")
        ProductType partnerInsurance = product("Partner Insurance", "Insurance from an external provider")

        ProductType coldChain = product("Cold Chain", "Temperature-controlled transport")
        ProductType fragileHandling = product("Fragile Handling", "Special handling for fragile items")
        ProductType pickupService = product("Pickup Service", "Pickup from the customer location")

        PackageType trackingPackage = Product.builder(
                ProductIdentifier.uuid(),
                ProductName.of("Tracking Package"),
                ProductDescription.of("Tracking options"))
                .asPackageType()
                .withSingleChoice("TrackingOption", systemTracking.id(), activeMonitoring.id())
                .build()

        PackageType notificationPackage = Product.builder(
                ProductIdentifier.uuid(),
                ProductName.of("Notification Package"),
                ProductDescription.of("Shipment notification options"))
                .asPackageType()
                .withOptionalChoice(
                        "NotificationChannel", smsNotification.id(), emailNotification.id(), webhookNotification.id())
                .build()

        PackageType transportPremium = Product.builder(
                ProductIdentifier.uuid(),
                ProductName.of("Transport Premium Package"),
                ProductDescription.of("Transport with insurance and monitoring"))
                .asPackageType()
                .withSingleChoice("Transport", domesticExpress.id(), internationalExpress.id())
                .withSingleChoice("Insurance", standardCargo.id(), extendedCargo.id(), partnerInsurance.id())
                .withChoice("AddOns", 0, 2, coldChain.id(), fragileHandling.id(), pickupService.id())
                .withRule(ifThen(
                        single(ProductSet.singleOf("International", internationalExpress.id())),
                        not(single(ProductSet.singleOf("StandardCargo", standardCargo.id())))))
                .withSingleChoice("Tracking", trackingPackage.id())
                .withOptionalChoice("Notifications", notificationPackage.id())
                .build()

        new LogisticsTransportFixture(
                systemTracking,
                activeMonitoring,
                smsNotification,
                emailNotification,
                webhookNotification,
                domesticExpress,
                internationalExpress,
                standardCargo,
                extendedCargo,
                partnerInsurance,
                coldChain,
                fragileHandling,
                pickupService,
                trackingPackage,
                notificationPackage,
                transportPremium)
    }

    private static ProductType product(String name, String description) {
        ProductType.identical(
                ProductIdentifier.uuid(), ProductName.of(name), ProductDescription.of(description), Unit.pieces())
    }
}
