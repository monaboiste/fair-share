package com.softwarearchetypes.product.fixture;

import static com.softwarearchetypes.product.SelectionRule.ifThen;
import static com.softwarearchetypes.product.SelectionRule.single;

import com.softwarearchetypes.product.PackageType;
import com.softwarearchetypes.product.Product;
import com.softwarearchetypes.product.ProductDescription;
import com.softwarearchetypes.product.ProductIdentifier;
import com.softwarearchetypes.product.ProductName;
import com.softwarearchetypes.product.ProductSet;
import com.softwarearchetypes.product.ProductTrackingStrategy;
import com.softwarearchetypes.product.ProductType;
import com.softwarearchetypes.quantity.Unit;

/**
 * A model for a telecom retailer offering mobile plans, phones, and accessories.
 *
 * <h2>Plans (interchangeable)</h2>
 *
 * <ul>
 *   <li>{@code "Basic Plan"} - 5 GB data, unlimited calls
 *   <li>{@code "Standard Plan"} - 20 GB data, unlimited calls, EU roaming
 *   <li>{@code "Premium Plan"} - unlimited data, calls, and worldwide roaming
 * </ul>
 *
 * <h2>Phones (individually tracked)</h2>
 *
 * <ul>
 *   <li>{@code "Samsung Galaxy A15"} - budget tier
 *   <li>{@code "Google Pixel 8"} - mid-range tier
 *   <li>{@code "iPhone 15 Pro Max"} - flagship tier
 * </ul>
 *
 * <h2>SIM cards and accessories (individually tracked / interchangeable)</h2>
 *
 * <ul>
 *   <li>{@code "5G SIM Card"} - Nano SIM with eSIM support; individually tracked because every SIM has a unique ICCID
 *       that must be recorded at point of sale
 *   <li>{@code "Universal Phone Case"}, {@code "Tempered Glass Screen Protector"}, {@code "Fast Charger 65W"} -
 *       commodity accessories; interchangeable units
 * </ul>
 *
 * <h2>Packages</h2>
 *
 * <ul>
 *   <li><strong>5G Starter Pack</strong> - one SIM card (required) + up to two accessories (optional). Uses
 *       {@code IDENTICAL} tracking because the package itself is a commodity bundle; individual item tracking is
 *       captured at the component level.
 *   <li><strong>Phone Bundle</strong> - one plan + one phone + one starter pack. Contains a conditional rule: selecting
 *       the flagship phone requires the premium plan.
 * </ul>
 *
 * <h2>Constraints</h2>
 *
 * <ul>
 *   <li>SIM card is mandatory in the starter pack
 *   <li>Flagship phone requires the premium plan (conditional rule on {@code PackageStructure})
 *   <li>At most two accessories per starter pack
 * </ul>
 *
 * <h2>Design decisions</h2>
 *
 * <p><strong>Tracking strategies.</strong> Plans and accessories use {@code IDENTICAL} because individual units are
 * interchangeable — there is nothing meaningful to distinguish one "Basic Plan" subscription unit from another at this
 * catalogue level. Phones and SIM cards use {@code INDIVIDUALLY_TRACKED} because every physical unit carries a unique
 * identifier (IMEI / ICCID) that must be recorded for regulatory and fulfilment purposes.
 *
 * <p><strong>Flagship–premium conditional rule.</strong> The constraint "flagship phone requires premium plan" is a
 * commercial bundle rule, not a property of the iPhone itself. It lives on the {@code PackageStructure} via
 * {@code SelectionRule.ifThen}. If the rule were intrinsic to the product (e.g. a hardware compatibility requirement)
 * it would belong on {@code ProductType.applicabilityConstraint}.
 *
 * <p><strong>Starter pack as a nested package.</strong> The starter pack is modelled as its own {@code PackageType} and
 * then referenced as a required single-choice component of the phone bundle. This allows the starter pack to be sold
 * standalone and reused across multiple bundle configurations without duplicating its structure.
 */
public record TelecomPackageFixture(
        // Plans
        ProductType basicPlan,
        ProductType standardPlan,
        ProductType premiumPlan,
        // Phones
        ProductType budgetPhone,
        ProductType midRangePhone,
        ProductType flagshipPhone,
        // SIM and accessories
        ProductType simCard,
        ProductType phoneCase,
        ProductType screenProtector,
        ProductType charger,
        // Packages
        PackageType starterPack,
        PackageType phoneBundle) {

    public static TelecomPackageFixture create() {

        // -------------------------------------------------------------------------
        // Plans - identical units; no individual tracking needed
        // -------------------------------------------------------------------------
        ProductType basicPlan = identical("Basic Plan", "5 GB and unlimited calls");
        ProductType standardPlan = identical("Standard Plan", "20 GB, unlimited calls, and EU roaming");
        ProductType premiumPlan = identical("Premium Plan", "Unlimited data, calls, and worldwide roaming");

        // -------------------------------------------------------------------------
        // Phones - individually tracked because each unit has a unique IMEI
        // -------------------------------------------------------------------------
        ProductType budgetPhone = tracked("Samsung Galaxy A15", "Budget smartphone");
        ProductType midRangePhone = tracked("Google Pixel 8", "Mid-range smartphone");
        ProductType flagshipPhone = tracked("iPhone 15 Pro Max", "Flagship smartphone");

        // -------------------------------------------------------------------------
        // SIM card - individually tracked (unique ICCID per card)
        // Accessories - identical commodity units
        // -------------------------------------------------------------------------
        ProductType simCard = tracked("5G SIM Card", "Nano SIM with eSIM support");
        ProductType phoneCase = identical("Universal Phone Case", "Protective case");
        ProductType screenProtector = identical("Tempered Glass Screen Protector", "9H hardness");
        ProductType charger = identical("Fast Charger 65W", "USB-C fast charging");

        // -------------------------------------------------------------------------
        // 5G Starter Pack
        //
        // SIM is required; up to two accessories are optional.
        // Package-level tracking is IDENTICAL because the bundle itself is a
        // commodity unit — individual component tracking happens at component level.
        // -------------------------------------------------------------------------
        PackageType starterPack = Product.builder(
                        ProductIdentifier.uuid(),
                        ProductName.of("5G Starter Pack"),
                        ProductDescription.of("SIM card with optional accessories"))
                .asPackageType()
                .withTrackingStrategy(ProductTrackingStrategy.IDENTICAL)
                .withRequiredChoice("SIM", simCard.id())
                .withChoice("Accessories", 0, 2, phoneCase.id(), screenProtector.id(), charger.id())
                .build();

        // -------------------------------------------------------------------------
        // Phone Bundle
        //
        // Conditional rule: if the flagship phone is selected, the premium plan is
        // required. This is a commercial upsell rule, not a hardware constraint.
        // -------------------------------------------------------------------------
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
                .build();

        return new TelecomPackageFixture(
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
                phoneBundle);
    }

    private static ProductType identical(String name, String description) {
        return ProductType.identical(
                ProductIdentifier.uuid(), ProductName.of(name), ProductDescription.of(description), Unit.pieces());
    }

    private static ProductType tracked(String name, String description) {
        return ProductType.individuallyTracked(
                ProductIdentifier.uuid(), ProductName.of(name), ProductDescription.of(description), Unit.pieces());
    }
}
