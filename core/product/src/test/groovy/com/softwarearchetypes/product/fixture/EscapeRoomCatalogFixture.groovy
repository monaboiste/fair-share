package com.softwarearchetypes.product.fixture

import static com.softwarearchetypes.product.ApplicabilityConstraint.and
import static com.softwarearchetypes.product.ApplicabilityConstraint.equalsTo
import static com.softwarearchetypes.product.ApplicabilityConstraint.greaterThan
import static com.softwarearchetypes.product.ApplicabilityConstraint.in
import static com.softwarearchetypes.product.ApplicabilityConstraint.not

import com.softwarearchetypes.product.ApplicabilityConstraint
import com.softwarearchetypes.product.CatalogEntry
import com.softwarearchetypes.product.CatalogEntryId
import com.softwarearchetypes.product.PackageType
import com.softwarearchetypes.product.Product
import com.softwarearchetypes.product.ProductDescription
import com.softwarearchetypes.product.ProductFeatureType
import com.softwarearchetypes.product.ProductIdentifier
import com.softwarearchetypes.product.ProductMetadata
import com.softwarearchetypes.product.ProductName
import com.softwarearchetypes.product.ProductRelationship
import com.softwarearchetypes.product.ProductRelationshipDefiningPolicy
import com.softwarearchetypes.product.ProductRelationshipFactory
import com.softwarearchetypes.product.ProductRelationshipId
import com.softwarearchetypes.product.ProductRelationshipType
import com.softwarearchetypes.product.ProductTrackingStrategy
import com.softwarearchetypes.product.ProductType
import com.softwarearchetypes.product.Validity
import com.softwarearchetypes.quantity.Unit

final class EscapeRoomCatalogFixture {

    private EscapeRoomCatalogFixture() {}

    static final ProductIdentifier ID_MAD_SCIENTIST_LAB = ProductIdentifier.of("ROOM_MAD_SCIENTIST_LAB")
    static final ProductIdentifier ID_ALCATRAZ = ProductIdentifier.of("ROOM_ALCATRAZ")
    static final ProductIdentifier ID_EGYPTIAN_TOMB = ProductIdentifier.of("ROOM_EGYPTIAN_TOMB")
    static final ProductIdentifier ID_CYBERPUNK_2077 = ProductIdentifier.of("ROOM_CYBERPUNK_2077")

    static final ProductIdentifier ID_ACTOR = ProductIdentifier.of("ADDON_ACTOR")
    static final ProductIdentifier ID_PHOTO_VIDEO = ProductIdentifier.of("ADDON_PHOTO_VIDEO")
    static final ProductIdentifier ID_CATERING = ProductIdentifier.of("ADDON_CATERING")
    static final ProductIdentifier ID_DEDICATED_GM = ProductIdentifier.of("ADDON_DEDICATED_GM")

    static final ProductIdentifier ID_TEAM_BUILDING = ProductIdentifier.of("PKG_TEAM_BUILDING")
    static final ProductIdentifier ID_HARDCORE = ProductIdentifier.of("PKG_HARDCORE")

    static final ProductType MAD_SCIENTIST_LAB = ProductType.builder(
                    ID_MAD_SCIENTIST_LAB,
                    ProductName.of("Mad Scientist's Laboratory"),
                    ProductDescription.of("Dismantle the professor's doomsday device before time runs out."),
                    Unit.of("booking", "booking"),
                    ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
            .withMetadata(ProductMetadata.of(Map.of(
                    "difficulty", "medium",
                    "durationMinutes", "60",
                    "minParticipants", "2",
                    "maxParticipants", "5")))
            .withMandatoryFeature(ProductFeatureType.withNumericRange("participants", 2, 5))
            .build()

    static final ProductType ALCATRAZ = ProductType.builder(
                    ID_ALCATRAZ,
                    ProductName.of("Alcatraz Prison"),
                    ProductDescription.of("Escape from the most notorious prison in history."),
                    Unit.of("booking", "booking"),
                    ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
            .withMetadata(ProductMetadata.of(Map.of(
                    "difficulty", "hard",
                    "durationMinutes", "75",
                    "minParticipants", "3",
                    "maxParticipants", "6")))
            .withMandatoryFeature(ProductFeatureType.withNumericRange("participants", 3, 6))
            .withApplicabilityConstraint(not(equalsTo("claustrophobia", "true")))
            .build()

    static final ProductType EGYPTIAN_TOMB = ProductType.builder(
                    ID_EGYPTIAN_TOMB,
                    ProductName.of("Egyptian Tomb"),
                    ProductDescription.of("Survive the ancient curse and find your way out."),
                    Unit.of("booking", "booking"),
                    ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
            .withMetadata(ProductMetadata.of(Map.of(
                    "difficulty", "easy",
                    "durationMinutes", "45",
                    "minParticipants", "2",
                    "maxParticipants", "4")))
            .withMandatoryFeature(ProductFeatureType.withNumericRange("participants", 2, 4))
            .build()

    static final ProductType CYBERPUNK_2077 = ProductType.builder(
                    ID_CYBERPUNK_2077,
                    ProductName.of("Cyberpunk 2077"),
                    ProductDescription.of("Hack the megacorp's mainframe in full virtual reality."),
                    Unit.of("booking", "booking"),
                    ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
            .withMetadata(ProductMetadata.of(Map.of(
                    "difficulty", "extreme",
                    "durationMinutes", "90",
                    "minParticipants", "4",
                    "maxParticipants", "6",
                    "requiresVr", "true")))
            .withMandatoryFeature(ProductFeatureType.withNumericRange("participants", 4, 6))
            .build()

    static final ProductType CATERING = ProductType.builder(
                    ID_CATERING,
                    ProductName.of("Post-game Catering"),
                    ProductDescription.of("A meal served after the game. Choose your menu."),
                    Unit.of("order", "order"),
                    ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
            .withMandatoryFeature(ProductFeatureType.withAllowedValues("variant", "pizza", "sushi", "vegetarian"))
            .build()

    static final ProductType ACTOR = ProductType.builder(
                    ID_ACTOR,
                    ProductName.of("Actor in the Room"),
                    ProductDescription.of("A professional actor joins your session for extra immersion."),
                    Unit.of("session", "session"),
                    ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
            .build()

    static final ProductType PHOTO_VIDEO = ProductType.builder(
                    ID_PHOTO_VIDEO,
                    ProductName.of("Photo & Video Package"),
                    ProductDescription.of("High-quality photos and a highlight video of your escape."),
                    Unit.of("session", "session"),
                    ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
            .build()

    static final ProductType DEDICATED_GM = ProductType.builder(
                    ID_DEDICATED_GM,
                    ProductName.of("Dedicated Game Master"),
                    ProductDescription.of("A senior GM exclusively assigned to your group."),
                    Unit.of("session", "session"),
                    ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
            .build()

    static final PackageType TEAM_BUILDING = buildTeamBuilding()
    static final PackageType HARDCORE = buildHardcore()

    private static PackageType buildTeamBuilding() {
        Product.builder(
                        ID_TEAM_BUILDING,
                        ProductName.of("Team Building"),
                        ProductDescription.of("Two escape rooms played back-to-back with catering and a dedicated GM."))
                .asPackageType()
                .withChoice("Rooms", 2, 2, ID_MAD_SCIENTIST_LAB, ID_ALCATRAZ, ID_EGYPTIAN_TOMB, ID_CYBERPUNK_2077)
                .withRequiredChoice("Catering", ID_CATERING)
                .withRequiredChoice("DedicatedGameMaster", ID_DEDICATED_GM)
                .build()
    }

    private static PackageType buildHardcore() {
        Product.builder(
                        ID_HARDCORE,
                        ProductName.of("Hardcore"),
                        ProductDescription.of("Cyberpunk 2077 with a live actor and a dedicated GM. Adults only (18+)."))
                .asPackageType()
                .withSingleChoice("HardcoreRoom", ID_CYBERPUNK_2077)
                .withSingleChoice("Actor", ID_ACTOR)
                .withSingleChoice("DedicatedGameMaster", ID_DEDICATED_GM)
                .withApplicabilityConstraint(greaterThan("age", 17))
                .build()
    }

    static List<ProductRelationship> relationships() {
        def factory = new ProductRelationshipFactory(new NoSelfRelationshipPolicy(), { ProductRelationshipId.random() })

        List.of(
                define(factory, ID_EGYPTIAN_TOMB, ID_MAD_SCIENTIST_LAB, ProductRelationshipType.UPGRADABLE_TO),
                define(factory, ID_MAD_SCIENTIST_LAB, ID_ALCATRAZ, ProductRelationshipType.UPGRADABLE_TO),
                define(factory, ID_ALCATRAZ, ID_CYBERPUNK_2077, ProductRelationshipType.UPGRADABLE_TO),
                define(factory, ID_MAD_SCIENTIST_LAB, ID_ACTOR, ProductRelationshipType.COMPLEMENTED_BY),
                define(factory, ID_MAD_SCIENTIST_LAB, ID_CATERING, ProductRelationshipType.COMPLEMENTED_BY),
                define(factory, ID_MAD_SCIENTIST_LAB, ID_PHOTO_VIDEO, ProductRelationshipType.COMPLEMENTED_BY),
                define(factory, ID_ALCATRAZ, ID_ACTOR, ProductRelationshipType.COMPLEMENTED_BY),
                define(factory, ID_ALCATRAZ, ID_CATERING, ProductRelationshipType.COMPLEMENTED_BY),
                define(factory, ID_ALCATRAZ, ID_PHOTO_VIDEO, ProductRelationshipType.COMPLEMENTED_BY),
                define(factory, ID_EGYPTIAN_TOMB, ID_ACTOR, ProductRelationshipType.COMPLEMENTED_BY),
                define(factory, ID_EGYPTIAN_TOMB, ID_CATERING, ProductRelationshipType.COMPLEMENTED_BY),
                define(factory, ID_EGYPTIAN_TOMB, ID_PHOTO_VIDEO, ProductRelationshipType.COMPLEMENTED_BY),
                define(factory, ID_CYBERPUNK_2077, ID_ACTOR, ProductRelationshipType.COMPLEMENTED_BY),
                define(factory, ID_CYBERPUNK_2077, ID_CATERING, ProductRelationshipType.COMPLEMENTED_BY),
                define(factory, ID_CYBERPUNK_2077, ID_PHOTO_VIDEO, ProductRelationshipType.COMPLEMENTED_BY),
                define(factory, ID_CYBERPUNK_2077, ID_EGYPTIAN_TOMB, ProductRelationshipType.INCOMPATIBLE_WITH),
                define(factory, ID_CYBERPUNK_2077, ID_MAD_SCIENTIST_LAB, ProductRelationshipType.INCOMPATIBLE_WITH),
                define(factory, ID_CYBERPUNK_2077, ID_ALCATRAZ, ProductRelationshipType.INCOMPATIBLE_WITH))
    }

    static List<CatalogEntry> catalogEntriesFor(String city) {
        List<CatalogEntry> entries = new ArrayList<>()

        entries.add(entry(MAD_SCIENTIST_LAB, "room"))
        entries.add(entry(ALCATRAZ, "room"))
        entries.add(entry(EGYPTIAN_TOMB, "room"))

        if ("Warsaw" == city) {
            entries.add(entry(CYBERPUNK_2077, "room", and(equalsTo("city", "Warsaw"), equalsTo("hasVrEquipment", "true"))))
        }

        entries.add(entry(ACTOR, "addon", in("dayType", "Saturday", "Sunday")))
        entries.add(entry(PHOTO_VIDEO, "addon"))
        entries.add(entry(CATERING, "addon"))
        entries.add(entry(DEDICATED_GM, "addon"))

        entries.add(entry(TEAM_BUILDING, "package"))
        entries.add(entry(HARDCORE, "package"))

        List.copyOf(entries)
    }

    private static CatalogEntry entry(Product product, String category) {
        entry(product, category, ApplicabilityConstraint.alwaysTrue())
    }

    private static CatalogEntry entry(Product product, String category, ApplicabilityConstraint salesConstraint) {
        CatalogEntry.builder()
                .id(CatalogEntryId.generate())
                .product(product)
                .displayName(product.name().toString())
                .description(product.description().toString())
                .categories(Set.of(category))
                .validity(Validity.always())
                .salesConstraint(salesConstraint)
                .build()
    }

    private static ProductRelationship define(
            ProductRelationshipFactory factory,
            ProductIdentifier from,
            ProductIdentifier to,
            ProductRelationshipType type) {
        factory.defineFor(from, to, type)
                .fold(
                        { err -> throw new IllegalStateException("Relationship policy violated: " + err) },
                        { r -> r })
    }

    private static final class NoSelfRelationshipPolicy implements ProductRelationshipDefiningPolicy {

        @Override
        boolean canDefineFor(ProductIdentifier from, ProductIdentifier to, ProductRelationshipType type) {
            !from.equals(to)
        }
    }
}
