package com.softwarearchetypes.product;

import org.jspecify.annotations.NonNull;

/** Defines a product feature type as mandatory or optional. */
record ProductFeatureTypeDefinition(ProductFeatureType featureType, boolean mandatory) {

    ProductFeatureTypeDefinition {
        if (featureType == null) {
            throw new IllegalArgumentException("ProductFeatureType must be defined");
        }
    }

    static ProductFeatureTypeDefinition mandatory(ProductFeatureType featureType) {
        return new ProductFeatureTypeDefinition(featureType, true);
    }

    static ProductFeatureTypeDefinition optional(ProductFeatureType featureType) {
        return new ProductFeatureTypeDefinition(featureType, false);
    }

    boolean isMandatory() {
        return mandatory;
    }

    boolean isOptional() {
        return !mandatory;
    }

    @Override
    @NonNull public String toString() {
        return "%s(%s)".formatted(mandatory ? "mandatory" : "optional", featureType.name());
    }
}
