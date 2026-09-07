package com.softwarearchetypes.product;

/** Commands accepted by {@link ProductRelationshipsFacade}. */
public final class ProductRelationshipCommands {

    private ProductRelationshipCommands() {}

    /**
     * Defines a directed relationship between two products.
     *
     * @param fromProductId source product identifier
     * @param toProductId target product identifier
     * @param relationshipType type of the relationship
     */
    public record DefineRelationship(
            ProductIdentifier fromProductId, ProductIdentifier toProductId, ProductRelationshipType relationshipType) {
        public DefineRelationship {
            if (fromProductId == null) {
                throw new IllegalArgumentException("From product ID must be defined");
            }
            if (toProductId == null) {
                throw new IllegalArgumentException("To product ID must be defined");
            }
            if (relationshipType == null) {
                throw new IllegalArgumentException("Relationship type must be defined");
            }
        }
    }

    /** Removes a product relationship. */
    public record RemoveRelationship(ProductRelationshipId relationshipId) {
        public RemoveRelationship {
            if (relationshipId == null) {
                throw new IllegalArgumentException("Relationship ID must be defined");
            }
        }
    }
}
