package com.softwarearchetypes.product

import spock.lang.Specification

class ProductMetadataSpec extends Specification {

    def "should create empty metadata"() {
        when:
        ProductMetadata metadata = ProductMetadata.empty()

        then:
        metadata.asMap().isEmpty()
        !metadata.has("category")
    }

    def "should create metadata from map"() {
        given:
        Map<String, String> values = Map.of(
                "category", "coffee",
                "seasonal", "false",
                "brand", "Starbucks"
        )

        when:
        ProductMetadata metadata = ProductMetadata.of(values)

        then:
        metadata.has(key)
        expectedValue == metadata.get(key).orElseThrow()

        where:
        key        | expectedValue
        "category" | "coffee"
        "seasonal" | "false"
        "brand"    | "Starbucks"
    }

    def "should add metadata immutably"() {
        given:
        ProductMetadata original = ProductMetadata.empty()

        when:
        ProductMetadata withCategory = original.with("category", "coffee")
        ProductMetadata withSeasonal = withCategory.with("seasonal", "true")

        then:
        expected == [original: original, withCategory: withCategory, withSeasonal: withSeasonal]
                .get(version)
                .has(key)

        where:
        version        | key        | expected
        "original"     | "category" | false
        "withCategory" | "category" | true
        "withCategory" | "seasonal" | false
        "withSeasonal" | "category" | true
        "withSeasonal" | "seasonal" | true
    }

    def "should get with default"() {
        given:
        ProductMetadata metadata = ProductMetadata.of(Map.of("category", "coffee"))

        expect:
        expectedValue == metadata.getOrDefault(key, "unknown")

        where:
        key        | expectedValue
        "category" | "coffee"
        "brand"    | "unknown"
    }

    def "should copy nullable source data"() {
        given:
        Map<String, String> values = new HashMap<>(Map.of("category", "coffee"))

        when:
        ProductMetadata metadata = ProductMetadata.of(values)
        values.put("category", "tea")

        then:
        metadata.asMap() == Map.of("category", "coffee")
        metadata.toString() == "ProductMetadata{category=coffee}"
        ProductMetadata.of(null).asMap().isEmpty()
    }

    def "should use metadata in product type"() {
        when:
        ProductType productType = ProductType.builder(
                        UuidProductIdentifier.random(),
                        ProductName.of("Pumpkin Spice Latte"),
                        ProductDescription.of("Seasonal coffee"),
                        com.softwarearchetypes.quantity.Unit.pieces(),
                        ProductTrackingStrategy.IDENTICAL
                )
                .withMetadata("category", "coffee")
                .withMetadata("seasonal", "true")
                .withMetadata("season", "autumn")
                .build()

        then:
        expectedValue == productType.metadata().get(key).orElseThrow()

        where:
        key        | expectedValue
        "category" | "coffee"
        "seasonal" | "true"
        "season"   | "autumn"
    }
}
