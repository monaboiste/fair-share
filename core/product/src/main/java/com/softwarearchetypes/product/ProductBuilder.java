package com.softwarearchetypes.product;

import com.softwarearchetypes.quantity.Unit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Builds product and package types through type-specific builders. */
public class ProductBuilder {

    @NonNull private final ProductIdentifier id;

    @NonNull private final ProductName name;

    @NonNull private final ProductDescription description;

    private ProductMetadata metadata = ProductMetadata.empty();

    private ApplicabilityConstraint applicabilityConstraint = ApplicabilityConstraint.alwaysTrue();

    ProductBuilder(@NonNull ProductIdentifier id, @NonNull ProductName name, @NonNull ProductDescription description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    /** Sets product metadata. */
    public ProductBuilder withMetadata(@Nullable ProductMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

    /** Adds a single metadata entry. */
    public ProductBuilder withMetadata(String key, String value) {
        if (metadata == null) {
            throw new IllegalArgumentException("Metadata must be set before adding entries");
        }
        this.metadata = this.metadata.with(key, value);
        return this;
    }

    /** Sets the product applicability constraint. */
    public ProductBuilder withApplicabilityConstraint(ApplicabilityConstraint constraint) {
        this.applicabilityConstraint = constraint;
        return this;
    }

    /** Returns a builder for a product type. */
    public ProductTypeBuilder asProductType(Unit preferredUnit, ProductTrackingStrategy trackingStrategy) {
        return new ProductTypeBuilder(preferredUnit, trackingStrategy);
    }

    /** Returns a builder for a package type. */
    public PackageTypeBuilder asPackageType() {
        return new PackageTypeBuilder();
    }

    /** Builds a product type using the enclosing builder's common fields. */
    public class ProductTypeBuilder {

        private final Unit preferredUnit;
        private final ProductTrackingStrategy trackingStrategy;
        private final List<ProductFeatureTypeDefinition> featureDefinitions = new ArrayList<>();

        ProductTypeBuilder(Unit preferredUnit, ProductTrackingStrategy trackingStrategy) {
            this.preferredUnit = preferredUnit;
            this.trackingStrategy = trackingStrategy;
        }

        public ProductTypeBuilder withMandatoryFeature(ProductFeatureType featureType) {
            this.featureDefinitions.add(ProductFeatureTypeDefinition.mandatory(featureType));
            return this;
        }

        public ProductTypeBuilder withOptionalFeature(ProductFeatureType featureType) {
            this.featureDefinitions.add(ProductFeatureTypeDefinition.optional(featureType));
            return this;
        }

        ProductTypeBuilder withFeature(ProductFeatureTypeDefinition definition) {
            this.featureDefinitions.add(definition);
            return this;
        }

        public ProductTypeBuilder withApplicabilityConstraint(ApplicabilityConstraint constraint) {
            ProductBuilder.this.applicabilityConstraint = constraint;
            return this;
        }

        public ProductTypeBuilder withMetadata(ProductMetadata metadata) {
            ProductBuilder.this.metadata = metadata;
            return this;
        }

        public ProductTypeBuilder withMetadata(Map<String, String> metadata) {
            ProductBuilder.this.metadata = new ProductMetadata(metadata);
            return this;
        }

        public ProductTypeBuilder withMetadata(String key, String value) {
            ProductBuilder.this.metadata = ProductBuilder.this.metadata.with(key, value);
            return this;
        }

        public ProductType build() {
            ProductFeatureTypes features = new ProductFeatureTypes(featureDefinitions);
            return new ProductType(
                    id,
                    name,
                    description,
                    preferredUnit,
                    trackingStrategy,
                    features,
                    metadata,
                    applicabilityConstraint);
        }
    }

    /** Builds a package type using the enclosing builder's common fields. */
    public class PackageTypeBuilder {

        private final Map<String, ProductSet> productSets = new HashMap<>();
        private final List<SelectionRule> selectionRules = new ArrayList<>();
        private ProductTrackingStrategy trackingStrategy = ProductTrackingStrategy.INDIVIDUALLY_TRACKED;

        PackageTypeBuilder() {}

        public PackageTypeBuilder withTrackingStrategy(ProductTrackingStrategy trackingStrategy) {
            this.trackingStrategy = trackingStrategy;
            return this;
        }

        /** Adds a product set that requires exactly one selection. */
        public PackageTypeBuilder withSingleChoice(String setName, ProductIdentifier... productIds) {
            return withChoice(setName, 1, 1, productIds);
        }

        /** Adds a product set that allows at most one selection. */
        public PackageTypeBuilder withOptionalChoice(String setName, ProductIdentifier... productIds) {
            return withChoice(setName, 0, 1, productIds);
        }

        /** Adds a product set that requires at least one selection. */
        public PackageTypeBuilder withRequiredChoice(String setName, ProductIdentifier... productIds) {
            return withChoice(setName, 1, Integer.MAX_VALUE, productIds);
        }

        /** Adds a product set with the given selection bounds. */
        public PackageTypeBuilder withChoice(String setName, int min, int max, ProductIdentifier... productIds) {
            ProductSet set = new ProductSet(setName, Set.of(productIds));
            productSets.put(setName, set);
            selectionRules.add(SelectionRule.isSubsetOf(set, min, max));
            return this;
        }

        public PackageTypeBuilder withProductSet(String setName, ProductIdentifier... productIds) {
            return withProductSet(new ProductSet(setName, Set.of(productIds)));
        }

        public PackageTypeBuilder withProductSet(ProductSet set) {
            productSets.put(set.name(), set);
            return this;
        }

        public PackageTypeBuilder withProductSets(ProductSet... productSets) {
            Arrays.stream(productSets).forEach(it -> this.productSets.put(it.name(), it));
            return this;
        }

        /** Adds a custom selection rule. */
        public PackageTypeBuilder withRule(SelectionRule rule) {
            selectionRules.add(rule);
            return this;
        }

        /** Returns a configured product set by name. */
        public ProductSet getProductSet(String setName) {
            return productSets.get(setName);
        }

        public PackageTypeBuilder withApplicabilityConstraint(ApplicabilityConstraint constraint) {
            ProductBuilder.this.applicabilityConstraint = constraint;
            return this;
        }

        public PackageTypeBuilder withMetadata(ProductMetadata metadata) {
            ProductBuilder.this.metadata = metadata;
            return this;
        }

        public PackageTypeBuilder withMetadata(Map<String, String> metadata) {
            ProductBuilder.this.metadata = new ProductMetadata(metadata);
            return this;
        }

        public PackageTypeBuilder withMetadata(String key, String value) {
            ProductBuilder.this.metadata = ProductBuilder.this.metadata.with(key, value);
            return this;
        }

        public PackageType build() {
            PackageStructure structure = new PackageStructure(productSets, selectionRules);
            return new PackageType(
                    id, name, description, trackingStrategy, metadata, applicabilityConstraint, structure);
        }
    }
}
