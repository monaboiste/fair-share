package com.softwarearchetypes.product

import com.softwarearchetypes.quantity.Quantity
import com.softwarearchetypes.quantity.Unit
import spock.lang.Specification

class PackageInstanceSpec extends Specification {

    private ProductType laptop
    private ProductType mouse
    private ProductType keyboard
    private ProductType simCard
    private PackageType laptopBundle
    private PackageType telecomPackage

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
                        ProductTrackingStrategy.BATCH_TRACKED
                ).build()

        keyboard = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Mechanical Keyboard"),
                ProductDescription.of("RGB keyboard"))
                .asProductType(
                        Unit.pieces(),
                        ProductTrackingStrategy.BATCH_TRACKED
                ).build()

        simCard = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("5G SIM Card"),
                ProductDescription.of("Prepaid SIM"))
                .asProductType(
                        Unit.pieces(),
                        ProductTrackingStrategy.INDIVIDUALLY_TRACKED
                ).build()

        laptopBundle = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Laptop Bundle"),
                ProductDescription.of("Complete workstation")
        )
                .asPackageType()
                .withRequiredChoice("laptop", laptop.id())
                .withRequiredChoice("mouse", mouse.id())
                .build()

        telecomPackage = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("5G Starter Pack"),
                ProductDescription.of("SIM with accessories")
        ).asPackageType()
                .withTrackingStrategy(ProductTrackingStrategy.BATCH_TRACKED)
                .withRequiredChoice("sim", simCard.id())
                .withChoice("accessories", 1, 2, mouse.id(), keyboard.id())
                .build()
    }

    def "should create package instance with valid selection"() {
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

        when:
        PackageInstance packageInstance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("BUNDLE-001"))
                .asPackageInstance(laptopBundle)
                .withSelection(List.of(
                        new SelectedInstance(laptopInstance, 1),
                        new SelectedInstance(mouseInstance, 1)
                ))
                .build()

        then:
        packageInstance != null
        packageInstance.packageType() == laptopBundle
        packageInstance.selection().size() == 2
        packageInstance.serialNumber().isPresent()
        packageInstance.serialNumber().get().value() == "BUNDLE-001"
    }

    def "should reject package instance with invalid selection"() {
        given:
        ProductInstance laptopInstance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("LAPTOP-001"))
                .asProductInstance(laptop)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        when:
        new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("BUNDLE-001"))
                .asPackageInstance(laptopBundle)
                .withSelection(List.of(
                        new SelectedInstance(laptopInstance, 1)
                ))
                .build()

        then:
        thrown(IllegalArgumentException)
    }

    def "should reject package instance with empty selection"() {
        when:
        new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("BUNDLE-001"))
                .asPackageInstance(laptopBundle)
                .withSelection(List.of())
                .build()

        then:
        thrown(IllegalArgumentException)
    }

    def "should enforce tracking strategy for individually tracked package"() {
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

        when:
        new InstanceBuilder(InstanceId.newOne())
                .withBatch(BatchId.newOne())
                .asPackageInstance(laptopBundle)
                .withSelection(List.of(
                        new SelectedInstance(laptopInstance, 1),
                        new SelectedInstance(mouseInstance, 1)
                ))
                .build()

        then:
        thrown(IllegalArgumentException)
    }

    def "should enforce tracking strategy for batch tracked package"() {
        given:
        ProductInstance simInstance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("SIM-IMSI-123456"))
                .asProductInstance(simCard)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        ProductInstance mouseInstance = new InstanceBuilder(InstanceId.newOne())
                .withBatch(BatchId.newOne())
                .asProductInstance(mouse)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        when:
        new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("PACK-001"))
                .asPackageInstance(telecomPackage)
                .withSelection(List.of(
                        new SelectedInstance(simInstance, 1),
                        new SelectedInstance(mouseInstance, 1)
                ))
                .build()

        then:
        thrown(IllegalArgumentException)
    }

    def "should allow both serial and batch for package instance"() {
        given:
        ProductInstance simInstance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("SIM-IMSI-123456"))
                .asProductInstance(simCard)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        ProductInstance mouseInstance = new InstanceBuilder(InstanceId.newOne())
                .withBatch(BatchId.newOne())
                .asProductInstance(mouse)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        when:
        PackageInstance packageInstance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("PACK-001"))
                .withBatch(BatchId.newOne())
                .asPackageInstance(telecomPackage)
                .withSelection(List.of(
                        new SelectedInstance(simInstance, 1),
                        new SelectedInstance(mouseInstance, 1)
                ))
                .build()

        then:
        packageInstance != null
        packageInstance.serialNumber().isPresent()
        packageInstance.batchId().isPresent()
    }

    def "should reject package instance without any tracking"() {
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

        when:
        new InstanceBuilder(InstanceId.newOne())
                .asPackageInstance(laptopBundle)
                .withSelection(List.of(
                        new SelectedInstance(laptopInstance, 1),
                        new SelectedInstance(mouseInstance, 1)
                ))
                .build()

        then:
        thrown(IllegalArgumentException)
    }

    def "should support multiple quantities of same instance"() {
        given:
        ProductInstance mouseInstance = new InstanceBuilder(InstanceId.newOne())
                .withBatch(BatchId.newOne())
                .asProductInstance(mouse)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        ProductInstance laptopInstance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("LAPTOP-001"))
                .asProductInstance(laptop)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        when:
        PackageInstance packageInstance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("BUNDLE-001"))
                .asPackageInstance(laptopBundle)
                .withSelection(List.of(
                        new SelectedInstance(laptopInstance, 1),
                        new SelectedInstance(mouseInstance, 2)
                ))
                .build()
        SelectedInstance mouseSelection = packageInstance.selection().stream()
                .filter(s -> s.product().id() == mouse.id())
                .findFirst()
                .orElseThrow()

        then:
        packageInstance != null
        packageInstance.selection().size() == 2
        mouseSelection.quantity() == 2
    }

    def "should create nested package instance"() {
        given:
        PackageType innerBundle = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Peripherals Bundle"),
                ProductDescription.of("Mouse and keyboard")
        ).asPackageType()
                .withTrackingStrategy(ProductTrackingStrategy.BATCH_TRACKED)
                .withRequiredChoice("mouse", mouse.id())
                .withRequiredChoice("keyboard", keyboard.id())
                .build()

        ProductInstance mouseInstance = new InstanceBuilder(InstanceId.newOne())
                .withBatch(BatchId.newOne())
                .asProductInstance(mouse)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        ProductInstance keyboardInstance = new InstanceBuilder(InstanceId.newOne())
                .withBatch(BatchId.newOne())
                .asProductInstance(keyboard)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        PackageInstance innerPackageInstance = new InstanceBuilder(InstanceId.newOne())
                .withBatch(BatchId.newOne())
                .asPackageInstance(innerBundle)
                .withSelection(List.of(
                        new SelectedInstance(mouseInstance, 1),
                        new SelectedInstance(keyboardInstance, 1)
                ))
                .build()

        PackageType outerBundle = Product.builder(
                UuidProductIdentifier.random(),
                ProductName.of("Complete Workstation"),
                ProductDescription.of("Laptop with peripherals bundle")
        )
                .asPackageType()
                .withRequiredChoice("laptop", laptop.id())
                .withRequiredChoice("innerBundle", innerBundle.id())
                .build()

        ProductInstance laptopInstance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("LAPTOP-001"))
                .asProductInstance(laptop)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        when:
        PackageInstance outerPackageInstance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("WORKSTATION-001"))
                .asPackageInstance(outerBundle)
                .withSelection(List.of(
                        new SelectedInstance(laptopInstance, 1),
                        new SelectedInstance(innerPackageInstance, 1)
                ))
                .build()
        SelectedInstance nestedPackageSelection = outerPackageInstance.selection().stream()
                .filter(s -> s.instance() instanceof PackageInstance)
                .findFirst()
                .orElseThrow()
        PackageInstance nestedPackage = (PackageInstance) nestedPackageSelection.instance()

        then:
        outerPackageInstance != null
        outerPackageInstance.selection().size() == 2
        nestedPackage.packageType() == innerBundle
        nestedPackage.selection().size() == 2
    }

    def "should provide access to package instance properties"() {
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
                .asPackageInstance(laptopBundle)
                .withSelection(List.of(
                        new SelectedInstance(laptopInstance, 1),
                        new SelectedInstance(mouseInstance, 1)
                ))
                .build()

        then:
        packageInstance.id() == packageId
        packageInstance.product() == laptopBundle
        packageInstance.packageType() == laptopBundle
        packageInstance.serialNumber().isPresent()
        packageInstance.serialNumber().get() == packageSerial
        packageInstance.selection().size() == 2
    }

    def "should generate readable to string"() {
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

        PackageInstance packageInstance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("BUNDLE-001"))
                .asPackageInstance(laptopBundle)
                .withSelection(List.of(
                        new SelectedInstance(laptopInstance, 1),
                        new SelectedInstance(mouseInstance, 1)
                ))
                .build()

        when:
        String toString = packageInstance.toString()

        then:
        toString.contains(fragment)

        where:
        fragment << ["PackageInstance", "Laptop Bundle", "BUNDLE-001", "2 products"]
    }
}
