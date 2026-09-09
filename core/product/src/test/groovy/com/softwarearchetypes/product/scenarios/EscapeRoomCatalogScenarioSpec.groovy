package com.softwarearchetypes.product.scenarios

import com.softwarearchetypes.product.ApplicabilityContext
import com.softwarearchetypes.product.CatalogEntry
import com.softwarearchetypes.product.CatalogEntryId
import com.softwarearchetypes.product.ProductIdentifier
import com.softwarearchetypes.product.ProductRelationshipType
import com.softwarearchetypes.product.Validity
import com.softwarearchetypes.product.fixture.EscapeRoomCatalogFixture
import java.time.LocalDate
import spock.lang.Specification

class EscapeRoomCatalogScenarioSpec extends Specification {

    private static final LocalDate TODAY = LocalDate.of(2025, 6, 15)

    def "cyberpunk is available in Warsaw with VR equipment"() {
        given:
        List<CatalogEntry> catalog = EscapeRoomCatalogFixture.catalogEntriesFor("Warsaw")
        ApplicabilityContext context = ApplicabilityContext.of(Map.of(
                "city", "Warsaw",
                "hasVrEquipment", "true"))

        when:
        boolean available = catalog.stream()
                .filter({ entry -> entry.product().id() == EscapeRoomCatalogFixture.ID_CYBERPUNK_2077 })
                .anyMatch({ entry -> entry.isAvailableFor(context, TODAY) })

        then:
        available
    }

    def "cyberpunk is unavailable when Warsaw VR equipment is down"() {
        given:
        List<CatalogEntry> catalog = EscapeRoomCatalogFixture.catalogEntriesFor("Warsaw")
        ApplicabilityContext context = ApplicabilityContext.of(Map.of(
                "city", "Warsaw",
                "hasVrEquipment", "false"))

        when:
        boolean available = catalog.stream()
                .filter({ entry -> entry.product().id() == EscapeRoomCatalogFixture.ID_CYBERPUNK_2077 })
                .anyMatch({ entry -> entry.isAvailableFor(context, TODAY) })

        then:
        !available
    }

    def "cyberpunk is absent from the #city catalog"() {
        when:
        List<CatalogEntry> catalog = EscapeRoomCatalogFixture.catalogEntriesFor(city)

        then:
        !catalog.isEmpty()
        catalog.every { it.product().id() != EscapeRoomCatalogFixture.ID_CYBERPUNK_2077 }

        where:
        city << ["Krakow", "Wroclaw"]
    }

    def "regular rooms are available in #city"() {
        when:
        List<ProductIdentifier> productIds = EscapeRoomCatalogFixture.catalogEntriesFor(city)
                .collect { it.product().id() }

        then:
        productIds.containsAll([
                EscapeRoomCatalogFixture.ID_MAD_SCIENTIST_LAB,
                EscapeRoomCatalogFixture.ID_ALCATRAZ,
                EscapeRoomCatalogFixture.ID_EGYPTIAN_TOMB
        ])

        where:
        city << ["Warsaw", "Krakow", "Wroclaw"]
    }

    def "actor is available on #day"() {
        given:
        List<CatalogEntry> catalog = EscapeRoomCatalogFixture.catalogEntriesFor("Warsaw")
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("dayType", day))

        when:
        boolean available = catalog.stream()
                .filter({ entry -> entry.product().id() == EscapeRoomCatalogFixture.ID_ACTOR })
                .anyMatch({ entry -> entry.isAvailableFor(context, TODAY) })

        then:
        available

        where:
        day << ["Saturday", "Sunday"]
    }

    def "actor is unavailable on weekdays"() {
        given:
        List<CatalogEntry> catalog = EscapeRoomCatalogFixture.catalogEntriesFor("Warsaw")
        ApplicabilityContext context = ApplicabilityContext.of(Map.of("dayType", "Monday"))

        when:
        boolean available = catalog.stream()
                .filter({ entry -> entry.product().id() == EscapeRoomCatalogFixture.ID_ACTOR })
                .anyMatch({ entry -> entry.isAvailableFor(context, TODAY) })

        then:
        !available
    }

    def "expired catalog entry is unavailable"() {
        given:
        Validity lastYear = Validity.between(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31))
        CatalogEntry expired = CatalogEntry.builder()
                .id(CatalogEntryId.generate())
                .product(EscapeRoomCatalogFixture.MAD_SCIENTIST_LAB)
                .displayName("Mad Scientist's Laboratory")
                .description("Dismantle the professor's doomsday device before time runs out.")
                .categories(Set.of("room"))
                .validity(lastYear)
                .build()

        when:
        boolean available = expired.isAvailableAt(TODAY)

        then:
        !available
    }

    def "rooms form a difficulty upgrade path"() {
        given:
        def relationships = EscapeRoomCatalogFixture.relationships()

        expect:
        relationships.any {
            it.from() == from &&
                    it.to() == to &&
                    it.type() == ProductRelationshipType.UPGRADABLE_TO
        }

        where:
        from                                          | to
        EscapeRoomCatalogFixture.ID_EGYPTIAN_TOMB     | EscapeRoomCatalogFixture.ID_MAD_SCIENTIST_LAB
        EscapeRoomCatalogFixture.ID_MAD_SCIENTIST_LAB | EscapeRoomCatalogFixture.ID_ALCATRAZ
        EscapeRoomCatalogFixture.ID_ALCATRAZ          | EscapeRoomCatalogFixture.ID_CYBERPUNK_2077
    }

    def "every room is complemented by every add-on"() {
        given:
        def relationships = EscapeRoomCatalogFixture.relationships()

        def rooms = [
                EscapeRoomCatalogFixture.ID_MAD_SCIENTIST_LAB,
                EscapeRoomCatalogFixture.ID_ALCATRAZ,
                EscapeRoomCatalogFixture.ID_EGYPTIAN_TOMB,
                EscapeRoomCatalogFixture.ID_CYBERPUNK_2077
        ]

        def addOns = [
                EscapeRoomCatalogFixture.ID_ACTOR,
                EscapeRoomCatalogFixture.ID_PHOTO_VIDEO,
                EscapeRoomCatalogFixture.ID_CATERING,
                EscapeRoomCatalogFixture.ID_DEDICATED_GM
        ]

        expect:
        rooms.every { room ->
            addOns.every { addOn ->
                relationships.any {
                    it.from() == room &&
                            it.to() == addOn &&
                            it.type() == ProductRelationshipType.COMPLEMENTED_BY
                }
            }
        }
    }

    def "cyberpunk is incompatible with the other rooms"() {
        given:
        def relationships = EscapeRoomCatalogFixture.relationships()

        when:
        def incompatible = relationships.findAll {
            it.from() == EscapeRoomCatalogFixture.ID_CYBERPUNK_2077 &&
                    it.type() == ProductRelationshipType.INCOMPATIBLE_WITH
        }

        then:
        incompatible*.to() as Set == [
                EscapeRoomCatalogFixture.ID_EGYPTIAN_TOMB,
                EscapeRoomCatalogFixture.ID_MAD_SCIENTIST_LAB,
                EscapeRoomCatalogFixture.ID_ALCATRAZ
        ].toSet()
    }

    def "no relationship ever relates a product to itself"() {
        expect:
        EscapeRoomCatalogFixture.relationships().every {
            it.from() != it.to()
        }
    }
}
