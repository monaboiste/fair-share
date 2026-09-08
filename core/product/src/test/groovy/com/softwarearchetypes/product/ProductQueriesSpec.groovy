package com.softwarearchetypes.product

import com.softwarearchetypes.product.ProductQueries.SearchCatalogCriteria
import java.time.LocalDate
import spock.lang.Specification

class ProductQueriesSpec extends Specification {

    def "should create #filter search criteria"() {
        when:
        SearchCatalogCriteria criteria = factory()

        then:
        criteria.searchText() == searchText
        criteria.categories() == categories
        criteria.availableAt() == availableAt
        criteria.productTypeId() == productTypeId
        criteria.productTypeFeatures() == productTypeFeatures

        where:
        filter         | factory                                                               | searchText | categories       | availableAt              | productTypeId | productTypeFeatures
        "no"           | { SearchCatalogCriteria.all() }                                       | null       | null             | null                     | null          | null
        "text"         | { SearchCatalogCriteria.byText("phone") }                             | "phone"    | null             | null                     | null          | null
        "category"     | { SearchCatalogCriteria.byCategories(Set.of("mobile")) }              | null       | Set.of("mobile") | null                     | null          | null
        "availability" | { SearchCatalogCriteria.availableAt(LocalDate.of(2025, 6, 1)) }       | null       | null             | LocalDate.of(2025, 6, 1) | null          | null
        "product type" | { SearchCatalogCriteria.byProductType("phone-id") }                   | null       | null             | null                     | "phone-id"    | null
        "features"     | { SearchCatalogCriteria.byFeatures(Map.of("color", Set.of("blue"))) } | null       | null             | null                     | null          | Map.of("color", Set.of("blue"))
    }

    def "should reject missing #field"() {
        when:
        constructor()

        then:
        thrown(IllegalArgumentException)

        where:
        field               | constructor
        "product ID"        | { new ProductQueries.FindProductTypeCriteria(" ") }
        "tracking strategy" | { new ProductQueries.FindByTrackingStrategyCriteria(null) }
        "catalog entry ID"  | { new ProductQueries.FindCatalogEntryCriteria("") }
        "category"          | { new ProductQueries.FindByCategoryCriteria(null) }
        "availability date" | { new ProductQueries.FindAvailableAtCriteria(null) }
        "metadata key"      | { new ProductQueries.FindByMetadataCriteria(" ", null) }
    }
}
