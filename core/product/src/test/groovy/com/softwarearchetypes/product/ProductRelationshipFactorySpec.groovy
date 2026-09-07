package com.softwarearchetypes.product

import com.softwarearchetypes.common.Result
import java.util.function.Supplier
import spock.lang.Specification

class ProductRelationshipFactorySpec extends Specification {

    def "define relationship when policy allows it"() {
        given:
        ProductRelationshipDefiningPolicy policy = { from, to, type -> true }
        ProductRelationshipId relationshipId = ProductRelationshipId.random()
        ProductRelationshipFactory factory = new ProductRelationshipFactory(policy, { relationshipId })
        ProductIdentifier from = ProductIdentifier.of("from")
        ProductIdentifier to = ProductIdentifier.of("to")
        ProductRelationshipType type = ProductRelationshipType.COMPATIBLE_WITH
        policy.canDefineFor(from, to, type) >> true

        when:
        Result<String, ProductRelationship> result = factory.defineFor(from, to, type)

        then:
        result.success()
        result.getSuccess() == new ProductRelationship(relationshipId, from, to, type)
    }

    def "reject relationship when policy rejects it"() {
        given:
        ProductRelationshipDefiningPolicy policy = { from, to, type -> false }
        Supplier<ProductRelationshipId> idSupplier = Mock()
        ProductRelationshipFactory factory = new ProductRelationshipFactory(policy, idSupplier)
        ProductIdentifier from = ProductIdentifier.of("from")
        ProductIdentifier to = ProductIdentifier.of("to")
        ProductRelationshipType type = ProductRelationshipType.INCOMPATIBLE_WITH

        when:
        Result<String, ProductRelationship> result = factory.defineFor(from, to, type)

        then:
        result.failure()
        result.getFailure() == "POLICIES_NOT_MET"
        0 * idSupplier.get()
    }

    def "use default policy when policy is null"() {
        given:
        ProductRelationshipId relationshipId = ProductRelationshipId.random()
        ProductRelationshipFactory factory = new ProductRelationshipFactory(null, { relationshipId })
        ProductIdentifier from = ProductIdentifier.of("from")
        ProductIdentifier to = ProductIdentifier.of("to")
        ProductRelationshipType type = ProductRelationshipType.UPGRADABLE_TO

        when:
        Result<String, ProductRelationship> result = factory.defineFor(from, to, type)

        then:
        result.success()
        result.getSuccess().id() == relationshipId
    }

    def "use default ID supplier when ID supplier is null"() {
        given:
        ProductRelationshipDefiningPolicy policy = { from, to, type -> true }
        ProductRelationshipFactory factory = new ProductRelationshipFactory(policy, null)
        ProductIdentifier from = ProductIdentifier.of("from")
        ProductIdentifier to = ProductIdentifier.of("to")
        ProductRelationshipType type = ProductRelationshipType.REPLACED_BY

        when:
        Result<String, ProductRelationship> result = factory.defineFor(from, to, type)

        then:
        result.success()
        result.getSuccess().id() != null
    }

    def "use supplied IDs"() {
        given:
        ProductRelationshipId firstId = ProductRelationshipId.random()
        ProductRelationshipId secondId = ProductRelationshipId.random()
        List<ProductRelationshipId> suppliedIds = [firstId, secondId]
        ProductRelationshipFactory factory = new ProductRelationshipFactory({ suppliedIds.remove(0) })
        ProductIdentifier from = ProductIdentifier.of("from")
        ProductIdentifier to = ProductIdentifier.of("to")
        ProductRelationshipType type = ProductRelationshipType.SUBSTITUTED_BY

        when:
        Result<String, ProductRelationship> firstResult = factory.defineFor(from, to, type)
        Result<String, ProductRelationship> secondResult = factory.defineFor(from, to, type)

        then:
        firstResult.getSuccess().id() == firstId
        secondResult.getSuccess().id() == secondId
    }
}
