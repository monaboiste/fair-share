package com.softwarearchetypes.product

import java.util.function.Predicate
import spock.lang.Specification

class ProductRelationshipsQueriesSpec extends Specification {

    private final ProductRelationshipRepository repository = Mock()
    private final ProductRelationshipsQueries queries = new ProductRelationshipsQueries(repository)

    def "should find relationship by ID"() {
        given:
        ProductRelationshipId relationshipId = ProductRelationshipId.newOne()
        ProductRelationship relationship = relationship(relationshipId)
        repository.findBy(relationshipId) >> Optional.of(relationship)

        when:
        Optional<ProductRelationship> result = queries.findBy(relationshipId)

        then:
        result == Optional.of(relationship)
    }

    def "should find all relations from product without type"() {
        given:
        ProductIdentifier productIdentifier = ProductIdentifier.of("product")
        List<ProductRelationship> relationships = [relationship(ProductRelationshipId.newOne())]
        repository.findAllRelationsFrom(productIdentifier) >> relationships

        when:
        List<ProductRelationship> result = queries.findAllRelationsFrom(productIdentifier)

        then:
        result == relationships
    }

    def "should find all relations from product by type"() {
        given:
        ProductIdentifier productIdentifier = ProductIdentifier.of("product")
        ProductRelationshipType type = ProductRelationshipType.COMPLEMENTED_BY
        List<ProductRelationship> relationships = [relationship(ProductRelationshipId.newOne())]
        repository.findAllRelationsFrom(productIdentifier, type) >> relationships

        when:
        List<ProductRelationship> result = queries.findAllRelationsFrom(productIdentifier, type)

        then:
        result == relationships
    }

    def "should aggregate typed relations from multiple products"() {
        given:
        ProductIdentifier firstProduct = ProductIdentifier.of("first")
        ProductIdentifier secondProduct = ProductIdentifier.of("second")
        ProductRelationshipType type = ProductRelationshipType.COMPLEMENTED_BY
        ProductRelationship firstRelationship = relationship(ProductRelationshipId.newOne())
        ProductRelationship secondRelationship = relationship(ProductRelationshipId.newOne())
        repository.findAllRelationsFrom(firstProduct, type) >> [firstRelationship]
        repository.findAllRelationsFrom(secondProduct, type) >> [secondRelationship]

        when:
        List<ProductRelationship> result = queries.findAllRelationsFrom([firstProduct, secondProduct], type)

        then:
        result == [firstRelationship, secondRelationship]
    }

    def "should find matching relationships"() {
        given:
        Predicate<ProductRelationship> predicate = Mock()
        List<ProductRelationship> relationships = [relationship(ProductRelationshipId.newOne())]
        repository.findMatching(predicate) >> relationships

        when:
        List<ProductRelationship> result = queries.findMatching(predicate)

        then:
        result == relationships
    }

    private ProductRelationship relationship(ProductRelationshipId id) {
        new ProductRelationship(
                id,
                ProductIdentifier.of("from"),
                ProductIdentifier.of("to"),
                ProductRelationshipType.COMPLEMENTED_BY
        )
    }
}
