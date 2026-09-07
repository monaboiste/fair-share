package com.softwarearchetypes.product;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** Query criteria accepted by {@link ProductFacade} and {@link ProductCatalog}. */
public final class ProductQueries {

    private ProductQueries() {}

    /** Criteria for finding a product type by identifier. */
    public record FindProductTypeCriteria(String productId) {
        public FindProductTypeCriteria {
            if (productId == null || productId.isBlank()) {
                throw new IllegalArgumentException("Product ID must be defined");
            }
        }
    }

    /** Criteria for finding product types by tracking strategy. */
    public record FindByTrackingStrategyCriteria(String trackingStrategy) {
        public FindByTrackingStrategyCriteria {
            if (trackingStrategy == null || trackingStrategy.isBlank()) {
                throw new IllegalArgumentException("Tracking strategy must be defined");
            }
        }
    }

    /**
     * Criteria for searching catalog entries. Null or empty filters are ignored.
     *
     * @param searchText text matched against display names and descriptions
     * @param categories categories matched using any-match semantics
     * @param availableAt date on which entries must be available
     * @param productTypeId product type identifier filter
     * @param productTypeFeatures accepted values grouped by feature name
     */
    public record SearchCatalogCriteria(
            @Nullable String searchText,
            @Nullable Set<String> categories,
            @Nullable LocalDate availableAt,
            @Nullable String productTypeId,
            @Nullable Map<String, Set<String>> productTypeFeatures) {

        /** Returns criteria without filters. */
        public static SearchCatalogCriteria all() {
            return new SearchCatalogCriteria(null, null, null, null, null);
        }

        /** Returns criteria that search display names and descriptions. */
        public static SearchCatalogCriteria byText(String searchText) {
            return new SearchCatalogCriteria(searchText, null, null, null, null);
        }

        /** Returns criteria that match any of the given categories. */
        public static SearchCatalogCriteria byCategories(Set<String> categories) {
            return new SearchCatalogCriteria(null, categories, null, null, null);
        }

        /** Returns criteria for entries available on the given date. */
        public static SearchCatalogCriteria availableAt(LocalDate date) {
            return new SearchCatalogCriteria(null, null, date, null, null);
        }

        /** Returns criteria for the given product type. */
        public static SearchCatalogCriteria byProductType(String productTypeId) {
            return new SearchCatalogCriteria(null, null, null, productTypeId, null);
        }

        /** Returns criteria that match any requested value for each feature. */
        public static SearchCatalogCriteria byFeatures(Map<String, Set<String>> features) {
            return new SearchCatalogCriteria(null, null, null, null, features);
        }
    }

    /** Criteria for finding a catalog entry by identifier. */
    public record FindCatalogEntryCriteria(String catalogEntryId) {
        public FindCatalogEntryCriteria {
            if (catalogEntryId == null || catalogEntryId.isBlank()) {
                throw new IllegalArgumentException("Catalog entry ID must be defined");
            }
        }
    }

    /** Criteria for finding catalog entries by category. */
    public record FindByCategoryCriteria(String category) {
        public FindByCategoryCriteria {
            if (category == null || category.isBlank()) {
                throw new IllegalArgumentException("Category must be defined");
            }
        }
    }

    /** Criteria for finding catalog entries available on a date. */
    public record FindAvailableAtCriteria(LocalDate date) {
        public FindAvailableAtCriteria {
            if (date == null) {
                throw new IllegalArgumentException("Date must be defined");
            }
        }
    }

    /**
     * Criteria for finding catalog entries by metadata.
     *
     * @param key required metadata key
     * @param value expected value, or {@code null} to match any value
     */
    public record FindByMetadataCriteria(
            String key, @Nullable String value) {
        public FindByMetadataCriteria {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Metadata key must be defined");
            }
        }
    }
}
