package com.softwarearchetypes.product

import com.softwarearchetypes.quantity.Quantity
import com.softwarearchetypes.quantity.Unit
import spock.lang.Specification
class ProductTrackingStrategySpec extends Specification {
    def "should track unique product"() {
        given:
        ProductType hetfieldsGuitar = Product.builder(
                        UuidProductIdentifier.random(),
                        ProductName.of("Hetfield's EET FUK Guitar"),
                        ProductDescription.of("1968 Gibson Explorer - one of a kind")
                )
                .asProductType(Unit.pieces(), ProductTrackingStrategy.UNIQUE)
                .build()

        when:
        ProductInstance guitarInstance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("HETFIELD-EET-FUK-1968"))
                .asProductInstance(hetfieldsGuitar)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        then:
        guitarInstance != null
        guitarInstance.serialNumber().isPresent()
        guitarInstance.serialNumber().get().toString() == "HETFIELD-EET-FUK-1968"

        hetfieldsGuitar.trackingStrategy().isTrackedIndividually()
        !hetfieldsGuitar.trackingStrategy().isTrackedByBatch()
    }
    def "should track individually tracked product"() {
        given:
        ProductType iphone = Product.builder(
                        UuidProductIdentifier.random(),
                        ProductName.of("iPhone 15 Pro"),
                        ProductDescription.of("256GB Space Gray")
                )
                .asProductType(Unit.pieces(), ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
                .build()

        when:
        ProductInstance iphone1 = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("IPHONE-123456789"))
                .asProductInstance(iphone)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        ProductInstance iphone2 = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("IPHONE-987654321"))
                .asProductInstance(iphone)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        then:
        iphone2.serialNumber().get() != iphone1.serialNumber().get()
        iphone1.product() == iphone
        iphone2.product() == iphone

        iphone.trackingStrategy().isTrackedIndividually()
        !iphone.trackingStrategy().isTrackedByBatch()
    }
    def "should track batch tracked product"() {
        given:
        ProductType milk = Product.builder(
                        UuidProductIdentifier.random(),
                        ProductName.of("Fresh Milk"),
                        ProductDescription.of("1L whole milk")
                )
                .asProductType(Unit.liters(), ProductTrackingStrategy.BATCH_TRACKED)
                .build()

        when:
        ProductInstance milkBatch1 = new InstanceBuilder(InstanceId.newOne())
                .withBatch(BatchId.newOne())
                .asProductInstance(milk)
                .withQuantity(Quantity.of(100, Unit.liters()))
                .build()

        then:
        milkBatch1 != null
        milkBatch1.batchId().isPresent()
        !milkBatch1.serialNumber().isPresent()

        !milk.trackingStrategy().isTrackedIndividually()
        milk.trackingStrategy().isTrackedByBatch()
    }
    def "should track product with both methods"() {
        given:
        ProductType tv = Product.builder(
                        UuidProductIdentifier.random(),
                        ProductName.of("Samsung QLED 65\""),
                        ProductDescription.of("4K Smart TV")
                )
                .asProductType(Unit.pieces(), ProductTrackingStrategy.INDIVIDUALLY_AND_BATCH_TRACKED)
                .build()

        when:
        ProductInstance tvInstance = new InstanceBuilder(InstanceId.newOne())
                .withSerial(SerialNumber.of("TV-SERIAL-123"))
                .withBatch(BatchId.newOne())
                .asProductInstance(tv)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        then:
        tvInstance.serialNumber().isPresent()
        tvInstance.batchId().isPresent()

        tv.trackingStrategy().isTrackedIndividually()
        tv.trackingStrategy().isTrackedByBatch()
        tv.trackingStrategy().requiresBothTrackingMethods()
    }
    def "should track identical product"() {
        given:
        ProductType screws = Product.builder(
                        UuidProductIdentifier.random(),
                        ProductName.of("M6x20 Screws"),
                        ProductDescription.of("Stainless steel screws")
                )
                .asProductType(Unit.pieces(), ProductTrackingStrategy.IDENTICAL)
                .build()

        when:
        ProductInstance screwsBox = new InstanceBuilder(InstanceId.newOne())
                .asProductInstance(screws)
                .withQuantity(Quantity.of(1000, Unit.pieces()))
                .build()

        then:
        !screwsBox.serialNumber().isPresent()
        !screwsBox.batchId().isPresent()

        !screws.trackingStrategy().isTrackedIndividually()
        !screws.trackingStrategy().isTrackedByBatch()
        screws.trackingStrategy().isInterchangeable()
    }
    def "should reject #invalidConfiguration for #trackingStrategy products"() {
        given:
        ProductType product = Product.builder(
                        UuidProductIdentifier.random(),
                        ProductName.of("Tracked Product"),
                        ProductDescription.of("Product with tracking requirements")
                )
                .asProductType(Unit.pieces(), trackingStrategy)
                .build()
        InstanceBuilder configuredBuilder = configureTracking(new InstanceBuilder(InstanceId.newOne()))

        when:
        configuredBuilder
                .asProductInstance(product)
                .withQuantity(Quantity.of(1, Unit.pieces()))
                .build()

        then:
        thrown(IllegalArgumentException)

        where:
        invalidConfiguration  | trackingStrategy                                        | configureTracking
        "missing serial"      | ProductTrackingStrategy.INDIVIDUALLY_TRACKED            | { InstanceBuilder builder -> builder }
        "missing batch"       | ProductTrackingStrategy.BATCH_TRACKED                   | { InstanceBuilder builder -> builder }
        "batch without serial" | ProductTrackingStrategy.INDIVIDUALLY_AND_BATCH_TRACKED | { InstanceBuilder builder -> builder.withBatch(BatchId.newOne()) }
        "serial without batch" | ProductTrackingStrategy.INDIVIDUALLY_AND_BATCH_TRACKED | { InstanceBuilder builder -> builder.withSerial(SerialNumber.of("PHONE-123")) }
        "batch tracking"      | ProductTrackingStrategy.IDENTICAL                       | { InstanceBuilder builder -> builder.withBatch(BatchId.newOne()) }
        "individual tracking" | ProductTrackingStrategy.IDENTICAL                       | { InstanceBuilder builder -> builder.withSerial(SerialNumber.of("RICE-001")) }
    }
}
