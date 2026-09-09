package com.softwarearchetypes.product;

import com.softwarearchetypes.quantity.Quantity;
import java.time.Instant;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** A quantity of a product type tracked together for production and quality control. */
class Batch {

    private final BatchId id;
    private final BatchName name;
    private final ProductIdentifier batchOf;
    private final Quantity quantityInBatch;
    private final @Nullable Instant dateProduced;
    private final @Nullable Instant sellBy;
    private final @Nullable Instant useBy;
    private final @Nullable Instant bestBefore;
    private final @Nullable SerialNumber startSerialNumber;
    private final @Nullable SerialNumber endSerialNumber;
    private final @Nullable String comments;

    private Batch(Builder builder) {
        if (builder.id == null) {
            throw new IllegalArgumentException("BatchId must be defined");
        }
        if (builder.name == null) {
            throw new IllegalArgumentException("BatchName must be defined");
        }
        if (builder.productType == null) {
            throw new IllegalArgumentException("ProductType must be defined");
        }
        if (builder.quantityInBatch == null) {
            throw new IllegalArgumentException("Quantity in batch must be defined");
        }

        if (!builder.quantityInBatch.unit().equals(builder.productType.preferredUnit())) {
            throw new IllegalArgumentException("Batch quantity unit must match ProductType's preferred unit");
        }

        this.id = builder.id;
        this.name = builder.name;
        this.batchOf = builder.productType.id();
        this.quantityInBatch = builder.quantityInBatch;
        this.dateProduced = builder.dateProduced;
        this.sellBy = builder.sellBy;
        this.useBy = builder.useBy;
        this.bestBefore = builder.bestBefore;
        this.startSerialNumber = builder.startSerialNumber;
        this.endSerialNumber = builder.endSerialNumber;
        this.comments = builder.comments;
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
    public String toString() {
        return "Batch{id=%s, name=%s, of=%s, quantity=%s}".formatted(id, name, batchOf, quantityInBatch);
    }

    static class Builder {

        private @Nullable BatchId id;
        private @Nullable BatchName name;
        private @Nullable ProductType productType;
        private @Nullable Quantity quantityInBatch;
        private @Nullable Instant dateProduced;
        private @Nullable Instant sellBy;
        private @Nullable Instant useBy;
        private @Nullable Instant bestBefore;
        private @Nullable SerialNumber startSerialNumber;
        private @Nullable SerialNumber endSerialNumber;
        private @Nullable String comments;

        private Builder() {}

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

        Builder dateProduced(@Nullable Instant dateProduced) {
            this.dateProduced = dateProduced;
            return this;
        }

        Builder sellBy(@Nullable Instant sellBy) {
            this.sellBy = sellBy;
            return this;
        }

        Builder useBy(@Nullable Instant useBy) {
            this.useBy = useBy;
            return this;
        }

        Builder bestBefore(@Nullable Instant bestBefore) {
            this.bestBefore = bestBefore;
            return this;
        }

        Builder startSerialNumber(@Nullable SerialNumber startSerialNumber) {
            this.startSerialNumber = startSerialNumber;
            return this;
        }

        Builder endSerialNumber(@Nullable SerialNumber endSerialNumber) {
            this.endSerialNumber = endSerialNumber;
            return this;
        }

        Builder comments(@Nullable String comments) {
            this.comments = comments;
            return this;
        }

        Batch build() {
            return new Batch(this);
        }
    }
}
