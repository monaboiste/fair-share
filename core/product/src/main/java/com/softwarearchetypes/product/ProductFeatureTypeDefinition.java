package com.softwarearchetypes.product;

/** Defines a product feature type as mandatory or optional. */
record ProductFeatureTypeDefinition(ProductFeatureType featureType, boolean mandatory) {

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
    public String toString() {
        return "%s(%s)".formatted(mandatory ? "mandatory" : "optional", featureType.name());
    }
}
