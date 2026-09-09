package com.softwarearchetypes.product;

import java.util.List;

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
    public String toString() {
        return "PackageType{id=%s, name=%s, tracking=%s, structure=%s}"
                .formatted(id, name, trackingStrategy, structure);
    }
}
