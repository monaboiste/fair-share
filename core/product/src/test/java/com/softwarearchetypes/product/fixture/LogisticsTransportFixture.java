package com.softwarearchetypes.product.fixture;

import static com.softwarearchetypes.product.SelectionRule.ifThen;
import static com.softwarearchetypes.product.SelectionRule.not;
import static com.softwarearchetypes.product.SelectionRule.single;

import com.softwarearchetypes.product.PackageType;
import com.softwarearchetypes.product.Product;
import com.softwarearchetypes.product.ProductDescription;
import com.softwarearchetypes.product.ProductIdentifier;
import com.softwarearchetypes.product.ProductName;
import com.softwarearchetypes.product.ProductSet;
import com.softwarearchetypes.product.ProductType;
import com.softwarearchetypes.quantity.Unit;

/**
 * A model for a premium parcel logistics service.
 *
 * <h2>Atomic products</h2>
 *
 * <h3>Transport modes</h3>
 *
 * <ul>
 *   <li>{@code "Domestic Express"} - fast delivery within the country
 *   <li>{@code "International Express"} - cross-border delivery including customs handling
 * </ul>
 *
 * <h3>Insurance</h3>
 *
 * <ul>
 *   <li>{@code "Standard Cargo Insurance"} - basic coverage; accepted for domestic only
 *   <li>{@code "Extended Cargo Insurance"} - full coverage; required for international
 *   <li>{@code "Partner Insurance"} - external insurer; accepted for all routes
 * </ul>
 *
 * <h3>Tracking</h3>
 *
 * <ul>
 *   <li>{@code "System Tracking"} - status-level (dispatched / in-transit / delivered)
 *   <li>{@code "Active Monitoring"} - GPS with temperature sensors
 * </ul>
 *
 * <h3>Notifications (optional)</h3>
 *
 * <ul>
 *   <li>{@code "SMS Notification"}, {@code "Email Notification"}, {@code "Webhook Notification"}
 * </ul>
 *
 * <h3>Add-ons (0–2)</h3>
 *
 * <ul>
 *   <li>{@code "Cold Chain"}, {@code "Fragile Handling"}, {@code "Pickup Service"}
 * </ul>
 *
 * <h2>Packages</h2>
 *
 * <ul>
 *   <li><strong>Tracking Package</strong> - choose exactly one tracking mode
 *   <li><strong>Notification Package</strong> - optionally choose one notification channel
 *   <li><strong>Transport Premium Package</strong> - one transport + one insurance + 0–2 add-ons + one tracking;
 *       notifications optional. Contains a conditional rule: international transport cannot be combined with standard
 *       cargo insurance.
 * </ul>
 *
 * <h2>Constraints</h2>
 *
 * <ul>
 *   <li>International transport requires extended or partner insurance (not standard)
 *   <li>At most two add-ons per shipment
 *   <li>Tracking is mandatory; notification is optional
 * </ul>
 *
 * <h2>Design decisions</h2>
 *
 * <p><strong>Tracking strategy.</strong> All atomic products use {@code IDENTICAL} because logistics services are
 * fulfilled at the shipment level (the {@code PackageInstance}), not at the level of individual plan units. There is no
 * meaningful serial number for "one unit of SMS Notification."
 *
 * <p><strong>Conditional insurance rule.</strong> The rule "international transport cannot use standard cargo
 * insurance" is expressed as a {@code SelectionRule.ifThen} on the {@code PackageStructure}. It belongs there because
 * it constrains the commercial bundle configuration, not the intrinsic nature of any single product. An intrinsic rule
 * would live on {@code ProductType.applicabilityConstraint}.
 *
 * <p><strong>Tracking vs. notification.</strong> Tracking is modelled as a required single-choice to ensure every
 * shipment is observable. Notification is optional to keep the basic offer lean; customers who need alerts add it.
 */
public record LogisticsTransportFixture(
        // Tracking
        ProductType systemTracking,
        // Notifications
        ProductType emailNotification,
        // Transport modes
        ProductType domesticExpress,
        ProductType internationalExpress,
        // Insurance
        ProductType standardCargo,
        ProductType extendedCargo,
        // Add-ons
        ProductType coldChain,
        ProductType fragileHandling,
        ProductType pickupService,
        // Packages
        PackageType trackingPackage,
        PackageType notificationPackage,
        PackageType transportPremium) {

    public static LogisticsTransportFixture create() {

        // -------------------------------------------------------------------------
        // Tracking modes - both are interchangeable units of service
        // -------------------------------------------------------------------------
        ProductType systemTracking = product("System Tracking", "Status-level tracking from dispatch through delivery");
        ProductType activeMonitoring = product("Active Monitoring", "GPS monitoring with temperature sensors");

        // -------------------------------------------------------------------------
        // Notification channels - interchangeable units; channel choice is a feature
        // -------------------------------------------------------------------------
        ProductType smsNotification = product("SMS Notification", "SMS alerts for shipment updates");
        ProductType emailNotification = product("Email Notification", "Email alerts for shipment updates");
        ProductType webhookNotification = product("Webhook Notification", "Webhook integration for ERP systems");

        // -------------------------------------------------------------------------
        // Transport modes
        // -------------------------------------------------------------------------
        ProductType domesticExpress = product("Domestic Express", "Fast delivery within the country");
        ProductType internationalExpress = product("International Express", "International delivery with customs");

        // -------------------------------------------------------------------------
        // Insurance tiers
        // -------------------------------------------------------------------------
        ProductType standardCargo = product("Standard Cargo Insurance", "Basic coverage for domestic transport");
        ProductType extendedCargo = product("Extended Cargo Insurance", "Extended coverage for valuable items");
        ProductType partnerInsurance = product("Partner Insurance", "Insurance from an external provider");

        // -------------------------------------------------------------------------
        // Optional add-ons (0–2 per shipment)
        // -------------------------------------------------------------------------
        ProductType coldChain = product("Cold Chain", "Temperature-controlled transport");
        ProductType fragileHandling = product("Fragile Handling", "Special handling for fragile items");
        ProductType pickupService = product("Pickup Service", "Pickup from the customer location");

        // -------------------------------------------------------------------------
        // Tracking Package - single mandatory tracking mode selection
        // -------------------------------------------------------------------------
        PackageType trackingPackage = Product.builder(
                        ProductIdentifier.uuid(),
                        ProductName.of("Tracking Package"),
                        ProductDescription.of("Tracking options"))
                .asPackageType()
                .withSingleChoice("TrackingOption", systemTracking.id(), activeMonitoring.id())
                .build();

        // -------------------------------------------------------------------------
        // Notification Package - zero-or-one channel selection
        // -------------------------------------------------------------------------
        PackageType notificationPackage = Product.builder(
                        ProductIdentifier.uuid(),
                        ProductName.of("Notification Package"),
                        ProductDescription.of("Shipment notification options"))
                .asPackageType()
                .withOptionalChoice(
                        "NotificationChannel", smsNotification.id(), emailNotification.id(), webhookNotification.id())
                .build();

        // -------------------------------------------------------------------------
        // Transport Premium Package
        //
        // Conditional rule: if the customer selects International Express, Standard
        // Cargo Insurance must NOT be selected. This is a commercial bundle constraint,
        // not an intrinsic product property, so it lives here in the PackageStructure.
        // -------------------------------------------------------------------------
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
                .build();

        return new LogisticsTransportFixture(
                systemTracking,
                emailNotification,
                domesticExpress,
                internationalExpress,
                standardCargo,
                extendedCargo,
                coldChain,
                fragileHandling,
                pickupService,
                trackingPackage,
                notificationPackage,
                transportPremium);
    }

    private static ProductType product(String name, String description) {
        return ProductType.identical(
                ProductIdentifier.uuid(), ProductName.of(name), ProductDescription.of(description), Unit.pieces());
    }
}
