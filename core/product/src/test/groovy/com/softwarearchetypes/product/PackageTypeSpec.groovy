package com.softwarearchetypes.product

import com.softwarearchetypes.quantity.Unit
import spock.lang.Specification

class PackageTypeSpec extends Specification {

    private ProductType laptop
    private ProductType mouse
    private ProductType keyboard
    private ProductType monitor
    private ProductType warranty
    private ProductType insurance

    def setup() {
        laptop = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Business Laptop"),
                ProductDescription.of("Professional laptop"))
                .asProductType(
                        Unit.pieces(),
                        ProductTrackingStrategy.INDIVIDUALLY_TRACKED
                ).build()

        mouse = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Wireless Mouse"),
                ProductDescription.of("Ergonomic mouse"))
                .asProductType(
                        Unit.pieces(),
                        ProductTrackingStrategy.IDENTICAL
                ).build()

        keyboard = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Mechanical Keyboard"),
                ProductDescription.of("RGB keyboard"))
                .asProductType(
                        Unit.pieces(),
                        ProductTrackingStrategy.IDENTICAL
                ).build()

        monitor = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("4K Monitor"),
                ProductDescription.of("27-inch display"))
                .asProductType(
                        Unit.pieces(),
                        ProductTrackingStrategy.INDIVIDUALLY_TRACKED
                ).build()

        warranty = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Extended Warranty"),
                ProductDescription.of("3-year warranty"))
                .asProductType(
                        Unit.pieces(),
                        ProductTrackingStrategy.IDENTICAL
                ).build()

        insurance = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Device Insurance"),
                ProductDescription.of("Accidental damage coverage"))
                .asProductType(
                        Unit.pieces(),
                        ProductTrackingStrategy.IDENTICAL
                ).build()
    }

    def "should create simple package with required product"() {
        when:
        PackageType bundle = Product.builder(UuidProductIdentifier.random(),
                ProductName.of("Laptop Bundle"),
                ProductDescription.of("Basic laptop package"))
                .asPackageType()
                .withRequiredChoice("laptop", laptop.id())
                .build()

        then:
        bundle != null
        bundle.name() == ProductName.of("Laptop Bundle")
        bundle.structure().selectionRules().size() == 1
    }

    def "required rule returns #expectedValidity for #selectionName"() {
        given:
        PackageType bundle = Product.builder(UuidProductIdentifier.random(),
                ProductName.of("Laptop Bundle"),
                ProductDescription.of("Basic laptop package"))
                .asPackageType()
                .withRequiredChoice("laptop", laptop.id())
                .build()

        expect:
        PackageValidationResult result = bundle.validateSelection(selection(this))
        result.isValid() == expectedValidity
        result.errors().isEmpty() == expectedValidity

        where:
        selectionName     | selection                                                     || expectedValidity
        "selected laptop" | { spec -> List.of(new SelectedProduct(spec.laptop.id(), 1)) } || true
        "empty selection" | { spec -> List.of() }                                         || false
    }

    def "optional rule returns valid for #selectionName"() {
        given:
        PackageType bundle = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Laptop with Optional Warranty"),
                ProductDescription.of("Laptop package"))
                .asPackageType()
                .withOptionalChoice("warranty", warranty.id())
                .build()

        expect:
        bundle.validateSelection(selection(this)).isValid()

        where:
        selectionName       | selection
        "empty selection"   | { spec -> List.of() }
        "selected warranty" | { spec -> List.of(new SelectedProduct(spec.warranty.id(), 1)) }
    }

    def "and rule returns #expectedValidity for #selectionName"() {
        given:
        PackageType bundle = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Laptop + Mouse Bundle"),
                ProductDescription.of("Complete workstation")
        )
                .asPackageType()
                .withRequiredChoice("laptop", laptop.id())
                .withRequiredChoice("mouse", mouse.id())
                .build()

        expect:
        bundle.validateSelection(selection(this)).isValid() == expectedValidity

        where:
        selectionName          | selection                                                                                              || expectedValidity
        "laptop and mouse"     | { spec -> List.of(new SelectedProduct(spec.laptop.id(), 1), new SelectedProduct(spec.mouse.id(), 1)) } || true
        "laptop without mouse" | { spec -> List.of(new SelectedProduct(spec.laptop.id(), 1)) }                                          || false
        "mouse without laptop" | { spec -> List.of(new SelectedProduct(spec.mouse.id(), 1)) }                                           || false
    }

    def "or rule returns #expectedValidity for #selectionName"() {
        given:
        ProductSet mouseSet = ProductSet.of("mouse", mouse.id())
        ProductSet keyboardSet = ProductSet.of("keyboard", keyboard.id())
        SelectionRule rule = SelectionRule.or(
                SelectionRule.required(mouseSet),
                SelectionRule.required(keyboardSet)
        )
        PackageType bundle = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Input Device Bundle"),
                ProductDescription.of("Choose mouse or keyboard")
        )
                .asPackageType()
                .withProductSets(mouseSet, keyboardSet)
                .withRule(rule)
                .build()

        expect:
        bundle.validateSelection(selection(this)).isValid() == expectedValidity

        where:
        selectionName        | selection                                                                                                || expectedValidity
        "selected mouse"     | { spec -> List.of(new SelectedProduct(spec.mouse.id(), 1)) }                                             || true
        "selected keyboard"  | { spec -> List.of(new SelectedProduct(spec.keyboard.id(), 1)) }                                          || true
        "both input devices" | { spec -> List.of(new SelectedProduct(spec.mouse.id(), 1), new SelectedProduct(spec.keyboard.id(), 1)) } || true
        "empty selection"    | { spec -> List.of() }                                                                                    || false
    }

    def "not rule returns #expectedValidity for #selectionName"() {
        given:
        ProductSet insuranceSet = ProductSet.of("insurance", insurance.id())
        SelectionRule rule = SelectionRule.not(
                SelectionRule.required(insuranceSet)
        )
        PackageType bundle = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("No Insurance Bundle"),
                ProductDescription.of("Insurance not allowed")
        )
                .asPackageType()
                .withProductSet(insuranceSet)
                .withRule(rule)
                .build()

        expect:
        bundle.validateSelection(selection(this)).isValid() == expectedValidity

        where:
        selectionName        | selection                                                        || expectedValidity
        "empty selection"    | { spec -> List.of() }                                            || true
        "selected insurance" | { spec -> List.of(new SelectedProduct(spec.insurance.id(), 1)) } || false
    }

    def "conditional rule returns #expectedValidity for #selectionName"() {
        given:
        ProductSet laptopSet = ProductSet.of("laptop", laptop.id())
        ProductSet warrantySet = ProductSet.of("warranty", warranty.id())
        SelectionRule rule = SelectionRule.ifThen(
                SelectionRule.required(laptopSet),
                SelectionRule.required(warrantySet)
        )
        PackageType bundle = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Laptop with Mandatory Warranty"),
                ProductDescription.of("Warranty required for laptop")
        )
                .asPackageType()
                .withProductSets(laptopSet, warrantySet)
                .withRule(rule)
                .build()

        expect:
        bundle.validateSelection(selection(this)).isValid() == expectedValidity

        where:
        selectionName             | selection                                                                                                 || expectedValidity
        "empty selection"         | { spec -> List.of() }                                                                                     || true
        "laptop with warranty"    | { spec -> List.of(new SelectedProduct(spec.laptop.id(), 1), new SelectedProduct(spec.warranty.id(), 1)) } || true
        "laptop without warranty" | { spec -> List.of(new SelectedProduct(spec.laptop.id(), 1)) }                                             || false
    }

    def "multiple rules return #expectedValidity for #selectionName"() {
        given:
        ProductSet laptopSet = ProductSet.of("laptop", laptop.id())
        ProductSet accessoriesSet = ProductSet.of("accessoriesSet", mouse.id(), keyboard.id())
        ProductSet warrantySet = ProductSet.of("warranty", warranty.id())
        SelectionRule rule = SelectionRule.and(
                SelectionRule.required(laptopSet),
                SelectionRule.isSubsetOf(accessoriesSet, 1, 2),
                SelectionRule.optional(warrantySet)
        )
        PackageType bundle = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Complete Workstation"),
                ProductDescription.of("Laptop with accessories")
        )
                .asPackageType()
                .withProductSets(laptopSet, accessoriesSet, warrantySet)
                .withRule(rule)
                .build()

        expect:
        bundle.validateSelection(selection(this)).isValid() == expectedValidity

        where:
        selectionName                   | selection                                                                                                                                                                                      || expectedValidity
        "laptop and one accessory"      | { spec -> List.of(new SelectedProduct(spec.laptop.id(), 1), new SelectedProduct(spec.mouse.id(), 1)) }                                                                                         || true
        "laptop, accessories, warranty" | { spec -> List.of(new SelectedProduct(spec.laptop.id(), 1), new SelectedProduct(spec.mouse.id(), 1), new SelectedProduct(spec.keyboard.id(), 1), new SelectedProduct(spec.warranty.id(), 1)) } || true
        "laptop without accessories"    | { spec -> List.of(new SelectedProduct(spec.laptop.id(), 1)) }                                                                                                                                  || false
        "accessory without laptop"      | { spec -> List.of(new SelectedProduct(spec.mouse.id(), 1)) }                                                                                                                                   || false
    }

    def "subset quantity rule returns #expectedValidity for #selectionName"() {
        given:
        ProductSet accessoriesSet = ProductSet.of("accessories", mouse.id(), keyboard.id(), monitor.id())
        SelectionRule rule = SelectionRule.isSubsetOf(accessoriesSet, 2, 3)
        PackageType bundle = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Accessories Bundle"),
                ProductDescription.of("Choose 2-3 accessories")
        )
                .asPackageType()
                .withProductSet(accessoriesSet)
                .withRule(rule)
                .build()

        expect:
        bundle.validateSelection(selection(this)).isValid() == expectedValidity

        where:
        selectionName         | selection                                                                                                                                           || expectedValidity
        "one accessory"       | { spec -> List.of(new SelectedProduct(spec.mouse.id(), 1)) }                                                                                        || false
        "two accessories"     | { spec -> List.of(new SelectedProduct(spec.mouse.id(), 1), new SelectedProduct(spec.keyboard.id(), 1)) }                                            || true
        "three accessories"   | { spec -> List.of(new SelectedProduct(spec.mouse.id(), 1), new SelectedProduct(spec.keyboard.id(), 1), new SelectedProduct(spec.monitor.id(), 1)) } || true
        "four total quantity" | { spec -> List.of(new SelectedProduct(spec.mouse.id(), 2), new SelectedProduct(spec.keyboard.id(), 1), new SelectedProduct(spec.monitor.id(), 1)) } || false
    }

    def "should create nested package"() {
        given:
        PackageType innerBundle = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Basic Bundle"),
                ProductDescription.of("Laptop and mouse")
        )
                .asPackageType()
                .withRequiredChoice("laptop", laptop.id())
                .withRequiredChoice("mouse", mouse.id())
                .build()

        when:
        PackageType outerBundle = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Premium Bundle"),
                ProductDescription.of("Basic bundle with optional monitor")
        )
                .asPackageType()
                .withRequiredChoice("inner", innerBundle.id())
                .withOptionalChoice("monitor", monitor.id())
                .build()

        then:
        outerBundle != null
        outerBundle.name() == ProductName.of("Premium Bundle")
        outerBundle.structure().selectionRules().size() == 2
    }

    def "should reject invalid package type creation"() {
        when:
        Product.builder(
                null,
                ProductName.of("Invalid Package"),
                ProductDescription.of("Missing ID")
        )
                .asPackageType()
                .build()

        then:
        thrown(IllegalArgumentException)

        when:
        Product.builder(
                UuidProductIdentifier.random(),
                null,
                ProductDescription.of("Missing name")
        )
                .asPackageType()
                .build()

        then:
        thrown(IllegalArgumentException)
    }

    def "should reject a package with missing #field"() {
        when:
        packageWithMissing(field)

        then:
        thrown(IllegalArgumentException)

        where:
        field << [
                "identifier",
                "name",
                "description",
                "tracking strategy",
                "metadata",
                "applicability constraint",
                "structure"
        ]
    }

    def "should define an individually tracked package with defaults"() {
        given:
        ProductSet set = ProductSet.singleOf("laptop", laptop.id())
        PackageStructure structure = new PackageStructure(Map.of(set.name(), set), List.of(SelectionRule.single(set)))

        when:
        PackageType packageType = PackageType.define(
                UuidProductIdentifier.random(),
                ProductName.of("Package"),
                ProductDescription.of("Description"),
                structure)

        then:
        packageType.trackingStrategy() == ProductTrackingStrategy.INDIVIDUALLY_TRACKED
        packageType.metadata().asMap().isEmpty()
        packageType.toString().contains("PackageType")
    }

    def "should provide access to package structure"() {
        given:
        ProductSet laptopSet = ProductSet.of("laptop", laptop.id())
        SelectionRule rule1 = SelectionRule.required(laptopSet)
        SelectionRule rule2 = SelectionRule.optional(ProductSet.of("warranty", warranty.id()))

        when:
        PackageType bundle = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Laptop Bundle"),
                ProductDescription.of("Laptop with optional warranty")
        )
                .asPackageType()
                .withProductSet(laptopSet)
                .withRule(rule1)
                .withRule(rule2)
                .build()

        then:
        PackageStructure structure = bundle.structure()
        structure != null
        structure.selectionRules().size() == 2
    }

    private PackageType packageWithMissing(String field) {
        ProductSet set = ProductSet.singleOf("laptop", laptop.id())
        PackageStructure structure = new PackageStructure(Map.of(set.name(), set), List.of(SelectionRule.single(set)))
        return new PackageType(
                field == "identifier" ? null : UuidProductIdentifier.random(),
                field == "name" ? null : ProductName.of("Package"),
                field == "description" ? null : ProductDescription.of("Description"),
                field == "tracking strategy" ? null : ProductTrackingStrategy.IDENTICAL,
                field == "metadata" ? null : ProductMetadata.empty(),
                field == "applicability constraint" ? null : ApplicabilityConstraint.alwaysTrue(),
                field == "structure" ? null : structure)
    }
}
