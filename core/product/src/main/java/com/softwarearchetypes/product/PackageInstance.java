package com.softwarearchetypes.product;

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** A validated package instance containing the delivered product instances. */
class PackageInstance implements Instance {

    private final InstanceId id;
    private final PackageType packageType;
    private final List<SelectedInstance> selection;
    private final @Nullable SerialNumber serialNumber;
    private final @Nullable BatchId batchId;

    PackageInstance(
            InstanceId id,
            PackageType packageType,
            List<SelectedInstance> selection,
            @Nullable SerialNumber serialNumber,
            @Nullable BatchId batchId) {
        validateTrackingRequirements(packageType, serialNumber, batchId);
        validateSelection(packageType, selection);

        this.id = id;
        this.packageType = packageType;
        this.selection = List.copyOf(selection);
        this.serialNumber = serialNumber;
        this.batchId = batchId;
    }

    private static void validateTrackingRequirements(
            PackageType packageType, @Nullable SerialNumber serialNumber, @Nullable BatchId batchId) {
        if (serialNumber == null && batchId == null) {
            throw new IllegalArgumentException("PackageInstance must have either SerialNumber or BatchId (or both)");
        }

        ProductTrackingStrategy strategy = validateAgainstStrategy(packageType, serialNumber, batchId);

        if (strategy.requiresBothTrackingMethods() && (serialNumber == null || batchId == null)) {
            throw new IllegalArgumentException(
                    "PackageType requires both individual and batch tracking (strategy: " + strategy + ")");
        }
    }

    private static ProductTrackingStrategy validateAgainstStrategy(
            PackageType packageType, @Nullable SerialNumber serialNumber, @Nullable BatchId batchId) {
        ProductTrackingStrategy strategy = packageType.trackingStrategy();

        if (strategy.isTrackedIndividually() && serialNumber == null) {
            throw new IllegalArgumentException("PackageType requires individual tracking (strategy: " + strategy
                    + ") but no serial number defined");
        }

        if (strategy.isTrackedByBatch() && batchId == null) {
            throw new IllegalArgumentException(
                    "PackageType requires batch tracking (strategy: " + strategy + ") but no batch id defined");
        }
        return strategy;
    }

    private static void validateSelection(PackageType packageType, List<SelectedInstance> selection) {
        List<SelectedProduct> selectedProducts =
                selection.stream().map(SelectedInstance::toSelectedProduct).toList();

        PackageValidationResult result = packageType.validateSelection(selectedProducts);
        if (!result.isValid()) {
            throw new IllegalArgumentException("Invalid package selection: " + String.join(", ", result.errors()));
        }
    }

    @Override
    public InstanceId id() {
        return id;
    }

    @Override
    public Product product() {
        return packageType;
    }

    PackageType packageType() {
        return packageType;
    }

    List<SelectedInstance> selection() {
        return selection;
    }

    @Override
    public Optional<SerialNumber> serialNumber() {
        return Optional.ofNullable(serialNumber);
    }

    @Override
    public Optional<BatchId> batchId() {
        return Optional.ofNullable(batchId);
    }

    @Override
    public String toString() {
        return "PackageInstance{id=%s, type=%s, serial=%s, batch=%s, selection=%d products}"
                .formatted(
                        id,
                        packageType.name(),
                        serialNumber != null ? serialNumber : "none",
                        batchId != null ? batchId : "none",
                        selection.size());
    }
}
