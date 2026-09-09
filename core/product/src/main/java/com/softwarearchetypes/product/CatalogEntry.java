package com.softwarearchetypes.product;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

    private CatalogEntry(Builder builder) {
        if (builder.id == null) {
            throw new IllegalArgumentException("CatalogEntryId must be defined");
        }
        if (builder.displayName == null || builder.displayName.isBlank()) {
            throw new IllegalArgumentException("Display name must be defined");
        }
        if (builder.description == null || builder.description.isBlank()) {
            throw new IllegalArgumentException("Description must be defined");
        }
        if (builder.product == null) {
            throw new IllegalArgumentException("Product must be defined");
        }
        if (builder.validity == null) {
            throw new IllegalArgumentException("Validity must be defined");
        }

        this.id = builder.id;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.product = builder.product;
        this.categories = Set.copyOf(builder.categories);
        this.validity = builder.validity;
        this.metadata = Map.copyOf(builder.metadata);
        this.salesConstraint = builder.salesConstraint;
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
        return toBuilder().validity(newValidity).build();
    }

    /** Returns a copy with the given metadata. */
    CatalogEntry withMetadata(Map<String, String> newMetadata) {
        return toBuilder().metadata(newMetadata).build();
    }

    private Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public boolean equals(@Nullable Object o) {
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
    public String toString() {
        return "CatalogEntry{id=%s, displayName='%s', product=%s, categories=%s, validity=%s}"
                .formatted(id, displayName, product.name(), categories, validity);
    }

    public static class Builder {

        private @Nullable CatalogEntryId id;
        private @Nullable String displayName;
        private @Nullable String description;
        private @Nullable Product product;
        private Set<String> categories = new HashSet<>();
        private @Nullable Validity validity;
        private Map<String, String> metadata = new HashMap<>();
        private ApplicabilityConstraint salesConstraint = ApplicabilityConstraint.alwaysTrue();

        private Builder() {}

        private Builder(CatalogEntry entry) {
            this.id = entry.id;
            this.displayName = entry.displayName;
            this.description = entry.description;
            this.product = entry.product;
            this.categories = new HashSet<>(entry.categories);
            this.validity = entry.validity;
            this.metadata = new HashMap<>(entry.metadata);
            this.salesConstraint = entry.salesConstraint;
        }

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

        public Builder categories(@Nullable Set<String> categories) {
            this.categories = categories != null ? new HashSet<>(categories) : new HashSet<>();
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

        public Builder metadata(@Nullable Map<String, String> metadata) {
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
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
            return new CatalogEntry(this);
        }
    }
}
