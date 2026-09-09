package com.softwarearchetypes.product;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Product feature instances indexed by feature name. */
class ProductFeatureInstances {

    private final Map<String, ProductFeatureInstance> features;

    ProductFeatureInstances(Collection<ProductFeatureInstance> instances) {
        this.features = instances.stream()
                .collect(Collectors.toUnmodifiableMap(inst -> inst.featureType().name(), Function.identity()));
    }

    static ProductFeatureInstances empty() {
        return new ProductFeatureInstances(Set.of());
    }

    static ProductFeatureInstances of(ProductFeatureInstance... instances) {
        return new ProductFeatureInstances(Set.of(instances));
    }

    /**
     * Returns the feature instance by feature name.
     *
     * @return the instance, or empty when absent
     */
    Optional<ProductFeatureInstance> get(String featureName) {
        return Optional.ofNullable(features.get(featureName));
    }

    /**
     * Returns the feature instance by feature name.
     *
     * @return the instance, or empty when absent
     */
    Optional<ProductFeatureInstance> get(ProductFeatureType featureType) {
        return features.values().stream().filter(it -> it.isOfType(featureType)).findFirst();
    }

    /** Returns whether a feature with the given name exists. */
    boolean has(String featureName) {
        return features.containsKey(featureName);
    }

    /** Returns whether the feature exists. */
    boolean has(ProductFeatureType featureType) {
        return has(featureType.name());
    }

    /** Returns all feature instances. */
    Collection<ProductFeatureInstance> all() {
        return features.values();
    }

    /** Returns the number of feature instances. */
    int size() {
        return features.size();
    }

    boolean isEmpty() {
        return features.isEmpty();
    }

    /**
     * Validates that all mandatory features from ProductType are present.
     *
     * @throws IllegalArgumentException if any mandatory feature is missing
     */
    void validateAgainst(ProductFeatureTypes featureTypes) {
        Set<ProductFeatureType> mandatoryFeatures = featureTypes.mandatoryFeatures();

        for (ProductFeatureType mandatory : mandatoryFeatures) {
            if (!has(mandatory.name())) {
                throw new IllegalArgumentException("Mandatory feature '%s' is missing".formatted(mandatory.name()));
            }
        }

        for (String featureName : features.keySet()) {
            if (!featureTypes.has(featureName)) {
                throw new IllegalArgumentException("Feature '%s' is not defined in ProductType".formatted(featureName));
            }
        }
    }

    @Override
    public String toString() {
        return "ProductFeatureInstances{%s}"
                .formatted(features.values().stream()
                        .map(f -> "%s=%s".formatted(f.featureType().name(), f.value()))
                        .collect(Collectors.joining(", ")));
    }
}
