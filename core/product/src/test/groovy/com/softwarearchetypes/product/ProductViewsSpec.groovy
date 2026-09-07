package com.softwarearchetypes.product

import spock.lang.Specification

class ProductViewsSpec extends Specification {

    def "should query metadata view attributes"() {
        given:
        ProductViews.MetadataView metadata = new ProductViews.MetadataView(Map.of("category", "mobile"))

        expect:
        metadata.get("category") == "mobile"
        metadata.get("missing") == null
        metadata.getOrDefault("missing", "default") == "default"
        metadata.has("category")
        !metadata.has("missing")
    }
}
