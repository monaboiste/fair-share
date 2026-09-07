package com.softwarearchetypes.product;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Views returned by {@link ProductFacade} and {@link ProductCatalog}. */
public final class ProductViews {

    private ProductViews() {}

    /** Describes a product type. */
    public record ProductTypeView(
            String productId,
            String name,
            String description,
            String unit,
            String trackingStrategy,
            Set<FeatureTypeView> mandatoryFeatures,
            Set<FeatureTypeView> optionalFeatures) {}

    /**
     * Describes a configurable product feature.
     *
     * @param name feature name
     * @param valueType value type such as {@code TEXT} or {@code DECIMAL}
     * @param constraintType constraint type such as {@code ALLOWED_VALUES} or {@code NUMERIC_RANGE}
     * @param constraintConfig constraint-specific configuration
     * @param constraintDescription human-readable constraint description
     */
    public record FeatureTypeView(
            String name,
            String valueType,
            String constraintType,
            Map<String, Object> constraintConfig,
            String constraintDescription) {}

    /** Describes a catalog entry. */
    public record CatalogEntryView(
            String catalogEntryId,
            String displayName,
            String description,
            String productTypeId,
            Set<String> categories,
            @Nullable LocalDate availableFrom,
            @Nullable LocalDate availableUntil,
            Map<String, String> metadata) {}

    /** Provides keyed metadata attributes. */
    public record MetadataView(Map<String, String> attributes) {
        public @Nullable String get(String key) {
            return attributes.get(key);
        }

        public String getOrDefault(String key, String defaultValue) {
            return attributes.getOrDefault(key, defaultValue);
        }

        public boolean has(String key) {
            return attributes.containsKey(key);
        }
    }
}
