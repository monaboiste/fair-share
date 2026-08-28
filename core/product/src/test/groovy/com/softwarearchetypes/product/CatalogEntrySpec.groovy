package com.softwarearchetypes.product

import java.time.LocalDate
import spock.lang.Specification

class CatalogEntrySpec extends Specification {

    private ProductType product = ProductType.define(
            UuidProductIdentifier.random(),
            ProductName.of("Product"),
            ProductDescription.of("Description"))

    def "should build and copy a catalog entry"() {
        given:
        CatalogEntryId id = CatalogEntryId.generate()
        ApplicabilityConstraint salesConstraint = ApplicabilityConstraint.equalsTo("channel", "web")
        CatalogEntry entry = CatalogEntry.builder()
                .id(id)
                .displayName("Offer")
                .description("Offer description")
                .product(product)
                .category("featured")
                .validity(Validity.always())
                .withMetadata("badge", "new")
                .salesConstraint(salesConstraint)
                .build()

        when:
        CatalogEntry withValidity = entry.withValidity(Validity.until(LocalDate.of(2025, 12, 31)))
        CatalogEntry withMetadata = entry.withMetadata(Map.of("badge", "sale"))

        then:
        entry.id() == id
        entry.product() == product
        entry.categories() == Set.of("featured")
        entry.salesConstraint() == salesConstraint
        entry.hasMetadata("badge")
        entry.getMetadata("badge").orElseThrow() == "new"
        entry.getMetadataOrDefault("missing", "default") == "default"
        withValidity.validity() == Validity.until(LocalDate.of(2025, 12, 31))
        withMetadata.getMetadata("badge").orElseThrow() == "sale"
        entry.toString().contains("Offer")
    }

    def "should compare entries by identifier"() {
        given:
        CatalogEntryId id = CatalogEntryId.generate()

        expect:
        entry(id, "First") == entry(id, "Second")
        entry(id, "First").hashCode() == entry(id, "Second").hashCode()
        entry(id, "First") != entry(CatalogEntryId.generate(), "First")
        !entry(id, "First").equals("not an entry")
    }

    def "should reject an entry with missing #field"() {
        given:
        CatalogEntry.Builder builder = CatalogEntry.builder()
                .id(CatalogEntryId.generate())
                .displayName("Offer")
                .description("Description")
                .product(product)
                .validity(Validity.always())
        configureMissing(builder, field)

        when:
        builder.build()

        then:
        thrown(IllegalArgumentException)

        where:
        field << ["identifier", "display name", "description", "product", "validity", "sales constraint"]
    }

    private CatalogEntry entry(CatalogEntryId id, String displayName) {
        return CatalogEntry.builder()
                .id(id)
                .displayName(displayName)
                .description("Description")
                .product(product)
                .validity(Validity.always())
                .build()
    }

    private static void configureMissing(CatalogEntry.Builder builder, String field) {
        switch (field) {
            case "identifier" -> builder.id(null)
            case "display name" -> builder.displayName(" ")
            case "description" -> builder.description(null)
            case "product" -> builder.product(null)
            case "validity" -> builder.validity(null)
            case "sales constraint" -> builder.salesConstraint(null)
        }
    }
}
