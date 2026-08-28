package com.softwarearchetypes.product.fixture;

import static com.softwarearchetypes.product.ApplicabilityConstraint.and;
import static com.softwarearchetypes.product.ApplicabilityConstraint.equalsTo;
import static com.softwarearchetypes.product.ApplicabilityConstraint.greaterThan;
import static com.softwarearchetypes.product.ApplicabilityConstraint.in;
import static com.softwarearchetypes.product.ApplicabilityConstraint.not;

import com.softwarearchetypes.product.ApplicabilityConstraint;
import com.softwarearchetypes.product.CatalogEntry;
import com.softwarearchetypes.product.CatalogEntryId;
import com.softwarearchetypes.product.PackageType;
import com.softwarearchetypes.product.Product;
import com.softwarearchetypes.product.ProductDescription;
import com.softwarearchetypes.product.ProductFeatureType;
import com.softwarearchetypes.product.ProductIdentifier;
import com.softwarearchetypes.product.ProductMetadata;
import com.softwarearchetypes.product.ProductName;
import com.softwarearchetypes.product.ProductRelationship;
import com.softwarearchetypes.product.ProductRelationshipDefiningPolicy;
import com.softwarearchetypes.product.ProductRelationshipFactory;
import com.softwarearchetypes.product.ProductRelationshipId;
import com.softwarearchetypes.product.ProductRelationshipType;
import com.softwarearchetypes.product.ProductTrackingStrategy;
import com.softwarearchetypes.product.ProductType;
import com.softwarearchetypes.product.Validity;
import com.softwarearchetypes.quantity.Unit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A model for a network of escape rooms in three cities.
 *
 * <h2>Rooms</h2>
 *
 * <ul>
 *   <li>{@code "Mad Scientist's Laboratory"} - 60 min, medium difficulty, 2-5 people
 *   <li>{@code "Alcatraz Prison"} - 75 min, hard difficulty, 3-6 people
 *   <li>{@code "Egyptian Tomb"} - 45 min, easy difficulty, 2-4 people
 *   <li>{@code "Cyberpunk 2077"} - 90 min, extreme difficulty, 4-6 people, requires VR
 * </ul>
 *
 * <h2>Add-ons</h2>
 *
 * <ul>
 *   <li>Actor in the room (+30% to the price)
 *   <li>Photo and video package
 *   <li>Catering after the game (pizza / sushi / vegetarian)
 *   <li>Dedicated game master (instead of standard)
 * </ul>
 *
 * <h2>Packages</h2>
 *
 * <ul>
 *   <li><strong>Birthday</strong> - any room + catering + photo package
 *   <li><strong>Team Building</strong> - 2 rooms sequentially + catering + dedicated game master
 *   <li><strong>Hardcore</strong> - {@code "Cyberpunk 2077"} + actor + dedicated game master (18+ only)
 * </ul>
 *
 * <h2>Constraints</h2>
 *
 * <ul>
 *   <li>{@code "Cyberpunk 2077"} is available only in Warsaw, where the VR equipment is located
 *   <li>Actor is available only on weekends
 *   <li>The <strong>Hardcore</strong> package is available only to adults
 *   <li>{@code "Alcatraz Prison"} is not available to people with self-declared claustrophobia
 * </ul>
 */
public final class EscapeRoomCatalogFixture {

    private EscapeRoomCatalogFixture() {}

    // =========================================================================
    // Fixed product identifiers - stable across the catalogue
    // =========================================================================

    // Rooms
    public static final ProductIdentifier ID_MAD_SCIENTIST_LAB = ProductIdentifier.of("ROOM_MAD_SCIENTIST_LAB");
    public static final ProductIdentifier ID_ALCATRAZ = ProductIdentifier.of("ROOM_ALCATRAZ");
    public static final ProductIdentifier ID_EGYPTIAN_TOMB = ProductIdentifier.of("ROOM_EGYPTIAN_TOMB");
    public static final ProductIdentifier ID_CYBERPUNK_2077 = ProductIdentifier.of("ROOM_CYBERPUNK_2077");

    // Add-ons
    public static final ProductIdentifier ID_ACTOR = ProductIdentifier.of("ADDON_ACTOR");
    public static final ProductIdentifier ID_PHOTO_VIDEO = ProductIdentifier.of("ADDON_PHOTO_VIDEO");
    public static final ProductIdentifier ID_CATERING = ProductIdentifier.of("ADDON_CATERING");
    public static final ProductIdentifier ID_DEDICATED_GM = ProductIdentifier.of("ADDON_DEDICATED_GM");

    // Packages
    public static final ProductIdentifier ID_TEAM_BUILDING = ProductIdentifier.of("PKG_TEAM_BUILDING");
    public static final ProductIdentifier ID_HARDCORE = ProductIdentifier.of("PKG_HARDCORE");

    // =========================================================================
    // Rooms
    //
    // Design decisions:
    //   METADATA  - attributes that define what the room IS: difficulty, duration, capacity,
    //               VR requirement. Changing any of them would mean a different product.
    //   FEATURES  - attributes the booking flow configures: how many participants are coming.
    //               The range is capped to the room's actual min/max capacity stored in metadata.
    //   TRACKING  - INDIVIDUALLY_TRACKED: every booking is a unique instance with its own
    //               serial number (booking reference), enabling per-booking audit and fulfilment.
    // =========================================================================

    public static final ProductType MAD_SCIENTIST_LAB = ProductType.builder(
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
            .build();

    public static final ProductType ALCATRAZ = ProductType.builder(
                    ID_ALCATRAZ,
                    ProductName.of("Alcatraz Prison"),
                    ProductDescription.of("Escape from the most notorious prison in history."),
                    new Unit("booking", "booking"),
                    ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
            .withMetadata(ProductMetadata.of(Map.of(
                    "difficulty", "hard",
                    "durationMinutes", "75",
                    "minParticipants", "3",
                    "maxParticipants", "6")))
            .withMandatoryFeature(ProductFeatureType.withNumericRange("participants", 3, 6))
            .withApplicabilityConstraint(not(equalsTo("claustrophobia", "true")))
            .build();

    public static final ProductType EGYPTIAN_TOMB = ProductType.builder(
                    ID_EGYPTIAN_TOMB,
                    ProductName.of("Egyptian Tomb"),
                    ProductDescription.of("Survive the ancient curse and find your way out."),
                    new Unit("booking", "booking"),
                    ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
            .withMetadata(ProductMetadata.of(Map.of(
                    "difficulty", "easy",
                    "durationMinutes", "45",
                    "minParticipants", "2",
                    "maxParticipants", "4")))
            .withMandatoryFeature(ProductFeatureType.withNumericRange("participants", 2, 4))
            .build();

    public static final ProductType CYBERPUNK_2077 = ProductType.builder(
                    ID_CYBERPUNK_2077,
                    ProductName.of("Cyberpunk 2077"),
                    ProductDescription.of("Hack the megacorp's mainframe in full virtual reality."),
                    new Unit("booking", "booking"),
                    ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
            .withMetadata(ProductMetadata.of(Map.of(
                    "difficulty", "extreme",
                    "durationMinutes", "90",
                    "minParticipants", "4",
                    "maxParticipants", "6",
                    "requiresVr", "true")))
            .withMandatoryFeature(ProductFeatureType.withNumericRange("participants", 4, 6))
            .build();

    // =========================================================================
    // Add-ons
    //
    // Catering as ONE ProductType with a "variant" feature: all three catering options
    // (pizza/sushi/vegetarian) share the same tracking strategy, pricing logic, applicability,
    // and operational lifecycle. The only difference is which meal is prepared - a configurable
    // choice, not a structural difference.
    // =========================================================================

    public static final ProductType CATERING = ProductType.builder(
                    ID_CATERING,
                    ProductName.of("Post-game Catering"),
                    ProductDescription.of("A meal served after the game. Choose your menu."),
                    new Unit("order", "order"),
                    ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
            .withMandatoryFeature(ProductFeatureType.withAllowedValues("variant", "pizza", "sushi", "vegetarian"))
            .build();

    public static final ProductType ACTOR = ProductType.builder(
                    ID_ACTOR,
                    ProductName.of("Actor in the Room"),
                    ProductDescription.of("A professional actor joins your session for extra immersion."),
                    new Unit("session", "session"),
                    ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
            .build();

    public static final ProductType PHOTO_VIDEO = ProductType.builder(
                    ID_PHOTO_VIDEO,
                    ProductName.of("Photo & Video Package"),
                    ProductDescription.of("High-quality photos and a highlight video of your escape."),
                    new Unit("session", "session"),
                    ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
            .build();

    public static final ProductType DEDICATED_GM = ProductType.builder(
                    ID_DEDICATED_GM,
                    ProductName.of("Dedicated Game Master"),
                    ProductDescription.of("A senior GM exclusively assigned to your group."),
                    new Unit("session", "session"),
                    ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
            .build();

    // =========================================================================
    // Packages
    // =========================================================================

    public static final PackageType TEAM_BUILDING = buildTeamBuilding();
    public static final PackageType HARDCORE = buildHardcore();

    private static PackageType buildTeamBuilding() {
        return Product.builder(
                        ID_TEAM_BUILDING,
                        ProductName.of("Team Building"),
                        ProductDescription.of("Two escape rooms played back-to-back with catering and a dedicated GM."))
                .asPackageType()
                .withChoice("Rooms", 2, 2, ID_MAD_SCIENTIST_LAB, ID_ALCATRAZ, ID_EGYPTIAN_TOMB, ID_CYBERPUNK_2077)
                .withRequiredChoice("Catering", ID_CATERING)
                .withRequiredChoice("DedicatedGameMaster", ID_DEDICATED_GM)
                .build();
    }

    private static PackageType buildHardcore() {
        return Product.builder(
                        ID_HARDCORE,
                        ProductName.of("Hardcore"),
                        ProductDescription.of(
                                "Cyberpunk 2077 with a live actor and a dedicated GM. Adults only (18+)."))
                .asPackageType()
                .withSingleChoice("HardcoreRoom", ID_CYBERPUNK_2077)
                .withSingleChoice("Actor", ID_ACTOR)
                .withSingleChoice("DedicatedGameMaster", ID_DEDICATED_GM)
                .withApplicabilityConstraint(greaterThan("age", 17))
                .build();
    }

    // =========================================================================
    // Relationships
    // =========================================================================

    public static List<ProductRelationship> relationships() {
        var factory = new ProductRelationshipFactory(new NoSelfRelationshipPolicy(), ProductRelationshipId::random);

        return List.of(
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
                define(factory, ID_CYBERPUNK_2077, ID_ALCATRAZ, ProductRelationshipType.INCOMPATIBLE_WITH));
    }

    // =========================================================================
    // Catalog entries
    //
    // Same product definitions, different offers per city. Cyberpunk 2077 appears only in
    // the Warsaw catalog (VR equipment installed there). Every other room and add-on is
    // available in all three cities.
    // =========================================================================

    /**
     * Returns catalog entries for the named city.
     *
     * <p>Cyberpunk 2077 is absent from the Krakow and Wroclaw catalogs - the VR equipment is only installed in Warsaw.
     * The absence of an entry IS the availability expression; no negative constraint is needed on the other cities'
     * entries.
     *
     * <p>Sales constraints on individual entries follow L08: restrictions that stem from operational context (equipment
     * state, day-of-week scheduling) live here, not on the product definition. Intrinsic product restrictions (Alcatraz
     * claustrophobia) stay on {@code ProductType.applicabilityConstraint}.
     *
     * @param city one of {@code "Warsaw"}, {@code "Krakow"}, {@code "Wroclaw"}
     */
    public static List<CatalogEntry> catalogEntriesFor(String city) {
        List<CatalogEntry> entries = new ArrayList<>();

        entries.add(entry(MAD_SCIENTIST_LAB, "room"));
        entries.add(entry(ALCATRAZ, "room"));
        entries.add(entry(EGYPTIAN_TOMB, "room"));

        if ("Warsaw".equals(city)) {
            // Cyberpunk is Warsaw-only because VR equipment is installed only there.
            // The city filter above already excludes other cities via entry absence (no entry
            // = not offered). The salesConstraint below adds the VR operational check on top:
            // if the headsets are down for maintenance, the entry becomes unavailable without
            // touching the product definition.
            //
            // On location as a ProductType constraint:
            // If city restrictions were definitional - e.g. local law prohibiting escape rooms
            // in a given jurisdiction - they would belong on ProductType.applicabilityConstraint,
            // because the restriction stems from what the product IS, not from where/how it is
            // commercially offered. In this case, however, Warsaw exclusivity is purely an
            // operational availability concern (equipment location), not a legal or definitional
            // one. Physical resource availability (equipment, stock, location) is the domain of
            // the availability sub-archetype within Inventory, not of the product definition.
            //
            // Note the split: `requiresVr=true` in ProductType.metadata says what the room IS
            // (it needs VR). The salesConstraint says whether VR is available RIGHT NOW at
            // this location - an operational state, not a product property.
            entries.add(
                    entry(CYBERPUNK_2077, "room", and(equalsTo("city", "Warsaw"), equalsTo("hasVrEquipment", "true"))));
        }

        // Actor is available in all cities, but only on weekends - a scheduling constraint
        // driven by staff availability, not by what the actor add-on IS as a product.
        // Weekend-only is a sales-context rule: a future corporate-events catalog could
        // offer actors on weekdays without changing the ProductType at all.
        //
        // Compare with Alcatraz's claustrophobia check: that lives on ProductType because
        // the room's physical properties are intrinsic and apply everywhere, always.
        entries.add(entry(ACTOR, "addon", in("dayType", "Saturday", "Sunday")));
        entries.add(entry(PHOTO_VIDEO, "addon"));
        entries.add(entry(CATERING, "addon"));
        entries.add(entry(DEDICATED_GM, "addon"));

        entries.add(entry(TEAM_BUILDING, "package"));
        entries.add(entry(HARDCORE, "package"));

        return List.copyOf(entries);
    }

    private static CatalogEntry entry(Product product, String category) {
        return entry(product, category, ApplicabilityConstraint.alwaysTrue());
    }

    private static CatalogEntry entry(Product product, String category, ApplicabilityConstraint salesConstraint) {
        return CatalogEntry.builder()
                .id(CatalogEntryId.generate())
                .product(product)
                .displayName(product.name().toString())
                .description(product.description().toString())
                .categories(Set.of(category))
                .validity(Validity.always())
                .salesConstraint(salesConstraint)
                .build();
    }

    private static ProductRelationship define(
            ProductRelationshipFactory factory,
            ProductIdentifier from,
            ProductIdentifier to,
            ProductRelationshipType type) {
        return factory.defineFor(from, to, type)
                .fold(
                        err -> {
                            throw new IllegalStateException("Relationship policy violated: " + err);
                        },
                        r -> r);
    }

    private static final class NoSelfRelationshipPolicy implements ProductRelationshipDefiningPolicy {

        @Override
        public boolean canDefineFor(ProductIdentifier from, ProductIdentifier to, ProductRelationshipType type) {
            return !from.equals(to);
        }
    }
}
