package com.softwarearchetypes.product;

import java.util.List;
import org.jspecify.annotations.NonNull;

/** Defines a product composed of products or nested packages. */
public record PackageType(
        ProductIdentifier id,
        ProductName name,
        ProductDescription description,
        ProductTrackingStrategy trackingStrategy,
        ProductMetadata metadata,
        ApplicabilityConstraint applicabilityConstraint,
        PackageStructure structure)
        implements Product {

    public PackageType {
        if (id == null) {
            throw new IllegalArgumentException("ProductIdentifier must be defined");
        }
        if (name == null) {
            throw new IllegalArgumentException("ProductName must be defined");
        }
        if (description == null) {
            throw new IllegalArgumentException("ProductDescription must be defined");
        }
        if (trackingStrategy == null) {
            throw new IllegalArgumentException("ProductTrackingStrategy must be defined");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("ProductMetadata must be defined");
        }
        if (applicabilityConstraint == null) {
            throw new IllegalArgumentException("ApplicabilityConstraint must be defined");
        }
        if (structure == null) {
            throw new IllegalArgumentException("PackageStructure must be defined");
        }
    }

    /** Creates an individually tracked package without applicability constraints. */
    public static PackageType define(
            ProductIdentifier id, ProductName name, ProductDescription description, PackageStructure structure) {
        return new PackageType(
                id,
                name,
                description,
                ProductTrackingStrategy.INDIVIDUALLY_TRACKED,
                ProductMetadata.empty(),
                ApplicabilityConstraint.alwaysTrue(),
                structure);
    }

    /** Validates whether selected products satisfy the package structure. */
    public PackageValidationResult validateSelection(List<SelectedProduct> selection) {
        return structure.validate(selection);
    }

    @Override
    @NonNull public String toString() {
        return "PackageType{id=%s, name=%s, tracking=%s, structure=%s}"
                .formatted(id, name, trackingStrategy, structure);
    }
}
