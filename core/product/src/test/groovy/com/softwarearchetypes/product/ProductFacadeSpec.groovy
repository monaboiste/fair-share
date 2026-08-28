package com.softwarearchetypes.product

import com.softwarearchetypes.common.Result
import com.softwarearchetypes.product.ProductCommands.AllowedValuesConfig
import com.softwarearchetypes.product.ProductCommands.DateRangeConfig
import com.softwarearchetypes.product.ProductCommands.DecimalRangeConfig
import com.softwarearchetypes.product.ProductCommands.DefineProductType
import com.softwarearchetypes.product.ProductCommands.MandatoryFeature
import com.softwarearchetypes.product.ProductCommands.NumericRangeConfig
import com.softwarearchetypes.product.ProductCommands.OptionalFeature
import com.softwarearchetypes.product.ProductCommands.RegexConfig
import com.softwarearchetypes.product.ProductCommands.UnconstrainedConfig
import com.softwarearchetypes.product.ProductQueries.FindByTrackingStrategyCriteria
import com.softwarearchetypes.product.ProductQueries.FindProductTypeCriteria
import com.softwarearchetypes.product.ProductViews.FeatureTypeView
import com.softwarearchetypes.product.ProductViews.ProductTypeView
import spock.lang.Specification

class ProductFacadeSpec extends Specification {

    private ProductConfiguration configuration
    private ProductFacade facade

    def setup() {
        configuration = ProductConfiguration.inMemory()
        facade = configuration.productFacade()
    }

    def "should define simple product type and find it"() {
        given:
        String productId = UUID.randomUUID().toString()

        when:
        Result<String, ProductIdentifier> result = facade.handle(new DefineProductType(
                "UUID",
                productId,
                "Simple Product",
                "A simple product without features",
                "pcs",
                "IDENTICAL",
                Set.of(),
                Set.of(),
                Map.of()
        ))

        then:
        result.success()

        when:
        ProductTypeView found = facade.findBy(new FindProductTypeCriteria(productId)).orElseThrow()

        then:
        "Simple Product" == found.name()
        "A simple product without features" == found.description()
        "IDENTICAL" == found.trackingStrategy()
    }

    def "should define product type with mandatory features and find it"() {
        given:
        String productId = UUID.randomUUID().toString()

        when:
        Result<String, ProductIdentifier> result = facade.handle(new DefineProductType(
                "UUID",
                productId,
                "Laptop",
                "Business laptop with configurable features",
                "pcs",
                "INDIVIDUALLY_TRACKED",
                Set.of(
                        new MandatoryFeature("color", new AllowedValuesConfig(Set.of("Black", "Silver", "Gold"))),
                        new MandatoryFeature("storage", new AllowedValuesConfig(Set.of("256GB", "512GB", "1TB")))
                ),
                Set.of(),
                Map.of("category", "electronics")
        ))

        then:
        result.success()

        when:
        ProductTypeView found = facade.findBy(new FindProductTypeCriteria(productId)).orElseThrow()

        then:
        "Laptop" == found.name()
        "INDIVIDUALLY_TRACKED" == found.trackingStrategy()
        2 == found.mandatoryFeatures().size()
    }

    def "should define product type with optional features and find it"() {
        given:
        String productId = UUID.randomUUID().toString()

        when:
        Result<String, ProductIdentifier> result = facade.handle(new DefineProductType(
                "UUID",
                productId,
                "Smartphone",
                "Smartphone with optional features",
                "pcs",
                "INDIVIDUALLY_TRACKED",
                Set.of(),
                Set.of(
                        new OptionalFeature("engraving", new UnconstrainedConfig("TEXT")),
                        new OptionalFeature("warranty_years", new NumericRangeConfig(1, 5))
                ),
                Map.of()
        ))

        then:
        result.success()

        when:
        ProductTypeView found = facade.findBy(new FindProductTypeCriteria(productId)).orElseThrow()

        then:
        2 == found.optionalFeatures().size()
    }

    def "should define product type with all constraint types"() {
        given:
        String productId = UUID.randomUUID().toString()

        when:
        Result<String, ProductIdentifier> result = facade.handle(new DefineProductType(
                "UUID",
                productId,
                "Complex Product",
                "Product with all constraint types",
                "pcs",
                "IDENTICAL",
                Set.of(
                        new MandatoryFeature("color", new AllowedValuesConfig(Set.of("Red", "Blue", "Green"))),
                        new MandatoryFeature("year", new NumericRangeConfig(2020, 2025)),
                        new MandatoryFeature("weight", new DecimalRangeConfig("0.1", "100.0")),
                        new MandatoryFeature("code", new RegexConfig("^[A-Z]{2}-\\d{4}" + String.valueOf((char) 36))),
                        new MandatoryFeature("expiry", new DateRangeConfig("2024-01-01", "2025-12-31")),
                        new MandatoryFeature("notes", new UnconstrainedConfig("TEXT"))
                ),
                Set.of(),
                Map.of()
        ))

        then:
        result.success()

        when:
        ProductTypeView found = facade.findBy(new FindProductTypeCriteria(productId)).orElseThrow()

        then:
        6 == found.mandatoryFeatures().size()
    }

    def "should fail for invalid identifier type"() {
        when:
        Result<String, ProductIdentifier> result = facade.handle(new DefineProductType(
                "INVALID_TYPE",
                "some-id",
                "Product",
                "Description",
                "pcs",
                "IDENTICAL",
                Set.of(),
                Set.of(),
                Map.of()
        ))

        then:
        result.failure()
        result.getFailure().contains("Unknown product identifier type")
    }

    def "should find #expectedCount product types with #trackingStrategy tracking"() {
        given:
        thereIsProductType("Bulk Coffee", "IDENTICAL")
        thereIsProductType("Tracked Laptop", "INDIVIDUALLY_TRACKED")
        thereIsProductType("Bulk Tea", "IDENTICAL")

        when:
        Set<ProductTypeView> products = facade.findBy(new FindByTrackingStrategyCriteria(trackingStrategy))

        then:
        expectedCount == products.size()

        where:
        trackingStrategy       | expectedCount
        "IDENTICAL"            | 2
        "INDIVIDUALLY_TRACKED" | 1
    }

    def "should return empty for non existent product"() {
        when:
        boolean found = facade.findBy(new FindProductTypeCriteria(UUID.randomUUID().toString())).isPresent()

        then:
        !found
    }

    def "should return correct feature type views"() {
        given:
        String productId = UUID.randomUUID().toString()
        facade.handle(new DefineProductType(
                "UUID",
                productId,
                "Product with Features",
                "Description",
                "pcs",
                "IDENTICAL",
                Set.of(new MandatoryFeature("color", new AllowedValuesConfig(Set.of("Red", "Blue")))),
                Set.of(new OptionalFeature("size", new NumericRangeConfig(1, 10))),
                Map.of()
        ))

        when:
        ProductTypeView view = facade.findBy(new FindProductTypeCriteria(productId)).orElseThrow()

        then:
        1 == view.mandatoryFeatures().size()
        1 == view.optionalFeatures().size()

        FeatureTypeView feature = (mandatory ? view.mandatoryFeatures() : view.optionalFeatures()).stream()
                .filter({ candidate -> candidate.name().equals(featureName) })
                .findFirst()
                .orElseThrow()
        valueType == feature.valueType()
        constraintType == feature.constraintType()
        expectedConfig.every { key, value -> feature.constraintConfig().get(key) == value }

        where:
        featureName | mandatory | valueType | constraintType  | expectedConfig
        "color"     | true      | "TEXT"    | "ALLOWED_VALUES" | [allowedValues: Set.of("Red", "Blue")]
        "size"      | false     | "INTEGER" | "NUMERIC_RANGE"  | [min: 1, max: 10]
    }

    def "should support isbn identifier"() {
        when:
        Result<String, ProductIdentifier> result = facade.handle(new DefineProductType(
                "ISBN",
                "0-201-77060-1",
                "Book",
                "A sample book",
                "pcs",
                "IDENTICAL",
                Set.of(),
                Set.of(),
                Map.of()
        ))

        then:
        result.success()
    }

    def "should support gtin identifier"() {
        when:
        Result<String, ProductIdentifier> result = facade.handle(new DefineProductType(
                "GTIN",
                "96385074",
                "Retail Product",
                "A product with GTIN",
                "pcs",
                "IDENTICAL",
                Set.of(),
                Set.of(),
                Map.of()
        ))

        then:
        result.success()
    }

    def "should reject null product id type"() {
        when:
        new DefineProductType(
                null,
                "id",
                "Name",
                "Description",
                "pcs",
                "IDENTICAL",
                Set.of(),
                Set.of(),
                Map.of()
        )

        then:
        thrown(IllegalArgumentException)
    }

    def "should reject blank product id"() {
        when:
        new DefineProductType(
                "UUID",
                "   ",
                "Name",
                "Description",
                "pcs",
                "IDENTICAL",
                Set.of(),
                Set.of(),
                Map.of()
        )

        then:
        thrown(IllegalArgumentException)
    }

    def "should reject blank name"() {
        when:
        new DefineProductType(
                "UUID",
                UUID.randomUUID().toString(),
                "",
                "Description",
                "pcs",
                "IDENTICAL",
                Set.of(),
                Set.of(),
                Map.of()
        )

        then:
        thrown(IllegalArgumentException)
    }

    private void thereIsProductType(String name, String trackingStrategy) {
        facade.handle(new DefineProductType(
                "UUID",
                UUID.randomUUID().toString(),
                name,
                "Description of " + name,
                "pcs",
                trackingStrategy,
                Set.of(),
                Set.of(),
                Map.of()
        ))
    }
}
