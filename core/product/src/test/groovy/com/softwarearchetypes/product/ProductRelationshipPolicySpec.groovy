package com.softwarearchetypes.product

import com.softwarearchetypes.quantity.Unit
import spock.lang.Specification

class ProductRelationshipPolicySpec extends Specification {

    private final InMemoryProductTypeRepository productTypeRepository = new InMemoryProductTypeRepository()

    def "should prevent self relationship"() {
        given:
        NoSelfRelationshipPolicy policy = new NoSelfRelationshipPolicy()
        ProductIdentifier productId = UuidProductIdentifier.random()

        when:
        boolean canDefine = policy.canDefineFor(productId, productId, ProductRelationshipType.COMPATIBLE_WITH)

        then:
        !canDefine
    }

    def "should allow relationship between different products"() {
        given:
        NoSelfRelationshipPolicy policy = new NoSelfRelationshipPolicy()
        ProductIdentifier product1 = UuidProductIdentifier.random()
        ProductIdentifier product2 = UuidProductIdentifier.random()

        when:
        boolean canDefine = policy.canDefineFor(product1, product2, ProductRelationshipType.COMPATIBLE_WITH)

        then:
        canDefine
    }

    def "should prevent compatibility between seasonal and non seasonal"() {
        given:
        NoSeasonalCompatibilityPolicy policy = new NoSeasonalCompatibilityPolicy(productTypeRepository)
        ProductType pumpkinSpiceLatte = createSeasonalProduct()
        ProductType regularLatte = createNonSeasonalProduct()

        when:
        boolean canDefine = policy.canDefineFor(
                pumpkinSpiceLatte.identifier(),
                regularLatte.identifier(),
                ProductRelationshipType.COMPATIBLE_WITH
        )

        then:
        !canDefine
    }

    def "should allow compatibility between both seasonal"() {
        given:
        NoSeasonalCompatibilityPolicy policy = new NoSeasonalCompatibilityPolicy(productTypeRepository)
        ProductType pumpkinSpiceLatte = createSeasonalProduct()
        ProductType gingerbreadLatte = createSeasonalProduct()

        when:
        boolean canDefine = policy.canDefineFor(
                pumpkinSpiceLatte.identifier(),
                gingerbreadLatte.identifier(),
                ProductRelationshipType.COMPATIBLE_WITH
        )

        then:
        canDefine
    }

    def "should allow compatibility between both non seasonal"() {
        given:
        NoSeasonalCompatibilityPolicy policy = new NoSeasonalCompatibilityPolicy(productTypeRepository)
        ProductType regularLatte = createNonSeasonalProduct()
        ProductType cappuccino = createNonSeasonalProduct()

        when:
        boolean canDefine = policy.canDefineFor(
                regularLatte.identifier(),
                cappuccino.identifier(),
                ProductRelationshipType.COMPATIBLE_WITH
        )

        then:
        canDefine
    }

    def "should allow non compatibility relationships between seasonal and non seasonal"() {
        given:
        NoSeasonalCompatibilityPolicy policy = new NoSeasonalCompatibilityPolicy(productTypeRepository)
        ProductType pumpkinSpiceLatte = createSeasonalProduct()
        ProductType regularLatte = createNonSeasonalProduct()

        when:
        boolean canDefineUpgrade = policy.canDefineFor(
                pumpkinSpiceLatte.identifier(),
                regularLatte.identifier(),
                ProductRelationshipType.UPGRADABLE_TO
        )

        then:
        canDefineUpgrade
    }

    private ProductType createSeasonalProduct() {
        ProductType product = ProductType.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Seasonal Product"),
                ProductDescription.of("Seasonal"),
                Unit.pieces(),
                ProductTrackingStrategy.IDENTICAL
        )
                .withMetadata("seasonal", "true")
                .build()
        productTypeRepository.save(product)
        return product
    }

    private ProductType createNonSeasonalProduct() {
        ProductType product = ProductType.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Non-Seasonal Product"),
                ProductDescription.of("Regular"),
                Unit.pieces(),
                ProductTrackingStrategy.IDENTICAL
        )
                .withMetadata("seasonal", "false")
                .build()
        productTypeRepository.save(product)
        return product
    }
}

class NoSelfRelationshipPolicy implements ProductRelationshipDefiningPolicy {
    @Override
    boolean canDefineFor(ProductIdentifier from, ProductIdentifier to, ProductRelationshipType type) {
        return from != to
    }
}

class NoSeasonalCompatibilityPolicy implements ProductRelationshipDefiningPolicy {
    private final ProductTypeRepository productRepo

    NoSeasonalCompatibilityPolicy(ProductTypeRepository productRepo) {
        this.productRepo = productRepo
    }

    @Override
    boolean canDefineFor(ProductIdentifier from, ProductIdentifier to, ProductRelationshipType type) {
        if (type != ProductRelationshipType.COMPATIBLE_WITH) {
            return true
        }

        ProductType fromProduct = productRepo.findById(from).orElseThrow()
        ProductType toProduct = productRepo.findById(to).orElseThrow()

        boolean fromSeasonal = "true" == fromProduct.metadata().getOrDefault("seasonal", "false")
        boolean toSeasonal = "true" == toProduct.metadata().getOrDefault("seasonal", "false")

        return fromSeasonal == toSeasonal
    }
}
