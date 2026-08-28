package com.softwarearchetypes.product

import spock.lang.Specification

class ProductFeatureInstancesSpec extends Specification {

    private ProductFeatureType color = ProductFeatureType.withAllowedValues("color", "blue")
    private ProductFeatureType size = ProductFeatureType.withAllowedValues("size", "small")
    private ProductFeatureTypes featureTypes = ProductFeatureTypes.of(
            ProductFeatureTypeDefinition.mandatory(color),
            ProductFeatureTypeDefinition.optional(size))

    def "should validate defined mandatory and optional features"() {
        given:
        ProductFeatureInstances instances = ProductFeatureInstances.of(
                ProductFeatureInstance.of(color, "blue"),
                ProductFeatureInstance.of(size, "small"))

        when:
        instances.validateAgainst(featureTypes)

        then:
        noExceptionThrown()
        instances.get("color").isPresent()
        instances.all().size() == 2
        !instances.isEmpty()
        instances.toString().contains("color=blue")
    }

    def "should reject a missing mandatory feature"() {
        given:
        ProductFeatureInstances instances = ProductFeatureInstances.of(
                ProductFeatureInstance.of(size, "small"))

        when:
        instances.validateAgainst(featureTypes)

        then:
        thrown(IllegalArgumentException)
    }

    def "should reject a feature absent from the product type"() {
        given:
        ProductFeatureType unknown = ProductFeatureType.withAllowedValues("unknown", "value")
        ProductFeatureInstances instances = ProductFeatureInstances.of(
                ProductFeatureInstance.of(color, "blue"),
                ProductFeatureInstance.of(unknown, "value"))

        when:
        instances.validateAgainst(featureTypes)

        then:
        thrown(IllegalArgumentException)
    }

    def "should reject duplicate feature names"() {
        when:
        new ProductFeatureInstances(List.of(
                ProductFeatureInstance.of(color, "blue"),
                ProductFeatureInstance.of(color, "blue")))

        then:
        thrown(IllegalStateException)
    }
}
