package com.softwarearchetypes.product

import com.softwarearchetypes.quantity.Quantity
import com.softwarearchetypes.quantity.Unit
import spock.lang.Specification

class SelectedInstanceSpec extends Specification {

    private ProductType laptop
    private ProductType mouse
    private PackageType bundle
    private ProductInstance laptopInstance
    private ProductInstance mouseInstance
    private PackageInstance bundleInstance
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
                               ProductTrackingStrategy.BATCH_TRACKED)
                       .build()

        bundle = Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Workstation Bundle"),
                                ProductDescription.of("Complete setup")
                        ).asPackageType()
                        .withRequiredChoice("laptop", laptop.id())
                        .build()

        laptopInstance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("LAPTOP-001"))
                .asProductInstance(laptop)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        mouseInstance = new InstanceBuilder(InstanceId.newOne())
                .withBatch(BatchId.newOne())
                .asProductInstance(mouse)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        bundleInstance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("BUNDLE-001"))
                .asPackageInstance(bundle)
                .withSelection(java.util.List.of(
                        new SelectedInstance(laptopInstance, 1)
                ))
                .build()
    }

    def "should create selected instance with product instance"() {
        when:
        SelectedInstance selected = new SelectedInstance(laptopInstance, 1)
        then:
        selected != null
        selected.instance() == laptopInstance
        selected.quantity() == 1
    }

    def "should create selected instance with package instance"() {
        when:
        SelectedInstance selected = new SelectedInstance(bundleInstance, 1)
        then:
        selected != null
        selected.instance() == bundleInstance
        selected.quantity() == 1
    }

    def "should create selected instance with multiple quantity"() {
        when:
        SelectedInstance selected = new SelectedInstance(mouseInstance, 5)
        then:
        selected.instance() == mouseInstance
        selected.quantity() == 5
    }

    def "should reject null instance"() {
        when:
        new SelectedInstance(null, 1)

        then:
        thrown(IllegalArgumentException)
    }

    def "should reject zero quantity"() {
        when:
        new SelectedInstance(laptopInstance, 0)

        then:
        thrown(IllegalArgumentException)
    }

    def "should reject negative quantity"() {
        when:
        new SelectedInstance(laptopInstance, -1)

        then:
        thrown(IllegalArgumentException)
    }

    def "should provide access to product"() {
        given:
        SelectedInstance selected = new SelectedInstance(laptopInstance, 1)

        when:
        Product product = selected.product()

        then:
        product != null
        product == laptop
    }

    def "should provide access to product id"() {
        given:
        SelectedInstance selected = new SelectedInstance(laptopInstance, 1)

        when:
        ProductIdentifier productId = selected.productId()

        then:
        productId != null
        productId == laptop.id()
    }

    def "should provide access to instance id"() {
        given:
        SelectedInstance selected = new SelectedInstance(laptopInstance, 1)

        when:
        InstanceId instanceId = selected.instanceId()

        then:
        instanceId != null
        instanceId == laptopInstance.id()
    }

    def "should convert to selected product"() {
        given:
        SelectedInstance selected = new SelectedInstance(laptopInstance, 2)

        when:
        SelectedProduct selectedProduct = selected.toSelectedProduct()
        then:
        selectedProduct != null
        selectedProduct.productId() == laptop.id()
        selectedProduct.quantity() == 2
    }

    def "should convert to selected product for package instance"() {
        given:
        SelectedInstance selected = new SelectedInstance(bundleInstance, 3)

        when:
        SelectedProduct selectedProduct = selected.toSelectedProduct()
        then:
        selectedProduct != null
        selectedProduct.productId() == bundle.id()
        selectedProduct.quantity() == 3
    }

    def "should maintain quantity when converting to selected product"() {
        given:
        int quantity = 10
        SelectedInstance selected = new SelectedInstance(mouseInstance, quantity)

        when:
        SelectedProduct selectedProduct = selected.toSelectedProduct()
        then:
        selectedProduct.quantity() == quantity
    }

    def "should support equality based on instance and quantity"() {
        given:
        SelectedInstance reference = new SelectedInstance(laptopInstance, 1)
        SelectedInstance equalSelection = new SelectedInstance(laptopInstance, 1)
        SelectedInstance differentQuantity = new SelectedInstance(laptopInstance, 2)

        when:
        SelectedInstance differentInstance = new SelectedInstance(mouseInstance, 1)

        then:
        equalSelection == reference
        equalSelection.hashCode() == reference.hashCode()
        differentQuantity != reference
        differentInstance != reference
    }

    def "should work as record with to string"() {
        given:
        SelectedInstance selected = new SelectedInstance(laptopInstance, 2)

        when:
        String toString = selected.toString()

        then:
        toString.contains(fragment)

        where:
        fragment << ["SelectedInstance", "2"]
    }

    def "should allow access to underlying instance properties"() {
        given:
        SelectedInstance selected = new SelectedInstance(laptopInstance, 1)

        when:
        Instance instance = selected.instance()
        Product product = selected.product()

        then:
        instance.serialNumber().isPresent()
        instance.serialNumber().get().value() == "LAPTOP-001"
        product.name() == ProductName.of("Business Laptop")
    }

    def "should work with product instance having batch"() {
        given:
        SelectedInstance selected = new SelectedInstance(mouseInstance, 3)

        when:
        Instance instance = selected.instance()

        then:
        instance.batchId().isPresent()
        instance.batchId().get() == mouseInstance.batchId().get()
    }

    def "should distinguish between different instances of same product"() {
        given:
        ProductInstance laptop1 = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("LAPTOP-001"))
                .asProductInstance(laptop)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        ProductInstance laptop2 = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("LAPTOP-002"))
                .asProductInstance(laptop)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        SelectedInstance selectedLaptop = new SelectedInstance(laptop1, 1)

        when:
        SelectedInstance selectedOtherLaptop = new SelectedInstance(laptop2, 1)

        then:
        selectedOtherLaptop.productId() == selectedLaptop.productId()
        selectedOtherLaptop.instanceId() != selectedLaptop.instanceId()
        selectedOtherLaptop != selectedLaptop
    }

    def "should support nested access"() {
        given:
        SelectedInstance selectedBundle = new SelectedInstance(bundleInstance, 1)

        when:
        PackageInstance pkg = (PackageInstance) selectedBundle.instance()
        SelectedInstance nestedLaptop = pkg.selection().get(0)

        then:
        pkg.selection().size() == 1
        nestedLaptop.instanceId() == laptopInstance.id()
    }
}
