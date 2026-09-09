package com.softwarearchetypes.product;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.product.ProductCommands.AddToOffer;
import com.softwarearchetypes.product.ProductCommands.DiscontinueProduct;
import com.softwarearchetypes.product.ProductCommands.UpdateMetadata;
import com.softwarearchetypes.product.ProductQueries.FindAvailableAtCriteria;
import com.softwarearchetypes.product.ProductQueries.FindByCategoryCriteria;
import com.softwarearchetypes.product.ProductQueries.FindByMetadataCriteria;
import com.softwarearchetypes.product.ProductQueries.FindCatalogEntryCriteria;
import com.softwarearchetypes.product.ProductQueries.SearchCatalogCriteria;
import com.softwarearchetypes.product.ProductViews.CatalogEntryView;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/** Manages the commercial product catalog through commands and queries. */
class ProductCatalog {

    private final CatalogEntryRepository catalogRepository;
    private final ProductTypeRepository productTypeRepository;

    ProductCatalog(CatalogEntryRepository catalogRepository, ProductTypeRepository productTypeRepository) {
        this.catalogRepository = catalogRepository;
        this.productTypeRepository = productTypeRepository;
    }

    static ProductCatalog create(ProductTypeRepository productTypeRepository) {
        return new ProductCatalog(CatalogEntryRepository.inMemory(), productTypeRepository);
    }

    /** Adds a product type to the catalog. */
    Result<String, CatalogEntryId> handle(AddToOffer command) {
        try {
            var productType = productTypeRepository
                    .findByIdValue(command.productTypeId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("ProductType not found: " + command.productTypeId()));

            var validity = buildValidity(command.availableFrom(), command.availableUntil());
            var catalogEntryId = CatalogEntryId.generate();

            var catalogEntry = CatalogEntry.builder()
                    .id(catalogEntryId)
                    .displayName(command.displayName())
                    .description(command.description())
                    .product(productType)
                    .categories(command.categories())
                    .validity(validity)
                    .metadata(command.metadata())
                    .build();

            catalogRepository.save(catalogEntry);

            return Result.success(catalogEntryId);

        } catch (Exception e) {
            return Result.failure(
                    Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()));
        }
    }

    /** Discontinues a product from the offer by setting the validity end date. */
    Result<String, CatalogEntryId> handle(DiscontinueProduct command) {
        try {
            var catalogEntryId = CatalogEntryId.of(command.catalogEntryId());
            var catalogEntry = catalogRepository
                    .findById(catalogEntryId)
                    .orElseThrow(
                            () -> new IllegalArgumentException("Catalog entry not found: " + command.catalogEntryId()));

            var availableFrom = catalogEntry.validity().from();
            var newValidity = availableFrom != null
                    ? Validity.between(availableFrom, command.discontinuationDate())
                    : Validity.until(command.discontinuationDate());

            var updated = catalogEntry.withValidity(newValidity);
            catalogRepository.save(updated);

            return Result.success(catalogEntryId);

        } catch (Exception e) {
            return Result.failure(
                    Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()));
        }
    }

    /** Updates catalog entry metadata. */
    Result<String, CatalogEntryId> handle(UpdateMetadata command) {
        try {
            var catalogEntryId = CatalogEntryId.of(command.catalogEntryId());
            var catalogEntry = catalogRepository
                    .findById(catalogEntryId)
                    .orElseThrow(
                            () -> new IllegalArgumentException("Catalog entry not found: " + command.catalogEntryId()));

            var updated = catalogEntry.withMetadata(command.metadata());
            catalogRepository.save(updated);

            return Result.success(catalogEntryId);

        } catch (Exception e) {
            return Result.failure(
                    Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()));
        }
    }

    /** Searches catalog entries with multiple filters. */
    Set<CatalogEntryView> findBy(SearchCatalogCriteria criteria) {
        var entries = catalogRepository.findAll();

        return entries.stream()
                .filter(entry -> matchesSearchText(entry, criteria.searchText()))
                .filter(entry -> matchesCategories(entry, criteria.categories()))
                .filter(entry -> matchesAvailability(entry, criteria.availableAt()))
                .filter(entry -> matchesProductType(entry, criteria.productTypeId()))
                .filter(entry -> matchesFeatures(entry, criteria.productTypeFeatures()))
                .map(this::toCatalogEntryView)
                .collect(Collectors.toSet());
    }

    /** Finds a catalog entry by its identifier. */
    Optional<CatalogEntryView> findBy(FindCatalogEntryCriteria criteria) {
        var catalogEntryId = CatalogEntryId.of(criteria.catalogEntryId());
        return catalogRepository.findById(catalogEntryId).map(this::toCatalogEntryView);
    }

    /** Finds catalog entries by category. */
    Set<CatalogEntryView> findBy(FindByCategoryCriteria criteria) {
        return catalogRepository.findByCategory(criteria.category()).stream()
                .map(this::toCatalogEntryView)
                .collect(Collectors.toSet());
    }

    /** Finds catalog entries available on a specific date. */
    Set<CatalogEntryView> findBy(FindAvailableAtCriteria criteria) {
        return catalogRepository.findAll().stream()
                .filter(entry -> entry.isAvailableAt(criteria.date()))
                .map(this::toCatalogEntryView)
                .collect(Collectors.toSet());
    }

    /** Finds catalog entries by metadata key-value. */
    Set<CatalogEntryView> findBy(FindByMetadataCriteria criteria) {
        return catalogRepository.findAll().stream()
                .filter(entry -> matchesMetadata(entry, criteria.key(), criteria.value()))
                .map(this::toCatalogEntryView)
                .collect(Collectors.toSet());
    }

    private boolean matchesSearchText(CatalogEntry entry, @Nullable String searchText) {
        if (searchText == null || searchText.isBlank()) {
            return true;
        }
        String lowerSearch = searchText.toLowerCase(Locale.ROOT);
        return entry.displayName().toLowerCase(Locale.ROOT).contains(lowerSearch)
                || entry.description().toLowerCase(Locale.ROOT).contains(lowerSearch);
    }

    private boolean matchesCategories(CatalogEntry entry, @Nullable Set<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return true;
        }
        return categories.stream().anyMatch(entry::isInCategory);
    }

    private boolean matchesAvailability(CatalogEntry entry, @Nullable LocalDate date) {
        if (date == null) {
            return true;
        }
        return entry.isAvailableAt(date);
    }

    private boolean matchesProductType(CatalogEntry entry, @Nullable String productTypeId) {
        if (productTypeId == null || productTypeId.isBlank()) {
            return true;
        }
        return entry.product().id().toString().equals(productTypeId);
    }

    private boolean matchesFeatures(CatalogEntry entry, @Nullable Map<String, Set<String>> features) {
        if (features == null || features.isEmpty()) {
            return true;
        }

        var product = entry.product();
        if (!(product instanceof ProductType productType)) {
            return false;
        }

        var allFeatures = new java.util.HashSet<>(productType.featureTypes().mandatoryFeatures());
        allFeatures.addAll(productType.featureTypes().optionalFeatures());

        for (var featureEntry : features.entrySet()) {
            var featureName = featureEntry.getKey();
            var requestedValues = featureEntry.getValue();

            var feature = allFeatures.stream()
                    .filter(f -> f.name().equals(featureName))
                    .findFirst();

            if (feature.isEmpty()) {
                return false;
            }

            var featureType = feature.get();
            boolean anyValueMatches = requestedValues.stream().anyMatch(featureType::isValidValue);

            if (!anyValueMatches) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesMetadata(CatalogEntry entry, String key, @Nullable String value) {
        if (value == null) {
            return entry.hasMetadata(key);
        }
        return value.equals(entry.getMetadata(key).orElse(null));
    }

    private Validity buildValidity(@Nullable LocalDate from, @Nullable LocalDate to) {
        if (from != null && to != null) {
            return Validity.between(from, to);
        } else if (from != null) {
            return Validity.from(from);
        } else if (to != null) {
            return Validity.until(to);
        } else {
            return Validity.always();
        }
    }

    private CatalogEntryView toCatalogEntryView(CatalogEntry entry) {
        return new CatalogEntryView(
                entry.id().value(),
                entry.displayName(),
                entry.description(),
                entry.product().id().toString(),
                entry.categories(),
                entry.validity().from(),
                entry.validity().to(),
                entry.metadata());
    }
}
