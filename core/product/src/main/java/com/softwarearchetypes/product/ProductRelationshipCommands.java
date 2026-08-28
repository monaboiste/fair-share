package com.softwarearchetypes.product;

import java.util.UUID;

/** Commands accepted by {@link ProductRelationshipsFacade}. */
public final class ProductRelationshipCommands {

    private ProductRelationshipCommands() {}

    /**
     * Defines a directed relationship between two products.
     *
     * @param fromProductId source product identifier
     * @param toProductId target product identifier
     * @param relationshipType {@link ProductRelationshipType} name
     */
    public record DefineRelationship(String fromProductId, String toProductId, String relationshipType) {
        public DefineRelationship {
            if (fromProductId == null || fromProductId.isBlank()) {
                throw new IllegalArgumentException("From product ID must be defined");
            }
            if (toProductId == null || toProductId.isBlank()) {
                throw new IllegalArgumentException("To product ID must be defined");
            }
            if (relationshipType == null || relationshipType.isBlank()) {
                throw new IllegalArgumentException("Relationship type must be defined");
            }
        }
    }

    /** Removes a product relationship. */
    public record RemoveRelationship(UUID relationshipId) {
        public RemoveRelationship {
            if (relationshipId == null) {
                throw new IllegalArgumentException("Relationship ID must be defined");
            }
        }
    }
}
