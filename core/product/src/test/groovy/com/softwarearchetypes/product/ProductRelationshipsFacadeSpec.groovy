package com.softwarearchetypes.product

import static com.softwarearchetypes.product.ProductRelationshipCommands.DefineRelationship
import static com.softwarearchetypes.product.ProductRelationshipCommands.RemoveRelationship

import com.softwarearchetypes.common.Result
import spock.lang.Specification

class ProductRelationshipsFacadeSpec extends Specification {

    private final InMemoryProductTypeRepository productTypeRepository = new InMemoryProductTypeRepository()
    private final ProductRelationshipFactory factory = new ProductRelationshipFactory(ProductRelationshipId::newOne)
    private final InMemoryProductRelationshipRepository relationshipRepository = new InMemoryProductRelationshipRepository()
    private final ProductRelationshipsFacade facade = new ProductRelationshipsFacade(factory, relationshipRepository, productTypeRepository)
    private final ProductRelationshipsQueries queries = new ProductRelationshipsQueries(relationshipRepository)

    def "fail to define relationship when from product does not exist"() {
        given:
        ProductIdentifier nonExistingFrom = UuidProductIdentifier.random()
        ProductType existingTo = thereIsProduct()

        when:
        Result<String, ProductRelationshipId> result = facade.handle(
                new DefineRelationship(
                        nonExistingFrom,
                        existingTo.identifier(),
                        ProductRelationshipType.UPGRADABLE_TO
                )
        )

        then:
        result.failure()
        result.getFailure().contains("PRODUCT_NOT_FOUND")
    }

    def "fail to define relationship when to product does not exist"() {
        given:
        ProductType existingFrom = thereIsProduct()
        ProductIdentifier nonExistingTo = UuidProductIdentifier.random()

        when:
        Result<String, ProductRelationshipId> result = facade.handle(
                new DefineRelationship(
                        existingFrom.identifier(),
                        nonExistingTo,
                        ProductRelationshipType.UPGRADABLE_TO
                )
        )

        then:
        result.failure()
        result.getFailure().contains("PRODUCT_NOT_FOUND")
    }

    def "define relationship between products"() {
        given:
        ProductType smallCoffee = thereIsProduct()
        ProductType largeCoffee = thereIsProduct()

        when:
        Result<String, ProductRelationshipId> result = facade.handle(
                new DefineRelationship(
                        smallCoffee.identifier(),
                        largeCoffee.identifier(),
                        ProductRelationshipType.UPGRADABLE_TO
                )
        )

        then:
        result.success()

        when:
        ProductRelationshipId relationshipId = result.getSuccess()
        ProductRelationship relationship = queries.findBy(relationshipId).orElseThrow()

        then:
        relationship.from() == smallCoffee.identifier()
        relationship.to() == largeCoffee.identifier()
        relationship.type() == ProductRelationshipType.UPGRADABLE_TO
    }

    def "remove the correct relationship"() {
        given:
        ProductType smallCoffee = thereIsProduct()
        ProductType largeCoffee = thereIsProduct()
        ProductRelationshipId relationshipId = facade.handle(
                        new DefineRelationship(
                                smallCoffee.identifier(),
                                largeCoffee.identifier(),
                                ProductRelationshipType.UPGRADABLE_TO
                        )
                )
                .getSuccess()

        when:
        Result<String, ProductRelationshipId> result = facade.handle(
                new RemoveRelationship(relationshipId)
        )

        then:
        result.success()
        result.getSuccess() == relationshipId
        queries.findBy(relationshipId).isEmpty()
    }

    def "remove relationship between products"() {
        given:
        ProductType smallCoffee = thereIsProduct()
        ProductType largeCoffee = thereIsProduct()
        ProductRelationshipId relationshipId = facade.handle(
                        new DefineRelationship(
                                smallCoffee.identifier(),
                                largeCoffee.identifier(),
                                ProductRelationshipType.UPGRADABLE_TO
                        )
                )
                .getSuccess()

        when:
        Result<String, ProductRelationshipId> result = facade.handle(
                new RemoveRelationship(relationshipId)
        )

        then:
        result.success()
        queries.findBy(relationshipId).isEmpty()
    }

    def "find all relations from product"() {
        given:
        ProductType burger = thereIsProduct()
        ProductType fries = thereIsProduct()
        ProductType coke = thereIsProduct()
        facade.handle(new DefineRelationship(
                burger.identifier(),
                fries.identifier(),
                ProductRelationshipType.COMPLEMENTED_BY
        ))
        facade.handle(new DefineRelationship(
                burger.identifier(),
                coke.identifier(),
                ProductRelationshipType.COMPLEMENTED_BY
        ))

        when:
        List<ProductRelationship> relations = queries.findAllRelationsFrom(burger.identifier(), ProductRelationshipType.COMPLEMENTED_BY)

        then:
        relations.size() == 2
        relations.stream().allMatch(rel -> rel.from() == burger.identifier())
        relations.stream().allMatch(rel -> rel.type() == ProductRelationshipType.COMPLEMENTED_BY)
    }

    private ProductType thereIsProduct() {
        ProductType productType = ProductType.define(
                UuidProductIdentifier.random(),
                ProductName.of("Test Product"),
                ProductDescription.of("Description")
        )
        productTypeRepository.save(productType)
        return productType
    }
}
