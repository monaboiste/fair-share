package com.softwarearchetypes.product.fixture

import static com.softwarearchetypes.product.SelectionRule.ifThen
import static com.softwarearchetypes.product.SelectionRule.single

import com.softwarearchetypes.product.PackageType
import com.softwarearchetypes.product.Product
import com.softwarearchetypes.product.ProductDescription
import com.softwarearchetypes.product.ProductIdentifier
import com.softwarearchetypes.product.ProductName
import com.softwarearchetypes.product.ProductSet
import com.softwarearchetypes.product.ProductTrackingStrategy
import com.softwarearchetypes.product.ProductType
import com.softwarearchetypes.quantity.Unit

record TelecomPackageFixture(
        ProductType basicPlan,
        ProductType standardPlan,
        ProductType premiumPlan,
        ProductType budgetPhone,
        ProductType midRangePhone,
        ProductType flagshipPhone,
        ProductType simCard,
        ProductType phoneCase,
        ProductType screenProtector,
        ProductType charger,
        PackageType starterPack,
        PackageType phoneBundle) {

    static TelecomPackageFixture create() {
        ProductType basicPlan = identical("Basic Plan", "5 GB and unlimited calls")
        ProductType standardPlan = identical("Standard Plan", "20 GB, unlimited calls, and EU roaming")
        ProductType premiumPlan = identical("Premium Plan", "Unlimited data, calls, and worldwide roaming")

        ProductType budgetPhone = tracked("Samsung Galaxy A15", "Budget smartphone")
        ProductType midRangePhone = tracked("Google Pixel 8", "Mid-range smartphone")
        ProductType flagshipPhone = tracked("iPhone 15 Pro Max", "Flagship smartphone")

        ProductType simCard = tracked("5G SIM Card", "Nano SIM with eSIM support")
        ProductType phoneCase = identical("Universal Phone Case", "Protective case")
        ProductType screenProtector = identical("Tempered Glass Screen Protector", "9H hardness")
        ProductType charger = identical("Fast Charger 65W", "USB-C fast charging")

        PackageType starterPack = Product.builder(
                        ProductIdentifier.uuid(),
                        ProductName.of("5G Starter Pack"),
                        ProductDescription.of("SIM card with optional accessories"))
                .asPackageType()
                .withTrackingStrategy(ProductTrackingStrategy.IDENTICAL)
                .withRequiredChoice("SIM", simCard.id())
                .withChoice("Accessories", 0, 2, phoneCase.id(), screenProtector.id(), charger.id())
                .build()

        PackageType phoneBundle = Product.builder(
                        ProductIdentifier.uuid(),
                        ProductName.of("Phone Bundle"),
                        ProductDescription.of("Mobile package with phone and plan"))
                .asPackageType()
                .withSingleChoice("Plan", basicPlan.id(), standardPlan.id(), premiumPlan.id())
                .withSingleChoice("Phone", budgetPhone.id(), midRangePhone.id(), flagshipPhone.id())
                .withRule(ifThen(
                        single(ProductSet.singleOf("Flagship", flagshipPhone.id())),
                        single(ProductSet.singleOf("Premium", premiumPlan.id()))))
                .withSingleChoice("Starter", starterPack.id())
                .build()

        new TelecomPackageFixture(
                basicPlan,
                standardPlan,
                premiumPlan,
                budgetPhone,
                midRangePhone,
                flagshipPhone,
                simCard,
                phoneCase,
                screenProtector,
                charger,
                starterPack,
                phoneBundle)
    }

    private static ProductType identical(String name, String description) {
        ProductType.identical(
                ProductIdentifier.uuid(), ProductName.of(name), ProductDescription.of(description), Unit.pieces())
    }

    private static ProductType tracked(String name, String description) {
        ProductType.individuallyTracked(
                ProductIdentifier.uuid(), ProductName.of(name), ProductDescription.of(description), Unit.pieces())
    }
}
