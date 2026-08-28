package com.softwarearchetypes.product

import com.softwarearchetypes.quantity.Quantity
import com.softwarearchetypes.quantity.Unit
import java.time.Instant
import spock.lang.Specification

class BatchSpec extends Specification {

    private final ProductIdentifier productId = UuidProductIdentifier.of("123e4567-e89b-12d3-a456-426614174000")
    private final ProductType productType = ProductType.batchTracked(
            productId,
            ProductName.of("Coffee Beans"),
            ProductDescription.of("Roasted coffee beans"),
            Unit.kilograms())

    def "builder propagates required and optional fields"() {
        given:
        BatchId id = BatchId.of("123e4567-e89b-12d3-a456-426614174001")
        BatchName name = BatchName.of("Roast 2026-01")
        Quantity quantity = Quantity.of(250, Unit.kilograms())
        Instant dateProduced = Instant.parse("2026-01-02T03:04:05Z")
        Instant sellBy = Instant.parse("2026-06-02T03:04:05Z")
        Instant useBy = Instant.parse("2026-07-02T03:04:05Z")
        Instant bestBefore = Instant.parse("2026-05-02T03:04:05Z")
        SerialNumber startSerial = SerialNumber.of("COFFEE-0001")
        SerialNumber endSerial = SerialNumber.of("COFFEE-0250")

        when:
        Batch batch = Batch.builder()
                .id(id)
                .name(name)
                .batchOf(productType)
                .quantityInBatch(quantity)
                .dateProduced(dateProduced)
                .sellBy(sellBy)
                .useBy(useBy)
                .bestBefore(bestBefore)
                .startSerialNumber(startSerial)
                .endSerialNumber(endSerial)
                .comments("Quality checked")
                .build()

        then:
        batch.id() == id
        batch.name() == name
        batch.batchOf() == productId
        batch.quantityInBatch() == quantity
        batch.dateProduced().get() == dateProduced
        batch.sellBy().get() == sellBy
        batch.useBy().get() == useBy
        batch.bestBefore().get() == bestBefore
        batch.startSerialNumber().get() == startSerial
        batch.endSerialNumber().get() == endSerial
        batch.comments().get() == "Quality checked"
    }

    def "optional fields are empty by default"() {
        when:
        Batch batch = Batch.builder()
                .id(BatchId.newOne())
                .name(BatchName.of("Unlabelled batch"))
                .batchOf(productType)
                .quantityInBatch(Quantity.of(1, Unit.kilograms()))
                .build()

        then:
        !batch.dateProduced().present
        !batch.sellBy().present
        !batch.useBy().present
        !batch.bestBefore().present
        !batch.startSerialNumber().present
        !batch.endSerialNumber().present
        !batch.comments().present
    }

    def "required fields must be defined"() {
        given:
        Batch.Builder builder = Batch.builder()
                .id(BatchId.newOne())
                .name(BatchName.of("Required fields"))
                .batchOf(productType)
                .quantityInBatch(Quantity.of(1, Unit.kilograms()))

        when:
        without(builder, missingField)

        then:
        thrown(IllegalArgumentException)

        where:
        missingField << ["id", "name", "batchOf", "quantityInBatch"]
    }

    def "quantity unit must match product type preferred unit"() {
        when:
        Batch.builder()
                .id(BatchId.newOne())
                .name(BatchName.of("Wrong unit"))
                .batchOf(productType)
                .quantityInBatch(Quantity.of(1, Unit.pieces()))
                .build()

        then:
        thrown(IllegalArgumentException)
    }

    def "toString contains identifying batch details"() {
        given:
        BatchId id = BatchId.of("123e4567-e89b-12d3-a456-426614174001")
        Batch batch = Batch.builder()
                .id(id)
                .name(BatchName.of("Roast 2026-01"))
                .batchOf(productType)
                .quantityInBatch(Quantity.of(250, Unit.kilograms()))
                .build()

        expect:
        batch.toString() == "Batch{id=${id}, name=Roast 2026-01, of=${productId}, quantity=2.5E+2 kg}"
    }

    private static void without(Batch.Builder builder, String field) {
        switch (field) {
            case "id" -> builder.id(null)
            case "name" -> builder.name(null)
            case "batchOf" -> builder.batchOf(null)
            case "quantityInBatch" -> builder.quantityInBatch(null)
        }
        builder.build()
    }
}
