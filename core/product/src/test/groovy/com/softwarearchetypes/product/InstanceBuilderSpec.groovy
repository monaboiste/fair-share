package com.softwarearchetypes.product

import com.softwarearchetypes.quantity.Quantity
import com.softwarearchetypes.quantity.Unit
import spock.lang.Specification

class InstanceBuilderSpec extends Specification {

    private ProductType laptop
    private ProductType mouse
    private PackageType bundle
    private ProductFeatureType colorFeature
    private ProductFeatureType storageFeature

    def setup() {
        colorFeature = ProductFeatureType.withAllowedValues("Color", "Silver", "Space Gray", "Black")
        storageFeature = ProductFeatureType.withAllowedValues("Storage", "512GB", "1024GB")

        laptop = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Business Laptop"),
                ProductDescription.of("Professional laptop"))
                .asProductType(
                        Unit.pieces(),
                        ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
                .withOptionalFeature(colorFeature)
                .withOptionalFeature(storageFeature)
                .build()

        mouse = ProductType.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Wireless Mouse"),
                ProductDescription.of("Ergonomic mouse"),
                Unit.pieces(),
                ProductTrackingStrategy.BATCH_TRACKED
        ).build()

        bundle = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Workstation Bundle"),
                ProductDescription.of("Complete setup"))
                .asPackageType()
                .withRequiredChoice("laptop", laptop.id())
                .withRequiredChoice("mouse", mouse.id())
                .build()

    }

    def "should build product instance with serial"() {
        given:
        InstanceId id = InstanceId.newOne()
        SerialNumber serial = SerialNumber.of("LAPTOP-123")

        when:
        ProductInstance instance = new InstanceBuilder(id)
                .withSerial(serial)
                .asProductInstance(laptop)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        then:
        instance != null
        instance.id() == id
        instance.product() == laptop
        instance.serialNumber().isPresent()
        instance.serialNumber().get() == serial
        !instance.batchId().isPresent()
    }

    def "should build product instance with batch"() {
        given:
        InstanceId id = InstanceId.newOne()
        BatchId batch = BatchId.newOne()

        when:
        ProductInstance instance = new InstanceBuilder(id)
                .withBatch(batch)
                .asProductInstance(mouse)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        then:
        instance != null
        instance.id() == id
        instance.product() == mouse
        !instance.serialNumber().isPresent()
        instance.batchId().isPresent()
        instance.batchId().get() == batch
    }

    def "should build product instance with both serial and batch"() {
        given:
        InstanceId id = InstanceId.newOne()
        SerialNumber serial = SerialNumber.of("LAPTOP-123")
        BatchId batch = BatchId.newOne()

        when:
        ProductInstance instance = new InstanceBuilder(id)
                .withSerial(serial)
                .withBatch(batch)
                .asProductInstance(laptop)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        then:
        instance != null
        instance.serialNumber().isPresent()
        instance.batchId().isPresent()
        instance.serialNumber().get() == serial
        instance.batchId().get() == batch
    }

    def "should build product instance with features"() {
        when:
        ProductInstance instance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("LAPTOP-123"))
                .asProductInstance(laptop)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .withFeature(colorFeature, "Silver")
                .build()

        then:
        instance != null
        instance.features().size() == 1
        instance.features().has(colorFeature)
        instance.features().get(colorFeature).orElseThrow().value() == "Silver"
    }

    def "should build product instance with multiple features"() {
        when:
        ProductInstance instance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("LAPTOP-123"))
                .asProductInstance(laptop)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .withFeature(colorFeature, "Silver")
                .withFeature(storageFeature, "512GB")
                .build()

        then:
        instance != null
        instance.features().size() == 2
        instance.features().has(colorFeature)
        instance.features().has(storageFeature)
    }

    def "should build product instance with feature instance"() {
        given:
        ProductFeatureInstance featureInstance = new ProductFeatureInstance(colorFeature, "Black")

        when:
        ProductInstance instance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("LAPTOP-123"))
                .asProductInstance(laptop)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .withFeature(featureInstance)
                .build()

        then:
        instance != null
        instance.features().size() == 1
        instance.features().get(colorFeature).orElseThrow().value() == "Black"
    }

    def "should build package instance with serial"() {
        given:
        ProductInstance laptopInstance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("LAPTOP-001"))
                .asProductInstance(laptop)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        ProductInstance mouseInstance = new InstanceBuilder(InstanceId.newOne())
                .withBatch(BatchId.newOne())
                .asProductInstance(mouse)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        InstanceId packageId = InstanceId.newOne()
        SerialNumber packageSerial = SerialNumber.of("BUNDLE-001")

        when:
        PackageInstance packageInstance = new InstanceBuilder(packageId)
                .withSerial(packageSerial)
                .asPackageInstance(bundle)
                .withSelection(List.of(
                        new SelectedInstance(laptopInstance, 1),
                        new SelectedInstance(mouseInstance, 1)
                ))
                .build()

        then:
        packageInstance != null
        packageInstance.id() == packageId
        packageInstance.product() == bundle
        packageInstance.serialNumber().isPresent()
        packageInstance.serialNumber().get() == packageSerial
        packageInstance.selection().size() == 2
    }

    def "should build package instance with batch"() {
        given:
        ProductInstance laptopInstance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("LAPTOP-001"))
                .asProductInstance(laptop)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        ProductInstance mouseInstance = new InstanceBuilder(InstanceId.newOne())
                .withBatch(BatchId.newOne())
                .asProductInstance(mouse)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        PackageType batchBundle = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Batch Bundle"),
                ProductDescription.of("Batch tracked")
        )
                .asPackageType()
                .withTrackingStrategy(ProductTrackingStrategy.BATCH_TRACKED)
                .withRequiredChoice("laptop", laptop.id())
                .withRequiredChoice("mouse", mouse.id())
                .build()

        BatchId packageBatch = BatchId.newOne()

        when:
        PackageInstance packageInstance = new InstanceBuilder(InstanceId.newOne())
                .withBatch(packageBatch)
                .asPackageInstance(batchBundle)
                .withSelection(List.of(
                        new SelectedInstance(laptopInstance, 1),
                        new SelectedInstance(mouseInstance, 1)
                ))
                .build()

        then:
        packageInstance != null
        !packageInstance.serialNumber().isPresent()
        packageInstance.batchId().isPresent()
        packageInstance.batchId().get() == packageBatch
    }

    def "should support fluent building style"() {
        when:
        ProductInstance instance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("LAPTOP-001"))
                .withBatch(BatchId.newOne())
                .asProductInstance(laptop)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .withFeature(colorFeature, "Space Gray")
                .build()

        then:
        instance != null
        instance.serialNumber().isPresent()
        instance.batchId().isPresent()
        instance.features().size() == 1
    }

    def "should allow switching between product and package builders"() {
        given:
        InstanceId id1 = InstanceId.newOne()
        InstanceId id2 = InstanceId.newOne()

        InstanceBuilder builder1 = new InstanceBuilder(id1)
        ProductInstance productInstance = builder1
                .withSerial(SerialNumber.of("PROD-001"))
                .asProductInstance(laptop)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        InstanceBuilder builder2 = new InstanceBuilder(id2)
        when:
        PackageInstance packageInstance = builder2
                .withSerial(SerialNumber.of("PKG-001"))
                .asPackageInstance(bundle)
                .withSelection(List.of(
                        new SelectedInstance(productInstance, 1),
                        new SelectedInstance(
                                new InstanceBuilder(InstanceId.newOne())
                                        .withBatch(BatchId.newOne())
                                        .asProductInstance(mouse)
                                        .withQuantity(Quantity.of(1, Unit.pieces()))
                                        .build(),
                                1
                        )
                ))
                .build()

        then:
        productInstance != null
        packageInstance != null
        productInstance.id() == id1
        packageInstance.id() == id2
    }

    def "should preserve common fields when building product instance"() {
        given:
        SerialNumber serial = SerialNumber.of("COMMON-001")
        BatchId batch = BatchId.newOne()

        when:
        ProductInstance instance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(serial)
                .withBatch(batch)
                .asProductInstance(laptop)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        then:
        instance.serialNumber().get() == serial
        instance.batchId().get() == batch
    }

    def "should preserve common fields when building package instance"() {
        given:
        SerialNumber serial = SerialNumber.of("COMMON-001")
        BatchId batch = BatchId.newOne()

        ProductInstance laptopInstance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("LAPTOP-001"))
                .asProductInstance(laptop)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        ProductInstance mouseInstance = new InstanceBuilder(InstanceId.newOne())
                .withBatch(BatchId.newOne())
                .asProductInstance(mouse)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        when:
        PackageInstance instance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(serial)
                .withBatch(batch)
                .asPackageInstance(bundle)
                .withSelection(List.of(
                        new SelectedInstance(laptopInstance, 1),
                        new SelectedInstance(mouseInstance, 1)
                ))
                .build()

        then:
        instance.serialNumber().get() == serial
        instance.batchId().get() == batch
    }

    def "should demonstrate parallel structure with product builder"() {
        given:
        PackageType packageType = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Test Package"),
                ProductDescription.of("Test")
        )
                .asPackageType()
                .withRequiredChoice("laptop", laptop.id())
                .build()

        packageType != null

        when:
        PackageInstance packageInstance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("INSTANCE-001"))
                .asPackageInstance(packageType)
                .withSelection(List.of(
                        new SelectedInstance(
                                new InstanceBuilder(InstanceId.newOne())
                                        .withSerial(SerialNumber.of("LAPTOP-001"))
                                        .asProductInstance(laptop)
                                        .withQuantity(Quantity.of(1, Unit.pieces()))
                                        .build(),
                                1
                        )
                ))
                .build()

        then:
        packageInstance != null
        packageInstance.packageType() == packageType
    }

    def "should use one preferred unit when quantity is omitted"() {
        when:
        ProductInstance instance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("LAPTOP-001"))
                .asProductInstance(laptop)
                .build()

        then:
        instance.quantity().isEmpty()
        instance.effectiveQuantity() == Quantity.of(1, Unit.pieces())
    }

    def "should reject quantity using a different unit"() {
        when:
        new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("LAPTOP-001"))
                .asProductInstance(laptop)
                .withQuantity(Quantity.of(1, Unit.kilograms()))
                .build()

        then:
        thrown(IllegalArgumentException)
    }

    def "should create instance with generated id"() {
        given:
        InstanceId generatedId = InstanceId.newOne()

        when:
        ProductInstance instance = new InstanceBuilder(generatedId)
                .withSerial(SerialNumber.of("AUTO-001"))
                .asProductInstance(laptop)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        then:
        instance.id() == generatedId
    }

    def "should create instance with explicit id"() {
        given:
        String explicitIdValue = "123e4567-e89b-12d3-a456-426614174000"
        InstanceId explicitId = InstanceId.of(explicitIdValue)

        when:
        ProductInstance instance = new InstanceBuilder(explicitId)
                .withSerial(SerialNumber.of("EXPLICIT-001"))
                .asProductInstance(laptop)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        then:
        instance.id() == explicitId
        instance.id().value().toString() == explicitIdValue
    }
}
