package com.softwarearchetypes.product;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** A product's commercial description and availability in a catalog. */
public class CatalogEntry {

    private final CatalogEntryId id;
    private final String displayName;
    private final String description;
    private final Product product;
    private final Set<String> categories;
    private final Validity validity;
    private final Map<String, String> metadata;
    /** Restricts availability using catalog-specific sales conditions. */
    private final ApplicabilityConstraint salesConstraint;

    private CatalogEntry(
            CatalogEntryId id,
            String displayName,
            String description,
            Product product,
            @Nullable Set<String> categories,
            Validity validity,
            @Nullable Map<String, String> metadata,
            ApplicabilityConstraint salesConstraint) {
        if (id == null) {
            throw new IllegalArgumentException("CatalogEntryId must be defined");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Display name must be defined");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description must be defined");
        }
        if (product == null) {
            throw new IllegalArgumentException("Product must be defined");
        }
        if (validity == null) {
            throw new IllegalArgumentException("Validity must be defined");
        }
        if (salesConstraint == null) {
            throw new IllegalArgumentException("salesConstraint must be defined");
        }

        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.product = product;
        this.categories = categories != null ? Set.copyOf(categories) : Set.of();
        this.validity = validity;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        this.salesConstraint = salesConstraint;
    }

    public static Builder builder() {
        return new Builder();
    }

    public CatalogEntryId id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public Product product() {
        return product;
    }

    public Set<String> categories() {
        return categories;
    }

    public Validity validity() {
        return validity;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    public ApplicabilityConstraint salesConstraint() {
        return salesConstraint;
    }

    /** Returns whether the entry is valid on the given date. */
    public boolean isAvailableAt(LocalDate date) {
        return validity.isValidAt(date);
    }

    /** Returns whether the date, sales constraint, and product constraint permit availability. */
    public boolean isAvailableFor(ApplicabilityContext context, LocalDate date) {
        return validity.isValidAt(date) && salesConstraint.isSatisfiedBy(context) && product.isApplicableFor(context);
    }

    /** Returns whether the entry belongs to the given category. */
    public boolean isInCategory(String category) {
        return categories.contains(category);
    }

    /** Returns metadata value for a given key. */
    public Optional<String> getMetadata(String key) {
        return Optional.ofNullable(metadata.get(key));
    }

    /** Returns the metadata value or the default when absent. */
    public String getMetadataOrDefault(String key, String defaultValue) {
        return metadata.getOrDefault(key, defaultValue);
    }

    /** Returns whether the metadata key exists. */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }

    /** Returns a copy with the given validity. */
    CatalogEntry withValidity(Validity newValidity) {
        return new CatalogEntry(
                id, displayName, description, product, categories, newValidity, metadata, salesConstraint);
    }

    /** Returns a copy with the given metadata. */
    CatalogEntry withMetadata(Map<String, String> newMetadata) {
        return new CatalogEntry(
                id, displayName, description, product, categories, validity, newMetadata, salesConstraint);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CatalogEntry that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    @NonNull public String toString() {
        return "CatalogEntry{id=%s, displayName='%s', product=%s, categories=%s, validity=%s}"
                .formatted(id, displayName, product.name(), categories, validity);
    }

    public static class Builder {
        private CatalogEntryId id;
        private String displayName;
        private String description;
        private Product product;
        private Set<String> categories = new HashSet<>();
        private Validity validity;
        private Map<String, String> metadata = new HashMap<>();
        private ApplicabilityConstraint salesConstraint = ApplicabilityConstraint.alwaysTrue();

        public Builder id(CatalogEntryId id) {
            this.id = id;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder product(Product product) {
            this.product = product;
            return this;
        }

        public Builder categories(Set<String> categories) {
            this.categories = new HashSet<>(categories);
            return this;
        }

        public Builder category(String category) {
            this.categories.add(category);
            return this;
        }

        public Builder validity(Validity validity) {
            this.validity = validity;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }

        public Builder withMetadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder salesConstraint(ApplicabilityConstraint salesConstraint) {
            this.salesConstraint = salesConstraint;
            return this;
        }

        public CatalogEntry build() {
            return new CatalogEntry(
                    id, displayName, description, product, categories, validity, metadata, salesConstraint);
        }
    }
}
