package com.softwarearchetypes.product

import spock.lang.Specification

class ProductFeatureTypesSpec extends Specification {

    def "should expose feature definitions by name and requirement"() {
        given:
        ProductFeatureType color = ProductFeatureType.withAllowedValues("color", "blue")
        ProductFeatureType size = ProductFeatureType.withAllowedValues("size", "small")
        ProductFeatureTypeDefinition mandatory = ProductFeatureTypeDefinition.mandatory(color)
        ProductFeatureTypeDefinition optional = ProductFeatureTypeDefinition.optional(size)

        when:
        ProductFeatureTypes features = ProductFeatureTypes.of(mandatory, optional)

        then:
        features.get("color").orElseThrow() == mandatory
        features.getFeatureType("size").orElseThrow() == size
        features.has("color")
        features.isMandatory("color")
        !features.isMandatory("size")
        features.mandatoryFeatures() == Set.of(color)
        features.optionalFeatures() == Set.of(size)
        features.allFeatures() == Set.of(color, size)
        features.size() == 2
        !features.isEmpty()
        features.toString() == "ProductFeatureTypes{mandatory=1, optional=1}"
    }

    def "should create an empty feature collection"() {
        when:
        ProductFeatureTypes features = ProductFeatureTypes.empty()

        then:
        features.isEmpty()
        features.size() == 0
        features.get("missing").isEmpty()
        features.getFeatureType("missing").isEmpty()
        !features.has("missing")
        !features.isMandatory("missing")
        features.allFeatures().isEmpty()
    }

    def "should describe mandatory and optional definitions"() {
        given:
        ProductFeatureType feature = ProductFeatureType.withAllowedValues("color", "blue")

        when:
        ProductFeatureTypeDefinition mandatory = ProductFeatureTypeDefinition.mandatory(feature)
        ProductFeatureTypeDefinition optional = ProductFeatureTypeDefinition.optional(feature)

        then:
        mandatory.featureType() == feature
        mandatory.mandatory()
        mandatory.isMandatory()
        !mandatory.isOptional()
        mandatory.toString() == "mandatory(color)"
        optional.isOptional()
        !optional.isMandatory()
        optional.toString() == "optional(color)"
    }
}
