package com.softwarearchetypes.product;

import com.softwarearchetypes.quantity.Unit;
import org.jspecify.annotations.NonNull;

/** Defines a product's identity, measurement, tracking, features, and applicability. */
public class ProductType implements Product {

    private final ProductIdentifier id;
    private final ProductName name;
    private final ProductDescription description;
    private final Unit preferredUnit;
    private final ProductTrackingStrategy trackingStrategy;
    private final ProductFeatureTypes featureTypes;
    private final ProductMetadata metadata;
    private final ApplicabilityConstraint applicabilityConstraint;

    ProductType(
            ProductIdentifier id,
            ProductName name,
            ProductDescription description,
            Unit preferredUnit,
            ProductTrackingStrategy trackingStrategy,
            ProductFeatureTypes featureTypes,
            ProductMetadata metadata,
            ApplicabilityConstraint applicabilityConstraint) {
        if (id == null) {
            throw new IllegalArgumentException("ProductIdentifier must be defined");
        }
        if (name == null) {
            throw new IllegalArgumentException("ProductName must be defined");
        }
        if (description == null) {
            throw new IllegalArgumentException("ProductDescription must be defined");
        }
        if (preferredUnit == null) {
            throw new IllegalArgumentException("Unit must be defined");
        }
        if (trackingStrategy == null) {
            throw new IllegalArgumentException("ProductTrackingStrategy must be defined");
        }
        if (featureTypes == null) {
            throw new IllegalArgumentException("ProductFeatureTypes must be defined");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("ProductMetadata must be defined");
        }
        if (applicabilityConstraint == null) {
            throw new IllegalArgumentException("ApplicabilityConstraint must be defined");
        }
        this.id = id;
        this.name = name;
        this.description = description;
        this.preferredUnit = preferredUnit;
        this.trackingStrategy = trackingStrategy;
        this.featureTypes = featureTypes;
        this.metadata = metadata;
        this.applicabilityConstraint = applicabilityConstraint;
    }

    /** Creates an interchangeable product type measured in pieces. */
    public static ProductType define(ProductIdentifier id, ProductName name, ProductDescription description) {
        return new ProductType(
                id,
                name,
                description,
                Unit.pieces(),
                ProductTrackingStrategy.IDENTICAL,
                ProductFeatureTypes.empty(),
                ProductMetadata.empty(),
                ApplicabilityConstraint.alwaysTrue());
    }

    /** Creates a unique product type measured in pieces. */
    public static ProductType unique(ProductIdentifier id, ProductName name, ProductDescription description) {
        return new ProductType(
                id,
                name,
                description,
                Unit.pieces(),
                ProductTrackingStrategy.UNIQUE,
                ProductFeatureTypes.empty(),
                ProductMetadata.empty(),
                ApplicabilityConstraint.alwaysTrue());
    }

    /** Creates a product type where each instance is individually tracked (e.g., by serial number). */
    public static ProductType individuallyTracked(
            ProductIdentifier id, ProductName name, ProductDescription description, Unit preferredUnit) {
        return new ProductType(
                id,
                name,
                description,
                preferredUnit,
                ProductTrackingStrategy.INDIVIDUALLY_TRACKED,
                ProductFeatureTypes.empty(),
                ProductMetadata.empty(),
                ApplicabilityConstraint.alwaysTrue());
    }

    /** Creates a product type where instances are tracked by production batch. */
    public static ProductType batchTracked(
            ProductIdentifier id, ProductName name, ProductDescription description, Unit preferredUnit) {
        return new ProductType(
                id,
                name,
                description,
                preferredUnit,
                ProductTrackingStrategy.BATCH_TRACKED,
                ProductFeatureTypes.empty(),
                ProductMetadata.empty(),
                ApplicabilityConstraint.alwaysTrue());
    }

    /** Creates a product type where instances are tracked both individually and by batch. */
    public static ProductType individuallyAndBatchTracked(
            ProductIdentifier id, ProductName name, ProductDescription description, Unit preferredUnit) {
        return new ProductType(
                id,
                name,
                description,
                preferredUnit,
                ProductTrackingStrategy.INDIVIDUALLY_AND_BATCH_TRACKED,
                ProductFeatureTypes.empty(),
                ProductMetadata.empty(),
                ApplicabilityConstraint.alwaysTrue());
    }

    /** Creates a product type where instances are interchangeable (identical). */
    public static ProductType identical(
            ProductIdentifier id, ProductName name, ProductDescription description, Unit preferredUnit) {
        return new ProductType(
                id,
                name,
                description,
                preferredUnit,
                ProductTrackingStrategy.IDENTICAL,
                ProductFeatureTypes.empty(),
                ProductMetadata.empty(),
                ApplicabilityConstraint.alwaysTrue());
    }

    /** Returns a builder with the given tracking and measurement settings. */
    public static ProductBuilder.ProductTypeBuilder builder(
            ProductIdentifier id,
            ProductName name,
            ProductDescription description,
            Unit preferredUnit,
            ProductTrackingStrategy trackingStrategy) {
        return new ProductBuilder(id, name, description).asProductType(preferredUnit, trackingStrategy);
    }

    @Override
    public ProductIdentifier id() {
        return id;
    }

    @Override
    public ProductName name() {
        return name;
    }

    @Override
    public ProductDescription description() {
        return description;
    }

    public Unit preferredUnit() {
        return preferredUnit;
    }

    public ProductTrackingStrategy trackingStrategy() {
        return trackingStrategy;
    }

    ProductFeatureTypes featureTypes() {
        return featureTypes;
    }

    @Override
    public ProductMetadata metadata() {
        return metadata;
    }

    @Override
    public ApplicabilityConstraint applicabilityConstraint() {
        return applicabilityConstraint;
    }

    ProductIdentifier identifier() {
        return id;
    }

    @Override
    public boolean isApplicableFor(ApplicabilityContext context) {
        return applicabilityConstraint.isSatisfiedBy(context);
    }

    @Override
    @NonNull public String toString() {
        return "ProductType{id=%s, name=%s, unit=%s, tracking=%s, features=%s}"
                .formatted(id, name, preferredUnit, trackingStrategy, featureTypes);
    }
}
