package com.softwarearchetypes.product.fixture;

import com.softwarearchetypes.product.ProductCommands.DefineProductType;
import com.softwarearchetypes.product.ProductFacade;
import com.softwarearchetypes.product.ProductIdentifier;
import com.softwarearchetypes.product.ProductRelationshipsFacade;
import com.softwarearchetypes.product.ProductType;
import com.softwarearchetypes.product.ProductTypeRepository;
import java.util.Map;
import java.util.Set;

/**
 * A model for a mobile operator's product catalog with device accessories and cloud storage.
 *
 * <h2>Subscription plans (identical)</h2>
 *
 * <ul>
 *   <li>{@code "Basic Plan"} - 5 GB data, unlimited calls
 *   <li>{@code "Standard Plan"} - 20 GB data, unlimited calls, EU roaming
 *   <li>{@code "Premium Plan"} - unlimited data, calls, and worldwide roaming
 * </ul>
 *
 * <h2>Smartphones (individually tracked)</h2>
 *
 * <ul>
 *   <li>{@code "iPhone 15"}, {@code "iPhone 15 Pro"} - Apple flagship line
 *   <li>{@code "Samsung Galaxy S24"}, {@code "Google Pixel 8"} - Android flagship line
 * </ul>
 *
 * <h2>Accessories (identical)</h2>
 *
 * <ul>
 *   <li>{@code "USB-C Fast Charger"} - compatible with Android flagships and USB-C iPhones
 *   <li>{@code "Lightning Charger"} - incompatible with USB-C phones
 *   <li>{@code "Wireless Charger"} - compatible with all phones; can be defined bidirectionally
 * </ul>
 *
 * <h2>Cloud storage (identical)</h2>
 *
 * <ul>
 *   <li>{@code "Cloud Storage 100 GB"} - complementary to the standard iPhone 15
 *   <li>{@code "Cloud Storage 1 TB"} - complementary to the pro model
 * </ul>
 *
 * <h2>Relationships defined in this fixture</h2>
 *
 * <ul>
 *   <li>Plan upgrade path: Basic → Standard → Premium ({@code UPGRADABLE_TO})
 *   <li>iPhone 15 is upgradable to iPhone 15 Pro ({@code UPGRADABLE_TO})
 *   <li>iPhone 15 is substitutable by Samsung Galaxy S24 and Google Pixel 8 ({@code SUBSTITUTED_BY})
 *   <li>Phones are complemented by cloud storage ({@code COMPLEMENTED_BY})
 *   <li>USB-C charger is compatible with Samsung and Pixel ({@code COMPATIBLE_WITH})
 *   <li>Lightning charger is incompatible with USB-C phones ({@code INCOMPATIBLE_WITH})
 * </ul>
 *
 * <h2>Design decisions</h2>
 *
 * <p><strong>Tracking strategies.</strong> Plans, accessories, and storage are {@code IDENTICAL} because units of the
 * same SKU are interchangeable. Phones are {@code INDIVIDUALLY_TRACKED} because each physical device carries a unique
 * IMEI.
 *
 * <p><strong>Fixture uses the facade API.</strong> Products are registered through {@link ProductFacade} rather than
 * constructed directly. This exercises the same command/query path used in production, making the fixture a realistic
 * integration harness rather than a pure unit-test helper.
 *
 * <p><strong>Relationship facade.</strong> A pre-wired {@link ProductRelationshipsFacade} is exposed directly so
 * scenario tests can define and query relationships in a single setup call without having to assemble the full
 * {@code ProductConfiguration}.
 */
public record ProductRelationshipsFixture(
        // Facade - pre-wired with the same repository that holds all products below
        ProductRelationshipsFacade facade,
        // Plans
        ProductType basicPlan,
        ProductType standardPlan,
        ProductType premiumPlan,
        // Phones
        ProductType iphone15,
        ProductType iphone15Pro,
        ProductType samsungS24,
        ProductType pixelPhone,
        // Accessories
        ProductType usbCCharger,
        ProductType lightningCharger,
        ProductType wirelessCharger,
        // Cloud storage
        ProductType cloudStorage100GB,
        ProductType cloudStorage1TB) {

    public static ProductRelationshipsFixture create() {
        ProductTypeRepository repository = ProductTypeRepository.inMemory();
        ProductFacade productFacade = new ProductFacade(repository);

        // -------------------------------------------------------------------------
        // Plans - identical units; plan tier is a product-level distinction
        // -------------------------------------------------------------------------
        ProductType basicPlan =
                define(productFacade, repository, "Basic Plan", "5 GB and unlimited calls", "IDENTICAL");
        ProductType standardPlan = define(
                productFacade, repository, "Standard Plan", "20 GB, unlimited calls, and EU roaming", "IDENTICAL");
        ProductType premiumPlan = define(
                productFacade, repository, "Premium Plan", "Unlimited data, calls, and worldwide roaming", "IDENTICAL");

        // -------------------------------------------------------------------------
        // Phones - individually tracked (unique IMEI per device)
        // -------------------------------------------------------------------------
        ProductType iphone15 = define(productFacade, repository, "iPhone 15", "128 GB", "INDIVIDUALLY_TRACKED");
        ProductType iphone15Pro = define(
                productFacade, repository, "iPhone 15 Pro", "256 GB with ProMotion display", "INDIVIDUALLY_TRACKED");
        ProductType samsungS24 =
                define(productFacade, repository, "Samsung Galaxy S24", "256 GB", "INDIVIDUALLY_TRACKED");
        ProductType pixelPhone = define(productFacade, repository, "Google Pixel 8", "128 GB", "INDIVIDUALLY_TRACKED");

        // -------------------------------------------------------------------------
        // Accessories - identical commodity units
        // -------------------------------------------------------------------------
        ProductType usbCCharger =
                define(productFacade, repository, "USB-C Fast Charger", "65 W USB-C charger", "IDENTICAL");
        ProductType lightningCharger =
                define(productFacade, repository, "Lightning Charger", "20 W Lightning charger", "IDENTICAL");
        ProductType wirelessCharger =
                define(productFacade, repository, "Wireless Charger", "15 W MagSafe-compatible charger", "IDENTICAL");

        // -------------------------------------------------------------------------
        // Cloud storage - identical units; capacity tier is a product-level distinction
        // -------------------------------------------------------------------------
        ProductType cloudStorage100GB =
                define(productFacade, repository, "Cloud Storage 100 GB", "100 GB cloud storage", "IDENTICAL");
        ProductType cloudStorage1TB =
                define(productFacade, repository, "Cloud Storage 1 TB", "1 TB cloud storage", "IDENTICAL");

        return new ProductRelationshipsFixture(
                ProductRelationshipsFacade.create(repository),
                basicPlan,
                standardPlan,
                premiumPlan,
                iphone15,
                iphone15Pro,
                samsungS24,
                pixelPhone,
                usbCCharger,
                lightningCharger,
                wirelessCharger,
                cloudStorage100GB,
                cloudStorage1TB);
    }

    private static ProductType define(
            ProductFacade facade,
            ProductTypeRepository repository,
            String name,
            String description,
            String trackingStrategy) {
        String id = ProductIdentifier.uuid().toString();
        var result = facade.handle(new DefineProductType(
                "UUID", id, name, description, "pcs", trackingStrategy, Set.of(), Set.of(), Map.of()));
        if (result.failure()) {
            throw new IllegalStateException(result.getFailure());
        }
        return repository.findByIdValue(id).orElseThrow();
    }
}
