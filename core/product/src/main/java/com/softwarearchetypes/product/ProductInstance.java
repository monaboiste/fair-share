package com.softwarearchetypes.product;

import com.softwarearchetypes.quantity.Quantity;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** A validated instance of a product type with tracking, quantity, and feature values. */
class ProductInstance implements Instance {

    private final InstanceId id;
    private final ProductType productType;
    private final SerialNumber serialNumber;
    private final BatchId batchId;
    private final Quantity quantity;
    private final ProductFeatureInstances features;

    ProductInstance(
            InstanceId id,
            ProductType productType,
            @Nullable SerialNumber serialNumber,
            @Nullable BatchId batchId,
            @Nullable Quantity quantity,
            ProductFeatureInstances features) {
        if (id == null) {
            throw new IllegalArgumentException("InstanceId must be defined");
        }
        if (productType == null) {
            throw new IllegalArgumentException("ProductType must be defined");
        }
        if (features == null) {
            throw new IllegalArgumentException("ProductFeatureInstances must be defined");
        }

        validateTrackingRequirements(productType, serialNumber, batchId);
        validateQuantityUnit(productType, quantity);
        features.validateAgainst(productType.featureTypes());

        this.id = id;
        this.productType = productType;
        this.serialNumber = serialNumber;
        this.batchId = batchId;
        this.quantity = quantity;
        this.features = features;
    }

    /**
     * Validates that serial number and batch ID satisfy the product type's tracking strategy. Interchangeable products
     * must not define tracking identifiers, while tracked products must provide the identifiers required by their
     * strategy.
     *
     * @param productType product type defining the tracking requirements
     * @param serialNumber serial number to validate, or {@code null} if not provided
     * @param batchId batch identifier to validate, or {@code null} if not provided
     * @throws IllegalArgumentException if the tracking identifiers do not satisfy the strategy
     */
    private static void validateTrackingRequirements(
            ProductType productType, @Nullable SerialNumber serialNumber, @Nullable BatchId batchId) {
        ProductTrackingStrategy strategy = productType.trackingStrategy();

        if (strategy.isInterchangeable()) {
            if (serialNumber != null || batchId != null) {
                throw new IllegalArgumentException("IDENTICAL products must not have SerialNumber or BatchId");
            }
            return;
        }

        if (serialNumber == null && batchId == null) {
            throw new IllegalArgumentException(
                    "ProductInstance must have either SerialNumber or BatchId for strategy: " + strategy);
        }

        if (strategy.isTrackedIndividually() && serialNumber == null) {
            throw new IllegalArgumentException("ProductType requires individual tracking (strategy: " + strategy
                    + ") but no serial number defined");
        }

        if (strategy.isTrackedByBatch() && batchId == null) {
            throw new IllegalArgumentException(
                    "ProductType requires batch tracking (strategy: " + strategy + ") but no batch id defined");
        }

        if (strategy.requiresBothTrackingMethods() && (serialNumber == null || batchId == null)) {
            throw new IllegalArgumentException("ProductType requires both individual and batch tracking (strategy: "
                    + strategy + ") but neither serial number nor batch id defined");
        }
    }

    private static void validateQuantityUnit(ProductType productType, @Nullable Quantity quantity) {
        if (quantity != null && !quantity.unit().equals(productType.preferredUnit())) {
            throw new IllegalArgumentException("Quantity unit must match ProductType's preferred unit");
        }
    }

    @Override
    public InstanceId id() {
        return id;
    }

    @Override
    public Product product() {
        return productType;
    }

    ProductType productType() {
        return productType;
    }

    @Override
    public Optional<SerialNumber> serialNumber() {
        return Optional.ofNullable(serialNumber);
    }

    @Override
    public Optional<BatchId> batchId() {
        return Optional.ofNullable(batchId);
    }

    Optional<Quantity> quantity() {
        return Optional.ofNullable(quantity);
    }

    /** Returns the explicit quantity or one preferred unit when no quantity was provided. */
    Quantity effectiveQuantity() {
        return quantity != null ? quantity : Quantity.of(1, productType.preferredUnit());
    }

    ProductFeatureInstances features() {
        return features;
    }

    @Override
    @NonNull public String toString() {
        return "ProductInstance{id=%s, type=%s, serial=%s, batch=%s, quantity=%s, features=%s}"
                .formatted(
                        id,
                        productType.name(),
                        serialNumber != null ? serialNumber : "none",
                        batchId != null ? batchId : "none",
                        quantity != null ? quantity : "implicit 1 " + productType.preferredUnit(),
                        features);
    }
}
