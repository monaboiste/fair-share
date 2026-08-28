package com.softwarearchetypes.product

import com.softwarearchetypes.common.Result
import com.softwarearchetypes.product.ProductCommands.AddToOffer
import com.softwarearchetypes.product.ProductCommands.DiscontinueProduct
import com.softwarearchetypes.product.ProductCommands.UpdateMetadata
import com.softwarearchetypes.product.ProductQueries.FindAvailableAtCriteria
import com.softwarearchetypes.product.ProductQueries.FindByCategoryCriteria
import com.softwarearchetypes.product.ProductQueries.FindByMetadataCriteria
import com.softwarearchetypes.product.ProductQueries.FindCatalogEntryCriteria
import com.softwarearchetypes.product.ProductQueries.SearchCatalogCriteria
import com.softwarearchetypes.product.ProductViews.CatalogEntryView
import java.time.LocalDate
import spock.lang.Specification

class ProductCatalogSpec extends Specification {

    private ProductConfiguration configuration
    private ProductCatalog catalog
    private ProductTypeRepository productTypeRepository

    def setup() {
        configuration = ProductConfiguration.inMemory()
        catalog = configuration.productCatalog()
        productTypeRepository = configuration.productTypeRepository()
    }

    def "should add product to offer and find it by id"() {
        given:
        ProductType laptop = thereIsProduct("Business Laptop")

        when:
        Result<String, CatalogEntryId> result = catalog.handle(new AddToOffer(
                laptop.id().toString(),
                "Premium Laptop",
                "High-end business laptop",
                Set.of("electronics"),
                null,
                null,
                Map.of()
        ))

        then:
        result.success()

        when:
        CatalogEntryView found = catalog.findBy(new FindCatalogEntryCriteria(result.getSuccess().value())).orElseThrow()

        then:
        "Premium Laptop" == found.displayName()
        "High-end business laptop" == found.description()
        laptop.id().toString() == found.productTypeId()
    }

    def "should fail to add non existent product to offer"() {
        given:
        ProductIdentifier nonExistent = UuidProductIdentifier.random()

        when:
        Result<String, CatalogEntryId> result = catalog.handle(new AddToOffer(
                nonExistent.toString(),
                "Ghost Product",
                "Does not exist",
                Set.of(),
                null,
                null,
                Map.of()
        ))

        then:
        result.failure()
        result.getFailure().contains("not found")
    }

    def "should find #expectedNames by #category category"() {
        given:
        ProductType laptop = thereIsProduct("Laptop")
        ProductType phone = thereIsProduct("Phone")
        thereIsCatalogEntry(laptop, "Gaming Laptop", Set.of("electronics", "gaming"))
        thereIsCatalogEntry(phone, "Smartphone", Set.of("electronics", "phones"))

        when:
        Set<CatalogEntryView> matchingEntries = catalog.findBy(new FindByCategoryCriteria(category))

        then:
        expectedNames == matchingEntries.collect({ entry -> entry.displayName() }).toSet()

        where:
        category      | expectedNames
        "electronics" | Set.of("Gaming Laptop", "Smartphone")
        "gaming"      | Set.of("Gaming Laptop")
        "phones"      | Set.of("Smartphone")
    }

    def "should return empty set for non existent category"() {
        given:
        ProductType laptop = thereIsProduct("Laptop")
        thereIsCatalogEntry(laptop, "Laptop", Set.of("electronics"))

        when:
        Set<CatalogEntryView> result = catalog.findBy(new FindByCategoryCriteria("non-existent"))

        then:
        result.isEmpty()
    }

    def "should find #expectedNames available on #availableAt"() {
        given:
        ProductType laptop = thereIsProduct("Laptop")
        ProductType phone = thereIsProduct("Phone")
        thereIsCatalogEntryWithValidity(laptop, "2024 Laptop", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))
        thereIsCatalogEntryWithValidity(phone, "Always Phone", null, null)

        when:
        Set<CatalogEntryView> availableEntries = catalog.findBy(new FindAvailableAtCriteria(availableAt))

        then:
        expectedNames == availableEntries.collect({ entry -> entry.displayName() }).toSet()

        where:
        availableAt                 | expectedNames
        LocalDate.of(2024, 6, 15)   | Set.of("2024 Laptop", "Always Phone")
        LocalDate.of(2025, 6, 15)   | Set.of("Always Phone")
    }

    def "should find #expectedNames on #availableAt after product is discontinued"() {
        given:
        ProductType laptop = thereIsProduct("Laptop")
        CatalogEntryId entryId = thereIsCatalogEntryWithValidity(laptop, "Old Laptop", LocalDate.of(2020, 1, 1), null)
        catalog.handle(new DiscontinueProduct(entryId.value(), LocalDate.of(2023, 12, 31)))

        when:
        Set<CatalogEntryView> availableEntries = catalog.findBy(new FindAvailableAtCriteria(availableAt))

        then:
        expectedNames == availableEntries.collect({ entry -> entry.displayName() }).toSet()

        where:
        availableAt                 | expectedNames
        LocalDate.of(2024, 6, 15)   | Set.of()
        LocalDate.of(2023, 6, 15)   | Set.of("Old Laptop")
    }

    def "should find #expectedNames by metadata #key=#value"() {
        given:
        ProductType laptop = thereIsProduct("Laptop")
        ProductType phone = thereIsProduct("Phone")
        thereIsCatalogEntryWithMetadata(laptop, "Featured Laptop", Map.of("featured", "true", "brand", "Dell"))
        thereIsCatalogEntryWithMetadata(phone, "Regular Phone", Map.of("featured", "false", "brand", "Samsung"))

        when:
        Set<CatalogEntryView> matchingEntries = catalog.findBy(new FindByMetadataCriteria(key, value))

        then:
        expectedNames == matchingEntries.collect({ entry -> entry.displayName() }).toSet()

        where:
        key        | value  | expectedNames
        "featured" | "true" | Set.of("Featured Laptop")
        "brand"    | "Dell" | Set.of("Featured Laptop")
    }

    def "should find products by metadata key only"() {
        given:
        ProductType laptop = thereIsProduct("Laptop")
        ProductType phone = thereIsProduct("Phone")
        thereIsCatalogEntryWithMetadata(laptop, "Laptop with brand", Map.of("brand", "Dell"))
        thereIsCatalogEntryWithMetadata(phone, "Phone without brand", Map.of())

        when:
        Set<CatalogEntryView> withBrand = catalog.findBy(new FindByMetadataCriteria("brand", null))

        then:
        1 == withBrand.size()
        withBrand.stream().anyMatch({ e -> e.displayName().equals("Laptop with brand") })
    }

    def "should find #expectedName by #searchText text"() {
        given:
        ProductType laptop = thereIsProduct("Laptop")
        ProductType phone = thereIsProduct("Phone")
        thereIsCatalogEntry(laptop, "Gaming Laptop Pro", Set.of())
        thereIsCatalogEntry(phone, "Budget Smartphone", Set.of())

        when:
        Set<CatalogEntryView> matchingEntries = catalog.findBy(SearchCatalogCriteria.byText(searchText))

        then:
        1 == matchingEntries.size()
        expectedName == matchingEntries.first().displayName()

        where:
        searchText   | expectedName
        "Laptop"     | "Gaming Laptop Pro"
        "Smartphone" | "Budget Smartphone"
    }

    def "should find Gaming Laptop with case-insensitive #searchText text"() {
        given:
        ProductType laptop = thereIsProduct("Laptop")
        thereIsCatalogEntry(laptop, "Gaming Laptop", Set.of())

        when:
        Set<CatalogEntryView> matchingEntries = catalog.findBy(SearchCatalogCriteria.byText(searchText))

        then:
        1 == matchingEntries.size()
        "Gaming Laptop" == matchingEntries.first().displayName()

        where:
        searchText << ["GAMING", "gaming"]
    }

    def "should return all entries when no filters"() {
        given:
        ProductType laptop = thereIsProduct("Laptop")
        ProductType phone = thereIsProduct("Phone")
        thereIsCatalogEntry(laptop, "Laptop", Set.of())
        thereIsCatalogEntry(phone, "Phone", Set.of())

        when:
        Set<CatalogEntryView> all = catalog.findBy(SearchCatalogCriteria.all())

        then:
        2 == all.size()
    }

    def "should combine catalog search filters"() {
        given:
        ProductType laptop = thereIsProduct("Laptop")
        ProductType phone = thereIsProduct("Phone")
        thereIsCatalogEntryWithValidity(laptop, "Gaming Laptop", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))
        thereIsCatalogEntryWithValidity(phone, "Gaming Phone", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))

        when:
        Set<CatalogEntryView> matchingEntries = catalog.findBy(new SearchCatalogCriteria(
                null,
                null,
                LocalDate.of(2025, 6, 1),
                laptop.id().toString(),
                null
        ))

        then:
        matchingEntries*.displayName() as Set == Set.of("Gaming Laptop")
    }

    def "should search catalog by any matching category"() {
        given:
        ProductType laptop = thereIsProduct("Laptop")
        ProductType phone = thereIsProduct("Phone")
        thereIsCatalogEntry(laptop, "Gaming Laptop", Set.of("electronics", "gaming"))
        thereIsCatalogEntry(phone, "Business Phone", Set.of("electronics", "business"))

        when:
        Set<CatalogEntryView> matchingEntries = catalog.findBy(SearchCatalogCriteria.byCategories(categories))

        then:
        matchingEntries*.displayName() as Set == expectedNames

        where:
        categories                    | expectedNames
        Set.of("gaming", "business") | Set.of("Gaming Laptop", "Business Phone")
        Set.of("missing")             | Set.of()
    }

    def "should search catalog by feature constraints"() {
        given:
        ProductFeatureType color = ProductFeatureType.withAllowedValues("color", "blue", "black")
        ProductFeatureType storage = ProductFeatureType.withAllowedValues("storage", "256GB", "512GB")
        ProductType phone = ProductType.builder(
                        UuidProductIdentifier.random(),
                        ProductName.of("Phone"),
                        ProductDescription.of("Configurable phone"),
                        com.softwarearchetypes.quantity.Unit.pieces(),
                        ProductTrackingStrategy.IDENTICAL
                )
                .withMandatoryFeature(color)
                .withOptionalFeature(storage)
                .build()
        productTypeRepository.save(phone)
        thereIsCatalogEntry(phone, "Configurable Phone", Set.of("mobile"))

        when:
        Set<CatalogEntryView> matchingEntries = catalog.findBy(SearchCatalogCriteria.byFeatures(features))

        then:
        matchingEntries*.displayName() as Set == expectedNames

        where:
        features                                                        | expectedNames
        Map.of("color", Set.of("blue"))                                 | Set.of("Configurable Phone")
        Map.of("storage", Set.of("128GB", "512GB"))                     | Set.of("Configurable Phone")
        Map.of("color", Set.of("blue"), "storage", Set.of("512GB"))   | Set.of("Configurable Phone")
        Map.of("color", Set.of("green"))                                | Set.of()
        Map.of("unknown", Set.of("blue"))                               | Set.of()
    }

    def "should exclude package types from feature searches"() {
        given:
        ProductType component = thereIsProduct("Component")
        PackageType bundle = Product.builder(
                        UuidProductIdentifier.random(),
                        ProductName.of("Bundle"),
                        ProductDescription.of("Bundle description")
                )
                .asPackageType()
                .withSingleChoice("component", component.id())
                .build()
        CatalogEntry entry = CatalogEntry.builder()
                .id(CatalogEntryId.generate())
                .displayName("Bundle")
                .description("Bundle description")
                .product(bundle)
                .categories(Set.of())
                .validity(Validity.always())
                .build()
        configuration.catalogEntryRepository().save(entry)

        when:
        Set<CatalogEntryView> matchingEntries = catalog.findBy(SearchCatalogCriteria.byFeatures(
                Map.of("color", Set.of("blue"))))

        then:
        matchingEntries.isEmpty()
    }

    def "should discontinue an entry without a start date"() {
        given:
        ProductType laptop = thereIsProduct("Laptop")
        CatalogEntryId entryId = thereIsCatalogEntryWithValidity(laptop, "Laptop", null, null)
        LocalDate discontinuationDate = LocalDate.of(2025, 6, 30)

        when:
        Result<String, CatalogEntryId> result = catalog.handle(new DiscontinueProduct(entryId.value(), discontinuationDate))

        then:
        result.success()
        CatalogEntryView entry = catalog.findBy(new FindCatalogEntryCriteria(entryId.value())).orElseThrow()
        entry.availableFrom() == null
        entry.availableUntil() == discontinuationDate
    }

    def "should discontinue product"() {
        given:
        ProductType laptop = thereIsProduct("Laptop")
        CatalogEntryId entryId = thereIsCatalogEntryWithValidity(laptop, "Old Laptop", LocalDate.of(2020, 1, 1), null)

        when:
        Result<String, CatalogEntryId> result = catalog.handle(new DiscontinueProduct(
                entryId.value(),
                LocalDate.of(2024, 6, 30)
        ))

        then:
        result.success()

        when:
        CatalogEntryView updated = catalog.findBy(new FindCatalogEntryCriteria(entryId.value())).orElseThrow()

        then:
        LocalDate.of(2024, 6, 30) == updated.availableUntil()
    }

    def "should fail to discontinue non existent entry"() {
        when:
        Result<String, CatalogEntryId> result = catalog.handle(new DiscontinueProduct(
                "non-existent-id",
                LocalDate.of(2024, 6, 30)
        ))

        then:
        result.failure()
    }

    def "should update metadata"() {
        given:
        ProductType laptop = thereIsProduct("Laptop")
        CatalogEntryId entryId = thereIsCatalogEntryWithMetadata(laptop, "Laptop", Map.of("featured", "false"))

        when:
        Result<String, CatalogEntryId> result = catalog.handle(new UpdateMetadata(
                entryId.value(),
                Map.of("featured", "true", "badge", "sale")
        ))

        then:
        result.success()

        when:
        CatalogEntryView updated = catalog.findBy(new FindCatalogEntryCriteria(entryId.value())).orElseThrow()

        then:
        "true" == updated.metadata().get("featured")
        "sale" == updated.metadata().get("badge")
    }

    def "should fail to update metadata for non existent entry"() {
        when:
        Result<String, CatalogEntryId> result = catalog.handle(new UpdateMetadata(
                "non-existent-id",
                Map.of("featured", "true")
        ))

        then:
        result.failure()
    }

    def "should return correct view fields"() {
        given:
        ProductType laptop = thereIsProduct("Laptop")
        Result<String, CatalogEntryId> result = catalog.handle(new AddToOffer(
                laptop.id().toString(),
                "Test Display Name",
                "Test Description",
                Set.of("cat1", "cat2"),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                Map.of("key1", "value1")
        ))

        when:
        Optional<CatalogEntryView> found = catalog.findBy(new FindCatalogEntryCriteria(result.getSuccess().value()))

        then:
        found.isPresent()
        CatalogEntryView view = found.get()
        result.getSuccess().value() == view.catalogEntryId()
        "Test Display Name" == view.displayName()
        "Test Description" == view.description()
        laptop.id().toString() == view.productTypeId()
        Set.of("cat1", "cat2") == view.categories()
        LocalDate.of(2024, 1, 1) == view.availableFrom()
        LocalDate.of(2024, 12, 31) == view.availableUntil()
        Map.of("key1", "value1") == view.metadata()
    }

    private ProductType thereIsProduct(String name) {
        ProductType productType = ProductType.define(
                UuidProductIdentifier.random(),
                ProductName.of(name),
                ProductDescription.of("Description of " + name)
        )
        productTypeRepository.save(productType)
        return productType
    }

    private CatalogEntryId thereIsCatalogEntry(ProductType product, String displayName, Set<String> categories) {
        Result<String, CatalogEntryId> result = catalog.handle(new AddToOffer(
                product.id().toString(),
                displayName,
                "Description of " + displayName,
                categories,
                null,
                null,
                Map.of()
        ))
        return result.getSuccess()
    }

    private CatalogEntryId thereIsCatalogEntryWithValidity(ProductType product, String displayName, LocalDate from, LocalDate to) {
        Result<String, CatalogEntryId> result = catalog.handle(new AddToOffer(
                product.id().toString(),
                displayName,
                "Description of " + displayName,
                Set.of(),
                from,
                to,
                Map.of()
        ))
        return result.getSuccess()
    }

    private CatalogEntryId thereIsCatalogEntryWithMetadata(ProductType product, String displayName, Map<String, String> metadata) {
        Result<String, CatalogEntryId> result = catalog.handle(new AddToOffer(
                product.id().toString(),
                displayName,
                "Description of " + displayName,
                Set.of(),
                null,
                null,
                metadata
        ))
        return result.getSuccess()
    }
}
