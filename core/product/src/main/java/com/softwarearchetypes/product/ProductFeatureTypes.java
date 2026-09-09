package com.softwarearchetypes.product;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Product feature type definitions indexed by feature name. */
class ProductFeatureTypes {

    private final Map<String, ProductFeatureTypeDefinition> features;

    ProductFeatureTypes(Collection<ProductFeatureTypeDefinition> definitions) {
        this.features = definitions.stream()
                .collect(Collectors.toUnmodifiableMap(def -> def.featureType().name(), def -> def));
    }

    static ProductFeatureTypes empty() {
        return new ProductFeatureTypes(List.of());
    }

    static ProductFeatureTypes of(ProductFeatureTypeDefinition... definitions) {
        return new ProductFeatureTypes(List.of(definitions));
    }

    /**
     * Returns the feature type definition by name.
     *
     * @return the definition, or empty when absent
     */
    Optional<ProductFeatureTypeDefinition> get(String featureName) {
        return Optional.ofNullable(features.get(featureName));
    }

    /**
     * Returns the feature type by name.
     *
     * @return the feature type, or empty when absent
     */
    Optional<ProductFeatureType> getFeatureType(String featureName) {
        return get(featureName).map(ProductFeatureTypeDefinition::featureType);
    }

    /** Returns whether a feature with the given name exists. */
    boolean has(String featureName) {
        return features.containsKey(featureName);
    }

    /**
     * Returns whether the named feature exists and is mandatory.
     *
     * @return {@code true} if the feature exists and is mandatory
     */
    boolean isMandatory(String featureName) {
        return get(featureName).map(ProductFeatureTypeDefinition::isMandatory).orElse(false);
    }

    /** Returns all mandatory feature types. */
    Set<ProductFeatureType> mandatoryFeatures() {
        return features.values().stream()
                .filter(ProductFeatureTypeDefinition::isMandatory)
                .map(ProductFeatureTypeDefinition::featureType)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Returns all optional feature types. */
    Set<ProductFeatureType> optionalFeatures() {
        return features.values().stream()
                .filter(ProductFeatureTypeDefinition::isOptional)
                .map(ProductFeatureTypeDefinition::featureType)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Returns all feature types. */
    Set<ProductFeatureType> allFeatures() {
        return features.values().stream()
                .map(ProductFeatureTypeDefinition::featureType)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Returns the number of feature types. */
    int size() {
        return features.size();
    }

    boolean isEmpty() {
        return features.isEmpty();
    }

    @Override
    public String toString() {
        return "ProductFeatureTypes{mandatory=%d, optional=%d}"
                .formatted(mandatoryFeatures().size(), optionalFeatures().size());
    }
}
