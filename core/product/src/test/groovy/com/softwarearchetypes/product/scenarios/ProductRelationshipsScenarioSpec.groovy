package com.softwarearchetypes.product.scenarios

import com.softwarearchetypes.common.Result
import com.softwarearchetypes.product.ProductIdentifier
import com.softwarearchetypes.product.ProductRelationshipCommands.DefineRelationship
import com.softwarearchetypes.product.ProductRelationshipCommands.RemoveRelationship
import com.softwarearchetypes.product.ProductRelationshipId
import com.softwarearchetypes.product.ProductRelationshipType
import com.softwarearchetypes.product.ProductRelationshipsFacade
import com.softwarearchetypes.product.ProductType
import com.softwarearchetypes.product.fixture.ProductRelationshipsFixture
import spock.lang.Specification

class ProductRelationshipsScenarioSpec extends Specification {

    private ProductRelationshipsFixture products
    private ProductRelationshipsFacade facade

    def setup() {
        products = ProductRelationshipsFixture.create()
        facade = products.facade()
    }

    def "subscription plans form an upgrade path"() {
        when:
        Result<String, ProductRelationshipId> basicToStandard = relate(
                products.basicPlan(), products.standardPlan(), "UPGRADABLE_TO")
        Result<String, ProductRelationshipId> standardToPremium = relate(
                products.standardPlan(), products.premiumPlan(), "UPGRADABLE_TO")

        then:
        basicToStandard.success()
        standardToPremium.success()
    }

    def "phone has multiple substitutes"() {
        when:
        Result<String, ProductRelationshipId> iphoneToSamsung = relate(
                products.iphone15(), products.samsungS24(), "SUBSTITUTED_BY")
        Result<String, ProductRelationshipId> iphoneToPixel = relate(
                products.iphone15(), products.pixelPhone(), "SUBSTITUTED_BY")

        then:
        iphoneToSamsung.success()
        iphoneToPixel.success()
    }

    def "new phone model replaces its predecessor"() {
        when:
        Result<String, ProductRelationshipId> replacement = relate(
                products.iphone15(), products.iphone15Pro(), "REPLACED_BY")

        then:
        replacement.success()
    }

    def "phones are complemented by cloud storage"() {
        when:
        Result<String, ProductRelationshipId> iphoneWithStorage = relate(
                products.iphone15(), products.cloudStorage100GB(), "COMPLEMENTED_BY")
        Result<String, ProductRelationshipId> iphoneProWithStorage = relate(
                products.iphone15Pro(), products.cloudStorage1TB(), "COMPLEMENTED_BY")

        then:
        iphoneWithStorage.success()
        iphoneProWithStorage.success()
    }

    def "USB-C charger is compatible with USB-C phones"() {
        when:
        Result<String, ProductRelationshipId> samsung = relate(
                products.usbCCharger(), products.samsungS24(), "COMPATIBLE_WITH")
        Result<String, ProductRelationshipId> pixel = relate(
                products.usbCCharger(), products.pixelPhone(), "COMPATIBLE_WITH")

        then:
        samsung.success()
        pixel.success()
    }

    def "Lightning charger is incompatible with USB-C phones"() {
        when:
        Result<String, ProductRelationshipId> samsung = relate(
                products.lightningCharger(), products.samsungS24(), "INCOMPATIBLE_WITH")
        Result<String, ProductRelationshipId> pixel = relate(
                products.lightningCharger(), products.pixelPhone(), "INCOMPATIBLE_WITH")

        then:
        samsung.success()
        pixel.success()
    }

    def "relationship can be removed"() {
        given:
        ProductRelationshipId relationshipId = relate(
                products.iphone15(), products.samsungS24(), "SUBSTITUTED_BY").getSuccess()

        when:
        Result<String, ProductRelationshipId> removal = facade.handle(new RemoveRelationship(relationshipId))

        then:
        removal.success()
    }

    def "relationship requires both products to exist"() {
        when:
        Result<String, ProductRelationshipId> result = facade.handle(new DefineRelationship(
                ProductIdentifier.uuid(),
                products.iphone15().id(),
                ProductRelationshipType.UPGRADABLE_TO))

        then:
        result.failure()
        result.getFailure().contains("PRODUCT_NOT_FOUND")
    }

    def "catalog supports a graph containing different relationship types"() {
        when:
        List<Result<String, ProductRelationshipId>> results = [
                relate(products.basicPlan(), products.standardPlan(), "UPGRADABLE_TO"),
                relate(products.standardPlan(), products.premiumPlan(), "UPGRADABLE_TO"),
                relate(products.iphone15(), products.iphone15Pro(), "UPGRADABLE_TO"),
                relate(products.iphone15(), products.samsungS24(), "SUBSTITUTED_BY"),
                relate(products.iphone15(), products.cloudStorage100GB(), "COMPLEMENTED_BY"),
                relate(products.usbCCharger(), products.samsungS24(), "COMPATIBLE_WITH"),
                relate(products.lightningCharger(), products.samsungS24(), "INCOMPATIBLE_WITH")
        ]

        then:
        results.every { it.success() }
    }

    def "compatibility can be defined in both directions"() {
        when:
        Result<String, ProductRelationshipId> forward = relate(
                products.wirelessCharger(), products.iphone15(), "COMPATIBLE_WITH")
        Result<String, ProductRelationshipId> reverse = relate(
                products.iphone15(), products.wirelessCharger(), "COMPATIBLE_WITH")

        then:
        forward.success()
        reverse.success()
    }

    private Result<String, ProductRelationshipId> relate(ProductType from, ProductType to, String type) {
        return facade.handle(new DefineRelationship(from.id(), to.id(), ProductRelationshipType.valueOf(type)))
    }
}
