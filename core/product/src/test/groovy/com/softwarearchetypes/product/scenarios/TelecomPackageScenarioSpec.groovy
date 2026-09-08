package com.softwarearchetypes.product.scenarios

import com.softwarearchetypes.product.PackageValidationResult
import com.softwarearchetypes.product.Product
import com.softwarearchetypes.product.ProductType
import com.softwarearchetypes.product.SelectedProduct
import com.softwarearchetypes.product.fixture.TelecomPackageFixture
import spock.lang.Specification

class TelecomPackageScenarioSpec extends Specification {

    private TelecomPackageFixture catalog

    def setup() {
        catalog = TelecomPackageFixture.create()
    }

    def "starter pack with #accessoryScenario is #outcome"() {
        given:
        List<ProductType> selectedTypes = [catalog.simCard()] + [
                catalog.phoneCase(),
                catalog.screenProtector(),
                catalog.charger()
        ].take(accessoryCount)
        List<SelectedProduct> selection = selectedProducts(*selectedTypes)

        when:
        PackageValidationResult result = catalog.starterPack().validateSelection(selection)

        then:
        result.isValid() == valid

        where:
        accessoryScenario   | outcome    | accessoryCount | valid
        "two accessories"   | "accepted" | 2              | true
        "no accessories"    | "accepted" | 0              | true
        "three accessories" | "rejected" | 3              | false
    }

    def "#phoneName phone with #planName plan is #outcome"() {
        given:
        ProductType phone = [
                "budget"   : catalog.budgetPhone(),
                "mid-range": catalog.midRangePhone(),
                "flagship" : catalog.flagshipPhone()
        ][phoneName]
        ProductType plan = [
                "basic"   : catalog.basicPlan(),
                "standard": catalog.standardPlan(),
                "premium" : catalog.premiumPlan()
        ][planName]
        List<SelectedProduct> selection = selectedProducts(plan, phone, catalog.starterPack())

        when:
        PackageValidationResult result = catalog.phoneBundle().validateSelection(selection)

        then:
        result.isValid() == valid

        where:
        phoneName   | planName   | outcome    | valid
        "budget"    | "basic"    | "accepted" | true
        "mid-range" | "standard" | "accepted" | true
        "flagship"  | "premium"  | "accepted" | true
        "mid-range" | "premium"  | "accepted" | true
        "budget"    | "premium"  | "accepted" | true
        "flagship"  | "basic"    | "rejected" | false
        "flagship"  | "standard" | "rejected" | false
    }

    def "phone bundle contains a nested starter pack"() {
        given:
        List<SelectedProduct> selection = selectedProducts(
                catalog.premiumPlan(),
                catalog.flagshipPhone(),
                catalog.starterPack())

        when:
        PackageValidationResult result = catalog.phoneBundle().validateSelection(selection)

        then:
        result.isValid()
    }

    private static List<SelectedProduct> selectedProducts(Product... products) {
        return products.collect { product -> new SelectedProduct(product.id(), 1) }
    }
}
