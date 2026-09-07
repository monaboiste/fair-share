package com.softwarearchetypes.product;

import com.softwarearchetypes.quantity.Quantity;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Builds product and package instances through type-specific builders. */
class InstanceBuilder {

    private final InstanceId id;

    @Nullable private SerialNumber serialNumber;

    @Nullable private BatchId batchId;

    InstanceBuilder(@NonNull InstanceId id) {
        this.id = id;
    }

    /** Sets serial number for the instance. */
    InstanceBuilder withSerial(@Nullable SerialNumber serialNumber) {
        this.serialNumber = serialNumber;
        return this;
    }

    /** Sets batch ID for the instance. */
    InstanceBuilder withBatch(@Nullable BatchId batchId) {
        this.batchId = batchId;
        return this;
    }

    /** Returns a builder for a product instance. */
    ProductInstanceBuilder asProductInstance(ProductType productType) {
        return new ProductInstanceBuilder(productType);
    }

    /** Returns a builder for a package instance. */
    PackageInstanceBuilder asPackageInstance(PackageType packageType) {
        return new PackageInstanceBuilder(packageType);
    }

    /** Builds a product instance using the enclosing builder's tracking fields. */
    class ProductInstanceBuilder {
        private final ProductType productType;
        private Quantity quantity;
        private final List<ProductFeatureInstance> features = new ArrayList<>();

        ProductInstanceBuilder(ProductType productType) {
            this.productType = productType;
        }

        ProductInstanceBuilder withQuantity(Quantity quantity) {
            this.quantity = quantity;
            return this;
        }

        ProductInstanceBuilder withFeature(ProductFeatureInstance feature) {
            this.features.add(feature);
            return this;
        }

        ProductInstanceBuilder withFeature(ProductFeatureType featureType, Object value) {
            this.features.add(new ProductFeatureInstance(featureType, value));
            return this;
        }

        ProductInstance build() {
            ProductFeatureInstances featureInstances = new ProductFeatureInstances(features);
            return new ProductInstance(id, productType, serialNumber, batchId, quantity, featureInstances);
        }
    }

    /** Builds a package instance using the enclosing builder's tracking fields. */
    class PackageInstanceBuilder {
        private final PackageType packageType;
        private List<SelectedInstance> selection;

        PackageInstanceBuilder(PackageType packageType) {
            this.packageType = packageType;
        }

        PackageInstanceBuilder withSelection(List<SelectedInstance> selection) {
            this.selection = selection;
            return this;
        }

        PackageInstance build() {
            return new PackageInstance(id, packageType, selection, serialNumber, batchId);
        }
    }
}
