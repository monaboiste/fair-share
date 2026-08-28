package com.softwarearchetypes.product;

import com.softwarearchetypes.quantity.Quantity;
import java.time.Instant;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** A quantity of a product type tracked together for production and quality control. */
class Batch {

    private final BatchId id;
    private final BatchName name;
    private final ProductIdentifier batchOf;
    private final Quantity quantityInBatch;
    private final Instant dateProduced;
    private final Instant sellBy;
    private final Instant useBy;
    private final Instant bestBefore;
    private final SerialNumber startSerialNumber;
    private final SerialNumber endSerialNumber;
    private final String comments;

    private Batch(
            BatchId id,
            BatchName name,
            ProductType productType,
            Quantity quantityInBatch,
            @Nullable Instant dateProduced,
            @Nullable Instant sellBy,
            @Nullable Instant useBy,
            @Nullable Instant bestBefore,
            @Nullable SerialNumber startSerialNumber,
            @Nullable SerialNumber endSerialNumber,
            @Nullable String comments) {
        if (id == null) {
            throw new IllegalArgumentException("BatchId must be defined");
        }
        if (name == null) {
            throw new IllegalArgumentException("BatchName must be defined");
        }
        if (productType == null) {
            throw new IllegalArgumentException("ProductType must be defined");
        }
        if (quantityInBatch == null) {
            throw new IllegalArgumentException("Quantity in batch must be defined");
        }

        if (!quantityInBatch.unit().equals(productType.preferredUnit())) {
            throw new IllegalArgumentException("Batch quantity unit must match ProductType's preferred unit");
        }

        this.id = id;
        this.name = name;
        this.batchOf = productType.id();
        this.quantityInBatch = quantityInBatch;
        this.dateProduced = dateProduced;
        this.sellBy = sellBy;
        this.useBy = useBy;
        this.bestBefore = bestBefore;
        this.startSerialNumber = startSerialNumber;
        this.endSerialNumber = endSerialNumber;
        this.comments = comments;
    }

    static Builder builder() {
        return new Builder();
    }

    BatchId id() {
        return id;
    }

    BatchName name() {
        return name;
    }

    ProductIdentifier batchOf() {
        return batchOf;
    }

    Quantity quantityInBatch() {
        return quantityInBatch;
    }

    Optional<Instant> dateProduced() {
        return Optional.ofNullable(dateProduced);
    }

    Optional<Instant> sellBy() {
        return Optional.ofNullable(sellBy);
    }

    Optional<Instant> useBy() {
        return Optional.ofNullable(useBy);
    }

    Optional<Instant> bestBefore() {
        return Optional.ofNullable(bestBefore);
    }

    Optional<SerialNumber> startSerialNumber() {
        return Optional.ofNullable(startSerialNumber);
    }

    Optional<SerialNumber> endSerialNumber() {
        return Optional.ofNullable(endSerialNumber);
    }

    Optional<String> comments() {
        return Optional.ofNullable(comments);
    }

    @Override
    @NonNull public String toString() {
        return "Batch{id=%s, name=%s, of=%s, quantity=%s}".formatted(id, name, batchOf, quantityInBatch);
    }

    static class Builder {
        private BatchId id;
        private BatchName name;
        private ProductType productType;
        private Quantity quantityInBatch;
        private Instant dateProduced;
        private Instant sellBy;
        private Instant useBy;
        private Instant bestBefore;
        private SerialNumber startSerialNumber;
        private SerialNumber endSerialNumber;
        private String comments;

        Builder id(BatchId id) {
            this.id = id;
            return this;
        }

        Builder name(BatchName name) {
            this.name = name;
            return this;
        }

        Builder batchOf(ProductType productType) {
            this.productType = productType;
            return this;
        }

        Builder quantityInBatch(Quantity quantityInBatch) {
            this.quantityInBatch = quantityInBatch;
            return this;
        }

        Builder dateProduced(Instant dateProduced) {
            this.dateProduced = dateProduced;
            return this;
        }

        Builder sellBy(Instant sellBy) {
            this.sellBy = sellBy;
            return this;
        }

        Builder useBy(Instant useBy) {
            this.useBy = useBy;
            return this;
        }

        Builder bestBefore(Instant bestBefore) {
            this.bestBefore = bestBefore;
            return this;
        }

        Builder startSerialNumber(SerialNumber startSerialNumber) {
            this.startSerialNumber = startSerialNumber;
            return this;
        }

        Builder endSerialNumber(SerialNumber endSerialNumber) {
            this.endSerialNumber = endSerialNumber;
            return this;
        }

        Builder comments(String comments) {
            this.comments = comments;
            return this;
        }

        Batch build() {
            return new Batch(
                    id,
                    name,
                    productType,
                    quantityInBatch,
                    dateProduced,
                    sellBy,
                    useBy,
                    bestBefore,
                    startSerialNumber,
                    endSerialNumber,
                    comments);
        }
    }
}
