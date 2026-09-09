package com.softwarearchetypes.product.fixture

import com.softwarearchetypes.product.ProductCommands.DefineProductType
import com.softwarearchetypes.product.ProductFacade
import com.softwarearchetypes.product.ProductIdentifier
import com.softwarearchetypes.product.ProductRelationshipsFacade
import com.softwarearchetypes.product.ProductTrackingStrategy
import com.softwarearchetypes.product.ProductType
import com.softwarearchetypes.product.ProductTypeRepository
import groovy.transform.ImmutableOptions

@ImmutableOptions(knownImmutableClasses = [ProductRelationshipsFacade, ProductType])
record ProductRelationshipsFixture(
        ProductRelationshipsFacade facade,
        ProductType basicPlan,
        ProductType standardPlan,
        ProductType premiumPlan,
        ProductType iphone15,
        ProductType iphone15Pro,
        ProductType samsungS24,
        ProductType pixelPhone,
        ProductType usbCCharger,
        ProductType lightningCharger,
        ProductType wirelessCharger,
        ProductType cloudStorage100GB,
        ProductType cloudStorage1TB) {

    static ProductRelationshipsFixture create() {
        ProductTypeRepository repository = ProductTypeRepository.inMemory()
        ProductFacade productFacade = new ProductFacade(repository)

        ProductType basicPlan = define(productFacade, repository, "Basic Plan", "5 GB and unlimited calls", ProductTrackingStrategy.IDENTICAL)
        ProductType standardPlan = define(productFacade, repository, "Standard Plan", "20 GB, unlimited calls, and EU roaming", ProductTrackingStrategy.IDENTICAL)
        ProductType premiumPlan = define(productFacade, repository, "Premium Plan", "Unlimited data, calls, and worldwide roaming", ProductTrackingStrategy.IDENTICAL)

        ProductType iphone15 = define(productFacade, repository, "iPhone 15", "128 GB", ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
        ProductType iphone15Pro = define(productFacade, repository, "iPhone 15 Pro", "256 GB with ProMotion display", ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
        ProductType samsungS24 = define(productFacade, repository, "Samsung Galaxy S24", "256 GB", ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
        ProductType pixelPhone = define(productFacade, repository, "Google Pixel 8", "128 GB", ProductTrackingStrategy.INDIVIDUALLY_TRACKED)

        ProductType usbCCharger = define(productFacade, repository, "USB-C Fast Charger", "65 W USB-C charger", ProductTrackingStrategy.IDENTICAL)
        ProductType lightningCharger = define(productFacade, repository, "Lightning Charger", "20 W Lightning charger", ProductTrackingStrategy.IDENTICAL)
        ProductType wirelessCharger = define(productFacade, repository, "Wireless Charger", "15 W MagSafe-compatible charger", ProductTrackingStrategy.IDENTICAL)

        ProductType cloudStorage100GB = define(productFacade, repository, "Cloud Storage 100 GB", "100 GB cloud storage", ProductTrackingStrategy.IDENTICAL)
        ProductType cloudStorage1TB = define(productFacade, repository, "Cloud Storage 1 TB", "1 TB cloud storage", ProductTrackingStrategy.IDENTICAL)

        new ProductRelationshipsFixture(
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
                cloudStorage1TB)
    }

    private static ProductType define(
            ProductFacade facade,
            ProductTypeRepository repository,
            String name,
            String description,
            ProductTrackingStrategy trackingStrategy) {
        String id = ProductIdentifier.uuid().toString()
        def result = facade.handle(new DefineProductType(
                "UUID", id, name, description, "pcs", trackingStrategy.name(), Set.of(), Set.of(), Map.of()))
        if (result.failure()) {
            throw new IllegalStateException(result.getFailure())
        }
        repository.findByIdValue(id).orElseThrow()
    }
}
