package com.softwarearchetypes.product;

import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Identifies a catalog entry independently of its product. */
public class CatalogEntryId {

    private final String value;

    private CatalogEntryId(String value) {
        if (value.isBlank()) {
            throw new IllegalArgumentException("CatalogEntryId must be defined");
        }
        this.value = value;
    }

    public static CatalogEntryId of(String value) {
        return new CatalogEntryId(value);
    }

    public static CatalogEntryId generate() {
        return new CatalogEntryId("CATALOG-" + UUID.randomUUID());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CatalogEntryId that)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
