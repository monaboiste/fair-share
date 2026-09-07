package com.softwarearchetypes.product

import com.softwarearchetypes.quantity.Unit
import spock.lang.Specification

class ProductTypeSpec extends Specification {

    def "factory methods produce the expected tracking strategy and unit"() {
        when:
        ProductType product = buildProduct(strategy, unit)

        then:
        product.trackingStrategy() == strategy
        product.preferredUnit() == unit
        product.identifier() == product.id()
        product.applicabilityConstraint() != null
        product.featureTypes() != null
        product.metadata() != null
        product.toString().contains(strategy.name())

        where:
        strategy                                               | unit
        ProductTrackingStrategy.IDENTICAL                      | Unit.pieces()
        ProductTrackingStrategy.UNIQUE                         | Unit.pieces()
        ProductTrackingStrategy.INDIVIDUALLY_TRACKED           | Unit.kilograms()
        ProductTrackingStrategy.BATCH_TRACKED                  | Unit.liters()
        ProductTrackingStrategy.INDIVIDUALLY_AND_BATCH_TRACKED | Unit.pieces()
    }

    def "should reject missing #field"() {
        when:
        new ProductType(
                field == "identifier" ? null : UuidProductIdentifier.random(),
                field == "name" ? null : ProductName.of("Product"),
                field == "description" ? null : ProductDescription.of("Description"),
                field == "unit" ? null : Unit.pieces(),
                field == "tracking strategy" ? null : ProductTrackingStrategy.IDENTICAL,
                field == "feature types" ? null : ProductFeatureTypes.empty(),
                field == "metadata" ? null : ProductMetadata.empty(),
                field == "applicability constraint" ? null : ApplicabilityConstraint.alwaysTrue())

        then:
        thrown(IllegalArgumentException)

        where:
        field << [
                "identifier",
                "name",
                "description",
                "unit",
                "tracking strategy",
                "feature types",
                "metadata",
                "applicability constraint"
        ]
    }

    private static ProductType buildProduct(ProductTrackingStrategy strategy, Unit unit) {
        ProductIdentifier id = UuidProductIdentifier.random()
        ProductName name = ProductName.of("Product")
        ProductDescription description = ProductDescription.of("Description")
        switch (strategy) {
            case ProductTrackingStrategy.UNIQUE: return ProductType.unique(id, name, description)
            case ProductTrackingStrategy.INDIVIDUALLY_TRACKED: return ProductType.individuallyTracked(id, name, description, unit)
            case ProductTrackingStrategy.BATCH_TRACKED: return ProductType.batchTracked(id, name, description, unit)
            case ProductTrackingStrategy.INDIVIDUALLY_AND_BATCH_TRACKED: return ProductType.individuallyAndBatchTracked(id, name, description, unit)
            default: return ProductType.define(id, name, description)
        }
    }
}
